# Google Play Store Automation Guide

This document explains how BioCue achieves deterministic, industry-grade, AI-independent publishing to the Google Play Console using the **Gradle Play Publisher (GPP)** plugin.

## 1. Authentication (`play-account.json`)
To bypass the web browser, the project uses a Google Cloud Service Account (`play-store-api@matapanda-studio.iam.gserviceaccount.com`). 
- The secret key for this account is stored locally as `play-account.json` in the project root.
- **SECURITY WARNING:** This file grants admin access to your Play Console. It has been strictly added to `.gitignore` and must never be committed to source control.

## 2. Release Keystore & App Signing
Google Play strictly requires release-signed App Bundles (`.aab`). We handle this securely without leaking passwords into the codebase.
- **The Script:** The `./build_release.sh` bash script automatically checks for `biocue-release-key.jks`. If it doesn't exist, it uses `keytool` to generate a 2048-bit RSA key valid for 10,000 days.
- **Secret Management:** The script injects the keystore passwords into `local.properties` (which is also git-ignored).
- **Gradle Injection:** Both `mobile/build.gradle.kts` and `wear/build.gradle.kts` are configured with a `signingConfigs { release { ... } }` block that safely reads the passwords from `local.properties` (or System Environment variables for CI/CD pipelines).

## 3. Managing the Store Listing (Text & Graphics)
You no longer need to type descriptions or upload screenshots via the Play Console website. The entire Store Listing is managed deterministically as code inside the `mobile/src/main/play/` directory.

### Directory Structure:
- `listings/en-US/title.txt`: The App Name (Max 30 chars).
- `listings/en-US/short-description.txt`: The Short Description (Max 80 chars).
- `listings/en-US/full-description.txt`: The Long Description (Max 4000 chars).
- `release-notes/en-US/default.txt`: The Release Notes for the current build.

### Graphical Assets:
All images must be dropped into their exact respective folders:
- `listings/en-US/graphics/icon/`: 512x512 App Icon
- `listings/en-US/graphics/feature-graphic/`: 1024x500 Feature Graphic
- `listings/en-US/graphics/phone-screenshots/`: Phone screenshots (16:9 or 9:16)
- `listings/en-US/graphics/wear-screenshots/`: Wear OS screenshots

If you want to update the app's description or add a new screenshot in the future, simply edit the text files or drop the new image into these folders.

## 4. Publishing Commands
Whenever you are ready to push an update to Google Play, open the terminal and run one of these commands:

- **`./gradlew publishListing`**: Uploads ONLY the text metadata and graphical assets. (Use this if you just want to update the store page without pushing new code).
- **`./gradlew publishBundle`**: Builds, signs, and uploads ONLY the `.aab` code bundles to the Internal Testing track.
- **`./gradlew publishBundle publishListing`**: The master command. Uploads everything (Code, Text, and Graphics) in one automated sweep.

*(Note: Currently, the GPP configuration in the Gradle files is set to target the `internal` track. To target production, change `track.set("internal")` to `track.set("production")` in `build.gradle.kts`.)*
