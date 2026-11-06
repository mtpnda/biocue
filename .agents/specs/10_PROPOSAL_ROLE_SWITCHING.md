# 10. PROPOSAL — Switching roles

> **STATUS: SUPERSEDED & REJECTED — Replaced by the 2-in-1 Account Model (Round 3 in 00_MERGE_LOG / doc 07 / doc 09).**
> Under the 2-in-1 model, one account holds both `WEARER` and `CAREGIVER` modes concurrently in `users/{uid}.modes` (no destructive role wiping or switching necessary).
> Kept for archival and historic decision record only.

## Options

| | Option | Cost | Risk |
|---|---|---|---|
| **A** | Keep 07: role is permanent; to switch, create a new account | none (already built) | user who chose wrong must start over, losing history |
| **B** | Adopt this proposal: switch from Settings; ends all pairings; epoch counter invalidates old pairings | rules + 2 clients + tests (§6) | most moving parts; touches the access-control rules |
| **C** | Narrow switch: allowed only while the account has **no events and no pairings** (fixes the "I picked wrong" case) | small client change + one rule | does not help an account that already has history |

## 1. Requirements

| # | Requirement |
|---|---|
| R1 | Any signed-in user can switch role from **Profile → Switch role**. |
| R2 | **Pairing stays the only route to wearer data.** A switch must never create, keep, or revive a caregiver's access without a fresh approval by the wearer. |
| R3 | Switching **ends every pairing** the account has (as a wearer: all caregivers; as a caregiver: the wearer being followed). |
| R4 | **Fail closed.** If a switch cannot be completed in full, nothing changes — no half-switched account. |
| R5 | A pairing made *before* a switch can **never become valid again**, even if the account later switches back. |
| R6 | A switch needs a connection. The app never shows the new role until the server has confirmed it. |
| R7 | The user is told exactly what will happen **before** they confirm. |

## 2. What happens to what

| | Wearer → Caregiver | Caregiver → Wearer |
|---|---|---|
| Pairings | Every caregiver is disconnected at once; each pairing is set to `REMOVED` | The pairing with the followed wearer is ended (`REMOVED`) |
| Access to the wearer's data | Former caregivers lose it **immediately** | The caregiver loses it **immediately** |
| Pairing code | Any live code is cleared (`users.pairingCode*` and its `active_codes` doc) | — |
| Wearer's events in the cloud | **Kept**, still owned by the account, readable by nobody else. If the account switches back they are the wearer's again — and former caregivers do **not** regain access | — |
| Watch link | Stops. Android: the cached role becomes CAREGIVER so the receiver drops data. iOS: the Bluetooth link is cancelled and released. (The watch also refuses a caregiver HELLO.) | If the account becomes a wearer it must connect a watch (a previously remembered watch for *this account* resumes) |
| Local raw vitals / local event log | Left in place under the existing retention; nothing in the caregiver experience can read them | — |
| Caregiver's cached copy of the wearer's events | — | Purged, best effort (§5) |
| App state | Role-specific objects, listeners and screens are discarded; the other role's home is built fresh | same |

Events are **not** deleted on a switch: deletion is irreversible and surprising, and nothing is exposed by keeping them. (A future "delete my data" feature is separate.)

## 3. User flow

1. **Profile** shows a row: *Switch to Caregiver* / *Switch to Wearer* — subtitle "Ends all connections".
2. A **confirmation screen** states the consequences for that direction, using real names/counts, e.g. wearer: "Your 2 caregivers (Maya, Jordan) will be disconnected and lose access now. Your watch will stop sending to this phone. Your history is kept but nobody else can see it. To follow someone you'll enter their pairing code." Caregiver: "You will stop following Sam and won't see their updates. You'll get a pairing code to share with caregivers." Two buttons: **Cancel** (default) and a destructive **Switch and disconnect**. If the wearer has events awaiting review the screen says they are kept.
3. **Switching…** spinner; the confirm button is disabled so it cannot run twice.
4. **Success** → the other role's home (a new wearer sees an empty "My Circle"; a new caregiver sees "Join a circle").
5. **Failure** → stays in the current role with a plain reason ("Check your connection and try again."). Nothing has changed (R4).

