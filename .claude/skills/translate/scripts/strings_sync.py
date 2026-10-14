#!/usr/bin/env python3
"""
Local translation I/O for the translate/review/feedback skills: works on the repo's
shared/src/commonMain/composeResources/values-*/strings.xml directly, with no Weblate.

It keeps weblate_sync.py's commands and file shapes, so a skill's procedure hardly changes:

    languages                      how much work each language has
    fetch   --lang X | --all       write <code>-untranslated.json (work to do) and
                                   <code>-translated.json (everything already translated)
    validate --lang X --file F     check a {key: translation} file before applying it
    apply   --lang X --file F | --all --out-dir D
                                   write translations into values-*/strings.xml

"Work to do" is every string with no translation plus every *stale* string: one whose English
changed after it was translated, as found by scripts/find-stale-translations.py. A stale unit
carries `was`, the English it was translated from, so the change can be seen.

Plurals: a plural unit's `source` and `target` are objects of {quantity: text}, and a
translation for one must be an object with exactly the quantities the language uses.

Language codes are Weblate's (pt_BR, zh_Hans, nb_NO, id), matching translations/guidance.

`apply` edits the working tree and does not commit. Commit afterwards: the stale check reads
git history, so an applied translation only stops showing as stale once it is committed.
"""
import argparse
import importlib.util
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any, Optional
from xml.sax.saxutils import escape, unescape

REPO = Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
RES = REPO / "shared/src/commonMain/composeResources"

# Weblate-style language code -> values-* directory suffix, where they differ.
VALUES_DIR = {"en_GB": "en-rGB", "fr_CA": "fr-rCA", "pt_BR": "pt-rBR",
              "zh_Hans": "zh-rCN", "nb_NO": "nb", "id": "in"}
CODE_FOR_DIR = {v: k for k, v in VALUES_DIR.items()}

PLACEHOLDER_RE = re.compile(r"%\d+\$[a-zA-Z]")
# Compose Resources only unescapes \n, \t, \uXXXX and \\ - a \" or \' reaches the app as a
# literal backslash. Quotes and apostrophes go in bare.
ESCAPED_QUOTE_RE = re.compile(r"""\\["']""")

ELEMENT_RE = re.compile(
    r'<!--(?P<comment>.*?)-->'
    r'|<string name="(?P<skey>[^"]+)"(?P<sattrs>[^>]*)>(?P<stext>.*?)</string>'
    r'|<plurals name="(?P<pkey>[^"]+)"[^>]*>(?P<pbody>.*?)</plurals>',
    re.S,
)
ITEM_RE = re.compile(r'<item quantity="([^"]+)">(.*?)</item>', re.S)


