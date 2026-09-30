#!/usr/bin/env python3
"""
Find translations whose English source has changed since they were last translated.

This is the check Weblate does when it marks a unit "needs editing", worked out from git
history instead. For every string in every values-<lang>/strings.xml it finds the commit where
the translation's text last really changed, reads what the English said at that commit, and
reports the string as stale if the English has changed since.

"Really changed" ignores ASCII double quotes and whitespace, including no-break spaces, on
both sides. Weblate rewrites translations without anyone translating them - stripping quotes,
dropping French no-break spaces - and counting those rewrites would make stale strings look
fresh. The same normalisation stops a pure formatting edit to the English from flagging every
language.

A change to the English that genuinely needs nothing from a language (a typo fix, say) can be
acknowledged, which records the current English for that string in
translations/stale-acknowledged.json. It then stays quiet until the English changes again.

History starts when the resources moved to shared/composeResources (2026-04-19). Anything
already stale before that is not detected.

While Weblate is still in the loop, a translation is dated by its commit's author time, and
Weblate's squash add-on stamps each squashed commit with the time of the squash rather than of
the translations in it. A string Weblate flagged after a source change can then look up to
date here until Weblate's own fix is merged back. translations/stale-acknowledged.json was
seeded on 2026-09-30 with every string this script found stale that Weblate had not flagged,
since Weblate's reviewers had accepted those.

Usage:
    scripts/find-stale-translations.py                  # summary for every language
    scripts/find-stale-translations.py --lang de fr     # details for some languages
    scripts/find-stale-translations.py --lang de --diff # also show old -> new English
    scripts/find-stale-translations.py --json out.json  # machine-readable, all languages
    scripts/find-stale-translations.py --lang de --acknowledge key1 key2
"""

import argparse
import bisect
import difflib
import hashlib
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
RES = "shared/src/commonMain/composeResources"
ENGLISH = f"{RES}/values/strings.xml"
ACK_FILE = REPO / "translations" / "stale-acknowledged.json"

# Weblate-style language codes (as translations/guidance uses) -> values-* suffix, where they
# differ. Either form is accepted on the command line.
WEBLATE_CODES = {"en_GB": "en-rGB", "fr_CA": "fr-rCA", "pt_BR": "pt-rBR",
                 "zh_Hans": "zh-rCN", "nb_NO": "nb", "id": "in"}


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=REPO, check=True, capture_output=True, text=True
    ).stdout


class BlobReader:
    """Reads and parses blobs through one `git cat-file --batch` process, caching by hash."""

    def __init__(self):
        self.proc = subprocess.Popen(
            ["git", "cat-file", "--batch"], cwd=REPO,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        )
        self.cache: dict[str, dict[str, str]] = {}

    def parsed(self, spec: str) -> dict[str, str]:
        """Strings in the file named by `spec` (`<commit>:<path>`), or {} if it doesn't exist."""
        self.proc.stdin.write((spec + "\n").encode())
        self.proc.stdin.flush()
        header = self.proc.stdout.readline().decode().split()
        if len(header) < 3 or header[1] != "blob":
            return {}
        sha, size = header[0], int(header[2])
        data = self.proc.stdout.read(size)
        self.proc.stdout.read(1)  # trailing newline
        if sha not in self.cache:
            self.cache[sha] = parse_strings(data)
        return self.cache[sha]


def parse_strings(data: bytes) -> dict[str, str]:
    """name -> text. A plural becomes its items joined as `quantity=text`, one per line."""
    try:
        root = ET.fromstring(data)
    except ET.ParseError:
        return {}
    out = {}
    for el in root:
        name = el.get("name")
        if not name or el.get("translatable") == "false":
            continue
        if el.tag == "string":
            out[name] = el.text or ""
        elif el.tag == "plurals":
            out[name] = "\n".join(
                f"{item.get('quantity')}={item.text or ''}" for item in el.findall("item")
            )
    return out


def normalise(text: str) -> str:
    text = text.replace('\\"', "").replace('"', "").replace("\\'", "'")
    text = text.replace(" ", " ").replace(" ", " ")
    return re.sub(r"\s+", " ", text).strip()


def fingerprint(text: str) -> str:
    return hashlib.sha1(normalise(text).encode()).hexdigest()[:12]


def languages() -> list[str]:
    dirs = sorted(p.name for p in (REPO / RES).glob("values-*") if (p / "strings.xml").exists())
    return [d[len("values-"):] for d in dirs]


def history(path: str, date: str = "%ct") -> list[tuple[str, int]]:
    """(commit, time) for every commit that touched `path`, oldest first in history order.

    `date` is a git format: %ct for when a commit landed, %at for when it was authored.
    """
    lines = git("log", "--reverse", f"--format=%H {date}", "--", path).split("\n")
    return [(c, int(t)) for c, t in (line.split() for line in lines if line)]


