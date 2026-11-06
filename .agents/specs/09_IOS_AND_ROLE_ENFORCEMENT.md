# 09. iPhone App, Role Enforcement & Account Lifecycle

> **Added by the spec merge.** Docs 01–07 describe an Android-only system. This document adds: the iPhone app (wearer **and** caregiver), how the wearer ⟂ caregiver separation is **enforced** (four layers), account-session lifecycle rules, and the defects found while building it. It does not replace anything in 01–07; where this document and 07 disagree, **07 wins** and the disagreement is logged in `00_MERGE_LOG.md`.
>
> Pairing flow, detection, watch UI and event clustering are **not repeated here** — see 07, 03, 04 and 06. This document records only how the implementation *differs* from them.

## 1. Scope

| Platform | App | Spec source |
|---|---|---|
| Android | `:mobile` (wearer + caregiver), `:wear`, `:core-detection` | 01–07 |
| iOS (added) | `ios/BioCue` — SwiftUI, wearer + caregiver, same Firebase project and document fields | this document |
| Watch ↔ iPhone (added) | BLE link | 08 |

## 2. System overview (as built)

```
 ┌────────── WEARER ───────────────────────────────┐          ┌──────── CAREGIVER ───────┐
 │ Watch ──(Data Layer | BLE Link)──► Wearer phone │          │  Caregiver phone         │
 │  HR · HRV · motion                  ├ scoring    │          │   (no watch link,        │
 │  one-tap emotion log                ├ local log  │          │    no raw vitals)        │
 │  SOS                                └ local      │          │                          │
 │                                       alerts     │          │                          │
 └───────────────────────┬─────────────────────────┘          └───────────┬──────────────┘
                         │ events + pairing only                           │ reads events,
                         ▼                                                 ▼ labels them
                ┌─────────────────── Firebase (Auth + Firestore) ──────────────────┐
                │ users · pairings · active_codes · wearerData/{uid}/events         │
                └──────────────────────────────────────────────────────────────────┘
```

| Component | Android | iOS | Watch |
|---|---|---|---|
| App | `:mobile` (Kotlin, Compose) | `ios/BioCue` (Swift, SwiftUI) | `:wear` (Kotlin, Compose for Wear) |
| Shared logic | `:core-detection` (scoring, clustering, pipeline, **link protocol**, role table) | `ios/Packages/BioCueCore` (Swift port, verified against Android fixtures) | uses `:core-detection` |
| Backend | Firebase Auth + Firestore (shared by both platforms; same field names and types) | | |

**Raw vitals never leave the wearer's phone** (01, *Extreme Privacy*; 07, *What stays local*). Only events (start, end, trigger type, label, resolved flag) are synced.


## 3. Roles — the separation

### 3.1 Capability matrix (single source of truth)

Pinned by tests in `RoleCapabilitiesTest.kt` and `RoleCapabilitiesTests.swift`, mirrored by `firestore.rules`.

| Capability | Wearer | Caregiver |
|---|:-:|:-:|
| Link to the watch / read raw vitals | ✅ | ❌ |
| Create / close stress events | ✅ | ❌ |
| Show a pairing code; approve / deny / remove caregivers | ✅ | ❌ |
| Enter a pairing code, request access | ❌ | ✅ |
| Read a paired wearer's events | ❌ | ✅ |
| Change `label` / `isResolved` on a paired wearer's events | ❌ | ✅ (and **only** those two fields) |
| Leave a pairing (set own pairing to `REMOVED`) | — | ✅ |

The two columns never overlap on any row.

### 3.2 Enforcement — four layers

