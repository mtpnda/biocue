# BioCue for iPhone

SwiftUI app (iOS 16+) with two **separate** experiences — wearer and caregiver — on one backend shared with the Android app.

> ⚠️ **Never compiled.** This code was written without a Swift toolchain; it was syntax-parsed only. Expect to fix some compile errors on first build. The pure logic in `BioCueCore` has tests with golden vectors (see below) that will tell you quickly whether it is correct.

## Build

```bash
brew install xcodegen
cd ios
# 1. In the Firebase console add an iOS app with bundle id  com.mtpnda.biocue.ios
#    and put its GoogleService-Info.plist in  BioCue/Resources/
# 2. Open project.yml and set GOOGLE_REVERSED_CLIENT_ID to REVERSED_CLIENT_ID from that plist
xcodegen generate
open BioCue.xcodeproj
```
Run on a **real device** — Bluetooth does not work in the Simulator. Use a `#if DEBUG` build and the *Developer tools* card to feed synthetic readings without a watch.

## Tests

| Where | What | How |
|---|---|---|
| `Packages/BioCueCore` | wire protocol (golden vectors from `tools/gen_link_vectors.py`, incl. `HELLO_ACK.watchTimeMs`), `WatchClock`, detection parity with the **Android Kotlin** (≈740 generated cases), roles, idempotent event ingest, and **28 pairing / caregiver-access / review tests that mirror the test-first Kotlin ones** | `cd Packages/BioCueCore && swift test` (works on macOS **and Linux**; no Apple frameworks) |
| `BioCueTests` | wearer service (pipeline, ACK-after-store, replay safety), role gates, file stores | Xcode ⌘U |
| `scripts/check_role_boundaries.py` | wearer ⟂ caregiver ⟂ shared | `python3 scripts/check_role_boundaries.py` (verified; add to CI) |

## Layout & the role split

```
Packages/BioCueCore/     pure Swift: detection, protocol, roles           (shared logic, testable anywhere)
BioCue/Sources/
  App/        composition root + RootView  ← the ONLY place that decides wearer vs caregiver
  Shared/     auth, session, cloud protocols, role-agnostic views        (depends on neither role)
  Wearer/     WatchLinkCentral (CoreBluetooth), WearerService, local stores, wearer screens
  Caregiver/  CaregiverViewModel, pairing + dashboard screens           (no Bluetooth, no vitals)
```
Cloud access is three protocols (`ProfileCloud`, `WearerCloud`, `CaregiverCloud`); a feature only receives its own. Wearer-only types throw `RoleError` if constructed for a caregiver. Full rationale: `../docs/spec/09_IOS_AND_ROLE_ENFORCEMENT.md` and `../docs/spec/08_WATCH_PHONE_CONNECTION.md`.

## Before shipping

* **Sign in with Apple** — App Review Guideline 4.8 expects it when a third-party login (Google) is offered.
* Deploy `../biocue/firestore.rules` **only after** running `../biocue/firestore-rules-tests` — the Android app depends on them too.
* Verify the fonts render (`AppFont` falls back to system serif/sans if the variable-font instances aren't found).
* Do the watch ⇄ iPhone hardware spike first (`../docs/spec/08_WATCH_PHONE_CONNECTION.md` §10).
