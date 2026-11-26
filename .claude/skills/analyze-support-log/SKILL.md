---
name: analyze-support-log
description: Analyse a log a user sent through Soundscape's Contact Support (soundscape_log_YYYYMMDD_HHMMSS.txt from Android, soundscape_log.txt from iOS, usually in ~/Downloads) - device and accessibility setup, a sweep of every setting against its default for anything unusual (callouts silenced or narrowed, streets and junctions off, odd voice, SD-card maps, permissions), errors and warnings, and a timeline. Use when asked to analyse, read, triage, check or look at a support log, a user's log, or a log attached to a support email.
---

# Analyse a Contact Support log

Contact Support attaches the app's own log buffer (`SupportLogBuffer`: the last 2000 lines the app
logged at Debug or above, in memory only, so anything before the last process death is gone) and
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
On iOS the report has `VoiceOver` and the location permission (`Always (or provisional Always)`
can still be provisional; see the iOS provisional-Always memory), and the preference rows are all
of NSUserDefaults, whose Apple system keys the script hides.

`?` keys are usually settings that have since been removed (`AccessibleMap` went in d7013572e)
and are harmless; say so only if one could still matter.

## Reading the rest of the log

- **Timestamps are UTC**; the filename is the phone's local time. The difference is the user's
  UTC offset (a 16:26 file whose log ends at 19:26Z is UTC-3), useful when they say "it stopped
  at about four".
- **Release builds don't log what was spoken.** Callout text, GPS fixes and headings are not in
  the log; `MapMatch: ... to get <road>` is the nearest thing, giving the road the user was
  matched to about once a second while moving (`null` = not matched to any road). A
  confected-looking name (e.g. the local word for "road", such as `Carretera`) is an unnamed road
  being announced because `UnnamedRoads` is on. To see the callouts, ask for the travel recording
  (Contact Support offers to attach `travel.gpx`, or Settings > Record travel), then use
  [[replay-gpx]].
- `SoundscapeService: onDestroy` with no `stopService` before it means the service went away
  without the user stopping it: swiped from recents, or killed by the OS.
- The 2000-line cap is roughly 30 minutes of walking, since MapMatch logs every fix. If the parser
  says the log is at the cap, the start of the session is missing.
- Several reports in one log mean the user pressed Contact Support several times; the script
  diffs them (e.g. a screen reader turned off in between).
- A log with no report block came from a build that wrote it differently or the block scrolled
  out; the launch-time `Preference:` dump still gives the settings.

## Don't

- Don't quote the user's location, extract name or anything identifying into commit messages,
  issues or public docs; describe it neutrally ("a user in Argentina").
- Don't change any code as part of the analysis. If a finding points at a bug, say where and
  offer to fix it.