1. **Server (the one that cannot be bypassed): `firestore.rules`.** **A role cannot change after it is set** (matches doc 07; profile delete is denied so delete + create is not a way around it). A *proposal* to allow switching, with the access design to go with it, is in doc 10. Only a CAREGIVER profile may create a pairing, only for a WEARER profile, only as itself, only as `PENDING`. Only the wearer changes a pairing's `status`. Events are created/deleted only by the owning WEARER; a caregiver of an `APPROVED` pairing may read them and update **only `label` and `isResolved`**. Only a WEARER may hold a pairing code. See `firestore-rules-tests/` (⚠ not yet executed — §9).
2. **Type system (iOS).** Cloud access is split into three protocols — `ProfileCloud`, `WearerCloud`, `CaregiverCloud`. Each role's feature is only ever handed *its* protocol, so caregiver code has no method to create an event and wearer code has no method to read someone else's events. `WatchLinkCentral`, `WearerService` and `WearerCircleViewModel` throw `RoleError` if constructed for a caregiver (`RoleGate`), and `CaregiverViewModel` throws for a wearer.
3. **Structure + automated boundary checks.** Wearer-only and caregiver-only code live in separate files/folders and may not reference each other.
   * Android: `RoleBoundaryTest` (wearer files: `WearerDashboardViewModel`, `WearerRoute`, `WatchDataReceiverService`, `ui/screens/wearer`, watch-connection files; caregiver files: `CaregiverDashboardViewModel`, `CaregiverRoute`, `ui/screens/caregiver`).
   * iOS: `ios/scripts/check_role_boundaries.py` over `Sources/Wearer`, `Sources/Caregiver`, `Sources/Shared`.
   Shared UI takes plain values and closures only — never a service or cloud protocol.
