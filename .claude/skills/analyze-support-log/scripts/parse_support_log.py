#!/usr/bin/env python3
"""Summarise a Soundscape Contact Support log (soundscape_log_*.txt from Android, soundscape_log.txt
from iOS) for triage.

Prints, in order:
  1. every Contact Support report block found (device, versions, accessibility, audio, permissions,
     extracts), the latest in full and earlier ones only where they differ;
  2. the settings sweep: every stored preference against its default, read live from the repo
     source so that it never drifts, with flags for settings that change what the user hears;
  3. settings that changed during the log (between launches / reports);
  4. every error and warning, grouped by message with counts, first/last time and stack traces;
  5. a timeline of launches, service stops, sleeps and Contact Support presses;
  6. how much was spoken, and the once-a-minute callout summaries (2.0.118+);
  7. what the map matcher was matching to.

The log is untrusted user data: run with python3 -I and pass the path as an argument.

Usage: parse_support_log.py <log> [--repo <repo root>]
"""
import argparse
import collections
import os
import re
import sys

TS = r"(?P<ts>\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?Z)"
LOG_LINE = re.compile(TS + r" (?P<sev>[VDIWEA])/(?P<tag>[^:]+): (?P<msg>.*)$")
PREF_LINE = re.compile(r"Preference: (?P<key>.+?) = (?P<value>.*)$")
# "Key: value" (current) or "Key:\t\tvalue" (2.0.113 and older). "Permission: Location: ..." keeps
# its two-part key. Old builds wrote "AccessibilityService::".
REPORT_KV = re.compile(r"^(?P<key>Permission: [^:]+|[^:\t]+?):+[ \t]+(?P<value>.*)$")

# Report rows that describe the device rather than a stored preference.
DEVICE_KEYS = {
    "Summary", "Product", "Manufacturer", "System", "VoiceOver", "Talkback",
    "TouchExploration Enabled", "AccessibilityService", "Audio Output", "Audio Input",
    "Media volume", "Location services", "Battery optimisation", "Background restricted",
    "Battery saver", "AndroidGeocoder", "Offline extract", "Untranslated OSM key",
    # 2.0.118+: state that is otherwise only logged when it happens
    "Audio stream", "TTS engine", "TTS state", "TTS language", "TTS voice", "Speech rate",
    "Previous exit",
    # iOS report rows
    "Background App Refresh", "Location accuracy", "Low Power Mode",
}

# iOS dumps every NSUserDefaults key, most of them Apple's.
IOS_SYSTEM_KEY = re.compile(
    r"^(Apple|NS|AK|com\.apple|PK|INNext|WebKit|MSV|CK|_|METAL|Carrier|TestRecipe|SKPurchase"
    r"|firebase-|MultiWindowMode$|shouldShowRSVPDataDetectors$|AddingEmojiKeybordHandled$"
    r"|ClearSettingsArchivesForMigration$|(Volume(Up|Down)|Ringer(Button|Switch))ShowsUI$)"
    r"|Prototyp")

# Soundscape's own keys that only the iOS app writes, so they aren't in the Kotlin source
IOS_APP_KEYS = {
    "LegacyMigrationDone": "internal: iOS LegacyMigrator has run (LegacyMigrator.swift)",
}


def normalize_booleans(prefs, defaults):
    """iOS stores booleans as 1/0. Where the source default is a boolean, read them as
    true/false, so that the comparison with the default and the flags see the same values as
    on Android."""
    result = dict(prefs)
    for k, v in prefs.items():
        d = defaults.get(k, (None,))[0]
        if d in ("true", "false") and v in ("1", "0"):
            result[k] = "true" if v == "1" else "false"
    return result

# Lines the app logs at E that are not errors (MainActivity uses E as "always show").
KNOWN_E_NOISE = re.compile(r"^(themeIsLight|themeContrast|startSoundscapeService|-{5})")
# Trouble words in any severity: the app sometimes logs failures at D/I.
ISSUE_TEXT = re.compile(r"Exception|FATAL|\bANR\b|\bfail(ed|ure)?\b|\berror\b|crash|"
                        r"timed? ?out|not granted|denied|unable to|could not|can't|cannot",
                        re.I)

SCREEN_READER = re.compile(r"talkback|marvin|jieshuo|commentary|ferda|screenreader|voiceassistant",
                           re.I)


