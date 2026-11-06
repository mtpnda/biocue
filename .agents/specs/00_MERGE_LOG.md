# 00. Merge Log

How your specs (01, 03–07) and the documents I wrote earlier fit together, what conflicts, and what still needs you.
**Rule applied** (your instruction): where your specs answer a question, your spec wins; only where they don't do I use my recommendation. Your documents 01, 03, 04, 05, 06, 07 are included **verbatim and unedited** — amendments are *proposed* in §D, not applied.

## Source inventory

| Item | Status |
|---|---|
| `01`, `03`, `04`, `05`, `06`, `07`, `README` | Received intact; included with approved amendments |
| **`02_ARCHITECTURE.md`** | **Received and reconciled.** Covers modules, Wearable Data Layer contracts, cloud sync, and developer stub pipelines |
| `PROTOTYPE.html`, `PROTOTYPE-V2.html` | Received intact; included in `.agents/specs/` |
| 08, 09, 10 | **Added**: 08 watch↔phone protocol (iPhone path), 09 iPhone app + role enforcement + gaps, 10 role-switching *proposal* (superseded by 2-in-1 model) |

---

## A. Role switching — **RESOLVED: Option A (keep 07, roles immutable)**

> *Resolution:* the product owner said "any other, follow your recommendations"; my recommendation was A. **That is my reading, not an explicit choice** — reopen if wrong (doc 10 is kept as the alternative).

### Original analysis

| Source | Says |
|---|---|
| **07**, *Role Selection* | "Role is chosen once at first login… **Role cannot be changed after selection. To switch, the user must create a new account.**" |
| **01** *Account Model*; **README** | Role chosen at first login; "role immutability" |
| **Original app code** | Only the first-login role picker ever wrote a role; Settings only displayed it |
| **Your chat message** | "can be switch on setting" |
| **Current build** | Role is permanent (rules + iOS picker) — **this matches 07** |

**Correction to something I told you:** I said making roles permanent was a decision I made and that I had "removed something you wanted". That was wrong. Immutability is *required by your own spec 07* and matched the original code. What differs is your later request in chat.

Your spec and your request disagree, so I will not pick for you:

* **A — Keep 07.** Roles are permanent; switching = new account. Nothing to build. Delete doc 10.
* **B — Adopt doc 10.** Switchable from Settings; ends all pairings; an epoch counter invalidates old pairings. Most work; touches the access rules. Amends 07.
* **C — Narrow switch.** Allowed only while the account has **no events and no pairings** (fixes "I picked the wrong role"). Small change, but useless once the account has history.

➡️ My lean: **A**, unless a real need beyond "I tapped the wrong button" exists, in which case **C**. B is only justified if people must switch *after* building history.

*Everything in 10 — and grill items R2-1/2/3 — is blocked on this answer.*

---

## B. The grill questions, reassessed against your specs

