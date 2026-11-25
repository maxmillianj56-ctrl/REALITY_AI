# REALITY AI — Native Android Gaming Companion & Visual Analysis

Optimized baseline: **Samsung Galaxy Note 20 Ultra (`SM-N986U1`)**, **Snapdragon 865+**, **Adreno 650**, **Android 13+ (API 33+)**.

## Architecture & Guarantees
- **100% Offline-First**: Zero `INTERNET` permission. All profile storage uses Jetpack DataStore Preferences and all frame processing/scene analysis executes locally.
- **Honest Android APIs**:
  - Screen capture uses `MediaProjection` + `FOREGROUND_SERVICE_MEDIA_PROJECTION` with explicit user consent per session and a persistent notification containing a **Stop Capture** action.
  - Visual analysis filters (`brightness`, `contrast`, `saturation`, `gamma`, `sharpness`) operate exclusively on REALITY AI's preview frames (`ColorMatrix` + Gamma LUT + 3×3 unsharp-mask convolution). They do not alter external game shaders or GPU pipelines.
  - FPS telemetry reports actual `ImageReader` capture callback cadence only while MediaProjection is active (`Unavailable` when idle).
  - Automatic game detection uses `PackageManager` launcher visibility and optional user-granted `PACKAGE_USAGE_STATS` (`UsageStatsManager`), offering to apply a saved profile without secretly starting capture.

## Build & Install (Android Studio / Gradle CLI)
1. Open `/android-project` in **Android Studio Ladybug / Meerkat** (JDK 17, Android SDK 35).
2. Or build from CLI with Android SDK configured:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
3. Install on your Galaxy Note 20 Ultra via USB debugging:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```


## Browser-only cloud APK build (GitHub Actions)

This ZIP includes `.github/workflows/android-apk.yml`. Upload the contents of this project to a GitHub repository, then open the repository's **Actions** tab and run **Build REALITY AI APK**. The workflow installs JDK 17, Android SDK 35 and Gradle 8.9, runs unit tests, builds the debug APK, and uploads it as an artifact named `REALITY-AI-debug-APK`.

Download the artifact from the completed workflow run. It contains the installable debug APK. Debug APKs are suitable for testing and sideloading; a separate signed release build is recommended before public distribution.
