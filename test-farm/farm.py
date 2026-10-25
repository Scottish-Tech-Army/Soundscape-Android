#!/usr/bin/env python3
"""Soundscape Android test farm.

Runs long-term tests of release builds on every Android phone plugged into this
PC. One worker thread per phone loops forever:

    upgrade (if a newer release exists) -> PostUpgrade check
    monkey soak -> the Maestro suite (maestro/Master.yaml order)
    collect crashes/ANRs, memory, battery, logcat

New releases are picked up from the upstream "Tag and build release" workflow's
release-apk artifact (the same APK that goes to Play) and installed with
`adb install -r` over the phone's existing data, so the real upgrade path is
exercised. Location is driven over adb with a shell mock provider replaying a
GPX track, because release builds don't have the debug REPLAY_GPX intent.

Results go into farm.db and report.html next to this script. See README.md.
"""

import argparse
import bisect
import contextlib
import datetime as dt
import glob
import gzip
import json
import html
import math
import os
import random
import re
import signal
import sqlite3
import subprocess
import sys
import threading
import time
import tomllib
import xml.etree.ElementTree as ET

import usb_proxy

FARM_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_DIR = os.path.dirname(FARM_DIR)
MAESTRO_DIR = os.path.join(REPO_DIR, "maestro")
POST_UPGRADE_FLOW = os.path.join(FARM_DIR, "flows", "PostUpgrade.yaml")
APK_DIR = os.path.join(FARM_DIR, "apks")
EXTRACT_DIR = os.path.join(FARM_DIR, "extracts")
FARM_SETTINGS_FLOW = os.path.join(FARM_DIR, "flows", "FarmSettings.yaml")
DOWNLOAD_EXTRACT_FLOW = os.path.join(FARM_DIR, "flows", "DownloadExtract.yaml")
SHOW_MAP_FLOW = os.path.join(FARM_DIR, "flows", "ShowMap.yaml")
RESTORE_FLOW = os.path.join(FARM_DIR, "flows", "Restore.yaml")
ROAM_DIR = os.path.join(FARM_DIR, "flows", "roam")
RUNS_DIR = os.path.join(FARM_DIR, "runs")
DB_PATH = os.path.join(FARM_DIR, "farm.db")
REPORT_PATH = os.path.join(FARM_DIR, "report.html")

PACKAGE = "org.scottishtecharmy.soundscape"
MOCK_PROVIDERS = ("gps", "network")
# GeoEngine reads extracts from <selected storage>/Download, which defaults to
# the app's external files dir (as CI's push in run-maestro-tests.yaml).
DEVICE_EXTRACT_DIR = f"/storage/emulated/0/Android/data/{PACKAGE}/files/Download"

DEFAULTS = {
    "release_repo": "Scottish-Tech-Army/Soundscape-Android",
    "release_workflow": "build-app.yaml",
    "release_artifact": "release-apk",
    "poll_minutes": 15,
    "monkey_events": 5000,
    # Rounds of the roaming flows (flows/roam/, shuffled each round) per cycle.
    "roam_rounds": 2,
    "monkey_throttle_ms": 250,
    "flow_timeout_minutes": 15,
    "keep_days": 14,
    "serialize_maestro": False,
    "allow_reinstall": False,
    "pause_on_restore_failure": False,
    "pause_minutes": 30,
    # false cuts apps off from the motion sensors (see Worker.set_sensors), so
    # Soundscape has no compass or attitude and never thinks the phone is lying
    # flat on the desk; it then takes its heading from travel, as in a pocket.
    # Overridable per phone in its [devices] entry.
    "sensors": True,
    "include_emulators": False,
    # iPhones (via pymobiledevice3) are observed, not driven: see IosWorker.
    "include_ios": True,
    "ios_poll_minutes": 10,
    "max_battery_temp_c": 42,
    # A phone whose USB port can't keep up with the farm's load (the A50) drains slowly. At
    # battery_low_percent the app is stopped and the screen turned off until the battery is back
    # up to battery_resume_percent. Samsung's battery protection stops charging at 80-85%.
    "battery_low_percent": 10,
    "battery_resume_percent": 80,
    # How often each Android phone's battery level and temperature are read
    # for the report and the vitals table, between the per-cycle readings.
    "battery_poll_minutes": 1,
    "replay_speed_mps": 1.4,
    "default_location": [55.9474, -4.3058],
    # Held while the Maestro suite runs: the flows assert distance-sorted list
    # order, so they need the fixed point CI pins with `adb emu geo fix`.
    "suite_location": [55.955360, -3.223538],
    # The offline map pushed to every phone, by its manifest "name". It has to
    # cover suite_location and the GPX track. The manifest URL is read from
    # local.properties' extractProviderUrl unless set here.
    "extract_name": "Glasgow",
    "extract_provider_url": "",
    # A day out on foot, by bus and by train around central Glasgow, made by
    # app/src/test/.../FarmTrackGenerator.kt and replayed by its timestamps.
    "default_gpx": os.path.join(FARM_DIR, "tracks", "glasgow-multimodal.gpx"),
    "devices": {},
}

# Same transient launch failures CI retries once (see run-maestro-tests.yaml).
MAESTRO_RETRY_RE = re.compile(
    r"Unable to launch app|device offline|Unknown error|Not able to reach the gRPC server")

stop_event = threading.Event()
maestro_lock = threading.Lock()
state_lock = threading.Lock()
device_state = {}  # serial -> {"state": str, "since": float, "model": str, ...}


def log(serial, msg):
    stamp = dt.datetime.now().strftime("%H:%M:%S")
    print(f"{stamp} [{serial or 'farm'}] {msg}", flush=True)


def set_state(serial, state, **extra):
    with state_lock:
        entry = device_state.setdefault(serial, {})
        entry.update(extra)
        entry["state"] = state
        entry["since"] = time.time()
    log(serial, state)


# --------------------------------------------------------------------------- config


def load_config(path):
    cfg = dict(DEFAULTS)
    if path and os.path.exists(path):
        with open(path, "rb") as f:
            cfg.update(tomllib.load(f))
        base = os.path.dirname(os.path.abspath(path))
    else:
        base = FARM_DIR
    # GPX paths in the config are relative to the config file.
    def resolve(p):
        return os.path.normpath(os.path.join(base, p)) if p else ""
    cfg["default_gpx"] = resolve(cfg["default_gpx"])
    for dev in cfg["devices"].values():
        if "gpx" in dev:
            dev["gpx"] = resolve(dev["gpx"])
    return cfg


# --------------------------------------------------------------------------- database


class Db:
    def __init__(self, path):
        self.conn = sqlite3.connect(path, check_same_thread=False)
        self.conn.row_factory = sqlite3.Row
        self.lock = threading.Lock()
        self.conn.executescript("""
            CREATE TABLE IF NOT EXISTS builds(
                run_id INTEGER PRIMARY KEY, version_code INTEGER, version_name TEXT,
                created_at TEXT, apk_path TEXT);
            CREATE TABLE IF NOT EXISTS cycles(
                id INTEGER PRIMARY KEY, serial TEXT, model TEXT, android TEXT,
                version_code INTEGER, started REAL, ended REAL, status TEXT,
                flows_passed INTEGER, flows_failed INTEGER, crashes INTEGER, anrs INTEGER,
                pss_kb INTEGER, battery_level INTEGER, battery_temp REAL, dir TEXT);
            CREATE TABLE IF NOT EXISTS results(
                id INTEGER PRIMARY KEY, cycle_id INTEGER, serial TEXT, ts REAL,
                kind TEXT, name TEXT, status TEXT, duration_s REAL, detail TEXT, dir TEXT);
            CREATE TABLE IF NOT EXISTS installs(
                serial TEXT PRIMARY KEY, last_update TEXT);
            CREATE TABLE IF NOT EXISTS setups(
                serial TEXT PRIMARY KEY, version_code INTEGER);
            CREATE TABLE IF NOT EXISTS events(
                id INTEGER PRIMARY KEY, serial TEXT, ts REAL, kind TEXT,
                version_code INTEGER, detail TEXT);
            CREATE TABLE IF NOT EXISTS vitals(
                serial TEXT, ts REAL, battery_level INTEGER, battery_temp REAL);
            CREATE INDEX IF NOT EXISTS vitals_serial_ts ON vitals(serial, ts);
        """)
        try:  # added after the table: whether the set-up suite all passed
            self.conn.execute("ALTER TABLE setups ADD COLUMN complete INTEGER DEFAULT 1")
        except sqlite3.OperationalError:
            pass
        try:  # added after the table: how long the app process had been running
            self.conn.execute("ALTER TABLE cycles ADD COLUMN app_uptime_s INTEGER")
        except sqlite3.OperationalError:
            pass
        # Added after the table: the app's memory each minute, and the phone's RAM. RssAnon and
        # VmSwap (from /proc/<pid>/status) together are what the Play Store's memory rules
        # measure against the device's RAM.
        for column in ("app_uptime_s INTEGER", "pss_kb INTEGER", "rss_anon_kb INTEGER",
                       "vm_swap_kb INTEGER", "mem_total_kb INTEGER", "proc_state INTEGER",
                       *(f"pss_{key}_kb INTEGER" for key, _, _ in PSS_PARTS)):
            try:
                self.conn.execute(f"ALTER TABLE vitals ADD COLUMN {column}")
            except sqlite3.OperationalError:
                pass

    def execute(self, sql, args=()):
        with self.lock:
            cur = self.conn.execute(sql, args)
            self.conn.commit()
            return cur.lastrowid

    def query(self, sql, args=()):
        with self.lock:
            return self.conn.execute(sql, args).fetchall()

    def event(self, serial, kind, version_code, detail):
        log(serial, f"EVENT {kind}: {detail.splitlines()[0][:200] if detail else ''}")
        self.execute("INSERT INTO events(serial, ts, kind, version_code, detail) VALUES(?,?,?,?,?)",
                     (serial, time.time(), kind, version_code, detail))

    def builds(self):
        return self.query("SELECT * FROM builds ORDER BY version_code")


# --------------------------------------------------------------------------- adb


children = set()  # running subprocesses, stopped when the farm stops
children_lock = threading.Lock()


def run(cmd, timeout=120, env=None):
    """subprocess.run, except that the farm can stop the process when it stops.
    A Maestro run left behind by a stopped farm keeps hold of the phone's
    driver, and every flow after a restart then fails with gRPC UNAVAILABLE."""
    with children_lock:
        # Once the farm is stopping, start nothing new: stop_children() may
        # already have run, and anything started now would be orphaned.
        if stop_event.is_set():
            return subprocess.CompletedProcess(cmd, -1, "", "farm stopping")
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                text=True, errors="replace",
                                env={**os.environ, **env} if env else None)
        children.add(proc)
    try:
        out, err = proc.communicate(timeout=timeout)
        return subprocess.CompletedProcess(cmd, proc.returncode, out, err)
    except subprocess.TimeoutExpired:
        proc.kill()
        out, _ = proc.communicate()
        return subprocess.CompletedProcess(cmd, -1, out or "", f"timeout after {timeout}s")
    finally:
        with children_lock:
            children.discard(proc)


def stop_children():
    with children_lock:
        procs = list(children)
    for proc in procs:
        proc.terminate()
    for proc in procs:
        try:
            proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            proc.kill()


def adb(serial, *args, timeout=120):
    return run(["adb", "-s", serial, *args], timeout=timeout)