| Q | Topic | Status | Basis | Outcome |
|---|---|---|---|---|
| 1 | Scope of the TDD pass | **Partly** | Specs don't define the rewrite, but they add requirements the code doesn't meet (§C) | Scope = spec-vs-code gaps in §C **plus** my additions. Caregiver push is a *requirement* (01, 04), so it is **sequenced after, not optional** |
| 2 | One role per account | **Answered** | 07, 01: one role, separate accounts on **separate devices** | Yes. (Subject to §A) |
| 3 | Stop listing users/codes | **Conflict inside 07** | 07 says the caregiver *queries `users`* by code, but its own table says `users` is **owner read/write** — the query cannot work under that rule. `active_codes` is not in 07 at all | Apply my recommendation (get-only lookup via `active_codes`), which **matches 07's access table**. Needs approval as an amendment (§D-1) |
| 4a | Single-use pairing codes | **Answered** | 07: "Code is single-use: once a caregiver submits it, the code is consumed" | Yes — **the code doesn't do this** (gap C-9). Mechanism (wearer's app consumes it on the first request) is mine |
| 4b | Show caregiver's **email** | **Rejected by spec** | 07/04: "Allow **[Caregiver Name]** to connect?"; 04: identity is always the **name, never the email** | **Dropped.** Names only. Residual risk (a self-typed name) is mitigated by the code being shared "out loud or in person" |
| 5 | One clock (watch time) | Not answered | Specs are silent on timestamps | My recommendation stands |
| 6 | Don't advertise a fingerprint | Not answered | Specs have no iPhone/BLE path | Stands |
| 7 | Fake data off by default | **Mostly answered** | 03: Test Mode is a **toggle** ("When active…") ⇒ off by default. The code turns it on at start (gap C-16) | Default **off** (spec). Whether it is hidden in release builds is my addition |
| 8 | Who operates a child's account | **Answered (as assumption)** | 01: the wearer runs the app **on their own phone**; separate accounts | Yes. Consent / children's-privacy law remains an open legal item |
| 9 | Caregiver edits only while unresolved | **Superseded** | 07: caregivers are **read-only on wearer data**; reviews go to a **pairing-scoped subcollection** the wearer app "can optionally import" | My premise (direct edit of the event) is **not** the spec's design. Follow 07; fill the gaps in §D-2 |
| 10 | Persist the watch outbox | Not answered | — | Stands |
| 11 | What counts as "passed" | Process | 05 defines `./gradlew test` / `connectedAndroidTest` only | Stands; Swift + rules commands get added to 05 (§D-6) |
| 12 | Drift-guard script | Not answered | — | Stands; add to 05 |
| 13 | Limits (≤2 caregivers, 1 wearer) | **Answered** | 01/07: ≤ 2 approved; at 2 the screen shows "Maximum caregivers reached" and **blocks code generation**; a caregiver is paired to *a* wearer | Yes. Server-side enforcement isn't addressed → client-only stands |
| 14 | Pairing-strength policy | Not answered | — | Stands |
| R2-1,2,3 | Switch details (close event, offline batch, epoch cost) | **Moot** | Depend on §A | Blocked on §A |
| R2-4 | Account deletion | Not answered | 01 covers caregiver *removal* only | Stands: specify the contract, build later |
| R2-5 | Caregiver push | **Answered as a requirement** | 01: alert ≤ 30 s of Tier 2; 04 §2: the *only* message a caregiver gets is a meltdown alert; 07: GPS shared "only via push" | Not optional. Sequencing (after the TDD slices) is mine. Until built, both caregiver apps are incomplete |
| R2-6 | Seams | Process | — | Revised in §F |

**Tally:** answered/decided by your specs: **7** (2, 4a, 7, 8, 13, R2-5, and 9 superseded); conflict or partial: **3** (1, 3, 4b rejected); not answered → my recommendation: **8** (5, 6, 10, 11, 12, 14, R2-4, R2-6); blocked on §A: **3**.

---

## C. Conflicts and deviations

"Follow spec" = the spec is the intent; the code/other doc changes. "Decide" = needs you.

