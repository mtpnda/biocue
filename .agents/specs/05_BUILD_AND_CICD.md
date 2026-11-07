# 05. Build & CI/CD Pipeline

## Automated Versioning
To prevent version collisions in the Play Store, the `versionCode` is configured via `android/gradle.properties` (`biocue.versionCode` and `biocue.versionName`), with dynamic fallback to Git commit count during Gradle execution:
- **Mobile versionCode:** `biocue.versionCode` (or `git rev-list --count HEAD + 22`)
- **Wear versionCode:** `biocue.versionCode + 10000` (or `git rev-list --count HEAD + 10022`)

## Google Play Automation
The project uses **Fastlane Supply (`upload_to_play_store`)** orchestrated via `fastlane/Fastfile` and the top-level `Makefile`, matching the `mac-to-ipad-mirroring-protocol` release standard.
- **Service Account:** Authenticated via `~/.secrets/android/play-account.json` (auto-resolved by Gradle or linked via `make setup-secrets`).
- **Primary Commands:**
  - `make release-playstore-internal`: Builds release AABs and uploads Mobile to `internal` and Wear OS to `wear:internal`.
  - `make release-playstore`: Releases Mobile to `production` (with configurable staged rollout `ROLLOUT=0.10`) and Wear OS to `wear:production`.
  - `make release-local`: Compiles release artifacts and synchronizes them to the MacBook Air Webhost (`~/Workspace/macbook-air-webhost`).

## Testing & Quality Gate Commands
- `make test`: Executes all verified test suites (Release Automation + Cloud Functions Backend).
- `make test-release`: Runs `tests/test_release_automation.py`.
- `make test-backend`: Runs `cd backend/functions && node --test test/*.test.js` (18 node unit tests).
- `cd android && ./gradlew test`: Runs Android & Wear OS unit tests.
- `cd backend/firestore-rules-tests && npm test`: Firestore rules emulator tests.
- `cd ios/Packages/BioCueCore && swift test` and `xcodebuild test -scheme BioCue`: iOS verification.

---

## Keystore & Key Rotation Protocol

BioCue uses **Google Play App Signing**, which splits signing into two distinct keys:
1. **Upload Key (`biocue-release-key.jks`)**: Stored in `~/.secrets/android/biocue-release-key.jks`. Used locally by Fastlane / Gradle to sign `.aab` bundles uploaded to Google Play.
2. **App Signing Key**: Held securely in Google Cloud KMS by Google Play to sign final `.apk` binaries delivered to user devices.

### 1. Rotating the Upload Key (Local Keystore)
If `biocue-release-key.jks` is lost, compromised, or due for scheduled rotation:
1. **Generate a new upload keystore**:
   ```bash
   keytool -genkeypair -v -keystore android/biocue-release-key-new.jks \
     -alias biocue-upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Export the public certificate in PEM format**:
   ```bash
   keytool -exportcert -rfc -keystore android/biocue-release-key-new.jks \
     -alias biocue-upload -file upload_certificate.pem
   ```
3. **Submit Reset Request to Google Play Console**:
   - Navigate to **Google Play Console** -> Select **BioCue** (`com.mtpnda.biocue`).
   - Go to **Release** > **Setup** > **App integrity** > **App signing** tab.
   - Click **Request upload key reset** (or contact Google Support via the provided link).
   - Select reason (*"I lost my upload key"* or *"Key compromised / routine rotation"*), upload `upload_certificate.pem`, and submit.
   - Google will send an email confirmation and the new key will take effect automatically within 24–48 hours.
4. **Update Local Configuration**:
   - Replace `android/biocue-release-key.jks` with the new file or update `android/local.properties`:
     ```properties
     RELEASE_KEYSTORE_PATH=/absolute/path/to/biocue-release-key-new.jks
     RELEASE_KEYSTORE_PASSWORD=...
     RELEASE_KEY_ALIAS=biocue-upload
     RELEASE_KEY_PASSWORD=...
     ```

### 2. Rotating the App Signing Key (Play Console)
If you need to upgrade or rotate the key that signs distributed APKs:
1. In **Google Play Console** -> **App integrity** -> **App signing**.
2. Under **App signing key certificate**, select **Upgrade key** (Google allows upgrading once per year to modern encryption standards or new signing keys).
3. Google Play supports Android 9+ (API 28+) APK Signature Scheme v3 key rotation, ensuring seamless trust transitions for existing installations.

### 3. Firebase & Google Sign-In Synchronization (Critical)
Google Sign-In validates incoming tokens against the SHA-1 fingerprints registered in Firebase:
1. **Locate SHA-1 fingerprints**:
   - **Local Upload Key**:
     ```bash
     keytool -list -v -keystore android/biocue-release-key.jks -alias biocue-release-key
     ```
   - **Google Play App Signing Key**: Found in **Google Play Console** > **App integrity** > **App signing** > **SHA-1 certificate fingerprint**.
2. **Register in Firebase**:
   - Open **Firebase Console** -> **Project Settings** (`biocueapp`) -> **Your apps**.
   - Select both **BioCue Mobile** (`com.mtpnda.biocue`) and **BioCue Wear** (`com.mtpnda.biocue`).
   - Click **Add fingerprint** and add the new SHA-1 and SHA-256.
   - **Rule**: *Never delete the old SHA-1 immediately.* Keep both old and new fingerprints active during the transition period so existing active installs and cached tokens continue functioning without outage.
3. **Refresh Config**:
   - Download the updated `google-services.json` and save to `android/mobile/google-services.json` and `android/wear/google-services.json`.

---

## Amendments (merge, approved)

* *Publishing:* Standardized on Fastlane Supply (`upload_to_play_store`) instead of direct GPP tasks.
* *Testing Commands:* Added centralized `make test`, `make test-release`, and `make test-backend`.
* *Security:* Added Keystore & Key Rotation Protocol for Play App Signing & Firebase OAuth sync.
