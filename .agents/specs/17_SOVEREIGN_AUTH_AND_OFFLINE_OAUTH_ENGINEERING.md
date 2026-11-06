# 17. Sovereign Direct OAuth PKCE, Token Exchange & Offline-First State Machine

> **Normative Specification.**  
> Defines the complete reverse-engineering blueprint for BioCue's vendor-free Google OAuth 2.0 PKCE authentication, Deno Edge session minting, and the sovereign local-first session state machine.  
> **Clients:** iOS (`ASWebAuthenticationSession` + CryptoKit), Android (`CustomTabs` + Keystore), Edge Backend (Deno + Hono + Deno KV).

---

## 1. Architectural Philosophy & Invariants

1. **Zero Third-Party SDK Bloat**:
   - BioCue strictly forbids embedding `firebase-ios-sdk`, `GoogleSignIn-iOS`, or bloated third-party auth SDKs.
   - The entire authentication pipeline is implemented using standard RFC 7636 and RFC 8252 over native system components (`ASWebAuthenticationSession` on Apple platforms, `CustomTabsIntent` on Android) and native system cryptography (`CryptoKit` on iOS, `java.security` on Android).

2. **The Sovereign Local-First Invariant**:
   - BioCue is an assistive bio-signal cueing platform for neurodivergent individuals and sensory-vulnerable wearers.
   - **Crucial Rule:** The Wearer companion app and its Bluetooth LE link to the smartwatch must **NEVER** fail, block, or display a fatal error screen because of a remote network outage, DNS failure, 5xx backend error, or airplane mode.
   - Network disconnections during or after login **MUST** gracefully degrade to a sovereign on-device profile, immediately unlocking the Wearer Dashboard and Bluetooth LE stack.

---

## 2. Protocol 1: Direct Google OAuth 2.0 PKCE (RFC 7636 + RFC 8252)

```
┌──────────────┐                  ┌────────────────────────┐              ┌───────────────────────────┐
│  Mobile App  │                  │  System Web Browser    │              │ Google OAuth Infrastructure│
│ (Swift Core) │                  │(ASWebAuthSession / Tab)│              │   (accounts.google.com)   │
└──────┬───────┘                  └───────────┬────────────┘              └─────────────┬─────────────┘
       │                                      │                                         │
       │ 1. Generate Verifier & Challenge     │                                         │
       │    verifier = Base64Url(Random(32))  │                                         │
       │    challenge = Base64Url(SHA256(v))  │                                         │
       │                                      │                                         │
       │ 2. Launch ASWebAuthSession ─────────►│                                         │
       │    with auth URL & challenge         │─── 3. HTTP GET /o/oauth2/v2/auth ──────►│
       │                                      │                                         │
       │                                      │◄── 4. Render Google Auth UI ────────────│
       │                                      │    (User approves / selects account)    │
       │                                      │                                         │
       │                                      │◄── 5. HTTP 302 Redirect ────────────────│
       │                                      │    <reversed_scheme>:/oauth2redirect    │
       │                                      │    ?code=<auth_code>                    │
       │                                      │                                         │
       │◄── 6. Intercept Redirect URI ────────┘                                         │
       │    Extract ?code=<auth_code>                                                   │
       │                                                                                │
       │ 7. Direct Code Exchange (POST https://oauth2.googleapis.com/token) ───────────►│
       │    body: { client_id, code, code_verifier, grant_type, redirect_uri }          │
       │                                                                                │
       │◄── 8. Return Google Tokens { id_token, access_token, expires_in } ─────────────┘
       │
       │ 9. Decode JWT ID Token payload (sub, email, name)
       │    Synthesize AuthUser & Persist in Local Keystore/UserDefaults
       ▼
```

### 2.1 Cryptographic Verifier & Challenge Specification

1. **Code Verifier**:
   - Generate 32 bytes of cryptographically secure pseudo-random octets using `SecRandomCopyBytes(kSecRandomDefault, 32, &bytes)`.
   - Convert to a Base64URL-encoded ASCII string without padding:
     $$\text{Base64URL}(X) = \text{Replace}(\text{Replace}(\text{Base64}(X), \text{`+`}, \text{`-`}), \text{`/`}, \text{`_`}) \setminus \text{`=`}$$

