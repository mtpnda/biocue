# 15. Mock Sensor Emitter System (Developer & QA Tooling)
> **Canonical Directory:** `.agents/specs/15_MOCK_SENSOR_EMITTER.md`  
> **Status:** Authoritative  
> **Target Subsystems:** `tools/mock_sensor_console.py`, `android/mock-emitter/`, `android/mobile/`, `android/wear/`

---

## 1. Purpose & Motivation

During development, manual QA, and automated UI testing, relying on physical Wear OS hardware or standard Android emulators presents significant challenges:
1. **Physical Impossibility:** Android Wear OS emulators do not possess physical PPG sensors and cannot naturally generate realistic fluctuating heart rate, HRV (RMSSD), motion accelerometer spikes, or stimming patterns.
2. **Screen Real Estate & Friction:** If test mock controls are embedded directly inside the primary watch or mobile screens, they pollute production UI flows, occlude screen elements, and risk leaking into release builds.
3. **Multi-Device Synchronization:** Developers must be able to independently control physiological vitals from an external controller (Mac browser or terminal CLI) while observing the Watch and Phone emulators side-by-side in real time.

The **Mock Sensor Emitter System** solves this by providing external developer interfaces that inject realistic synthetic telemetry directly into BioCue's detection pipelines via ADB and internal broadcast channels.

---

## 2. System Architecture

```
                                  +---------------------------------------+
                                  |   Developer Mac / Workstation         |
                                  |   (Local Web Console / CLI / Make)    |
                                  +---------------------------------------+
                                         |                         |
               make mock-web             |                         |  make mock-cli
               (http://localhost:8080)   |                         |  (Terminal CLI)
                                         v                         v
                                  +---------------------------------------+
                                  |    tools/mock_sensor_console.py       |
                                  |    - Device discovery (adb devices)   |
                                  |    - 1-click permission granter       |
                                  |    - Scenario engine / Live sliders   |
                                  +---------------------------------------+
                                                     |
                                            ADB Broadcast Intents
                                                     |
                     +-------------------------------+-------------------------------+
                     |                                                               |
                     v                                                               v
      +-----------------------------+                                 +-----------------------------+
      |  Android Companion (:mobile)|                                 |      Wear OS (:wear)        |
      |  WatchDataReceiverService   |                                 |  SensorCollectionService    |
      |                             |                                 |                             |
      |  - MOCK_SENSOR_DATA         |                                 |  - SET_TEST_MODE            |
      |  - MOCK_MANUAL_EVENT        |                                 |  - Synthetic Vitals Loop    |
      |  - MOCK_EVENT_LABEL         |                                 |                             |
      |  - MOCK_EVENT_END           |                                 |                             |
      |  - MOCK_SOS_LOCATION        |                                 |                             |
      +-----------------------------+                                 +-----------------------------+
```

In addition to the host console, the system provides an optional in-project debug Android app:
- **`:mock-emitter` (`android/mock-emitter`)**: Standalone Jetpack Compose developer app runnable on an Android phone or tablet when testing away from a workstation.

---

## 3. Data Contracts & Intent Interfaces

### 3.1. Telemetry Broadcast Contract (`com.mtpnda.biocue.MOCK_SENSOR_DATA`)

Injected into `:mobile` to simulate a 30-second physiological sensor window:

| Extra Key | Type | Range | Description |
| :--- | :--- | :--- | :--- |
| `hr` | `Float` / `Double` | `40.0` - `180.0` | Heart Rate in Beats Per Minute (BPM) |
| `rmssd` | `Float` / `Double` | `5.0` - `100.0` | HRV RMSSD in milliseconds (lower indicates higher sympathetic stress) |
| `magnitude` | `Float` / `Double` | `0.0` - `3.0` | Acceleration magnitude ($g$) |
| `repetitiveness` | `Float` / `Double` | `0.0` - `1.0` | Autocorrelation coefficient indicating repetitive motion (e.g. stimming) |
| `hr_slope` | `Float` / `Double` | `-3.0` - `+3.0` | Rate of HR change (BPM / sec) |
| `is_exercising` | `Boolean` | `true` / `false` | Physical exercise flag (suppresses false-positive stress alerts) |
| `is_off_body` | `Boolean` | `true` / `false` | Wearer off-body detector |

