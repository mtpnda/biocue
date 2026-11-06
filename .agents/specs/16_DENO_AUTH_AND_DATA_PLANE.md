# 16. Deno Edge Auth & Data Plane (Firebase Replacement)

> **Normative specification.** Replaces Firebase Auth, Cloud Firestore, and Cloud Functions with a unified **Deno Edge (Hono + Deno KV)** architecture.
> **Clients:** iOS (Swift, `ASWebAuthenticationSession` + standard `URLSession`), Android (Kotlin, `CustomTabs` + standard HTTP), and Web (`biocue.app`).

---

## 1. Architectural Motivation

1. **Zero SDK Bloat & Crash Elimination**:
   - `firebase-ios-sdk` introduces 18+ heavy transitive C++/binary packages (`gRPC`, `leveldb`, `nanopb`, `AppAuth`).
   - Removing Firebase saves >60MB of binary size and reduces iOS clean build time by over 75%.
   - Eliminates fragile platform files (`GoogleService-Info.plist`, `google-services.json`).

2. **Unified Sovereign Backend**:
   - Single Deno runtime powering both the public website and the real-time API.
   - Deno KV provides zero-cold-start, ACID transactional storage with native atomic mutations (`kv.atomic()`).
   - Live synchronization via Server-Sent Events (`kv.watch()`) and WebSockets, completely replacing Firestore listeners.

3. **Web-Based Sign-In (RFC 8252)**:
   - Authentication is performed via system-sandboxed web authentication (`ASWebAuthenticationSession` on iOS, Custom Tabs on Android).
   - The app never sees Google passwords or cookies; it receives a cryptographically signed BioCue session JWT from the Deno backend via the custom URL scheme `biocue://auth/callback?token=<JWT>`.
   - Complies with Apple Guideline 4.8 by supporting both Google and Apple OIDC on the Deno server.

---

## 2. Deno KV Storage Schema

All keys are structured hierarchically:

| Namespace | Key Pattern | Value Schema |
| :--- | :--- | :--- |
| **Users** | `["users", uid]` | `UserProfile { uid, email, displayName, photoUrl?, modes: ("WEARER" \| "CAREGIVER")[], pairingCode?, pairingCodeExpiresAt?, createdAt, updatedAt }` |
| **Pairing Codes** | `["pairing_codes", code]` | `PairingCodeEntry { code: string, wearerUid: string, wearerName: string, expiresAt: number }` (TTL: 15 min) |
| **Pairings** | `["pairings", pairingId]` | `Pairing { id, wearerUid, caregiverUid, wearerName, caregiverName, status: "APPROVED" \| "PENDING" \| "REMOVED", createdAt, removedAt? }` |
| **Pairings by Wearer** | `["pairings_by_wearer", wearerUid, pairingId]` | `true` |
| **Pairings by Caregiver** | `["pairings_by_caregiver", caregiverUid, pairingId]` | `true` |
| **Events** | `["events", wearerUid, eventId]` | `StressEvent { id, userId, startTime, endTime?, triggerType, label, isResolved }` |
| **Event Reviews** | `["reviews", pairingId, reviewId]` | `CaregiverReview { id, pairingId, eventId, caregiverUid, note, createdAt }` |
| **User Devices** | `["devices", uid, deviceId]` | `DeviceRegistration { id, uid, token, platform: "ios" \| "android", updatedAt }` |
| **Push Log** | `["push_logs", wearerUid, eventId]` | `PushLogEntry { caregivers: string[], sentAt: number }` |

---

## 3. Authentication & OAuth 2.0 PKCE Flow

```
Mobile App (iOS / Android)               Deno Edge (Hono)                    Google / Apple
       │                                        │                                   │
       │─── 1. Open ASWebAuthSession ──────────►│                                   │
       │    /auth/google                        │─── 2. Redirect to Google OAuth ──►│
       │                                        │                                   │
       │                                        │◄── 3. User Approves & Returns ────│
       │                                        │    with code                      │
       │                                        │─── 4. Exchange code for user ────►│
       │                                        │    info (email, name, sub)        │
       │                                        │                                   │
       │                                        │─── 5. Upsert User in Deno KV      │
       │                                        │    Sign BioCue JWT (HS256)        │
       │◄── 6. Redirect to biocue://auth/───────│                                   │
       │    callback?token=<JWT>                │                                   │
       │                                        │                                   │
       │─── 7. Authenticated API Requests ─────►│                                   │
       │    Authorization: Bearer <JWT>         │                                   │
```

### JWT Claims:
- `sub`: User ID (`uid`, e.g. `google_1029384756`)
- `email`: User's Google email
- `name`: User's display name
- `modes`: `["WEARER", "CAREGIVER"]`
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp (default: 30 days)

---

## 4. API Endpoints

### 4.1 Authentication
- `GET /auth/google`: Initiates Google OAuth2 web authorization.
- `GET /auth/callback`: Handles Google OAuth callback, generates BioCue JWT, redirects to `biocue://auth/callback?token=<JWT>` (mobile) or `/dashboard` (web).
- `POST /api/auth/token`: Exchanges an ID token directly (native fallback).
- `GET /api/auth/me`: Retrieves current authenticated profile.
- `PUT /api/auth/profile`: Updates user display name and active modes (`WEARER` / `CAREGIVER`).

### 4.2 Caregiver Pairing
- `POST /api/pairings/code`: Wearer generates a 6-digit numeric pairing code (valid for 15 minutes).
- `POST /api/pairings/claim`: Caregiver claims the 6-digit code to establish an `APPROVED` pairing.
- `GET /api/pairings`: Returns all pairings where caller is either wearer or caregiver.
- `DELETE /api/pairings/:id`: Revokes a pairing (sets status `REMOVED` and `removedAt`).
- `POST /api/pairings/:id/review`: Adds a caregiver journal reflection/review.

### 4.3 Events & Alerts
- `POST /api/events`: Wearer stores a detected stress window or event.
- `GET /api/events/:wearerUid`: Caregiver (or wearer) fetches recent stress events.
- `POST /api/events/meltdown`: Wearer triggers sensory spike alert; Deno validates recent timing and dispatches push notifications to all approved caregiver devices.
- `GET /api/events/stream`: Server-Sent Events (SSE) streaming live event state changes.

---

## 5. Security Model & Role Enforcement

1. **Protocol-Level Role Validation**:
   - Every request requires a valid `Authorization: Bearer <JWT>`.
   - A caller can only write events if `jwt.sub == wearerUid` and `modes` contains `"WEARER"`.
   - A caregiver can only read events for a wearer if an active `APPROVED` pairing exists between them.
2. **Zero Plaintext Storage**:
   - Pairing codes expire in 15 minutes and are single-use.
   - Revocation instantly drops caregiver access.
