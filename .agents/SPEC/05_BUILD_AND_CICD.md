# 05. Build & CI/CD Pipeline

## Automated Versioning
To prevent version collisions in the Play Store, the `versionCode` for both modules is dynamically calculated during the Gradle build process based on the Git commit history. Manual edits to `gradle.properties` are forbidden.
- **Mobile versionCode:** `git rev-list --count HEAD` + 20
- **Wear versionCode:** `git rev-list --count HEAD` + 10020

## Google Play Automation
The project uses the `com.github.triplet.play` Gradle plugin to publish builds entirely from the CLI, bypassing the Google Play website.
- **Service Account:** Authenticated via `play-account.json`.
- **Command:** `./gradlew publishBundle publishListing`
- **Tracks:** Pushes `:mobile` to the `internal` track and `:wear` to the `wear:internal` track.

## Testing Commands
- `./gradlew assembleDebug` (Compile all)
- `./gradlew test` (Run ML Unit Tests)
- `./gradlew connectedAndroidTest` (Run Room/Compose UI Integration Tests)
