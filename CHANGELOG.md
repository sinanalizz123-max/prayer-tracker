# Changelog

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
