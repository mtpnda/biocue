# 11. TDD Progress

How the spec amendments were turned into code under the `tdd` skill (one seam → one failing test → minimal code → repeat; test only at confirmed seams, mock only at boundaries). Seams were confirmed in `00_MERGE_LOG.md` §F.

## What "passing" means here

| Layer | Status |
|---|---|
| **Kotlin domain** (`:core-detection`) | **Executed.** Stand-alone Kotlin 2.0.20 + JUnit 4: **109 tests pass** (44 new this round + the pre-existing clusterer, scoring, schema and engine tests). `RoomDatabaseTest` needs Android and was **not** run |
| Watch Bluetooth code | Type-checked against the real Android API 34 library; **never run** on a device |
| Android app wiring (`AppViewModel`, `FirestorePairingStore`, caregiver/wearer view models, `MainActivity`) | Syntax-parsed only. **Not built, not run.** One bug of mine (a stale-file overwrite that dropped imports) was found by inspection, not by a compiler |
| `firestore.rules` + `firestore-rules-tests` | Rules written to spec 07; tests valid JavaScript. **Not executed** (no emulator) |
| Swift (`BioCueCore`, app, tests) | Syntax-parsed only; **never compiled or run** (no Swift toolchain). The 28 Swift pairing/access/review tests mirror the Kotlin ones one-for-one |
| `tools/verify.sh` | Run here: protocol vectors, iOS role boundaries and detection fixtures reproduce; Gradle / rules / Swift checks **skipped** (and reported as skipped) |

## Slices (44 cycles)

| Seam | Cycles | Tests | Notes |
|---|---|---|---|
| `PairingLifecycle` (wearer + caregiver halves) | 1–14 | 14 | max-2 block; code issue (10 min TTL); single-use consumption (and *not* when nothing pending); deny→delete; remove→`REMOVED`+`removedAt`; approve (+ refused at 2); lookup, expiry, create, re-pair on the same document, no duplicate on pending/approved |
| `CaregiverAccess` | 15–22 | 8 | revoked + wipe once; never-had-access ≠ revoked; pending; vanished approved pairing; acknowledge; cold start after removal; dismissal survives restart |
| `ReviewSubmission` / `ReviewApplier` | 23–28 | 6 | allowed only on APPROVED; two-stage rules; first resolution wins; caregiver's journal overlay |
| `HELLO_ACK.watchTimeMs` (wire change) | — | link vectors | Python reference changed first → both languages' vectors regenerated → Kotlin red (compile) → green. 17 vectors, byte-exact |
| `WatchClock` | 29–31 | 3 | includes the end-to-end proof: with conversion a 14-min-old event is still "ongoing" on a 20-min-slow watch; without it a duplicate event opens |
| `AdvertisePolicy` | 32–33 | 2 | service id only while pairing |
| `LinkOutbox` persistence + `FileOutboxStorage` | 34–42 | 9 | survives reboot; ack persisted; sequence continues; unreadable file → empty; cap on restore; failed save keeps the event; CRC detects a flipped byte |
| `TestModePolicy` | 43–44 | 2 | release never; debug only if requested |

## Round 3 slices

| Seam | Cycles | Tests | Language / status |
|---|---|---|---|
| `ClockSkewEstimator` (Android one-clock fix) | 45–48 | 4 | Kotlin, **run** |
| `Cooldown` (one GPS fix per SOS; push rate limit) | 49–50 | 2 | Kotlin, **run** |
| `planNotifications()` (push: who/what/never-what) | P1–P11 | 11 | JS (`node --test`), **run** |
| `handleNotifyMeltdown()` (Cloud Function logic, injected Firebase) | H1–H7 | 7 | JS, **run** |
| `gen_dls.py` (resolve, validate, contrast, Kotlin/Swift/HTML emitters, generate/check) + reskin guard | D1–D7, K1–S1, H1–H4, G1–G2, R1 | 18 | Python (`unittest`), **run** |

Totals now: Kotlin 115 · JS 18 · Python 18.

## Process lapses (kept on the record)

* **Round 3 — my test runner lied to me twice.** `node --test test/` failed on every run on Node 22 (a folder is treated as a module), so my first "red" and "green" lines for the push planner meant nothing; after fixing that, a syntax error I had introduced in cycle P3 (statements after a brace-less `for`) was hidden behind a generic "test failed". Both were found by reading the raw error, then the three affected tests were mutation-checked from scratch. The runner now prints the real error.
* **Cycle 48 (clock estimator)** first passed *without* the logic it was meant to drive (the scenario was non-discriminating). Replaced with a scenario that needs the window, and shown red→green.
* **A spec of mine contradicted yours** (I said an automatic Tier 2 push carries no GPS; your 03/04 say it does). Found while writing call sites; test and spec corrected first.
* My `run()` helper once printed an entire HTML page into the log as an "error"; output is now capped.
* An invalid CSS shorthand (`font: … inherit`) in the generated page was found by re-reading, not by a test.
* **The generated HTML page has not been opened in a browser.**

* **Cycle 3:** test and code were written together; red was never observed. Proven afterwards with a mutant (consumption removed → test fails).
* **Cycles 12–13:** both tests had a bug of mine (the fake clock moved past the code's expiry), so their first "reds" were for the wrong reason. Fixed, then mutation-checked.
* **Re-pair test:** passed even with the re-request branch removed, because the fake store silently overwrote on `create`. The fake now enforces the real contract (create fails on an existing document); the mutant then fails.
* **Cycle 17:** test and code written together (mutation-checked afterwards). **Cycle 18, 25, 30** passed immediately (already covered by earlier code); 18 and 25 were mutation-checked, **30 (identity clock) was not** shown capable of failing.
* **Cycle 22:** its first red was a compile error (new constructor parameter), not a behavioural failure.

## Known limits of this evidence

* The Kotlin tests pin the *domain*. They say nothing about the Firestore adapter, the view models, the Compose screens, or the rules.
* `FirestorePairingStore` calls the suspend repository through `runBlocking(Dispatchers.IO)`; it assumes callers are off the main thread.
* `AdvertisePolicy` removes the service UUID once bonded; whether iOS reconnects without it, and whether an empty advertisement is accepted, is a **hardware question**.
