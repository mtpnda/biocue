# 02. System Architecture

The project is split into three tightly-coupled Gradle modules to separate concerns between the watch, the phone, and their shared contracts.

## Module 1: `:mobile` (The Phone App)
- **Role:** Dashboard for both Wearer and Caregiver roles, ML Scoring Engine, Firestore sync layer.
- **Tech Stack:** Jetpack Compose (Material 3), MVVM, `StateFlow`, Firebase Auth + Firestore.
- **Key Components:**
  - `AppViewModel`: Manages global app state (Auth, Role, Pairing Flow).
  - `WearerDashboardViewModel`: Calculates the Fusion Score continuously based on incoming telemetry, manages local Room DB, and syncs derived data to Firestore.
  - `CaregiverDashboardViewModel`: Read-only listener watching Firestore snapshots to present the wearer's live status.
  - `WatchDataReceiverService`: Intercepts `/sensor_window` and `/sos_location` payloads from the watch.
  - `FirestoreRepository`: Manages user profiles, pairing, and data sync between wearer and caregiver accounts.
  - `RoleSelectionScreen`: First-time role selection (WEARER or CAREGIVER).
  - `PairingScreen`: 6-digit code generation (wearer) and code entry (caregiver) for account linking.

## Module 2: `:wear` (The Wearer Watch Interface)
- **Role:** Sensor collection, SOS GPS Responder, and minimal UI.
- **Tech Stack:** Wear Compose, Foreground Services (Type `health`), `FusedLocationProviderClient`.
- **Key Components:**
  - `SensorCollectionService`: Runs continuously to fetch Heart Rate via Samsung Health SDK.
  - `PingResponderService`: Listens for `/sos_gps_request` and triggers the `LocationTracker`.

## Module 3: `:core-detection` (Shared Resources)
- **Role:** Enforces API contracts and local persistence across both apps.
- **Key Components:**
  - `DataLayerSchema`: Centralized constants for all Wearable API paths and keys.
  - `AppDatabase`: Shared Room Database entities (e.g., `StressEventEntity`, `SensorWindowEntity`).

## The Data Layer API Contract
All watch↔phone communication occurs via Google Play Services Wearable API (local Bluetooth/Wi-Fi).

| Sender | Receiver | Path | Payload (DataMap keys) | Purpose & Frequency |
|---|---|---|---|---|
| Wear | Phone | `/sensor_window` | `timestamp`, `hr_mean`, `rmssd`, `acc_mag` | Streams raw physiological telemetry. **Frequency: Every 2 seconds.** |
| Phone | Wear | `/sos_gps_request` | *(Empty ByteArray Message)* | Phone commands the Watch to fetch GPS. **Frequency: Only during Tier 2 event.** |
| Wear | Phone | `/sos_location` | `timestamp`, `latitude`, `longitude` | Watch returns the SOS GPS coordinates. **Frequency: Once per Tier 2 event.** |
| Wear | Phone | `/event_start` | `timestamp`, `trigger` | Wearer tapped "I need help". **Frequency: Event-driven.** |

## Multi-Device Sync Architecture

```mermaid
flowchart LR
    subgraph WearerDevice ["Wearer's Devices"]
        Watch["⌚ Watch<br/>(Sensors)"]
        WPhone["📱 Wearer Phone<br/>(Room DB + ML)"]
    end
    
    subgraph Cloud ["Firebase"]
        FS["☁️ Firestore<br/>(events only)"]
    end
    
    subgraph CaregiverDevice ["Caregiver's Device"]
        CPhone["📱 Caregiver Phone<br/>(read-only)"]
    end
    
    Watch -->|"Wearable DataLayer<br/>Bluetooth/Wi-Fi"| WPhone
    WPhone -->|"Sync Events<br/>(Meltdown/Journal)"| FS
    FS -->|"Snapshot Listener<br/>real-time"| CPhone
```

### What syncs to Firestore (and what doesn't)
| Data | Syncs? | Frequency / Volume | Reason |
|---|---|---|---|
| Stress events (start, end, label) | ✅ Yes | **Event-driven** (~3-10 writes/day) | Caregiver needs journal + pattern review |
| Current status (stress %, tier, activity) | ❌ No | **Never** (0 writes) | Removed to save cloud cost/battery. Caregiver only needs alerts. |
| Raw sensor windows (HR, HRV, accel) | ❌ No | **Never** (0 writes) | Privacy: raw biometrics stay on-device |
| GPS coordinates | ❌ No | **Never** (0 writes) | Only shared via push notification on Tier 2 |