No blocking conditions: an ongoing event does not prevent a switch.

## 4. Server design (no backend needed)

The rules cannot list a user's pairings or run clean-up code. Instead, **one counter on the account invalidates all of its pairings at once.**

* `users/{uid}.roleEpoch` — integer, missing = 0. Incremented on every role change.
* `pairings/{…}.wearerEpoch` and `.caregiverEpoch` — each account's `roleEpoch` **at the moment the request was made**. Missing = 0.

Rules (target):

```
function epochOf(uid)  { return get(userDoc(uid)).data.get("roleEpoch", 0); }
function approvedPairing(w, c) {                     // used by every pairing-gated read/write
  let p = /databases/$(database)/documents/pairings/$(w + "_" + c);
  return exists(p) && get(p).data.status == "APPROVED"
      && get(p).data.get("wearerEpoch", 0)    == epochOf(w)
      && get(p).data.get("caregiverEpoch", 0) == epochOf(c);
}
// users: ordinary edit keeps role AND roleEpoch; a switch must bump the epoch by exactly 1,
//        pick a valid role, and clear the pairing code. create: roleEpoch must be absent or 0. delete: denied.
// pairings create: as today, plus wearerEpoch == epochOf(wearer) and caregiverEpoch == epochOf(caller).
```

Consequences:

* The instant a role change is written, every pairing the account ever had stops granting access — **even if no clean-up ran**, and **even one that lands mid-switch** (a request that arrives during the switch carries the old epoch and is born invalid). That is what makes R5 hold for a switch-away-and-back.
* Cost: at most 5 `get()`/`exists()` per rule evaluation (limit 10).
* **Backward compatible:** missing fields count as 0, so existing accounts and pairings keep working until their first switch. An older client that doesn't write epochs still pairs fine for a never-switched account and simply fails closed for a switched one.

**Client write = one atomic batch** (all-or-nothing, R4), preceded by a *server-sourced* read of the account's pairings (fails when offline, R6):

1. `users/{uid}`: `role`, `roleEpoch + 1`, `pairingCode = null`, `pairingCodeExpiresAt = null`
2. every pairing of this account: `status = REMOVED` (the rules already allow the wearer or the caregiver to do this on their own pairing) — this is for the *other person's* screen; access is already gone via step 1
3. delete `active_codes/{code}` if a live code exists

