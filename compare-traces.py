#!/usr/bin/env python3
"""
Compare two Soundscape GPX recordings of the same journey, one per platform.

    python3 compare-traces.py iphoneTravel.gpx androidTravel.gpx
    python3 compare-traces.py a.gpx b.gpx --shift 1.52   # skip the fit, use this offset

Reports each trace's reported accuracy and bearing-accuracy distributions, estimates the
clock offset between the two handsets, and measures how far apart the two tracks sit once
that offset is taken out.

Three things this exists to catch, all of them found the hard way:

- Comparing platforms at one speed proves nothing. Course accuracy is strongly
  speed-dependent on both, and they cross over - iOS is the better of the two above
  2.5m/s and the worse below it - so every figure is broken out by speed band. A
  walking-only sample suggested iOS course accuracy was 6x Android's; it isn't.
- The handsets' clocks disagree, by 1.52s in the 2026-09-20 pair. At 20m/s that puts
  the two tracks 30m apart for reasons that have nothing to do with positioning, and it
  swamps everything else. See estimate_clock_offset.
- A platform can report a constant where a measurement should be. 56% of the iOS fixes
  in that same pair carry a horizontal accuracy of exactly 14.2459545, which matters
  because KalmanFilter squares that figure to decide how much to trust a fix.
"""
import math
import re
import sys
from collections import Counter
from datetime import datetime, timezone

BEARING_GATE_DEGREES = 45.0          # GeoEngine.createUserGeometry
SPEED_BANDS = [("stationary", 0.0, 0.3), ("walking", 0.3, 2.5),
               ("vehicle", 2.5, 8.0), ("fast", 8.0, 1e9)]

# --- clock-offset fit ------------------------------------------------------------------
# Below this speed the along-track component of the separation is buried in position
# noise: at 1m/s a 1s offset moves the fix by a metre, against several metres of scatter.
MIN_SPEED_FOR_FIT = 10.0
# The offset is read along the direction of travel, so a fix whose course can't be
# trusted can't contribute one.
MAX_BEARING_ACCURACY_FOR_FIT = 20.0
# Consecutive 1Hz fixes are heavily correlated - treating them as independent samples
# gives an error bar several times too small. Blocks of this length are near enough
# independent of each other to take a spread across.
BLOCK_SECONDS = 120
MIN_SAMPLES_PER_BLOCK = 15
MIN_BLOCKS = 3


def parse(path):
    text = open(path).read()
    stream = re.search(r'locationStream="(\w+)"', text)
    version = re.search(r'recorderVersion="(\d+)"', text)
    points = []
    for m in re.finditer(r'<trkpt lat="([-\d.]+)" lon="([-\d.]+)">(.*?)</trkpt>', text, re.S):
        body = m.group(3)

        def field(tag):
            hit = re.search(r"<%s>([-\d.eE]+)</%s>" % (tag, tag), body)
            return float(hit.group(1)) if hit else None

        stamp = re.search(r"<time>([^<]+)</time>", body)
        when = None
        if stamp:
            when = datetime.strptime(stamp.group(1), "%Y-%m-%dT%H:%M:%S.%fZ") \
                .replace(tzinfo=timezone.utc).timestamp()
        points.append(dict(lat=float(m.group(1)), lon=float(m.group(2)), t=when,
                           acc=field("accuracy"), speed=field("speed"),
                           bearing=field("bearing"),
                           bacc=field("bearingAccuracyDegrees")))
    points.sort(key=lambda p: (p["t"] is None, p["t"]))
    return dict(path=path, points=points,
                stream=stream.group(1) if stream else "filtered (unmarked, v1)",
                version=version.group(1) if version else None)


def pct(values, p):
    if not values:
        return float("nan")
    ordered = sorted(values)
    return ordered[min(int(len(ordered) * p / 100.0), len(ordered) - 1)]


def metres_per_degree(lat):
    return 111320.0 * math.cos(math.radians(lat)), 110540.0


def interpolate(points, when):
    """Position of `points` at time `when`, or None outside the track or across a gap."""
    if not points or points[0]["t"] is None:
        return None
    if when < points[0]["t"] or when > points[-1]["t"]:
        return None
    lo, hi = 0, len(points) - 1
    while hi - lo > 1:
        mid = (lo + hi) // 2
        if points[mid]["t"] <= when:
            lo = mid
        else:
            hi = mid
    a, b = points[lo], points[hi]
    if b["t"] - a["t"] > 3.0:
        return None
    span = b["t"] - a["t"]
    f = 0.0 if span == 0 else (when - a["t"]) / span
    return (a["lat"] + f * (b["lat"] - a["lat"]),
            a["lon"] + f * (b["lon"] - a["lon"]))


