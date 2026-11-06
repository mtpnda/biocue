#!/usr/bin/env python3
"""
BioCue Mock Sensor Developer Console
====================================
Standalone developer tool to emit simulated biometric sensor data to the BioCue
Wear OS and Android Companion apps over ADB without needing a real wrist or physical watch.

Supports:
  1. Interactive Web Server & GUI Dashboard (default or --web)
  2. Direct CLI commands & automation scripts (--scenario, --stream, etc.)
  3. Real-time live ECG/vitals canvas visualization
  4. Full lifecycle event simulation (Start, Label, End, SOS GPS)
  5. One-click device permission granting and app launching
"""

import argparse
import http.server
import json
import os
import random
import re
import socketserver
import subprocess
import sys
import threading
import time
import urllib.parse
import webbrowser

# Default configurations
DEFAULT_PORT = 8080
PACKAGE_NAME = "com.mtpnda.biocue"

# Standard Presets
SCENARIOS = {
    "calm": {
        "title": "Calm & Baseline",
        "icon": "🧘",
        "desc": "Resting vitals, nominal HRV, steady state (Tier 0)",
        "hr": 70.0,
        "rmssd": 55.0,
        "acc": 0.05,
        "rep": 0.02,
        "slope": 0.0,
        "offbody": False,
        "exercising": False,
    },
    "mild_stress": {
        "title": "Mild Stress (Tier 1)",
        "icon": "⚡",
        "desc": "Elevated HR, dropping HRV, positive slope -> Early Cue",
        "hr": 92.0,
        "rmssd": 30.0,
        "acc": 0.15,
        "rep": 0.10,
        "slope": 1.2,
        "offbody": False,
        "exercising": False,
    },
    "overwhelmed": {
        "title": "Overwhelmed (Tier 2)",
        "icon": "🚨",
        "desc": "Tachycardia, suppressed HRV, high stimming -> Tier 2 Meltdown Alert",
        "hr": 128.0,
        "rmssd": 16.0,
        "acc": 0.45,
        "rep": 0.65,
        "slope": 2.8,
        "offbody": False,
        "exercising": False,
    },
    "exercise": {
        "title": "Physical Exercise",
        "icon": "🏃",
        "desc": "High HR + high motion; marked as exercising to suppress false alerts",
        "hr": 140.0,
        "rmssd": 22.0,
        "acc": 1.85,
        "rep": 0.85,
        "slope": 1.5,
        "offbody": False,
        "exercising": True,
    },
    "off_body": {
        "title": "Watch Off Wrist",
        "icon": "📴",
        "desc": "Watch removed from wrist, vitals nullified",
        "hr": 0.0,
        "rmssd": 0.0,
        "acc": 0.01,
        "rep": 0.0,
        "slope": 0.0,
        "offbody": True,
        "exercising": False,
    },
}

