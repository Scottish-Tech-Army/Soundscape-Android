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
RUNS_DIR = os.path.join(FARM_DIR, "runs")
DB_PATH = os.path.join(FARM_DIR, "farm.db")
REPORT_PATH = os.path.join(FARM_DIR, "report.html")

PACKAGE = "org.scottishtecharmy.soundscape"
MOCK_PROVIDERS = ("gps", "network")

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
    "max_battery_temp_c": 42,
    "replay_speed_mps": 1.4,
    "default_location": [55.9474, -4.3058],
    # Held while the Maestro suite runs: the flows assert distance-sorted list
    # order, so they need the fixed point CI pins with `adb emu geo fix`.
    "suite_location": [55.955360, -3.223538],
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


def run(cmd, timeout=120):
    try:
        return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout,
                              errors="replace")
    except subprocess.TimeoutExpired as e:
        return subprocess.CompletedProcess(cmd, -1, e.stdout or "", f"timeout after {timeout}s")


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
        stop_event.wait(cfg["poll_minutes"] * 60)


# --------------------------------------------------------------------------- location


def load_track(path):
    root = ET.parse(path).getroot()
    points = [(float(p.get("lat")), float(p.get("lon")))
              for p in root.iter() if p.tag.split("}")[-1] in ("trkpt", "rtept")]
    if len(points) < 2:
        raise ValueError(f"{path}: need at least 2 track points")
    return points


def haversine(a, b):
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = (math.sin((lat2 - lat1) / 2) ** 2
         + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2)
    return 2 * 6371000 * math.asin(math.sqrt(h))


class LocationFeeder(threading.Thread):
    """Feeds mock fixes to the phone once a second over adb.

    Walks the GPX track at a constant speed and turns round at each end rather
    than jumping back to the start. With no track, holds one fixed point.
    Setting `hold` to a (lat, lon) pins the phone there until it's set to None.
    """

    def __init__(self, serial, gpx, fixed, speed, db, alive):
        super().__init__(daemon=True)
        self.serial, self.speed, self.db, self.alive = serial, speed, db, alive
        self.hold = None
        self.points = load_track(gpx) if gpx else [tuple(fixed), tuple(fixed)]
        self.cumulative = [0.0]
        for a, b in zip(self.points, self.points[1:]):
            self.cumulative.append(self.cumulative[-1] + haversine(a, b))

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

    def run(self):
        start = time.time()
        while not stop_event.is_set() and self.alive.is_set():
            lat, lon = self.hold or self.position(time.time() - start)
            cmd = "; ".join(
                f"cmd location providers set-test-provider-location {p} "
                f"--location {lat:.7f},{lon:.7f} --accuracy 5" for p in MOCK_PROVIDERS)
            out = shell(self.serial, cmd, timeout=10)
            if "Exception" in out:
                self.db.event(self.serial, "error", None, f"mock location rejected:\n{out}")
                return
            time.sleep(1)


def enable_mock_location(serial):
    cmds = ["appops set com.android.shell android:mock_location allow"]
    for p in MOCK_PROVIDERS:
        cmds += [f"cmd location providers add-test-provider {p}",
                 f"cmd location providers set-test-provider-enabled {p} true"]
    return shell(serial, "; ".join(cmds))


def disable_mock_location(serial):
    shell(serial, "; ".join(f"cmd location providers remove-test-provider {p}"
                             for p in MOCK_PROVIDERS), timeout=20)


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
        if "Exception" in out:
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
            try:
                with open(report) as f:
                    output += f.read()
            except OSError:
                pass
            if res.returncode == 0 or not MAESTRO_RETRY_RE.search(output) or attempt:
                break
            log(self.serial, f"{name}: transient launch failure, retrying once")
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
            <td>{e(st.get('android', ''))}</td><td>{e(versions.get(ver, str(ver or '')))}</td>
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
<h2>Devices</h2><div class=wrap><table><tr><th>Serial</th><th>Model</th><th>Android</th><th>App</th>
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

    threading.Thread(target=release_watcher, args=(cfg, db), daemon=True).start()
    workers = {}
    last_prune = 0
    while not stop_event.is_set():
        for serial in connected_devices(cfg["include_emulators"]):
            if serial not in workers or not workers[serial].is_alive():
                workers[serial] = Worker(serial, cfg, db)
                workers[serial].start()
        write_report(db)
        if time.time() - last_prune > 86400:
            prune_runs(cfg["keep_days"])
            last_prune = time.time()
        stop_event.wait(30)

    for serial in workers:
        disable_mock_location(serial)
    write_report(db)


if __name__ == "__main__":
    main()
