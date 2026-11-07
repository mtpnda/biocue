# 02. System Architecture & Multiplatform Topology

> **Canonical Specification.** Defines the physical directory boundaries, module contracts, telemetry pipelines, local-first storage invariants, and cross-platform sync topology across **Android**, **iOS**, and the **Deno Edge Backend**.

---

## 1. Top-Level Repository Architecture

The BioCue repository is structured into isolated, purpose-built platform subsystems:

```
biocue/
├── android/                         # Android Multiplatform Project (Gradle 9.5, AGP 8.9)
│   ├── mobile/                      # Android Companion App (Phone / Tablet)
│   ├── wear/                        # Wear OS 3+ Smartwatch App (OLED Zero-Anxiety)
│   └── core-detection/              # Shared Room DB, ML Fusion, and Data Layer schemas
├── ios/                             # Native iOS Subsystem (Swift 6, SwiftUI)
│   ├── BioCue/                      # iPhone & iPad Companion Application
│   └── Packages/BioCueCore/         # Shared Core: BLE protocol, Role Gates, Swift Testing
├── backend/                         # Sovereign Edge Data Plane & Web Portal (Deno 2, TypeScript)
│   ├── src/routes/                  # REST APIs (Auth, Pairings, Events, Support, Config)
│   ├── src/pages/                   # SSR DLS Web Pages (Home, Whitepaper, Support, Legal)
│   ├── src/i18n/                    # Strictly English (US) and Bahasa Indonesia dictionaries
│   └── src/ui/                      # Design Language System (DLS) token rendering
├── fastlane/                        # Unified Mobile & Webhost Automation Pipeline
├── Makefile                         # Single-pane command controller
└── .agents/specs/                   # Centralized Architectural Specifications (SSOT)
```

---

## 2. Platform Subsystem Breakdown

### 2.1 Android Subsystem (`android/`)
1. **`:mobile` (Companion App):**
   - **Role:** Dual-role interface (Wearer & Caregiver), Fusion ML scoring engine, BioCue Buddy co-regulation, somatic breathing pacer, local Room DB persistence, and cloud sync relay.
   - **Tech Stack:** Jetpack Compose, Material 3, Kotlin Coroutines, `StateFlow`, Navigation 3.
   - **Key ViewModels:**
     - `AppViewModel`: Global state, Sovereign Auth session, role context, and pairing lifecycle.
     - `WearerDashboardViewModel`: Continuous physiological ingestion, on-device anomaly classification, somatic pacer state, and local event persistence.
     - `CaregiverDashboardViewModel`: Read-only observer receiving real-time event broadcasts from paired wearers.
2. **`:wear` (Wear OS Smartwatch App):**
   - **Role:** High-efficiency optical photoplethysmography (PPG) pulse capture, 3-axis accelerometer sampling, Zero-Anxiety Anchor UI, and on-demand SOS GPS responder.
   - **Tech Stack:** Jetpack Compose for Wear OS, AndroidX Health Services API, `FusedLocationProviderClient`.
   - **Key Services:**
     - `SensorCollectionService`: Low-power foreground service duty-cycling PPG sampling.
     - `PingResponderService`: Executes consent-first GPS fixes strictly on manual or Tier 2 SOS triggers.
3. **`:core-detection` (Shared Contracts & Persistence):**
   - **Entities:** `SensorWindowEntity` (raw biometrics, local-only), `StressEventEntity` (derived events).
   - **Schemas:** `DataLayerSchema` defining Wearable DataLayer paths and DataMap contracts.

### 2.2 iOS Subsystem (`ios/`)
1. **`BioCue` (SwiftUI Client):**
   - Pure Swift 6 architecture enforcing strict Light Mode DLS styling (`.preferredColorScheme(.light)`).
   - Domain-separated views: `WearerView`, `CaregiverView`, `SettingsView`, `SharedViews`.
2. **`BioCueCore` (Domain Package):**
   - **BioCue Link v1:** Direct Bluetooth Low Energy (BLE) peripheral/central protocol streaming sensor frames and clock skew synchronization.
   - **Role Capability Enforcement:** Mutually exclusive capabilities guarantee Caregivers can never trigger Wearer actions and Wearers cannot spoof Caregiver reviews.

### 2.3 Backend Subsystem (`backend/` — Deno KV Edge)
- **Framework:** Hono + OpenAPI + Deno KV.
- **Sovereign Auth:** Pure Google OAuth PKCE and local token minting emitting 24-hour HS256 JWTs. Zero third-party tracker cookies.
- **Real-Time Streaming:** Server-Sent Events (SSE) via `/api/events/stream` and `/api/pairings/stream` providing instant, sub-50ms reactive delivery without heavy database polling.
- **Bilingual DLS SSR:** Fully localized English (US) and Bahasa Indonesia web pages, including the mathematical Technical Whitepaper (`/whitepaper`).

