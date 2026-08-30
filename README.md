# Prayer Tracker

A private, offline-first Android app for prayer times, the Hijri calendar, and Qibla direction.

[![Build](https://github.com/sinanalizz123-max/prayer-tracker/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/sinanalizz123-max/prayer-tracker/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Features

- **Prayer times** — daily prayer schedule with multiple calculation methods
  (MWL, ISNA, Egypt, Makkah, Karachi, Jafari, Tehran, Gulf, …), madhab-based Asr,
  high-latitude rules, and per-prayer manual minute offsets.
- **Location aware** — automatic GPS detection or manual city search, with
  automatic timezone handling. Fully optional: you can use a selected city only.
- **Hijri calendar** — monthly lunar calendar, Islamic event dates, moon-sighting
  day adjustment (±2 days), and Eastern Arabic numeral support.
- **Qibla compass** — live compass with true-north correction via local magnetic
  declination and a "facing the Kaaba" alignment indicator.
- **Tasbih counter** — preset and custom dhikr with translations, haptic feedback,
  and persistent counts.
- **Reminders** — per-prayer notifications, follow-up reminders, and optional
  silent alerts.
- **Widgets** — home-screen glances powered by Glance.
- **Privacy first** — no accounts, no data collection; everything stays on-device.

## Tech stack

| Layer   | Choice                                                                 |
| ------- | ---------------------------------------------------------------------- |
| UI      | Jetpack Compose (Material 3), Compose BOM `2024.12.01`                 |
| Language| Kotlin `2.0.21` (Compose compiler plugin), Java 17 target              |
| Arch    | MVVM — `AndroidViewModel` + Kotlin `StateFlow`                         |
| Data    | Room (persistence) + `SharedPreferences` (settings), coroutines        |
| Math    | [adhan](https://github.com/batoulapps/adhan-java) prayer calculations  |
| Maps    | Google Play Services Location                                          |
| Widgets | Jetpack Glance                                                         |
| Build   | Gradle `8.10.2`, AGP `8.7.3`, KSP, `compileSdk`/`targetSdk` 35, `minSdk` 26 |

## Project structure

```
.
├── app/
│   └── src/
│       ├── main/java/com/praytracker/
│       │   ├── data/        # SettingsManager, Room repository, entities
│       │   ├── ui/          # ViewModels, screens, reusable components, theme
│       │   ├── util/        # PrayerCalculator, HijriHelper, CompassManager,
│       │   │                # AlarmScheduler/Receiver, location utilities
│       │   └── ...          # MainActivity, application class, widgets
│       └── test/            # JVM unit tests (HijriHelper, PrayerCalculator)
└── .github/workflows/       # CI: build debug APK + unit tests; signed release build
```

## Getting started

### Prerequisites

- JDK 17+
- Android SDK (compile SDK 35)
- An Android device or emulator running API 26+

### Build

```bash
# Debug APK + unit tests
./gradlew assembleDebug testDebugUnitTest

# Debug APK only
./gradlew assembleDebug

# Local unit tests only
./gradlew testDebugUnitTest

# Release build
./gradlew assembleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
The release APK (signed where signing material is configured) is written to
`app/build/outputs/apk/release/app-release.apk`.

### Install

Users install the app from **GitHub Releases** — no Google Play. Get the latest
`PrayerTracker-vX.Y.Z.apk` from
<https://github.com/sinanalizz123-max/prayer-tracker/releases> and install it on
an Android device (API 26+).

For local development:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## CI

The `build.yml` workflow runs on every push to `main` and on pull requests:

1. Checks out the repository.
2. Sets up Temurin JDK 17 and the Gradle toolchain.
3. Runs `./gradlew assembleDebug testDebugUnitTest`.
4. Uploads the debug APK as the `prayer-tracker-debug-apk` artifact and test
   results as the `unit-test-results` artifact (on failure).

You can also run it manually from the Actions tab (workflow_dispatch).

### Releases & signing

Distribution is **GitHub Releases only**. Pushing a tag named `vX.Y.Z` triggers
the `release.yml` workflow, which builds a *signed* release APK and attaches it
to the GitHub Release automatically.

The keystore and its passwords are **never committed**. The `release` signing
config in `app/build.gradle.kts` is only activated when these are present in the
environment (CI) or in `~/.gradle/gradle.properties` (local):

| Variable                    | Purpose                             |
| --------------------------- | ----------------------------------- |
| `PRAYTRACKER_KEYSTORE_PATH` | Absolute path to the `.jks` file    |
| `PRAYTRACKER_KEYSTORE_PASSWORD` | Keystore password               |
| `PRAYTRACKER_KEY_ALIAS`     | Signing key alias                   |
| `PRAYTRACKER_KEY_PASSWORD`  | Signing key password                |

GitHub Secrets needed for CI (see the workflow for exact names):

| Secret                        | Value                                                     |
| ----------------------------- | --------------------------------------------------------- |
| `PRAYTRACKER_KEYSTORE_BASE64` | `base64 -w0` of `prayer-tracker-release.jks` (one line)   |
| `PRAYTRACKER_KEYSTORE_PASSWORD` | Keystore password                                       |
| `PRAYTRACKER_KEY_ALIAS`       | Signing key alias                                         |
| `PRAYTRACKER_KEY_PASSWORD`    | Signing key password                                      |

To release: bump `versionCode`/`versionName` in `app/build.gradle.kts`, commit,
then `git tag vX.Y.Z && git push origin vX.Y.Z`. The workflow publishes a signed
`PrayerTracker-vX.Y.Z.apk` under <https://github.com/sinanalizz123-max/prayer-tracker/releases>.

## Documentation

- [Contributing](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security](SECURITY.md)
- [Changelog](CHANGELOG.md)

## License

[MIT](LICENSE) — see the [LICENSE](LICENSE) file for details.