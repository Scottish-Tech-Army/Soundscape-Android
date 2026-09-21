#!/usr/bin/env python3
"""
Deterministic Weblate I/O for the weblate-translate skill: list languages,
fetch untranslated/translated units, and upload finished translations.

Auth comes from wlc's standard config search path (e.g. ~/.config/weblate),
in the same [keys] format used by the `wlc` CLI:

    [keys]
    https://hosted.weblate.org/api/ = <token>

No API keys are ever read from or written to this repo.

This intentionally does NOT call any translation/LLM API - the translating
is done by Claude in the skill itself. This script only knows how to talk to
Weblate.
"""
import argparse
import json
import re
import sys
import time
from pathlib import Path
from typing import Any, Callable, Optional
from urllib.parse import urlparse

try:
    import wlc
    from wlc.config import WeblateConfig
except ImportError:
    print("Please `pip install wlc` first.", file=sys.stderr)
    sys.exit(1)

PROJECT = "soundscape-android"
COMPONENT = "androidkmp"

# hosted.weblate.org throttles bursts of requests hard. A whole-component run is
# ~50 languages x 2 paginated queries, which trips it within a few seconds if
# fired back to back, so requests retry with backoff and whole-language loops
# pause between languages. These defaults were arrived at empirically: no pause
# fails roughly 40 of 46 languages, ~10s between languages fails none.
RETRIES = 4
BACKOFF_SECONDS = 20
PAUSE_SECONDS = 10

# Android positional format specifiers, e.g. %1$s, %2$d. A translation has to
# carry exactly the same ones as its source: dropping or duplicating one throws
# at format time, and reordering them silently swaps the arguments round.
PLACEHOLDER_RE = re.compile(r"%\d+\$[a-zA-Z]")


def client() -> wlc.Weblate:
    cfg = WeblateConfig()
    cfg.load()
    c = wlc.Weblate(config=cfg)
    if not c.key:
        print(
            "No Weblate API key found. Add one to ~/.config/weblate, e.g.:\n"
            "  [keys]\n"
            f"  {c.url} = <token>\n",
            file=sys.stderr,
        )
        sys.exit(1)
    return c


def with_retry(what: str, call: Callable[[], Any]) -> Any:
    """Run a Weblate request, retrying throttling and transient server errors.

    Auth failures are not retried - they will never succeed, and burning four
    backoffs on them only obscures the real problem.
    """
    for attempt in range(1, RETRIES + 1):
        try:
            return call()
        except (wlc.WeblateDeniedError, wlc.WeblatePermissionError):
            raise
        except (wlc.WeblateThrottlingError, wlc.WeblateException, OSError) as exc:
            if attempt == RETRIES:
                raise
            delay = BACKOFF_SECONDS * attempt
            print(
                f"  {what}: {type(exc).__name__}: {exc} - retry {attempt}/{RETRIES - 1} in {delay}s",
                file=sys.stderr,
            )
            time.sleep(delay)


def _to_path(base_url: str, full_url: Optional[str]) -> Optional[str]:
    """Convert a full URL from a Weblate `next` field into a path for client.get()."""
    if not full_url:
        return None
    if full_url.startswith(base_url):
        return full_url[len(base_url):]
    parsed = urlparse(full_url)
    path = parsed.path.lstrip("/")
    if parsed.query:
        path += "?" + parsed.query
    return path


def cmd_languages(args: argparse.Namespace) -> None:
    c = client()
    base_url = c.url
    path = f"components/{PROJECT}/{COMPONENT}/translations/"
    rows = []
    while path:
        data = with_retry("languages", lambda: c.get(path))
        for t in data["results"]:
            if t.get("is_source"):
                continue
            lang = t["language"]
            untranslated = t["total"] - t["translated"]
            rows.append((lang["code"], lang["name"], untranslated, t["total"]))
        path = _to_path(base_url, data.get("next"))

    rows.sort(key=lambda r: r[0])
    if args.json:
        print(json.dumps(
            [{"code": c_, "name": n, "untranslated": u, "total": t} for c_, n, u, t in rows],
            ensure_ascii=False, indent=2,
        ))
    else:
        for code, name, untranslated, total in rows:
            print(f"{code:10s} {name:30s} untranslated={untranslated:4d} total={total}")