*Considered and rejected:* a Cloud Function (needs a backend this project doesn't have; the stronger design if one is added later) · deleting events on switch (irreversible) · keeping pairings across a switch (violates R2/R5) · a cooldown between switches (not enforceable in rules; the confirmation screen covers accidental taps).

## 5. Client obligations

* **Ignore stale pairings.** A pairing whose epoch for *this* account differs from the account's current `roleEpoch` can never grant access (§4), but a request that arrived during a switch may still sit there as `PENDING`. Clients must **not list it** (not as a pending request, not as a connected caregiver, not toward the 2-caregiver limit) and may delete or `REMOVE` it. Without this, a wearer could tap *Approve* on a stale request and see a caregiver who has no access. (The model check below finds exactly this sequence.)
* **Route on server-confirmed state only.** While a switch is pending the UI shows "Switching…"; it never routes on a locally-cached, not-yet-confirmed role.
* **Android:** `AppViewModel.switchRole()` performs 4.4.4; `RoleStore` is updated only after confirmation; hosts are rebuilt. **Cache purge:** Firestore's offline cache cannot be selectively cleared while in use, so a caregiver→wearer switch (and a removal) sets a flag and the app calls `clearPersistence()` at the next cold start *before* any Firestore access. Until then the cached copy remains on the device — a stated limitation.
* **iOS:** account management is its own interface (`AccountCloud`: profile + `switchRole`), separate from `WearerCloud` / `CaregiverCloud`; `SessionStore.switchRole(to:)` drives it. `WatchLinkCentral` gains `stopAndRelease()` (cancel connection and scan, then release) and the **remembered watch is stored per account** (§8, §9). The role picker no longer says "permanent".
* **Watch:** no change; the HELLO role check already refuses a caregiver phone.

## 6. What must change in the current build

| Area | Change |
|---|---|
| `firestore.rules` | Replace "role immutable" with the epoch rule; add epochs to pairing create and `approvedPairing` (§4) |
| `firestore-rules-tests` | Add the cases in §7 |
| Android | `UserProfile.roleEpoch`; `Pairing.wearerEpoch/caregiverEpoch`; `requestPairing` writes epochs; `AppViewModel.switchRole`; Profile row + confirmation screen + strings; `RoleStore` update; cache-purge flag; tests |
| iOS | `UserProfile.roleEpoch`; `Pairing` epochs + parsing; `FirebaseCloud.requestPairing` writes epochs; `AccountCloud.switchRole`; `SessionStore.switchRole`; Profile row + confirmation view; `WatchLinkCentral.stopAndRelease`, per-account remembered watch; remove "permanent" copy; tests |
| Docs | Amend doc 07 (§8 here); update doc 09 §4–§6; doc 08 §7 is unchanged (the link's role rule does not depend on switching) |

## 7. Acceptance tests

*Rules (emulator):* a role change without an epoch bump is denied · an epoch bump without a role change is denied · an epoch bump of +2 is denied · a switch with a live pairing code left in place is denied · profile create with `roleEpoch ≠ 0` is denied · **after a wearer's switch, a previously-approved caregiver can no longer read, even though the pairing document still says `APPROVED`** · after switching away **and back**, that caregiver still cannot read · a brand-new pairing after the switch works only once the wearer approves it · pairing create with wrong epochs is denied · after a caregiver's switch their old pairing grants nothing · the atomic batch applies fully or not at all.

*Design-level check (**run**):* `tools/model_check_role_switch.py` exhaustively explores every interleaving of switch / request / approve / remove by both accounts (470 reachable states, depth 9) and asserts "a caregiver can read ⇒ the wearer approved under the current epochs of both accounts". It holds **even when the clients' `REMOVED` clean-up never runs**, and it **fails in three steps** (`C.request → W.switch → W.approve`) when the epoch check is removed, so the check can catch a real hole. It models the rule *logic*, not Firestore itself — the emulator tests above are still required.

*Client (unit):* stale pairings are not listed or counted · success path ends all pairings, bumps the epoch, clears the code, updates local role state · any cloud failure leaves the role unchanged and surfaces an error · an offline server read refuses the switch · confirmation text contains the real names/count · the UI does not route until the server confirms · stale snapshots from the previous role are ignored · iOS: the link is stopped and the remembered watch is per account.

*Manual, two devices:* wearer with an approved caregiver switches → the caregiver's app shows "Access removed" within seconds and cannot read; the wearer switches back → the old caregiver **still** cannot read; re-pairing with a new code and approval works · caregiver switches to wearer → the wearer's circle no longer lists them · a switch attempted offline is refused · force-quitting mid-switch leaves one consistent state (the server's).

## 8. Amendments to doc 07 required if this is adopted

* **Role Selection:** replace "Role cannot be changed after selection. To switch, the user must create a new account." with a reference to this document.
* **Firestore Collections → `users`:** add `roleEpoch`. **`pairings`:** add `wearerEpoch`, `caregiverEpoch`.
* **Security Rules:** role may change only together with a `roleEpoch` bump (§4); `approvedPairing()` additionally compares epochs.
* **01 Account Model:** "Role … is chosen at first login" → "…and may be switched from Settings (see 10)".


> **Superseded (round 3):** the product owner chose a **2-in-1 model** (an account can hold both modes at once), which removes the need for switching. See `00_MERGE_LOG.md` §J.
