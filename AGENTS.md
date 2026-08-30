# AGENTS.md

Guidance for AI coding agents and maintainers working in this repository.

## Project

Offline-first Android prayer tracker (`com.praytracker`): prayer times, Hijri
calendar, Tasbih, Qibla compass, reminders, and Glance widgets. Kotlin + Jetpack
Compose (Material 3), MVVM with `StateFlow`.

## Toolchain (do not change casually)

- Gradle wrapper `8.10.2`, AGP `8.7.3`, Kotlin `2.0.21`, KSP `2.0.21-1.0.28`
- `compileSdk`/`targetSdk` 35, `minSdk` 26, Java 17 target
- Package/applicationId: `com.praytracker`
- Remote: `https://github.com/sinanalizz123-max/prayer-tracker.git`

## Build and verify

Run everything from the repository root:

```bash
./gradlew assembleDebug testDebugUnitTest
```

- Local JVM for unit tests: OpenJDK 21. CI uses Java 17.
- Robolectric is NOT supported on this machine (Linux aarch64) — do not add
  Robolectric/Gradle Managed Device tests here.

## Unit tests

`app/src/test/` contains pure JVM tests (`HijriHelperTest`,
`PrayerCalculatorTest`, 16 passing). They must stay green on CI
`.github/workflows/build.yml` runs `assembleDebug testDebugUnitTest`.

## Architecture notes

- `SettingsManager` implements the `PrayerSettings` interface and is the single
  source of truth for settings. Its `settingsChanged` StateFlow is a monotonic
  counter; every UI that depends on settings must read it reactively (e.g. via
  `collectAsState` / `remember(settingsChanged)`) — never rely on a plain
  composition-time getter read alone.
- Prayer values flow through `MainViewModel` StateFlows combined on
  `settingsChanged` + a per-second clock (`_currentTime`).
- Qibla screen uses true heading (magnetic + `GeomagneticField` declination).

## Conventions

- No code comments unless they explain *why*.
- Keep commits small and scoped; only commit/push when the user asks.
- Before finishing a task: run `./gradlew assembleDebug testDebugUnitTest` and,
  if a change was pushed, confirm the CI run is green.