class EnglishTimeline:
    """What the English said at a given moment, by when each English commit landed.

    A translation is judged against the English as it stood when the translation was
    *authored*, not against its parent commit. Weblate's commits are rebased onto main when
    its PR merges, so in history a translation saved before an English change can sit after
    it - and would look up to date against an English it never saw.
    """

    def __init__(self, blobs: "BlobReader"):
        commits = sorted(history(ENGLISH), key=lambda ct: ct[1])
        self.times = [t for _, t in commits]
        self.versions = [blobs.parsed(f"{c}:{ENGLISH}") for c, _ in commits]

    def at(self, when: int) -> dict[str, str]:
        i = bisect.bisect_right(self.times, when) - 1
        return self.versions[max(i, 0)]


def stale_for(lang: str, blobs: BlobReader, timeline: EnglishTimeline,
              english_now: dict[str, str]) -> dict:
    path = f"{RES}/values-{lang}/strings.xml"
    # key -> (commit, author time) where its translation last really changed
    last_change: dict[str, tuple[str, int]] = {}
    previous: dict[str, str] = {}
    for commit, authored in history(path, "%at"):
        strings = blobs.parsed(f"{commit}:{path}")
        for key, text in strings.items():
            norm = normalise(text)
            if previous.get(key) != norm:
                previous[key] = norm
                last_change[key] = (commit, authored)
    current = blobs.parsed(f"HEAD:{path}")

    stale, untranslated = [], []
    for key, english in english_now.items():
        if key not in current:
            untranslated.append(key)
            continue
        commit, authored = last_change.get(key, (None, 0))
        seen = timeline.at(authored).get(key) if commit else None
        if seen is None:
            # The English key didn't exist when this was translated: it was translated under
            # a key that was later reused, or before the English was committed. Either way the
            # translation can't have been made against the current English.
            stale.append({"key": key, "was": None, "now": english, "translated_in": commit})
        elif normalise(seen) != normalise(english):
            stale.append({"key": key, "was": seen, "now": english, "translated_in": commit})
    return {"stale": stale, "untranslated": untranslated}


def load_acks() -> dict:
    return json.loads(ACK_FILE.read_text()) if ACK_FILE.exists() else {}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--lang", nargs="+", help="Weblate-style or directory codes, e.g. de pt-rBR")
    ap.add_argument("--diff", action="store_true", help="show how the English changed")
    ap.add_argument("--json", help="write all results to this file")
    ap.add_argument("--acknowledge", nargs="+", metavar="KEY",
                    help="mark these keys as checked for --lang (exactly one language)")
    args = ap.parse_args()

    all_langs = languages()
    wanted = all_langs
    if args.lang:
        # Accept Weblate's codes (pt_BR, zh_Hans) as well as the directory names (pt-rBR).
        def to_dir(code: str) -> str:
            d = WEBLATE_CODES.get(code, code)
            if d in all_langs:
                return d
            sys.exit(f"No values-{d}/strings.xml (known: {' '.join(all_langs)})")
        wanted = [to_dir(c) for c in args.lang]

    blobs = BlobReader()
    english_now = blobs.parsed(f"HEAD:{ENGLISH}")
    timeline = EnglishTimeline(blobs)
    acks = load_acks()

    if args.acknowledge:
        if len(wanted) != 1:
            sys.exit("--acknowledge needs exactly one --lang")
        lang = wanted[0]
        for key in args.acknowledge:
            if key not in english_now:
                sys.exit(f"No English string {key}")
            acks.setdefault(lang, {})[key] = fingerprint(english_now[key])
        ACK_FILE.write_text(json.dumps(acks, indent=1, sort_keys=True, ensure_ascii=False) + "\n")
        print(f"Acknowledged {len(args.acknowledge)} string(s) for {lang} in {ACK_FILE.relative_to(REPO)}")
        return 0

    results = {}
    for lang in wanted:
        r = stale_for(lang, blobs, timeline, english_now)
        acked = acks.get(lang, {})
        r["stale"] = [s for s in r["stale"] if acked.get(s["key"]) != fingerprint(s["now"])]
        results[lang] = r

    if args.json:
        Path(args.json).write_text(json.dumps(results, indent=1, ensure_ascii=False))

    detailed = bool(args.lang)
    for lang, r in results.items():
        print(f"{lang:8} stale={len(r['stale']):4}  untranslated={len(r['untranslated']):4}")
        if not detailed:
            continue
        for s in r["stale"]:
            print(f"    stale         {s['key']}")
            if args.diff:
                if s["was"] is None:
                    print("        (English key did not exist when this was translated)")
                else:
                    for line in difflib.ndiff([s["was"]], [s["now"]]):
                        if line[:1] in "-+?":
                            print(f"        {line.rstrip()}")
        for key in r["untranslated"]:
            print(f"    untranslated  {key}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
