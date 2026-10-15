---
title: Test farm
layout: page
parent: Information for developers
has_toc: false
---

# Test farm

The test farm runs long-term tests of **release builds** on physical phones
plugged into a developer's PC. It complements CI: the
[Maestro UI tests]({% link developers/maestro-tests.md %}) in CI run once per
change on emulators, while the farm keeps real phones exercising the app for
days at a time and upgrades them as new releases are built.

The farm is a single Python script, `test-farm/farm.py`. The setup steps and
every config option are in
[`test-farm/README.md`](https://github.com/Scottish-Tech-Army/Soundscape-Android/blob/main/test-farm/README.md).

## What it does

Each Android phone loops through a **cycle**:

1. If a newer release has been built, it installs it over the existing app and
   data (`adb install -r`), then runs `test-farm/flows/PostUpgrade.yaml` to
   check that the app starts and the suite's markers and route survived the
   upgrade.
2. A `monkey` soak: random input inside the app, logged with its seed so a
   failure can be reproduced.
3. The Maestro suite, in `maestro/Master.yaml` order. After `Onboarding.yaml`
   it pushes an offline map and runs `test-farm/flows/OfflineSearch.yaml`, so
   every flow runs offline.
4. It collects crashes and ANRs (from Android's dropbox), logcat, memory (PSS)
   and battery.

Results go into `test-farm/farm.db`, with a summary page at
`test-farm/report.html` that refreshes every minute.

iPhones are **observed, not driven**. Maestro can't control a physical iPhone,
because its iOS driver needs Xcode on a Mac. Instead the farm uses
[pymobiledevice3](https://github.com/doronz88/pymobiledevice3) to:
- replay a GPX walk;
- record TestFlight upgrades, and app restarts when the app wasn't running;
- collect Soundscape crash and hang reports, battery level and a Soundscape
  syslog.

## How it differs from CI

| | CI | Test farm |
|---|---|---|
| Devices | Emulators | Physical phones, including an AOSP phone without Play services |
| Build | Debug | The release APK from the `Tag and build release` workflow |
| Upgrades | Not tested | Each release is installed over the previous one, keeping the data |
| Map data | Glasgow extract pushed as root | The current extract from the manifest, pushed without root, with search set to Offline |
| Location | `adb emu geo fix` | A replayed GPX walk with speed and bearing, held at CI's Edinburgh point while the suite runs |
| Duration | One run | Continuous |

## Making release builds testable

The farm doesn't use any debug-only hooks:

- **Analytics:** setting `firebase.test.lab` makes `MainActivity` use dummy
  analytics, as it does in Firebase Test Lab.
- **Location:** a small mock location helper app (`test-farm/mock-location/`,
  not part of Soundscape) sets every fix, including speed and bearing.
  Soundscape only takes its direction from travel when a fix says the user is
  moving. Without speed and bearing it uses the phone's compass, and on a desk
  that points nowhere useful.
- **Maestro:** `testTagsAsResourceId` is set for every build type, so the same
  flows work on release builds.

## Things that will catch you out

- Phones need **Screen lock: None**. Maestro can't get past a PIN, and the farm
  reports a locked phone instead of testing it.
- Phones must be in **English**. The Settings screen has no testTags, so
  `OfflineSearch.yaml` taps its English labels.
- Remove any Play Store or debug copy of Soundscape first. The farm only tests a
  build it installed itself, and reports anything else as blocked.
- Keep phones off USB hubs that lose disconnects. One here kept a ghost iPhone
  connection that hung `usbmuxd`.
