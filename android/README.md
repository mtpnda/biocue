# BioCue for Android (Companion & Wear OS)

Multi-device Android application suite for sensory overload meltdown prediction, bio-signal cueing, and caregiver circle synchronization.

## Project Structure

```
android/
├── mobile/           # Android Companion App (Phone & Tablet) — Jetpack Compose, ML Scoring, Room DB, Firebase Sync
├── wear/             # Wear OS Smartwatch App (Wear OS 3+) — Health Services PPG sensor, Tiles, Zero-Anxiety Anchor UI
├── core-detection/   # Shared Kotlin Multiplatform/JVM library (DataLayer schemas, Room DB entities, link protocols)
├── mock-emitter/     # Standalone Developer Mock Sensor Emitter (Debug-only biometric injection app)
├── gradle/           # Gradle 9.5 wrapper and Version Catalog (libs.versions.toml)
├── build.gradle.kts  # Multi-module root build configuration
├── settings.gradle.kts # Includes :mobile, :wear, :core-detection, :mock-emitter
└── local.properties  # Local Android SDK and keystore path configuration
```

## Module Overview

1. **`:mobile` (Companion Phone & Tablet)**:
   - Wearer and Caregiver dual-role dashboard.
   - Continuous physiological Fusion Score evaluation (Heart Rate + HRV + Accelerometer).
   - Local Room SQLite caching with privacy bounds (raw biometrics stay on-device).
   - Event-driven Cloud Firestore synchronization for Caregiver Circles.
   - 6-digit cryptographic Pairing Code generation.

2. **`:wear` (Wear OS 3+ Smartwatch)**:
   - Zero-anxiety Anchor Screen and one-tap emoji check-ins.
   - Battery-preserving passive PPG sensor collection via `androidx.health.services.client`.
   - On-demand SOS GPS responder via `FusedLocationProviderClient`.
   - Quick-access Wear OS Tiles and complications (`MainTileService`).

3. **`:core-detection` (Shared Architecture)**:
   - Centralized contract schemas for Google Play Services Wearable Data Layer API (`/sensor_window`, `/sos_location`, `/sos_gps_request`).
   - Shared entities (`StressEventEntity`, `SensorWindowEntity`).
   - Detection pipeline and cooldown logic.

4. **`:mock-emitter` (Developer Mock Sensor Emitter — Debug Only)**:
   - Standalone Jetpack Compose app allowing developer simulation of real-time sensor streams without physical hardware.
   - Dispatches ADB-compatible broadcast intents (`MOCK_SENSOR_DATA`, `MOCK_MANUAL_EVENT`, etc.).
   - Strictly isolated from production releases (never bundled into `:mobile` or `:wear`).

## Building & Testing from CLI

```bash
# Compile all release binaries (production builds exclude :mock-emitter)
./gradlew bundleRelease assembleRelease

# Run unit tests across all modules (including :mock-emitter)
./gradlew :mobile:testDebugUnitTest :wear:testDebugUnitTest :core-detection:test :mock-emitter:testDebugUnitTest

# Build & install the mock emitter to an attached Android phone/tablet
./gradlew :mock-emitter:installDebug

# Publish to Google Play Store (Internal Track)
./gradlew publishBundle publishListing
```

## Developer Simulation & Mock Tooling

From the repository root, you can also control simulation externally via the Python host tools:

```bash
# Launch interactive Web Console (Mac browser on http://localhost:8080)
make mock-web

# Run one-off CLI simulation command
make mock-cli ARGS="--scenario overwhelmed"

# Grant all required watch permissions via ADB
make mock-cli ARGS="--grant-permissions"
```