## Data Transmission Pipeline (Including Stubs)

The data pipeline strictly adheres to an "I -> S -> OUT" architecture. To ensure absolute parity between development testing and production, the Developer Stubs do **not** bypass any logic and do **not** write directly to the database. Instead, they emulate physical watch events by emitting Android Broadcasts that are intercepted by the exact same receiver pipeline as production Wear OS telemetry.

```mermaid
flowchart TD
    classDef watch fill:#1A237E,stroke:#fff,stroke-width:2px,color:#fff;
    classDef mock fill:#004D40,stroke:#fff,stroke-width:2px,color:#fff;
    classDef handler fill:#4A148C,stroke:#fff,stroke-width:2px,color:#fff;
    classDef db fill:#BF360C,stroke:#fff,stroke-width:2px,color:#fff;
    classDef ui fill:#006064,stroke:#fff,stroke-width:2px,color:#fff;
    classDef engine fill:#FF6F00,stroke:#fff,stroke-width:2px,color:#fff;
    classDef sync fill:#1B5E20,stroke:#fff,stroke-width:2px,color:#fff;

    subgraph Input ["Data Sources"]
        W["Physical Watch (Wear OS)"]:::watch
        S["Developer Stub (UI Button)"]:::mock
    end

    WT["Wearable API<br/>DataEventBuffer"]:::watch
    MB["Android Broadcast<br/>Intent"]:::mock

    W -- "/sensor_window" --> WT
    S -- "MOCK_SENSOR_WINDOW" --> MB

    subgraph Handler ["WatchDataReceiverService"]
        direction TB
        WR["WearableListenerService<br/>onDataChanged()"]:::handler
        BR["BroadcastReceiver<br/>onReceive()"]:::handler
        PW(("processWindow")):::handler
    end

    WT --> WR
    MB --> BR
    WR --> PW
    BR --> PW

    SE["Scoring Engine<br/>(Evaluates ML fusion score)"]:::engine
    PW --> SE

    subgraph Database ["Local Persistence"]
        DB[("Room Database<br/>SensorWindowEntity<br/>StressEventEntity")]:::db
    end

    SE --> DB

    subgraph Cloud ["Firestore Sync (Wearer only)"]
        SYNC["FirestoreRepository<br/>syncEvent()"]:::sync
    end

    DB -. "New events" .-> SYNC
    DB -. "Polled every 2s" .-> WVM

    subgraph Output ["UI Layer"]
        WVM["WearerDashboardViewModel"]:::ui
        CVM["CaregiverDashboardViewModel"]:::ui
        UI["Wearer / Caregiver Dashboard"]:::ui
    end

    SYNC -. "Snapshot Listener" .-> CVM

    WVM --> UI
    CVM --> UI
```

## UI Architecture (`android/mobile/src/main/java/com/mtpnda/biocue/ui/`)

To prevent "God Objects" and make debugging scalable, the UI is decoupled from the main activity and split by domain:

- `screens/wearer/WearerDashboardScreen.kt`: Exclusively renders the Wearer interface (stress meter, stub controls, journal).
- `screens/caregiver/CaregiverDashboardScreen.kt`: Exclusively renders the Caregiver interface (pattern review, alerts). **Does not contain wearer telemetry UI**, ensuring caregiver bounds are physically impossible to violate.
- `screens/profile/ProfileScreen.kt`: Full-page user profile with language toggles (EN/ID) and Firebase logout.
- `components/DashboardComponents.kt`: Reusable, stateless widgets (e.g., `JournalCard`, `PatternReviewCard`, `LabelBtn`).
- `theme/Color.kt`: Centralized source of truth for semantic colors (e.g., `CalmColor`, `DangerColor`).
- `Previews.kt`: Centralized hub for `@Preview` annotations to enforce visual parity during development (Complete Wearer, Debug Wearer, Complete Caregiver).

`MainActivity.kt` acts exclusively as the **Top-Level Navigation Router**. It runs a simple compose state machine (`if/else` on `AppViewModel` auth/role states) and injects the necessary `ViewModels` into the respective screen routes.
