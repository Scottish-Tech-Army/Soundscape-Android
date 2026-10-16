"""Checks farm.py without any phones: run-tests runs these on every PR.

    python3 -m unittest discover -s test-farm -v

They cover what can break without a device: the config, the wiring between
the farm's flows and maestro/, the settings the farm shares with CI's Maestro
run, and the parsers that read adb output.
"""

import glob
import os
import re
import tempfile
import tomllib
import unittest

import farm

MAESTRO_WORKFLOW = os.path.join(farm.REPO_DIR, ".github", "workflows", "run-maestro-tests.yaml")
FLOW_REF_RE = re.compile(r"^\s*(?:-\s*runFlow:|file:)\s*(\S+\.yaml)\s*$", re.M)


class FakeShell:
    """Stands in for farm.shell: answers each command from `replies`, keyed by a
    substring of the command, and records the commands it was given."""

    def __init__(self, replies):
        self.replies = replies
        self.commands = []

    def __call__(self, serial, command, timeout=120):
        self.commands.append(command)
        for key, reply in self.replies.items():
            if key in command:
                return reply
        return ""


class ShellTestCase(unittest.TestCase):
    def fake_shell(self, replies):
        fake = FakeShell(replies)
        original = farm.shell
        farm.shell = fake
        self.addCleanup(setattr, farm, "shell", original)
        return fake


class ConfigTest(unittest.TestCase):
    EXAMPLE = os.path.join(farm.FARM_DIR, "farm.toml.example")

    def test_example_loads(self):
        cfg = farm.load_config(self.EXAMPLE)
        self.assertEqual(cfg["extract_name"], "Glasgow")

    def test_example_only_uses_known_settings(self):
        with open(self.EXAMPLE, "rb") as f:
            keys = set(tomllib.load(f))
        self.assertEqual(keys - set(farm.DEFAULTS), set())

    def test_gpx_paths_are_relative_to_the_config(self):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "farm.toml")
            with open(path, "w") as f:
                f.write('default_gpx = "a.gpx"\n[devices.X]\ngpx = "../b.gpx"\n')
            cfg = farm.load_config(path)
        self.assertEqual(cfg["default_gpx"], os.path.join(d, "a.gpx"))
        self.assertEqual(cfg["devices"]["X"]["gpx"], os.path.join(os.path.dirname(d), "b.gpx"))


class FlowWiringTest(unittest.TestCase):
    def test_suite_flows_exist(self):
        flows = farm.suite_flows()
        self.assertTrue(flows, "no runFlow lines found in maestro/Master.yaml")
        for flow in flows:
            self.assertTrue(os.path.isfile(os.path.join(farm.MAESTRO_DIR, flow)), flow)

    def test_roam_flows_exist(self):
        self.assertTrue(farm.roam_flows())

    def test_farm_flows_exist(self):
        for path in (farm.POST_UPGRADE_FLOW, farm.FARM_SETTINGS_FLOW, farm.DOWNLOAD_EXTRACT_FLOW,
                     farm.SHOW_MAP_FLOW, farm.RESTORE_FLOW):
            self.assertTrue(os.path.isfile(path), path)

    def test_flow_references_resolve(self):
        # Roam flows sit a level deeper than the others, so their paths into
        # maestro/ need one more "../".
        flows = glob.glob(os.path.join(farm.FARM_DIR, "flows", "**", "*.yaml"), recursive=True)
        self.assertTrue(flows)
        for flow in flows:
            with open(flow) as f:
                refs = FLOW_REF_RE.findall(f.read())
            for ref in refs:
                target = os.path.normpath(os.path.join(os.path.dirname(flow), ref))
                with self.subTest(flow=os.path.relpath(flow, farm.FARM_DIR), ref=ref):
                    self.assertTrue(os.path.isfile(target))


