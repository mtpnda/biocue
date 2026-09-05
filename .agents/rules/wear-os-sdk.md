---
name: wear-os-sdk-rule
description: Enforces modern Wear OS SDK dependencies and avoids useLibrary("wear-sdk").
---

# Wear OS SDK Dependencies

Never use `useLibrary("wear-sdk")` in Wear OS `build.gradle.kts` files. Modern Wear OS projects using Jetpack Compose rely on standard Android SDK platform APIs.

If the Wearable Data Layer is needed:
1. Only declare it as a `compileOnly` dependency.
2. Add `<uses-library android:name="com.google.android.wearable" android:required="false" />` to the `AndroidManifest.xml`.
