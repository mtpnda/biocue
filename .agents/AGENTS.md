# BioCue — Agentic Operating & Architecture Guide
> **Repository:** `BioCue` (Wear OS + Android Companion for Habit & Bio-Signal Cueing)  
> **Central Agent Plugin:** [/Users/valdiearsanur/.gemini/config/plugins/central-agents/](file:///Users/valdiearsanur/.gemini/config/plugins/central-agents/) (12 Specialized Agents, Centralized Verified Skills)  
> **Release Standard:** [custom-release-manager](file:///Users/valdiearsanur/.gemini/config/plugins/central-agents/skills/custom-release-manager/SKILL.md)

---

## 1. Zero-Waste Repository Boundary & Centralized Agent Protocol

1. **Zero-Waste Repository Boundary (No Local `.agents/skills`)**:
   - This project repository strictly forbids storing local skill definitions inside `.agents/skills`.
   - All agent capabilities, playbooks, and specialized tools are centralized in the global plugin:
     **`central-agents`** (`/Users/valdiearsanur/.gemini/config/plugins/central-agents`).
   - All agents and developers must draw from the centralized catalog in `central-agents`.

2. **Unified Release Automation Facade**:
   - The single-pane controller for all releases, local webhost deployments, and Google Play Store uploads is the root `Makefile` and `fastlane/Fastfile`.
   - Ad-hoc release scripts (e.g. `build_release.sh`) are strictly prohibited and deprecated.
   - All release workflows are managed by `@release-manager` using `custom-release-manager`.

---

## 2. Master Intent-to-Agent Navigation Matrix

| Developer Intent / Task in BioCue | Target Agent | Primary Central Skills Paired | Target Subsystem / File Scope |
| :--- | :--- | :--- | :--- |
| **Android / Wear OS UI, Jetpack Compose, Sensors, Health Services** | `@mobile-dev` | `android-architecture`, `android-ui-engineering`, `android-background-work`, `mobile-automation` | `mobile/`, `wear/`, `core-detection/` |
| **Automated Testing, Robolectric, Compose Test Rules, Quality Gates** | `@qa-engineer` | `test-driven-development`, `tdd`, `android-device-testing`, `code-review-and-quality` | `mobile/src/test/`, `wear/src/test/`, `tests/` |
| **Play Store Publishing, Fastlane, GPP, Keystore Signing, Webhost Ingestion** | `@release-manager` | `custom-release-manager`, `reusable-fastlane`, `shipping-and-launch` | `Makefile`, `fastlane/`, `play-account.json`, `mobile/src/main/play/`, `wear/src/main/play/` |
| **Firebase / Cloud Sync, Firestore Rules, Data Modeling** | `@backend-dev` | `api-and-interface-design`, `security-and-hardening`, `clean-code-principles` | `firestore.rules`, `firebase.json`, `functions/` |
| **Feature Specs, PRDs, Multi-Agent Swarm Orchestration** | `@product-architect`| `interview-me`, `spec-driven-development`, `pstack`, `planning-and-task-breakdown` | `specs/` |

---

## 3. Google Play Store & Multiplatform Release Protocol

BioCue adheres to the standardized `custom-release-manager` protocol:

1. **Single-Pane Command Controller (`Makefile`)**:
   - `make release-local`: Compiles release bundles (`:mobile:bundleRelease`, `:wear:bundleRelease`) and synchronizes them to the local MacBook Air Webhost.
   - `make release-playstore`: Builds, signs, and uploads release bundles and store listings to the Google Play Store via Gradle Play Publisher (`./gradlew publishBundle publishListing`).
   - `make release-playstore-internal`: Uploads release bundles to Google Play Internal Testing track.
   - `make release-playstore-listing`: Synchronizes store text and graphical assets (`./gradlew publishListing`) without triggering a code build.
   - `make install-android`: Installs Companion APK directly to connected Android phone/tablet/emulator.
   - `make install-wear`: Installs Wear OS APK directly to connected smartwatch/emulator.
   - `make setup-secrets`: Links credentials from `~/.secrets/` into the project for multi-machine parity.
   - `make doctor`: Verifies build toolchain, connected webhost, and global credentials.
   - `make test-release`: Runs the automated release verification test suite (`tests/test_release_automation.py`).
   - `make test-reviewer`: Runs the deep sequential UI reviewer automation suite (`tests/test_playstore_reviewer.py`).

2. **Store Listing as Code (`mobile/src/main/play/` & `wear/src/main/play/`)**:
   - `listings/en-US/title.txt`: App title (max 30 characters).
   - `listings/en-US/short-description.txt`: App summary (max 80 characters).
   - `listings/en-US/full-description.txt`: Full description (max 4000 characters).
   - `listings/en-US/graphics/`: Icons, feature graphics, and phone/tablet/wear screenshots.
   - `release-notes/en-US/default.txt`: Release changelog for current build.

3. **Authentication, Centralized Secrets & Signing**:
   - **Zero Secrets in Repository:** Real private keys, service account JSON files, and keystores are **never** stored inside the project folder.
   - **Global Secrets Storage (`~/.secrets/`):**
     - `~/.secrets/android/biocue-release-key.jks`: BioCue production upload key.
     - `~/.secrets/android/play-account.json`: Google Play Console publishing credentials.
     - `~/.secrets/android/biocue/google-services.json`: Firebase client config.
   - **Multi-Machine Linking:** Run `make setup-secrets` on any cloned machine to symlink global credentials into place. Gradle also directly checks `~/.secrets/android/` automatically.
   - `local.properties`: Contains SDK path and local keystore properties (strictly gitignored).

---

## 4. Engineering & Workspace Governance Rules

1. **Zero Root Litter Rule (`misc/`)**:
   - Never write temporary test scripts, scratch files, throwaway patches, or exploratory notes to the repository root.
   - All auxiliary and non-production files must be routed into the gitignored `misc/` container (`misc/scratch/`, `misc/tasks/`, `misc/notes/`, `misc/export/`).

2. **Spec-First Architecture (`.agents/specs/`)**:
   - The `.agents/specs/` directory is the single source of truth for all requirements, ML thresholds, and UI state models.
   - Any architectural changes or new feature requirements must be documented in `.agents/specs/` before implementation begins.

3. **Wear OS Modern Platform Rule**:
   - Never use `useLibrary("wear-sdk")` in Wear OS `build.gradle.kts`. Modern Wear OS uses standard Jetpack Compose for Wear OS and `androidx.health.services.client`.
   - Wearable Data Layer dependencies must be scoped properly with `<uses-library android:name="com.google.android.wearable" android:required="false" />` in `AndroidManifest.xml`.
