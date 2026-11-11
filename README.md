# Plover lookup

Personal offline Android app for a Plover dictionary that is too large to open in the browser on a tablet. Search follows the core of the [lim.au Plover lookup](https://lim.au/#/software/steno-lookup/plover/): type an outline or a translation and see the matches.

The app is Kotlin and Jetpack Compose over a local SQLite database. It is not a WebView. It does not sign in, sync, or use the network.

## Search

Enabled dictionaries are searched together. Dictionary name is a switch on the dictionaries screen, off until you turn it on. When it is on, the name sits on the same line as the stroke.

A stroke has one translation. Imported dictionaries are checked in the order they were added, and the built-in Lapwing dictionary is checked last. The first dictionary that defines a stroke wins, and a later copy of that stroke is ignored. Inside one file, the first entry for a stroke is kept. A winning `{plover:deleted}` removes the stroke.

- An outline matches when it is the same stroke after trimming, removing spaces, and uppercasing. `kat` finds `KAT`. Searching `S-T` shows that one translation, not every dictionary that also defined `S-T`.
- A translation matches the text itself, the start of what you typed, and a few endings (`s`, `es`, `ed`, `ing`). Matching the text itself folds case, removes spaces, and unwraps Plover affixes such as `{^ing}` to `ing`, so `theyear` finds `the year`. A prefix match keeps spaces, so `they` finds `they're` and does not find `the year`. An outline is included only when its winning translation is the one that matched, so `is the` does not list a stroke whose earlier entry is `is it`.

## Dictionaries

The built-in dictionary is the Lapwing dictionary from the [lim.au lookup](https://lim.au/#/software/steno-lookup/lapwing/). It includes the briefs `T` and `EUT` for `it`, and `-T` for `the`. In that dictionary `THE` is `they`, not `the`. Import one or more Plover JSON files with the system file picker. An import is checked before the built-in dictionary. The file is parsed as a stream and inserted in batches inside one transaction. Outline and translation are indexed.

Personal dictionaries stay on the device. Do not commit one to this repository.

## Build

JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is signed with the sideload key in `app/sideload.keystore`, so a newer build installs over the previous one. On GitHub Actions the version code is the workflow run number. minSdk is 26 (Android 8.0).

## Continuous integration

[`.github/workflows/android_debug.yml`](.github/workflows/android_debug.yml) runs on `ubuntu-latest`. It sets up Temurin 17 with `actions/setup-java`, points `ANDROID_HOME` and `ANDROID_SDK_ROOT` at the same Android SDK, runs the unit tests and `assembleDebug`, and signs the APK with the sideload key.

The current APK is [plover-lookup.apk](https://github.com/phuertay/plover-lookup/releases/download/sideload/plover-lookup.apk). That link is the APK file. A run of the [Android debug workflow](https://github.com/phuertay/plover-lookup/actions/workflows/android_debug.yml) on `main` replaces it and repeats the same link in the run summary. Builds from before the sideload key used a different signature, so the first install of this APK needs the old app removed once. Later downloads update in place.

## License

Copyright (c) 2026 Pedro Huerta.

Licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (SPDX: `CC-BY-NC-SA-4.0`). The official legal text is in [LICENSE](LICENSE).

https://creativecommons.org/licenses/by-nc-sa/4.0/