---

## 3. The Data Layer API Contract (Watch ↔ Phone)

All watch-to-phone communication occurs via local Bluetooth / Wi-Fi links (Wearable Data Layer on Android; BioCue Link v1 BLE on iOS).

| Path / Characteristic | Direction | Payload Schema | Purpose & Trigger |
| :--- | :--- | :--- | :--- |
| `/sensor_window` | Watch $\to$ Phone | `timestamp`, `hr_mean`, `rmssd`, `acc_mag` | 2-second duty-cycled physiological telemetry. |
| `/sos_gps_request` | Phone $\to$ Watch | *(Empty Message)* | Commands the watch to acquire an emergency GPS fix (Tier 2 only). |
| `/sos_location` | Watch $\to$ Phone | `timestamp`, `latitude`, `longitude` | Returns the single emergency GPS fix. |
| `/event_start` | Watch $\to$ Phone | `timestamp`, `trigger` (`MANUAL` or `AUTO_TIER_1`) | Wearer tapped "Anchor" or early warning detected. |
| `/event_end` | Watch $\to$ Phone | `timestamp`, `resolved` | Wearer confirmed de-escalation. |

---

## 4. End-to-End Data Pipeline & Persistence Invariant

```mermaid
flowchart TD
    classDef watch fill:#1A237E,stroke:#fff,stroke-width:2px,color:#fff;
    classDef local fill:#004D40,stroke:#fff,stroke-width:2px,color:#fff;
    classDef engine fill:#FF6F00,stroke:#fff,stroke-width:2px,color:#fff;
    classDef cloud fill:#4A148C,stroke:#fff,stroke-width:2px,color:#fff;
    classDef ui fill:#006064,stroke:#fff,stroke-width:2px,color:#fff;

    subgraph Hardware ["Smartwatch Hardware"]
        PPG["PPG Pulse Intervals (IBI/RR)"]:::watch
        ACC["3-Axis Accelerometer (g)"]:::watch
        WUI["Zero-Anxiety Anchor UI"]:::watch
    end

    subgraph WearerDevice ["Wearer Phone (Local Sanctuary)"]
        WDR["WatchDataReceiverService"]:::local
        ML["On-Device Fusion ML Scoring<br/>(HRV RMSSD + Motion Decoupling)"]:::engine
        RDB[("Local Room / SQLite DB<br/>• SensorWindowEntity (Private)<br/>• StressEventEntity (Events)")]:::local
        BUDDY["BioCue Buddy Engine<br/>(Empathetic Co-Regulation)"]:::ui
        WVIEW["Wearer Dashboard<br/>(Breathing Pacer + Journal)"]:::ui
    end

    subgraph SovereignEdge ["Deno KV Edge Data Plane"]
        AUTH["Sovereign JWT Auth"]:::cloud
        PAIR["Pairing PIN Registry (6-Digit)"]:::cloud
        SSE["SSE Event Stream<br/>(/api/events/stream)"]:::cloud
    end

    subgraph CaregiverDevice ["Caregiver Phone"]
        CLISTENER["SSE Stream Observer"]:::local
        CVIEW["Caregiver Dashboard<br/>(Pattern Review + SOS Beacon)"]:::ui
    end

    PPG & ACC -->|"/sensor_window" (Local BLE/DataLayer)| WDR
    WUI -->|"/event_start"| WDR
    WDR --> ML
    ML --> RDB
    RDB --> BUDDY & WVIEW
    RDB -.->|"Derived Events Only<br/>(Zero Raw Biometrics)"| SSE
    AUTH & PAIR --> SSE
    SSE -->|Real-time Reactive Push| CLISTENER
    CLISTENER --> CVIEW
```

### The Strict Data Sovereignty Boundary
1. **Raw Biometrics Never Leave the Wearer Device:** PPG raw interval buffers, raw heart rate values, and 3-axis accelerometer traces are stored exclusively in the wearer's local Room / SQLite database. They are **NEVER** transmitted over the network to the Deno KV backend or caregiver devices.
2. **Derived Events Only:** Only high-level event metadata (timestamp, emotional label, activity classification, and emergency SOS GPS if explicitly triggered) is synchronized to the caregiver circle.
3. **Instant Revocation & Cache Wipe:** When a wearer revokes a caregiver in the Settings screen, the Deno KV pairing token is instantly deleted, and the caregiver's local client immediately purges all cached journal entries.