class AdbController:
    """Manages adb discovery and intent dispatching."""
    
    @staticmethod
    def run_cmd(cmd_args):
        try:
            res = subprocess.run(cmd_args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=10)
            return res.returncode == 0, res.stdout.strip(), res.stderr.strip()
        except Exception as e:
            return False, "", str(e)

    @classmethod
    def get_devices(cls):
        ok, out, _ = cls.run_cmd(["adb", "devices", "-l"])
        if not ok:
            return []
        devices = []
        for line in out.splitlines()[1:]:
            line = line.strip()
            if not line or line.startswith("*"):
                continue
            parts = line.split()
            serial = parts[0]
            status = parts[1] if len(parts) > 1 else "unknown"
            info = " ".join(parts[2:]) if len(parts) > 2 else ""
            model_match = re.search(r"model:(\S+)", info)
            model = model_match.group(1) if model_match else serial
            is_wear = "wear" in serial.lower() or "wear" in info.lower() or "watch" in serial.lower()
            devices.append({
                "serial": serial,
                "status": status,
                "model": model,
                "is_wear": is_wear
            })
        return devices

    @classmethod
    def emit_sensor_window(cls, device_serial, hr, rmssd, acc, rep, slope, offbody, exercising):
        ts = int(time.time() * 1000)
        cmd = ["adb"]
        if device_serial and device_serial != "all":
            cmd += ["-s", device_serial]
        cmd += [
            "shell", "am", "broadcast",
            "-a", "com.mtpnda.biocue.MOCK_SENSOR_WINDOW",
            "-p", PACKAGE_NAME,
            "--el", "timestamp", str(ts),
            "--ef", "hrMean", f"{float(hr):.2f}",
            "--ef", "rmssd", f"{float(rmssd):.2f}",
            "--ef", "accMag", f"{float(acc):.2f}",
            "--ef", "repetitiveness", f"{float(rep):.2f}",
            "--ef", "hrSlope", f"{float(slope):.2f}",
            "--ez", "isOffBody", "true" if offbody else "false",
            "--ez", "isExercising", "true" if exercising else "false"
        ]
        return cls.run_cmd(cmd)

    @classmethod
    def emit_event(cls, device_serial, action, data=None):
        data = data or {}
        cmd = ["adb"]
        if device_serial and device_serial != "all":
            cmd += ["-s", device_serial]

        if action == "start":
            trigger = data.get("trigger", "MANUAL")
            cmd += [
                "shell", "am", "broadcast",
                "-a", "com.mtpnda.biocue.MOCK_MANUAL_EVENT",
                "-p", PACKAGE_NAME,
                "--es", "trigger", trigger
            ]
        elif action == "label":
            label = data.get("label", "OVERWHELMED")
            cmd += [
                "shell", "am", "broadcast",
                "-a", "com.mtpnda.biocue.MOCK_EVENT_LABEL",
                "-p", PACKAGE_NAME,
                "--es", "label", label
            ]
        elif action == "end":
            cmd += [
                "shell", "am", "broadcast",
                "-a", "com.mtpnda.biocue.MOCK_EVENT_END",
                "-p", PACKAGE_NAME
            ]
        elif action == "sos":
            lat = data.get("lat", -6.2088)
            lng = data.get("lng", 106.8456)
            cmd += [
                "shell", "am", "broadcast",
                "-a", "com.mtpnda.biocue.MOCK_SOS_LOCATION",
                "-p", PACKAGE_NAME,
                "--ef", "lat", str(lat),
                "--ef", "lng", str(lng)
            ]
        return cls.run_cmd(cmd)

    @classmethod
    def set_watch_test_mode(cls, device_serial, enabled):
        cmd = ["adb"]
        if device_serial and device_serial != "all":
            cmd += ["-s", device_serial]
        cmd += [
            "shell", "am", "broadcast",
            "-a", "com.mtpnda.biocue.wear.SET_TEST_MODE",
            "-p", PACKAGE_NAME,
            "--ez", "enabled", "true" if enabled else "false"
        ]
        return cls.run_cmd(cmd)

    @classmethod
    def grant_permissions(cls, device_serial):
        perms = [
            "android.permission.BODY_SENSORS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.POST_NOTIFICATIONS"
        ]
        results = []
        for p in perms:
            cmd = ["adb"]
            if device_serial and device_serial != "all":
                cmd += ["-s", device_serial]
            cmd += ["shell", "pm", "grant", PACKAGE_NAME, p]
            ok, out, err = cls.run_cmd(cmd)
            results.append({"permission": p, "ok": ok, "error": err if not ok else ""})
        return results

    @classmethod
    def launch_app(cls, device_serial, target="mobile"):
        cmd = ["adb"]
        if device_serial and device_serial != "all":
            cmd += ["-s", device_serial]
        if target == "wear":
            cmd += ["shell", "am", "start", "-n", f"{PACKAGE_NAME}/.wear.presentation.MainActivity"]
        else:
            cmd += ["shell", "am", "start", "-n", f"{PACKAGE_NAME}/.MainActivity"]
        return cls.run_cmd(cmd)