# --------------------------------------------------------------------------------------------
# Defaults, from source

def kotlin_files(repo):
    for sub in ("shared/src/commonMain", "shared/src/androidMain", "app/src/main/java"):
        for root, _, files in os.walk(os.path.join(repo, sub)):
            for f in files:
                if f.endswith(".kt"):
                    yield os.path.join(root, f)


def parse_literal(expr, consts):
    expr = expr.strip().rstrip(",")
    m = re.match(r'^"(.*)"$', expr)
    if m:
        return m.group(1)
    if expr in ("true", "false"):
        return expr
    m = re.match(r"^(-?\d+(?:\.\d+)?)[fFL]?$", expr)
    if m:
        return m.group(1)
    # A reference to another constant, possibly fully qualified
    name = expr.split(".")[-1]
    return consts.get(name)


def load_defaults(repo):
    """Returns {preference key string: (default or None, constant name)}."""
    const_re = re.compile(r"^\s*(?:private |internal )?const val (\w+)\s*=\s*(.*)$")
    object_re = re.compile(r"^object (PreferenceKeys|PreferenceDefaults)\b")
    raw_keys, raw_defaults, plain = {}, {}, {}
    for path in kotlin_files(repo):
        try:
            lines = open(path, encoding="utf-8").read().splitlines()
        except OSError:
            continue
        current_object = None
        for i, line in enumerate(lines):
            om = object_re.match(line)
            if om:
                current_object = om.group(1)
            elif line.startswith("}"):
                current_object = None
            cm = const_re.match(line)
            if not cm:
                continue
            name, expr = cm.group(1), cm.group(2).strip()
            if not expr and i + 1 < len(lines):  # value on the next line
                expr = lines[i + 1].strip()
            if current_object == "PreferenceKeys":
                raw_keys[name] = expr
            elif current_object == "PreferenceDefaults":
                raw_defaults[name] = expr
            else:
                plain.setdefault(name, expr)
    # Resolve plain constants first (they can refer to each other), then the objects.
    consts = {}
    for _ in range(3):
        for name, expr in plain.items():
            v = parse_literal(expr, consts)
            if v is not None:
                consts[name] = v
    result = {}
    for name, expr in plain.items():
        if name.endswith("_KEY") and name in consts:
            base = name[:-4]
            result.setdefault(consts[name], (consts.get(base + "_DEFAULT"), name))
    for name, expr in raw_keys.items():
        key = parse_literal(expr, consts)
        if key is None:
            continue
        default = parse_literal(raw_defaults[name], consts) if name in raw_defaults else None
        old = result.get(key)
        result[key] = (default if default is not None else (old[0] if old else None),
                       "PreferenceKeys." + name)
    return result


def current_version(repo):
    try:
        m = re.search(r'versionName\s*=\s*"([^"]+)"',
                      open(os.path.join(repo, "app/build.gradle.kts")).read())
        return m.group(1) if m else None
    except OSError:
        return None


# --------------------------------------------------------------------------------------------
# Log parsing

