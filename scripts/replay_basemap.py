"""Base map for the replay viewer (scripts/replay-gpx.py), built from the replay's own pmtiles
extract rather than a tile server.

Everything the map needs goes into the page itself, so it still opens from disk with nothing
fetched at view time:
  - the vector tiles around the track, read straight out of the extract;
  - the OpenMapTiles project's own "OSM OpenMapTiles" style, composed from the per-layer style
    snippets in its repo the way its `make build-style` does, with the icons as SVG (the page
    rasterises them, so no sprite build is needed);
  - the Noto Sans glyph ranges that the names in those tiles use, from the OpenMapTiles fonts
    release;
  - MapLibre GL JS itself.
The style, fonts and MapLibre are pinned and downloaded once into the cache.
"""

import base64
import gzip
import hashlib
import json
import math
import re
import struct
import subprocess
import sys
import tarfile
import zipfile
from pathlib import Path

OMT_TAG = "v3.16"
OMT_TARBALL = f"https://codeload.github.com/openmaptiles/openmaptiles/tar.gz/refs/tags/{OMT_TAG}"
FONTS_URL = "https://github.com/openmaptiles/fonts/releases/download/v2.0/noto-sans.zip"
FONTS_SHA256 = "d117316544b43a5dde7ee761b36e17701e9f85574e181d76a74814240fdbaf34"
MAPLIBRE_VERSION = "5.24.1"
MAPLIBRE = {
    "maplibre-gl.js": "a9c9e2aa36d4d5b63b131f27c1272f1a28883eeecc0eddd1e0dbfae4033bc7c9",
    "maplibre-gl.css": "ab1e70d59ec40465bae7e7030da2f3ccf28133fd502e62bd598eefbadfd7a732",
}
# Fonts in noto-sans.zip. The style asks for Open Sans first with Noto Sans as the fallback,
# which is what its own tileserver serves when Open Sans isn't installed.
FONTS = ("Noto Sans Regular", "Noto Sans Bold", "Noto Sans Italic")

# Every tile in the track's bounding box (plus this margin) up to DETAIL_ZOOM - 1, and from
# DETAIL_ZOOM up only the tiles along the track, so a long drive doesn't embed a whole county.
BBOX_MARGIN_M = 1000
DETAIL_ZOOM = 12
TRACK_SPACING_M = 50
# Glyph ranges are worked out from the labels in tiles from this zoom up
GLYPH_MIN_ZOOM = 10
# Characters MapLibre GL JS draws with the browser's own font (its localIdeographFontFamily,
# "sans-serif" by default) and so never asks for glyphs for: CJK, kana, Hangul, Yi and the
# full-width forms. Copied from its local-glyph test (5.24.1, BMP part); embedding these ranges
# would add tens of MB in a city with Chinese or Korean names and never be read.
LOCAL_GLYPHS = re.compile(
    "[\u02EA\u02EB\u1100-\u11FF\u2E80-\u2FDF\u3000-\u30FF\u3105-\u312F\u3131-\u318E"
    "\u31A0-\u4DBF\u4E00-\uA48C\uA490-\uA4C6\uA960-\uA97C\uAC00-\uD7C6\uD7CB-\uD7FB"
    "\uF900-\uFA6D\uFA70-\uFAD9\uFE10-\uFE1F\uFE30-\uFE4F\uFF00-\uFFEF]")


# ---------------------------------------------------------------------------------------------
# Downloads

def _download(url, dest, sha256=None):
    if dest.exists():
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_name(dest.name + ".part")
    print(f"  downloading {url}", flush=True)
    subprocess.run(["curl", "-sSfL", "--retry", "3", "-o", str(tmp), url], check=True)
    if sha256:
        got = hashlib.sha256(tmp.read_bytes()).hexdigest()
        if got != sha256:
            tmp.unlink()
            sys.exit(f"Checksum mismatch for {url}: got {got}, expected {sha256}")
    tmp.rename(dest)
    return dest


def _style(cache):
    """The composed OSM OpenMapTiles style and its icons, built once from the pinned release."""
    folder = cache / f"openmaptiles-{OMT_TAG}"
    style_path, icons_path = folder / "style.json", folder / "icons.json"
    if style_path.exists() and icons_path.exists():
        return json.loads(style_path.read_text()), json.loads(icons_path.read_text())
    tarball = _download(OMT_TARBALL, folder / "source.tar.gz")
    header, layers, icons = None, [], {}
    # Read the members we need straight out of the archive; nothing is extracted to disk.
    with tarfile.open(tarball) as tar:
        for member in tar:
            if not member.isfile():
                continue
            parts = member.name.split("/")[1:]  # drop the openmaptiles-<tag>/ prefix
            if parts == ["style", "style-header.json"]:
                header = json.load(tar.extractfile(member))
            elif len(parts) == 3 and parts[0] == "layers" and parts[2] == "style.json":
                layers += json.load(tar.extractfile(member))["layers"]
            elif len(parts) == 3 and parts[:2] == ["style", "icons"] and parts[2].endswith(".svg"):
                svg = tar.extractfile(member).read().decode()
                icons[parts[2][:-4]] = re.sub(r">\s+<", "><", svg).strip()
    if header is None or not layers:
        sys.exit(f"{OMT_TARBALL} doesn't look like the OpenMapTiles repo")
    # As openmaptiles-tools' style recompose does: header layers, then every layer's snippets
    # in their "order".
    layers.sort(key=lambda layer: layer.get("order", 0))
    for layer in layers:
        layer.pop("order", None)
    style = dict(header, layers=header["layers"] + layers)
    folder.mkdir(parents=True, exist_ok=True)
    style_path.write_text(json.dumps(style))
    icons_path.write_text(json.dumps(icons))
    tarball.unlink()
    return style, icons


