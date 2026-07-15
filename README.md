# TBT Beep — quiet turn-by-turn sounds for Hammerhead Karoo

[![Build](https://github.com/RyTiXz/karoo-tbt-beep/actions/workflows/android.yml/badge.svg)](https://github.com/RyTiXz/karoo-tbt-beep/actions/workflows/android.yml)

The stock turn-by-turn (TBT) alert on the Karoo is loud and has no volume control.
This extension replaces it: you disable the stock TBT audio alert and TBT Beep plays
its own, configurable tones instead — at distances you choose.

Inspired by [kxradar](https://github.com/itxsvv/kxradar), which does the same for radar alerts.

## How it works

The extension streams the Karoo's *Distance to Next Turn* value
(`DISTANCE_TO_NEXT_TURN` from the official [karoo-ext SDK](https://github.com/hammerheadnav/karoo-ext))
and fires two independent alerts:

- **Approach alert** — e.g. 100 m before the turn (default: 1 beep)
- **At-turn alert** — e.g. 20 m before the turn (default: 2 beeps)

For each alert you can configure the trigger distance, tone frequency (Hz),
duration (ms) and beep count. Lower frequency + shorter duration = quieter.
Sounds play through the internal beeper via the SDK (`PlayBeepPattern`);
there is no hardware volume control on the Karoo, so perceived loudness is
shaped by frequency and duration.

The approach-alert distance should be larger than the at-turn distance.
Alerts re-arm automatically after each turn (detected by the distance value
jumping up) and after reroutes.

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

## Building

CI (GitHub Actions) builds the APK on every push. Locally you need JDK 17 and
the Android SDK; `karoo-ext` is resolved from Maven Central (with GitHub
Packages as fallback — see `settings.gradle.kts`).

If no release keystore is configured (repository secrets `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), release builds are signed
with the debug key. Note: debug-signed builds from different machines/CI runs
have different signatures, so updating may require uninstalling first.

## Credits

- [kxradar](https://github.com/itxsvv/kxradar) by itxsvv — architecture template (Apache-2.0)
- [karoo-ext](https://github.com/hammerheadnav/karoo-ext) by Hammerhead — official SDK

## License

[Apache-2.0](LICENSE)