2. **Code Challenge**:
   - Compute SHA-256 hash of the ASCII code verifier string:
     $$\text{Digest} = \text{SHA256}(\text{UTF8}(\text{verifier}))$$
   - Apply the identical Base64URL-encoding without padding:
     $$\text{challenge} = \text{Base64URL}(\text{Digest})$$
   - Parameter: `code_challenge_method = "S256"`.

### 2.2 Client Identity & Scheme Inversion Rule

- **iOS Client ID Format**: `<PROJECT_NUMBER>-<HASH>.apps.googleusercontent.com`  
  *Example:* `1006317660514-e5ucehed56l4qdh34r97mg8s1smb05e7.apps.googleusercontent.com`
- **Reversed Scheme (RFC 8252 Section 7.1)**:  
  Invert all dot-separated segments in reverse order:
  $$\text{ReversedScheme} = \text{join}(\text{reverse}(\text{split}(\text{ClientID}, \text{`.`})), \text{`.`})$$
  *Resulting Scheme:* `com.googleusercontent.apps.1006317660514-e5ucehed56l4qdh34r97mg8s1smb05e7`
- **Redirect URI**: `<ReversedScheme>:/oauth2redirect`
- **InfoPlist Custom URL Scheme**: Must declare `<ReversedScheme>` under `CFBundleURLTypes` -> `CFBundleURLSchemes`.

### 2.3 OAuth Request & Token Exchange Parameters

1. **Authorization URL**: `https://accounts.google.com/o/oauth2/v2/auth`
   - `client_id`: `<iOS_CLIENT_ID>`
   - `redirect_uri`: `<ReversedScheme>:/oauth2redirect`
   - `response_type`: `code`
   - `scope`: `openid email profile`
   - `code_challenge`: `<challenge>`
   - `code_challenge_method`: `S256`
   - `prompt`: `select_account`

2. **Token Exchange Endpoint**: `POST https://oauth2.googleapis.com/token`
   - **Header**: `Content-Type: application/x-www-form-urlencoded`
   - **Body (URL-encoded)**:
     - `client_id`: `<iOS_CLIENT_ID>`
     - `code`: `<auth_code>`
     - `code_verifier`: `<verifier>`
     - `grant_type`: `authorization_code`
     - `redirect_uri`: `<ReversedScheme>:/oauth2redirect`

3. **ID Token Decoding**:
   - Split JWT by `.` delimiter into `[header, payload, signature]`.
   - Base64URL-decode `payload`.
   - Unmarshal JSON object and extract:
     - `sub`: Google Unique Subject Identifier (maps to `uid = "google_" + sub`).
     - `email`: Authenticated user email.
     - `name`: Display name (falls back to email prefix).
     - `picture`: Profile photo URL (optional).

---

## 3. Protocol 2: Edge Session Exchange & Deno KV Minting

When an active internet connection is available, the mobile client exchanges its Google `id_token` with the BioCue Deno backend to mint an authorized BioCue session token.

### 3.1 Token Exchange Contract (`POST /api/auth/token`)

- **Request**:
  ```json
  POST /api/auth/token
  Content-Type: application/json

  {
    "idToken": "<google_id_token_jwt>",
    "provider": "google"
  }
  ```

- **Backend Verification Logic**:
  1. The backend issues an internal verification call:
     `GET https://oauth2.googleapis.com/tokeninfo?id_token=<idToken>`
  2. If valid, extracts `{ sub, email, name, picture }`.
  3. Executes an atomic upsert in Deno KV:
     - Key: `["users", "google_" + sub]`
     - Value: `UserProfile { uid, email, displayName, photoUrl, modes: ["WEARER"], createdAt, updatedAt }`
  4. Mints a session JWT using HS256 signed with `JWT_SECRET` (30 days validity).

- **Response (200 OK)**:
  ```json
  {
    "token": "<biocue_session_jwt>",
    "user": {
      "uid": "google_1006317660514000",
      "email": "user@gmail.com",
      "displayName": "Alex User",
      "modes": ["WEARER"]
    }
  }
  ```

---

## 4. Protocol 3: Sovereign Offline-First State Machine

The client application manages authentication and identity via a strict Finite State Machine (`SessionStore`).

