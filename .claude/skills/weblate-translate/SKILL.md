---
name: weblate-translate
description: Translate Soundscape-Android's untranslated Weblate strings (androidkmp component) directly in-session and upload the results back to Weblate. Use when the user asks to translate untranslated/missing/unfinished strings, sync translations with Weblate, or run the "weblate-translate-unfinished" workflow — for a language the app doesn't support at all yet, use [[weblate-add-language]] instead.
---

# Weblate translate-unfinished

Replaces the external `weblate-translate-unfinished.py` (OpenAI-based) workflow:
you do the translating yourself, in-session, instead of calling an LLM API.
`scripts/weblate_sync.py` only handles the deterministic Weblate HTTP calls
(auth, pagination, upload) — it never talks to any translation API.

## One-time setup (tell the user if this fails)

Auth comes from `wlc`'s standard config path, `~/.config/weblate`:

```ini
[keys]
https://hosted.weblate.org/api/ = <weblate-api-token>
```

`chmod 600 ~/.config/weblate`. If `scripts/weblate_sync.py languages` reports
no API key, stop and tell the user to add it there — never ask for the key
in chat or write it into any file in this repo.

## Args

Optional language codes narrow scope, e.g. `/weblate-translate de fr`. With
no args, process every language `languages` reports as having untranslated
units. Language codes are Weblate's (e.g. `de`, `fr_CA`, `zh_Hans`, `en_GB`),
matching what `languages` prints.

## Procedure

Work in a **fresh, empty** output directory — `/tmp/weblate-translate` is
reused across runs and a previous run's cache files there are
indistinguishable from this run's. `fetch` deletes its own outputs before
writing, so a failed fetch leaves nothing behind, but files for languages you
never fetched will still be sitting there looking current.

1. Run `python3 .claude/skills/weblate-translate/scripts/weblate_sync.py languages`
   to see every language and its untranslated count. If none have untranslated
   strings, say so and stop.

2. Fetch. For a whole-component run use `--all`, which fetches exactly the
   languages that have untranslated strings and paces itself:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py fetch --all --out-dir <dir>
   ```
   For the caller's subset, one `fetch --lang <code> --out-dir <dir>` each.
   Either way it exits non-zero and names the languages if any failed — do not
   carry on translating until every language you intend to upload fetched
   cleanly. Don't write your own shell loop around `fetch`: `--all` exists
   because a bare loop hides failures and trips Weblate's rate limiter.

3. Read `docs/developers/translations.md` and
   `docs/developers/translation-terminology.md` for app context and the
   canonical meaning of Soundscape-specific terms. If the user points you at a
   glossary file for this language (only a few exist), load it too and prefer
   its terms.

4. For each language, translate the units in `<code>-untranslated.json`:

   - `source` is the English text; `note`/`context` are the translator
     comments from Weblate saying where the string is used — read them.
   - **Anchor to prior art rather than translating cold.** `<code>-translated.json`
     holds every string already approved in that language. Before translating,
     find the sibling strings that share wording with the new one and reuse
     their choices — it keeps terminology consistent and settles questions the
     English can't. Real examples: a new `%1$s, %2$s` joiner should copy the
     existing `%1$s, %2$s` string verbatim, which is what preserves the Arabic
     `،`, Japanese `、` and Chinese `，` separators instead of an ASCII comma;
     an "X next to Y" string should take its preposition from the existing
     "Sidewalk next to %1$s". Grep the file for the distinctive English word.
   - Head-final languages usually need the clause restructured, not just the
     words swapped: Japanese `%2$s 沿いの%1$s`, Turkish `%2$s yanındaki %1$s`,
     Korean `%2$s 옆 %1$s`. Follow whatever order the sibling strings use.
   - If two new strings would collapse onto the same word (e.g. "near" and
     "next to" both rendering as Romanian `lângă`), pick a distinct term for
     one of them — they are separate strings because the app distinguishes them.
   - Preserve markdown, line breaks, and placeholders (`%1$s`, `%2$d`) exactly.
     Placeholders are positional: dropping one crashes at format time and
     swapping two silently transposes the arguments.
   - Where no direct translation exists, prefer a clear, concise, contextually
     appropriate phrase over a literal one, remembering this is an audio-first
     app for blind and low-vision users.

   Write `{context-key: translated-text}` to
   `<dir>/<code>-translations.json` — the shape Weblate's upload endpoint
   expects. Translate in batches of roughly 25-30 units so each batch stays
   checkable.

5. Validate before uploading anything:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py validate --lang <code> --file <dir>/<code>-translations.json
   ```
   This checks that placeholders and line breaks survived, that nothing is
   empty, and that every key really is one of the language's untranslated
   ones — a key that isn't is how a stale fetch cache shows up. Uploading only
   part of a language is fine and reported as a note rather than an error, so
   the batched whole-language runs [[weblate-add-language]] does still pass;
   add `--require-complete` when a file is meant to cover everything.

   `upload` runs the same checks itself and uploads nothing if any language
   fails, so this step is only for catching problems early — never reach for
   `--skip-validate` to get past a failure.

6. Upload. Uploads land as **live translations** with no suggestion or review
   queue, so quality matters and there is no undo:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py upload --all --out-dir <dir>
   ```
   `--all` validates every language first, uploads none if any fails, then
   paces the uploads. For a single language, `upload --lang <code> --file <path>`.

7. Confirm by re-running `languages` — the counts you translated should now
   read `untranslated=0`. Then report a short summary table: language, how many
   strings were uploaded, and anything skipped or failed.

Tell the user the Weblate side is done and the remaining steps are theirs:
Commit in the Weblate UI, then merge the translation branch back into the repo
(see `docs/developers/translations.md`).

## Notes

- Component is fixed to `androidkmp`
  (`shared/src/commonMain/composeResources/values-*/strings.xml`). Don't use
  the `android-app` component in the same Weblate project — it points at a
  `strings.xml` path that no longer exists in this repo (pre-KMP-migration,
  stale).
- hosted.weblate.org throttles bursts hard. Requests already retry with
  backoff and the `--all` loops pause between languages; if you still see
  throttling, raise `--pause`. Firing ~50 languages back to back fails most of
  them.
- Never print or log the Weblate API key. It's only ever read by
  `weblate_sync.py` from `~/.config/weblate`.
- If `weblate_sync.py` isn't runnable (e.g. `wlc` not installed), tell the
  user to `pip install wlc` rather than trying to reimplement its HTTP calls
  inline.
