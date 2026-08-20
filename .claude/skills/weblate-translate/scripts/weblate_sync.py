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
import sys
from pathlib import Path
from typing import Any, Optional
from urllib.parse import urlparse

try:
    import wlc
    from wlc.config import WeblateConfig
except ImportError:
    print("Please `pip install wlc` first.", file=sys.stderr)
    sys.exit(1)

PROJECT = "soundscape-android"
COMPONENT = "androidkmp"


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
        data = c.get(path)
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
        data = c.get(path)
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


def cmd_fetch(args: argparse.Namespace) -> None:
    c = client()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    untranslated = get_units(c, args.lang, "is:untranslated")
    translated = get_units(c, args.lang, "is:translated")

    untranslated_path = out_dir / f"{args.lang}-untranslated.json"
    translated_path = out_dir / f"{args.lang}-translated.json"
    untranslated_path.write_text(json.dumps(untranslated, ensure_ascii=False, indent=2))
    translated_path.write_text(json.dumps(translated, ensure_ascii=False, indent=2))

    print(f"Wrote {len(untranslated)} untranslated units -> {untranslated_path}")
    print(f"Wrote {len(translated)} translated units -> {translated_path}")


def cmd_upload(args: argparse.Namespace) -> None:
    c = client()
    translations = json.loads(Path(args.file).read_text())
    if not isinstance(translations, dict):
        print("Upload file must be a JSON object of {context-key: translated-text}.", file=sys.stderr)
        sys.exit(1)

    path = f"translations/{PROJECT}/{COMPONENT}/{args.lang}/file/"
    files = {"file": open(args.file, "rb")}
    # These must go in as POST kwargs (-> wlc sends them as multipart form
    # fields alongside `files`), NOT as `params=` (URL query string) - the
    # Weblate API only reads method/conflicts/author/email from the form
    # body, so passing them as query params makes `conflicts` silently
    # fall back to its default and already-translated strings get skipped.
    result = c.post(
        path,
        files=files,
        conflicts="replace-translated",
        email=args.email,
        author=args.author,
        method="translate",
    )
    print(f"Uploaded {len(translations)} translations for {args.lang}:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


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
    p_fetch.add_argument("--lang", required=True, help="Weblate language code, e.g. de, fr_CA, zh_Hans.")
    p_fetch.add_argument("--out-dir", default=".", help="Directory to write cache JSON into.")
    p_fetch.set_defaults(func=cmd_fetch)

    p_upload = sub.add_parser("upload", help="Upload a {key: translation} JSON file as the live translation.")
    p_upload.add_argument("--lang", required=True, help="Weblate language code.")
    p_upload.add_argument("--file", required=True, help="Path to a JSON object of {context-key: translated-text}.")
    p_upload.add_argument("--author", default=git_config("user.name", "Claude"))
    p_upload.add_argument("--email", default=git_config("user.email", "noreply@anthropic.com"))
    p_upload.set_defaults(func=cmd_upload)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
