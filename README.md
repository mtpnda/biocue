# BioCue — Bio-Signal Cueing & Sensory Overload Protection

> Multi-device bio-signal cueing platform combining **Wear OS (Smartwatch)** and an **Android Companion App** with real-time ML meltdown prediction, privacy-first local storage, and caregiver circle synchronization.

---

## 1. Project Directory Structure

```
biocue/
├── android/
│   ├── mobile/            # Android Companion App (Phone & Tablet) — Jetpack Compose, ML Scoring, Room DB, Firebase Sync
│   ├── wear/              # Wear OS Smartwatch App (Wear OS 3+) — Health Services PPG sensor, Tiles, Zero-Anxiety Anchor UI
│   ├── core-detection/    # Shared Kotlin Multiplatform/JVM library (DataLayer schemas, Room DB entities, link protocols)
│   ├── mock-emitter/      # Standalone Developer Mock Sensor Emitter (Debug-only biometric injection app)
│   ├── build.gradle.kts   # Root multi-module build script
│   └── settings.gradle.kts# Module registry (:mobile, :wear, :core-detection, :mock-emitter)
├── tools/
│   └── mock_sensor_console.py # Zero-dependency Python Web Server & CLI for ADB biometric simulation
├── .agents/
│   ├── AGENTS.md          # Multi-agent operating guidelines and release protocol
│   └── specs/             # Canonical system specifications (PRDs, ML math, contracts, DLS)
├── tests/
│   ├── test_release_automation.py  # Release hygiene & fastlane test suite
│   └── test_playstore_reviewer.py  # Google Play Store reviewer sequential UI test
├── fastlane/              # Multiplatform Fastlane deployment automation
└── Makefile               # Single-pane command controller
```

---

## 2. Prerequisites

- **macOS** or **Linux**
- **Android SDK** with `platform-tools` (`adb`) in `PATH`
- **JDK 17** or **JDK 21**
- **Python 3.9+** (uses 100% standard library — zero `pip` dependencies required)
- *(Optional for Play Store Publishing)*: Ruby, Bundler, Fastlane

---

## 3. Quick Start & Installation

### 3.1. Install to Devices / Emulators

With your phone/tablet and/or Wear OS emulator attached via ADB:

```bash
# Install Android Companion App to connected phone/tablet/emulator
make install-android

# Install Wear OS App to connected smartwatch/emulator
make install-wear

# Install Standalone Mock Sensor Emitter App (Debug only)
make install-mock
```

### 3.2. Automatic ADB Permission Granting