def maplibre(cache):
    folder = cache / f"maplibre-gl-{MAPLIBRE_VERSION}"
    return {name: _download(f"https://unpkg.com/maplibre-gl@{MAPLIBRE_VERSION}/dist/{name}",
                            folder / name, sha).read_text()
            for name, sha in MAPLIBRE.items()}


# ---------------------------------------------------------------------------------------------
# PMTiles v3 (https://github.com/protomaps/PMTiles/blob/main/spec/v3/spec.md)

def _varint(buf, pos):
    result = shift = 0
    while True:
        b = buf[pos]
        pos += 1
        result |= (b & 0x7F) << shift
        if b < 0x80:
            return result, pos
        shift += 7


def _decompress(data, compression):
    if compression in (0, 1):  # unknown / none
        return data
    if compression == 2:
        return gzip.decompress(data)
    sys.exit(f"Unsupported pmtiles compression {compression}")


def zxy_to_tile_id(z, x, y):
    acc = ((1 << (2 * z)) - 1) // 3
    n = 1 << z
    for a in range(z - 1, -1, -1):
        s = 1 << a
        rx, ry = s & x, s & y
        acc += ((3 * rx) ^ ry) << a
        if ry == 0:
            if rx:
                x, y = n - 1 - x, n - 1 - y
            x, y = y, x
    return acc


class PmTiles:
    def __init__(self, path):
        self.f = open(path, "rb")
        h = self.f.read(127)
        if h[:7] != b"PMTiles" or h[7] != 3:
            sys.exit(f"{path} is not a v3 pmtiles file")
        (self.root_off, self.root_len, _, _, self.leaf_off, _, self.data_off, _) = \
            struct.unpack_from("<8Q", h, 8)
        self.internal_compression, self.tile_compression = h[97], h[98]
        self.min_zoom, self.max_zoom = h[100], h[101]
        self._dirs = {}

    def _directory(self, offset, length):
        key = (offset, length)
        if key not in self._dirs:
            self.f.seek(offset)
            buf = _decompress(self.f.read(length), self.internal_compression)
            n, pos = _varint(buf, 0)
            ids, runs, lens, offs = [], [], [], []
            last = 0
            for _ in range(n):
                d, pos = _varint(buf, pos)
                last += d
                ids.append(last)
            for out in (runs, lens):
                for _ in range(n):
                    v, pos = _varint(buf, pos)
                    out.append(v)
            for i in range(n):
                v, pos = _varint(buf, pos)
                offs.append(offs[i - 1] + lens[i - 1] if v == 0 and i > 0 else v - 1)
            self._dirs[key] = (ids, runs, lens, offs)
        return self._dirs[key]

    def get(self, z, x, y):
        """The decompressed tile, or None."""
        if not self.min_zoom <= z <= self.max_zoom:
            return None
        tid = zxy_to_tile_id(z, x, y)
        offset, length = self.root_off, self.root_len
        for _ in range(4):  # the spec allows at most 3 levels of leaf directories
            ids, runs, lens, offs = self._directory(offset, length)
            lo, hi = 0, len(ids) - 1
            i = -1
            while lo <= hi:  # last entry with id <= tid
                mid = (lo + hi) // 2
                if ids[mid] <= tid:
                    i, lo = mid, mid + 1
                else:
                    hi = mid - 1
            if i < 0:
                return None
            if runs[i] == 0:  # leaf directory
                offset, length = self.leaf_off + offs[i], lens[i]
                continue
            if tid >= ids[i] + runs[i]:
                return None
            self.f.seek(self.data_off + offs[i])
            return _decompress(self.f.read(lens[i]), self.tile_compression)
        return None


# ---------------------------------------------------------------------------------------------
# Choosing tiles and glyphs

def _tile_xy(lon, lat, z):
    n = 1 << z
    lat = max(min(lat, 85.0511), -85.0511)
    x = int((lon + 180) / 360 * n)
    y = int((1 - math.asinh(math.tan(math.radians(lat))) / math.pi) / 2 * n)
    return min(max(x, 0), n - 1), min(max(y, 0), n - 1)