def shell(serial, command, timeout=120):
    return adb(serial, "shell", command, timeout=timeout).stdout.strip()


def connected_devices(include_emulators):
    out = run(["adb", "devices"]).stdout
    serials = []
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) == 2 and parts[1] == "device":
            if include_emulators or not parts[0].startswith("emulator-"):
                serials.append(parts[0])
    return serials


def installed_version(serial):
    out = shell(serial, f"dumpsys package {PACKAGE} | grep -m1 versionCode")
    m = re.search(r"versionCode=(\d+)", out)
    return int(m.group(1)) if m else None


def find_aapt2():
    sdk = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/Android/Sdk")
    tools = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "aapt2")))
    if not tools:
        sys.exit("aapt2 not found: set ANDROID_HOME to your Android SDK")
    return tools[-1]


# --------------------------------------------------------------------------- releases


def release_watcher(cfg, db):
    """Caches the APKs of the two newest successful release builds."""
    aapt2 = find_aapt2()
    known = {b["run_id"] for b in db.builds()}
    while not stop_event.is_set():
        res = run(["gh", "run", "list", "-R", cfg["release_repo"], "-w", cfg["release_workflow"],
                   "-s", "success", "-L", "2", "--json", "databaseId,createdAt",
                   "-q", '.[] | "\\(.databaseId) \\(.createdAt)"'])
        if res.returncode != 0:
            log(None, f"gh run list failed: {res.stderr.strip()}")
        for line in res.stdout.split("\n"):
            if not line.strip():
                continue
            run_id, created = line.split()
            if int(run_id) in known:
                continue
            dest = os.path.join(APK_DIR, f"run-{run_id}")
            log(None, f"downloading release build from run {run_id}")
            dl = run(["gh", "run", "download", run_id, "-R", cfg["release_repo"],
                      "-n", cfg["release_artifact"], "-D", dest], timeout=900)
            apks = glob.glob(os.path.join(dest, "**", "*.apk"), recursive=True)
            if dl.returncode != 0 or not apks:
                log(None, f"download of run {run_id} failed: {dl.stderr.strip()}")
                continue
            badging = run([aapt2, "dump", "badging", apks[0]]).stdout
            m = re.search(r"versionCode='(\d+)' versionName='([^']*)'", badging)
            if not m:
                log(None, f"could not read version from {apks[0]}")
                continue
            db.execute("INSERT INTO builds VALUES(?,?,?,?,?)",
                       (int(run_id), int(m.group(1)), m.group(2), created, apks[0]))
            known.add(int(run_id))
            log(None, f"new release {m.group(2)} ({m.group(1)}) cached")
        sync_extract(cfg)
        stop_event.wait(cfg["poll_minutes"] * 60)


# --------------------------------------------------------------------------- offline map

current_extract = None  # local path of the cached .pmtiles; its sidecar is path + ".geojson"


def extract_provider_url(cfg):
    if cfg["extract_provider_url"]:
        return cfg["extract_provider_url"]
    try:
        with open(os.path.join(REPO_DIR, "local.properties")) as f:
            m = re.search(r"^extractProviderUrl=(.+)$", f.read(), re.M)
        return m.group(1).strip() if m else ""
    except OSError:
        return ""


def sync_extract(cfg):
    """Keeps the manifest's current extract for cfg["extract_name"] cached locally.

    Downloads only when the manifest names a new file, so the monthly rebuild
    is picked up and nothing else is fetched.
    """
    global current_extract
    base = extract_provider_url(cfg)
    if not base:
        log(None, "no extractProviderUrl in local.properties; phones will use the network")
        return
    res = subprocess.run(["curl", "-sfL", f"{base.rstrip('/')}/manifest.geojson.gz"],
                         capture_output=True, timeout=120)
    try:
        manifest = json.loads(gzip.decompress(res.stdout))
        feature = next(f for f in manifest["features"]
                       if f["properties"].get("name") == cfg["extract_name"])
    except Exception as e:  # manifest unreachable or no such extract; keep what we have
        log(None, f"extract manifest unusable ({e!r}); keeping {current_extract}")
        if not current_extract:
            cached = sorted(glob.glob(os.path.join(EXTRACT_DIR, "*.pmtiles")))
            current_extract = cached[-1] if cached else None
        return
    filename = feature["properties"]["filename"]
    path = os.path.join(EXTRACT_DIR, filename)
    if not os.path.exists(path):
        log(None, f"downloading offline map {filename}")
        part = path + ".part"
        dl = run(["curl", "-sSfL", "-o", part, f"{base.rstrip('/')}/{filename}"], timeout=3600)
        if dl.returncode != 0:
            log(None, f"offline map download failed (exit {dl.returncode}): {dl.stderr.strip()}; "
                      "retrying at the next release poll")
            return
        os.replace(part, path)
        with open(path + ".geojson", "w") as f:
            json.dump(feature, f)
        for old in glob.glob(os.path.join(EXTRACT_DIR, "*")):
            if not old.startswith(path):
                os.remove(old)
    current_extract = path


# --------------------------------------------------------------------------- location


TRACK_MIN_STEP_M = 15
# FarmTrackGenerator marks its tracks with this keyword, to be replayed by their
# timestamps. Real recordings have timestamps too, but they start with minutes
# of a phone standing still, so those are replayed at a constant speed.
TIMED_TRACK_KEYWORD = "farm-timed-replay"


def load_timed_track(path):
    """(times, points) of a track marked for timed replay, with times in
    seconds from its first point, or None for any other track."""
    root = ET.parse(path).getroot()
    if not any(e.tag.split("}")[-1] == "keywords" and TIMED_TRACK_KEYWORD in (e.text or "")
               for e in root.iter()):
        return None
    times, points = [], []
    for p in root.iter():
        if p.tag.split("}")[-1] != "trkpt":
            continue
        stamp = next(c.text for c in p if c.tag.split("}")[-1] == "time")
        times.append(dt.datetime.fromisoformat(stamp.replace("Z", "+00:00")).timestamp())
        points.append((float(p.get("lat")), float(p.get("lon"))))
    if len(points) < 2:
        raise ValueError(f"{path}: need at least 2 track points")
    return [t - times[0] for t in times], points


def load_track(path):
    """The track's points, keeping only those at least TRACK_MIN_STEP_M from the
    last one kept. A phone recording that starts with its owner standing still
    is minutes of GPS jitter (local-pixel.gpx: ~700 m of "path" within 15 m of
    the start), which a constant-speed replay would otherwise walk on the spot."""
    root = ET.parse(path).getroot()
    raw = [(float(p.get("lat")), float(p.get("lon")))
           for p in root.iter() if p.tag.split("}")[-1] in ("trkpt", "rtept")]
    points = raw[:1]
    for p in raw[1:]:
        if haversine(points[-1], p) >= TRACK_MIN_STEP_M:
            points.append(p)
    if len(points) < 2:
        raise ValueError(f"{path}: need at least 2 track points {TRACK_MIN_STEP_M} m apart")
    return points


def haversine(a, b):
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = (math.sin((lat2 - lat1) / 2) ** 2
         + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2)
    return 2 * 6371000 * math.asin(math.sqrt(h))


class Track:
    """Walks a GPX track at a constant speed, turning round at each end rather
    than jumping back to the start. With no track, holds one fixed point.

    A track marked for timed replay (see load_timed_track) is played by its
    timestamps instead, stops and all, and then played backwards."""

    def __init__(self, gpx, fixed, speed):
        self.speed = speed
        timed = load_timed_track(gpx) if gpx else None
        self.times = timed[0] if timed else None
        if timed:
            self.points = timed[1]
            return
        self.points = load_track(gpx) if gpx else [tuple(fixed), tuple(fixed)]
        self.cumulative = [0.0]
        for a, b in zip(self.points, self.points[1:]):
            self.cumulative.append(self.cumulative[-1] + haversine(a, b))

    def round_trip_seconds(self):
        if self.times:
            return max(60, int(2 * self.times[-1]))
        return max(60, int(2 * self.cumulative[-1] / self.speed))

    def speed_between(self, here, ahead):
        """Speed while moving from `here` to `ahead`, a second later."""
        return haversine(here, ahead) if self.times else self.speed

    def position(self, t):
        if self.times:
            return self.timed_position(t)
        total = self.cumulative[-1]
        if total == 0:
            return self.points[0]
        d = (self.speed * t) % (2 * total)
        if d > total:
            d = 2 * total - d
        i = max(j for j, c in enumerate(self.cumulative) if c <= d)
        if i == len(self.points) - 1:
            return self.points[-1]
        seg = self.cumulative[i + 1] - self.cumulative[i]
        f = (d - self.cumulative[i]) / seg if seg else 0
        (la, lo), (lb, lob) = self.points[i], self.points[i + 1]
        return la + (lb - la) * f, lo + (lob - lo) * f

    def timed_position(self, t):
        total = self.times[-1]
        t %= 2 * total
        if t > total:
            t = 2 * total - t
        i = min(bisect.bisect_right(self.times, t) - 1, len(self.points) - 2)
        seg = self.times[i + 1] - self.times[i]
        f = (t - self.times[i]) / seg if seg else 0
        (la, lo), (lb, lob) = self.points[i], self.points[i + 1]
        if (la, lo) == (lb, lob):
            return la, lo
        return la + (lb - la) * f, lo + (lob - lo) * f


class LocationFeeder(threading.Thread):
    """Feeds the Track's position to an Android phone once a second over adb.
    Setting `hold` to a (lat, lon) pins the phone there until it's set to None.
    """

    def __init__(self, serial, gpx, fixed, speed, db, alive):
        super().__init__(daemon=True)
        self.serial, self.db, self.alive = serial, db, alive
        self.hold = None
        self.track = Track(gpx, fixed, speed)

    def fix(self, t):
        """(lat, lon, speed, bearing) at t seconds. Speed and bearing matter:
        Soundscape only takes its direction from travel when a fix says the
        user is moving, and otherwise uses the compass of a phone on a desk."""
        if self.hold:
            return (*self.hold, 0.0, None)
        here, ahead = self.track.position(t), self.track.position(t + 1)
        if here == ahead:
            return (*here, 0.0, None)
        return (*here, self.track.speed_between(here, ahead), initial_bearing(here, ahead))

    def run(self):
        start = time.time()
        failures = 0
        while not stop_event.is_set() and self.alive.is_set():
            lat, lon, speed, bearing = self.fix(time.time() - start)
            extras = f"--es lat {lat:.7f} --es lon {lon:.7f} --es speed {speed:.2f}"
            if bearing is not None:
                extras += f" --es bearing {bearing:.1f}"
            out = shell(self.serial, f"am broadcast -f 0x10000000 -n {HELPER_PKG}/.FixReceiver "
                                     f"-a {HELPER_PKG}.FIX {extras}", timeout=10)
            if 'data="ok"' in out:
                failures = 0
            else:
                # Keep going: if the feed stops, the phone falls back to its real
                # position mid-suite, which breaks flows in confusing ways.
                failures += 1
                if failures in (1, 60) or failures % 600 == 0:
                    self.db.event(self.serial, "error", None,
                                  f"mock location rejected ({failures} in a row):\n{out}")
                allow_helper(self.serial)
            time.sleep(1)


def initial_bearing(a, b):
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    y = math.sin(lon2 - lon1) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(lon2 - lon1)
    return (math.degrees(math.atan2(y, x)) + 360) % 360


