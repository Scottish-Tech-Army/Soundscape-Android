#!/usr/bin/env python3
"""Structural checks on docs/translation-questions/questions-*.md.

Usage: check_questionnaires.py [code ...]   (default: every sheet)

Fails (exit 1) on:
  - front matter missing layout/nav_exclude/permalink, or a permalink that
    isn't /translation-questions/questions-<code>/ (polyglot silently drops
    pages whose file name or URL segment is a bare language code);
  - no link to the terminology page (the "What is Soundscape" section);
  - question numbers that aren't 1..n with no gaps;
  - an RTL language not wrapped in <div dir="rtl" markdown="1">;
  - an unresolved grammar marker outside Hungarian's own explanation
    (a static page is never run through resolveGrammarMarkers()).
Also warns when a sheet doesn't mention a guidance file's question range.
"""
import glob
import os
import re
import subprocess
import sys

ROOT = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
DOCS = f"{ROOT}/docs/translation-questions"
RTL = {"ar", "fa", "ur"}
MARKERS = re.compile(r"을\(를\)|를\(을\)|이\(가\)|은\(는\)|과\(와\)|와\(과\)|\(으\)로|\(이\)[가-힣]|'\{(?:DAn|DA|A|In|I)\}")


def check(path):
    code = os.path.basename(path)[len("questions-"):-3]
    s = open(path, encoding="utf-8").read()
    errs = []
    fm = s.split("---", 2)[1] if s.startswith("---") else ""
    for need in ("layout: default", "nav_exclude: true", f"permalink: /translation-questions/questions-{code}/"):
        if need not in fm:
            errs.append(f"front matter lacks '{need}'")
    if "translation-terminology" not in s:
        errs.append("no 'What is Soundscape' section (no terminology link)")
    nums = [int(n) for n in re.findall(r"^(?:#{2,3} |\*\*)Q(\d+)", s, re.M)]
    if nums and nums != list(range(1, len(nums) + 1)):
        errs.append(f"question numbers not continuous: {nums}")
    if code in RTL and '<div dir="rtl" markdown="1">' not in s:
        errs.append("RTL language without the dir=rtl wrapper")
    if MARKERS.search(s):
        errs.append(f"unresolved grammar marker: {MARKERS.search(s).group(0)}")
    g = f"{ROOT}/translations/guidance/{code}.md"
    if nums and os.path.exists(g):
        m = re.search(r"\(Q1…Q(\d+)\)", open(g, encoding="utf-8").read())
        if m and int(m.group(1)) != len(nums):
            errs.append(f"guidance says Q1…Q{m.group(1)} but sheet has {len(nums)} questions")
    return errs


def main():
    codes = sys.argv[1:]
    paths = [f"{DOCS}/questions-{c}.md" for c in codes] if codes else sorted(glob.glob(f"{DOCS}/questions-*.md"))
    bad = 0
    for p in paths:
        errs = check(p)
        for e in errs:
            print(f"{os.path.basename(p)}: {e}")
        bad += bool(errs)
    print(f"{len(paths) - bad}/{len(paths)} sheets OK")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