class CiDriftTest(unittest.TestCase):
    """The farm repeats two things from CI's Maestro run, which must agree."""

    @classmethod
    def setUpClass(cls):
        with open(MAESTRO_WORKFLOW) as f:
            cls.workflow = f.read()

    def test_suite_location_matches_ci_geo_fix(self):
        # The suite's flows assert distance-sorted lists, so the farm has to
        # hold the phone at the point CI pins the emulator to.
        m = re.search(r"adb emu geo fix (-?[\d.]+) (-?[\d.]+)", self.workflow)
        self.assertIsNotNone(m)
        lon, lat = float(m.group(1)), float(m.group(2))
        self.assertEqual([lat, lon], farm.DEFAULTS["suite_location"])

    def test_retry_regex_covers_ci_retries(self):
        m = re.search(r'grep -Eq "([^"]+)" "maestro_outputs', self.workflow)
        self.assertIsNotNone(m)
        for message in m.group(1).split("|"):
            with self.subTest(message=message):
                self.assertRegex(message, farm.MAESTRO_RETRY_RE)


class DropboxTest(ShellTestCase):
    @staticmethod
    def entry(stamp, tag, body):
        return f"========================================\n{stamp} {tag} (text, 100 bytes)\n{body}\n"

    def test_collects_our_crashes_and_anrs_since(self):
        pkg = farm.PACKAGE
        self.fake_shell({
            "data_app_crash": self.entry("2026-10-01 09:00:00", "data_app_crash", f"Process: {pkg}")
                              + self.entry("2026-10-02 10:00:00", "data_app_crash", f"Process: {pkg}")
                              + self.entry("2026-10-02 11:00:00", "data_app_crash", "Process: com.other"),
            "data_app_anr": self.entry("2026-10-02 12:00:00", "data_app_anr", f"Process: {pkg}"),
        })
        found = farm.collect_dropbox("S", "2026-10-02 00:00:00")
        self.assertEqual([kind for kind, _ in found], ["crash", "anr"])
        self.assertIn("2026-10-02 10:00:00", found[0][1])

    def test_asks_for_one_tag_per_call(self):
        # dumpsys dropbox only prints entries matching every argument, so
        # several tags in one call print nothing.
        fake = self.fake_shell({})
        farm.collect_dropbox("S", "2026-10-02 00:00:00")
        self.assertEqual(fake.commands, [f"dumpsys dropbox --print {tag}" for tag in farm.DROPBOX_TAGS])


class DeviceReadingsTest(ShellTestCase):
    def test_app_uptime(self):
        for etime, seconds in (("03:04", 184), ("02:03:04", 7384), ("1-02:03:04", 93784)):
            with self.subTest(etime=etime):
                self.fake_shell({"pidof": "1234\n", "ps -o": f"  {etime}\n"})
                self.assertEqual(farm.app_uptime_s("S"), seconds)

    def test_app_uptime_not_running(self):
        self.fake_shell({"pidof": ""})
        self.assertIsNone(farm.app_uptime_s("S"))

    def test_pss(self):
        self.fake_shell({"meminfo": "  TOTAL PSS:   123456   TOTAL RSS: 200000"})
        self.assertEqual(farm.pss_kb("S"), 123456)
        self.fake_shell({"meminfo": "  Native Heap  1000\n        TOTAL    98765    1234"})
        self.assertEqual(farm.pss_kb("S"), 98765)

    def test_battery(self):
        self.fake_shell({"battery": "  level: 80\n  temperature: 315\n"})
        self.assertEqual(farm.battery("S"), (80, 31.5))
        self.fake_shell({"battery": ""})
        self.assertEqual(farm.battery("S"), (None, None))


