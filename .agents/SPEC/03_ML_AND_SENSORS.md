# 03. Machine Learning & Sensors

## Sensor Collection (Wear OS)
- **Heart Rate (PPG):** Collected via `androidx.health.services.client`.
- **Accelerometer:** Collected via raw `SensorManager` to detect pacing/stimming.
- **Synthetic Test Mode:** Since Wear OS emulators cannot generate realistic HR fluctuations, the app includes a "Test Mode" toggle on the watch. When active, it generates synthetic `SensorWindow` payloads every 5 seconds to test the Phone's ML engine.

## The Fusion Score Algorithm (Mobile)
A Logistic Regression model (or heuristic rules for v1) fuses Heart Rate, HRV (RMSSD), and Accelerometer data into a single continuous `score`.

### Tier Thresholds
The `MainViewModel` evaluates the score against specific bounds:
- **Tier 0 (Score < 1.5):** Calm / Baseline.
- **Tier 1 (1.5 <= Score <= 2.5):** Rising Stress. Triggers a warning.
- **Tier 2 (Score > 2.5):** Meltdown. Triggers the SOS GPS Ping.

## The SOS GPS Ping Architecture
To preserve battery, the GPS is 100% inactive by default.
1. Phone's `MainViewModel` hits Tier 2.
2. Phone sends a `MessageClient` payload to `/sos_gps_request`.
3. Watch intercepts the message, grabs exactly *one* high-accuracy GPS fix via `FusedLocationProviderClient`.
4. Watch returns the coordinates to the Phone via `/sos_location`.
