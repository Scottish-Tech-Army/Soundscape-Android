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
import contextlib
import datetime as dt
import glob
import gzip
import json
import html
import math
import os
import re
import signal
import sqlite3
import subprocess
import sys
import threading
import time
import tomllib
import xml.etree.ElementTree as ET

FARM_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_DIR = os.path.dirname(FARM_DIR)
MAESTRO_DIR = os.path.join(REPO_DIR, "maestro")
POST_UPGRADE_FLOW = os.path.join(FARM_DIR, "flows", "PostUpgrade.yaml")
APK_DIR = os.path.join(FARM_DIR, "apks")
EXTRACT_DIR = os.path.join(FARM_DIR, "extracts")
OFFLINE_SEARCH_FLOW = os.path.join(FARM_DIR, "flows", "OfflineSearch.yaml")
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
    "monkey_throttle_ms": 250,
    "flow_timeout_minutes": 15,
    "keep_days": 14,
    "serialize_maestro": False,
    "allow_reinstall": False,
    "include_emulators": False,
    # iPhones (via pymobiledevice3) are observed, not driven: see IosWorker.
    "include_ios": True,
    "ios_poll_minutes": 10,
    "max_battery_temp_c": 42,
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
    "default_gpx": "",
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
            CREATE TABLE IF NOT EXISTS events(
                id INTEGER PRIMARY KEY, serial TEXT, ts REAL, kind TEXT,
                version_code INTEGER, detail TEXT);
        """)

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


def run(cmd, timeout=120):
    """subprocess.run, except that the farm can stop the process when it stops.
    A Maestro run left behind by a stopped farm keeps hold of the phone's
    driver, and every flow after a restart then fails with gRPC UNAVAILABLE."""
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, errors="replace")
    with children_lock:
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
        dl = run(["curl", "-sfL", "-o", part, f"{base.rstrip('/')}/{filename}"], timeout=3600)
        if dl.returncode != 0:
            log(None, f"offline map download failed: {dl.stderr.strip()}")
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
    than jumping back to the start. With no track, holds one fixed point."""

    def __init__(self, gpx, fixed, speed):
        self.speed = speed
        self.points = load_track(gpx) if gpx else [tuple(fixed), tuple(fixed)]
        self.cumulative = [0.0]
        for a, b in zip(self.points, self.points[1:]):
            self.cumulative.append(self.cumulative[-1] + haversine(a, b))

    def round_trip_seconds(self):
        return max(60, int(2 * self.cumulative[-1] / self.speed))

    def position(self, t):
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
        return (*here, self.track.speed, initial_bearing(here, ahead))

    def run(self):
        start = time.time()
        failures = 0
        while not stop_event.is_set() and self.alive.is_set():
            lat, lon, speed, bearing = self.fix(time.time() - start)
            extras = f"--es lat {lat:.7f} --es lon {lon:.7f} --es speed {speed:.2f}"
            if bearing is not None:
                extras += f" --es bearing {bearing:.1f}"
            out = shell(self.serial, f"am broadcast -n {HELPER_PKG}/.FixReceiver "
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
        shell(serial, f"am broadcast -n {HELPER_PKG}/.FixReceiver -a {HELPER_PKG}.STOP",
              timeout=20)


# --------------------------------------------------------------------------- crashes


DROPBOX_TAGS = ("data_app_crash", "data_app_anr", "data_app_native_crash", "SYSTEM_TOMBSTONE")


def collect_dropbox(serial, since):
    """Returns (kind, text) for every crash/ANR of our app logged at or after `since`.

    `since` is a device-local "YYYY-MM-DD HH:MM:SS" string, which is also the
    format dropbox prints entries with, so plain string comparison works.
    """
    out = shell(serial, "dumpsys dropbox --print " + " ".join(DROPBOX_TAGS), timeout=60)
    found = []
    for entry in re.split(r"^={10,}\s*$", out, flags=re.M):
        m = re.search(r"^(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d) (\S+)", entry.strip(), re.M)
        if m and m.group(1) >= since and PACKAGE in entry:
            kind = "anr" if "anr" in m.group(2) else "crash"
            found.append((kind, entry.strip()))
    return found


def pss_kb(serial):
    out = shell(serial, f"dumpsys meminfo {PACKAGE}", timeout=60)
    m = re.search(r"TOTAL PSS:\s+(\d+)", out) or re.search(r"^\s*TOTAL\s+(\d+)", out, re.M)
    return int(m.group(1)) if m else None


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

    # ----- setup

    def prepare(self):
        s = self.serial
        shell(s, "svc power stayon usb")
        # MainActivity uses dummy analytics when this is set, which keeps the
        # farm's traffic out of the production Firebase numbers.
        shell(s, "settings put system firebase.test.lab true")
        adb(s, "logcat", "-G", "16M")
        self.quieten()
        out = enable_mock_location(s)
        if "Exception" in out or "failed" in out:
            self.db.event(s, "error", None, f"could not enable mock location:\n{out}")
        else:
            gpx = self.dev_cfg.get("gpx", self.cfg["default_gpx"])
            self.feeder = LocationFeeder(s, gpx, self.cfg["default_location"],
                                         self.cfg["replay_speed_mps"], self.db, self.alive)
            self.feeder.start()

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
        extracts directory, named as CI does (without the build-date prefix)."""
        if not current_extract:
            self.db.event(self.serial, "error", None, "no offline map cached; using the network")
            return
        set_state(self.serial, "pushing offline map")
        name = re.sub(r"^\d{8}-\d{4}-", "", os.path.basename(current_extract))
        shell(self.serial, f"rm -f {DEVICE_EXTRACT_DIR}/*.pmtiles {DEVICE_EXTRACT_DIR}/*.geojson; "
                           f"mkdir -p {DEVICE_EXTRACT_DIR}")
        for src, dest in ((current_extract, name), (current_extract + ".geojson", name + ".geojson")):
            res = adb(self.serial, "push", src, f"{DEVICE_EXTRACT_DIR}/{dest}", timeout=900)
            if res.returncode != 0:
                self.db.event(self.serial, "error", None,
                              f"offline map push failed:\n{res.stdout}{res.stderr}")
                return

    def repair(self):
        """Undoes things the monkey can do from the notification shade."""
        shell(self.serial, "cmd connectivity airplane-mode disable; svc wifi enable; "
                           "cmd statusbar collapse; input keyevent KEYCODE_WAKEUP")
        self.quieten()

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

    def ensure_version(self, cycle_dir):
        """Installs or upgrades as needed. Returns the version to test, or None."""
        builds = self.db.builds()
        if not builds:
            return None
        latest = builds[-1]
        current = installed_version(self.serial)
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

    def run_flow(self, cycle_id, path, cycle_dir, kind="flow", note=""):
        name = os.path.splitext(os.path.basename(path))[0]
        out_dir = os.path.join(cycle_dir, name)
        os.makedirs(out_dir, exist_ok=True)
        report = os.path.join(out_dir, "report.xml")
        cmd = [os.path.expanduser("~/.maestro/bin/maestro"), "test", "--device", self.serial,
               "--format", "junit", "--output", report, "--test-output-dir", out_dir,
               "--debug-output", os.path.join(out_dir, "debug"), "--flatten-debug-output",
               "--no-ansi", path]
        timeout = self.cfg["flow_timeout_minutes"] * 60
        start = time.time()
        for attempt in range(2):
            set_state(self.serial, f"running {name}")
            if self.cfg["serialize_maestro"]:
                with maestro_lock:
                    res = run(cmd, timeout=timeout)
            else:
                res = run(cmd, timeout=timeout)
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
            if "gRPC" in output:
                # Maestro's on-device driver is unreachable (seen after a phone
                # sat on its lock screen); a fresh install brings it back.
                cmd = cmd[:2] + ["--reinstall-driver"] + cmd[2:]
        with open(os.path.join(out_dir, "maestro.log"), "w") as f:
            f.write(res.stdout + res.stderr)
        status = "pass" if res.returncode == 0 else "fail"
        detail = "\n".join(filter(None, [note, "" if status == "pass" else
                                         (res.stderr or res.stdout)[-2000:]]))
        self.db.execute(
            "INSERT INTO results(cycle_id, serial, ts, kind, name, status, duration_s, detail, dir) "
            "VALUES(?,?,?,?,?,?,?,?,?)",
            (cycle_id, self.serial, start, kind, name, status, time.time() - start, detail, out_dir))
        return status == "pass"

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

    def cycle(self):
        s = self.serial
        level, temp = battery(s)
        if temp and temp > self.cfg["max_battery_temp_c"]:
            set_state(s, f"cooling down ({temp}°C)")
            stop_event.wait(600)
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

        # Monkey first: Onboarding wipes whatever it leaves behind, and the suite
        # then ends with the markers and route that PostUpgrade checks for.
        self.run_monkey(cycle_id, cycle_dir)
        passed = failed = 0
        with self.suite_location():
            for flow in suite_flows():
                if stop_event.is_set():
                    break
                if self.run_flow(cycle_id, os.path.join(MAESTRO_DIR, flow), cycle_dir):
                    passed += 1
                else:
                    failed += 1
                if flow == "Onboarding.yaml":
                    # Onboarding cleared the app's storage and settings, so put the
                    # offline map back and switch search to offline.
                    self.push_extract()
                    if self.run_flow(cycle_id, OFFLINE_SEARCH_FLOW, cycle_dir, kind="setup"):
                        passed += 1
                    else:
                        failed += 1

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
        self.db.execute(
            "UPDATE cycles SET ended=?, status=?, flows_passed=?, flows_failed=?, crashes=?, anrs=?, "
            "pss_kb=?, battery_level=?, battery_temp=? WHERE id=?",
            (time.time(), status, passed, failed, crashes, anrs, pss_kb(s), level, temp, cycle_id))
        set_state(s, f"cycle {status}: {passed} passed, {failed} failed, "
                     f"{crashes} crashes, {anrs} ANRs")

    def run(self):
        set_state(self.serial, "preparing", model=self.model, android=self.android)
        try:
            self.prepare()
            while not stop_event.is_set():
                if adb(self.serial, "get-state", timeout=15).stdout.strip() != "device":
                    break
                if self.dev_cfg.get("skip"):
                    set_state(self.serial, "skipped in config")
                    stop_event.wait(300)
                    continue
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


def fmt_time(ts):
    return dt.datetime.fromtimestamp(ts).strftime("%Y-%m-%d %H:%M") if ts else ""


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
            <td>{last['pss_kb'] // 1024 if last and last['pss_kb'] else ''} MB</td>
            <td>{last['battery_level'] if last else ''}% / {last['battery_temp'] if last else ''}°C</td></tr>""")
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
.wrap{{overflow-x:auto}}</style></head><body>
<h1>Soundscape test farm</h1><p class=dim>Updated {fmt_time(time.time())}. Cached releases: {builds or 'none yet'}</p>
<h2>Devices</h2><div class=wrap><table><tr><th>Serial</th><th>Model</th><th>OS</th><th>App</th>
<th>Now</th><th>Last cycle</th><th>Passed 24h</th><th>Crashes 24h</th><th>ANRs 24h</th>
<th>PSS</th><th>Battery</th></tr>{''.join(rows)}</table></div>
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
    for path in glob.glob(os.path.join(RUNS_DIR, "*", "*")):
        if os.path.getmtime(path) < cutoff:
            subprocess.run(["rm", "-rf", path])


# --------------------------------------------------------------------------- main


def main():
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
    workers = {}
    last_prune = 0
    while not stop_event.is_set():
        for serial in connected_devices(cfg["include_emulators"]):
            if serial not in workers or not workers[serial].is_alive():
                workers[serial] = Worker(serial, cfg, db)
                workers[serial].start()
        for udid in ios_devices() if cfg["include_ios"] else []:
            if udid not in workers or not workers[udid].is_alive():
                workers[udid] = IosWorker(udid, cfg, db)
                workers[udid].start()
        write_report(db)
        if time.time() - last_prune > 86400:
            prune_runs(cfg["keep_days"])
            last_prune = time.time()
        stop_event.wait(30)

    stop_children()
    for serial, worker in workers.items():
        if isinstance(worker, IosWorker):
            worker.stop_processes()
        else:
            disable_mock_location(serial)
    write_report(db)


if __name__ == "__main__":
    main()