# Broadcasts to the helper go on the foreground queue (-f 0x10000000,
# FLAG_RECEIVER_FOREGROUND): on the Android 11 A50 the background queue backed
# up behind other apps' broadcasts and a fix took over 15 s to land.
#
# The mock location helper in mock-location/. `cmd location providers` can only
# set a position, so fixes from it have no speed or bearing; the helper sets
# both. It isn't Soundscape, so the farm still tests only release builds.
# Deliberately not under org.scottishtecharmy.soundscape: as
# org.scottishtecharmy.soundscape.farmlocation, Maestro's launchApp stopped
# granting Soundscape's permissions on the AOSP Samsung (it worked again as soon
# as the helper was uninstalled or renamed), and Onboarding failed every cycle.
HELPER_PKG = "org.scottishtecharmy.farmlocation"
HELPER_DIR = os.path.join(FARM_DIR, "mock-location")
HELPER_APK = os.path.join(HELPER_DIR, "build", "farm-location.apk")


def build_helper():
    if not os.path.exists(HELPER_APK):
        res = run([os.path.join(HELPER_DIR, "build.sh")], timeout=600)
        if res.returncode != 0:
            sys.exit("mock location helper build failed:\n" + res.stdout + res.stderr)


def enable_mock_location(serial):
    # Shell test providers from older farm runs would sit beside the helper's.
    disable_mock_location(serial, shell_providers=True)
    out = adb(serial, "install", "-r", HELPER_APK, timeout=300)
    if "Success" not in out.stdout + out.stderr:
        return "helper install failed: " + out.stdout + out.stderr
    return allow_helper(serial)


def allow_helper(serial):
    """Grants the helper the mock location app-op. Straight after a fresh
    install the grant can be lost (seen on the AOSP Samsung), so it's read back
    and repeated until it sticks."""
    for _ in range(5):
        shell(serial, f"appops set {HELPER_PKG} android:mock_location allow")
        if "allow" in shell(serial, f"appops get {HELPER_PKG} android:mock_location"):
            return ""
        time.sleep(2)
    return "failed to allow mock location for " + HELPER_PKG


def disable_mock_location(serial, shell_providers=False):
    if shell_providers:
        shell(serial, "; ".join(f"cmd location providers remove-test-provider {p}"
                                 for p in MOCK_PROVIDERS), timeout=20)
    else:
        shell(serial, f"am broadcast -f 0x10000000 -n {HELPER_PKG}/.FixReceiver -a {HELPER_PKG}.STOP",
              timeout=20)


# --------------------------------------------------------------------------- crashes


def lock_shade(serial):
    """Stops the notification shade being pulled down. The monkey's swipes
    from the top edge opened it, and on the A50 a shade the farm then
    collapsed was left invisible but holding key focus: every Back went to
    it, so LaunchHome couldn't leave the screen underneath and Restore failed.
    Lasts until reset (the farm does when it stops) or SystemUI restarts."""
    shell(serial, "cmd statusbar collapse; cmd statusbar send-disable-flag statusbar-expansion")


# The allowlist for SensorService's restricted mode when a phone's sensors are
# off: apps whose package name contains it keep their sensors, and none does.
SENSORS_OFF = "test-farm-sensors-off"
SENSOR_MODE = "dumpsys sensorservice 2> /dev/null | grep -m1 '^Mode'"

DROPBOX_TAGS = ("data_app_crash", "data_app_anr", "data_app_native_crash", "SYSTEM_TOMBSTONE")


def collect_dropbox(serial, since):
    """Returns (kind, text) for every crash/ANR of our app logged at or after `since`.

    `since` is a device-local "YYYY-MM-DD HH:MM:SS" string, which is also the
    format dropbox prints entries with, so plain string comparison works.
    """
    # One tag per call: dumpsys dropbox only prints entries that match every
    # argument, so several tags in one call print nothing.
    out = "\n".join(shell(serial, f"dumpsys dropbox --print {tag}", timeout=60)
                    for tag in DROPBOX_TAGS)
    found = []
    for entry in re.split(r"^={10,}\s*$", out, flags=re.M):
        m = re.search(r"^(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d) (\S+)", entry.strip(), re.M)
        if m and m.group(1) >= since and PACKAGE in entry:
            kind = "anr" if "anr" in m.group(2) else "crash"
            found.append((kind, entry.strip()))
    return found


# The rows of dumpsys meminfo's App Summary, which together make up the app's total PSS:
# (vitals column suffix, row label, short name for the report)
PSS_PARTS = (("native", "Native Heap", "Native"), ("graphics", "Graphics", "Graphics"),
             ("java", "Java Heap", "Java"), ("code", "Code", "Code"),
             ("system", "System", "System"), ("other", "Private Other", "Other"),
             ("stack", "Stack", "Stack"))


def app_meminfo(serial):
    """The app's total PSS and its App Summary breakdown in kB, as (total, {part: kB}). The
    breakdown is empty where the summary isn't there, and the total None if the app isn't
    running."""
    out = shell(serial, f"dumpsys meminfo {PACKAGE}", timeout=60)
    m = re.search(r"TOTAL PSS:\s+(\d+)", out) or re.search(r"^\s*TOTAL\s+(\d+)", out, re.M)
    summary = out.split("App Summary", 1)[1] if "App Summary" in out else ""
    parts = {}
    for key, label, _ in PSS_PARTS:
        p = re.search(rf"^\s*{label}:\s+(\d+)", summary, re.M)
        if p:
            parts[key] = int(p.group(1))
    return (int(m.group(1)) if m else None), parts


def pss_kb(serial):
    return app_meminfo(serial)[0]


def app_anon_kb(serial):
    """The app process's RssAnon and VmSwap in kB (from /proc/<pid>/status), or Nones if it isn't
    running."""
    pid = shell(serial, f"pidof {PACKAGE}").split()
    if not pid:
        return None, None
    out = shell(serial, f"grep -E '^(RssAnon|VmSwap):' /proc/{pid[0]}/status")
    anon = re.search(r"RssAnon:\s+(\d+)", out)
    swap = re.search(r"VmSwap:\s+(\d+)", out)
    return (int(anon.group(1)) if anon else None, int(swap.group(1)) if swap else None)


# ActivityManager process states: on screen, and running a foreground service
PROC_STATE_TOP = 2
PROC_STATE_FOREGROUND_SERVICE = 4


def app_proc_state(serial):
    """The app process's ActivityManager state (PROC_STATE_*), or None if it isn't running."""
    m = re.search(r"curProcState=(\d+)", shell(serial, f"dumpsys activity processes {PACKAGE}"))
    return int(m.group(1)) if m else None


# Google Play's memory limits for apps from February 2027: the 90th percentile of RssAnon +
# VmSwap over 28 days must stay under these, by the phone's RAM tier (GB), for an app on
# screen and for one with only user-perceived services (a foreground service) running. See
# https://support.google.com/googleplay/android-developer/answer/17492799
PLAY_MEMORY_LIMITS_GB = {4: (2.0, 1.0), 6: (2.25, 1.25), 8: (2.25, 1.5), 12: (3.25, 1.75),
                         16: (4.25, 2.0)}


def play_memory_limit_kb(mem_total_kb, state):
    """The Play limit for an app in [state] on a phone with [mem_total_kb] of RAM, or None. A
    phone reports less RAM than it's sold with, so it's put in the nearest tier; under 3 GB
    (the 1 GB Cubot) is below the lowest."""
    if not mem_total_kb or state not in (PROC_STATE_TOP, PROC_STATE_FOREGROUND_SERVICE):
        return None
    gb = mem_total_kb / 1048576
    if gb < 3:
        return None
    tier = min(PLAY_MEMORY_LIMITS_GB, key=lambda t: abs(t - gb))
    foreground, services = PLAY_MEMORY_LIMITS_GB[tier]
    return int((foreground if state == PROC_STATE_TOP else services) * 1048576)


def proc_state_name(state):
    return {PROC_STATE_TOP: "on screen",
            PROC_STATE_FOREGROUND_SERVICE: "service only"}.get(state, f"state {state}")


def mem_total_kb(serial):
    m = re.search(r"MemTotal:\s+(\d+)", shell(serial, "grep MemTotal /proc/meminfo"))
    return int(m.group(1)) if m else None


def app_uptime_s(serial):
    """Seconds the app's main process has been running, or None if it isn't."""
    pid = shell(serial, f"pidof {PACKAGE}").split()
    if not pid:
        return None
    # ps prints ELAPSED as [[dd-]hh:]mm:ss.
    out = shell(serial, f"ps -o ETIME= -p {pid[0]}").strip()
    m = re.fullmatch(r"(?:(?:(\d+)-)?(\d+):)?(\d+):(\d+)", out)
    if not m:
        return None
    d, h, mins, secs = (int(g or 0) for g in m.groups())
    return ((d * 24 + h) * 60 + mins) * 60 + secs


def format_uptime(seconds):
    if seconds is None:
        return "app not running"
    h, rem = divmod(seconds, 3600)
    return f"app up {h}h{rem // 60:02d}m" if h else f"app up {rem // 60}m"


def battery(serial):
    out = shell(serial, "dumpsys battery")
    level = re.search(r"level: (\d+)", out)
    temp = re.search(r"temperature: (\d+)", out)
    return (int(level.group(1)) if level else None,
            int(temp.group(1)) / 10 if temp else None)


# --------------------------------------------------------------------------- worker


def suite_flows():
    """The suite in Master.yaml order, which is the order the flows depend on."""
    with open(os.path.join(MAESTRO_DIR, "Master.yaml")) as f:
        return re.findall(r"^- runFlow: (\S+\.yaml)", f.read(), re.M)


def roam_flows():
    return sorted(f for f in os.listdir(ROAM_DIR) if f.endswith(".yaml"))


