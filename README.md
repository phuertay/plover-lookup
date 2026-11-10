# Plover lookup

Personal offline Android app for a Plover dictionary that is too large to open in the browser on a tablet. Search follows the core of the [lim.au Plover lookup](https://lim.au/#/software/steno-lookup/plover/): type an outline or a translation and see the matches.

The app is Kotlin and Jetpack Compose over a local SQLite database. It is not a WebView. It does not sign in, sync, or use the network.

## Search

Enabled dictionaries are searched together. Each match shows the dictionary it came from.

- An outline matches when it is the same stroke after trimming, removing spaces, and uppercasing. `kat` finds `KAT`.
- A translation matches the text itself, the start of the folded text, and a few endings (`s`, `es`, `ed`, `ing`, and the same kind of fold the lim.au lookup uses). Folding lowercases, removes spaces, and unwraps Plover affixes such as `{^ing}` to `ing`.

## Dictionaries

A tiny sample dictionary is built in, so search works before any import. Import one or more Plover JSON files with the system file picker. The file is parsed as a stream and inserted in batches inside one transaction. Outline and translation are indexed.

Personal dictionaries stay on the device. Do not commit one to this repository.

## Build

JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is signed with the debug key and can be sideloaded. minSdk is 26 (Android 8.0).

## Continuous integration

[`.github/workflows/android_debug.yml`](.github/workflows/android_debug.yml) runs on `ubuntu-latest`. It sets up Temurin 17 with `actions/setup-java`, installs the Android SDK, runs the unit tests and `assembleDebug`, and uploads the debug APK.

Download it from a run of the [Android debug workflow](https://github.com/phuertay/plover-lookup/actions/workflows/android_debug.yml): open the run and download the `plover-lookup-debug` artifact.

## License

Copyright (c) 2026 Pedro Huerta.

Licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (SPDX: `CC-BY-NC-SA-4.0`). The official legal text is in [LICENSE](LICENSE).

https://creativecommons.org/licenses/by-nc-sa/4.0/