def offsets(first, second, shift=0.0):
    """(speed, along-track, cross-track) for each fix of `first` matched into `second`.

    `shift` is how far `second`'s clock runs ahead of `first`'s, in seconds: a record
    `second` labels t was captured at t-shift, so the fix contemporary with `first`'s
    t is looked up at t+shift.
    """
    pts = [p for p in first["points"] if p["t"] is not None]
    if not pts:
        return []
    kx, ky = metres_per_degree(pts[0]["lat"])
    out = []
    for p in pts:
        other = interpolate(second["points"], p["t"] + shift)
        if other is None or p["bearing"] is None or p["speed"] is None:
            continue
        dx = (p["lon"] - other[1]) * kx
        dy = (p["lat"] - other[0]) * ky
        th = math.radians(p["bearing"])            # compass bearing: 0=N, 90=E
        out.append((p["speed"],
                    dx * math.sin(th) + dy * math.cos(th),
                    dx * math.cos(th) - dy * math.sin(th)))
    return out


def estimate_clock_offset(first, second, verbose=True):
    """Seconds by which `second`'s clock runs ahead of `first`'s, or None if unmeasurable.

    If the two devices' clocks differ, the tracks are displaced ALONG the direction of
    travel by offset*speed while the cross-track displacement stays put - which is what
    separates a clock difference from the phones simply being in different pockets. So
    each fast sample gives its own estimate, along-track divided by speed.

    Taken as the median of per-block medians rather than over all samples at once: 1Hz
    fixes are correlated over tens of seconds, so pooling them produces a confident
    answer to three decimal places that moves by a tenth of a second if you change the
    estimator. The block spread is the honest error bar.
    """
    usable = [p for p in first["points"]
              if p["t"] is not None and p["speed"] is not None
              and p["speed"] >= MIN_SPEED_FOR_FIT
              and p["bacc"] is not None and p["bacc"] <= MAX_BEARING_ACCURACY_FOR_FIT]
    if not usable:
        if verbose:
            print("== clock offset ==")
            print(f"   Not measurable: no fixes above {MIN_SPEED_FOR_FIT:.0f} m/s with a")
            print(f"   course accuracy of {MAX_BEARING_ACCURACY_FOR_FIT:.0f} degrees or better.")
            print("   A walking-only recording cannot show a clock difference.\n")
        return None

    kx, ky = metres_per_degree(usable[0]["lat"])
    samples = []
    for p in usable:
        other = interpolate(second["points"], p["t"])
        if other is None:
            continue
        dx = (p["lon"] - other[1]) * kx
        dy = (p["lat"] - other[0]) * ky
        th = math.radians(p["bearing"])
        along = dx * math.sin(th) + dy * math.cos(th)
        samples.append((p["t"], along / p["speed"]))

    blocks, current, start = [], [], None
    for when, estimate in samples:
        if start is None:
            start = when
        elif when - start > BLOCK_SECONDS:
            if len(current) >= MIN_SAMPLES_PER_BLOCK:
                blocks.append((start, pct(current, 50), len(current)))
            start, current = when, []
        current.append(estimate)
    if len(current) >= MIN_SAMPLES_PER_BLOCK:
        blocks.append((start, pct(current, 50), len(current)))

    if len(blocks) < MIN_BLOCKS:
        if verbose:
            print("== clock offset ==")
            print(f"   Not measurable: only {len(blocks)} usable block(s) of fast travel, "
                  f"{MIN_BLOCKS} needed.\n")
        return None

    values = [b[1] for b in blocks]
    offset = pct(values, 50)
    if verbose:
        print("== clock offset ==")
        print(f"   {len(samples)} samples above {MIN_SPEED_FOR_FIT:.0f} m/s, "
              f"in {len(blocks)} blocks of up to {BLOCK_SECONDS}s:")
        for when, value, n in blocks:
            stamp = datetime.fromtimestamp(when, timezone.utc).strftime("%H:%M")
            print(f"     {stamp}  n={n:4d}  {value:+7.3f} s")
        print(f"   {second['path']} clock runs {offset:+.2f} s relative to {first['path']}")
        print(f"   (block range {min(values):+.3f} to {max(values):+.3f} s)")
        print("   Measured from the recordings themselves, which is the figure to use -")
        print("   a clock read off the handsets afterwards has drifted since, and a")
        print("   status-bar clock lags the second boundary differently per platform.\n")
    return offset


