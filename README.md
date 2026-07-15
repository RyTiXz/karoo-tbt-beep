# TBT Beep — quiet turn-by-turn sounds for Hammerhead Karoo

[![Build](https://github.com/RyTiXz/karoo-tbt-beep/actions/workflows/android.yml/badge.svg)](https://github.com/RyTiXz/karoo-tbt-beep/actions/workflows/android.yml)

The stock turn-by-turn (TBT) alert on the Karoo is loud and has no volume control.
This extension replaces it: you disable the stock TBT audio alert and TBT Beep plays
its own, configurable tones instead — at distances you choose.

Inspired by [kxradar](https://github.com/itxsvv/kxradar), which does the same for radar alerts.

## How it works

The extension streams the Karoo's *Distance to Next Turn* value
(`DISTANCE_TO_NEXT_TURN` from the official [karoo-ext SDK](https://github.com/hammerheadnav/karoo-ext))
and fires up to three independent alerts:

- **Early alert** — optional long-range heads-up, e.g. 1000 m (default: off, 3 beeps)
- **Approach alert** — e.g. 250 m before the turn (default: 2 beeps)
- **At-turn alert** — e.g. 50 m before the turn (default: 1 beep)

For each alert you can configure the trigger distance, tone frequency (Hz),
duration (ms) and beep count. Lower frequency + shorter duration = quieter.
Sounds play through the internal beeper via the SDK (`PlayBeepPattern`);
there is no hardware volume control on the Karoo, so perceived loudness is
shaped by frequency and duration.

Thresholds should be configured in descending order (early > approach >
at-turn). Alerts re-arm automatically after each turn (detected by the
distance value jumping up) and after reroutes. On consecutive close turns,
thresholds the rider is already inside of are skipped instead of beeping
retroactively — only what still lies ahead is announced.

## Requirements

**You must disable the default TBT sound in the Karoo settings**, otherwise
you will hear both sounds:
Profiles → *your profile* → Audio Alerts → disable **Turn By Turn**.

Navigation (a routed ride) must be active — without a route there is no
next-turn distance and no beeps.

## Installation

**Karoo 3 / Karoo 2 (KOS 1.524+):**
Share this link with the Hammerhead Companion App:
[Latest APK](https://github.com/RyTiXz/karoo-tbt-beep/releases/latest/download/app-release.apk)

**Sideloading via adb:** download the APK from the
[releases page](https://github.com/RyTiXz/karoo-tbt-beep/releases) and run
`adb install app-release.apk`.

## Usage

Open the *TBT Beep* app on the Karoo, adjust the two alerts, test them with the
play button, and tap **Save**. Options:

- **Enabled** — master switch
- **In-ride only** — only beep while a ride is being recorded
- **Wake up screen** — turn the screen on when an alert fires

## Privacy

The app requests no Android permissions, contains no network code and collects
no data. Settings are stored locally on the device (Jetpack DataStore) and
never leave it. The only inputs are the Karoo's own distance-to-next-turn and
ride-state streams via the official SDK; the only output is the internal beeper.

## Building

CI (GitHub Actions) builds and unit-tests the app on every push
(`./gradlew build`). Locally you need JDK 17 and the Android SDK; `karoo-ext`
is resolved from Maven Central (with GitHub Packages as fallback — see
`settings.gradle.kts`).

Releases since v0.3.0 are signed with a fixed release key (repository secrets
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), so
updates install in place. If you are upgrading from an older debug-signed
build (v0.1.0/v0.2.0), uninstall once before installing.

### Release checklist

1. Bump the version in three places: `versionCode`/`versionName` in
   `app/build.gradle.kts`, the `KarooExtension` constructor in
   `KarooTbtExtension.kt`, and `latestVersionCode`/`latestVersion` in
   `app/manifest.json`.
2. Push to `main`, wait for CI to pass.
3. Draft a GitHub release with tag `vX.Y.Z` — CI attaches the APK,
   `manifest.json` and icon automatically.

## Credits

- [kxradar](https://github.com/itxsvv/kxradar) by itxsvv — architecture template (Apache-2.0)
- [karoo-ext](https://github.com/hammerheadnav/karoo-ext) by Hammerhead — official SDK

## License

[Apache-2.0](LICENSE)
