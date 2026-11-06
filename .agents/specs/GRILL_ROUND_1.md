# Grill — Round 1 (frontier)

> **UPDATE — reassessed against your specs 01, 03–07.** Each question now has a **📎 Spec check** line. Where your spec answers it, the spec wins and my recommendation is dropped; the full table, conflicts and the one open decision (role switching) are in [`00_MERGE_LOG.md`](00_MERGE_LOG.md). Round 2 and the seams were revised there (§B, §F).

Specs under review (original round): my earlier `SPEC.md` v0.3 and connection doc, now [`09`](09_IOS_AND_ROLE_ENFORCEMENT.md), [`10`](10_PROPOSAL_ROLE_SWITCHING.md) and [`08`](08_WATCH_PHONE_CONNECTION.md).
Method: the `grilling` skill. Every question has my recommended answer, worded so **"yes" accepts it**. Reply `yes to all`, or list the numbers you want to change.

> **Honest note on "another agent".** I cannot launch a separate agent in this environment (there is no sub-agent tool), and the skill itself says the *decisions* are the user's. So this is one adversarial pass **by me**, written from the specs + code with the goal of breaking them — not an independent review. For real independence, run `grill-me` in a fresh agent session against `SPEC.md` and compare. Facts below were checked against the files (listed next); the recommendations are judgment.

## Facts verified before asking (so you don't have to)

| # | Fact | Where |
|---|---|---|
| F1 | `users` and `active_codes` use `allow read`, which includes **list**. Any signed-in account can fetch *every* user and *every* live pairing code. **This is a flaw in my own rules**, and the spec's §9 item 9 understated it. | `firestore.rules` lines 26, 58 |
| F2 | Android's `findWearerByCode` has a fallback query over `users` by `pairingCode`; that is the only thing that needs `list` on `users`. | `FirestoreRepository.kt:107` |
| F3 | **No code path ever consumes a pairing code**, yet both apps say "Single-use". Within 10 minutes several people can use one code. | grep of Android + iOS; UI strings |
| F4 | The phone mixes **two clocks**: event/window timestamps come from the *watch*, while "is the event still ongoing?" uses the *phone's* `now`. A watch clock off by >15 min silently breaks orphan detection. | `WatchDataReceiverService.kt:104,183`; `WearerService.swift:100–142` |
| F5 | The watch advertises the service UUID **permanently** while disconnected, even once bonded. Anyone scanning can tell "a BioCue watch is nearby". | `BleLinkServer.kt:228` |
| F6 | Synthetic stress readings are **on by default** in the watch service. | `SensorCollectionService.kt:108` |
| F7 | There is no account deletion anywhere. | grep |
| F8 | The Bluetooth outbox is in memory; a watch reboot loses un-ACKed events. | `LinkOutbox.kt` |

---

❓ **Q1** - **Scope of the TDD pass**: For this rewrite, do we implement **only the spec's changes** — role switching (now a *proposal*, doc 10), the rules hardening below, and the connection fixes below — each test-first, and leave already-working code alone? Explicitly **out of scope** for now: caregiver push notifications, real HRV from inter-beat intervals, per-user baselines, account deletion UI, localization. (Rewriting code that already works under tests would not be TDD — the tests would be written after the fact.)

➡️ Yes. Spec the out-of-scope items now (a short "contract" for push and deletion), build them later.

📎 **Spec check:** **ANSWERED IN PART.** Your specs add requirements the code misses (00 §E). Scope = those gaps + my additions. Caregiver push is a *requirement* (01, 04), so it is *sequenced after*, not optional.

---

❓ **Q2** - **One account, one role at a time**: Do we keep "an account is exactly one role at a time", so someone who is both a wearer *and* a caregiver for someone else uses two accounts (or switches, ending all connections each time)? The alternative — an account holding both roles at once — roughly doubles every rule, screen and test, and breaks the clean "wearer ⟂ caregiver" boundary.

➡️ Yes. Document that dual-role people use two accounts. Revisit only if real users ask for it.

📎 **Spec check:** **ANSWERED — yes.** 07/01: one role per account, separate accounts on separate devices. (Subject to the role-switching decision, 00 §A.)

---

❓ **Q3** - **Stop listing users and codes** (fixes F1/F2): Do we change the rules so `users` and `active_codes` allow **get only** (a lookup by a known id), never **list**, and delete Android's fallback query? A caregiver can still look a wearer up: code → `active_codes/{code}` → `users/{wearerUid}`.

➡️ Yes. This is the smallest change that closes the directory leak, and the only code that depended on listing is the legacy fallback.

