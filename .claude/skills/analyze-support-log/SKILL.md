---
name: analyze-support-log
description: Analyse a log a user sent through Soundscape's Contact Support (soundscape_log_YYYYMMDD_HHMMSS.txt from Android, soundscape_log.txt from iOS, usually in ~/Downloads) - device and accessibility setup, a sweep of every setting against its default for anything unusual (callouts silenced or narrowed, streets and junctions off, odd voice, SD-card maps, permissions), errors and warnings, and a timeline. Use when asked to analyse, read, triage, check or look at a support log, a user's log, or a log attached to a support email.
---

# Analyse a Contact Support log

Contact Support attaches the app's own log buffer (`SupportLogBuffer`: the newest lines the app
logged at Debug or above that fit in 800,000 characters, about 10,000 lines (the last 2000 lines
before 2.0.118), in memory only, so anything before the last process death is gone) and
writes a **report block** into it: device, accessibility services, audio routes, permissions
(2.0.114+), offline extracts and every stored preference. MainActivity also dumps every stored
preference as `Preference: key = value` lines at each launch. `scripts/parse_support_log.py`
pulls all of this out.

## Steps

1. **Copy the log into its own empty scratchpad directory.** It is untrusted user data; never run
   anything from inside it. If the user named a file, use it; otherwise
   `ls -t ~/Downloads/soundscape_log*.txt | head` and confirm which one if there are several.
2. **Run the parser** from the repo root (it reads the preference defaults from the source, and
   the current version from `app/build.gradle.kts`):
   ```
   python3 -I .claude/skills/analyze-support-log/scripts/parse_support_log.py <copy of log> --repo "$PWD"
   ```
3. **Read the log itself as well**, at least around each launch, each report and each error. The
   script is a sweep, not the analysis: it doesn't know what the user complained about.
4. **Report** in this order, short and specific:
   - **Device**: platform, OS, model, app version (and how far behind current it is), locale,
     screen reader(s), audio output, extract(s).
   - **Settings**: every `HIGH` and `CHECK` flag, with what it means for what the user hears.
     Then the non-default settings worth knowing, in one line. Say plainly whether the settings
     alone could explain the complaint, e.g. "Callout Detail is Quiet and Places to Call Out is
     Landmarks,Transit, so no shop is ever called out - that matches 'it doesn't tell me about
     shops'".
   - **Errors and warnings**: every group the script lists, with count and time, each one
     traced to the code that logs it (`grep -rn` the message) and said whether it matters. If
     there are none, say so in one line. Don't call MainActivity's E-level status lines
     (`themeIsLight`, `themeContrast`, `startSoundscapeService`, the `-----` separator) errors:
     the script already leaves them out.
   - **What happened**: the timeline in words (launches, service killed without a stop = swiped
     away or killed by the system, user stopped it, Contact Support pressed N times, settings
     changed in between).
   - **Not visible in the log**, so the user isn't left thinking it was checked (see below).

## What the settings sweep is looking for

Only keys the user's store holds are logged; an absent key is at its default. The script marks
`*` for a non-default value and `?` for a key current source doesn't define. Defaults come from
`PreferenceKeys`/`PreferenceDefaults` in
`shared/src/commonMain/.../preferences/PreferencesProvider.kt` and the `*_KEY`/`*_DEFAULT` pairs;
when a flag's meaning is in doubt, read the code, starting with
`shared/src/commonMain/.../geoengine/callouts/CalloutVerbosity.kt`.

The settings that silence or thin out callouts, most severe first:

| Key | Value | Effect |
|---|---|---|
| `CalloutVerbosity` | `Silent` | No automatic callouts at all. Beacons, routes, markers, manual callouts still work. The headphone button cycles it, so it can be set by accident. |
| `AllowCallouts` | `false` | Legacy switch; becomes `Silent` on the next engine start unless `CalloutVerbosity` is already set. |
| `PlacesToCallOut` | `Nothing` | No places, markers only. |
| `Mobility` | `false` | Shown as **Streets and Junctions**: no junction callouts walking, no road-sense callout travelling. |
| `CalloutVerbosity` | `Quiet` | Walking POIs are landmarks only (plus any folder chosen in Places to Call Out). Travelling: major or numbered roads only, no minor junctions, no bus/tram stops, at most one road callout a minute. |
| `PlacesToCallOut` | anything but `Everything` | Narrowed: shops and amenities outside the chosen folders dropped; bus/tram stops only with `Transit`. |
| `CalloutVerbosity` | `Balanced` | Detailed is the default; Balanced drops path-only junctions and repeats less often. |
| `DistanceToIntersection` | `false` | Junctions announced without distances. |
| `HeadTrackingEnabled` | `true` | Directions follow the head tracker; a badly placed tracker gives wrong left/right. |