def summarise(trace):
    points = trace["points"]
    print(f"== {trace['path']} ==")
    print(f"   recorder v{trace['version']}, {trace['stream']} stream, {len(points)} points")
    if trace["version"] is None:
        print("   NOTE: no version marker - positions are already Kalman filtered, so the")
        print("         accuracy figures below describe a smoothed track.")

    accs = [p["acc"] for p in points if p["acc"] is not None]
    if accs:
        print(f"   accuracy m:  median={pct(accs,50):6.2f}  p68={pct(accs,68):6.2f}  "
              f"p95={pct(accs,95):6.2f}  min={min(accs):5.2f}  max={max(accs):6.2f}")
        value, count = Counter(accs).most_common(1)[0]
        if count > 0.2 * len(accs):
            print(f"   WARNING: {100.0*count/len(accs):.0f}% of fixes report accuracy of exactly "
                  f"{value} -")
            print("            a clamp rather than a measurement. KalmanFilter squares this")
            print("            figure, so it cannot tell those fixes apart by quality.")

    baccs = [p["bacc"] for p in points if p["bacc"] is not None]
    if baccs:
        passing = sum(1 for v in baccs if v < BEARING_GATE_DEGREES)
        print(f"   bearingAcc:  median={pct(baccs,50):6.2f}  p68={pct(baccs,68):6.2f}  "
              f"under {BEARING_GATE_DEGREES:.0f} deg: {passing}/{len(points)} "
              f"({100.0*passing/len(points):.0f}% of all fixes)")
    else:
        print("   bearingAcc:  none recorded")

    print("   by speed band:")
    for name, low, high in SPEED_BANDS:
        band = [p for p in points
                if p["speed"] is not None and low <= p["speed"] < high
                and p["bacc"] is not None]
        if len(band) < 5:
            continue
        vals = [p["bacc"] for p in band]
        acc = [p["acc"] for p in band if p["acc"] is not None]
        passing = sum(1 for v in vals if v < BEARING_GATE_DEGREES)
        print(f"     {name:10s} n={len(band):5d}  accuracy={pct(acc,50):6.2f} m  "
              f"bearingAcc={pct(vals,50):6.2f} deg  passing={100.0*passing/len(band):5.1f}%")
    print()


def separation(first, second, shift, label):
    rows = offsets(first, second, shift)
    if not rows:
        print(f"== separation {label} ==\n   The two traces do not overlap in time.\n")
        return
    n = len(rows)
    seps = [math.hypot(r[1], r[2]) for r in rows]
    mean_along = sum(r[1] for r in rows) / n
    mean_cross = sum(r[2] for r in rows) / n
    scatter = [math.hypot(r[1] - mean_along, r[2] - mean_cross) for r in rows]

    print(f"== separation {label} ==")
    print(f"   {n} matched samples: median={pct(seps,50):5.2f} m  p68={pct(seps,68):5.2f}  "
          f"p95={pct(seps,95):5.2f}")
    print(f"   constant offset {math.hypot(mean_along, mean_cross):5.2f} m "
          f"(along-track {mean_along:+.2f}, cross-track {mean_cross:+.2f})")
    print(f"   scatter about it: median={pct(scatter,50):5.2f} m  p68={pct(scatter,68):5.2f}")
    print(f"   {'band':11s} {'n':>6s} {'along-track':>12s} {'cross-track':>12s}")
    for name, low, high in SPEED_BANDS:
        band = [r for r in rows if low <= r[0] < high]
        if len(band) < 20:
            continue
        print(f"   {name:11s} {len(band):6d} {pct([r[1] for r in band],50):+11.2f}m "
              f"{pct([r[2] for r in band],50):+11.2f}m")
    print()


def main():
    args = [a for a in sys.argv[1:]]
    shift = None
    if "--shift" in args:
        i = args.index("--shift")
        try:
            shift = float(args[i + 1])
        except (IndexError, ValueError):
            print("--shift needs a number of seconds")
            return 1
        del args[i:i + 2]
    if len(args) != 2:
        print(__doc__)
        return 1

    first, second = parse(args[0]), parse(args[1])
    summarise(first)
    summarise(second)

    if shift is None:
        shift = estimate_clock_offset(first, second)
    else:
        print(f"== clock offset ==\n   Using the {shift:+.2f} s given on the command line.\n")

    separation(first, second, 0.0, "as recorded")
    if shift:
        separation(first, second, shift, f"with the {shift:+.2f} s clock offset removed")
        print("Two phones carried together share satellites and multipath, so their errors")
        print("correlate: the residual above is a floor on the true error, not a measure of it.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
