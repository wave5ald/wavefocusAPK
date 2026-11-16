# Wave Focus — Android (APK) 2.0

Kotlin + Jetpack Compose routine tracker for Android 8.0+ (minSdk 26).

## v2 changes

- Saudi (Umm al-Qura) Hijri verification on launch / reconnect via
  `app/src/main/java/com/example/util/HijriSync.kt`; the day offset is applied
  in `util/HijriCalendarHelper.kt`.
- Configurable task reset time in Settings → *Day & Calendar*
  (`util/RoutineDay.kt`, `RoutineViewModel.setDayResetMinutes`) with automatic
  rollover when the reset time passes.
- `versionCode 2`, `versionName "2.0"`.

## Build

```bash
./gradlew assembleRelease
```

Requirements: JDK 17+ and an Android SDK with API 36. Create
`local.properties` with `sdk.dir=...` if Gradle cannot find the SDK.

The release build falls back to the debug signing config when no
`my-upload-key.jks` is present. For store uploads provide a keystore through
`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`.

A prebuilt signed (debug key) APK is included as `WaveFocus-2.0.apk`.