class EmissionEngine:
    """Background streaming engine with Gaussian micro-jitter."""
    
    def __init__(self):
        self.lock = threading.Lock()
        self.is_streaming = False
        self.stream_thread = None
        self.interval = 2.0  # seconds
        self.selected_device = "all"
        self.jitter = True
        self.current_params = dict(SCENARIOS["calm"])
        self.selected_scenario = "calm"
        self.total_emitted = 0
        self.history = []

    def set_params(self, params, scenario_name="custom"):
        with self.lock:
            self.current_params.update(params)
            self.selected_scenario = scenario_name

    def emit_once(self, device=None):
        target_device = device or self.selected_device
        with self.lock:
            p = dict(self.current_params)
            jitter_active = self.jitter and not p.get("offbody", False)
            hr_jitter = random.uniform(-1.0, 1.0) if jitter_active else 0.0
            rmssd_jitter = random.uniform(-0.8, 0.8) if jitter_active else 0.0
            
            hr = max(0.0, p["hr"] + hr_jitter)
            rmssd = max(0.0, p["rmssd"] + rmssd_jitter)
            acc = p["acc"]
            rep = p["rep"]
            slope = p["slope"]
            offbody = p["offbody"]
            exercising = p["exercising"]
            scenario = self.selected_scenario

        ok, out, err = AdbController.emit_sensor_window(
            target_device, hr, rmssd, acc, rep, slope, offbody, exercising
        )

        with self.lock:
            self.total_emitted += 1
            record = {
                "id": self.total_emitted,
                "time": time.strftime("%H:%M:%S"),
                "hr": round(hr, 1),
                "rmssd": round(rmssd, 1),
                "acc": round(acc, 2),
                "scenario": scenario,
                "device": target_device,
                "ok": ok,
                "output": out if ok else err
            }
            self.history.insert(0, record)
            self.history = self.history[:35]
        return record

    def start_streaming(self, interval=None, device=None):
        with self.lock:
            if self.is_streaming:
                return
            if interval:
                self.interval = max(0.5, float(interval))
            if device:
                self.selected_device = device
            self.is_streaming = True
            self.stream_thread = threading.Thread(target=self._stream_loop, daemon=True)
            self.stream_thread.start()

    def stop_streaming(self):
        with self.lock:
            self.is_streaming = False

    def _stream_loop(self):
        while True:
            with self.lock:
                if not self.is_streaming:
                    break
                iv = self.interval
            self.emit_once()
            time.sleep(iv)

    def get_state(self):
        with self.lock:
            return {
                "streaming": self.is_streaming,
                "interval": self.interval,
                "selected_device": self.selected_device,
                "selected_scenario": self.selected_scenario,
                "total_emitted": self.total_emitted,
                "params": dict(self.current_params),
                "history": list(self.history)
            }


ENGINE = EmissionEngine()