class Worker(threading.Thread):
    def __init__(self, serial, cfg, db):
        super().__init__(daemon=True, name=serial)
        self.serial, self.cfg, self.db = serial, cfg, db
        self.dev_cfg = cfg["devices"].get(serial, {})
        self.alive = threading.Event()
        self.alive.set()
        self.model = shell(serial, "getprop ro.product.model")
        self.android = shell(serial, "getprop ro.build.version.release")
        self.last_cycle_ok = False
        self.feeder = None
        self.adopt_tried = set()  # versions of a foreign install already tried
        self.reinstall_driver = False  # set after a Maestro run had to be killed

    # ----- setup

    def prepare(self):
        s = self.serial
        # Any power source: on the USB hub a phone may report AC (the Moto does,
        # and the A54 did until it was replugged), which `stayon usb` ignores,
        # so its screen timed out in the middle of flows.
        shell(s, "svc power stayon true")
        # Everything the farm tests works offline, and the farm's Wi-Fi is
        # unreliable. It also keeps Wi-Fi scans out of Google's fused location,
        # which would otherwise mix the phone's real position into the mock one.
        shell(s, "svc wifi disable")
        self.clear_usb_proxy()  # in case the farm stopped mid-download
        # MainActivity uses dummy analytics when this is set, which keeps the
        # farm's traffic out of the production Firebase numbers.
        shell(s, "settings put system firebase.test.lab true")
        adb(s, "logcat", "-G", "16M")
        self.quieten()
        self.set_sensors()
        lock_shade(s)
        out = enable_mock_location(s)
        if "Exception" in out or "failed" in out:
            self.db.event(s, "error", None, f"could not enable mock location:\n{out}")
        else:
            gpx = self.dev_cfg.get("gpx", self.cfg["default_gpx"])
            self.feeder = LocationFeeder(s, gpx, self.cfg["default_location"],
                                         self.cfg["replay_speed_mps"], self.db, self.alive)
            self.feeder.start()

    def set_sensors(self):
        """Puts the sensor service into the mode the phone's `sensors` setting
        asks for. Off is SensorService's restricted mode with an allowlist no
        package matches: apps (and Google Play services, which computes
        Soundscape's orientation on phones that have it) get no sensor events;
        only the system's own listeners do. It lasts until reboot or `enable`.

        A running app keeps the last orientation it was sent, which on a farm
        phone is lying flat, so turning the sensors off stops the app; it starts
        again without one."""
        s = self.serial
        want = self.dev_cfg.get("sensors", self.cfg["sensors"])
        restricted = SENSORS_OFF in shell(s, SENSOR_MODE)
        if want and restricted:
            shell(s, "dumpsys sensorservice enable > /dev/null")
        elif not want and not restricted:
            # Restricting a phone that's already restricted (by hand, with another
            # allowlist) leaves the old allowlist, so enable first.
            shell(s, f"dumpsys sensorservice enable > /dev/null; "
                     f"dumpsys sensorservice restrict {SENSORS_OFF} > /dev/null")
            if SENSORS_OFF not in shell(s, SENSOR_MODE):
                self.db.event(s, "error", None, "could not turn the sensors off")
            elif shell(s, f"pidof {PACKAGE}"):
                shell(s, f"am force-stop {PACKAGE}")

    def quieten(self):
        """Dims the screen right down and turns media volume to zero."""
        s = self.serial
        shell(s, "settings put system screen_brightness_mode 0; "
                 "settings put system screen_brightness 1; "
                 "cmd media_session volume --stream 3 --set 0")
        # Some ROMs (the AOSP Samsung) ignore that. Volume-down keys work there
        # because Soundscape is playing, so the keys adjust the media stream.
        if "volume is 0 " not in shell(s, "cmd media_session volume --stream 3 --get"):
            shell(s, "for i in $(seq 1 16); do input keyevent KEYCODE_VOLUME_DOWN; done")

    def push_extract(self):
        """Copies the cached offline map and its metadata sidecar into the app's
        extracts directory, named as CI does (without the build-date prefix).
        Returns False if the phone won't allow it."""
        if not current_extract:
            self.db.event(self.serial, "error", None, "no offline map cached; using the network")
            return True
        set_state(self.serial, "pushing offline map")
        name = re.sub(r"^\d{8}-\d{4}-", "", os.path.basename(current_extract))
        shell(self.serial, f"rm -f {DEVICE_EXTRACT_DIR}/*.pmtiles {DEVICE_EXTRACT_DIR}/*.geojson; "
                           f"mkdir -p {DEVICE_EXTRACT_DIR}")
        for src, dest in ((current_extract, name), (current_extract + ".geojson", name + ".geojson")):
            res = adb(self.serial, "push", src, f"{DEVICE_EXTRACT_DIR}/{dest}", timeout=900)
            if res.returncode != 0 and not stop_event.is_set():
                if "Permission denied" in res.stdout + res.stderr:
                    return False
                self.db.event(self.serial, "error", None,
                              f"offline map push failed:\n{res.stdout}{res.stderr}")
                return True
        return True

    def download_extract(self, cycle_id, cycle_dir):
        """For phones whose shell can't write to the app's Android/data (the
        Cubot J10): the app downloads the extract itself from its Offline maps
        screen, through this PC's network. usb_proxy's port is reversed onto
        the phone and set as its global HTTP proxy for the download only, so
        Wi-Fi stays off (the Cubot's barely reaches the access point)."""
        s = self.serial
        set_state(s, "downloading the offline map over USB")
        adb(s, "reverse", f"tcp:{usb_proxy.PORT}", f"tcp:{usb_proxy.PORT}")
        shell(s, f"settings put global http_proxy 127.0.0.1:{usb_proxy.PORT}")
        try:
            return self.run_flow(cycle_id, DOWNLOAD_EXTRACT_FLOW, cycle_dir, kind="setup",
                                 env={"EXTRACT": self.cfg["extract_name"]},
                                 timeout_minutes=70)
        finally:
            self.clear_usb_proxy()

    def clear_usb_proxy(self):
        shell(self.serial, "settings put global http_proxy :0")
        adb(self.serial, "reverse", "--remove", f"tcp:{usb_proxy.PORT}")

    def repair(self):
        """Undoes things the monkey can do from the notification shade."""
        shell(self.serial, "cmd connectivity airplane-mode disable; svc wifi disable; "
                           "cmd statusbar collapse; input keyevent KEYCODE_WAKEUP")
        self.quieten()
        self.set_sensors()
        lock_shade(self.serial)

    # ----- install / upgrade

    def last_update(self):
        out = shell(self.serial, f"dumpsys package {PACKAGE} | grep -m1 lastUpdateTime")
        return out.split("=", 1)[-1].strip() if out else None

    def farm_installed(self):
        """True if the app on the phone is the one the farm last installed.

        Anything else (a Play Store install, a debug build, a Play update over
        the farm's build) must not be tested as if it were a farm release.
        """
        rows = self.db.query("SELECT last_update FROM installs WHERE serial=?", (self.serial,))
        return bool(rows) and rows[0]["last_update"] == self.last_update()

    def install(self, build, upgrade):
        s = self.serial
        before = installed_version(s)
        res = adb(s, "install", "-r", build["apk_path"], timeout=600)
        out = res.stdout + res.stderr
        ok = res.returncode == 0 and "Success" in out
        kind = "upgrade" if upgrade and before else "install"
        if ok:
            self.db.execute("INSERT OR REPLACE INTO installs VALUES(?,?)", (s, self.last_update()))
        self.db.event(s, kind if ok else "error", build["version_code"],
                      f"{kind} {before} -> {build['version_name']} ({build['version_code']})"
                      + ("" if ok else "\n" + out))
        return ok

    def ensure_local(self, path):
        """Installs the locally built APK that local_apk names, to test in place of the CI
        releases. It's normally signed with a different key, so it can't go over a release (nor a
        release over it): whatever else is on the phone is uninstalled first, and the app set up
        afresh. A rebuilt APK is only installed again if its versionCode changes."""
        badging = run([find_aapt2(), "dump", "badging", path]).stdout
        m = re.search(r"versionCode='(\d+)' versionName='([^']*)'", badging)
        if not m:
            log(self.serial, f"local_apk {path}: couldn't read its version")
            return None
        build = {"apk_path": path, "version_code": int(m.group(1)), "version_name": m.group(2)}
        current = installed_version(self.serial)
        if current != build["version_code"] or not self.farm_installed():
            if current is not None:
                self.db.event(self.serial, "uninstall", current,
                              f"uninstalling {current} to test the local build "
                              f"{build['version_name']} ({build['version_code']})")
                adb(self.serial, "uninstall", PACKAGE)
            set_state(self.serial, f"installing local build {build['version_name']}")
            if not self.install(build, upgrade=False):
                return None
        return installed_version(self.serial) if self.farm_installed() else None

    def ensure_version(self, cycle_dir):
        """Installs or upgrades as needed. Returns the version to test, or None."""
        local = self.dev_cfg.get("local_apk", self.cfg.get("local_apk"))
        if local:
            return self.ensure_local(local)
        builds = self.db.builds()
        if not builds:
            return None
        latest = builds[-1]
        current = installed_version(self.serial)
        if (current is not None and self.farm_installed()
                and current not in {b["version_code"] for b in builds}):
            # Not a CI release: a local_apk build that has since been taken out of the config.
            # It's signed differently, so the release can only replace it, not upgrade it.
            self.db.event(self.serial, "uninstall", current,
                          "uninstalling a local build now that local_apk is unset")
            adb(self.serial, "uninstall", PACKAGE)
            current = None
        if (current is not None and not self.farm_installed()
                and current <= latest["version_code"] and current not in self.adopt_tried):
            self.adopt_tried.add(current)
            # A sideloaded release (same signing key) accepts the latest release
            # over the top, keeping its data, just as a user's update would. Play
            # Store and debug installs are signed differently and refuse it.
            set_state(self.serial, f"adopting Soundscape {current}")
            if self.install(latest, upgrade=True):
                self.db.event(self.serial, "adopt", latest["version_code"],
                              f"adopted a sideloaded release {current} by installing "
                              f"{latest['version_name']} over it")
                return installed_version(self.serial)
        if current is not None and not self.farm_installed():
            if not self.cfg["allow_reinstall"]:
                set_state(self.serial, f"blocked: Soundscape {current} on the phone wasn't "
                                       "installed by the farm; uninstall it by hand")
                return None
            self.db.event(self.serial, "uninstall", current,
                          "uninstalling a Soundscape the farm didn't install (allow_reinstall)")
            adb(self.serial, "uninstall", PACKAGE)
            current = None
        if current is None:
            # Start one release behind when we can, so the first upgrade
            # happens as soon as the first cycle is done.
            set_state(self.serial, "installing")
            self.install(builds[-2] if len(builds) > 1 else latest, upgrade=False)
        elif current < latest["version_code"]:
            set_state(self.serial, f"upgrading to {latest['version_name']}")
            if self.install(latest, upgrade=True):
                note = "" if self.last_cycle_ok else \
                    "previous cycle failed, so the suite's markers/route may not have existed"
                with self.suite_location():
                    self.run_flow(None, POST_UPGRADE_FLOW, cycle_dir, kind="upgrade", note=note)
        return installed_version(self.serial) if self.farm_installed() else None

    @contextlib.contextmanager
    def suite_location(self):
        if self.feeder:
            self.feeder.hold = tuple(self.cfg["suite_location"])
        try:
            yield
        finally:
            if self.feeder:
                self.feeder.hold = None

    # ----- tests

    def run_flow(self, cycle_id, path, cycle_dir, kind="flow", note="", record=True, env=None,
                 timeout_minutes=None):
        """Runs one Maestro flow and records its result. Returns True or False,
        or None if the farm stopped it (which isn't recorded)."""
        name = os.path.splitext(os.path.basename(path))[0]
        out_dir = os.path.join(cycle_dir, name)
        n = 1
        while os.path.exists(out_dir):  # the roaming flows run more than once
            n += 1
            out_dir = os.path.join(cycle_dir, f"{name}-{n}")
        os.makedirs(out_dir, exist_ok=True)
        report = os.path.join(out_dir, "report.xml")
        # Every flow expects the screen on; this is a backstop for stayon.
        shell(self.serial, "input keyevent KEYCODE_WAKEUP")
        # And the app, not an invisible notification shade, to have key focus
        # (see lock_shade); Home moves focus whatever holds it, and the flows'
        # launchApp brings the app back.
        if "NotificationShade" in shell(self.serial, "dumpsys window | grep -m1 mCurrentFocus"):
            shell(self.serial, "cmd statusbar collapse; input keyevent KEYCODE_HOME")
        self.unfloat_app()
        # Whether the phone roams with the map on (show_map in its [devices]
        # entry). ShowMap.yaml sets it, and the roaming flows skip the map
        # without it. Suite flows ignore it.
        env = {"SHOW_MAP": "true" if self.dev_cfg.get("show_map", True) else "false",
               **(env or {})}
        cmd = [os.path.expanduser("~/.maestro/bin/maestro"), "test", "--device", self.serial,
               "--format", "junit", "--output", report, "--test-output-dir", out_dir,
               "--debug-output", os.path.join(out_dir, "debug"), "--flatten-debug-output",
               "--no-ansi", *[a for k, v in env.items() for a in ("-e", f"{k}={v}")], path]
        if self.reinstall_driver:
            # A Maestro run killed at the timeout leaves the phone's driver
            # unreachable, and the next flows fail at launch until it's
            # reinstalled.
            cmd = cmd[:2] + ["--reinstall-driver"] + cmd[2:]
            self.reinstall_driver = False
        timeout = (timeout_minutes or self.cfg["flow_timeout_minutes"]) * 60
        # Each Maestro run keeps its debug log in $XDG_CACHE_HOME/maestro/logs
        # (else ~/.cache) and prunes all but the newest few runs' logs there.
        # Shared between phones, a long run had its log folder deleted by
        # another phone's run; its main thread then died at exit while the
        # driver's threads kept the JVM alive, until the flow timeout.
        own = os.path.join(RUNS_DIR, self.serial, "maestro-home")
        maestro_env = {"XDG_CACHE_HOME": os.path.join(own, "cache"),
                       "XDG_STATE_HOME": os.path.join(own, "state")}
        start = time.time()
        for attempt in range(2):
            set_state(self.serial, f"running {name}")
            if self.cfg["serialize_maestro"]:
                with maestro_lock:
                    res = run(cmd, timeout=timeout, env=maestro_env)
            else:
                res = run(cmd, timeout=timeout, env=maestro_env)
            output = res.stdout + res.stderr
            # The driver errors that decide a retry only reach the debug log.
            for extra in (report, os.path.join(out_dir, "debug", "maestro.log")):
                try:
                    with open(extra, errors="replace") as f:
                        output += f.read()
                except OSError:
                    pass
            if res.returncode == 0 or not MAESTRO_RETRY_RE.search(output) or attempt:
                break
            log(self.serial, f"{name}: transient launch failure, retrying once")
            if "gRPC" in output and "--reinstall-driver" not in cmd:
                # Maestro's on-device driver is unreachable (seen after a phone
                # sat on its lock screen); a fresh install brings it back.
                cmd = cmd[:2] + ["--reinstall-driver"] + cmd[2:]
        with open(os.path.join(out_dir, "maestro.log"), "w") as f:
            f.write(res.stdout + res.stderr)
        if "timeout after" in res.stderr:
            self.reinstall_driver = True
        if stop_event.is_set():
            return None
        status = "pass" if res.returncode == 0 else "fail"
        if status == "fail" and "timeout after" in res.stderr and "Flow Passed" in res.stdout:
            # Maestro reported the pass and then hung on the way out until the
            # flow timeout killed it (see maestro_env above).
            status = "pass"
            note = "\n".join(filter(None, [note, "Maestro hung after the flow passed; killed"]))
        if not record:
            return status == "pass"
        detail = "\n".join(filter(None, [note, "" if status == "pass" else
                                         (res.stderr or res.stdout)[-2000:]]))
        self.db.execute(
            "INSERT INTO results(cycle_id, serial, ts, kind, name, status, duration_s, detail, dir) "
            "VALUES(?,?,?,?,?,?,?,?,?)",
            (cycle_id, self.serial, start, kind, name, status, time.time() - start, detail, out_dir))
        return status == "pass"

    def recharge(self, level):
        """Stops the app and turns the screen off until the battery is back up to
        battery_resume_percent, for a phone that the farm's load drains faster than its USB port
        charges it. The next cycle starts the app again; the cycle after a recharge measures a
        fresh run of the app, not a continuation."""
        s = self.serial
        target = self.cfg["battery_resume_percent"]
        self.db.event(s, "recharge", installed_version(s),
                      f"battery at {level}%: app stopped and screen off until it's back to {target}%")
        shell(s, f"am force-stop {PACKAGE}")
        shell(s, "svc power stayon false; input keyevent KEYCODE_SLEEP")
        start, best, warned = time.time(), level, False
        while not stop_event.is_set():
            set_state(s, f"recharging: {level}% of {target}%")
            if level is not None and level >= target:
                break
            stop_event.wait(60)
            if adb(s, "get-state", timeout=15).stdout.strip() != "device":
                return  # unplugged: run() notices and stops this worker
            level = battery(s)[0]
            if level is not None and level > best:
                best, start = level, time.time()
            elif not warned and time.time() - start > 3600:
                warned = True  # one warning, and keep waiting: carrying on would only drain it
                self.db.event(s, "error", None, f"battery not charging: still {level}% after an hour "
                                                f"with the screen off")
        shell(s, "svc power stayon true; input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard")
        if not stop_event.is_set():
            self.db.event(s, "recharged", None, f"battery back to {level}%")

    def pause_for_investigation(self, version):
        """Before restarting the app, leaves the phone as the failed Restores left it, so that
        someone can see what needed the restart, until a file named resume-<serial> appears next to this script or pause_minutes
        pass. The other phones carry on meanwhile."""
        s = self.serial
        flag = os.path.join(FARM_DIR, f"resume-{s}")
        minutes = self.cfg["pause_minutes"]
        self.db.event(s, "paused", version, f"Restore couldn't get back to Home even with the app "
                                            f"brought to the front; paused for up to {minutes} "
                                            f"minutes before restarting it, until "
                                            f"{os.path.basename(flag)} exists")
        set_state(s, "paused: Restore failed")
        deadline = time.time() + minutes * 60
        while not stop_event.is_set() and time.time() < deadline and not os.path.exists(flag):
            stop_event.wait(5)
        if os.path.exists(flag):
            os.remove(flag)
            log(s, "resumed")

    def bring_to_front(self):
        """Brings the app's task back in front of anything the monkey left over it, full screen,
        without restarting it. MainActivity is singleTask, so starting it brings its task forward
        (clearing anything stacked on it in that task). Starting it in full screen windowing mode
        doesn't take a task that's already a pop-up out of freeform, but a task that can't be
        resized can't stay in freeform, so it's made unresizeable. It's left that way: the A54
        remembers the task was a pop-up, and once it's resizeable again the next configuration
        change (redraw_app's dark mode switch) puts it straight back into one. The setting only
        lasts as long as the task, so the app's next restart undoes it."""
        s = self.serial
        self.pin_task_fullscreen()
        shell(s, "input keyevent KEYCODE_HOME")
        shell(s, f"am start -W -n {PACKAGE}/.MainActivity", timeout=60)
        self.redraw_app()

    def pin_task_fullscreen(self):
        """Makes the app's task unresizeable if it's in a pop-up (freeform) window, and returns
        whether it was. See bring_to_front."""
        tasks = shell(self.serial, "dumpsys activity activities")
        m = re.search(r"Task\{\S+ #(\d+) [^}]*A=\d+:" + re.escape(PACKAGE) + r" [^}]*mode=(\S+)", tasks)
        if m is None or m.group(2) == "fullscreen":
            return False
        shell(self.serial, f"am task resizeable {m.group(1)} 0")
        return True

    def unfloat_app(self):
        """Takes the app out of a pop-up window before a flow. The monkey can leave it as one on
        the A54 even when Restore then passes, and in the small window the home buttons are off
        screen, so every flow of the cycle failed at its first tap."""
        if self.pin_task_fullscreen():
            log(self.serial, "app was in a pop-up window; back to full screen")
            shell(self.serial, "input keyevent KEYCODE_HOME")
            shell(self.serial, f"am start -W -n {PACKAGE}/.MainActivity", timeout=60)

    def redraw_app(self):
        """Switches dark mode over and back again, which only redraws the app if it's fine. The
        Moto has been left with the app's window up but blank grey: its Compose content had
        stopped composing and drawing (no frames, nothing in the view tree), though the process
        ran on. Turning the screen off and on didn't bring it back; a configuration change did.
        It can go blank again within a minute, so this is done before the first Restore after
        the monkey soak too, not just when bringing the app back to the front for the retry."""
        s = self.serial
        night = re.search(r"Night mode: (\w+)", shell(s, "cmd uimode night"))
        if night:
            shell(s, "cmd uimode night " + ("no" if night.group(1) == "yes" else "yes"))
            time.sleep(3)
            shell(s, f"cmd uimode night {night.group(1)}")
            time.sleep(3)

    def run_monkey(self, cycle_id, cycle_dir):
        set_state(self.serial, "monkey soak")
        seed = int(time.time()) % 100000
        start = time.time()
        res = adb(self.serial, "shell", "monkey", "-p", PACKAGE, "-s", str(seed),
                  "--throttle", str(self.cfg["monkey_throttle_ms"]),
                  "--pct-syskeys", "0", "-v", str(self.cfg["monkey_events"]),
                  timeout=self.cfg["monkey_events"] * self.cfg["monkey_throttle_ms"] / 1000 + 600)
        with open(os.path.join(cycle_dir, "monkey.log"), "w") as f:
            f.write(res.stdout + res.stderr)
        ok = "Monkey finished" in res.stdout
        self.db.execute(
            "INSERT INTO results(cycle_id, serial, ts, kind, name, status, duration_s, detail, dir) "
            "VALUES(?,?,?,?,?,?,?,?,?)",
            (cycle_id, self.serial, start, "monkey", f"monkey seed {seed}",
             "pass" if ok else "fail", time.time() - start,
             "" if ok else res.stdout[-2000:], cycle_dir))
        self.repair()
        return ok

    def set_up_version(self):
        rows = self.db.query("SELECT version_code FROM setups WHERE serial=?", (self.serial,))
        return rows[0]["version_code"] if rows else None

    def set_up(self, cycle_id, cycle_dir, version, tally):
        """Onboards from scratch and builds the state the roaming flows and
        PostUpgrade use: runs the Maestro suite in Master.yaml order, held at
        suite_location, with the offline map pushed and the farm's settings
        applied after Onboarding. Done once per release on each phone."""
        # Onboarding wipes whatever an earlier set-up made, so until this one
        # finishes the phone isn't set up (a farm stopped mid-way redoes it).
        self.db.execute("DELETE FROM setups WHERE serial=?", (self.serial,))
        failed_before = tally[False]
        with self.suite_location():
            for flow in suite_flows():
                if stop_event.is_set():
                    return
                result = self.run_flow(cycle_id, os.path.join(MAESTRO_DIR, flow), cycle_dir)
                if result is not None:
                    tally[result] += 1
                if flow == "Onboarding.yaml":
                    if not result:
                        return  # tried again next cycle
                    # Onboarding cleared the app's storage and settings, so put the
                    # offline map back and switch search to offline.
                    if not self.push_extract():
                        result = self.download_extract(cycle_id, cycle_dir)
                        if result is not None:
                            tally[result] += 1
                        if not result:
                            return
                    result = self.run_flow(cycle_id, FARM_SETTINGS_FLOW, cycle_dir, kind="setup")
                    if result is not None:
                        tally[result] += 1
                    if not result:
                        return
        if stop_event.is_set():
            return
        # The suite runs with the map off, as in CI; roaming has it on.
        result = self.run_flow(cycle_id, SHOW_MAP_FLOW, cycle_dir, kind="setup")
        if result is None:
            return
        tally[result] += 1
        # Recorded even if a later suite flow failed: that's a result, and a
        # missing marker or route is caught by the check after the monkey.
        self.db.execute("INSERT OR REPLACE INTO setups VALUES(?,?,?)",
                        (self.serial, version, int(tally[False] == failed_before)))

    def cycle(self):
        s = self.serial
        level, temp = battery(s)
        if temp and temp > self.cfg["max_battery_temp_c"]:
            set_state(s, f"cooling down ({temp}°C)")
            stop_event.wait(600)
            return
        if level is not None and level <= self.cfg["battery_low_percent"]:
            self.recharge(level)
            return
        stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
        cycle_dir = os.path.join(RUNS_DIR, s, stamp)

        if "isKeyguardShowing=true" in shell(s, "dumpsys window | grep isKeyguardShowing"):
            shell(s, "input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard")
            if "isKeyguardShowing=true" in shell(s, "dumpsys window | grep isKeyguardShowing"):
                # A PIN or pattern: Maestro can't get past it, and every flow
                # fails in under a second with the driver unreachable.
                set_state(s, "locked: set Screen lock to None on this phone")
                stop_event.wait(300)
                return

        version = self.ensure_version(cycle_dir)
        if version is None:
            if not self.db.builds():
                set_state(s, "waiting for a release download")
            stop_event.wait(300)
            return
        os.makedirs(cycle_dir, exist_ok=True)

        device_since = shell(s, "date '+%Y-%m-%d %H:%M:%S'")
        adb(s, "logcat", "-c")
        cycle_id = self.db.execute(
            "INSERT INTO cycles(serial, model, android, version_code, started, status, dir) "
            "VALUES(?,?,?,?,?,?,?)",
            (s, self.model, self.android, version, time.time(), "running", cycle_dir))

        tally = {True: 0, False: 0}
        if self.set_up_version() != version:
            self.set_up(cycle_id, cycle_dir, version, tally)
        # The roaming flows only look around and don't assert what's nearby, so
        # they run on the moving walk rather than held at suite_location.
        # Without a finished set-up there's no offline map, settings, markers
        # or route to roam with; the monkey still runs and set-up is retried
        # next cycle.
        flows = roam_flows() if self.set_up_version() == version else []
        complete = self.db.query("SELECT complete FROM setups WHERE serial=?", (s,))
        if flows and not complete[0]["complete"] and not stop_event.is_set():
            # A suite flow failed during set-up; build what it missed first.
            with self.suite_location():
                restored = self.run_flow(cycle_id, RESTORE_FLOW, cycle_dir, kind="restore")
            if restored is not None:
                tally[restored] += 1
            if restored:
                self.db.execute("UPDATE setups SET complete=1 WHERE serial=?", (s,))
        for _ in range(self.cfg["roam_rounds"]):
            random.shuffle(flows)
            for flow in flows:
                if stop_event.is_set():
                    break
                result = self.run_flow(cycle_id, os.path.join(ROAM_DIR, flow), cycle_dir,
                                       kind="roam")
                if result is not None:
                    tally[result] += 1
        if not stop_event.is_set():
            self.run_monkey(cycle_id, cycle_dir)
        # The monkey can delete the markers or route, or change settings, and
        # roaming and the next upgrade's PostUpgrade check need them. Restore
        # puts them back without restarting the app, or with a restart if it
        # can't reach Home; only if that fails too is the phone set up again,
        # now rather than at the start of the next cycle,
        # which may begin with an upgrade.
        # Restore also runs when a suite flow failed during set-up, and builds
        # what that flow didn't; otherwise every roam round fails for want of
        # a marker or the route until the next release. A reset only follows
        # a set-up that completed: after one that didn't, it would most likely
        # fail the same way and onboard again every cycle.
        complete = self.db.query("SELECT complete FROM setups WHERE serial=?", (s,))
        if not stop_event.is_set() and self.set_up_version() == version:
            with self.suite_location():
                self.redraw_app()
                restored = self.run_flow(cycle_id, RESTORE_FLOW, cycle_dir, kind="restore")
                if restored is False and not stop_event.is_set():
                    # The monkey can leave the app where Restore can't get it
                    # back to Home: behind another app (Gmail, from Contact
                    # support) or a launcher panel, or with its window shrunk
                    # to a pop-up. Bringing it back to the front gets out of
                    # those without ending its process, which is what lets it
                    # run for hours.
                    self.db.event(s, "front", version, "Restore couldn't get back to Home "
                                                       "after the monkey soak; bringing the app "
                                                       "to the front")
                    self.bring_to_front()
                    restored = self.run_flow(cycle_id, RESTORE_FLOW, cycle_dir, kind="restore")
                if restored is False and not stop_event.is_set():
                    if self.cfg["pause_on_restore_failure"]:
                        self.pause_for_investigation(version)
                if restored is False and not stop_event.is_set():
                    # Restarting the app gets out of anything else, and keeps
                    # its markers, route and settings, which setting up again
                    # would wipe.
                    self.db.event(s, "restart", version, "Restore still couldn't get back to "
                                                         "Home; restarting the app")
                    shell(s, f"am force-stop {PACKAGE}")
                    restored = self.run_flow(cycle_id, RESTORE_FLOW, cycle_dir, kind="restore")
            if restored is not None:
                tally[restored] += 1
            if restored and not complete[0]["complete"]:
                self.db.execute("UPDATE setups SET complete=1 WHERE serial=?", (s,))
            if restored is False and complete[0]["complete"] and not stop_event.is_set():
                self.db.event(s, "reset", version, "couldn't restore the markers, route or "
                                                   "settings after the monkey soak; setting up again")
                self.set_up(cycle_id, cycle_dir, version, tally)
        passed, failed = tally[True], tally[False]
        if stop_event.is_set():
            self.db.execute("UPDATE cycles SET ended=?, status=? WHERE id=?",
                            (time.time(), "stopped", cycle_id))
            return

        with open(os.path.join(cycle_dir, "logcat.txt"), "w") as f:
            f.write(adb(s, "logcat", "-d", timeout=120).stdout)
        problems = collect_dropbox(s, device_since)
        for i, (kind, text) in enumerate(problems):
            with open(os.path.join(cycle_dir, f"{kind}-{i}.txt"), "w") as f:
                f.write(text)
            self.db.event(s, kind, version, text)
        crashes = sum(1 for k, _ in problems if k == "crash")
        anrs = len(problems) - crashes
        level, temp = battery(s)
        status = "pass" if failed == 0 and not problems else "fail"
        self.last_cycle_ok = failed == 0
        uptime = app_uptime_s(s)
        self.db.execute(
            "UPDATE cycles SET ended=?, status=?, flows_passed=?, flows_failed=?, crashes=?, anrs=?, "
            "pss_kb=?, battery_level=?, battery_temp=?, app_uptime_s=? WHERE id=?",
            (time.time(), status, passed, failed, crashes, anrs, pss_kb(s), level, temp, uptime,
             cycle_id))
        set_state(s, f"cycle {status}: {passed} passed, {failed} failed, "
                     f"{crashes} crashes, {anrs} ANRs, {format_uptime(uptime)}")
        if uptime is not None and not stop_event.is_set():
            # The flows keep the app on screen, so its memory is otherwise only ever read there.
            # The Play Store's memory rules set a different limit for an app running in the
            # background, so take a reading with only its service running too. The next
            # cycle's first flow brings it back.
            set_state(s, "app in the background for a reading")
            shell(s, "input keyevent KEYCODE_HOME")
            stop_event.wait(15)
            record_vitals(self.db, s)

    def run(self):
        set_state(self.serial, "preparing", model=self.model, android=self.android)
        try:
            if self.dev_cfg.get("skip"):
                # Not prepared either: a skipped phone may be someone's own, and
                # preparing turns its Wi-Fi and sensors off, among other things.
                set_state(self.serial, "skipped in config")
                stop_event.wait()
                return
            self.prepare()
            while not stop_event.is_set():
                if adb(self.serial, "get-state", timeout=15).stdout.strip() != "device":
                    break
                try:
                    self.cycle()
                except Exception as e:  # keep the farm going; record and retry
                    self.db.event(self.serial, "error", None, f"worker exception: {e!r}")
                    stop_event.wait(60)
        finally:
            self.alive.clear()
            set_state(self.serial, "disconnected")


