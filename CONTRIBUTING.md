# Contributing to Lumen

Thank you for helping improve Lumen.

## Before you start

- Search existing issues and pull requests first.
- For anything larger than a small fix, open an issue to discuss the idea before writing code.

## Development setup

1. Install JDK 21 and Android SDK 37.
2. Clone the repository and open it in Android Studio, or build with `./gradlew :app:assembleDebug`.
3. Test on a real device running Android 12 or newer, and set Lumen as the home app.

## Guidelines

- **Design**: follow Material 3 Expressive. Use the shared components in `ExpressiveKit.kt`, the shapes in
  `LumenShapes` and the motion tokens in `Springs` rather than one-off values.
- **Strings**: put user-visible text in `res/values/strings.xml`.
- **Permissions**: every new permission must be optional, requested only when its feature is turned on,
  and documented in the README and `PRIVACY.md`.
- **Privacy**: no analytics, tracking or new network calls without discussion.
- **Performance**: read animation values inside `graphicsLayer` or draw lambdas, not during composition.
- **Adaptive layouts**: check portrait, landscape and large screens.

## Pull requests

1. Create a branch from `main`.
2. Keep each pull request focused on one change, with a clear description and screenshots for UI changes.
3. Make sure `./gradlew :app:assembleRelease` succeeds.

By contributing you agree that your contributions are licensed under the Apache License 2.0.