def _load_stale_module():
    spec = importlib.util.spec_from_file_location(
        "find_stale", REPO / "scripts/find-stale-translations.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def values_path(code: Optional[str]) -> Path:
    if code is None:
        return RES / "values/strings.xml"
    return RES / f"values-{VALUES_DIR.get(code, code)}/strings.xml"


def all_codes() -> list[str]:
    dirs = sorted(p.parent.name[len("values-"):] for p in RES.glob("values-*/strings.xml"))
    return [CODE_FOR_DIR.get(d, d) for d in dirs]


def parse(path: Path) -> dict[str, dict[str, Any]]:
    """key -> {"value": str | {quantity: str}, "note": comment before it, "span": (start, end)}.

    A language with no file yet (one being added) parses as empty.
    """
    if not path.exists():
        return {}
    text = path.read_text()
    out: dict[str, dict[str, Any]] = {}
    note = ""
    for m in ELEMENT_RE.finditer(text):
        if m.group("comment") is not None:
            note = m.group("comment").strip()
            continue
        if m.group("skey"):
            if 'translatable="false"' not in m.group("sattrs"):
                out[m.group("skey")] = {"value": unescape(m.group("stext")), "note": note,
                                        "span": m.span()}
        else:
            items = {q: unescape(t) for q, t in ITEM_RE.findall(m.group("pbody"))}
            out[m.group("pkey")] = {"value": items, "note": note, "span": m.span()}
        note = ""
    return out


def dirty_files() -> list[str]:
    out = subprocess.run(["git", "status", "--porcelain", "--", str(RES)],
                         cwd=REPO, capture_output=True, text=True).stdout
    return [line[3:] for line in out.splitlines() if line.endswith("strings.xml")]


# --- languages / fetch -------------------------------------------------------------------

def pending(codes: list[str]) -> dict[str, dict[str, Any]]:
    """For each code: {"stale": {key: was}, "untranslated": [keys]}, acknowledgements applied."""
    fst = _load_stale_module()
    blobs = fst.BlobReader()
    english_now = blobs.parsed(f"HEAD:{fst.ENGLISH}")
    timeline = fst.EnglishTimeline(blobs)
    acks = fst.load_acks()
    result = {}
    for code in codes:
        d = VALUES_DIR.get(code, code)
        r = fst.stale_for(d, blobs, timeline, english_now)
        acked = acks.get(d, {})
        stale = {s["key"]: s["was"] for s in r["stale"]
                 if acked.get(s["key"]) != fst.fingerprint(s["now"])}
        result[code] = {"stale": stale, "untranslated": r["untranslated"]}
    return result


def cmd_languages(args: argparse.Namespace) -> None:
    codes = args.lang or all_codes()
    for code, p in pending(codes).items():
        print(f"{code:10s} untranslated={len(p['untranslated']):4d} stale={len(p['stale']):4d}")


def cmd_fetch(args: argparse.Namespace) -> None:
    dirty = dirty_files()
    if dirty:
        print("Uncommitted changes to translation files - commit them first, since the stale "
              "check reads git history:\n  " + "\n  ".join(dirty), file=sys.stderr)
        sys.exit(1)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    codes = all_codes() if args.all else args.lang
    if not codes:
        sys.exit("Pass --lang <code> or --all.")
    for code in codes:
        if not values_path(code).exists():
            print(f"{code}: no {values_path(code).relative_to(REPO)} yet - every string is "
                  "untranslated, and `apply` will create the file.")

    english = parse(values_path(None))
    work = pending(codes)
    for code in codes:
        todo_path = out_dir / f"{code}-untranslated.json"
        done_path = out_dir / f"{code}-translated.json"
        todo_path.unlink(missing_ok=True)
        done_path.unlink(missing_ok=True)
        if args.all and not (work[code]["stale"] or work[code]["untranslated"]):
            continue
        current = parse(values_path(code))
        todo, done = [], []
        for key, en in english.items():
            unit = {"context": key, "source": en["value"], "note": en["note"],
                    "target": current[key]["value"] if key in current else ""}
            if key in work[code]["untranslated"]:
                todo.append({**unit, "kind": "untranslated"})
            elif key in work[code]["stale"]:
                todo.append({**unit, "kind": "stale", "was": work[code]["stale"][key]})
            elif key in current:
                done.append(unit)
        todo_path.write_text(json.dumps(todo, ensure_ascii=False, indent=2))
        done_path.write_text(json.dumps(done, ensure_ascii=False, indent=2))
        n_stale = sum(1 for u in todo if u["kind"] == "stale")
        print(f"{code:10s} untranslated={len(todo) - n_stale:4d} stale={n_stale:4d} "
              f"translated={len(done)}")


# --- validate / apply --------------------------------------------------------------------

def plural_quantities(current: dict[str, dict[str, Any]]) -> Optional[set[str]]:
    """The quantities this language's plurals use, taken from one it already has."""
    for entry in current.values():
        if isinstance(entry["value"], dict) and entry["value"]:
            return set(entry["value"])
    return None


def check_text(key: str, source: str, value: str, errors: list[str]) -> None:
    if not value.strip():
        errors.append(f"{key}: empty translation")
        return
    src_ph, val_ph = PLACEHOLDER_RE.findall(source), PLACEHOLDER_RE.findall(value)
    for ph in sorted(set(src_ph)):
        if src_ph.count(ph) != val_ph.count(ph):
            errors.append(f"{key}: placeholder {ph} appears {val_ph.count(ph)}x, "
                          f"source has it {src_ph.count(ph)}x -> {value!r}")
    for ph in sorted(set(val_ph) - set(src_ph)):
        errors.append(f"{key}: placeholder {ph} is not in the source string -> {value!r}")
    if source.count("\\n") != value.count("\\n"):
        errors.append(f"{key}: line-break count differs from the source")
    if ESCAPED_QUOTE_RE.search(value):
        errors.append(f"{key}: escaped quote (write \" and ' without a backslash) -> {value!r}")


def validate(code: str, translations: Any, out_dir: Optional[Path], revise: bool,
             require_complete: bool) -> tuple[list[str], list[str]]:
    """(errors, warnings) for a {key: translation} object.

    Without `revise`, every key must be one of the language's pending units in `out_dir`, which
    is how a stale fetch cache shows up. With `revise`, any key with English source may be
    rewritten - the review skill's path for correcting translations that are not stale.
    """
    if not isinstance(translations, dict):
        return ["file is not a JSON object of {context-key: translation}"], []
    errors, warnings = [], []
    english = parse(values_path(None))
    current = parse(values_path(code))
    quantities = plural_quantities(current)

    if not revise:
        units_path = (out_dir or Path(".")) / f"{code}-untranslated.json"
        if not units_path.exists():
            sys.exit(f"Can't validate: {units_path} not found. Run `fetch --lang {code}` first, "
                     "or pass --revise for corrections to strings that are not pending.")
        expected = {u["context"] for u in json.loads(units_path.read_text())}
        for key in sorted(set(translations) - expected):
            errors.append(f"{key}: not pending in this language (already up to date, or stale cache)")
        missing = sorted(expected - set(translations))
        if missing:
            shown = ", ".join(missing[:5]) + (f" (+{len(missing) - 5} more)" if len(missing) > 5 else "")
            (errors if require_complete else warnings).append(
                f"{len(missing)} pending key(s) not in this file: {shown}")

    for key, value in sorted(translations.items()):
        if key not in english:
            errors.append(f"{key}: no such English string")
            continue
        source = english[key]["value"]
        if isinstance(source, dict):
            if not isinstance(value, dict):
                errors.append(f"{key}: is a plural - give an object of {{quantity: text}}")
                continue
            want = set(current[key]["value"]) if key in current else quantities
            if want is None:
                warnings.append(f"{key}: no existing plural to take this language's quantities "
                                f"from - check {sorted(value)} against CLDR by hand")
            elif set(value) != want:
                errors.append(f"{key}: plural quantities {sorted(value)} should be {sorted(want)}")
            ref = source.get("other") or next(iter(source.values()))
            for q, text in value.items():
                check_text(f"{key}[{q}]", source.get(q, ref), text, errors)
        elif not isinstance(value, str):
            errors.append(f"{key}: is not a plural - give a string")
        else:
            check_text(key, source, value, errors)
    return errors, warnings


def render(key: str, value: Any) -> str:
    if isinstance(value, dict):
        items = "".join(f'\n        <item quantity="{q}">{escape(t)}</item>' for q, t in value.items())
        return f'<plurals name="{key}">{items}\n    </plurals>'
    return f'<string name="{key}">{escape(value)}</string>'


def apply_language(code: str, translations: dict[str, Any]) -> tuple[int, int]:
    """Write translations into the language's file. Returns (replaced, added)."""
    path = values_path(code)
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n')
    english_order = list(parse(values_path(None)))
    replaced = added = 0
    # One key at a time, re-parsing each time, so every span refers to the current text.
    for key, value in translations.items():
        text = path.read_text()
        current = parse(path)
        if key in current:
            start, end = current[key]["span"]
            text = text[:start] + render(key, value) + text[end:]
            replaced += 1
        else:
            # After the nearest English predecessor this file has, so new strings land beside
            # their neighbours rather than all at the end.
            anchor = None
            for prior in reversed(english_order[:english_order.index(key)]):
                if prior in current:
                    anchor = current[prior]["span"][1]
                    break
            insert = "\n    " + render(key, value)
            if anchor is None:
                anchor = text.rindex("</resources>")
                insert = "    " + render(key, value) + "\n"
            text = text[:anchor] + insert + text[anchor:]
            added += 1
        path.write_text(text)
    return replaced, added


def jobs_from(args: argparse.Namespace) -> list[tuple[str, Path]]:
    if args.all:
        if not args.out_dir:
            sys.exit("--all needs --out-dir (where the *-translations.json files are).")
        jobs = [(f.name[:-len("-translations.json")], f)
                for f in sorted(Path(args.out_dir).glob("*-translations.json"))]
        if not jobs:
            sys.exit(f"No *-translations.json files in {args.out_dir}.")
        return jobs
    if not (args.lang and args.file):
        sys.exit("Pass --lang and --file, or --all with --out-dir.")
    return [(args.lang[0], Path(args.file))]


def run_validation(jobs, args) -> dict[str, list[str]]:
    failures = {}
    for code, file in jobs:
        out_dir = Path(args.out_dir) if args.out_dir else file.parent
        errors, warnings = validate(code, json.loads(file.read_text()), out_dir,
                                    args.revise, args.require_complete)
        for w in warnings:
            print(f"{code}: note: {w}")
        if errors:
            failures[code] = errors
    for code, errors in sorted(failures.items()):
        print(f"{code}: {len(errors)} problem(s):", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
    return failures


def cmd_validate(args: argparse.Namespace) -> None:
    jobs = jobs_from(args)
    if run_validation(jobs, args):
        sys.exit(1)
    for code, file in jobs:
        print(f"{code}: {len(json.loads(file.read_text()))} translation(s) OK")


def cmd_apply(args: argparse.Namespace) -> None:
    jobs = jobs_from(args)
    # Everything is checked before anything is written, so a bad file can't leave a batch
    # half-applied.
    if run_validation(jobs, args):
        print("Nothing applied.", file=sys.stderr)
        sys.exit(1)
    for code, file in jobs:
        replaced, added = apply_language(code, json.loads(file.read_text()))
        print(f"{code:10s} replaced={replaced} added={added}  {values_path(code).relative_to(REPO)}")
    print("\nApplied to the working tree. Commit to make the stale check see it.")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="command", required=True)

    p_lang = sub.add_parser("languages", help="Untranslated and stale counts per language.")
    p_lang.add_argument("--lang", nargs="+")
    p_lang.set_defaults(func=cmd_languages)

    p_fetch = sub.add_parser("fetch", help="Write the pending and translated units for languages.")
    p_fetch.add_argument("--lang", nargs="+")
    p_fetch.add_argument("--all", action="store_true", help="Every language with pending work.")
    p_fetch.add_argument("--out-dir", default=".")
    p_fetch.set_defaults(func=cmd_fetch)

    for name, func, help_text in (
        ("validate", cmd_validate, "Check {key: translation} files without applying them."),
        ("apply", cmd_apply, "Validate, then write translations into values-*/strings.xml."),
    ):
        sp = sub.add_parser(name, help=help_text)
        sp.add_argument("--lang", nargs=1)
        sp.add_argument("--file")
        sp.add_argument("--all", action="store_true", help="Every <code>-translations.json in --out-dir.")
        sp.add_argument("--out-dir", help="Where the fetch cache and translation files are.")
        sp.add_argument("--revise", action="store_true",
                        help="Allow any key with English source, not just pending ones (review fixes).")
        sp.add_argument("--require-complete", action="store_true",
                        help="Also fail if a pending key is missing from the file.")
        sp.set_defaults(func=func)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
