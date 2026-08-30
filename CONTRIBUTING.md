# Contributing to Prayer Tracker

Thanks for your interest in contributing! This guide keeps contributions easy to
review and safe to merge.

## Code of conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting started

1. Fork the repository and clone your fork.
2. Create a branch: `git checkout -b feature/my-change`.
3. Make your changes.
4. Verify locally (see below) and commit with a clear message.
5. Open a pull request against `main`.

## Development setup

- JDK 17+ and Android SDK 35 are required.
- Build and test from the repository root:

```bash
./gradlew assembleDebug testDebugUnitTest
```

## Testing

- Unit tests live under `app/src/test/` and are pure JVM (no emulator required).
  Always run them before pushing:

```bash
./gradlew testDebugUnitTest
```

- CI runs both `assembleDebug` and `testDebugUnitTest`. A pull request must keep
  both green.

## Code style

- Follow the existing Kotlin conventions (official Kotlin style).
- Do not add comments unless they explain *why* something is done a certain way.
- Prefer small, focused commits and meaningful messages:
  e.g. `Fix Qibla compass true-north correction`.
- Keep changes scoped to the issue/feature at hand.

## Commit messages

Use the imperative mood and mention the subsystem, e.g.:

```
Fix settings reactivity so theme changes apply immediately
```

## Pull requests

- Reference the issue you are fixing when possible.
- Describe what changed and why.
- Keep the diff minimal and reviewable.