To grant all required runtime permissions (`BODY_SENSORS`, `ACTIVITY_RECOGNITION`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`) to all connected devices in one step:

```bash
make mock-cli ARGS="--grant-permissions"
```

### 3.3. Autonomous Physical Watch Wi-Fi Connection (Minimum Intervention)

For modern Wear OS watches (such as Samsung Galaxy Watch 4 Classic), Bluetooth debugging is deprecated by Samsung/Google. BioCue includes an autonomous discovery and connection tool ([`tools/watch_auto_connect.py`](file:///tools/watch_auto_connect.py)) that eliminates manual port renewal and IP typing:

#### 1. One-Time Pairing (Watch Lifetime)
With watch on same Wi-Fi as Mac, go to **Settings** &rarr; **Developer options** &rarr; **Wireless debugging** &rarr; **Pair new device**:
```bash
make pair-watch IP=<watch_ip> PORT=<pairing_port> CODE=<6_digit_code>
```
*(This saves your watch IP and permanently pairs your Mac's `~/.android/adbkey`)*

#### 2. Daily Automatic Connection (1 Command)
```bash
make connect-watch
```
**What this does automatically:**
1. Verifies if the watch is already connected.
2. Auto-discovers the watch's dynamic port over local Wi-Fi via mDNS (`adb mdns services`).
3. If mDNS is filtered by your router, it uses your cached IP and performs a parallel socket probe across Android's active port range in under 1.5 seconds.
4. Connects via `adb connect` and automatically promotes to fixed port 5555 (`adb tcpip 5555`) for instant future connects.
5. To disconnect when finished: `make disconnect-watch`.

---

## 4. Developer Mock Sensor Simulation

Because Wear OS emulators cannot produce physical heart rate fluctuations or accelerometer motion, BioCue provides **three independent ways** to simulate and test physiological vitals in development mode without interfering with user-facing screens:

### Option A: Interactive Web Console (Recommended)
Launches a responsive Web GUI on `http://localhost:8080` in your workstation browser:

```bash
make mock-web
```

**Features:**
- **External & Off-Screen:** Operates in your workstation browser so both the Watch and Phone emulators remain completely visible side-by-side.
- **Device Selector:** Automatically detects attached ADB devices (`adb devices -l`).
- **One-Click Scenarios:** Instantly trigger *Calm/Baseline*, *Mild Stress*, *Overwhelmed Spike*, *Exercise*, or *Off-Wrist*.
- **Live Real-Time Sliders:** Adjust Heart Rate (40–180 BPM), HRV RMSSD (5–100 ms), Motion (0–3g), Stimming / Repetitiveness (0–1), and HR Slope.
- **Animated Canvas:** Live streaming ECG / biometric waveform canvas.
- **Event Lifecycle Controls:** Trigger manual stress events, set labels (`OVERWHELMED`, `ACTIVE`), end events, and inject SOS GPS fixes.
- **Watch Test Mode:** Toggle internal synthetic vitals generation on Wear OS.

### Option B: Terminal CLI Mode
For automated test scripts, headless environments, or terminal-based workflows:

```bash
# Inject an Overwhelmed meltdown spike
make mock-cli ARGS="--scenario overwhelmed"

# Continuously stream sensor windows every 1.5 seconds
make mock-cli ARGS="--stream --interval 1.5"

# Target a specific emulator or device serial
make mock-cli ARGS="--device emulator-5554 --scenario mild_stress"

# Inject custom biometrics
make mock-cli ARGS="--hr 142 --rmssd 14.2 --motion 0.65"

# Trigger a stress event and end it
make mock-cli ARGS="--event start --label OVERWHELMED"
make mock-cli ARGS="--event end"
```

### Option C: Standalone Android App (`:mock-emitter`)
If you want an on-device app to control simulation when away from your laptop:
```bash
make install-mock
```
Launch **BioCue Mock Emitter** from the app drawer on the test phone/tablet.

> [!NOTE]
> All mock simulation receivers are gated behind `BuildConfig.DEBUG`. Release bundles (`:mobile:bundleRelease`, `:wear:bundleRelease`) do not include `:mock-emitter` and ignore spoofed telemetry.

---

## 5. Automated Tests & Quality Gates

Run all quality gates directly via the Makefile controller:

```bash
# Run release automation verification suite
make test-release

# Run Google Play Store sequential reviewer automation test
make test-reviewer

# Run Android & Wear OS unit test suite
make test-android
```

---

## 6. Specifications & Documentation

Authoritative system architecture, data contracts, and design tokens are documented in [`.agents/specs/`](file:///.agents/specs/):

- [**`01_PRODUCT_REQUIREMENTS.md`**](file:///.agents/specs/01_PRODUCT_REQUIREMENTS.md): Core PRD & 2-in-1 account model.
- [**`02_ARCHITECTURE.md`**](file:///.agents/specs/02_ARCHITECTURE.md): Modular system architecture & Data Layer protocol.
- [**`03_ML_AND_SENSORS.md`**](file:///.agents/specs/03_ML_AND_SENSORS.md): Fusion Score scoring algorithm & GPS ping architecture.
- [**`04_USER_INTERFACE.md`**](file:///.agents/specs/04_USER_INTERFACE.md): Liquid Glass UX guidelines & Anchor screen.
- [**`13_DESIGN_LANGUAGE_SYSTEM.md`**](file:///.agents/specs/13_DESIGN_LANGUAGE_SYSTEM.md): Tokens for Android Compose, iOS SwiftUI, and Web.
- [**`15_MOCK_SENSOR_EMITTER.md`**](file:///.agents/specs/15_MOCK_SENSOR_EMITTER.md): Comprehensive mock emitter data contracts, broadcast intents, and architecture.

---

## 7. Multi-Machine Setup & Centralized Secrets

BioCue strictly follows a **zero-secrets-in-repo** standard. Private keys, signing certificates, and cloud service account credentials are stored in your user profile under `~/.secrets/` so this repository can be cloned and worked on across multiple machines (e.g. Home Mac and Work Mac) without committing or scattering sensitive credentials.

### 7.1. Global Secrets Hierarchy (`~/.secrets/`)

All keys reside globally on your machine:
```
~/.secrets/
├── android/
│   ├── biocue-release-key.jks        # BioCue release upload key
│   ├── play-account.json             # Google Play Console publishing credentials
│   └── biocue/
│       └── google-services.json      # Firebase configuration for biocueapp
└── apple/                            # App Store Connect & developer certs
```

### 7.2. Syncing to a New Machine (e.g., Work Mac)

When onboarding a second machine:

1. **Copy your global folders once** from your primary machine (via AirDrop, secure USB, or local network):
   * `~/.secrets/` (production signing keys & service accounts)
   * `~/.android/debug.keystore` (shares the same debug SHA-1/SHA-256 for Firebase Google Sign-In)
   * `~/.gemini/` (Antigravity agents and central skill plugins)

2. **Clone the repository:**
   ```bash
   git clone git@github.com:mtpnda/biocue.git ~/Workspace/biocue
   cd ~/Workspace/biocue
   ```

3. **Link secrets & verify toolchain:**
   ```bash
   make setup-secrets
   make doctor
   ```

`make setup-secrets` creates symlinks from `~/.secrets/` directly into the project directory, and `make doctor` verifies that all build tools and credentials are in place. You can now build, test, and release seamlessly on both machines.