def _wanted_tiles(points, max_zoom):
    lons, lats = [p[0] for p in points], [p[1] for p in points]
    dlat = BBOX_MARGIN_M / 111_320
    dlon = dlat / max(math.cos(math.radians(sum(lats) / len(lats))), 0.01)
    west, east = min(lons) - dlon, max(lons) + dlon
    south, north = min(lats) - dlat, max(lats) + dlat
    wanted = set()
    for z in range(0, min(DETAIL_ZOOM, max_zoom + 1)):
        x0, y0 = _tile_xy(west, north, z)
        x1, y1 = _tile_xy(east, south, z)
        wanted |= {(z, x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)}
    # Along the track: each fix's tile and its neighbours, so the edges of the view aren't bare
    step = max(1, len(points) // 20000)
    for z in range(DETAIL_ZOOM, max_zoom + 1):
        n = 1 << z
        last = None
        for lon, lat in points[::step]:
            if last and abs(lon - last[0]) + abs(lat - last[1]) < TRACK_SPACING_M / 111_320:
                continue
            last = (lon, lat)
            x, y = _tile_xy(lon, lat, z)
            wanted |= {(z, (x + dx) % n, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                       if 0 <= y + dy < n}
    return wanted


def _label_keys(style):
    """The tile attributes the style draws as text: {key} tokens and ["get", key] in text-field."""
    keys = set()
    for layer in style["layers"]:
        field = layer.get("layout", {}).get("text-field")
        if field is None:
            continue
        text = json.dumps(field)
        keys.update(re.findall(r"\{([^{}\"]+)\}", text))
        keys.update(re.findall(r'\["get", "([^"]+)"\]', text))
    return keys


def _fields(buf):
    """(field number, bytes) for each length-delimited protobuf field in buf."""
    pos = 0
    while pos < len(buf):
        key, pos = _varint(buf, pos)
        wire = key & 7
        if wire == 0:
            _, pos = _varint(buf, pos)
        elif wire == 1:
            pos += 8
        elif wire == 5:
            pos += 4
        elif wire == 2:
            length, pos = _varint(buf, pos)
            yield key >> 3, buf[pos:pos + length]
            pos += length
        else:
            return


def _label_strings(pbf, label_keys):
    """The values of label_keys in an MVT tile - the text the style can draw from it, which is
    what decides the glyph ranges. The tiles carry every name:xx translation too, and counting
    those would pull in most of Unicode."""
    for field, layer in _fields(pbf):
        if field != 3:  # Tile.layers
            continue
        keys, values, features = [], [], []
        for lfield, data in _fields(layer):
            if lfield == 3:  # Layer.keys
                keys.append(data.decode("utf-8", "replace"))
            elif lfield == 4:  # Layer.values
                values.append(data)
            elif lfield == 2:  # Layer.features
                features.append(data)
        wanted = {i for i, k in enumerate(keys) if k in label_keys}
        if not wanted:
            continue
        used = set()
        for feature in features:
            for ffield, tags in _fields(feature):
                if ffield != 2:  # Feature.tags, packed key/value index pairs
                    continue
                pos, pair = 0, []
                while pos < len(tags):
                    v, pos = _varint(tags, pos)
                    pair.append(v)
                    if len(pair) == 2:
                        if pair[0] in wanted:
                            used.add(pair[1])
                        pair = []
        for i in used:
            if i < len(values):
                for vfield, s in _fields(values[i]):
                    if vfield == 1:  # Value.string_value
                        yield s.decode("utf-8", "replace")


def build(cache, extracts, points):
    """Everything the viewer needs for its base map, as one JSON-able dict."""
    style, icons = _style(cache)
    readers = [PmTiles(p) for p in extracts]
    max_zoom = max(r.max_zoom for r in readers)
    label_keys = _label_keys(style)
    tiles, ranges, layer_names = {}, {0}, set()
    for z, x, y in sorted(_wanted_tiles(points, max_zoom)):
        for reader in readers:
            data = reader.get(z, x, y)
            if data:
                tiles[f"{z}/{x}/{y}"] = base64.b64encode(data).decode()
                # Every layer in the tiles, styled or not, for the viewer's inspect mode
                for field, layer in _fields(data):
                    if field == 3:
                        layer_names.update(n.decode() for f, n in _fields(layer) if f == 1)
                # Low zooms carry world-wide labels in their own scripts (country names, for
                # one); far from the track they can go unlabelled rather than pull in CJK fonts
                if z >= GLYPH_MIN_ZOOM:
                    for s in _label_strings(data, label_keys):
                        ranges.update(ord(c) // 256 for c in LOCAL_GLYPHS.sub("", s))
                break
    glyphs = {}
    with zipfile.ZipFile(_download(FONTS_URL, cache / "noto-sans-v2.0.zip", FONTS_SHA256)) as z:
        names = set(z.namelist())
        for font in FONTS:
            for r in sorted(ranges):
                name = f"{font}/{r * 256}-{r * 256 + 255}.pbf"
                if name in names:
                    glyphs[name[:-4]] = base64.b64encode(z.read(name)).decode()
    return {"style": style, "icons": icons, "tiles": tiles, "glyphs": glyphs,
            "fonts": list(FONTS), "maxzoom": max_zoom, "layers": sorted(layer_names)}
