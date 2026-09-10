# 07. Multi-Account Architecture

## Overview
BioCue supports two account roles: **Wearer** and **Caregiver**. They are separate Firebase Auth accounts on separate physical devices, linked via a consent-based pairing protocol.

## Role Selection
- Role is chosen once at first login and stored in Firestore `users/{uid}.role`.
- Role cannot be changed after selection. To switch, the user must create a new account.
- The app's navigation, UI, and data access are entirely determined by the stored role.

## Pairing Protocol

### Code Generation (Wearer)
1. Wearer taps "Generate Pairing Code" on their phone.
2. A random 6-digit numeric code is written to `users/{uid}.pairingCode` with a 10-minute TTL (`pairingCodeExpiresAt`).
3. The code is displayed on-screen with a countdown timer.
4. Code is single-use: once a caregiver submits it, the code is consumed.

### Code Submission (Caregiver)
1. Caregiver enters the 6-digit code.
2. App queries Firestore for `users` where `pairingCode == input` AND `pairingCodeExpiresAt > now`.
3. If found → creates a `pairings/{wearerUid}_{caregiverUid}` document with `status: PENDING`.
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
- Exception: `resolveReviewEvent` — caregiver writes label/context to a pairing-scoped subcollection, which the wearer's app can optionally import.

## Removal & Data Wipe

### Trigger
Wearer taps "Remove" on a connected caregiver.

### Sequence
1. `pairings/{id}.status` → `REMOVED`, `removedAt` → `now`
2. Caregiver's snapshot listener fires immediately
3. Caregiver app:
   - Wipes all locally cached wearer data (`SharedPreferences`, in-memory state)
   - Navigates to `AccessRevokedScreen`
4. Firestore security rules (`isApprovedCaregiver()`) now return `false` for this caregiver
5. Any subsequent read attempt by the caregiver is blocked by security rules

### Re-pairing
After removal, the wearer can generate a new code and the same caregiver can re-pair. This creates a new pairing document.

## Firestore Collections

| Collection | Document ID | Fields | Access |
|---|---|---|---|
| `users` | Firebase UID | `role`, `displayName`, `pairingCode?`, `pairingCodeExpiresAt?`, `createdAt` | Owner read/write |
| `pairings` | `{wearerUid}_{caregiverUid}` | `wearerUid`, `caregiverUid`, `caregiverName`, `wearerName`, `status`, `createdAt`, `removedAt?` | Wearer read/write, Caregiver read |
| `wearerData/{uid}/events` | Event UUID | `startTime`, `endTime?`, `triggerType`, `label`, `isResolved` | Wearer write, Approved caregiver read |

## Security Rules
See `firestore.rules` and `firestore.indexes.json` in project root. 
These files natively enforce the multi-account restrictions on Google's servers. 
Deployment is managed via the Firebase CLI using:
`firebase deploy --only firestore`