*Compatibility Note:* ADB `am broadcast --ef` dispatches extras as `Float`. The receiver unwraps both `Float` and `Double` cleanly to avoid unboxing failures.

### 3.2. Event Lifecycle Broadcast Contracts

| Intent Action | Extras | Description |
| :--- | :--- | :--- |
| `com.mtpnda.biocue.MOCK_MANUAL_EVENT` | `--es label <LABEL>`<br>`--ei duration_minutes <MIN>` | Triggers a stress event (e.g. `OVERWHELMED`, `EXCITED`, `ACTIVE`). |
| `com.mtpnda.biocue.MOCK_EVENT_LABEL` | `--es label <LABEL>` | Updates the active stress event label in Room DB and UI. |
| `com.mtpnda.biocue.MOCK_EVENT_END` | *(None)* | Marks the active stress event as resolved / ended. |
| `com.mtpnda.biocue.MOCK_SOS_LOCATION` | `--ef latitude <LAT>`<br>`--ef longitude <LNG>`<br>`--ef accuracy <ACC>` | Mocks an incoming emergency GPS location ping from the watch. |
| `com.mtpnda.biocue.wear.SET_TEST_MODE` | `--ez enabled <true\|false>` | Broadcasted to `:wear` to toggle internal synthetic vitals generation. |

---

## 4. Built-in Preset Scenarios

| Scenario | HR (BPM) | RMSSD (ms) | Motion ($g$) | Repetitiveness | HR Slope | Clinical Outcome |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Calm / Baseline** | `68.0` | `55.0` | `0.05` | `0.00` | `0.0` | Tier 0 (Calm). Zero notifications. |
| **Mild Stress (Tier 1)** | `98.0` | `28.0` | `0.15` | `0.20` | `+0.8` | Tier 1 (Pre-Meltdown Warning). Early cueing initiated. |
| **Overwhelmed Spike (Tier 2)** | `132.0` | `12.0` | `0.50` | `0.85` | `+2.4` | Tier 2 (Meltdown). Triggers anchor cueing and SOS GPS Ping. |
| **Physical Exercise** | `145.0` | `22.0` | `1.80` | `0.10` | `+1.5` | Filtered out. Exercise suppression prevents false alerts. |
| **Off-Body** | `0.0` | `0.0` | `0.00` | `0.00` | `0.0` | Watch off-wrist. Pipeline pauses scoring. |

---

## 5. Security & Production Release Isolation

1. **`BuildConfig.DEBUG` Isolation:**
   - In `WatchDataReceiverService.kt`, dynamic broadcast receivers for mock actions are registered using `RECEIVER_EXPORTED` **strictly when `BuildConfig.DEBUG` is true**.
   - In release builds, receivers are registered with `RECEIVER_NOT_EXPORTED` or omitted, preventing external apps or commands from spoofing biometrics.
2. **Modular Independence:**
   - The `:mock-emitter` module (`android/mock-emitter`) is an independent application module (`com.android.application`).
   - It is **never** added as an `implementation` dependency to `:mobile` or `:wear`.
   - Release bundles (`:mobile:bundleRelease`, `:wear:bundleRelease`) contain zero mock emitter code or assets.
3. **No Root Litter & Zero Symlinks:**
   - All host tooling (`tools/mock_sensor_console.py`) is contained within `tools/`.
   - Zero symlinks (`find . -type l` remains 0).

---

## 6. Verification & Automated Quality Gates

The mock sensor system is verified through automated test suites:
- **Unit Testing:** `android/mock-emitter/src/test/java/com/mtpnda/biocue/mockemitter/MockEmitterEngineTest.kt` verifies scenario parameter bounds, custom override math, and broadcast intent creation.
- **Automated Verification:** `make test-release` asserts clean separation and buildability without build configuration leaks.