def get_units(c: wlc.Weblate, language_code: str, query: str) -> list[dict[str, Any]]:
    """Return units for translations/{PROJECT}/{COMPONENT}/{language_code}/units/?q={query}."""
    base_url = c.url
    path = f"translations/{PROJECT}/{COMPONENT}/{language_code}/units/?q={query}"
    units = []
    while path:
        data = with_retry(f"{language_code} {query}", lambda p=path: c.get(p))
        for unit in data["results"]:
            source_list = unit.get("source") or [""]
            target_list = unit.get("target") or [""]
            units.append({
                "id": unit["id"],
                "source": source_list[0],
                "target": target_list[0],
                "context": unit.get("context"),
                "note": unit.get("note"),
            })
        path = _to_path(base_url, data.get("next"))
    return units


def unit_paths(out_dir: Path, lang: str) -> tuple[Path, Path]:
    return out_dir / f"{lang}-untranslated.json", out_dir / f"{lang}-translated.json"


def fetch_language(c: wlc.Weblate, lang: str, out_dir: Path) -> tuple[int, int]:
    """Fetch one language's units, leaving no stale output behind on failure.

    The cache files are deleted up front rather than overwritten at the end.
    A previous run's files for the same language are indistinguishable from a
    fresh fetch once written, so a failure that left them in place would be read
    as current data - which is exactly how a run against a stale cache produced
    translations for strings that were no longer the untranslated ones.
    """
    untranslated_path, translated_path = unit_paths(out_dir, lang)
    untranslated_path.unlink(missing_ok=True)
    translated_path.unlink(missing_ok=True)

    untranslated = get_units(c, lang, "is:untranslated")
    translated = get_units(c, lang, "is:translated")

    untranslated_path.write_text(json.dumps(untranslated, ensure_ascii=False, indent=2))
    translated_path.write_text(json.dumps(translated, ensure_ascii=False, indent=2))
    return len(untranslated), len(translated)


def languages_with_untranslated(c: wlc.Weblate) -> list[str]:
    base_url = c.url
    path = f"components/{PROJECT}/{COMPONENT}/translations/"
    codes = []
    while path:
        data = with_retry("languages", lambda p=path: c.get(p))
        for t in data["results"]:
            if t.get("is_source"):
                continue
            if t["total"] - t["translated"] > 0:
                codes.append(t["language"]["code"])
        path = _to_path(base_url, data.get("next"))
    return sorted(codes)


def cmd_fetch(args: argparse.Namespace) -> None:
    c = client()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    if args.all:
        langs = languages_with_untranslated(c)
        if not langs:
            print("Nothing to fetch - no language has untranslated strings.")
            return
        print(f"Fetching {len(langs)} languages with untranslated strings.")
    else:
        if not args.lang:
            print("Pass --lang <code> or --all.", file=sys.stderr)
            sys.exit(1)
        langs = [args.lang]

    failed = []
    for i, lang in enumerate(langs):
        try:
            n_untranslated, n_translated = fetch_language(c, lang, out_dir)
            print(f"{lang:10s} untranslated={n_untranslated:4d} translated={n_translated}")
        except Exception as exc:
            failed.append(lang)
            print(f"{lang:10s} FAILED: {type(exc).__name__}: {exc}", file=sys.stderr)
        if i < len(langs) - 1:
            time.sleep(args.pause)

    if failed:
        print(f"\nFailed to fetch: {' '.join(failed)}", file=sys.stderr)
        print("No cache files were left behind for these - re-run for them before translating.",
              file=sys.stderr)
        sys.exit(1)


def validate_translations(
    translations: dict[str, str], units: list[dict[str, Any]], require_complete: bool = False
) -> tuple[list[str], list[str]]:
    """Check a {key: translation} object against the units it claims to translate.

    Returns (errors, warnings). Errors block the upload; warnings are reported
    and allowed through.

    Keys in the file that are *not* currently untranslated are an error: that
    means the file and the fetch cache disagree about what needs translating,
    which is what a stale cache looks like. Untranslated keys missing from the
    file are only a warning, because uploading a language in batches (as a
    brand-new language must be) is a normal, intended way to work - pass
    require_complete when the file is meant to cover everything.
    """
    errors: list[str] = []
    warnings: list[str] = []
    if not isinstance(translations, dict):
        return (["file is not a JSON object of {context-key: translated-text}"], [])

    by_context = {u["context"]: u for u in units}
    expected = set(by_context)
    got = set(translations)

    for key in sorted(got - expected):
        errors.append(f"{key}: not untranslated in this language (already translated, or stale cache)")
    missing = sorted(expected - got)
    if missing:
        target = errors if require_complete else warnings
        shown = ", ".join(missing[:5]) + (f" (+{len(missing) - 5} more)" if len(missing) > 5 else "")
        target.append(f"{len(missing)} untranslated key(s) not in this file: {shown}")

    for key in sorted(got & expected):
        value = translations[key]
        if not isinstance(value, str) or not value.strip():
            errors.append(f"{key}: empty translation")
            continue
        source = by_context[key].get("source") or ""
        for ph in sorted(set(PLACEHOLDER_RE.findall(source))):
            want, have = source.count(ph), value.count(ph)
            if want != have:
                errors.append(
                    f"{key}: placeholder {ph} appears {have}x, source has it {want}x -> {value!r}"
                )
        for ph in sorted(set(PLACEHOLDER_RE.findall(value)) - set(PLACEHOLDER_RE.findall(source))):
            errors.append(f"{key}: placeholder {ph} is not in the source string -> {value!r}")
        if source.count("\n") != value.count("\n"):
            errors.append(f"{key}: line-break count differs from the source")
    return errors, warnings


