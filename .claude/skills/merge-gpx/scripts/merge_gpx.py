#!/usr/bin/env python3
"""Merge overlapping Soundscape GPX recordings into one long track.

The app's recorder exports a rolling window (the last 3600 fixes), so
successive exports from one device overlap. This stitches them together:
files are ordered by their first fix, and each file contributes only the
fixes later than everything already merged. Points are copied verbatim
(including <extensions> such as steps/headTracking), and fixes within a
file keep their original order, duplicates included (iOS writes some fixes
twice, once with headTracking).

Usage: merge_gpx.py -o OUT.gpx IN1.gpx IN2.gpx ...
"""
import argparse
import re
import sys
from datetime import datetime

TRKPT = re.compile(r"<trkpt\b.*?</trkpt>", re.S)
TIME = re.compile(r"<time>([^<]+)</time>")
LATLON = re.compile(r'lat="([^"]+)"\s+lon="([^"]+)"')


def parse_time(s):
    return datetime.fromisoformat(s.replace("Z", "+00:00"))


def load(path):
    text = open(path, encoding="utf-8").read()
    head = text[: text.index("<trkseg>")]
    points = []
    for m in TRKPT.finditer(text):
        block = m.group(0)
        t = TIME.search(block)
        if not t:
            sys.exit(f"{path}: trkpt without <time>")
        points.append((parse_time(t.group(1)), block))
    if not points:
        sys.exit(f"{path}: no trkpt elements")
    if text.count("<trkseg>") != 1:
        print(f"warning: {path} has {text.count('<trkseg>')} segments; merging them as one",
              file=sys.stderr)
    backwards = sum(1 for a, b in zip(points, points[1:]) if b[0] < a[0])
    if backwards:
        print(f"warning: {path} has {backwards} fixes earlier than the one before",
              file=sys.stderr)
    return head, points


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-o", "--output", required=True)
    ap.add_argument("inputs", nargs="+")
    args = ap.parse_args()

    files = [(p, *load(p)) for p in args.inputs]
    files.sort(key=lambda f: f[2][0][0])

    merged, last, header = [], None, files[0][1]
    for path, head, pts in files:
        first, end = pts[0][0], pts[-1][0]
        if last is not None and first > last:
            gap = (first - last).total_seconds()
            print(f"warning: {gap:.0f}s GAP before {path} (no overlap)", file=sys.stderr)
        # Check the overlapping fixes agree with what is already merged.
        if last is not None:
            have = {TIME.search(b).group(1) + LATLON.search(b).group(0)
                    for t, b in merged if t >= first}
            overlap = [b for t, b in pts if t <= last]
            mismatched = sum(1 for b in overlap
                             if TIME.search(b).group(1) + LATLON.search(b).group(0) not in have)
            if mismatched:
                print(f"warning: {path}: {mismatched}/{len(overlap)} overlapping fixes "
                      f"don't match the earlier file(s)", file=sys.stderr)
        new = [(t, b) for t, b in pts if last is None or t > last]
        print(f"{path}: {first:%H:%M:%S} - {end:%H:%M:%S}, {len(pts)} fixes, "
              f"{len(new)} new", file=sys.stderr)
        merged.extend(new)
        if new:
            last = max(last, new[-1][0]) if last else new[-1][0]

    with open(args.output, "w", encoding="utf-8") as f:
        f.write(header)
        f.write("<trkseg>\n")
        for _, b in merged:
            f.write(b + "\n")
        f.write("</trkseg>\n</trk>\n</gpx>\n")
    dur = merged[-1][0] - merged[0][0]
    print(f"wrote {args.output}: {len(merged)} fixes, "
          f"{merged[0][0]:%Y-%m-%d %H:%M:%S} - {merged[-1][0]:%H:%M:%S} ({dur})",
          file=sys.stderr)


if __name__ == "__main__":
    main()