| # | Conflict | Resolution |
|---|---|---|
| C-1 | Role switching: 07/01 (immutable) vs your chat message | **Decide** — §A |
| C-2 | 01 says the phone syncs "stress events **and current status**"; 07 says current status "stays local" and sync is event-driven only | **Follow 07** (more specific; code already follows it). Amend 01 (§D-3) |
| C-3 | 07: caregiver looks codes up by **querying `users`**; same doc: `users` = owner-only | Apply recommendation — §B Q3, §D-1 |
| C-4 | 07: caregiver writes a review to a **pairing-scoped subcollection**; the code (and my rules) edit `label`/`isResolved` **on the event** (the original rules blocked even that — caregiver review never persisted) | **Follow 07.** Code + rules change; design gaps in §D-2 |
| C-5 | 07: **Deny** → pairing document is **deleted**; code sets `REMOVED` | Follow 07 |
| C-6 | 07: removal records `removedAt`; code never writes it | Follow 07 |
| C-7 | 07: on removal the caregiver **wipes cached data** and shows `AccessRevokedScreen`; Android's caregiver listener filters `status == APPROVED`, so it **never sees the removal**, and nothing wipes anything | Follow 07. (iOS already observes all statuses; wipe still to do) |
| C-8 | 07: re-pairing "creates a **new** pairing document"; the id is deterministic (`{wearer}_{caregiver}`) and the rules (original **and** mine) forbid a caregiver overwriting an existing one — **re-pairing after removal has never worked** | **Decide** — §D-4 |
| C-9 | 07: code is single-use; the code never consumes it, yet both apps say "Single-use" | Follow 07 |
| C-10 | 04: registration collects Email, Password, **Name, Gender, Birthdate**; 07: `users.createdAt`. The apps collect name/email/password only | **Decide** whether in scope — this is personal data of possibly-minor users; its *purpose* isn't stated |
| C-11 | 04: caregiver dashboard shows SOS latitude/longitude; 07: GPS is "shared only via push notification" ⇒ the caregiver can only learn the location from a push payload, which doesn't exist yet | Resolved by building push (R2-5) |
| C-12 | 03 refers to `MainViewModel`, which no longer exists (now `WearerDashboardViewModel`, `WatchDataReceiverService`, `DetectionPipeline`) | Doc drift — update 03 names (§D-3) |
| C-13 | 03: the watch "intercepts `/sos_gps_request`… returns `/sos_location`"; the watch only handles `/ping`, and nothing on the phone consumes `/sos_location` — the whole SOS-GPS path is dead on Android | Follow 03. (iOS implements it over BLE) |
| C-14 | 04 §2 describes only the **caregiver** dashboard; the wearer's phone UI ("My Circle") is **unspecified** | Spec gap — needs a section in 04 |
| C-15 | 03: Test Mode is a toggle; code enables it at start | Follow 03 (Q7) |
| C-16 | 07 lists collections `users`, `pairings`, `wearerData`; the code also uses **`active_codes`** | Add to 07 (§D-1) |
| C-17 | 01/03: transport is the Data Layer only; the iPhone path (08) is new | Additive — 08 |
| C-18 | `02_ARCHITECTURE.md` reconciliation | **Resolved** — Reconciled against multiplatform architecture (`android/`, `backend/`, `ios/`) and included in `.agents/specs/` |

---

## D. Proposed amendments to your documents (**not applied** — approve or reject)

1. **07 — Code lookup + collections.** Replace "App queries Firestore for `users` where `pairingCode == input`…" with: caregiver reads `active_codes/{code}` (get-only), checks `expiresAt`, then reads `users/{wearerUid}`. Add `active_codes/{code}` {`wearerUid`, `expiresAt`} to the collections table. `users` stays owner-only **plus get-by-uid for a code holder**; **no list access** on `users` or `active_codes`.
2. **07 — Review submission** (fills what the spec leaves open). Caregiver writes `pairings/{pairingId}/reviews/{eventId}` = {`label`, `isResolved`, `updatedAt`}; only the caregiver of an **APPROVED** pairing may write; the wearer reads it. The wearer's app applies a caregiver review to an event **only while that event is unresolved** (first resolution wins) — this resolves 07's "optionally import". The caregiver's journal merges the wearer's events with its own reviews for display.
3. **01** — delete "and current status" from *Wearer Phone → Firestore* (C-2). **03** — replace `MainViewModel` with the current class names (C-12) and state that Test Mode defaults to **off** (C-15).
4. **07 — Re-pairing (C-8).** Keep the deterministic id; let a caregiver whose pairing is `REMOVED` re-request by setting it back to `PENDING` (and clearing `removedAt`). This replaces "creates a new pairing document".
5. **07 — Deny** keeps "document is deleted"; **Remove** keeps `REMOVED` + `removedAt`. No change — listed so the code is changed to match.
6. **05 — Testing commands.** Add: `tools/verify.sh` (protocol vectors + detection fixtures + role-switch model check must reproduce), `cd firestore-rules-tests && npm test`, `cd ios/Packages/BioCueCore && swift test`, `xcodebuild test`, `python3 ios/scripts/check_role_boundaries.py`.