Other things worth a line: `VoiceType` in a different language from the device locale (HIGH: the
user hears another language's voice reading their language), a non-Google `SpeechEngine`, an
extreme `SpeechRate`, `SelectedStorage` on `/storage/XXXX-XXXX/` (a removable SD card, so the
extract vanishes if the card is unmounted), no offline extract, more than one screen reader
enabled (e.g. TalkBack plus a vendor or third-party reader both speaking), `sleep_resume_active`
/ `service_sleeping` left true, and for 2.0.114+ reports: location not precise or Location
services Off, notifications blocked, Background restricted, Battery saver, a low Media volume.
On iOS the report has `VoiceOver`, the location permission (`Always (or provisional Always)`
can still be provisional; see the iOS provisional-Always memory), `Location accuracy` (anything
but Precise is HIGH), `Low Power Mode` and `Background App Refresh`. Its preference rows are all
of NSUserDefaults: the script hides Apple's and Firebase's keys, reads iOS's `1`/`0` booleans as
true/false, and labels `LegacyMigrationDone` as the iOS app's own. iOS logs have no app or
service lifecycle lines (the timeline starts at `location tracking started`), no per-utterance
speech lines, and no `ProcessExit`, `Audio stream` or `TTS` rows; and iOS numbers its releases
separately, so the script doesn't compare its version with Android's.

`?` keys are usually settings that have since been removed (`AccessibleMap` went in d7013572e)
and are harmless; say so only if one could still matter. `UnnamedRoads` is still defined but
nothing reads it any more (it was Street Preview's "Include unnamed roads during location
simulation"), so its value changes nothing.

## Reading the rest of the log

- **Timestamps are UTC**; the filename is the phone's local time. The difference is the user's
  UTC offset (a 16:26 file whose log ends at 19:26Z is UTC-3), useful when they say "it stopped
  at about four".
- **What was spoken is only logged from 2.0.118** (`speakCallout: <text>`); GPS fixes and
  headings never are. `MapMatch: ... to get <road>` gives the road the user was matched to about
  once a second (`null` = not matched to any road). The localized word for "road" on its own
  (`Road`, `Carretera`) is a road with no name in OSM - that's the name given to every unnamed
  road, whatever the settings. To see the callouts in an older log, or where they were heard
  from, ask for the travel recording
  (Contact Support offers to attach `travel.gpx`, or Settings > Record travel), then use
  [[replay-gpx]].
- Up to 2.0.117, `SoundscapeService: onDestroy` with no `stopService` before it means the service
  went away without the user stopping it: swiped from recents, or killed by the OS. From 2.0.118
  `onDestroy after Xm Ys: <reason>` says which.
- Each Contact Support press writes a report block, which the timeline shows as the press. (The
  "Attach log file" line comes after the log is saved, so from 2.0.114 it isn't in the file.)
- The cap is 800,000 characters from 2.0.118, 2000 lines before: roughly two and a half hours
  or 30 minutes of walking, since MapMatch logs every fix. If the parser says the log is at the
  cap, the start of the session is missing.
- Several reports in one log mean the user pressed Contact Support several times; the script
  diffs them (e.g. a screen reader turned off in between).
- A log with no report block came from a build that wrote it differently or the block scrolled
  out; the launch-time `Preference:` dump still gives the settings.

## "It stops talking" / "silent with the screen off"

The commonest complaint, and the log never shows the screen state. Work it out from what is
there:

- **Was anything said?** The script's Speech section counts `TTS: OnStart <id>` (every
  utterance, all versions) and, from 2.0.118, `CalloutController: speakCallout: <text>` (every
  callout, with its earcons - automatic ones and the home-screen buttons alike; a button press is
  the one with `AudioTour: Button pressed: <button>` just before it). Speech outside the callouts
  is `SoundscapeService: speakText <text>` (beacon set, Street Preview; all Android versions) and
  `speak2dText <text>` (audio menu, the headphone button's Callout Detail, deep links; 2.0.118+,
  and both on iOS from 2.0.118). Every callout asks for audio focus first, so a log with
  no `Audio focus request granted.` at all never even tried to speak.
- **Why not? (2.0.118+)** Once a minute GeoEngine logs `Callouts in last 60s: N fixes (+U
  unusable), accuracy median Am, stationary S (undecided D), in vehicle V, audio busy B, menu open
  M, produced P [verbosity; places]`, and
  `User is now stationary` / `User is moving again` / `User is now in a vehicle` as they change.
  Each service run starts with `User is moving|stationary (first verdict, Ns after the first
  fix)`: the stationary detector needs about 30s of fixes before it can say, so nothing before
  that line means "moving". It ignores fixes worse than 25m, while the rest of the app takes them
  up to 50m: an accuracy median above 25m with a high undecided count (typical indoors) means it
  never got enough good fixes to decide, and no first-verdict line appears at all.
  `No location fix for 30s` (a warning, once per gap) and `Location fixes resumed after Ns
  without one` bracket a stretch with no fixes at all, which otherwise leaves nothing in the log -
  summaries are only written as fixes arrive, so a gap also stretches the next one's period
  (`Callouts in last 279s`). Not checked during Street Preview.
  Fixes all stationary: the user was standing still, or the detector thought so. Fixes moving
  but produced 0: the settings had nothing to say there. `Setting changed: key = value` catches a
  change mid-walk, e.g. the headphone button stepping Callout Detail down to Silent.
- **2.0.118+ says outright why the service went:** `onTaskRemoved` (task removed),
  `stopForegroundService: <reason>` (exit, sleep, language change, settings reset, location
  permission removed), and `onDestroy after Xm Ys: <reason>`, where "no stop was requested"
  means the system stopped it. The bullets below are for older logs.
- **2.0.118+ logs how earlier processes ended** at each process start (`ProcessExit`, from
  Android's ApplicationExitInfo): `LOW_MEMORY`, `CRASH`, `ANR`, `OTHER` with a description such
  as "remove task", and the importance (e.g. "running a foreground service"). This is the only
  record of a process death, since the buffer dies with the process. The report repeats them as
  `Previous exit` rows, so they survive a long session scrolling the startup lines out.
- **2.0.118+ reports also carry the audio state** that is otherwise only logged at service start:
  `Audio stream` (the Oboe stream in use, how often it has been restarted, or why there's "no
  audio output"), `TTS engine`, `TTS state`, `TTS language` (what was asked for and whether the
  engine has it - "missing data" or "not supported" means no speech in that language), `TTS
  voice` and `Speech rate`. The parser flags the ones that mean silence.

- **Did the service go away? (2.0.117 and older)** The timeline marks `service destroyed WITHOUT a
  user stop`. Up to 2.0.117 the service had `android:stopWithTask="true"`, so removing the task
  (swiping Soundscape from recents, Samsung's or a third-party cleaner, an accessibility service
  that closes apps) stopped it with exactly this signature: `onServiceDisconnected` then
  `SoundscapeService: onDestroy`, no `stopService`, no `onTaskRemoved`. A user stop or sleep
  always logs `stopService forSleep=...` first. From 2.0.118 the service stops itself in
  `onTaskRemoved`, which logs the removal, so this inference is only needed for older logs.
- **Did the process survive?** The buffer is in memory, so if the log runs unbroken from before
  the destroy to after it, the process lived and only the service was taken down: a task removal,
  not a process kill. A log that starts at a launch with nothing older means the process itself
  died (OOM, OEM battery killer, crash) and everything before it is lost.
- **Was it running but with nothing to say?** Steady `MapMatch` lines (about one a second while
  moving) mean location kept flowing. Then look at the settings: Quiet or Silent callout detail,
  narrowed Places to Call Out, Streets and Junctions off, or a walk along unnamed roads (the
  localized "Road", e.g. `Carretera`) can all give long stretches with nothing to call out, which
  users report as "not talking".
- **Did the release they blame change anything?** `git log soundscape-2.0.<prev>..soundscape-2.0.<N>`
  over `app/src/main/java/.../services`, `SoundscapeServiceConnection.kt`, `locationprovider/`,
  `shared/src/androidMain` and `AndroidManifest.xml`. If nothing there changed, say the update
  is unlikely to be the cause rather than looking for a regression.
- **Ask for what's missing.** Reports from 2.0.113 and older have no battery optimisation,
  background restriction, battery saver, permission or media volume rows, so ask the user to
  update first. Then: Samsung Settings > Apps > Soundscape > Battery > Unrestricted and "Never
  sleeping apps"; and next time it goes quiet, press Contact Support straight away, before
  reopening anything, and include the travel recording.

## Don't

- Don't quote the user's location, extract name or anything identifying into commit messages,
  issues or public docs; describe it neutrally ("a user in Argentina").
- Don't change any code as part of the analysis. If a finding points at a bug, say where and
  offer to fix it.