# --------------------------------------------------------------------------- iPhone


IOS_BUNDLE = "org.scottishtecharmy.soundscape"
PMD3 = os.path.expanduser("~/.local/bin/pymobiledevice3")


def pmd3(udid, *args, timeout=120, tunnel=False):
    """Runs pymobiledevice3 against one iPhone. Developer services (location,
    launch, process list, screenshots) go over the iOS 17+ tunnel, which
    --userspace builds in-process, so nothing here needs root."""
    extra = ["--userspace"] if tunnel else []
    return run([PMD3, *args, *extra, "--udid", udid], timeout=timeout)


def ios_devices():
    """UDIDs of USB-connected iPhones. --simple skips lockdown, so a phone that
    isn't answering can't hang discovery."""
    if not os.path.exists(PMD3):
        return []
    res = run([PMD3, "usbmux", "list", "--usb", "--simple"], timeout=20)
    try:
        return json.loads(res.stdout or "[]")
    except json.JSONDecodeError:
        return []


class IosWorker(threading.Thread):
    """Observes Soundscape on an iPhone. Maestro can't drive a physical iPhone,
    so there is no UI testing: the app (installed and upgraded by TestFlight)
    is kept running on a replayed GPX walk while the farm records upgrades,
    restarts, crash and hang reports, battery and a Soundscape syslog."""

    def __init__(self, udid, cfg, db):
        super().__init__(daemon=True, name=udid)
        self.udid, self.cfg, self.db = udid, cfg, db
        self.dev_cfg = cfg["devices"].get(udid, {})
        self.dir = os.path.join(RUNS_DIR, udid)
        self.player = self.syslog = None
        self.syslog_day = None
        self.version = self.pid = None
        info = {}
        res = run([PMD3, "usbmux", "list", "--usb"], timeout=60)
        try:
            info = next(d for d in json.loads(res.stdout) if d.get("UniqueDeviceID") == udid)
        except Exception:
            pass
        self.model = f"{info.get('DeviceName', 'iPhone')} ({info.get('ProductType', '?')})"
        self.os = f"iOS {info.get('ProductVersion', '?')}"

    # ----- background processes

    def write_walk(self):
        """One there-and-back of the GPX track as a timestamped GPX at 1 Hz,
        which `simulate-location play` replays in real time. A moving track
        matters: the app ignored a single fixed simulated point."""
        track = Track(self.dev_cfg.get("gpx", self.cfg["default_gpx"]),
                      self.cfg["default_location"], self.cfg["replay_speed_mps"])
        t0 = dt.datetime.now(dt.timezone.utc)
        pts = []
        for i in range(track.round_trip_seconds()):
            lat, lon = track.position(i)
            stamp = (t0 + dt.timedelta(seconds=i)).strftime("%Y-%m-%dT%H:%M:%SZ")
            pts.append(f'<trkpt lat="{lat:.7f}" lon="{lon:.7f}"><time>{stamp}</time></trkpt>')
        path = os.path.join(self.dir, "walk.gpx")
        with open(path, "w") as f:
            f.write('<?xml version="1.0"?><gpx version="1.1" creator="soundscape-test-farm">'
                    f'<trk><trkseg>{"".join(pts)}</trkseg></trk></gpx>')
        return path

    def keep_processes_running(self):
        if self.player is None or self.player.poll() is not None:
            with open(os.path.join(self.dir, "player.log"), "w") as out:
                self.player = subprocess.Popen(
                    [PMD3, "developer", "dvt", "simulate-location", "play", "--userspace",
                     "--udid", self.udid, self.write_walk()],
                    stdin=subprocess.DEVNULL, stdout=out, stderr=subprocess.STDOUT)
        day = dt.date.today().strftime("%Y%m%d")
        if self.syslog_day != day or self.syslog is None or self.syslog.poll() is not None:
            if self.syslog and self.syslog.poll() is None:
                self.syslog.terminate()
            self.syslog = subprocess.Popen(
                # DEBUG is mostly CoreMotion sensor fusion: ~1.7 GB a day.
                [PMD3, "syslog", "live", "--udid", self.udid, "-pn", "Soundscape", "--no-debug",
                 "-o", os.path.join(self.dir, f"syslog-{day}.txt")],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            self.syslog_day = day

    def stop_processes(self):
        for p in (self.player, self.syslog):
            if p and p.poll() is None:
                p.terminate()
        pmd3(self.udid, "developer", "dvt", "simulate-location", "clear", tunnel=True, timeout=60)

    # ----- one observation

    def installed_version(self):
        res = pmd3(self.udid, "apps", "list", timeout=120)
        try:
            app = json.loads(res.stdout)[IOS_BUNDLE]
            return f"{app['CFBundleShortVersionString']} ({app['CFBundleVersion']})"
        except Exception:
            return None

    def app_pid(self):
        res = pmd3(self.udid, "developer", "dvt", "process-id-for-bundle-id", IOS_BUNDLE,
                   tunnel=True, timeout=60)
        m = re.search(r"^\s*(\d+)\s*$", res.stdout, re.M)
        return int(m.group(1)) if m and int(m.group(1)) > 0 else None

    def new_reports(self, obs_dir):
        """Pulls Soundscape crash/hang reports not seen before. Returns
        (kind, name, text); bug_type 309 is a crash, anything else (hangs,
        resource reports) is counted as an ANR."""
        res = pmd3(self.udid, "crash", "ls", timeout=120)
        names = [n.strip().strip('",') for n in res.stdout.splitlines()]
        names = [n for n in names if re.search(r"(^|/)Soundscape[-_.]", n)]
        seen = {r["detail"] for r in self.db.query(
            "SELECT detail FROM events WHERE serial=? AND kind='ios-report'", (self.udid,))}
        found = []
        for name in names:
            if name in seen:
                continue
            base = os.path.basename(name)
            os.makedirs(obs_dir, exist_ok=True)
            pmd3(self.udid, "crash", "pull", obs_dir, "--match", re.escape(base), timeout=300)
            text = ""
            for path in glob.glob(os.path.join(obs_dir, "**", base), recursive=True):
                with open(path, errors="replace") as f:
                    text = f.read()
            m = re.search(r'"bug_type"\s*:\s*"?(\d+)', text[:2000])
            kind = "crash" if m and m.group(1) == "309" else "anr"
            self.db.execute("INSERT INTO events(serial, ts, kind, version_code, detail) "
                            "VALUES(?,?,?,?,?)", (self.udid, time.time(), "ios-report", None, name))
            found.append((kind, base, f"{base} (bug_type {m.group(1) if m else '?'})\n{text[:4000]}"))
        return found

    def battery(self):
        res = pmd3(self.udid, "diagnostics", "battery", "single", timeout=60)
        level = re.search(r'"CurrentCapacity":\s*(\d+)', res.stdout)
        temp = re.search(r'"Temperature":\s*(\d+)', res.stdout)
        return (int(level.group(1)) if level else None,
                int(temp.group(1)) / 100 if temp else None)

    def observe(self):
        u = self.udid
        started = time.time()
        obs_dir = os.path.join(self.dir, dt.datetime.now().strftime("%Y%m%d-%H%M%S"))
        set_state(u, "checking")
        # Needed after every reboot; succeeds immediately when already mounted.
        mount = pmd3(u, "mounter", "auto-mount", tunnel=True, timeout=300)
        if mount.returncode != 0:
            self.db.event(u, "error", None, "developer disk image mount failed (is Developer "
                                            "Mode on?)\n" + (mount.stdout + mount.stderr)[-1500:])
        self.keep_processes_running()

        version = self.installed_version()
        if version and self.version and version != self.version:
            self.db.event(u, "upgrade", None, f"TestFlight upgrade {self.version} -> {version}")
        self.version = version or self.version

        problems = self.new_reports(obs_dir)
        for kind, name, text in problems:
            self.db.event(u, kind, None, text)
        pid = self.app_pid()
        if pid is None:
            if version:
                self.db.event(u, "relaunch", None, "Soundscape wasn't running; relaunching it")
                pmd3(u, "developer", "dvt", "launch", "--no-kill-existing", IOS_BUNDLE,
                     tunnel=True, timeout=60)
                pid = self.app_pid()
            else:
                self.db.event(u, "error", None, "Soundscape isn't installed (TestFlight)")
        elif self.pid and pid != self.pid and not problems:
            self.db.event(u, "restart", None, f"Soundscape restarted (pid {self.pid} -> {pid}) "
                                              "with no crash report: killed by iOS or the user")
        self.pid = pid

        level, temp = battery = self.battery()
        crashes = sum(1 for k, _, _ in problems if k == "crash")
        anrs = len(problems) - crashes
        status = "pass" if pid and not problems else "fail"
        self.db.execute(
            "INSERT INTO cycles(serial, model, android, version_code, started, ended, status, "
            "flows_passed, flows_failed, crashes, anrs, battery_level, battery_temp, dir) "
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (u, self.model, self.os, None, started, time.time(), status, 0, 0, crashes, anrs,
             level, temp, obs_dir))
        set_state(u, f"observing Soundscape {version or '?'} (pid {pid or 'none'}); "
                     f"{crashes} crashes, {anrs} hang/other reports",
                  model=self.model, android=self.os, app=version)

    def run(self):
        os.makedirs(self.dir, exist_ok=True)
        set_state(self.udid, "preparing", model=self.model, android=self.os)
        try:
            while not stop_event.is_set():
                if self.udid not in ios_devices():
                    break
                try:
                    self.observe()
                except Exception as e:  # keep observing; record and retry
                    self.db.event(self.udid, "error", None, f"iPhone worker exception: {e!r}")
                stop_event.wait(self.cfg["ios_poll_minutes"] * 60)
        finally:
            self.stop_processes()
            set_state(self.udid, "disconnected")


# --------------------------------------------------------------------------- report


def battery_cell(st, last):
    """The live reading if there is one (Android), else the last cycle's."""
    if st.get("battery"):
        level, temp, ts = st["battery"]
        when = fmt_time(ts)
    elif last:
        level, temp, when = last["battery_level"], last["battery_temp"], "at the last cycle"
    else:
        return ""
    text = f"{'' if level is None else level}% / {'' if temp is None else temp}°C"
    return f"{html.escape(text)}<div class=dim>{html.escape(when)}</div>"


def pss_cell(st, db, serial):
    """The app's memory use: the live reading if there is one (Android), else the newest cycle
    that recorded it (one stopped by the farm shutting down doesn't)."""
    if st.get("pss") and st["pss"][0] is not None:
        kb, ts = st["pss"]
        when = fmt_time(ts)
    else:
        row = db.query("SELECT pss_kb, ended FROM cycles WHERE serial=? AND pss_kb IS NOT NULL "
                       "ORDER BY id DESC LIMIT 1", (serial,))
        if not row:
            return ""
        kb, when = row[0]["pss_kb"], f"at the cycle ending {fmt_time(row[0]['ended'])}"
    return f"{kb // 1024} MB<div class=dim>{html.escape(when)}</div>{pss_parts_table(st, db, serial)}"


PSS_CHANGE_WINDOW_H = 4  # the breakdown's change column covers at most this long
PSS_CHANGE_AVERAGE_MIN = 15  # each end of the window is the mean of this many minutes


def pss_parts_table(st, db, serial):
    """What the live PSS is made of, each part with its change over the last
    PSS_CHANGE_WINDOW_H hours of this run of the app (growth across an app restart would mean
    nothing). Readings move tens of MB from minute to minute, so the change is between the means
    of the first and last PSS_CHANGE_AVERAGE_MIN minutes of the window, on-screen readings only."""
    parts = st.get("pss_parts")
    if not parts:
        return ""
    now = time.time()
    since = now - PSS_CHANGE_WINDOW_H * 3600
    if st.get("uptime") and st["uptime"][0] is not None:
        since = max(since, st["uptime"][1] - st["uptime"][0])
    rows_in = db.query("SELECT * FROM vitals WHERE serial=? AND ts>=? AND pss_native_kb IS NOT NULL "
                       "AND proc_state=? ORDER BY ts", (serial, since, PROC_STATE_TOP))
    span = PSS_CHANGE_AVERAGE_MIN * 60
    hours = (rows_in[-1]["ts"] - rows_in[0]["ts"]) / 3600 if rows_in else 0
    usable = hours * 3600 >= 3 * span  # the two averaged ends mustn't overlap much
    head = [r for r in rows_in if r["ts"] < rows_in[0]["ts"] + span] if usable else []
    tail = [r for r in rows_in if r["ts"] > rows_in[-1]["ts"] - span] if usable else []

    def mean(rs, key):
        vals = [r[f"pss_{key}_kb"] for r in rs if r[f"pss_{key}_kb"] is not None]
        return sum(vals) / len(vals) if vals else None

    rows = []
    for key, _, name in PSS_PARTS:
        if key not in parts:
            continue
        change = ""
        a, b = (mean(head, key), mean(tail, key)) if usable else (None, None)
        if a is not None and b is not None:
            change = f"{(b - a) / 1024:+.0f}"
        rows.append(f"<tr><td>{name}</td><td>{parts[key] // 1024}</td><td>{change}</td></tr>")
    over = f"over {hours:.1f}h" if usable else ""
    return (f"<table class=parts><tr><th></th><th>MB</th><th>{over}</th></tr>{''.join(rows)}</table>")


def anon_cell(st, db, serial, since):
    """RssAnon + VmSwap now, as a share of the phone's RAM, with the peak since [since]: what the
    Play Store's memory rules measure."""
    reading = st.get("anon")
    ram = st.get("mem_total_kb")
    if not reading or reading[0] is None:
        return ""
    anon, swap, state, ts = reading
    total = anon + (swap or 0)
    share = f" ({100 * total / ram:.0f}% of {ram / 1048576:.1f} GB)" if ram else ""
    detail = [f"RssAnon {anon // 1024} + VmSwap {(swap or 0) // 1024} MB, "
              f"{proc_state_name(state)}, {fmt_time(ts)}"]
    # The Play Store's limits differ on screen and in the background, so a peak for each
    for value in (PROC_STATE_TOP, PROC_STATE_FOREGROUND_SERVICE):
        name = proc_state_name(value)
        row = db.query("SELECT MAX(rss_anon_kb + COALESCE(vm_swap_kb, 0)) p FROM vitals "
                       "WHERE serial=? AND ts>? AND rss_anon_kb IS NOT NULL AND proc_state=?",
                       (serial, since, value))
        peak = row[0]["p"] if row and row[0]["p"] else None
        if peak:
            limit = play_memory_limit_kb(ram, value)
            text = f"24h peak {name} {peak // 1024} MB"
            if limit:
                text += f", {100 * peak / limit:.0f}% of the {limit / 1048576:g} GB Play limit"
            detail.append((text, limit is not None and peak > limit))
    lines = [(d, False) if isinstance(d, str) else d for d in detail]
    return (f"{total // 1024} MB{html.escape(share)}"
            + "".join(f"<div class=\"dim{' fail' if over else ''}\">{html.escape(d)}</div>"
                      for d, over in lines))


def uptime_cell(st, last):
    """How long the app has been running: the live reading if there is one (Android), else
    as at the end of the last cycle."""
    if st.get("uptime"):
        seconds, ts = st["uptime"]
        when = fmt_time(ts)
    elif last and last["app_uptime_s"] is not None:
        seconds, when = last["app_uptime_s"], "at the last cycle"
    else:
        return ""  # e.g. an iPhone, whose cycles don't record it
    if seconds is None:
        text = "not running"
    else:
        h, rem = divmod(seconds, 3600)
        text = f"{h}h {rem // 60:02d}m" if h else f"{rem // 60}m"
    return f"{html.escape(text)}<div class=dim>{html.escape(when)}</div>"


def fmt_time(ts):
    return dt.datetime.fromtimestamp(ts).strftime("%Y-%m-%d %H:%M") if ts else ""


def record_vitals(db, serial):
    """Reads the phone's battery, and how long the app has been running, now: for the report
    and the vitals table."""
    level, temp = battery(serial)
    if level is None and temp is None:
        return  # disconnected or not answering
    uptime = app_uptime_s(serial)
    pss, parts = app_meminfo(serial) if uptime is not None else (None, {})
    anon, swap = app_anon_kb(serial) if uptime is not None else (None, None)
    state = app_proc_state(serial) if uptime is not None else None
    with state_lock:
        ram = device_state.get(serial, {}).get("mem_total_kb")
    if ram is None:
        ram = mem_total_kb(serial)
    now = time.time()
    part_columns = [f"pss_{key}_kb" for key, _, _ in PSS_PARTS]
    db.execute("INSERT INTO vitals (serial, ts, battery_level, battery_temp, app_uptime_s, pss_kb, "
               f"rss_anon_kb, vm_swap_kb, mem_total_kb, proc_state, {', '.join(part_columns)}) "
               f"VALUES({', '.join('?' * (10 + len(part_columns)))})",
               (serial, now, level, temp, uptime, pss, anon, swap, ram, state,
                *(parts.get(key) for key, _, _ in PSS_PARTS)))
    with state_lock:
        device_state.setdefault(serial, {})["battery"] = (level, temp, now)
        device_state[serial]["uptime"] = (uptime, now)
        device_state[serial]["pss"] = (pss, now)
        device_state[serial]["pss_parts"] = parts
        device_state[serial]["anon"] = (anon, swap, state, now)
        device_state[serial]["mem_total_kb"] = ram


def write_report(db):
    e = html.escape
    day_ago = time.time() - 86400
    versions = {b["version_code"]: b["version_name"] for b in db.builds()}
    rows = []
    with state_lock:
        states = {k: dict(v) for k, v in device_state.items()}
    for serial, st in sorted(states.items()):
        last = db.query("SELECT * FROM cycles WHERE serial=? AND ended IS NOT NULL "
                        "ORDER BY id DESC LIMIT 1", (serial,))
        day = db.query("SELECT COUNT(*) n, SUM(status='pass') ok, SUM(crashes) c, SUM(anrs) a "
                       "FROM cycles WHERE serial=? AND started>? AND ended IS NOT NULL",
                       (serial, day_ago))[0]
        last = last[0] if last else None
        ver = last["version_code"] if last else None
        rows.append(f"""<tr><td>{e(serial)}</td><td>{e(st.get('model', ''))}</td>
            <td>{e(st.get('android', ''))}</td><td>{e(versions.get(ver, str(ver or st.get('app', ''))))}</td>
            <td>{e(st['state'])}<div class=dim>since {fmt_time(st['since'])}</div></td>
            <td class="{last['status'] if last else ''}">{e(last['status']) if last else ''}</td>
            <td>{day['ok'] or 0}/{day['n']}</td><td>{day['c'] or 0}</td><td>{day['a'] or 0}</td>
            <td>{pss_cell(st, db, serial)}</td>
            <td>{anon_cell(st, db, serial, day_ago)}</td>
            <td>{uptime_cell(st, last)}</td>
            <td>{battery_cell(st, last)}</td></tr>""")
    failures = "".join(
        f"<tr><td>{fmt_time(r['ts'])}</td><td>{e(r['serial'])}</td><td>{e(r['kind'])}</td>"
        f"<td>{e(r['name'])}</td><td><a href='{e(os.path.relpath(r['dir'], FARM_DIR))}'>files</a></td>"
        f"<td><pre>{e((r['detail'] or '')[-600:])}</pre></td></tr>"
        for r in db.query("SELECT * FROM results WHERE status!='pass' ORDER BY id DESC LIMIT 50"))
    events = "".join(
        f"<tr><td>{fmt_time(r['ts'])}</td><td>{e(r['serial'] or '')}</td><td>{e(r['kind'])}</td>"
        f"<td>{e(versions.get(r['version_code'], str(r['version_code'] or '')))}</td>"
        f"<td><pre>{e((r['detail'] or '')[:600])}</pre></td></tr>"
        for r in db.query("SELECT * FROM events ORDER BY id DESC LIMIT 50"))
    builds = ", ".join(f"{e(b['version_name'])} ({b['version_code']})" for b in db.builds())
    page = f"""<!doctype html><html><head><meta charset=utf-8>
<meta name=viewport content="width=device-width, initial-scale=1">
<meta http-equiv=refresh content=60><title>Soundscape Test Farm</title><style>
:root{{--bg:#fff;--fg:#1a1a1a;--dim:#666;--line:#ddd;--pass:#1a7f37;--fail:#cf222e}}
@media (prefers-color-scheme: dark){{:root{{--bg:#111;--fg:#eee;--dim:#999;--line:#333;
--pass:#3fb950;--fail:#ff7b72}}}}
body{{background:var(--bg);color:var(--fg);font:14px system-ui,sans-serif;margin:16px}}
table{{border-collapse:collapse;width:100%;margin-bottom:24px}}
td,th{{border-bottom:1px solid var(--line);padding:6px 8px;text-align:left;vertical-align:top}}
.dim{{color:var(--dim);font-size:12px}} .pass{{color:var(--pass)}} .fail{{color:var(--fail)}}
pre{{margin:0;white-space:pre-wrap;font-size:12px;max-width:70ch}} a{{color:inherit}}
.wrap{{overflow-x:auto}}
table.parts{{width:auto;margin:4px 0 0;font-size:12px;color:var(--dim)}}
table.parts td,table.parts th{{border:0;padding:0 8px 0 0;font-weight:normal;text-align:right}}
table.parts td:first-child{{text-align:left}}</style></head><body>
<h1>Soundscape test farm</h1><p class=dim>Updated {fmt_time(time.time())}. Cached releases: {builds or 'none yet'}</p>
<h2>Devices</h2><div class=wrap><table><tr><th>Serial</th><th>Model</th><th>OS</th><th>App</th>
<th>Now</th><th>Last cycle</th><th>Passed 24h</th><th>Crashes 24h</th><th>ANRs 24h</th>
<th>PSS</th><th>Anon + swap</th><th>App uptime</th><th>Battery</th></tr>{''.join(rows)}</table></div>
<h2>Recent failures</h2><div class=wrap><table><tr><th>When</th><th>Device</th><th>Kind</th>
<th>Name</th><th></th><th>Detail</th></tr>{failures}</table></div>
<h2>Events</h2><div class=wrap><table><tr><th>When</th><th>Device</th><th>Kind</th><th>App</th>
<th>Detail</th></tr>{events}</table></div></body></html>"""
    tmp = REPORT_PATH + ".tmp"
    with open(tmp, "w") as f:
        f.write(page)
    os.replace(tmp, REPORT_PATH)


def prune_runs(keep_days):
    cutoff = time.time() - keep_days * 86400
    # Per-cycle folders (named by timestamp) and iPhone syslogs; not the
    # per-phone Maestro home or the iPhone's walk and player log.
    for path in (glob.glob(os.path.join(RUNS_DIR, "*", "[0-9]*"))
                 + glob.glob(os.path.join(RUNS_DIR, "*", "syslog-*"))):
        if os.path.getmtime(path) < cutoff:
            subprocess.run(["rm", "-rf", path])


# --------------------------------------------------------------------------- main


def main():
    # The farm, and Maestro under it, run in UTC. Maestro names its log folders by local time
    # and keeps the newest few by name, so when the clocks went back each new folder sorted as
    # the oldest and was deleted as soon as it was made, and every run crashed on its own lock
    # file for an hour (25 Oct 2026). UTC also keeps the farm's log and run folders in order.
    os.environ["TZ"] = "UTC"
    time.tzset()
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--config", default=os.path.join(FARM_DIR, "farm.toml"))
    args = parser.parse_args()
    cfg = load_config(args.config)
    os.makedirs(APK_DIR, exist_ok=True)
    os.makedirs(RUNS_DIR, exist_ok=True)
    db = Db(DB_PATH)

    def handle_signal(*_):
        log(None, "stopping...")
        stop_event.set()
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    build_helper()
    os.makedirs(EXTRACT_DIR, exist_ok=True)
    sync_extract(cfg)  # before any phone starts, so the first cycle has it
    threading.Thread(target=release_watcher, args=(cfg, db), daemon=True).start()
    usb_proxy.start()  # network over USB for in-app map downloads
    workers = {}
    last_prune = 0
    last_vitals = 0
    while not stop_event.is_set():
        for serial in connected_devices(cfg["include_emulators"]):
            if serial not in workers or not workers[serial].is_alive():
                workers[serial] = Worker(serial, cfg, db)
                workers[serial].start()
        for udid in ios_devices() if cfg["include_ios"] else []:
            if udid not in workers or not workers[udid].is_alive():
                workers[udid] = IosWorker(udid, cfg, db)
                workers[udid].start()
        if time.time() - last_vitals >= cfg["battery_poll_minutes"] * 60:
            last_vitals = time.time()
            for serial, worker in list(workers.items()):
                if isinstance(worker, Worker) and worker.is_alive():
                    record_vitals(db, serial)
        write_report(db)
        if time.time() - last_prune > 86400:
            prune_runs(cfg["keep_days"])
            db.execute("DELETE FROM vitals WHERE ts<?", (time.time() - cfg["keep_days"] * 86400,))
            last_prune = time.time()
        stop_event.wait(30)

    stop_children()
    for serial, worker in workers.items():
        if isinstance(worker, IosWorker):
            worker.stop_processes()
        else:
            disable_mock_location(serial)
            # Give the phone its sensors and its notification shade back.
            if SENSORS_OFF in shell(serial, SENSOR_MODE):
                shell(serial, "dumpsys sensorservice enable > /dev/null")
            shell(serial, "cmd statusbar send-disable-flag none")
    write_report(db)


if __name__ == "__main__":
    main()