def parse(path):
    text = open(path, encoding="utf-8", errors="replace").read()
    lines = text.splitlines()
    reports, dumps, events, mapmatch = [], [], [], collections.Counter()
    spoken = collections.Counter()
    callout_summaries = []
    issues = collections.OrderedDict()  # normalised (sev, tag, msg) -> [count, first, last, example, stack]
    last_issue = None
    last_ts = None
    current_dump = None
    i = 0
    while i < len(lines):
        line = lines[i]
        m = LOG_LINE.match(line)
        body = m.group("msg") if m else line
        if m:
            last_ts = m.group("ts")
        # Report block: starts at a "Summary:" row, with or without a log prefix
        if re.match(r"^Summary:\s", body):
            tag = m.group("tag") if m else None
            report = {"ts": last_ts, "rows": [], "line": i + 1}
            while i < len(lines):
                ln = lines[i]
                lm = LOG_LINE.match(ln)
                if lm and lm.group("tag") != tag:
                    break
                row = lm.group("msg") if lm else ln
                if row.startswith("Untranslated OSM keys"):
                    i += 1
                    while i < len(lines) and not LOG_LINE.match(lines[i]):
                        k = lines[i].strip()
                        if k and not k.startswith("---"):
                            report["rows"].append(("Untranslated OSM key", k))
                        i += 1
                    break
                kv = REPORT_KV.match(row.strip())
                if kv:
                    report["rows"].append((kv.group("key").strip(), kv.group("value").strip()))
                elif row.strip() and not row.startswith("---") and lm:
                    break
                i += 1
            reports.append(report)
            # The report block is the record of the press: from 2.0.114 the "Attach log file"
            # line is written after the log is saved, so it isn't in the file it attaches
            events.append((report["ts"], "Contact Support pressed (report block at line "
                                         f"{report['line']})"))
            continue
        if m:
            msg, tag, sev, ts = m.group("msg"), m.group("tag"), m.group("sev"), m.group("ts")
            last_issue = None
            if (sev in "WEA" and not KNOWN_E_NOISE.match(msg)) or ISSUE_TEXT.search(msg):
                norm = (sev, tag, re.sub(r"0x[0-9a-f]+|@[0-9a-f]{5,}|\d+(\.\d+)?", "#", msg))
                entry = issues.setdefault(norm, [0, ts, ts, msg, []])
                entry[0] += 1
                entry[2] = ts
                last_issue = entry
            pm = PREF_LINE.match(msg)
            if pm:
                if current_dump is None or current_dump["ts"][:19] != ts[:19]:
                    current_dump = {"ts": ts, "prefs": {}}
                    dumps.append(current_dump)
                current_dump["prefs"][pm.group("key")] = pm.group("value")
            else:
                current_dump = None
            mm = re.match(r"MapMatch: .* to get (.*)$", msg)
            if msg.startswith("speakCallout: ") or re.match(r"OnStart \S", msg):
                spoken[tag] += 1
            # speakText (beacon set, Street Preview, ...) and speak2dText (audio menu,
            # headphone-button Callout Detail, deep links) speak outside CalloutController
            if (tag in ("SoundscapeService", "IosSoundscapeService")
                    and msg.startswith(("speakText ", "speak2dText "))):
                spoken["other"] += 1
            if tag == "AudioTour" and msg.startswith("Button pressed:"):
                spoken["button"] += 1
                events.append((ts, msg))
            if mm:
                mapmatch[mm.group(1)] += 1
            elif msg.startswith("startSoundscapeService"):
                events.append((ts, "app launch: service started"))
            elif tag == "GeoEngine" and msg.startswith("startTileGridService"):
                # Android logs it too, just after startSoundscapeService; iOS logs only this
                if not (events and events[-1][1].startswith("app launch")
                        and events[-1][0][:16] == ts[:16]):
                    events.append((ts, "location tracking started"))
            elif msg.startswith("stopService"):
                events.append((ts, "service stopped by user (" + msg + ")"))
            elif tag == "SoundscapeService" and msg.startswith("onDestroy after"):
                # 2.0.118+: the service says why it went, and how long it ran
                events.append((ts, "service " + msg))
            elif tag == "SoundscapeService" and msg.startswith(("onTaskRemoved",
                                                                "stopForegroundService")):
                events.append((ts, "service " + msg))
            elif tag == "ProcessExit":
                events.append((ts, "previous process: " + msg.strip()))
            elif msg.startswith(("User is now", "User is moving", "User is no longer",
                                 "User is stationary", "Setting changed:")):
                events.append((ts, msg))
            elif msg.startswith("Callouts in last"):
                callout_summaries.append((ts, msg))
            elif (tag == "SoundscapeService" and msg.startswith("onDestroy") and events
                  and events[-1][1].startswith("service onDestroy after")):
                pass  # the bare line logged alongside the 2.0.118 one, before it was removed
            elif tag == "SoundscapeService" and msg.startswith("onDestroy"):
                # stopService logs just before onDestroy; nothing else comes between them
                stopped = any(w.startswith("service stopped by user") for _, w in events[-2:])
                events.append((ts, "service destroyed" if stopped else
                               "service destroyed WITHOUT a user stop: task removed (swiped from "
                               "recents, or a task cleaner - stopWithTask) or killed"))
            elif "Attach travel recording" in msg or "Travel recording asked for" in msg:
                events.append((ts, "Contact Support: " + msg[:90]))
            elif re.search(r"sleep|Sleep", msg) and tag in ("SoundscapeService",
                                                            "SoundscapeServiceConnection"):
                events.append((ts, f"{tag}: {msg[:100]}"))
            elif last_issue is not None:
                events.append((ts, f"{sev}/{tag}: {msg[:160]}  (see Errors and warnings)"))
        elif re.match(r"^\s+at |^Caused by|^\s+\.\.\. \d+ more|^[\w.$]+(Exception|Error)\b", line):
            if last_issue is not None and len(last_issue[4]) < 25:
                last_issue[4].append(line.rstrip())
        i += 1
    first = next((LOG_LINE.match(l).group("ts") for l in lines if LOG_LINE.match(l)), None)
    return (reports, dumps, events, mapmatch, issues, first, last_ts, len(lines), spoken,
            callout_summaries)


