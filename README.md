# Wave Focus - Android (APK) 2.2

Kotlin + Jetpack Compose routine tracker for Android 8.0+ (minSdk 26), with
in-app updates and a user-selectable theme color.

## v2.2 changes

- **Theme color slider** (`ui/theme/Hue.kt`,
  `ui/screens/BackupSettingsDialog.kt`):
  - Settings → *Theme & Appearance* gains a **Theme color** card with a hue
    slider, eight preset swatches and **Reset**.
  - `Color.shiftHue` / `ColorScheme.shiftHue` / `WaveColors.shiftHue` rotate the
    accent palette (primary, secondary, tertiary, soft tints, hero glow) while
    leaving neutrals untouched, so contrast survives in light and dark mode.
  - `RoutineViewModel.themeHue` persists the offset as `theme_hue` in the app
    preferences; `MyApplicationTheme(themeMode, hueShift)` applies it app-wide.
- `versionCode 4`, `versionName "2.2"`.

## v2.1 changes

- **In-app updates** (`app/src/main/java/com/example/update/`):
  - `AppVersion.kt` reads `BuildConfig.VERSION_NAME` / `VERSION_CODE`.
  - `SemVer.kt` compares versions (SemVer 2.0).
  - `UpdateChecker.kt` reads the shared `updates/version.json` manifest
    (`android` block), downloads the APK to `cache/updates`, verifies the
    advertised size and SHA-256 and hands it to the system installer through
    the existing `FileProvider`.
  - `ui/components/AppUpdateBanner.kt` shows a dismissible banner with the
    release notes and a one-tap **Update** button; Settings → *Updates* adds a
    manual check, progress bar and **Install update** action.
  - Android asks once for "install unknown apps" permission; the app detects
    that and links straight to the right settings screen.
- `versionCode 3`, `versionName "2.1"`.
- `REQUEST_INSTALL_PACKAGES` permission and an `updates/` cache path in
  `res/xml/file_paths.xml`.

## v2 changes

- Saudi (Umm al-Qura) Hijri verification on launch / reconnect via
  `util/HijriSync.kt`; the day offset is applied in `util/HijriCalendarHelper.kt`.
- Configurable task reset time in Settings → *Day & Calendar*
  (`util/RoutineDay.kt`, `RoutineViewModel.setDayResetMinutes`) with automatic
  rollover when the reset time passes.

## Build

```bash
./gradlew assembleRelease
```

Requirements: JDK 17+ and an Android SDK with API 36. Create
`local.properties` with `sdk.dir=...` if Gradle cannot find the SDK. A
`.env.example` is committed because the Secrets plugin reads it; Firebase is
configured in code, so no keys are required.

The release build falls back to the debug signing config when no
`my-upload-key.jks` is present. For store uploads provide a keystore through
`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`. Updates must be signed with
the same key as the installed app, so keep using one keystore for releases.

A prebuilt signed (debug key) APK is included as `WaveFocus-2.2.apk`.

## Release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew assembleRelease`.
3. Upload `WaveFocus-<version>.apk` to
   <https://github.com/wave5ald/wavefocusAPK/releases> (tag `Release`).
4. Publish the manifest with `tools/publish-update.ps1` (see the root README)
   so installed apps see the new version on their next launch or manual check.
