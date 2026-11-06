fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### install

```sh
[bundle exec] fastlane install
```

Install BioCue Companion APK to connected device/emulator

### install_wear

```sh
[bundle exec] fastlane install_wear
```

Install BioCue Wear OS APK to connected watch/emulator

### build_all

```sh
[bundle exec] fastlane build_all
```

Build all present platforms locally into ./build/ and ./android/

### internal_release

```sh
[bundle exec] fastlane internal_release
```

Full internal release: Build present platforms and output to destination folder (macbook-air-webhost)

### external_release

```sh
[bundle exec] fastlane external_release
```

Full external release: Publish macOS DMG, Android APK to GitHub Releases & TestFlight

### appstore_release

```sh
[bundle exec] fastlane appstore_release
```

Submit all Apple targets (macOS Host & iOS Client) to App Store Connect

### playstore_release

```sh
[bundle exec] fastlane playstore_release
```

Publish to Google Play Store Production track

### metadata

```sh
[bundle exec] fastlane metadata
```

Sync store metadata across platforms without building binaries

### copy_to_folder

```sh
[bundle exec] fastlane copy_to_folder
```

Deploy current local build artifacts directly to a destination folder

----


## Mac

### mac build

```sh
[bundle exec] fastlane mac build
```

Build macOS Release App bundle & DMG installer locally into ./build/

### mac internal

```sh
[bundle exec] fastlane mac internal
```

Build and release macOS app into destination folder (e.g. macbook-air-webhost)

### mac external

```sh
[bundle exec] fastlane mac external
```

Build signed & notarized macOS release for external distribution (DMG + GitHub)

### mac uninstall

```sh
[bundle exec] fastlane mac uninstall
```

Uninstall BioCue from local Mac: resets TCC permissions and removes app

### mac release_appstore

```sh
[bundle exec] fastlane mac release_appstore
```

Build and submit macOS Host to Mac App Store

----


## iOS

### ios build

```sh
[bundle exec] fastlane ios build
```

Build iOS client IPA locally into build/ios_internal/

### ios internal

```sh
[bundle exec] fastlane ios internal
```

Build iOS client and export IPA to destination folder for sideloading

### ios testflight_upload

```sh
[bundle exec] fastlane ios testflight_upload
```

Build and upload iOS client to Apple TestFlight

### ios release_appstore

```sh
[bundle exec] fastlane ios release_appstore
```

Build and submit iOS client to App Store

----


## Android

### android build

```sh
[bundle exec] fastlane android build
```

Build Android release APK & AAB locally

### android internal

```sh
[bundle exec] fastlane android internal
```

Build Android release APK & AAB and export into destination folder

### android external

```sh
[bundle exec] fastlane android external
```

Build and publish Android release to GitHub and Closed/Open Testing

### android release_playstore

```sh
[bundle exec] fastlane android release_playstore
```

Publish production AAB to Google Play Store

### android release_playstore_internal

```sh
[bundle exec] fastlane android release_playstore_internal
```

Upload AAB to Google Play Internal Testing track

### android release_playstore_listing

```sh
[bundle exec] fastlane android release_playstore_listing
```

Publish Store Listing (text descriptions & graphics) only

### android upload_metadata

```sh
[bundle exec] fastlane android upload_metadata
```

Upload Google Play Store metadata (listings, descriptions, graphics)

### android install

```sh
[bundle exec] fastlane android install
```

Install BioCue Companion APK onto connected Android device or emulator

### android install_wear

```sh
[bundle exec] fastlane android install_wear
```

Install BioCue Wear OS APK onto connected Wear OS device or emulator

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