def report_prefs(report, defaults):
    prefs = {}
    for k, v in report["rows"]:
        if k in DEVICE_KEYS or k.startswith("Permission"):
            continue
        prefs[k] = v
    return prefs


# --------------------------------------------------------------------------------------------
# The sweep

def flags_for(prefs, report_rows, defaults, app_version, latest_version):
    """Returns a list of (level, message). level: HIGH = probably explains a 'no callouts'/'no
    sound' report, CHECK = changes what the user hears or sees, INFO = worth knowing."""
    out = []

    def get(key):
        if key in prefs:
            return prefs[key]
        d = defaults.get(key)
        return d[0] if d else None

    verbosity = get("CalloutVerbosity") or "Detailed"
    if verbosity == "Silent":
        out.append(("HIGH", "Callout Detail is Silent: no automatic callouts at all (beacons, "
                            "routes, markers and manual callouts still work)"))
    elif verbosity == "Quiet":
        out.append(("CHECK", "Callout Detail is Quiet: walking POI callouts are landmarks only; "
                             "travelling names only major/numbered roads, no minor junctions, no "
                             "bus/tram stops, at most one road callout a minute"))
    elif verbosity == "Balanced":
        out.append(("INFO", "Callout Detail is Balanced (default is Detailed): fewer junctions "
                            "and longer repeat suppression"))
    if prefs.get("AllowCallouts") == "false":
        out.append(("HIGH", "Legacy AllowCallouts=false: migrates to Silent on next engine start "
                            "unless a Callout Detail is already set"))

    places = get("PlacesToCallOut") or "Everything"
    chosen = [p.strip() for p in places.split(",") if p.strip()]
    if "Nothing" in chosen:
        out.append(("HIGH", "Places to Call Out is Nothing: no place callouts (markers only)"))
    elif chosen and "Everything" not in chosen:
        out.append(("CHECK", f"Places to Call Out narrowed to {', '.join(chosen)}: other shops "
                             "and amenities are dropped" +
                    ("" if "Transit" in chosen else ", and bus/tram stops")))
    if "PlaceAndLandmarks" in prefs and "PlacesToCallOut" not in prefs:
        out.append(("CHECK", f"Legacy PlaceAndLandmarks={prefs['PlaceAndLandmarks']} not yet "
                             "migrated to PlacesToCallOut"))

    if get("Mobility") == "false":
        out.append(("HIGH", "Streets and Junctions (key Mobility) is off: no junction callouts "
                            "walking, no road-sense callout travelling"))
    if get("DistanceToIntersection") == "false":
        out.append(("INFO", "Junction callouts don't say the distance"))
    if prefs.get("PositionTextDescription") == "true":
        out.append(("INFO", "Position description includes heading and distance (non-default)"))
    if get("DistanceToBeacon") == "false":
        out.append(("CHECK", "Beacon distance is not announced"))

    rate = prefs.get("SpeechRate")
    try:
        if rate is not None and not 0.6 <= float(rate) <= 2.0:
            out.append(("CHECK", f"Speech rate {rate} is extreme (1.0 is normal)"))
    except ValueError:
        pass
    engine = prefs.get("SpeechEngine", "")
    if engine and "com.google.android.tts" not in engine:
        out.append(("CHECK", f"Non-Google TTS engine: {engine}"))
    voice = prefs.get("VoiceType", "")
    summary = next((v for k, v in report_rows if k == "Summary"), "")
    lm = re.search(r", ([a-z]{2,3})-([A-Za-z]*), ", summary)
    if voice and voice != "Default" and lm:
        vlang = re.match(r"([a-z]{2,3})-([a-z]{2,3})?", voice)
        if vlang and vlang.group(1) != lm.group(1):
            out.append(("HIGH", f"Voice {voice} is not in the device language {lm.group(1)}"))
        elif vlang and vlang.group(2) and lm.group(2) and vlang.group(2) != lm.group(2).lower():
            out.append(("INFO", f"Voice {voice} is a different regional variant from the device "
                                f"locale {lm.group(1)}-{lm.group(2)} (usually deliberate)"))
    if get("MixAudio") == "true":
        out.append(("INFO", "Mix audio with other apps is on"))
    if get("MediaControlsMode") not in (None, "Original"):
        out.append(("INFO", f"Media controls mode {get('MediaControlsMode')}"))
    if get("BeaconType") not in (None, "Classic"):
        out.append(("INFO", f"Beacon sound {get('BeaconType')} (default Classic)"))
    if get("HeadTrackingEnabled") == "true":
        out.append(("CHECK", "Head tracking is on: callout directions follow the head tracker, "
                             "not the phone"))
    if get("GeocoderMode") == "Offline":
        out.append(("CHECK", "Geocoder mode Offline: search and addresses use only the extract"))
    if get("AlphaFeatures") == "true" or get("PoiRankStrategy") not in (None, "off"):
        out.append(("CHECK", "Debug/alpha options are on (AlphaFeatures / PoiRankStrategy)"))
    if get("ShowMap") == "false":
        out.append(("INFO", "Map hidden on the home screen"))
    if get("RecordTravel") == "true":
        out.append(("INFO", "Travel recording is on: ask for the travel.gpx if it wasn't sent"))
    if get("MeasurementUnits") not in (None, "Auto"):
        out.append(("INFO", f"Units forced to {get('MeasurementUnits')}"))
    if get("RelativeDirectionMode") not in (None, "LeftRight"):
        out.append(("INFO", f"Directions as {get('RelativeDirectionMode')}"))
    if prefs.get("sleep_resume_active") == "true" or prefs.get("service_sleeping") == "true":
        out.append(("HIGH", "App was left in sleep mode (service_sleeping / sleep_resume_active)"))

    storage = prefs.get("SelectedStorage", "")
    if re.match(r"^/storage/[0-9A-F]{4}-[0-9A-F]{4}/", storage):
        out.append(("CHECK", "Offline maps are on a removable SD card: if the card is removed or "
                             "unmounted the extract disappears"))
    extracts = [v for k, v in report_rows if k == "Offline extract"]
    if report_rows and not extracts:
        out.append(("CHECK", "No offline extract downloaded"))

    # Device rows (only present when there is a report block)
    rows = collections.defaultdict(list)
    for k, v in report_rows:
        rows[k].append(v)
    services = rows.get("AccessibilityService", [])
    readers = [s for s in services if SCREEN_READER.search(s)]
    if len(readers) > 1:
        out.append(("CHECK", f"{len(readers)} screen-reader services enabled at once: "
                             + ", ".join(readers)))
    others = [s for s in services if s not in readers]
    if others:
        out.append(("INFO", "Other spoken-feedback accessibility services: " + ", ".join(others)))
    if rows.get("Talkback") == ["Off"] or rows.get("VoiceOver") == ["Off"]:
        out.append(("INFO", "No screen reader running when the report was made"))
    for k in ("Permission: Location", "Location services", "Permission: Notifications",
              "Permission: Physical activity", "Battery optimisation", "Background restricted",
              "Battery saver", "Media volume"):
        for v in rows.get(k, []):
            bad = (
                (k == "Permission: Location" and not v.startswith(("Precise", "Always")))
                or (k == "Location services" and v == "Off")
                or (k == "Permission: Notifications" and ("Not granted" in v or "disabled" in v))
                or (k == "Background restricted" and v == "true")
                or (k == "Battery saver" and v == "true")
                or (k == "Media volume" and re.match(r"^([0-9]|[12][0-9])%", v))
            )
            if bad:
                level = "HIGH" if k in ("Permission: Location", "Location services",
                                        "Media volume") else "CHECK"
                out.append((level, f"{k}: {v}"))
            elif k == "Battery optimisation" and v == "Optimised":
                out.append(("INFO", "Battery optimisation: Optimised (callouts may stop when the "
                                    "phone is locked on aggressive OEMs)"))

    for v in rows.get("Audio stream", []):
        if "no audio output" in v:
            out.append(("HIGH", f"Audio stream: {v}"))
        elif "restart pending" in v or "restarted" in v:
            out.append(("CHECK", f"Audio stream: {v} (output device changes, e.g. Bluetooth)"))
    for v in rows.get("TTS state", []):
        if v != "initialised":
            out.append(("HIGH", f"TTS {v}"))
    for v in rows.get("TTS language", []):
        if "missing data" in v or "not supported" in v:
            out.append(("HIGH", f"TTS language {v}: speech may be silent or in another language"))
    for v in rows.get("Previous exit", []):
        if re.search(r" (CRASH|CRASH_NATIVE|ANR|LOW_MEMORY|EXCESSIVE_RESOURCE_USAGE|SIGNALED) ", v):
            out.append(("CHECK", f"Previous exit: {v}"))

    for v in rows.get("Location accuracy", []):
        if v != "Precise":
            out.append(("HIGH", f"Location accuracy: {v} - callouts need precise location"))
    for v in rows.get("Low Power Mode", []):
        if v == "On":
            out.append(("CHECK", "Low Power Mode is on"))
    for v in rows.get("Background App Refresh", []):
        if v != "On":
            out.append(("CHECK", f"Background App Refresh: {v}"))

    # iOS numbers its releases separately from Android's versionName, so only compare Android
    is_ios = "(iOS " in summary
    if app_version and latest_version and app_version != latest_version and not is_ios:
        out.append(("INFO", f"App {app_version}, current is {latest_version}"))
    return out


