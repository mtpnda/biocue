# 18. Battery-Optimized HRV & Stress Level Profiling & Local Sync

> **Normative for:** `core-detection/.../ml/*`, `wear/.../wear/*` (Wear OS), and `ios/.../Wearer/*` (iOS Companion).  
> **Status:** Approved & Normative.

---

## 1. Problem & Objectives

1. **Autonomic Stress Profiling:** Calculate true Heart Rate Variability (HRV RMSSD in milliseconds) from optical sensor intervals and maintain a personalized, rolling resting baseline on the watch.
2. **Resource & Battery Conservation:** Smartwatches (e.g. Galaxy Watch 4 Classic, 247–361 mAh) experience severe battery exhaustion when optical PPG LEDs and BLE radios run continuously. The architecture must achieve all-day wearability (> 16 hours) through intelligent duty-cycling, hardware off-body detection, and radio burst batching.
3. **Local Sovereignty & Offline Sync:** All baseline profiles and raw vitals remain on-device (watch and phone only). Data is synced locally over Bluetooth LE (BioCue Link v1 for iPhone) or Wearable Data Layer (Android phone) with zero cloud relay of raw vitals.

---

## 2. Mathematical & Algorithmic Foundation

### 2.1 Inter-Beat Interval (IBI) & RMSSD Computation
Given heart rate readings $HR_i$ (BPM) sampled at timestamps $t_i$:
$$RR_i = \frac{60000.0}{HR_i} \text{ (ms)}$$

For a sliding window of $N$ valid consecutive beat intervals ($N \in [15, 30]$):
$$\Delta RR_i = RR_{i} - RR_{i-1}$$
$$RMSSD = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N-1} (\Delta RR_i)^2}$$

Artifact rejection: Beat intervals outside $300\text{ ms} \le RR_i \le 1500\text{ ms}$ (corresponding to 40–200 BPM) or $\Delta RR_i > 300\text{ ms}$ are marked as ectopic/noise and excluded.

### 2.2 Heart Rate Slope ($HR_{slope}$)
Computed as the linear change over a 10-second trailing window:
$$HR_{slope} = \frac{HR(t) - HR(t - 10\text{s})}{\Delta t \text{ (s)}} \text{ (bpm/s)}$$

### 2.3 Rolling Baseline Adaptation
Baseline parameters:
- `hrMean`: Resting heart rate mean ($\mu_{HR}$, default 72.0 BPM)
- `hrStd`: Resting heart rate standard deviation ($\sigma_{HR}$, default 8.0 BPM, min 2.0)
- `hrvMean`: Resting HRV RMSSD mean ($\mu_{RMSSD}$, default 42.0 ms)
- `hrvStd`: Resting HRV RMSSD standard deviation ($\sigma_{RMSSD}$, default 10.0 ms, min 2.0)

**Calibration Criteria:** Adaptation occurs *only* when the wearer is verified calm:
1. `isOffBody == false`
2. `isExercising == false`
3. Accelerometer magnitude $accMag < 0.25\text{ m/s}^2$
4. Tier == 0 (no active or recent stress event)

When a valid calibration window $w$ arrives:
$$\alpha = \frac{1}{\min(N_{calibrated} + 1, 100)}$$
$$\mu_{HR} \leftarrow (1 - \alpha) \mu_{HR} + \alpha \cdot w.hrMean$$
$$\mu_{RMSSD} \leftarrow (1 - \alpha) \mu_{RMSSD} + \alpha \cdot w.rmssd$$

---

## 3. Four-Pillar Battery & Resource Architecture

