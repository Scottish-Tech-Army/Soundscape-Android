#!/usr/bin/env python3
"""Combine per-language translation-review findings into one summary.

Usage: summarize_findings.py [--dir /tmp/translation-review] [--out FILE] <code> ...

Reads <dir>/<code>-findings.json for each code, checks each finding's `current`
against <dir>/<code>-translated.json, and writes a markdown summary (default
<dir>/all-summary.md). Prints the per-language table to stdout as well.
"""
import argparse
import json
import os
from collections import Counter, defaultdict

SERIOUS = ("meaning", "placeholders", "formatting")
CATEGORIES = SERIOUS + ("gender", "terminology", "tone", "other")
CROSS = "CROSS-LANGUAGE"


def is_cross(f):
    return str(f.get("reason", "")).lstrip().upper().startswith(CROSS)


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def flat(value):
    if isinstance(value, dict):
        return " | ".join(f"{k}: {v}" for k, v in value.items())
    return str(value)


def one_line(text, limit=160):
    text = flat(text).replace("\n", "\\n")
    return text if len(text) <= limit else text[: limit - 1] + "…"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default="/tmp/translation-review")
    ap.add_argument("--out")
    ap.add_argument("codes", nargs="+")
    args = ap.parse_args()
    out_path = args.out or os.path.join(args.dir, "all-summary.md")

    rows = []
    missing = []
    cross = defaultdict(list)
    serious = {}
    mismatches = []
    totals = Counter()

    for code in args.codes:
        fpath = os.path.join(args.dir, f"{code}-findings.json")
        if not os.path.exists(fpath):
            missing.append(code)
            continue
        findings = load(fpath)
        tpath = os.path.join(args.dir, f"{code}-translated.json")
        units = load(tpath) if os.path.exists(tpath) else []
        live = {u["context"]: u["target"] for u in units}

        cats = Counter()
        fixable = 0
        for f in findings:
            cat = f.get("category") or "other"
            if cat not in CATEGORIES:
                cat = "other"
            cats[cat] += 1
            if f.get("suggested") and f["suggested"] != f.get("current"):
                fixable += 1
            if live and live.get(f.get("context")) != f.get("current"):
                mismatches.append((code, f.get("context")))
            if is_cross(f):
                cross[f.get("context")].append((code, f))
        serious[code] = [
            f for f in findings
            if (f.get("category") in SERIOUS)
            and not is_cross(f)
        ]
        row = {
            "code": code,
            "units": len(units),
            "flagged": len(findings),
            "fixable": fixable,
            "serious": sum(cats[c] for c in SERIOUS),
            **{c: cats[c] for c in CATEGORIES},
        }
        rows.append(row)
        for k in ("units", "flagged", "fixable"):
            totals[k] += row[k]

    rows.sort(key=lambda r: (-r["serious"], -r["flagged"], r["code"]))

    header = ["language", "units", "flagged", "fixable"] + list(CATEGORIES)
    table = ["| " + " | ".join(header) + " |",
             "|" + "|".join("---" for _ in header) + "|"]
    for r in rows:
        table.append("| " + " | ".join(
            [r["code"], str(r["units"]), str(r["flagged"]), str(r["fixable"])]
            + [str(r[c]) for c in CATEGORIES]) + " |")

    lines = ["# Translation review, all languages", ""]
    lines.append(
        f"{len(rows)} languages reviewed, {totals['units']} units read, "
        f"{totals['flagged']} flagged, {totals['fixable']} with a confident fix.")
    if missing:
        lines.append(f"\n**Not reviewed (no findings file):** {', '.join(missing)}")
    if mismatches:
        lines.append(
            f"\n**{len(mismatches)} findings quote a `current` that doesn't match "
            "the fetched string**, so the apply step will drop them as stale: "
            + ", ".join(f"{c}:{k}" for c, k in mismatches[:40])
            + (" …" if len(mismatches) > 40 else ""))

    lines += ["", "## Per language", "", *table]

    lines += ["", "## Cross-language", ""]
    if not cross:
        lines.append("None reported.")
    for key, hits in sorted(cross.items(), key=lambda kv: -len(kv[1])):
        codes = ", ".join(c for c, _ in hits)
        lines.append(f"### `{key}` ({len(hits)}: {codes})")
        seen = set()
        for c, f in hits:
            reason = f["reason"].lstrip()[len(CROSS):].lstrip(" :,-").strip()
            if reason in seen:
                continue
            seen.add(reason)
            lines.append(f"- **{c}:** {one_line(reason, 400)}")
        lines.append("")

    lines += ["", "## Meaning, placeholder and formatting findings", ""]
    for r in rows:
        items = serious.get(r["code"]) or []
        if not items:
            continue
        lines.append(f"### {r['code']} ({len(items)})")
        for f in items:
            fix = "" if f.get("suggested") == f.get("current") else \
                f" → «{one_line(f.get('suggested'))}»"
            lines.append(
                f"- `{f.get('context')}` ({f.get('category')}): {one_line(f.get('reason'), 300)}"
                f"  \n  «{one_line(f.get('current'))}»{fix}")
        lines.append("")

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    print("\n".join(table))
    if missing:
        print("not reviewed:", " ".join(missing))
    if mismatches:
        print(f"{len(mismatches)} findings with a stale or misquoted `current`")
    print(f"{len(cross)} cross-language keys")
    print("wrote", out_path)


if __name__ == "__main__":
    main()
