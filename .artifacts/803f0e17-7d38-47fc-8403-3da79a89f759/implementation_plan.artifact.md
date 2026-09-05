# Implementation Plan - AVD Cleanup and Pairing Setup

Optimize the Android Virtual Device (AVD) environment by keeping only the necessary devices for BioCue development and setting up the pairing bridge.

## User Review Required

> [!CAUTION]
> This action will **permanently delete** the following AVDs:
> 1. `Nexus_7_API_33`
> 2. `Pixel_C_API_33`
>
> We will keep `Pixel_3a_API_33_arm64-v8a` (Phone) and `Wear_OS_Large_Round` (Watch) as the primary pairing set.

## Proposed Changes

### AVD Cleanup

#### [DELETE] Nexus 7 AVD
- Remove `~/.android/avd/Nexus_7_API_33.avd` and `.ini`.

#### [DELETE] Pixel C AVD
- Remove `~/.android/avd/Pixel_C_API_33.avd` and `.ini`.

### Pairing Configuration

#### [SET] ADB Port Forwarding
- Configure a persistent bridge between the phone and watch emulators using:
  `adb forward tcp:5601 tcp:5601`
- This allows the Wear OS companion app on the Pixel 3a to "see" the Wear OS Large Round emulator.

## Verification Plan

### Manual Verification
1. List remaining AVDs to ensure only the Pixel 3a and Wear OS device remain.
2. Verify port forwarding status with `adb forward --list`.
3. Provide instructions to the user to complete the pairing in the Wear OS app.
