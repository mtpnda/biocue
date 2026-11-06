# 03. Machine Learning & Sensors

## Sensor Collection (Wear OS)
- **Heart Rate (PPG):** Collected via `androidx.health.services.client`.
- **Accelerometer:** Collected via raw `SensorManager` to detect pacing/stimming.
- **Synthetic Test Mode:** Since Wear OS emulators cannot generate realistic HR fluctuations, the app includes a "Test Mode" toggle on the watch. When active, it generates synthetic `SensorWindow` payloads every 5 seconds to test the Phone's ML engine. **Test Mode is off by default and is available in debug builds only**, so a release build can never feed invented readings into a system that alerts caregivers.

## The Fusion Score Algorithm (Mobile)
A Logistic Regression model (or heuristic rules for v1) fuses Heart Rate, HRV (RMSSD), and Accelerometer data into a single continuous `score`.

### Tier Thresholds
`DetectionPipeline` (core module; used by `WatchDataReceiverService` on Android and `WearerService` on iOS) evaluates the score against specific bounds:
- **Tier 0 (Score < 1.5):** Calm / Baseline.
- **Tier 1 (1.5 <= Score <= 2.5):** Rising Stress. Triggers a warning.
- **Tier 2 (Score > 2.5):** Meltdown. Triggers the SOS GPS Ping.

## The SOS GPS Ping Architecture
To preserve battery, the GPS is 100% inactive by default.
1. Phone's detection pipeline reaches Tier 2.
2. Phone sends a `MessageClient` payload to `/sos_gps_request`.
3. Watch intercepts the message, grabs exactly *one* high-accuracy GPS fix via `FusedLocationProviderClient`.
4. Watch returns the coordinates to the Phone via `/sos_location`.

---

## Amendments (merge, approved)

* *Test Mode:* now explicitly off by default and debug-only (the code previously enabled it at start). Can be toggled externally via `tools/mock_sensor_console.py` (`SET_TEST_MODE`).
* *Class names:* `MainViewModel` no longer exists; replaced by `DetectionPipeline` / `WatchDataReceiverService` / `WearerService`.
* *Mock Emitter Tooling:* See [**`15_MOCK_SENSOR_EMITTER.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/15_MOCK_SENSOR_EMITTER.md) for full external Web Console, CLI, and standalone emitter module contracts.
