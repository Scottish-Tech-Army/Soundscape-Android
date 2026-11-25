#!/usr/bin/env python3
"""Replay a Soundscape GPX recording (e.g. travel.gpx emailed in by a user) and show the
callouts on a map.

  python3 -I scripts/replay-gpx.py ~/Downloads/travel.gpx

1. reads the track and picks the offline extract(s) covering it from the extract provider's
   manifest (extractProviderUrl in local.properties, or the EXTRACT_PROVIDER_URL env var);
2. downloads them once into ~/.cache/soundscape-replay/extracts (keyed by build, so a new
   extract build is a new file and old ones can be deleted by hand);
3. runs MvtTileTest.replayExternalGpx, the same production-equivalent replay the GPX unit tests
   use, which writes app/build/test-output/replay/<name>.geojson and <name>.txt;
4. writes <name>.html next to them and opens it: a self-contained map page with the replay
   embedded, over a base map drawn from the same extract (see replay_basemap.py).

Nothing is uploaded anywhere: the GPX and the output stay on this machine, and the page
fetches nothing when it is opened.
"""

import argparse
import gzip
import html
import importlib.util
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
LOCAL_PROPERTIES = REPO / "local.properties"
MANIFEST = "manifest.geojson.gz"
CACHE = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "soundscape-replay"
OUTPUT = REPO / "app/build/test-output/replay"
VIEWER_TEMPLATE = REPO / "scripts/replay-viewer.html"
MANIFEST_MAX_AGE = 24 * 3600
# Track points closer together than this add nothing to choosing extracts
SAMPLE_SPACING_M = 100


def provider_url():
    """The extract provider isn't public, so it is never written into the repo."""
    url = os.environ.get("EXTRACT_PROVIDER_URL", "").strip()
    if not url and LOCAL_PROPERTIES.exists():
        for line in LOCAL_PROPERTIES.read_text().splitlines():
            if line.startswith("extractProviderUrl="):
                url = line.split("=", 1)[1].strip()
    if not url:
        sys.exit("No extract provider: set extractProviderUrl in local.properties or "
                 "EXTRACT_PROVIDER_URL, or pass --extract with local .pmtiles files")
    return url if url.endswith("/") else url + "/"


def curl_download(url, dest, expected_size=None, attempts=12):
    """Download with curl, retrying while the worker builds the extract on demand (503)."""
    tmp = dest.with_name(dest.name + ".part")
    for attempt in range(1, attempts + 1):
        result = subprocess.run(
            ["curl", "-sS", "-L", "-C", "-", "--retry", "3", "-o", str(tmp),
             "-w", "%{http_code}", url],
            capture_output=True, text=True)
        code = result.stdout.strip()
        if result.returncode == 0 and code in ("200", "206"):
            break
        if code == "416" and expected_size and tmp.exists() and tmp.stat().st_size == expected_size:
            break  # already complete from an earlier attempt
        if code == "503":
            print(f"    503 (worker building the extract), retry {attempt}/{attempts} in 60s",
                  flush=True)
            if tmp.exists() and tmp.stat().st_size < 10_000:
                tmp.unlink()  # the 503 body, not tile data
            time.sleep(60)
            continue
        sys.exit(f"Download failed: {url} http={code} {result.stderr.strip()}")
    else:
        sys.exit(f"Gave up after {attempts} attempts: {url}")
    size = tmp.stat().st_size
    if expected_size is not None and size != expected_size:
        sys.exit(f"Size mismatch for {url}: got {size}, manifest says {expected_size}")
    tmp.rename(dest)
    return size


def read_track(path):
    """Every <trkpt> (and <rtept>, for exported routes) as (lon, lat)."""
    points = []
    for el in ET.parse(path).getroot().iter():
        tag = el.tag.rsplit("}", 1)[-1]
        if tag in ("trkpt", "rtept"):
            try:
                points.append((float(el.get("lon")), float(el.get("lat"))))
            except (TypeError, ValueError):
                pass
    return points


def distance_m(a, b):
    lat = math.radians((a[1] + b[1]) / 2)
    dx = math.radians(b[0] - a[0]) * math.cos(lat)
    dy = math.radians(b[1] - a[1])
    return 6371000 * math.hypot(dx, dy)