📎 **Spec check:** **CONFLICT INSIDE 07.** It says the caregiver *queries `users`* but its own table makes `users` owner-only. Recommendation kept (it matches the table); needs approval as an amendment (00 §D-1).

---

❓ **Q4** - **Make pairing codes actually single-use, and show who is asking** (fixes F3): Do we (a) have the wearer's app delete the code as soon as the first pairing request appears, and (b) show the wearer the requester's **email address** (not only a self-typed name) with "Someone entered your code" on the approval card? (Server-side consumption isn't possible with the current rules; (a) plus the 10-minute expiry is the practical fix. Not adopting App Check / longer codes now.)

➡️ Yes to (a) and (b). Record App Check and a longer code as a later hardening item.

📎 **Spec check:** **(a) ANSWERED — yes:** 07 says the code is single-use and consumed (the code doesn't do this). **(b) REJECTED:** 04/07 show `Allow [Caregiver Name]`, and identity is *name, never email*. Names only.

---

❓ **Q5** - **One clock** (fixes F4): Do we change the protocol so the watch reports its current time in `HELLO_ACK`, the phone computes a clock offset, and all watch timestamps are converted to **phone time** on arrival (and on Android's Data Layer path, stamp on receipt)? Link v1 has not shipped anywhere, so changing the wire format now costs only regenerated test vectors.

➡️ Yes. Add `watchTimeMs` to `HELLO_ACK`; regenerate vectors from `tools/gen_link_vectors.py` first.

📎 **Spec check:** **NOT ANSWERED** by your specs → recommendation stands.

---

❓ **Q6** - **Don't advertise a fingerprint** (fixes F5): Do we advertise the service UUID **only during the 120-second pairing window**, and advertise with no service data once the phone is bonded (iOS reconnects to a bonded watch by its identity, not by scanning for a UUID)? This matters more than usual: detecting a BioCue watch implies something sensitive about its wearer.

➡️ Yes. The hardware spike must confirm iOS still reconnects without the UUID in the advertisement; if it doesn't, we revisit rather than quietly reverting.

📎 **Spec check:** **NOT ANSWERED** (your specs have no iPhone/BLE path) → recommendation stands.

---

❓ **Q7** - **Fake data off by default** (fixes F6): Do we make the synthetic-reading mode **debug-builds-only and off unless explicitly switched on**, so a release build can never feed invented stress readings into a system that alerts caregivers?

➡️ Yes.

📎 **Spec check:** **MOSTLY ANSWERED:** 03 says Test Mode is a toggle ("when active") ⇒ default off; the code turns it on. Release-build hiding is my addition.

---

❓ **Q8** - **Who operates a child's account**: Do we document the assumption that **the wearer's account is operated by the wearer or their guardian on the wearer's own phone** — there is no separate "guardian-managed" mode, and the wearer's account is the one that approves caregivers? (This leaves open the legal side — consent and children's-privacy rules — which needs a decision from someone qualified, not from this spec.)

➡️ Yes, as a documented assumption, with consent/children's-privacy flagged as an open legal item.

📎 **Spec check:** **ANSWERED as an assumption:** 01 has the wearer run the app on their own phone. Legal side stays open.

---

❓ **Q9** - **Wearer and caregiver both review the same event**: Do we add a rule that a caregiver may edit an event **only while it is unresolved** (the wearer, as owner, can always edit), so a finished review can't be silently overwritten by the other party? Concurrent edits to an *unresolved* event stay last-writer-wins.

➡️ Yes.

📎 **Spec check:** **SUPERSEDED.** 07 makes caregivers read-only on wearer data; reviews go to a pairing-scoped subcollection the wearer app may import. My premise (editing the event) is not the spec's design — see 00 §D-2.

---

❓ **Q10** - **Don't lose events on a watch reboot** (fixes F8): Do we persist the watch's un-ACKed critical frames to a small file (rewritten on change, cap 64) and reload them at start, instead of keeping them only in memory?

➡️ Yes. Events are the product's core record; the file is tiny. Keep the "oldest dropped when full" rule.

📎 **Spec check:** **NOT ANSWERED** → recommendation stands.

---

❓ **Q11** - **What counts as "tests passed" here**: I can run **Kotlin** tests for real in this environment, but not Swift (no toolchain) and not the Firestore rules (no emulator). Do we accept that real red→green TDD applies to the Kotlin slices, while Swift and rules tests are written first but labelled **"written, not executed"** everywhere (spec, summaries), and never described as passing?

➡️ Yes. I will not claim a Swift or rules test passes until you've run it.

📎 **Spec check:** Process, not a spec matter. 05 only lists `./gradlew test` / `connectedAndroidTest` → extend 05 (00 §D-6).

---

❓ **Q12** - **Stop the specs drifting from the code**: Do we add one script (`tools/verify.sh`) that regenerates the protocol vectors and detection fixtures, fails if they differ from what's committed, runs the Kotlin tests and the role-switch model check — so a spec or protocol change that isn't reflected everywhere fails loudly?

➡️ Yes.

📎 **Spec check:** **NOT ANSWERED** → recommendation stands; add to 05.

---

❓ **Q13** - **Limits**: Do we confirm and document: a caregiver follows **exactly one** wearer, a wearer has **at most 2** caregivers, and the 2-caregiver limit is enforced **only in the wearer's app** (rules can't count documents), so a modified client could exceed it — which only affects that wearer's own circle?

➡️ Yes. Documented as intentional.

📎 **Spec check:** **ANSWERED — yes:** 01/07 give ≤ 2 approved caregivers and say code generation is *blocked* at 2; a caregiver is paired to *a* wearer.

---

❓ **Q14** - **If the watch can't do secure pairing**: Do we keep "encrypted + MITM-protected (numeric comparison) pairing is mandatory", and if the hardware test shows a Galaxy Watch can't do it, we **stop and decide together** rather than quietly weakening to "Just Works"?

➡️ Yes. Weakening pairing for a device that protects a vulnerable person's data is a decision, not a fallback.

📎 **Spec check:** **NOT ANSWERED** (no BLE in your specs) → recommendation stands.

---

## Round 2 — blocked until the answers above (not asked yet)

These depend on **Q1** (scope), **Q2** (single role) and **Q3–Q5**, so by the skill they come next. Pre-answer with "yes" if you agree and we can skip a round:

| # | Question | My recommendation |
|---|---|---|
| R2-1 | Close a wearer's ongoing event at the moment of a role switch (otherwise it stays "ongoing" forever)? | Yes — include it in the atomic batch |
| R2-2 | A switch uses a batch that may sit **queued** if the phone is offline and apply later. Accept "may complete later; UI follows the server's profile" after a pre-check read and a timeout? | Yes |
| R2-3 | Accept the extra rule reads from the epoch check (≤ 5 per rule evaluation) as a cost? | Yes |
| R2-4 | Account deletion: specify the contract now (what is deleted, what happens to a wearer's caregivers), implement later? Apple requires in-app deletion for apps with sign-up. | Yes |
| R2-5 | Caregiver push: specify the contract now (Cloud Function trigger on event creation → FCM/APNs, payload with no health detail)? | Yes |
| R2-6 | **TDD seams to confirm (required by the `tdd` skill before any test is written)** — see below | Yes |

### Proposed seams (R2-6) — each with what it catches and what it misses

| Seam (public interface) | Language | Catches | Misses |
|---|---|---|---|
| **`RoleSwitchService.switchRole(account, target)`** over an `AccountCloud` port (faked at the boundary) | Kotlin — *runnable here* | Right batch contents (role, epoch+1, code cleared, all pairings removed, ongoing event closed); failure leaves nothing changed; offline refused; stale pairings hidden | Real Firestore behaviour |
| **`PairingVisibility.visible(pairings, ownEpoch, side)`** | Kotlin — *runnable* | Stale/mid-switch pairings never listed or counted | UI rendering |
| **`WatchClock` offset** (`HelloAck.watchTimeMs` → phone time) and `LinkCodec` v1 vectors | Kotlin — *runnable* + Python reference | Skew, boundary at the 15-minute orphan edge, wire bytes | Real watch clocks |
| **`AdvertisePolicy.payload(bonded, pairingOpen)`** | Kotlin — *runnable* | UUID only in the pairing window | Whether iOS really reconnects (hardware) |
| **`LinkOutbox` persistence** (`save`/`load` via a file port) | Kotlin — *runnable* | Reboot survival, cap, ordering, corrupt file | Flash wear / real reboot |
| **`firestore.rules`** via the emulator | JS — *written, not executable here* | Doc 10 §7 (if adopted), listing lockdown, the review-subcollection rules (see 00 §D-2; "caregiver-edit-while-unresolved" is superseded) | — (it is the real thing; I just can't run it) |
| **`RememberedWatchStore(account)`** and `SessionStore.switchRole` | Swift — *written, not executable here* | Per-account watch memory, switch routing | Anything needing a compiler |

I will not write a test at any seam you haven't confirmed.