# --------------------------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("log")
    ap.add_argument("--repo", default=os.getcwd())
    args = ap.parse_args()

    defaults = load_defaults(args.repo)
    latest = current_version(args.repo)
    (reports, dumps, events, mapmatch, issues, first, last, nlines, spoken,
     callout_summaries) = parse(args.log)

    print(f"# {os.path.basename(args.log)}: {nlines} lines, {first} .. {last} (UTC)")
    # From 2.0.118 the buffer keeps the newest lines that fit in 800,000 characters; before that,
    # the newest 2000 lines (a stack trace is one of those but several lines of the file, so a
    # log at that cap can run a little over it).
    nchars = len(open(args.log, encoding="utf-8", errors="replace").read())
    if nchars >= 800_000 - 2_000:
        print("  (at the 800,000-character SupportLogBuffer cap: older lines were dropped)")
    elif 1990 <= nlines <= 2400:
        print("  (at or near the 2000-line SupportLogBuffer cap of 2.0.117 and older: older lines "
              "were dropped)")

    # 1. Reports
    print(f"\n## Contact Support reports: {len(reports)}")
    latest_report = reports[-1] if reports else None
    app_version = None
    if latest_report:
        print(f"Latest at {latest_report['ts']} (line {latest_report['line']}):")
        for k, v in latest_report["rows"]:
            if k in DEVICE_KEYS or k.startswith("Permission"):
                print(f"  {k}: {v}")
        s = next((v for k, v in latest_report["rows"] if k == "Summary"), "")
        vm = re.search(r", ([\d.]+)\)", s)
        app_version = vm.group(1) if vm else None
        for r in reports[:-1]:
            a = set(r["rows"]) - set(latest_report["rows"])
            b = set(latest_report["rows"]) - set(r["rows"])
            if a or b:
                print(f"Report at {r['ts']} differs from the latest:")
                for k, v in sorted(a):
                    print(f"  - {k}: {v}")
                for k, v in sorted(b):
                    print(f"  + {k}: {v}")
            else:
                print(f"Report at {r['ts']}: identical")
    else:
        print("None in the log (it scrolled out of the buffer, or the log came from an older "
              "build). Settings below come from the launch-time Preference dump.")

    # 2. Settings sweep
    if latest_report:
        prefs, source = report_prefs(latest_report, defaults), f"report at {latest_report['ts']}"
        if dumps and dumps[-1]["ts"] > latest_report["ts"]:
            prefs, source = dumps[-1]["prefs"], f"Preference dump at {dumps[-1]['ts']}"
    elif dumps:
        prefs, source = dumps[-1]["prefs"], f"Preference dump at {dumps[-1]['ts']}"
    else:
        prefs, source = {}, "nothing"
    prefs = normalize_booleans(prefs, defaults)
    print(f"\n## Settings (from {source})")
    print("Only keys the user's store holds are logged; anything absent is at its default.")
    ios_system = 0
    for k in sorted(prefs, key=str.lower):
        v = prefs[k]
        if k in defaults:
            d = defaults[k][0]
            state = ("default" if d is not None and str(d) == v else
                     "no default in source: internal state" if d is None else f"default {d!r}")
            print(f"  {'  ' if d is not None and str(d) == v else '* '}{k} = {v}   [{state}]")
        elif k in IOS_APP_KEYS:
            print(f"    {k} = {v}   [{IOS_APP_KEYS[k]}]")
        elif IOS_SYSTEM_KEY.search(k):
            ios_system += 1
        else:
            print(f"  ? {k} = {v}   [not a key in current source: removed setting or "
                  f"platform/internal key]")
    if ios_system:
        print(f"  ({ios_system} iOS system NSUserDefaults keys hidden)")

    report_rows = latest_report["rows"] if latest_report else []
    flags = flags_for(prefs, report_rows, defaults, app_version, latest)
    print("\n## Flags")
    for level in ("HIGH", "CHECK", "INFO"):
        for lv, msg in flags:
            if lv == level:
                print(f"  [{lv}] {msg}")
    if not flags:
        print("  nothing unusual")

    # 3. Changes during the log
    snapshots = [(d["ts"], "launch dump", d["prefs"]) for d in dumps]
    snapshots += [(r["ts"], "report", report_prefs(r, defaults)) for r in reports]
    snapshots.sort(key=lambda s: s[0] or "")
    print("\n## Settings changes during the log")
    changed = False
    for (t0, _, p0), (t1, kind, p1) in zip(snapshots, snapshots[1:]):
        for k in sorted(set(p0) | set(p1)):
            if p0.get(k) != p1.get(k):
                changed = True
                print(f"  by {t1} ({kind}): {k}: {p0.get(k)} -> {p1.get(k)}")
    if not changed:
        print(f"  none across {len(snapshots)} snapshots")

    # 4. Errors and warnings
    print("\n## Errors and warnings")
    if not issues:
        print("  none (W/E/A lines, excluding MainActivity's E-level status lines, plus any "
              "line mentioning an exception, failure, error, timeout or denial)")
    for (sev, tag, _), (count, t0, t1, example, stack) in sorted(
            issues.items(), key=lambda kv: ("AEWDIV".index(kv[0][0]) if kv[0][0] in "AEWDIV" else 9,
                                            kv[1][1])):
        when = t0 if count == 1 else f"{count}x {t0} .. {t1}"
        print(f"  {sev}/{tag}: {example[:200]}\n      [{when}]")
        for st in stack:
            print(f"      {st[:160]}")

    # 5. Timeline
    print("\n## Timeline")
    for ts, what in events:
        print(f"  {ts}  {what}")

    # 6. Speech
    print("\n## Speech")
    button_presses = spoken.get("button", 0)
    ios_log = any("(iOS " in v for r in reports for k, v in r["rows"] if k == "Summary")
    if ios_log:
        print("  iOS logs no line per utterance: only what was asked to be spoken below counts")
    print(f"  {spoken.get('TTS', 0)} utterances started (TTS OnStart), "
          f"{spoken.get('CalloutController', 0)} callouts, automatic or from a button "
          f"(speakCallout, 2.0.118+), {spoken.get('other', 0)} other speech (speakText, "
          f"and speak2dText from 2.0.118), {button_presses} button presses")
    if not any(spoken.get(k) for k in ("TTS", "CalloutController", "other")):
        print("  NOTHING was spoken in the whole log")
    for ts, msg in callout_summaries:
        print(f"  {ts}  {msg}")

    # 7. Map matching
    if mapmatch:
        total = sum(mapmatch.values())
        print(f"\n## Map matched to ({total} fixes)")
        for road, n in mapmatch.most_common(10):
            print(f"  {n:5d}  {road}")


if __name__ == "__main__":
    main()
