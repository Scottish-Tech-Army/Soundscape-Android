# Test farm

`farm.py` runs long-term tests of **release builds** on every Android phone
plugged into this PC. Each phone loops forever:

1. If a newer release has been built, `adb install -r` it over the existing
   app and data, then run `flows/PostUpgrade.yaml` to check the app starts and
   the suite's markers and route survived.
2. **Set up**, once per release on each phone (recorded in the `setups`
   table): the Maestro suite in `maestro/Master.yaml` order, one flow at a
   time, held at `suite_location`. Onboarding wipes the app, so after it the
   farm pushes the offline map and runs `flows/FarmSettings.yaml` (search mode
   Offline). After the suite, which expects the map off as in CI,
   `flows/ShowMap.yaml` turns the map on for roaming (or leaves it off
   for a phone with `show_map = false` in its `[devices]` entry, which the
   roaming flows then skip the map steps on). A phone whose Onboarding or
   settings fail is set up again next cycle.
3. **Roam**: `roam_rounds` rounds of `flows/roam/*.yaml`, shuffled each round,
   while the phone walks its GPX track. These look around (callouts, beacons on
   places and search results, the route, markers, menus, sleep) and assert
   only that the screens work, not what's nearby. They use the markers and
   route the set-up made, and change nothing that later flows rely on.
   Each starts with `flows/LaunchHome.yaml`, which brings the app to the
   front and backs out to Home without restarting it, so one app process runs
   through the rounds, the monkey and following cycles, soaking for hours.
4. A `monkey` soak (random input inside the app, fixed seed per run, logged).
   Then `flows/Restore.yaml`, without restarting the app, puts back what the
   monkey may have changed and roaming and the next upgrade rely on: search
   mode Offline, markers B and C and Test Route (recreated where the suite
   made them), and the map on. Only if that fails is the phone set up again
   (a `reset` event).
5. Collect crashes and ANRs (from dropbox), logcat, memory (PSS), battery
   and how long the app process has been up (`app_uptime_s`; a value that
   resets without a crash means something else restarted the app).

Flow results are recorded by kind: `flow` (set-up suite), `setup`
(FarmSettings, DownloadExtract, ShowMap), `roam`, `monkey`, `restore` and
`upgrade`.

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
- **Location:** release builds don't have the debug `REPLAY_GPX` intent, so
  the farm installs a small mock location helper (`mock-location/`, built by
  its `build.sh` with plain SDK tools; it isn't Soundscape) and grants it the
  mock location app-op. It feeds the helper's `gps` and `network` test
  providers a fix once a second, with **speed and bearing**. Soundscape only
  takes its direction from travel when a fix says the user is moving.
  Without them (all that `cmd location providers` can set), callouts follow
  the compass of a phone lying on a desk. This reaches both the fused provider
  (phones with Play services) and plain `LocationManager` (AOSP phones). The
  helper's receiver requires `android.permission.DUMP`, so only adb can feed it.
  Tracks are thinned to points 15 m apart,
  because a phone recording that starts with its owner standing still
  begins with minutes of on-the-spot GPS jitter. While the Maestro suite runs, the phone is
  held still (speed 0) at `suite_location` (CI's Edinburgh point), because
  the flows assert distance-sorted list order.
  iPhones can't get the same fix: `simulate-location` sets latitude and
  longitude only. Whether iOS adds a course to simulated fixes is untested.
- **Maestro:** `testTagsAsResourceId` is set in `Theme.kt` for every build type.
- **Offline maps and search:** the phones' Wi-Fi can't be relied on, so the
  farm keeps the current manifest extract (`extract_name`, default Glasgow,
  which covers the suite point and the GPX walk) in `extracts/`, using
  `extractProviderUrl` from `local.properties`. Onboarding wipes app storage,
  so after it runs the farm `adb push`es the extract into the
  app's `files/Download`. That needs no root on most phones. Where the
  shell can't write to other apps' `Android/data` (the Cubot J10), the app
  downloads the extract itself instead (`flows/DownloadExtract.yaml`), through
  this PC's network: the farm runs a small HTTP proxy (`usb_proxy.py`),
  `adb reverse`s its port onto the phone and sets the phone's global HTTP
  proxy to it for the download only.
  It then runs `flows/FarmSettings.yaml` to set Settings › Search › Search
  mode to Offline. Releases
  before the Settings test tags are driven by English labels, so the phones
  must be set to English for now. The farm turns **Wi-Fi off** on every
  phone when it's attached and after each monkey soak, so everything runs
  offline. That also stops Google's fused location (on phones with Play
  services) mixing the phone's real Wi-Fi position into the mock fixes.

## iPhones

Maestro can't drive a physical iPhone. It drives iOS through an XCUITest
driver that only Xcode on a Mac can build, sign and launch, and its CLI only
targets simulators. So iPhones are **observed, not driven**.
`pymobiledevice3` (`pipx install pymobiledevice3`) does everything from Linux
without root, because `--userspace` builds the iOS 17+ developer tunnel
in-process. Every `ios_poll_minutes` (10) the farm:

- mounts the developer disk image (needed after a reboot);
- records a TestFlight version change as an upgrade (TestFlight auto-update
  installs releases; the App Store-signed IPA can't be sideloaded);
- relaunches Soundscape if it isn't running, and records a PID change with no
  crash report as a restart (killed by iOS or the user);
- pulls new `Soundscape-*` crash and hang reports. bug_type 309 counts as a
  crash; anything else is counted with the ANRs;
- reads the battery.

Throughout, it replays the GPX walk with `simulate-location play`. The app
ignored a single fixed simulated point, so the walk has to move. It also keeps
a daily Soundscape syslog (`--no-debug`, about 66 MB/day) in `runs/<udid>/`.

Preparing an iPhone: Developer Mode on, paired with this PC ("Trust"), Soundscape
from TestFlight with automatic updates on, and the battery charge limit set to
80%. Keep it off USB hubs that lose disconnects. One VIA Labs hub here kept a
ghost iPhone connection that hung usbmuxd until it was removed via
`/sys/bus/usb/devices/<port>/remove` and usbmuxd was restarted.

## Preparing a phone

- Developer options on, USB debugging on, and on Xiaomi/Oppo-type phones also
  "USB debugging (Security settings)" so adb can tap and install.
- Screen lock **None**. Maestro can't get past a PIN.
- An existing Soundscape is fine if it's a sideloaded release. The farm
  only tests a build it installed itself (it records the install time), but
  it first tries installing the latest release over a foreign copy. A
  release-signed copy accepts that, keeping its data like a user's update,
  and the phone is adopted. A Play Store or debug copy is signed differently
  and refuses, which leaves the phone **blocked** and untested unless
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

- Set-up wipes app data (Onboarding uses `clearState`), once per release. The
  data a release upgrades over is whatever the previous release's cycles left.
- The monkey can change other settings (language, voice, callouts).
  Restore only puts back search mode, the map and the markers and route.
- The monkey can open the notification shade. After each soak the farm turns
  off airplane mode, turns off Wi-Fi and collapses the shade.
- Each phone's Maestro gets its own `XDG_CACHE_HOME`/`XDG_STATE_HOME`
  (`runs/<serial>/maestro-home/`). Maestro prunes all but its newest few
  runs' logs, so with a shared `~/.cache/maestro` a long run lost its log
  folder to another phone's run, crashed at exit and hung until the flow
  timeout.
- Maestro runs on several phones at once. One run lost its driver connection
  (`gRPC UNAVAILABLE`) when another phone's session was starting, and that
  error is now retried once. If it keeps happening, set
  `serialize_maestro = true`.
