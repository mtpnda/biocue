# 07. Multi-Account Architecture

## Overview
BioCue supports two account roles: **Wearer** and **Caregiver**. They are separate Firebase Auth accounts on separate physical devices, linked via a consent-based pairing protocol.

## Role Selection
- **2-in-1 (amended):** an account holds one or both **modes** — WEARER and CAREGIVER — stored in `users/{uid}.modes` (array). Existing accounts migrate from `role` to `modes: [role]`. A user can add the other mode at any time; the app shows a **My Circle** (wearer) area and a **Following** (caregiver) area for the modes they have.
- A mode only ever grants that mode's capabilities (see 09 §3); the two areas share no state. Data access is determined by the modes **and** by pairings.
- A user **cannot pair with themselves** (a code lookup that resolves to your own uid is refused).
- *(Superseded text, kept for the record: "Role is chosen once… cannot be changed… to switch, create a new account.")*

## Pairing Protocol

### Code Generation (Wearer)
1. Wearer taps "Generate Pairing Code" on their phone.
2. A random 6-digit numeric code is written to `users/{uid}.pairingCode` with a 10-minute TTL (`pairingCodeExpiresAt`), and to `active_codes/{code}` (`wearerUid`, `expiresAt`) so it can be looked up directly.
3. The code is displayed on-screen with a countdown timer.
4. Code is single-use: once a caregiver submits it, the code is consumed. The wearer's app consumes it as soon as it observes the first `PENDING` request (deletes `active_codes/{code}` and clears `users/{uid}.pairingCode*`); the 10-minute TTL is the backstop.

### Code Submission (Caregiver)
1. Caregiver enters the 6-digit code.
2. App reads `active_codes/{input}` with a direct `get` (**there is no list or query access on `users` or `active_codes`**), checks `expiresAt > now`, then reads `users/{wearerUid}`.
3. If found → creates a `pairings/{wearerUid}_{caregiverUid}` document with `status: PENDING`. If that document already exists with `status: REMOVED`, the caregiver sets it back to `PENDING` instead (see *Re-pairing*).
4. If not found → shows "Code not found or expired".

### Approval (Wearer)
1. Wearer's snapshot listener detects the new PENDING pairing.
2. Wearer sees: "Allow [Caregiver Name] to connect?"
3. On Approve → `status: APPROVED`. Caregiver gains read access.
4. On Deny → document is deleted.

### Limits
- Max 2 APPROVED caregivers per wearer.
- If wearer already has 2, the pairing screen shows "Maximum caregivers reached" and blocks new code generation.

## Data Sync

### What the Wearer syncs
The wearer's phone syncs strictly on an event-driven basis to preserve privacy and cloud cost:
- **Stress events** → `wearerData/{uid}/events/{eventId}`: `startTime`, `endTime`, `triggerType`, `label`, `isResolved`

### What stays local
- Current real-time status (stress %, live activity, tier)
- Raw sensor windows (HR, HRV, accelerometer values)
- GPS coordinates (shared only via push notification)
- ML model weights and calibration data

### Caregiver reads
- Firestore snapshot listeners on `wearerData/{wearerUid}/events/`
- Read-only. Caregivers cannot write to wearer data.
- Review submission (`resolveReviewEvent`): the caregiver writes `pairings/{pairingId}/reviews/{eventId}` = {`label`, `isResolved`, `updatedAt`}. Only the caregiver of an `APPROVED` pairing may write it; the wearer reads it. The wearer's app applies a review to an event **only while that event is unresolved** (the first resolution wins; later reviews are ignored). The caregiver's journal merges the wearer's events with its own reviews for display.

## Removal & Data Wipe

### Trigger
Wearer taps "Remove" on a connected caregiver.

### Sequence
1. `pairings/{id}.status` → `REMOVED`, `removedAt` → `now`
2. Caregiver's snapshot listener fires immediately (the caregiver observes **all** statuses of its own pairings, not only `APPROVED`, so a removal is seen)
3. Caregiver app:
   - Wipes all locally cached wearer data (`SharedPreferences`, in-memory state)
   - Navigates to `AccessRevokedScreen`
4. Firestore security rules (`isApprovedCaregiver()`) now return `false` for this caregiver
5. Any subsequent read attempt by the caregiver is blocked by security rules

### Re-pairing
After removal, the wearer can generate a new code and the same caregiver can re-pair. The pairing document keeps its id; the caregiver sets `status` back to `PENDING` and clears `removedAt`. Approval by the wearer is required again.

## Firestore Collections

| Collection | Document ID | Fields | Access |
|---|---|---|---|
| `users` | Firebase UID | `role`, `displayName`, `pairingCode?`, `pairingCodeExpiresAt?`, `createdAt` | Owner read/write. Any signed-in user may **get** a profile by uid (needed after a code lookup); **no list** |
| `active_codes` | The 6-digit code | `wearerUid`, `expiresAt` | Owning wearer write/delete; any signed-in user **get** only; **no list** |
| `pairings` | `{wearerUid}_{caregiverUid}` | `wearerUid`, `caregiverUid`, `caregiverName`, `wearerName`, `status`, `createdAt`, `removedAt?` | Wearer read/write, Caregiver read |
| `wearerData/{uid}/events` | Event UUID | `startTime`, `endTime?`, `triggerType`, `label`, `isResolved` | Wearer write, Approved caregiver read (caregivers never write here) |
| `pairings/{id}/reviews` | Event UUID | `label`, `isResolved`, `updatedAt` | Caregiver of an APPROVED pairing write; wearer and that caregiver read |

## Security Rules
See `firestore.rules` and `firestore.indexes.json` in project root. 
These files natively enforce the multi-account restrictions on Google's servers. 
Deployment is managed via the Firebase CLI using:
`firebase deploy --only firestore`

---

## Amendments (merge, approved)

* *Code lookup:* direct `get` on `active_codes/{code}`; no list/query on `users` or `active_codes` (the original text queried `users`, which its own access table forbade).
* *Single-use:* defined how the code is consumed.
* *Review submission:* defined the pairing-scoped `reviews` subcollection and the "first resolution wins" rule (the original left "optionally import" open).
* *Removal:* the caregiver observes every status of its own pairings.
* *Re-pairing:* same document id, `REMOVED → PENDING` (the original said "a new pairing document", which the deterministic id and the rules do not allow).
* *Collections:* added `active_codes` and `reviews`.
* *Unchanged by decision:* **role immutability** — roles cannot be changed; to switch, create a new account (Option A; see 00 §A).