HTML_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>BioCue Sensor Dev Console</title>
  <style>
    :root {
      --bg: #0B0E14;
      --card-bg: #151922;
      --card-border: #232936;
      --primary: #6366F1;
      --primary-hover: #4F46E5;
      --accent-green: #10B981;
      --accent-amber: #F59E0B;
      --accent-rose: #F43F5E;
      --accent-teal: #14B8A6;
      --text: #F3F4F6;
      --text-muted: #9CA3AF;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      padding: 24px;
      line-height: 1.5;
    }
    .container { max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--card-border);
    }
    .header-left { display: flex; align-items: center; gap: 12px; }
    h1 { font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }
    .badge {
      background: #312E81;
      color: #A5B4FC;
      font-size: 11px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      text-transform: uppercase;
    }
    .status-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      padding: 6px 14px;
      border-radius: 999px;
      font-size: 13px;
    }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-muted); }
    .dot.live { background: var(--accent-green); box-shadow: 0 0 8px var(--accent-green); animation: pulse 2s infinite; }
    @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.4; } 100% { opacity: 1; } }

    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    @media(max-width: 860px) { .grid-2 { grid-template-columns: 1fr; } }

    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 14px;
      padding: 18px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .card-title {
      font-size: 15px;
      font-weight: 600;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: var(--text);
    }
    .devices-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
    }
    select, button, input[type=number] {
      background: #1E2330;
      border: 1px solid var(--card-border);
      color: var(--text);
      padding: 8px 12px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    select:focus, button:focus { outline: 2px solid var(--primary); }
    button:hover { background: #2A3142; border-color: #374151; }
    button.btn-primary { background: var(--primary); color: white; border: none; }
    button.btn-primary:hover { background: var(--primary-hover); }
    button.btn-danger { background: var(--accent-rose); color: white; border: none; }
    button.btn-amber { background: var(--accent-amber); color: black; border: none; font-weight: 600; }
    button.btn-success { background: var(--accent-green); color: black; border: none; font-weight: 600; }

    .presets-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 10px;
    }
    .preset-card {
      background: #1B202C;
      border: 1px solid var(--card-border);
      padding: 12px;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .preset-card:hover { border-color: var(--primary); transform: translateY(-2px); }
    .preset-card.active { border-color: var(--primary); background: #23283A; box-shadow: 0 0 12px rgba(99, 102, 241, 0.25); }
    .preset-header { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 13px; }
    .preset-desc { font-size: 11px; color: var(--text-muted); line-height: 1.3; }

    .slider-row { display: flex; flex-direction: column; gap: 6px; }
    .slider-label { display: flex; justify-content: space-between; font-size: 13px; }
    .slider-val { font-weight: 700; }
    input[type=range] {
      width: 100%;
      height: 6px;
      border-radius: 3px;
      background: #252D3D;
      outline: none;
      -webkit-appearance: none;
    }
    input[type=range]::-webkit-slider-thumb {
      -webkit-appearance: none;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: var(--primary);
      cursor: pointer;
      box-shadow: 0 0 6px rgba(99, 102, 241, 0.6);
    }

    .toggles-row { display: flex; gap: 20px; align-items: center; font-size: 13px; }
    .toggles-row label { display: flex; align-items: center; gap: 6px; cursor: pointer; }

    canvas#ecg {
      width: 100%;
      height: 100px;
      background: #0D1017;
      border: 1px solid var(--card-border);
      border-radius: 8px;
      display: block;
    }

    table.log-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
    }
    table.log-table th, table.log-table td {
      padding: 8px 10px;
      text-align: left;
      border-bottom: 1px solid #1F2432;
    }
    table.log-table th { color: var(--text-muted); font-weight: 600; }
    .history-container { max-height: 220px; overflow-y: auto; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="header-left">
        <h1>BioCue Sensor Dev Console</h1>
        <span class="badge">Development</span>
      </div>
      <div class="status-pill">
        <div id="stream-dot" class="dot"></div>
        <span id="stream-text">Engine Paused</span>
        <span style="color: var(--card-border);">|</span>
        <span id="stat-count" style="font-weight: 600; color: var(--accent-green);">0 sent</span>
      </div>
    </header>

    <!-- ADB Devices Bar -->
    <div class="card" style="padding: 12px 18px; flex-direction: row; justify-content: space-between; align-items: center; flex-wrap: wrap;">
      <div class="devices-bar">
        <span style="font-size: 13px; font-weight: 600; color: var(--text-muted);">TARGET ADB DEVICE:</span>
        <select id="device-select" onchange="onDeviceChange()">
          <option value="all">All Connected Devices</option>
        </select>
        <button onclick="refreshDevices()">🔄 Refresh Devices</button>
      </div>
      <div class="devices-bar">
        <button class="btn-primary" onclick="grantPermissions()">🛡️ Grant All Permissions</button>
        <button onclick="launchApp('wear')">⌚ Launch Watch App</button>
        <button onclick="launchApp('mobile')">📱 Launch Mobile App</button>
      </div>
    </div>

    <!-- ECG Visualizer & Stream Control -->
    <div class="card">
      <div class="card-title">
        <span>Live Heart Rate Waveform & Telemetry Stream</span>
        <div style="display: flex; gap: 8px; align-items: center;">
          <span style="font-size: 12px; color: var(--text-muted);">Interval:</span>
          <select id="interval-select" onchange="updateInterval()">
            <option value="1">1 second</option>
            <option value="2" selected>2 seconds</option>
            <option value="3">3 seconds</option>
            <option value="5">5 seconds</option>
          </select>
          <button id="stream-btn" class="btn-primary" onclick="toggleStreaming()">▶ Start Stream</button>
          <button class="btn-amber" onclick="emitOnce()">⚡ Emit 1x</button>
        </div>
      </div>
      <canvas id="ecg"></canvas>
    </div>

    <div class="grid-2">
      <!-- Preset Scenarios -->
      <div class="card">
        <div class="card-title">
          <span>Preset Scenarios</span>
          <span style="font-size: 11px; color: var(--text-muted);">One-click trigger</span>
        </div>
        <div class="presets-grid" id="presets-list">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Fine-Tuning Sliders -->
      <div class="card">
        <div class="card-title">
          <span>Manual Parameter Sliders</span>
          <span id="current-scenario-badge" class="badge" style="background:#252D3D; color:var(--text);">Calm</span>
        </div>

        <div class="slider-row">
          <div class="slider-label">
            <span>Heart Rate (HR)</span>
            <span class="slider-val" id="val-hr" style="color: var(--accent-rose);">70 BPM</span>
          </div>
          <input type="range" id="slider-hr" min="40" max="180" step="1" value="70" oninput="onSliderChange()">
        </div>

        <div class="slider-row">
          <div class="slider-label">
            <span>Heart Rate Variability (RMSSD)</span>
            <span class="slider-val" id="val-rmssd" style="color: var(--accent-teal);">55 ms</span>
          </div>
          <input type="range" id="slider-rmssd" min="5" max="100" step="1" value="55" oninput="onSliderChange()">
        </div>

        <div class="slider-row">
          <div class="slider-label">
            <span>Movement Magnitude (AccMag)</span>
            <span class="slider-val" id="val-acc" style="color: var(--accent-amber);">0.05 g</span>
          </div>
          <input type="range" id="slider-acc" min="0" max="3" step="0.05" value="0.05" oninput="onSliderChange()">
        </div>

        <div class="slider-row">
          <div class="slider-label">
            <span>Repetitive Stimming Power</span>
            <span class="slider-val" id="val-rep" style="color: var(--primary);">0.02</span>
          </div>
          <input type="range" id="slider-rep" min="0" max="1" step="0.05" value="0.02" oninput="onSliderChange()">
        </div>

        <div class="toggles-row">
          <label><input type="checkbox" id="chk-exercising" onchange="onSliderChange()"> Exercising</label>
          <label><input type="checkbox" id="chk-offbody" onchange="onSliderChange()"> Off Wrist</label>
          <label><input type="checkbox" id="chk-jitter" checked onchange="toggleJitter()"> Natural Vitals Jitter</label>
        </div>
      </div>
    </div>

    <!-- Event Lifecycle Simulation & Watch synthetic mode -->
    <div class="card">
      <div class="card-title">
        <span>Stress Event & Watch Control</span>
      </div>
      <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
        <span style="font-size: 13px; font-weight: 600; color: var(--text-muted);">EVENT LIFECYCLE:</span>
        <button class="btn-primary" onclick="triggerEvent('start', {trigger:'MANUAL'})">🚩 Start Manual Event</button>
        <button onclick="triggerEvent('start', {trigger:'AUTO'})">⚡ Start Auto Event</button>
        <button onclick="triggerEvent('label', {label:'OVERWHELMED'})">🏷️ Label Overwhelmed</button>
        <button onclick="triggerEvent('label', {label:'EXCITED'})">🏷️ Label Excited</button>
        <button onclick="triggerEvent('label', {label:'ACTIVE'})">🏷️ Label Active</button>
        <button class="btn-danger" onclick="triggerEvent('end')">🏁 Resolve / End Event</button>
        <button class="btn-amber" onclick="triggerEvent('sos', {lat:-6.2088, lng:106.8456})">🆘 Send SOS GPS Fix</button>
        <span style="color: var(--card-border);">|</span>
        <button id="btn-watch-mode" onclick="toggleWatchSynthetic()">⌚ Toggle Watch Synthetic Mode</button>
      </div>
    </div>

    <!-- Live Telemetry History Log -->
    <div class="card">
      <div class="card-title">
        <span>Transmission Log (Recent ADB Broadcasts)</span>
      </div>
      <div class="history-container">
        <table class="log-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Time</th>
              <th>HR (BPM)</th>
              <th>RMSSD (ms)</th>
              <th>Motion (g)</th>
              <th>Scenario</th>
              <th>Target Device</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody id="log-tbody">
            <tr><td colspan="8" style="text-align:center; color:var(--text-muted);">No packets transmitted yet.</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <script>
    let state = {};
    let presets = %PRESETS_JSON%;
    let watchMode = false;

    // ECG Canvas Animation
    const canvas = document.getElementById('ecg');
    const ctx = canvas.getContext('2d');
    let points = [];
    const maxPoints = 200;

    function resizeCanvas() {
      canvas.width = canvas.parentElement.clientWidth - 36;
      canvas.height = 100;
    }
    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    function drawEcg() {
      ctx.fillStyle = '#0D1017';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Grid lines
      ctx.strokeStyle = '#181E2C';
      ctx.lineWidth = 1;
      for (let x = 0; x < canvas.width; x += 30) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
      }
      for (let y = 0; y < canvas.height; y += 20) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
      }

      if (points.length < 2) return;
      ctx.strokeStyle = '#6366F1';
      ctx.lineWidth = 2;
      ctx.beginPath();
      const step = canvas.width / (maxPoints - 1);
      for (let i = 0; i < points.length; i++) {
        const x = i * step;
        const y = canvas.height - ((points[i] - 40) / (180 - 40) * (canvas.height - 20) + 10);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      // Glowing head point
      const lastX = (points.length - 1) * step;
      const lastY = canvas.height - ((points[points.length - 1] - 40) / (180 - 40) * (canvas.height - 20) + 10);
      ctx.fillStyle = '#F43F5E';
      ctx.beginPath();
      ctx.arc(lastX, lastY, 4, 0, Math.PI * 2);
      ctx.fill();
    }

    function renderPresets() {
      const el = document.getElementById('presets-list');
      el.innerHTML = '';
      for (const [key, p] of Object.entries(presets)) {
        const active = (state.selected_scenario === key) ? 'active' : '';
        el.innerHTML += `
          <div class="preset-card ${active}" onclick="selectScenario('${key}')">
            <div class="preset-header"><span>${p.icon}</span><span>${p.title}</span></div>
            <div class="preset-desc">${p.desc}</div>
          </div>
        `;
      }
    }

    async function fetchState() {
      try {
        const res = await fetch('/api/status');
        state = await res.json();
        updateUI();
      } catch (e) {
        console.error(e);
      }
    }

    function updateUI() {
      document.getElementById('stream-text').innerText = state.streaming ? 'Streaming Active' : 'Engine Paused';
      document.getElementById('stream-dot').className = state.streaming ? 'dot live' : 'dot';
      document.getElementById('stream-btn').innerText = state.streaming ? '⏹ Stop Stream' : '▶ Start Stream';
      document.getElementById('stream-btn').className = state.streaming ? 'btn-danger' : 'btn-primary';
      document.getElementById('stat-count').innerText = state.total_emitted + ' sent';
      document.getElementById('current-scenario-badge').innerText = (state.selected_scenario || 'Custom').toUpperCase();

      if (state.params) {
        document.getElementById('val-hr').innerText = Math.round(state.params.hr) + ' BPM';
        document.getElementById('val-rmssd').innerText = Math.round(state.params.rmssd) + ' ms';
        document.getElementById('val-acc').innerText = Number(state.params.acc).toFixed(2) + ' g';
        document.getElementById('val-rep').innerText = Number(state.params.rep).toFixed(2);
      }

      renderPresets();

      // Render Logs
      const tbody = document.getElementById('log-tbody');
      if (state.history && state.history.length > 0) {
        tbody.innerHTML = state.history.map(h => `
          <tr>
            <td>#${h.id}</td>
            <td><strong>${h.time}</strong></td>
            <td style="color:var(--accent-rose); font-weight:700;">${h.hr} BPM</td>
            <td style="color:var(--accent-teal);">${h.rmssd} ms</td>
            <td>${h.acc} g</td>
            <td><span class="badge" style="background:#1E293B;">${h.scenario}</span></td>
            <td><code>${h.device}</code></td>
            <td><span style="color:${h.ok ? 'var(--accent-green)' : 'var(--accent-rose)'};">${h.ok ? '✓ Sent' : '✗ Error'}</span></td>
          </tr>
        `).join('');

        // Push HR to waveform
        if (state.history[0]) {
          points.push(state.history[0].hr);
          if (points.length > maxPoints) points.shift();
          drawEcg();
        }
      }
    }

    async function selectScenario(name) {
      const p = presets[name];
      if (!p) return;
      document.getElementById('slider-hr').value = p.hr;
      document.getElementById('slider-rmssd').value = p.rmssd;
      document.getElementById('slider-acc').value = p.acc;
      document.getElementById('slider-rep').value = p.rep;
      document.getElementById('chk-exercising').checked = p.exercising;
      document.getElementById('chk-offbody').checked = p.offbody;

      await fetch('/api/set_params', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ params: p, scenario: name })
      });
      fetchState();
    }

    async function onSliderChange() {
      const params = {
        hr: parseFloat(document.getElementById('slider-hr').value),
        rmssd: parseFloat(document.getElementById('slider-rmssd').value),
        acc: parseFloat(document.getElementById('slider-acc').value),
        rep: parseFloat(document.getElementById('slider-rep').value),
        slope: 0.0,
        exercising: document.getElementById('chk-exercising').checked,
        offbody: document.getElementById('chk-offbody').checked
      };
      await fetch('/api/set_params', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ params: params, scenario: 'custom' })
      });
      fetchState();
    }

    async function emitOnce() {
      await fetch('/api/emit', { method: 'POST' });
      fetchState();
    }

    async function toggleStreaming() {
      const next = !state.streaming;
      const interval = parseFloat(document.getElementById('interval-select').value);
      await fetch('/api/stream', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ active: next, interval: interval })
      });
      fetchState();
    }

    async function updateInterval() {
      const interval = parseFloat(document.getElementById('interval-select').value);
      if (state.streaming) {
        await fetch('/api/stream', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({ active: true, interval: interval })
        });
      }
    }

    async function refreshDevices() {
      const res = await fetch('/api/devices');
      const data = await res.json();
      const sel = document.getElementById('device-select');
      sel.innerHTML = '<option value="all">All Connected Devices</option>';
      data.devices.forEach(d => {
        sel.innerHTML += `<option value="${d.serial}">${d.model} (${d.serial}) ${d.is_wear ? '⌚ Wear' : '📱 Phone'}</option>`;
      });
    }

    async function onDeviceChange() {
      const dev = document.getElementById('device-select').value;
      await fetch('/api/set_device', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ device: dev })
      });
      fetchState();
    }

    async function grantPermissions() {
      const dev = document.getElementById('device-select').value;
      const res = await fetch('/api/action', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ action: 'grant_permissions', device: dev })
      });
      alert("Requested permissions granted via ADB!");
    }

    async function launchApp(target) {
      const dev = document.getElementById('device-select').value;
      await fetch('/api/action', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ action: 'launch', target: target, device: dev })
      });
    }

    async function triggerEvent(action, data) {
      const dev = document.getElementById('device-select').value;
      await fetch('/api/event', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ action: action, data: data, device: dev })
      });
      fetchState();
    }

    async function toggleWatchSynthetic() {
      watchMode = !watchMode;
      const dev = document.getElementById('device-select').value;
      await fetch('/api/action', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ action: 'watch_test_mode', enabled: watchMode, device: dev })
      });
      document.getElementById('btn-watch-mode').innerText = watchMode ? "⌚ Watch Synthetic: ON" : "⌚ Watch Synthetic: OFF";
    }

    // Polling loop
    refreshDevices();
    fetchState();
    setInterval(fetchState, 1500);
  </script>
