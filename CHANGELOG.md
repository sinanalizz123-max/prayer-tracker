# Changelog

All notable changes to this project are documented in this file.

## [1.0.2] - 2026-08-30

### Fixed
- Theme changes now apply immediately (theme value is read at flow emission, not at composition).
- Settings toggles (Arabic numerals, notifications, silent alerts, translation, haptics, GPS) now update the UI the moment they are changed.
- Compass dial animates on the shortest angular path so the 359° → 1° wrap no longer spins the full dial.
- Qibla direction is corrected from magnetic to true north using the local magnetic declination and is recomputed when the location changes.
- Follow-up reminder is scheduled at the configured delay after each prayer; cancelling alarms clears follow-up codes too.
- Silent Alerts toggle now actually silences the notification sound/vibration.
- GPS permission is requested at tap time across the prayer list, settings, and location picker.

## [1.0.1] - 2026-08-30

### Fixed
- Settings changes now recompose the whole UI tree immediately instead of requiring an app restart.

## [1.0.0] - 2026-08-30

### Added
- Initial port of the prayer tracker app to `com.praytracker`.
- Prayer times, Hijri calendar, Tasbih, Qibla compass, reminders, and Glance widgets.