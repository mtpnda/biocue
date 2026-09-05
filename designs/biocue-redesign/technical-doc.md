# BioCue Technical Documentation

This document covers the technical setup, pairing, testing, and deployment procedures for the BioCue application suite (Mobile and Wear OS).

## 1. Emulator Pairing (Android Studio)

When developing and testing BioCue locally, you can use Android Studio to pair a Wear OS emulator with a Phone emulator. Android Studio natively supports bridging the connection between these emulators, allowing them to communicate via the Wearable DataLayer API just like physical devices.

**Steps to Pair Emulators:**
1. Open the **Device Manager** in Android Studio (located on the right-hand sidebar).
2. Start both your **Phone Emulator** (e.g., Pixel 7) and your **Wear OS Emulator**.
3. In the Device Manager list, click the three dots menu (**⋮**) next to your Wear OS Emulator.
4. Select **"Pair device"**.
5. A wizard will open. Select your running Phone emulator from the list and click **Pair**.
6. Android Studio will automatically forward the TCP ports so the two emulators can discover each other as if they were physically connected via Bluetooth.

*Note: You must have the Wear OS companion app installed on the phone emulator to complete the software-side pairing. Or you can use the generic Wear OS by Google app.*

## 2. Running on Real Devices

Running BioCue on physical hardware is necessary for actual sensor data collection (Heart Rate, Accelerometer) and Bluetooth communication testing.

### Prerequisites
1. **Application IDs & Signatures**: Both the Phone (`:mobile`) and Watch (`:wear`) modules **must** have the exact same `applicationId` (e.g., `com.mtpnda.biocue`). 
2. **Key Store Signing**: Both modules **must** be signed with the same keystore. When running from Android Studio via USB debugging, the `debug.keystore` is automatically applied to both, satisfying this requirement.
3. **Physical Pairing**: The Wear OS watch must be actively paired to the Android phone via Bluetooth using the official Wear OS app.

### Deployment
1. Connect the Phone to your computer via USB (or wireless debugging) and click **Run 'mobile'**.
2. Connect the Watch to your computer via Wi-Fi debugging or USB and click **Run 'wear'**.
3. Launch the app on both devices. Grant the necessary Body Sensors and Notifications permissions when prompted.

## 3. Data Transfer & Event Lifecycle

BioCue relies on the Google Play Services **Wearable DataLayer API** to sync data.

- **Centralized Schema**: All communication paths and keys are defined centrally in `DataLayerSchema.kt` (inside the `:core-detection` module) to guarantee type safety and path alignment.
- **Sensor Stream**: The watch sends `SensorWindow` payloads to the `/sensor_window` path.
- **Event Lifecycle**:
  - `/event_start`: Fired when the user taps the watch face to initiate an event.
  - `/event_label`: Fired when the user selects a context label (e.g., "Overwhelmed").
  - `/event_end`: Fired when the user successfully concludes the event.

## 4. Testing Modes & Capabilities

### Synthetic "Test Mode" (Wear OS)
Because the Wear OS Emulator cannot generate realistic, continuous biometric fluctuations, BioCue includes a **Test Mode**.
- **How to enable**: On the Watch app's anchor screen, tap the `Test Mode: OFF` badge.
- **What it does**: The `SensorCollectionService` spins up a coroutine loop that generates synthetic `SensorWindow` payloads (with randomized Heart Rate and HRV patterns) and transmits them to the phone every 5 seconds over the real DataLayer.
- **Purpose**: This allows you to verify that the Phone's ML `ScoringEngine` successfully receives data, computes Fusion Scores, and triggers Tier 1/Tier 2 alerts without wearing a physical watch.

### Automated End-to-End UI Testing
BioCue includes UI integration tests utilizing the `createComposeRule()` test framework and an in-memory Room database.

**Location**: `mobile/src/androidTest/java/com/mtpnda/biocue/EventLifecycleE2ETest.kt`

**Scenarios Covered**:
1. **Orphaned Auto-Events**: Injects an `AUTO` generated event (simulating an ML Spike detection) into the database, launches the Caregiver Dashboard, and asserts that it appears in the "Pattern Review" card. It then simulates a Caregiver tap to resolve it and verifies it moves to the Journal.
2. **Manual Events**: Simulates a wearer initiating and resolving an event on the watch, verifying that it appears immediately in the Caregiver's Journal.

To run the tests:
```bash
./gradlew :mobile:connectedAndroidTest
```