class TrackTest(unittest.TestCase):
    START = (55.9474, -4.3058)

    def write_gpx(self, points):
        f = tempfile.NamedTemporaryFile("w", suffix=".gpx", delete=False)
        self.addCleanup(os.unlink, f.name)
        pts = "".join(f'<trkpt lat="{lat}" lon="{lon}"/>' for lat, lon in points)
        f.write('<gpx xmlns="http://www.topografix.com/GPX/1/1"><trk><trkseg>'
                f"{pts}</trkseg></trk></gpx>")
        f.close()
        return f.name

    def north(self, metres):
        return self.START[0] + metres / 111_195, self.START[1]

    def test_jitter_is_dropped(self):
        jitter = [self.north(m) for m in (0, 3, 1, 5, 2)]
        points = farm.load_track(self.write_gpx(jitter + [self.north(100), self.north(200)]))
        self.assertEqual(len(points), 3)

    def test_too_short_track_is_rejected(self):
        with self.assertRaises(ValueError):
            farm.load_track(self.write_gpx([self.north(0), self.north(5)]))

    def test_walks_to_the_end_and_back(self):
        track = farm.Track(self.write_gpx([self.north(0), self.north(100), self.north(200)]),
                           None, speed=2.0)
        total = track.cumulative[-1]
        self.assertAlmostEqual(total, 200, delta=0.5)
        self.assertEqual(track.position(0), self.START)
        end = track.position(total / 2.0)
        self.assertAlmostEqual(end[0], self.north(200)[0], places=6)
        # A quarter of the way back is three quarters of the way along.
        back = track.position(total * 1.25 / 2.0)
        self.assertAlmostEqual(back[0], self.north(150)[0], places=5)
        self.assertAlmostEqual(track.position(total / 1.0)[0], self.START[0], places=6)

    def test_fixed_point(self):
        track = farm.Track("", self.START, speed=1.4)
        for t in (0, 10, 1000):
            self.assertEqual(track.position(t), self.START)


class TimedTrackTest(unittest.TestCase):
    START = (55.8642, -4.2518)

    def write_gpx(self, points, keyword=farm.TIMED_TRACK_KEYWORD):
        f = tempfile.NamedTemporaryFile("w", suffix=".gpx", delete=False)
        self.addCleanup(os.unlink, f.name)
        pts = "".join(f'<trkpt lat="{lat}" lon="{lon}"><time>2026-01-01T09:00:{s:02d}Z</time></trkpt>'
                      for s, (lat, lon) in points)
        f.write('<gpx xmlns="http://www.topografix.com/GPX/1/1">'
                f"<metadata><keywords>{keyword}</keywords></metadata>"
                f"<trk><trkseg>{pts}</trkseg></trk></gpx>")
        f.close()
        return f.name

    def north(self, metres):
        return self.START[0] + metres / 111_195, self.START[1]

    def track(self):
        # Stands still for 10 s, then moves 100 m north in 10 s.
        gpx = self.write_gpx([(0, self.START), (10, self.START), (20, self.north(100))])
        return farm.Track(gpx, None, speed=1.4)

    def test_stands_still_then_moves(self):
        track = self.track()
        self.assertEqual(track.position(3), self.START)
        self.assertEqual(track.position(3), track.position(4))
        self.assertAlmostEqual(track.position(15)[0], self.north(50)[0], places=7)
        here, ahead = track.position(12), track.position(13)
        self.assertAlmostEqual(track.speed_between(here, ahead), 10, delta=0.1)

    def test_plays_backwards_after_the_end(self):
        track = self.track()
        self.assertAlmostEqual(track.position(20)[0], self.north(100)[0], places=7)
        self.assertAlmostEqual(track.position(25)[0], self.north(50)[0], places=7)
        self.assertEqual(track.position(35), self.START)
        self.assertEqual(track.round_trip_seconds(), 60)

    def test_unmarked_recording_keeps_constant_speed(self):
        gpx = self.write_gpx([(0, self.START), (10, self.north(50)), (20, self.north(100))], keyword="")
        track = farm.Track(gpx, None, speed=2.0)
        self.assertIsNone(track.times)
        self.assertEqual(track.speed_between(track.position(0), track.position(1)), 2.0)

    def test_committed_track(self):
        # The default track exists, replays by time, and never jumps further in
        # a second than a train travels.
        track = farm.Track(farm.DEFAULTS["default_gpx"], None, speed=1.4)
        self.assertIsNotNone(track.times)
        self.assertGreater(track.times[-1], 3600)
        steps = [farm.haversine(a, b) for a, b in zip(track.points, track.points[1:])]
        self.assertLess(max(steps), 30)


if __name__ == "__main__":
    unittest.main()
