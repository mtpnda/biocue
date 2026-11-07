# BioCue — Centralized Technical Specifications & Architecture Index
> **Canonical Directory:** `.agents/specs/` (Single Source of Truth)  
> All requirements, schemas, data contracts, and verification gates are consolidated here.

---

## 1. Core System Specifications

| Document | Title & Scope | Status |
| :--- | :--- | :--- |
| [**`00_MERGE_LOG.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/00_MERGE_LOG.md) | **Merge Log & Audit Trail:** Conflict resolutions, grill answers, decisions log, and TDD seams | Authoritative |
| [**`01_PRODUCT_REQUIREMENTS.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/01_PRODUCT_REQUIREMENTS.md) | **Product Requirements Document (PRD):** 2-in-1 account model, zero-anxiety philosophy, data flows, and KPIs | Authoritative |
| [**`02_ARCHITECTURE.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/02_ARCHITECTURE.md) | **System Architecture:** Modular architecture (`android/`, `backend/`, `ios/`), Wearable Data Layer contract, developer stubs | Authoritative |
| [**`03_ML_AND_SENSORS.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/03_ML_AND_SENSORS.md) | **Sensors & Fusion Engine:** PPG HR, Accelerometer, Tier 0–2 scoring, debug-only Test Mode, SOS GPS Ping | Authoritative |
| [**`04_USER_INTERFACE.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/04_USER_INTERFACE.md) | **User Interface & UX:** Apple Liquid Glass HIG compliance, Name-only privacy, caregiver journal & alert cards | Authoritative |
| [**`05_BUILD_AND_CICD.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/05_BUILD_AND_CICD.md) | **Build & CI/CD Pipelines:** Fastlane Supply publishing, dynamic versioning, Make controller, testing gates | Authoritative |
| [**`06_EVENT_CLUSTERING.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/06_EVENT_CLUSTERING.md) | **Event Clustering:** Sweep-line algorithm ($O(N \log N)$) combining algorithmic detections and user tap inputs | Authoritative |
| [**`07_MULTI_ACCOUNT.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/07_MULTI_ACCOUNT.md) | **Multi-Account & Security:** 6-digit code pairing (`active_codes`), single-use consumption, `reviews` subcollection, Firestore rules | Authoritative |
| [**`14_FIREBASE_SETUP.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/14_FIREBASE_SETUP.md) | **Firebase & Firestore Setup:** Console configuration, Firestore rules deployment, auth provider settings | Operational Guide |
| [**`15_MOCK_SENSOR_EMITTER.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/15_MOCK_SENSOR_EMITTER.md) | **Mock Sensor Emitter System:** Web console, CLI tools, ADB broadcast contracts, scenarios, and test tooling | Authoritative |
| [**`16_DENO_AUTH_AND_DATA_PLANE.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/16_DENO_AUTH_AND_DATA_PLANE.md) | **Deno Edge Auth & Data Plane:** Replaces Firebase with Deno KV, HS256 JWT, Caregiver Circles, and SSE live streams | Authoritative |
| [**`17_SOVEREIGN_AUTH_AND_OFFLINE_OAUTH_ENGINEERING.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/17_SOVEREIGN_AUTH_AND_OFFLINE_OAUTH_ENGINEERING.md) | **Sovereign OAuth & Offline State Machine:** Pure Swift PKCE, reversed scheme, offline fallback FSM, and Deno token minting | Authoritative |
| [**`18_BATTERY_OPTIMIZED_HRV_PROFILING_AND_SYNC.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/18_BATTERY_OPTIMIZED_HRV_PROFILING_AND_SYNC.md) | **Battery-Optimized HRV & Stress Level Profiling & Local Sync:** 4-pillar battery strategy, adaptive duty-cycling, hardware offbody, RMSSD baseline | Authoritative |
| [**`19_INTELLIGENT_COMPANION_AND_CRISIS_GUARDIAN.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/19_INTELLIGENT_COMPANION_AND_CRISIS_GUARDIAN.md) | **Intelligent Companion & Crisis Guardian Engine (BioCue Buddy):** 4-phase empathetic co-regulation, somatic pacer, free-text semantics, 15m safety guardian | Authoritative |
| [**`20_HYBRID_INTELLIGENCE_AND_ON_CHIP_SLM_ARCHITECTURE.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/20_HYBRID_INTELLIGENCE_AND_ON_CHIP_SLM_ARCHITECTURE.md) | **Hybrid Intelligence & On-Chip SLM Architecture:** Autonomic-motor decoupling mathematical formulation, deterministic safety air-gap, and Gemma 2B / CoreML on-chip SLM execution | Authoritative |

---

## 2. Multiplatform & Extended Protocols

| Document | Title & Scope | Status |
| :--- | :--- | :--- |
| [**`08_WATCH_PHONE_CONNECTION.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/08_WATCH_PHONE_CONNECTION.md) | **BioCue Link v1 (BLE):** Watch ↔ iPhone wire protocol, packet frames, clock skew estimation | Active |
| [**`09_IOS_AND_ROLE_ENFORCEMENT.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/09_IOS_AND_ROLE_ENFORCEMENT.md) | **iOS Architecture:** 4-layer wearer ⟂ caregiver isolation, session lifecycle, boundary check script | Active |
| [**`10_PROPOSAL_ROLE_SWITCHING.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/10_PROPOSAL_ROLE_SWITCHING.md) | **Role Switching Proposal:** Historic analysis; superseded by the 2-in-1 model (both roles concurrently) | Archived Record |
| [**`11_TDD_PROGRESS.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/11_TDD_PROGRESS.md) | **TDD Progress & Verification:** Unit test evidence, test seams, verified coverage across modules | Active |
| [**`12_PUSH_AND_GPS.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/12_PUSH_AND_GPS.md) | **FCM Push & SOS GPS:** Cloud Functions trigger, payload privacy contracts, lat/lng transmission | Active |
| [**`13_DESIGN_LANGUAGE_SYSTEM.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/13_DESIGN_LANGUAGE_SYSTEM.md) | **Design Language System (DLS):** Unified tokens across Android Compose, iOS SwiftUI, and Web HTML | Active |
| [**`GRILL_ROUND_1.md`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/GRILL_ROUND_1.md) | **Grill Review Round 1:** 14 adversarial design stress-tests and resolution consensus | Reference |

---

## 3. Interactive Prototypes

* [**`PROTOTYPE-V2.html`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/PROTOTYPE-V2.html): High-fidelity interactive prototype demonstrating Apple Liquid Glass Light Mode, live telemetry visualization, and clinical UX guidelines.
* [**`PROTOTYPE.html`**](file:///Users/valdiearsanur/Workspace/biocue/.agents/specs/PROTOTYPE.html): Initial interactive dashboard prototype for stress curves and sensor feeds.