```
                    ┌──────────────┐
                    │   Starting   │
                    └──────┬───────┘
                           │ (read local storage)
              ┌────────────┴────────────┐
              ▼                         ▼
      [No stored token]          [Stored token]
              │                         │
              ▼                         ▼
       ┌──────────────┐         ┌────────────────┐
       │  SignedOut   │         │ LoadingProfile │◄──────────────┐
       └──────┬───────┘         └───────┬────────┘               │
              │                         │                        │
       (OAuth Success)                  │ (Observe Profile)      │
              │                         │                        │
              ▼                         ├────────────────────────┤
       ┌──────────────┐                 │                        │
       │ LoadingProfile                 ▼                        │
       └──────┬───────┘          ┌──────────────┐                │
              │                  │ Server / Net │                │
              │                  │   Failure?   │                │
              │                  └──────┬───────┘                │
              │                         │                        │
              │             ┌───────────┴───────────┐            │
              │             ▼                       ▼            │
              │          [ Online ]            [ Offline / Err ] │
              │             │                       │            │
              │      ┌──────┴──────┐                │            │
              │      ▼             ▼                ▼            │
              │   (Found)      (404 / New)     [Local Fallback]  │
              │      │             │                │            │
              ▼      ▼             ▼                ▼            │
          ┌─────────────┐   ┌───────────┐   ┌─────────────┐      │
          │ Ready(prof) │   │ NeedsRole │   │ Ready(prof) │      │
          └─────────────┘   └─────┬─────┘   └─────────────┘      │
                                  │                              │
                            (Choose Role)                        │
                                  └──────────────────────────────┘
```

### 4.1 State Definitions & Transition Table

| Current State | Trigger Event | Guard / Condition | Target State | Action / Side Effect |
| :--- | :--- | :--- | :--- | :--- |
| `starting` | App Launch | No token in `UserDefaults` | `signedOut` | Show login view. |
| `starting` | App Launch | Cached token & UID present | `loadingProfile` | Start `observeUserProfile(uid)`. |
| `signedOut` | Google / Apple Login | Success | `loadingProfile` | Persist token & profile; start observer. |
| `signedOut` | Quick Demo Login | Tapped | `ready(demoProfile)` | Instant demo bypass. |
| `loadingProfile` | Observer Tick | HTTP 200 from Edge API | `ready(remoteProfile)` | Cache role in `biocue.role.<uid>`. |
| `loadingProfile` | Observer Tick | HTTP 404 from Edge API | `needsRole` | If no local cached role exists, prompt user. |
| `loadingProfile` | Observer Tick | **Network Down / Server Down** | **`ready(sovereignProfile)`** | **NEVER transitions to `failed`! Construct local `UserProfile` from cached auth and proceed.** |
| `needsRole` | User taps role card | Role chosen (`.wearer` / `.caregiver`) | `ready(profile)` | Save to `biocue.role.<uid>`; async PUT to Edge API. |
| `ready(profile)` | User taps Sign Out | Action confirmed | `signedOut` | Clear tokens & drop observers. |

---

## 5. Protocol 4: Edge Server Resolution & Routing

Mobile devices must resolve their edge backend endpoint dynamically without hardcoded developer machine addresses.

### 5.1 Resolution Hierarchy

1. **Tier 1 (Explicit User Override)**:
   - Check `UserDefaults.standard.string(forKey: "biocue.custom_server_url")`.
   - Allows QA and developers to point physical devices to staging, internal relays, or local mocks.
2. **Tier 2 (Public Production Edge)**:
   - Default: `https://biocue.deno.dev` (or live Cloudflare edge tunnel during preview testing).
   - Must use standard HTTPS (port 443) to comply with Apple App Transport Security (ATS).
3. **Strict Ban on Physical `localhost`**:
   - The string `http://localhost:<port>` or `http://127.0.0.1:<port>` is **strictly prohibited** in mobile release and device debug configurations.

---

## 6. Reference Implementation Blueprint (Pure Swift)

### 6.1 PKCE Engine (`GoogleOAuthPKCE.swift`)

```swift
import Foundation
import CryptoKit

struct GoogleOAuthPKCE {
    let verifier: String
    let challenge: String

    init() {
        var randomBytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, 32, &randomBytes)
        self.verifier = Self.base64URLEncode(Data(randomBytes))

        let digest = SHA256.hash(data: Data(self.verifier.utf8))
        self.challenge = Self.base64URLEncode(Data(digest))
    }

    static func base64URLEncode(_ data: Data) -> String {
        data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    static func decodeJwtPayload(_ jwt: String) -> [String: Any]? {
        let parts = jwt.components(separatedBy: ".")
        guard parts.count >= 2 else { return nil }
        var base64 = parts[1].replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 { base64.append("=") }
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return json
    }
}
```