4. **Runtime guards.** The watch link refuses a non-wearer phone in its first message (`ROLE_NOT_ALLOWED`). `WatchDataReceiverService` ignores watch data when the cached role is CAREGIVER (`RoleStore`; an *unknown* role is allowed so upgrading never drops a wearer's data). Sign-out wipes role state (§8).

### 3.3 What each role sees

| | Wearer | Caregiver |
|---|---|---|
| Home | "My Circle": watch status, own stress meter, pending requests, connected caregivers, pairing code, "what leaves this phone", journal | "BioCue · Caregiver": wearer's state, SOS card when an event is ongoing, pattern review, journal |
| Review | Reviews **own** ended-but-unlabelled events | Reviews the wearer's **unresolved** events |
| Settings | Profile, log out | Profile, **Manage connections** (enter a code / unlink), log out |


## 4. Account & session lifecycle

* Sign-in: email/password or Google (iOS: add Sign in with Apple before App Store review — Guideline 4.8).
* The UI **waits for the profile to load** before offering role selection, so an existing account can never be shown (and tap) the role picker.
* **Sign-out resets everything** that belonged to the previous account (role, pairings, listeners, cached role). Without this, the next account on the device inherited the previous role's screens.
* iOS stores events/windows per account; windows are not carried over to a different account.
* **Everything tied to a watch must be per account**, not per device (a phone can be used by more than one account): the remembered iPhone↔watch link (§5 below — currently *not* the case on iOS) and, on Android, the attribution of incoming watch data (§9).


## 5. Known gaps and defects (found while building the iPhone app)

Fixed in this change:
* **Caregiver label/resolve writes were rejected** by the old rules (only the wearer could write under `wearerData`) and the error was swallowed — caregiver review never persisted. *Rules fixed (needs the rules tests run).*
* **"Unlink from Wearer" never worked** for the same reason. *Rules now let a caregiver set their own pairing to `REMOVED`.*
* `active_codes` was writable by any signed-in user, and roles were not enforced server-side. *Rules fixed — but they currently make a role permanent; doc 10 replaces that with the epoch rule.*
* Previous account's role leaked to the next sign-in; role-picker flash could overwrite a role; profile listeners stacked and survived logout. *Fixed in `AppViewModel`/`MainActivity`.*
* Detection logic lived inline in `WatchDataReceiverService`. *Extracted to the tested `DetectionPipeline`; behaviour unchanged.*

**Found while specifying role switching (current build, not yet fixed):**
* ~~**iOS remembers the paired watch under one global key**~~ — **fixed:** the key is now per account (`biocue.watch.peripheralID.<uid>`) and sign-out calls `shutdown()` (cancel connection and scan, keep nothing running). *Written, not compiled or run.*
* **Android attributes watch data to whoever is signed in.** The watch pairs with the *phone*, not the account, so a second wearer account on that phone would record the first person's readings. (Only a CAREGIVER account is blocked, by the `RoleStore` guard.)
* **A removed caregiver's cached copy:** now handled per spec 07 — in-memory data is wiped when access is revoked, and a flag makes the app clear Firestore's on-disk cache at the **next cold start** (it can only be cleared before Firestore is first used), on both platforms. Until that next launch the on-disk copy remains. *Written, not built or run.*
* **The wearer approves a request seeing only a self-typed display name**, so a pairing-code holder can pose as someone else. Showing the caregiver's email (or requiring a second confirmation) would help.
* *(Removed: "roles are permanent" was listed here as a defect. It is **required by doc 07**; the open question is whether to change 07 — see doc 10 and `00_MERGE_LOG.md` §A.)*

**Found in this round (open):**
* **Android's `AppViewModel` is shared and mixed** — it holds both the wearer's and the caregiver's pairing functions (the iOS side splits them per role, and its boundary script enforces it). The Android boundary test cannot police a class that legitimately serves both; splitting it is a larger refactor.
* ~~The one-clock fix is not applied on Android~~ — **applied** via `ClockSkewEstimator` (doc 08 §4.2b); written, not built or run.
* **Pairing-code lookup trusts the client for expiry.** The server only checks that the code document exists; `expiresAt` is compared in the app. A modified client could use an expired code (the wearer still has to approve).

**Still open (not changed):**
1. **Caregivers get no push notifications — and 01/04 require them** (01: alert within 30 s of a Tier 2; 04 §2: the only message a caregiver receives is a meltdown alert; 07: GPS is shared "only via push notification"). They currently see updates only while the app is alive. Needs a backend (Cloud Function → FCM/APNs).
2. **SOS location is a dead end on Android.** The phone sends `/sos_gps_request`; the watch's `PingResponderService` only handles `/ping`; and nothing on the phone consumes `/sos_location`. (iOS implements the full loop over BLE.) The review card on Android shows a hard-coded placeholder address.
3. **The watch ships with synthetic data enabled** (`setTestMode(true)` at start) and the real HR callback sends a hard-coded `rmssd = 45`, `hrSlope = 1`. HRV is not actually computed from inter-beat intervals yet, so detection is not driven by real HRV.
4. **No per-user baseline** (fixed 75/5/40/8), and no personal calibration or learning loop in the app.
5. **Android journal date arrows skip days** (the UI passes a target offset to a method that adds it as a delta).
6. **Android `ongoingEventLabel` is always null** (it searches a list already filtered to ended events). The Android caregiver pairing listener only queries `APPROVED`, so a revocation is never observed there.
7. **Pairing-code countdown** shows 5 min on Android while the code is valid for 10; iOS shows the true remaining time.
8. `WatchDataReceiverService` awaits Firestore writes inline; offline, that blocks the receiver. (iOS does not await cloud writes.)
9. `users` is readable by any signed-in user (needed for code lookup); move lookup behind a Cloud Function to close it.
10. The watch outbox for the iPhone link is in-memory (§ connection doc).
11. iOS strings are English only; Android has Indonesian.


## 6. Verification status — what is and is not proven

| Area | Status |
|---|---|
| Pairing, caregiver access, reviews, link protocol, session/outbox/persistence, clock, advertise and test-mode policies, detection pipeline, role table (Kotlin) | **Run, test-first:** 115 tests pass in one JVM run (50 written test-first, plus the pre-existing clusterer/scoring/schema/engine tests); `RoomDatabaseTest` needs Android and was not run. Mutation-checked where noted in `11_TDD_PROGRESS.md` |
| Watch BLE server | Compiles against Android API 34; **not run** on a device |
| Android role/session changes | Parsed; boundary + store tests run; app **not built or run** (Gradle unavailable) |
| Firestore rules + tests | Written; **not executed** (needs the Firebase emulator) |
| Swift (BioCueCore + app + tests) | Syntax-parsed only; **never compiled or run** (no Swift toolchain) |
| Android ⇄ iOS detection parity | Fixtures generated from the real Kotlin; Swift side not run |
| Hardware: Galaxy Watch 4 Classic ⇄ iPhone | **Not tested** (see doc 08 §10) |
| Role switching (doc 10 — **proposal**) | No code, rules or tests. The proposed access *design* was model-checked (`tools/model_check_role_switch.py`: holds, and fails when the epoch check is removed) |
| Pairing-gated access (a caregiver sees data only with an `APPROVED` pairing) | Present in the current rules and both clients, **traced by reading, not executed** (emulator unavailable) |

**First things to do:** (1) run `firestore-rules-tests`; (2) `xcodegen generate`, build, run `BioCueCoreTests` and `BioCueTests`; (3) `./gradlew :core-detection:test :mobile:testDebugUnitTest :wear:testDebugUnitTest`; (4) do the hardware spike before investing in more iOS UI.
