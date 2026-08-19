---
name: weblate-translate
description: Translate Soundscape-Android's untranslated Weblate strings (androidkmp component) directly in-session and upload the results back to Weblate. Use when the user asks to translate untranslated/missing/unfinished strings, sync translations with Weblate, or run the "weblate-translate-unfinished" workflow.
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

1. Run `python3 .claude/skills/weblate-translate/scripts/weblate_sync.py languages`
   to see every language and its untranslated count. Build the list of
   languages to process (all with untranslated > 0, or the caller's subset).
   If none have untranslated strings, say so and stop.

2. For each language, in turn:

   a. Fetch its units:
      ```
      python3 .claude/skills/weblate-translate/scripts/weblate_sync.py fetch --lang <code> --out-dir /tmp/weblate-translate
      ```
      This writes `<code>-untranslated.json` and `<code>-translated.json`.
      Skip the language if `-untranslated.json` is empty.

   b. Read both files. `<code>-translated.json` is prior art for this
      language — use it to keep terminology and tone consistent (e.g. how
      "Audio Beacon", "Marker", "Waypoint" etc. have already been rendered).

   c. Read `docs/developers/translations.md` and
      `docs/developers/translation-terminology.md` from this repo for app
      context and the canonical meaning of Soundscape-specific terms — this
      replaces the external `soundscape-description.md` the old script used.
      If the user points you at a glossary file for this language (only a
      few exist), load it too and prefer its terms.

   d. Translate the untranslated units yourself, from US English into the
      target language, in batches of roughly 25-30 units at a time (smaller
      batches are easier to sanity-check and keep consistent). For each
      unit:
      - `source` is the English text to translate; `note`/`context` give
        translator context from Weblate (comments on the string, i.e. where
        it's used) — use them.
      - Preserve markdown formatting and line breaks exactly.
      - Preserve placeholders exactly and don't reorder/drop them (e.g.
        `%1$s`, `%2$d`) — these are positional Android string format args.
      - Where no direct translation exists, prefer a clear, concise,
        contextually appropriate phrase over a literal one, keeping in mind
        this is an audio-first app for blind/low-vision users (see
        `docs/developers/translations.md`).
      - Build a JSON object mapping each unit's `context` (its string key)
        to your translated text: `{"<context>": "<translated text>", ...}`.
        This is the exact shape Weblate's upload endpoint expects.

   e. Write the accumulated `{context: translation}` object for the whole
      language to `/tmp/weblate-translate/<code>-translations.json`.

   f. Upload it:
      ```
      python3 .claude/skills/weblate-translate/scripts/weblate_sync.py upload --lang <code> --file /tmp/weblate-translate/<code>-translations.json
      ```
      This lands as the direct translation (replacing any existing
      "translated" state for those keys), matching the old script's
      behavior — there is no suggestion/review queue here, so translation
      quality matters.

3. After all languages are done, report a short summary table: language,
   how many strings were translated/uploaded, and any language skipped or
   that failed to upload (print the script's error and move on to the next
   language rather than aborting the whole run).

## Notes

- Component is fixed to `androidkmp`
  (`shared/src/commonMain/composeResources/values-*/strings.xml`). Don't use
  the `android-app` component in the same Weblate project — it points at a
  `strings.xml` path that no longer exists in this repo (pre-KMP-migration,
  stale).
- Never print or log the Weblate API key. It's only ever read by
  `weblate_sync.py` from `~/.config/weblate`.
- If `weblate_sync.py` isn't runnable (e.g. `wlc` not installed), tell the
  user to `pip install wlc` rather than trying to reimplement its HTTP calls
  inline.