def load_for_validation(lang: str, file: str, out_dir: Optional[str]) -> tuple[dict, list]:
    translations = json.loads(Path(file).read_text())
    base = Path(out_dir) if out_dir else Path(file).parent
    units_path = base / f"{lang}-untranslated.json"
    if not units_path.exists():
        print(
            f"Can't validate: {units_path} not found. Run `fetch --lang {lang}` first "
            f"(or pass --out-dir where the fetch cache lives).",
            file=sys.stderr,
        )
        sys.exit(1)
    return translations, json.loads(units_path.read_text())


def cmd_validate(args: argparse.Namespace) -> None:
    translations, units = load_for_validation(args.lang, args.file, args.out_dir)
    errors, warnings = validate_translations(translations, units, args.require_complete)
    for w in warnings:
        print(f"{args.lang}: note: {w}")
    if errors:
        print(f"{args.lang}: {len(errors)} problem(s):", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        sys.exit(1)
    print(f"{args.lang}: {len(translations)} translation(s) OK")


def cmd_add_language(args: argparse.Namespace) -> None:
    c = client()
    component = c.get_component(f"{PROJECT}/{COMPONENT}")
    result = component.add_translation(args.lang)
    print(f"Added language '{args.lang}' to {COMPONENT}:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


def cmd_create_language(args: argparse.Namespace) -> None:
    c = client()
    plural = {"number": args.plural_number, "formula": args.plural_formula}
    result = c.create_language(args.code, args.name, direction=args.direction, plural=plural)
    print(f"Created language '{args.code}' in Weblate's language database:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


def upload_language(c: wlc.Weblate, lang: str, file: str, author: str, email: str) -> int:
    translations = json.loads(Path(file).read_text())
    path = f"translations/{PROJECT}/{COMPONENT}/{lang}/file/"

    def post():
        # Reopened per attempt: a retried request needs the handle back at the
        # start of the file, and a spent handle uploads zero bytes.
        with open(file, "rb") as fh:
            # These must go in as POST kwargs (-> wlc sends them as multipart form
            # fields alongside `files`), NOT as `params=` (URL query string) - the
            # Weblate API only reads method/conflicts/author/email from the form
            # body, so passing them as query params makes `conflicts` silently
            # fall back to its default and already-translated strings get skipped.
            return c.post(
                path,
                files={"file": fh},
                conflicts="replace-translated",
                email=email,
                author=author,
                method="translate",
            )

    with_retry(f"upload {lang}", post)
    return len(translations)


def cmd_upload(args: argparse.Namespace) -> None:
    c = client()
    out_dir = Path(args.out_dir) if args.out_dir else None

    if args.all:
        if not out_dir:
            print("--all needs --out-dir (where the *-translations.json files are).", file=sys.stderr)
            sys.exit(1)
        jobs = [
            (f.name.replace("-translations.json", ""), str(f))
            for f in sorted(out_dir.glob("*-translations.json"))
        ]
        if not jobs:
            print(f"No *-translations.json files in {out_dir}.", file=sys.stderr)
            sys.exit(1)
    else:
        if not (args.lang and args.file):
            print("Pass --lang and --file, or --all with --out-dir.", file=sys.stderr)
            sys.exit(1)
        jobs = [(args.lang, args.file)]

    # Validate everything before uploading anything. Uploads land as live
    # translations with no review queue, so a batch that is going to be
    # rejected halfway is better caught before any of it is published.
    if not args.skip_validate:
        all_errors = {}
        for lang, file in jobs:
            translations, units = load_for_validation(lang, file, str(out_dir) if out_dir else None)
            errors, warnings = validate_translations(translations, units, args.require_complete)
            for w in warnings:
                print(f"{lang}: note: {w}")
            if errors:
                all_errors[lang] = errors
        if all_errors:
            print("Refusing to upload - validation failed:", file=sys.stderr)
            for lang, errors in sorted(all_errors.items()):
                print(f"  {lang}:", file=sys.stderr)
                for e in errors:
                    print(f"    - {e}", file=sys.stderr)
            sys.exit(1)
        print(f"Validated {len(jobs)} language(s).")

    failed = []
    for i, (lang, file) in enumerate(jobs):
        try:
            n = upload_language(c, lang, file, args.author, args.email)
            print(f"{lang:10s} uploaded {n} translation(s)")
        except Exception as exc:
            failed.append(lang)
            print(f"{lang:10s} FAILED: {type(exc).__name__}: {exc}", file=sys.stderr)
        if i < len(jobs) - 1:
            time.sleep(args.pause)

    print(f"\nUploaded {len(jobs) - len(failed)}/{len(jobs)} language(s).")
    if failed:
        print(f"Failed: {' '.join(failed)}", file=sys.stderr)
        sys.exit(1)


def git_config(key: str, default: str) -> str:
    import subprocess
    try:
        out = subprocess.run(
            ["git", "config", key], capture_output=True, text=True, check=True
        ).stdout.strip()
        return out or default
    except Exception:
        return default


def main() -> None:
    p = argparse.ArgumentParser(description="Weblate I/O helper for the weblate-translate skill.")
    sub = p.add_subparsers(dest="command", required=True)

    p_lang = sub.add_parser("languages", help="List languages and their untranslated counts.")
    p_lang.add_argument("--json", action="store_true")
    p_lang.set_defaults(func=cmd_languages)

    p_fetch = sub.add_parser("fetch", help="Fetch untranslated + translated units for a language.")
    p_fetch.add_argument("--lang", help="Weblate language code, e.g. de, fr_CA, zh_Hans.")
    p_fetch.add_argument("--all", action="store_true",
                         help="Fetch every language that has untranslated strings.")
    p_fetch.add_argument("--out-dir", default=".", help="Directory to write cache JSON into.")
    p_fetch.add_argument("--pause", type=float, default=PAUSE_SECONDS,
                         help="Seconds between languages (throttling protection).")
    p_fetch.set_defaults(func=cmd_fetch)

    p_validate = sub.add_parser(
        "validate",
        help="Check a {key: translation} file against the language's untranslated units.")
    p_validate.add_argument("--lang", required=True)
    p_validate.add_argument("--file", required=True)
    p_validate.add_argument("--out-dir", help="Where the fetch cache lives (default: alongside --file).")
    p_validate.add_argument("--require-complete", action="store_true",
                            help="Also fail if any untranslated key is missing (not a partial batch).")
    p_validate.set_defaults(func=cmd_validate)

    p_addlang = sub.add_parser("add-language", help="Add an existing Weblate language as a new translation of the component.")
    p_addlang.add_argument("--lang", required=True, help="Weblate language code, e.g. et, nn_NO, cy.")
    p_addlang.set_defaults(func=cmd_add_language)

    p_createlang = sub.add_parser("create-language", help="Define a language Weblate doesn't know at all yet (rare).")
    p_createlang.add_argument("--code", required=True, help="New Weblate language code.")
    p_createlang.add_argument("--name", required=True, help="Language name (in English), e.g. 'Scottish Gaelic'.")
    p_createlang.add_argument("--direction", default="ltr", choices=["ltr", "rtl"])
    p_createlang.add_argument("--plural-number", type=int, default=2, help="Number of plural forms.")
    p_createlang.add_argument("--plural-formula", default="n != 1", help="CLDR-style plural formula.")
    p_createlang.set_defaults(func=cmd_create_language)

    p_upload = sub.add_parser("upload", help="Upload a {key: translation} JSON file as the live translation.")
    p_upload.add_argument("--lang", help="Weblate language code.")
    p_upload.add_argument("--file", help="Path to a JSON object of {context-key: translated-text}.")
    p_upload.add_argument("--all", action="store_true",
                          help="Upload every <code>-translations.json in --out-dir.")
    p_upload.add_argument("--out-dir", help="Directory holding the translation and fetch-cache files.")
    p_upload.add_argument("--pause", type=float, default=PAUSE_SECONDS,
                          help="Seconds between languages (throttling protection).")
    p_upload.add_argument("--require-complete", action="store_true",
                          help="Also fail if any untranslated key is missing (not a partial batch).")
    p_upload.add_argument("--skip-validate", action="store_true",
                          help="Upload without checking keys/placeholders first. Not advised.")
    p_upload.add_argument("--author", default=git_config("user.name", "Claude"))
    p_upload.add_argument("--email", default=git_config("user.email", "noreply@anthropic.com"))
    p_upload.set_defaults(func=cmd_upload)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