def sample(points):
    out = []
    for p in points:
        if not out or distance_m(out[-1], p) >= SAMPLE_SPACING_M:
            out.append(p)
    if points and out[-1] != points[-1]:
        out.append(points[-1])
    return out


def in_ring(p, ring):
    x, y = p
    inside = False
    j = len(ring) - 1
    for i in range(len(ring)):
        xi, yi = ring[i][0], ring[i][1]
        xj, yj = ring[j][0], ring[j][1]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def polygons(geometry):
    if geometry["type"] == "Polygon":
        return [geometry["coordinates"]]
    if geometry["type"] == "MultiPolygon":
        return geometry["coordinates"]
    return []


def in_geometry(p, polys):
    for rings in polys:
        if in_ring(p, rings[0]) and not any(in_ring(p, hole) for hole in rings[1:]):
            return True
    return False


def bbox(polys):
    xs = [c[0] for rings in polys for c in rings[0]]
    ys = [c[1] for rings in polys for c in rings[0]]
    return min(xs), min(ys), max(xs), max(ys)


def load_manifest(provider):
    CACHE.mkdir(parents=True, exist_ok=True)
    path = CACHE / MANIFEST
    if not path.exists() or time.time() - path.stat().st_mtime > MANIFEST_MAX_AGE:
        print(f"Fetching manifest from {provider}", flush=True)
        if path.exists():
            path.unlink()
        curl_download(provider + MANIFEST, path)
    extracts = []
    for feature in json.load(gzip.open(path))["features"]:
        props = feature["properties"]
        polys = polygons(feature["geometry"])
        if not props.get("filename") or not polys:
            continue
        extracts.append({"name": props.get("name", props["filename"]),
                         "filename": props["filename"],
                         "size": props.get("extract-size") or 0,
                         "polys": polys, "bbox": bbox(polys)})
    return extracts


def choose_extracts(points, extracts):
    """The smallest download that covers the track: the smallest single extract covering every
    sampled point, or a greedy set of extracts if that comes out smaller (a walk crossing the
    edge of two cities shouldn't pull in a whole country)."""
    samples = sample(points)
    covers = {}
    for i, e in enumerate(extracts):
        x0, y0, x1, y1 = e["bbox"]
        hit = frozenset(j for j, p in enumerate(samples)
                        if x0 <= p[0] <= x1 and y0 <= p[1] <= y1 and in_geometry(p, e["polys"]))
        if hit:
            covers[i] = hit
    reachable = frozenset().union(*covers.values()) if covers else frozenset()
    missed = len(samples) - len(reachable)

    single = [i for i, hit in covers.items() if hit == reachable]
    best_single = min(single, key=lambda i: extracts[i]["size"]) if single else None

    # Weighted set cover: each step takes the extract with the fewest bytes per point it newly
    # covers. Taking the one that covers most would always pick the country for a long drive.
    greedy, left = [], set(reachable)
    while left:
        i = min((k for k in covers if covers[k] & left),
                key=lambda k: extracts[k]["size"] / len(covers[k] & left))
        greedy.append(i)
        left -= covers[i]

    chosen = greedy
    if best_single is not None and \
            extracts[best_single]["size"] <= sum(extracts[i]["size"] for i in greedy):
        chosen = [best_single]
    return [extracts[i] for i in chosen], missed, len(samples)


def fetch(provider, extract):
    folder = CACHE / "extracts"
    folder.mkdir(parents=True, exist_ok=True)
    dest = folder / extract["filename"]
    if dest.exists() and (not extract["size"] or dest.stat().st_size == extract["size"]):
        print(f"  {extract['name']}: cached {dest.name}")
    else:
        print(f"  {extract['name']}: downloading {dest.name} ({extract['size'] / 1e6:,.0f} MB)",
              flush=True)
        curl_download(provider + extract["filename"], dest, extract["size"] or None)
    return dest


def run_replay(gpx, extract_dir, start, end):
    env = dict(os.environ, REPLAY_GPX=str(gpx), REPLAY_EXTRACTS=str(extract_dir))
    if start is not None:
        env["REPLAY_START"] = str(start)
    if end is not None:
        env["REPLAY_END"] = str(end)
    cmd = [str(REPO / "gradlew"), "-q", ":app:testDebugUnitTest",
           "--tests", "org.scottishtecharmy.soundscape.MvtTileTest.replayExternalGpx", "--rerun"]
    print("Running the replay (" + " ".join(cmd[2:]) + ")", flush=True)
    result = subprocess.run(cmd, cwd=REPO, env=env)
    if result.returncode != 0:
        sys.exit("Replay failed - see the gradle output above")


