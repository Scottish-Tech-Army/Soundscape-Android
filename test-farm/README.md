# Test farm

`farm.py` runs long-term tests of **release builds** on every Android phone
plugged into this PC. Each phone loops forever:

1. If a newer release has been built, `adb install -r` it over the existing
   app and data, then run `flows/PostUpgrade.yaml` to check the app starts and
   the suite's markers and route survived.
2. A `monkey` soak (random input inside the app, fixed seed per run, logged).
3. The Maestro suite in `maestro/Master.yaml` order, one flow at a time.
4. Collect crashes and ANRs (from dropbox), logcat, memory (PSS) and battery.

Releases come from the upstream `build-app.yaml` workflow's `release-apk`
artifact (the APK that goes to Play), via `gh`. The newest two are cached in
`apks/`, and a freshly attached phone starts one release behind so its first
upgrade happens after its first cycle. Artifacts expire after 90 days, so
leave the farm running (or keep `apks/`) to keep older releases.

Results: `report.html` (refreshes every minute), `farm.db` (SQLite), and
per-cycle files under `runs/<serial>/<timestamp>/`.

```bash
cp farm.toml.example farm.toml   # optional
python3 test-farm/farm.py        # Ctrl-C to stop; removes the mock GPS providers
```

## How release builds are made testable

- **Analytics:** the farm sets `settings put system firebase.test.lab true`, which
  makes `MainActivity` use dummy analytics, as it does in Firebase Test Lab.
- **Location:** release builds don't have the debug `REPLAY_GPX` intent, so the
  farm adds shell mock providers for `gps` and `network`
  (`cmd location providers ...`) and feeds them a GPX track once a second.
  This reaches both the fused provider (phones with Play services) and plain
  `LocationManager` (AOSP phones). While the Maestro suite runs, the phone is
  held at `suite_location` (CI's Edinburgh point), because the flows assert
  distance-sorted list order.
- **Maestro:** `testTagsAsResourceId` is set in `Theme.kt` for every build type.

## Preparing a phone

- Developer options on, USB debugging on, and on Xiaomi/Oppo-type phones also
  "USB debugging (Security settings)" so adb can tap and install.
- Screen lock **None**. Maestro can't get past a PIN.
- Uninstall any existing Soundscape first. The farm only tests a build it
  installed itself (it records the install time), so a Play Store or debug
  copy leaves the phone **blocked** and untested, unless
  `allow_reinstall = true`, which uninstalls the copy and its data.
- Turn off Play Store auto-updates, so Play doesn't replace the farm's build.
- Charge limit (e.g. 80%) where the phone has one. A phone on charge 24/7
  without one will eventually swell its battery.
- Use a powered USB hub.

The farm sets the screen to minimum brightness and media volume to zero when a
phone is attached and again after every monkey soak. The audio still plays at
zero volume, so the audio path is still tested.

The farm deliberately leaves the OEM battery savers alone. If one kills the
service, that shows up as a failure, which is a real user problem.

## Known limits

- The suite wipes app data (Onboarding uses `clearState`), so a phone's data
  only lives from the end of one cycle to the start of the next. That window
  is where upgrades happen.
- The monkey can open the notification shade. After each soak the farm turns
  off airplane mode, turns on Wi-Fi and collapses the shade.
- Maestro runs on several phones at once. One run lost its driver connection
  (`gRPC UNAVAILABLE`) when another phone's session was starting, and that
  error is now retried once. If it keeps happening, set
  `serialize_maestro = true`.