```
                          ┌────────────────────────┐
                          │ Hardware Offbody Detect│
                          │  (Sensor Type 34)      │
                          └───────────┬────────────┘
                                      │
                 ┌────────────────────┴────────────────────┐
                 │                                         │
                 ▼ (Off Wrist)                             ▼ (On Wrist)
        ┌──────────────────┐                     ┌──────────────────┐
        │   STATE: OFF_BODY│                     │ Check Activity   │
        │  • PPG OFF       │                     │ & Stress Tier    │
        │  • No Radio TX   │                     └─────────┬────────┘
        │  • Low-power wait│                               │
        └──────────────────┘                 ┌─────────────┴─────────────┐
                                             ▼ (Calm & Low Motion)       ▼ (Tier 1/2 or Motion Spike)
                                   ┌──────────────────────┐    ┌──────────────────────┐
                                   │ STATE: CALM_DUTY     │    │ STATE: ACTIVE_HIGH   │
                                   │ • 30s PPG sample     │    │ • 100% continuous PPG│
                                   │ • 90s PPG sleep      │    │ • Real-time tracking │
                                   │ • Accel FIFO guard   │    │ • Immediate radio TX │
                                   │ • 30s batched sync   │    └──────────────────────┘
                                   └──────────────────────┘
```

### Pillar 1: Hardware Off-Body Detection
- Subscribes to `Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT` (Samsung handle 0x44 / Android type 34).
- When off-wrist (`values[0] == 0.0f`):
  - Health Services PPG `MeasureCallback` is immediately unregistered.
  - Radio transmission is silenced.
  - Zero optical battery consumed while watch is charging or off-body.

### Pillar 2: Adaptive Sensor Duty-Cycling (Smart Windowing)
- In `CALM_DUTY_CYCLE` mode:
  - Collects PPG for a 30-second window to compute HR, HRV RMSSD, and update the rolling baseline.
  - Pauses PPG optical sensor registration for 90 seconds.
  - Low-power hardware accelerometer monitors movement in background; if acceleration spike $> 0.8\text{ m/s}^2$ or HR slope rises, immediately wakes PPG.
  - **Power Reduction:** Reduces continuous optical LED drain by **75%**.

### Pillar 3: Radio Transmission Batching (Modem Sleep)
- BLE GATT notifications and Data Layer writes wake the baseband modem.
- In `CALM_DUTY_CYCLE`: Sensor windows are batched and dispatched every 30 seconds rather than once per second.
- In `ACTIVE_HIGH` / Stress Spikes: Windows are transmitted immediately (0ms delay) to ensure real-time alerting.
- If phone is disconnected: Best-effort telemetry is dropped cleanly; critical events queue in `LinkOutbox` (Spec 08).

### Pillar 4: Efficient Local Storage (Zero-Spill I/O)
- Watch profiles are cached 100% in memory (`HrvProfile`).
- Disk persistence occurs via `AtomicFile` to `files/hrv_profile.json` at:
  - Service shutdown (`onDestroy`)
  - Periodic 15-minute maintenance checkpoints
- Zero disk writes in the real-time sensor loop.

---

## 4. Local Profile Storage Schema (`hrv_profile.json`)

```json
{
  "version": 1,
  "updatedAtMs": 1700000000000,
  "calibratedSamples": 48,
  "baseline": {
    "hrMean": 71.4,
    "hrStd": 7.8,
    "hrvMean": 44.2,
    "hrvStd": 9.5
  },
  "currentMetrics": {
    "latestHr": 69.5,
    "latestRmssd": 46.0,
    "latestStressPercent": 24,
    "tier": 0
  },
  "hourlyRollup": [
    {
      "hourOfDay": 13,
      "avgHr": 72.1,
      "avgRmssd": 43.8,
      "avgStressPercent": 22
    }
  ]
}
```

---

## 5. Verification & Quality Gates

1. **Unit Tests (`core-detection`):**
   - Verify RMSSD calculation with known beat vectors.
   - Verify baseline EMA adaptation when calm vs rejection when moving or off-body.
   - Verify stress percentage mapping bounds $[0, 100]$.
2. **Duty-Cycle Tests (`wear`):**
   - Verify transitions between `OFF_BODY`, `CALM_DUTY`, and `ACTIVE_HIGH`.
   - Verify sensor unregistration when off-body.
3. **End-to-End Parity:**
   - Verify sync over BLE to iOS `WearerService` and over Data Layer to Android Companion.
