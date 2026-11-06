# Changelog

## 1.1.0

### New Features
- **Imsak Time**: Added Suhoor stopping time (20 minutes before Fajr) to Ramadan mode with prominent red-highlighted card
- **Jumu'ah Prayer**: Automatically displays Jumu'ah prayer on Fridays at Dhuhr time, with Dhuhr hidden on Fridays
- **Islamic Events Calendar**: Enhanced with 20+ Islamic events (Eid al-Fitr, Eid al-Adha, Ashura, Mawlid, Isra & Mi'raj, etc.) with both English and Arabic names
- **Adhan Audio**: Play Islamic call to prayer audio when prayer notifications fire, with volume control and reciter selection
- **Share Prayer Times**: Generate and share beautiful prayer times image via WhatsApp or other apps with date, location, and Hijri date
- **App Shortcuts**: Quick access to Prayer Times, Qibla Compass, Tasbih Counter, and Ramadan Mode from home screen
- **Localization**: Added support for Arabic, Urdu, Malay, and Indonesian languages

### Improvements
- **Compass Visual**: Enhanced with Material 3 Expressive design featuring gradient ring, Kaaba cube symbol, cardinal directions (N, E, S, W), and improved readability
- **Color Scheme**: Added custom color palette for better visual hierarchy
- **UI/UX**: Improved overall user interface with better typography and spacing

### Technical
- Added FileProvider for image sharing
- Added app shortcuts configuration
- Prepared project structure for Wear OS support
- Added string resources for multiple languages

## 1.0.3

### Performance
- Prayer schedule recomputes only when its inputs (location, calculation
  method, madhab, latitude rule, adjustments, hijri correction, date) change,
  instead of every second.
- Per-second countdown now reads cached today/tomorrow schedules; the hero
  countdown card is the only subtree recomposing every tick.
- Qibla heading and magnetic declination update at sensor rate only in the
  small alignment pill and compass dial leaves, not the full screen.

### Reliability
- Survive polar-day / polar-night locations (e.g. Longyearbyen): the adhan
  library leaves Fajr/Isha `null` at extreme latitudes; the calculator now
  substitutes ordered sentinel solar times only where null, so normal
  locations are unaffected.
- Alarm arming reconciles desired plan against armed plan and arms/cancels
  only deltas, so firing one alarm no longer tears down and re-arms every
  other alarm.

### Data integrity
- Coordinates persist as full-precision strings (`latitude_precise` /
  `longitude_precise`) instead of 32-bit floats; old stored coordinates
  migrate seamlessly.

### Tests (36 passing)
- AlarmPlanBuilder — full plan, master-off, flag gating, past-day omission,
  follow-up delays, code-range coverage.
- PrayerCalculator — ordering across all 5 methods, Hanafi-Asr, high-latitude
  rules and seasons, polar no-crash, timezone correctness, midnight
  rollover, cached-schedule fidelity.
- SettingsCoordinate — fallback, precision, invalid handling, round-trip.

## 1.0.2
Initial repository release.