---

## E. New requirements in your specs that the current code does not meet

Candidates for the test-first work (§F). Each is a spec statement, not my opinion.

| # | Requirement | Source | Gap |
|---|---|---|---|
| E-1 | Codes are single-use | 07 | C-9 |
| E-2 | Deny deletes the pairing; removal sets `REMOVED` + `removedAt` | 07 | C-5, C-6 |
| E-3 | Removed caregiver: cache wiped, `AccessRevokedScreen`, reads blocked | 01, 07 | C-7 |
| E-4 | Re-pairing after removal works | 07 | C-8 |
| E-5 | Caregiver reviews go to a pairing-scoped subcollection | 07 | C-4 |
| E-6 | At 2 caregivers, code generation is **blocked** | 01, 07 | partly (button disabled; verify) |
| E-7 | Registration collects Gender + Birthdate; `createdAt` stored | 04, 07 | C-10 (needs your scope call) |
| E-8 | Caregiver alert within 30 s of Tier 2 / manual SOS; GPS via push | 01, 04, 07 | no push exists |
| E-9 | Watch Test Mode off by default | 03 | C-15 |
| E-10 | SOS GPS ping (`/sos_gps_request` → `/sos_location`) works | 03 | C-13 |

---

## F. Revised TDD seams (to confirm before any test is written — required by the `tdd` skill)

| Seam | Lang | Catches | Misses |
|---|---|---|---|
| **`PairingLifecycle`** over a `PairingCloud` port: consume code on first request, deny→delete, remove→`REMOVED`+`removedAt`, re-request after removal, block at 2 caregivers | Kotlin — runnable | E-1, E-2, E-4, E-6 | real Firestore |
| **`CaregiverAccess`**: on `REMOVED`/deleted ⇒ wipe cache + revoked state; never read after | Kotlin — runnable | E-3 | the device cache itself |
| **`ReviewSubmission`** + wearer-side apply ("first resolution wins") | Kotlin — runnable | E-5 | rules |
| `WatchClock` offset + `LinkCodec` vectors (Q5); `AdvertisePolicy` (Q6); outbox persistence (Q10) | Kotlin — runnable | skew, wire bytes, UUID only in pairing window, reboot survival | real hardware |
| `firestore.rules` via emulator (lookup get-only, review subcollection, `removedAt`, re-pair) | JS — **written, not executable here** | the access rules | — |
| Swift: same behaviours in `BioCueCore` / `SessionStore` / `RememberedWatchStore(account)` | Swift — **written, not executable here** | parity | anything needing a compiler |

I will not write a test at any seam you haven't confirmed.

---

## G. What I need back

1. **§A** — A, B or C for role switching.
2. **§D** — approve / reject each proposed amendment (1–6).
3. **C-10** — are Gender/Birthdate in scope now, and what are they *for*?
4. **`02_ARCHITECTURE.md`** — please upload it (plain `.md`, like the others).
5. **§F** — confirm the seams.

---

## H. Decisions recorded

| Date | Decision | Source |
|---|---|---|
| 2026-10-30 | Gender / Birthdate **not collected** (C-10 closed; 04 amended) | product owner: "personal data no need" |
| 2026-10-30 | Current status **stays local**; 01 amended (C-2) | "data stay local true" |
| 2026-10-30 | Pairing mechanism follows my safer design: direct code lookup (no listing), single-use consumed by the wearer's app, reviews in `pairings/{id}/reviews` with first-resolution-wins, re-pair `REMOVED→PENDING` (§D-1, D-2, D-4 approved; 07 amended; Q3, Q4a, Q9) | "follow your suggestion which safer" |
| 2026-10-30 | Q5, Q6, Q10, Q11, Q12, Q14, R2-4 (spec only), R2-5 sequencing, seams §F → **my recommendations** | "any other, follow your recommendations" |
| 2026-10-30 | Role switching → **Option A** (roles immutable) — *interpreted, see §A* | same |
| 2026-10-31 | `02_ARCHITECTURE.md` reconciled & unified into `.agents/specs/` | Centralization pass |