def _basemap_module():
    # python3 -I leaves the script's own directory off sys.path, so load the module by path
    spec = importlib.util.spec_from_file_location("replay_basemap", REPO / "scripts/replay_basemap.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _json_for_html(value):
    # Inside <script type="application/json"> a "</script" or "<!--" would end or confuse the
    # element; "<" only ever appears inside JSON strings, where \u003c means the same thing
    return json.dumps(value, separators=(",", ":"), ensure_ascii=False).replace("<", "\\u003c")


def write_viewer(name, gpx_points, geojson_path, txt_path, extracts):
    basemap = _basemap_module()
    print("Building the map page", flush=True)
    data = {
        "name": name,
        "track": gpx_points,
        "geojson": json.loads(geojson_path.read_text()),
        "log": txt_path.read_text() if txt_path.exists() else "",
    }
    lib = basemap.maplibre(CACHE / "basemap")
    values = {
        "TITLE": html.escape(name),
        "MAPLIBRE_CSS": lib["maplibre-gl.css"],
        "MAPLIBRE_JS": lib["maplibre-gl.js"],
        "BASEMAP": _json_for_html(basemap.build(CACHE / "basemap", extracts, gpx_points)),
        "REPLAY_DATA": _json_for_html(data),
    }
    # One pass, so nothing inside an inserted value is ever taken for a placeholder
    page = re.sub(r"__(" + "|".join(values) + r")__", lambda m: values[m.group(1)],
                  VIEWER_TEMPLATE.read_text())
    out = geojson_path.with_suffix(".html")
    out.write_text(page)
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("gpx", help="the GPX recording")
    parser.add_argument("--extract", nargs="+", metavar="PMTILES",
                        help="replay against these local extracts instead of choosing from the "
                             "manifest")
    parser.add_argument("--start", type=int, help="first GPX point to replay")
    parser.add_argument("--end", type=int, help="replay up to this GPX point")
    parser.add_argument("--no-open", action="store_true", help="don't open the map page")
    args = parser.parse_args()

    gpx = Path(args.gpx).expanduser().resolve()
    if not gpx.is_file():
        sys.exit(f"No such file: {gpx}")
    points = read_track(gpx)
    if not points:
        sys.exit(f"No track points in {gpx}")
    print(f"{gpx.name}: {len(points)} points")

    with tempfile.TemporaryDirectory(prefix="soundscape-replay-") as tmp:
        extract_dir = Path(tmp)
        if args.extract:
            files = [Path(f).expanduser().resolve() for f in args.extract]
            for f in files:
                if not f.is_file():
                    sys.exit(f"No such extract: {f}")
        else:
            provider = provider_url()
            chosen, missed, total = choose_extracts(points, load_manifest(provider))
            if not chosen:
                sys.exit("No extract in the manifest covers this track")
            if missed:
                print(f"Warning: {missed} of {total} sampled points are outside every extract")
            print("Extracts:")
            files = [fetch(provider, e) for e in chosen]
        # The grid reads every .pmtiles in one directory, so give it just these
        for f in files:
            (extract_dir / f.name).symlink_to(f)
        name = gpx.stem
        geojson = OUTPUT / f"{name}.geojson"
        txt = OUTPUT / f"{name}.txt"
        # A skipped test still passes, so stale output must not look like a result
        for old in (geojson, txt):
            old.unlink(missing_ok=True)
        run_replay(gpx, extract_dir, args.start, args.end)

    if not geojson.exists():
        sys.exit(f"The replay didn't write {geojson} (was the test skipped?)")
    page = write_viewer(name, points, geojson, txt, files)
    print(f"\nCallouts: {txt}\nGeoJSON:  {geojson}\nMap:      {page}")
    if not args.no_open and shutil.which("xdg-open"):
        subprocess.Popen(["xdg-open", str(page)], stdout=subprocess.DEVNULL,
                         stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    main()
