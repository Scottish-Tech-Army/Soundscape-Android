#!/usr/bin/env python3
"""Flag translations that are much shorter than their English source.

Catches the failure mode recorded as rule C16 in translations/guidance/_common.md:
a pass that re-translates only the changed fragment of an edited English string
and throws the rest of the translation away (often marking the gap with "...").

Reads the <code>-translated.json files written by
`weblate_sync.py fetch --lang <code> --out-dir <dir>`.

    python3 truncation_check.py <dir> [<code> ...]

Each language is judged against its own median target/source length ratio, so
languages that are naturally short in characters (ja, zh_Hans, ko) aren't
flagged wholesale. A string is flagged when it is under 55% of that median, or
when the English has 3+ sentences and the translation has at least 2 fewer
(and is under 80% of the median), or when the translation starts or ends with
an ellipsis the English doesn't have. POI names (osm_*) and English sources
under 60 characters are skipped.

Expect a few false positives: Thai has no sentence punctuation, and a human
translator's deliberately concise wording can look short. Check every flag
against the English before acting on it. If the string was truncated by an
earlier pass, the complete translation is usually still in git history.
"""
import glob
import json
import os
import re
import statistics
import sys

SENTENCE = re.compile(r'[.!?؟।]+(?=\s|$|\*|"|\\)|[。！？]+')
ELLIPSIS_START = re.compile(r'\s*(\.\.\.|…)')
ELLIPSIS_END = re.compile(r'(\.\.\.|…)\s*$')


def clean(s):
    s = re.sub(r'\\n|\n', ' ', s)
    s = re.sub(r'%\d\$s', 'X', s)
    s = re.sub(r'\[([^\]]*)\]\([^)]*\)', r'\1', s)
    s = re.sub(r'[*_#\\"]', '', s)
    return re.sub(r'\s+', ' ', s).strip()


def sentences(s):
    return len([x for x in SENTENCE.split(clean(s)) if len(x.strip()) > 3])


def check(path):
    units = [u for u in json.load(open(path, encoding='utf-8'))
             if not u['context'].startswith('osm_') and u.get('target')
             and len(clean(u['source'])) >= 60]
    if not units:
        return None, []
    ratios = [len(clean(u['target'])) / len(clean(u['source'])) for u in units]
    median = statistics.median(ratios)
    flags = []
    for u, ratio in zip(units, ratios):
        rel = ratio / median
        en_s, tr_s = sentences(u['source']), sentences(u['target'])
        ellipsis = ((ELLIPSIS_START.match(u['target']) and not ELLIPSIS_START.match(u['source']))
                    or (ELLIPSIS_END.search(u['target']) and not ELLIPSIS_END.search(u['source'])))
        if ellipsis or rel < 0.55 or (en_s >= 3 and tr_s <= en_s - 2 and rel < 0.8):
            flags.append((u['context'], len(clean(u['target'])), len(clean(u['source'])),
                          round(rel, 2), tr_s, en_s, bool(ellipsis)))
    return median, flags


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    directory, wanted = sys.argv[1], set(sys.argv[2:])
    total = 0
    for path in sorted(glob.glob(os.path.join(directory, '*-translated.json'))):
        code = os.path.basename(path)[:-len('-translated.json')]
        if wanted and code not in wanted:
            continue
        median, flags = check(path)
        for key, tlen, elen, rel, ts, es, ell in flags:
            total += 1
            print(f"{code:8} {key:48} {tlen:4}/{elen:4} chars  rel={rel:<4}  "
                  f"sentences {ts}/{es}{'  ELLIPSIS' if ell else ''}")
    print(f"{total} flagged", file=sys.stderr)


if __name__ == '__main__':
    main()
