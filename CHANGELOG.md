# Changelog

All notable changes to Lumen are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- **Welcome tour** on first run: set Lumen as the home app, turn on optional extras (notifications, weather, contacts) with a short reason for each, and pick a clock, accent colour and icon shape. It can be replayed from Customise.
- **Search in Customise**, which filters settings as you type and opens the matching sections.
- **New logo and app icons**: the Lit Corner mark (an L holding a light, with M3 shapes spilling out) and a custom-drawn wordmark replace the script logo. The app icon picker now offers four marks in twelve colours.
- **Expressive press motion** on buttons, chips, swatches, tiles and cards, and all M3 components now follow the bounce and speed settings.
- **Segmented, M3 Expressive lists** in search and notifications, switches with a check icon, and a marked choice in the gesture menu.
- **Folders** on the home grid and in the dock. Drop one app on another to make a folder, then rename it, remove apps from it, or move it to or from the dock.
- **Multiple home pages**, from one to five, with page dots and a "move to next or previous page" option in the app menu.
- **Icon packs** that use the ADW, Nova, GO, Apex, Teslacoil or Lawnchair theme formats (`appfilter.xml`), with a picker in Customise.
- **Restore settings** from an exported backup file.
- **Translations** into Spanish, French, German, Arabic (right-to-left) and Hindi, with per-app language support on Android 13 and newer.
- **City search** for weather, using Open-Meteo geocoding, for when location is off or not allowed.
- Weather status messages (no permission, location off, no fix, offline) and a button to turn on location.
- Unit tests for search ranking, the calculator, unit conversion and the settings index.

### Changed
- **Customise is now compact**: each section is a header with a one-line summary that expands smoothly when tapped, and switch rows react to a press anywhere on the row.
- Accent colours are now built from tone targets with a contrast check, so text and icons stay readable on every accent, blue included.
- The home screen menu opens reliably on long-press, with a clear remove button; folders on the home screen can be removed from their sheet.
- Icon shapes are now baked into the cached bitmaps, so the GPU no longer clips each icon. This removes most of the jank when scrolling the app drawer on low-end phones.
- When there is no recent fix, weather now asks for a fresh location (with a 12-second timeout) and retries every minute until it succeeds.
- Removed the Google Lens button from the search pill; voice search stays.

### Removed
- Unused strings.

## [1.0] - 2026-11-24

### Added
- First public release:
  - Expressive clock faces, glance chips, a free-form home grid with widgets, the dock and the search pill.
  - Control panel with tiles detected from the device and grouped, swipeable notifications.
  - Universal search with quick actions, a calculator, unit conversion and indexed system settings.
  - Optional lock-screen cover with a Now Bar media player.
  - Around 50 personalisation options and 72 alternative app icons.

[Unreleased]: https://github.com/faizhussain7/Lumen/compare/46dbcf7...HEAD
[1.0]: https://github.com/faizhussain7/Lumen/commit/46dbcf7