**TDD now proceeds** on the confirmed seams (§F). Status of each slice is tracked in `11_TDD_PROGRESS.md`.

---

## I. Progress against section E (the requirements your specs add)

Details and evidence: `11_TDD_PROGRESS.md`. "Done" means *written test-first in Kotlin and passing*; the Android/iOS wiring is written but **not built or run**.

| # | Requirement | Domain (Kotlin, tested) | Android wiring | iOS (Swift port + wiring) |
|---|---|---|---|---|
| E-1 | Codes are single-use | ✅ | ✍️ written | ✍️ written |
| E-2 | Deny deletes; removal sets `REMOVED` + `removedAt` | ✅ | ✍️ | ✍️ |
| E-3 | Removed caregiver: cache wiped + revoked screen | ✅ | ✍️ (next-launch disk purge) | ✍️ |
| E-4 | Re-pairing after removal | ✅ | ✍️ | ✍️ |
| E-5 | Caregiver reviews in a pairing-scoped subcollection, first resolution wins | ✅ | ✍️ | ✍️ |
| E-6 | Code generation blocked at 2 caregivers | ✅ | ✍️ | ✍️ |
| E-7 | Gender / Birthdate | **dropped by decision** | — | — |
| E-8 | Caregiver push (≤ 30 s, GPS via push) | ❌ not started | ❌ | ❌ |
| E-9 | Watch Test Mode off by default, debug-only | ✅ | ✍️ (watch service) | — |
| E-10 | SOS GPS ping works end-to-end | ❌ (dead path on Android; iOS implements it over BLE) | ❌ | ✍️ |

✅ run · ✍️ written, not built/run · ❌ not done.

---

## J. Decisions recorded — round 3 (product owner)

| Decision | Effect |
|---|---|
| **2-in-1 app.** One account can be a **wearer and a caregiver at the same time**. A wearer registers **up to 2 caregivers**, who can observe them. | **Supersedes** 07 *Role Selection* ("role immutable, switch by new account"), my earlier Option A, the "separate accounts on separate devices" assumption in 01/07, and the role-switching proposal (10). The role matrix becomes a per-**mode** matrix; a user's *modes* are additive. See 07/09 amendments below. |
| Self-pairing is impossible (you cannot be your own caregiver). | New rule + test |
| A caregiver follows **one** wearer (unchanged default). | Open: say if a caregiver should follow several |
| **Push notifications via FCM** on Android **and** iPhone. | New doc 12 |
| **SOS GPS** on both platforms. | Doc 12 §4 |
| **One-clock fix** for Android's Data Layer path. | Doc 08 §4.2b (estimator) |
| **Design Language System**: one HTML reference + one tokens file; code reskinnable from it. | New doc 13 |
| Use the `improve-codebase-architecture` skill for code organization. | Report delivered; **stopped for your choice of candidate** (see `architecture-review.html`) |

### Round 3 — status (see `11_TDD_PROGRESS.md`)

| Item | State |
|---|---|
| One-clock fix, Android | ✅ `ClockSkewEstimator` tested (4); ✍️ wired in the receiver (not built) |
| SOS GPS, Android | ✍️ watch answers `/sos_gps_request`; phone consumes `/sos_location`; 60 s cooldown (`Cooldown`, 2 tests); fake address removed (not built) |
| SOS GPS, iPhone | ✅ already worked over BLE (unchanged) |
| Push: decision logic + Cloud Function handler | ✅ 18 JS tests run |
| Push: Android/iPhone FCM clients + the call that triggers it | ❌ **not written** |
| DLS: tokens, generator (17 tests), HTML page, bridge for colours, guard | ✅/✍️ (see 13 §5) |
| Architecture review | ✅ report delivered — **waiting for you to pick a candidate** |
| **2-in-1 (dual role)** — spec amended; code | ❌ **not started**: needs the Account-modes decision above (candidate 1); `RoleCapabilities`, `AppViewModel`, navigation, rules and the boundary tests all change |