</body>
</html>
"""

class MockWebServer(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Keep terminal output clean
        pass

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        if url.path == "/" or url.path == "/index.html":
            page = HTML_PAGE.replace("%PRESETS_JSON%", json.dumps(SCENARIOS))
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(page.encode("utf-8"))
        elif url.path == "/api/status":
            self.send_json(ENGINE.get_state())
        elif url.path == "/api/devices":
            devs = AdbController.get_devices()
            self.send_json({"devices": devs})
        else:
            self.send_error(404, "Not Found")

    def do_POST(self):
        url = urllib.parse.urlparse(self.path)
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
        try:
            payload = json.loads(body)
        except Exception:
            payload = {}

        if url.path == "/api/emit":
            rec = ENGINE.emit_once()
            self.send_json({"ok": True, "record": rec})
        elif url.path == "/api/stream":
            active = payload.get("active", False)
            iv = payload.get("interval", 2.0)
            if active:
                ENGINE.start_streaming(interval=iv)
            else:
                ENGINE.stop_streaming()
            self.send_json({"ok": True, "streaming": ENGINE.is_streaming})
        elif url.path == "/api/set_params":
            p = payload.get("params", {})
            sc = payload.get("scenario", "custom")
            ENGINE.set_params(p, sc)
            self.send_json({"ok": True})
        elif url.path == "/api/set_device":
            ENGINE.selected_device = payload.get("device", "all")
            self.send_json({"ok": True})
        elif url.path == "/api/event":
            action = payload.get("action", "start")
            data = payload.get("data", {})
            dev = payload.get("device", ENGINE.selected_device)
            ok, out, err = AdbController.emit_event(dev, action, data)
            self.send_json({"ok": ok, "out": out, "err": err})
        elif url.path == "/api/action":
            action = payload.get("action")
            dev = payload.get("device", ENGINE.selected_device)
            if action == "grant_permissions":
                res = AdbController.grant_permissions(dev)
                self.send_json({"ok": True, "results": res})
            elif action == "launch":
                target = payload.get("target", "mobile")
                ok, out, err = AdbController.launch_app(dev, target)
                self.send_json({"ok": ok, "out": out, "err": err})
            elif action == "watch_test_mode":
                en = payload.get("enabled", True)
                ok, out, err = AdbController.set_watch_test_mode(dev, en)
                self.send_json({"ok": ok, "out": out, "err": err})
            else:
                self.send_error(400, "Unknown action")
        else:
            self.send_error(404, "Not Found")

    def send_json(self, data):
        res = json.dumps(data).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(res)))
        self.end_headers()
        self.wfile.write(res)


def run_server(port=DEFAULT_PORT, open_browser=True):
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", port), MockWebServer) as httpd:
        url = f"http://localhost:{port}"
        print(f"\n=======================================================")
        print(f"🚀 BioCue Sensor Dev Console running at: {url}")
        print(f"=======================================================\n")
        print("Connected Devices:")
        for d in AdbController.get_devices():
            print(f"  • {d['serial']} ({d['model']}) [{'Wear OS' if d['is_wear'] else 'Android Phone'}]")
        print("\nPress Ctrl+C to terminate the console.\n")
        if open_browser:
            webbrowser.open(url)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down console...")


def run_cli(args):
    """Direct non-interactive or terminal loop CLI."""
    scenario_key = args.scenario
    if scenario_key and scenario_key in SCENARIOS:
        p = dict(SCENARIOS[scenario_key])
    else:
        p = dict(SCENARIOS["calm"])

    if args.hr is not None: p["hr"] = args.hr
    if args.rmssd is not None: p["rmssd"] = args.rmssd
    if args.motion is not None: p["acc"] = args.motion
    if args.rep is not None: p["rep"] = args.rep
    if args.slope is not None: p["slope"] = args.slope
    if args.exercising is not None: p["exercising"] = args.exercising
    if args.offbody is not None: p["offbody"] = args.offbody

    ENGINE.set_params(p, scenario_key or "custom")
    ENGINE.selected_device = args.device or "all"

    if args.grant_permissions:
        print(f"Granting permissions to device: {ENGINE.selected_device}...")
        res = AdbController.grant_permissions(ENGINE.selected_device)
        for r in res:
            status = "✓ OK" if r["ok"] else f"✗ {r['error']}"
            print(f"  {r['permission']}: {status}")

    if args.launch:
        print(f"Launching app: {args.launch} on {ENGINE.selected_device}...")
        ok, out, err = AdbController.launch_app(ENGINE.selected_device, args.launch)
        print("Done." if ok else f"Error: {err}")

    if args.event:
        print(f"Emitting event: {args.event} (label={args.label})...")
        ok, out, err = AdbController.emit_event(ENGINE.selected_device, args.event, {"label": args.label})
        print(f"Result: {'Success' if ok else 'Failed: ' + err}")

    if args.stream:
        print(f"Streaming {scenario_key} to {ENGINE.selected_device} every {args.interval}s (Ctrl+C to stop)...")
        ENGINE.start_streaming(interval=args.interval)
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            ENGINE.stop_streaming()
            print("\nStreaming stopped.")
    else:
        # Single shot emit
        rec = ENGINE.emit_once()
        print(f"\n[EMITTED] {rec['time']} | HR: {rec['hr']} BPM | RMSSD: {rec['rmssd']} ms | Motion: {rec['acc']} g")
        print(f"Device: {rec['device']} | Status: {'✓ SUCCESS' if rec['ok'] else '✗ FAILED'}")


def main():
    parser = argparse.ArgumentParser(description="BioCue Mock Sensor Developer Console")
    parser.add_argument("--web", action="store_true", help="Launch interactive Web Server GUI")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"Web server port (default {DEFAULT_PORT})")
    parser.add_argument("--no-browser", action="store_true", help="Do not auto-open browser in web mode")

    parser.add_argument("--scenario", choices=list(SCENARIOS.keys()), help="Preset scenario to emit")
    parser.add_argument("--hr", type=float, help="Override Heart Rate BPM (40-180)")
    parser.add_argument("--rmssd", type=float, help="Override HRV RMSSD ms (5-100)")
    parser.add_argument("--motion", type=float, help="Override movement magnitude g (0.0-3.0)")
    parser.add_argument("--rep", type=float, help="Override repetitiveness (0.0-1.0)")
    parser.add_argument("--slope", type=float, help="Override HR slope (-3.0 to +3.0)")
    parser.add_argument("--exercising", action="store_true", default=None, help="Set isExercising true")
    parser.add_argument("--offbody", action="store_true", default=None, help="Set isOffBody true")

    parser.add_argument("--stream", action="store_true", help="Continuously stream sensor windows")
    parser.add_argument("--interval", type=float, default=2.0, help="Stream interval in seconds (default 2.0)")
    parser.add_argument("--device", type=str, default="all", help="Target ADB device serial (default: all)")

    parser.add_argument("--event", choices=["start", "label", "end", "sos"], help="Trigger stress event lifecycle")
    parser.add_argument("--label", type=str, default="OVERWHELMED", help="Stress label (OVERWHELMED, EXCITED, ACTIVE)")
    parser.add_argument("--grant-permissions", action="store_true", help="Grant all runtime permissions via ADB")
    parser.add_argument("--launch", choices=["mobile", "wear"], help="Launch Mobile or Wear OS app")

    args = parser.parse_args()

    # If no CLI emit arguments specified, or --web flag passed, default to Web Console
    is_cli_call = any([
        args.scenario, args.hr, args.rmssd, args.motion, args.rep, args.slope,
        args.stream, args.event, args.grant_permissions, args.launch
    ])

    if args.web or not is_cli_call:
        run_server(port=args.port, open_browser=not args.no_browser)
    else:
        run_cli(args)


if __name__ == "__main__":
    main()