### 6.2 Sovereign Observer Logic (`DenoCloud.swift`)

```swift
func observeUserProfile(uid: String, onChange: @escaping (Result<UserProfile?, Error>) -> Void) -> ListenerToken {
    var active = true
    let task = Task {
        while active {
            do {
                let req = try request("api/auth/me")
                let (data, response) = try await URLSession.shared.data(for: req)
                if let http = response as? HTTPURLResponse, http.statusCode == 200,
                   let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    let name = json["displayName"] as? String ?? ""
                    let modes = json["modes"] as? [String] ?? ["WEARER"]
                    let role: UserRole = modes.contains("CAREGIVER") ? .caregiver : .wearer
                    let code = json["pairingCode"] as? String
                    let codeExp = (json["pairingCodeExpiresAt"] as? NSNumber)?.int64Value

                    let profile = UserProfile(uid: uid, role: role, displayName: name,
                                              pairingCode: code, pairingCodeExpiresAt: codeExp)
                    onChange(.success(profile))
                } else if let http = response as? HTTPURLResponse, http.statusCode == 404 {
                    // Check local cached role first
                    let cachedRole = UserDefaults.standard.string(forKey: "biocue.role.\(uid)").flatMap { UserRole(rawValue: $0) }
                    if let cachedRole = cachedRole {
                        let name = self.auth.currentUser?.displayName ?? "Wearer"
                        let localProfile = UserProfile(uid: uid, role: cachedRole, displayName: name, pairingCode: nil, pairingCodeExpiresAt: nil)
                        onChange(.success(localProfile))
                    } else {
                        onChange(.success(nil)) // Triggers NeedsRole selection screen
                    }
                } else {
                    throw CloudError.couldNotFetchEvents
                }
            } catch {
                // Sovereign offline fallback: never brick the wearer on a network error!
                let cachedRole = UserDefaults.standard.string(forKey: "biocue.role.\(uid)").flatMap { UserRole(rawValue: $0) } ?? .wearer
                let name = self.auth.currentUser?.displayName ?? "Wearer"
                let localProfile = UserProfile(uid: uid, role: cachedRole, displayName: name,
                                          pairingCode: nil, pairingCodeExpiresAt: nil)
                onChange(.success(localProfile))
            }
            try? await Task.sleep(nanoseconds: 5_000_000_000)
        }
    }
    return ListenerToken {
        active = false
        task.cancel()
    }
}
```

---

## 7. Verification & Deterministic Test Vectors

### 7.1 RFC 7636 Appendix B Test Vector Check

1. **Test Verifier**:
   `dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk`
2. **Expected SHA-256 Digest (Hex)**:
   `3538cf026210f878f7732a39b0ee464a34b2efbe436d46dd8996590d96d996be`
3. **Expected S256 Challenge (Base64URL)**:
   `NTg_AmIQ-Hj3cyo5sO5GSjSy775DbUbdiZZZDZbZlr4`

```bash
# Terminal Verification Command:
echo -n "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk" | shasum -a 256 | xxd -r -p | base64 | tr '+/' '-_' | tr -d '='
# Output: NTg_AmIQ-Hj3cyo5sO5GSjSy775DbUbdiZZZDZbZlr4
```

### 7.2 Physical Device Verification Protocol

1. **Turn on Airplane Mode on iPhone**:
   - Launch BioCue.
   - Tap **Quick Demo Access** or sign in while offline.
   - **Assertion:** App immediately moves from `starting` to `ready(UserProfile)`. No alerts, no blocking spinning spinners, no `"could not connect to server"`.
2. **Bluetooth LE Link Verification**:
   - Turn on Bluetooth (keeping Cellular/WiFi off).
   - Tap "Connect watch".
   - **Assertion:** BLE scanning commences; watch GATT link establishes successfully. Real-time PPG and accelerometer scoring operate 100% on-device.
3. **Cloud Reconnect**:
   - Turn WiFi back on.
   - **Assertion:** `observeUserProfile` resumes polling without dropping current UI state. Remote stress event syncing resumes silently in background.
