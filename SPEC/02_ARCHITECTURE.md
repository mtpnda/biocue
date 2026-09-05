# 02. System Architecture

The project is split into three tightly-coupled Gradle modules to separate concerns between the watch, the phone, and their shared contracts.

## Module 1: `:mobile` (The Caregiver Dashboard)
- **Role:** ML Scoring Engine, UI display, and Data Layer listener.
- **Tech Stack:** Jetpack Compose (Material 3), MVVM, `StateFlow`.
- **Key Components:**
  - `MainViewModel`: Calculates the Fusion Score continuously based on incoming telemetry.
  - `WatchDataReceiverService`: Intercepts `/sensor_window` and `/sos_location` payloads.

## Module 2: `:wear` (The Wearer Interface)
- **Role:** Sensor collection, SOS GPS Responder, and minimal UI.
- **Tech Stack:** Wear Compose, Foreground Services (Type `health`), `FusedLocationProviderClient`.
- **Key Components:**
  - `SensorCollectionService`: Runs continuously to fetch Heart Rate via Samsung Health SDK.
  - `PingResponderService`: Listens for `/sos_gps_request` and triggers the `LocationTracker`.

## Module 3: `:core-detection` (Shared Resources)
- **Role:** Enforces API contracts and local persistence across both apps.
- **Key Components:**
  - `DataLayerSchema`: Centralized constants for all Wearable API paths and keys.
  - `AppDatabase`: Shared Room Database entities (e.g., `StressEventEntity`).

## The Data Layer API Contract
All cross-device communication occurs via Google Play Services Wearable API.

| Sender | Receiver | Path | Payload (DataMap keys) | Purpose |
|---|---|---|---|---|
| Wear | Phone | `/sensor_window` | `timestamp`, `hr_mean`, `rmssd`, `acc_mag` | Streams raw physiological telemetry. |
| Phone | Wear | `/sos_gps_request` | *(Empty ByteArray Message)* | Phone commands the Watch to fetch GPS. |
| Wear | Phone | `/sos_location` | `timestamp`, `latitude`, `longitude` | Watch returns the SOS GPS coordinates. |
| Wear | Phone | `/event_start` | `timestamp`, `trigger` | Wearer tapped "I need help". |
