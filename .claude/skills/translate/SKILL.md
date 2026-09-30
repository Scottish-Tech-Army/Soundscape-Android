---
name: translate
description: Translate Soundscape-Android's untranslated and stale strings directly in the repo's shared/composeResources values-*/strings.xml files, in-session, then commit. Use when the user asks to translate untranslated/missing/unfinished strings, bring translations up to date after an English change, or run a translation pass — for a language the app doesn't support at all yet, use [[add-language]] instead.
---

# Translate untranslated and stale strings

You do the translating yourself, in-session, and write the results straight into
`shared/src/commonMain/composeResources/values-<lang>/strings.xml`. Weblate is no longer
in the loop: the repo is the source of truth, and translations land as ordinary commits.

`scripts/strings_sync.py` handles the deterministic file work — finding what needs
translating, validating, and writing the XML. It never translates anything.

What needs work comes from `scripts/find-stale-translations.py`, the repo's replacement
for Weblate's "needs editing": a string is **untranslated** if the language has no entry
for it, and **stale** if its English changed after the translation was last changed.

## Args

Optional language codes narrow scope, e.g. `/translate de fr`. With no args,
process every language `languages` reports as having pending work. Codes are the ones
`translations/guidance/` uses (`de`, `fr_CA`, `zh_Hans`, `en_GB`, `nb_NO`, `id`);
`strings_sync.py` maps them to the `values-*` directories.

## Procedure

Work in a **fresh, empty** output directory — a previous run's files are
indistinguishable from this run's. `fetch` deletes its own outputs before writing.

1. Run `python3 .claude/skills/translate/scripts/strings_sync.py languages`
   to see every language's untranslated and stale counts. If everything is 0, say so
   and stop.

2. Fetch:
   ```
   python3 .claude/skills/translate/scripts/strings_sync.py fetch --all --out-dir <dir>
   ```
   or `fetch --lang <code> [<code> ...] --out-dir <dir>` for a subset. It refuses to run
   while translation files have uncommitted changes, because the stale check reads git
   history — commit or stash them first.

   It writes, per language, `<code>-untranslated.json` (the work: every pending unit,
   with `kind` = `untranslated` or `stale`) and `<code>-translated.json` (everything
   already translated and up to date, for anchoring).

3. Read `docs/developers/translations.md` and
   `docs/developers/translation-terminology.md` for app context and the
   canonical meaning of Soundscape-specific terms. The alternate wordings in
   the terminology doc are English glosses to explain each concept — not a
   shortlist to translate from; prefer the word the target language's own
   mapping apps already use.

   Then load `translations/guidance/_common.md` and
   `translations/guidance/<code>.md` if it exists — recorded native-speaker
   decisions for that language. Any term with status `confirmed` or `agreed`
   is binding: use it verbatim (in the right case/inflection) rather than
   coining your own. This is the point of the file — new strings should be
   born consistent with what a native speaker already approved, instead of
   being corrected in a later review round.

4. For each language, translate the units in `<code>-untranslated.json`:

   - `source` is the English text; `note` is the translator comment from the English
     `strings.xml`, saying where the string is used — read it.
   - **Stale units** (`kind: "stale"`) have a `target` (the current translation) and `was`
     (the English it was translated from). Compare `was` with `source` to see what
     changed, then produce the **whole** new translation (rule C16) — keep the existing
     translation's choices wherever the English didn't change. If the change needs
     nothing from this language (an English typo fix, say), don't rewrite it:
     acknowledge it instead, with
     `python3 scripts/find-stale-translations.py --lang <code> --acknowledge <key> ...`.
   - **Plural units** have `source`/`target` objects of `{quantity: text}`. Translate every
     quantity the language uses (the same set as its other plurals) — see
     [[compose-plurals-select-on-int-only]].
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

   Write `{context-key: translated-text}` to `<dir>/<code>-translations.json` (a plural's
   value is an object of `{quantity: text}`). Translate in batches of roughly 25-30 units
   so each batch stays checkable.

5. Validate:
   ```
   python3 .claude/skills/translate/scripts/strings_sync.py validate --lang <code> --file <dir>/<code>-translations.json --out-dir <dir>
   ```
   This checks placeholders, line breaks, escaped quotes, empty values and plural
   quantities, and that every key really is pending for the language — a key that isn't
   is how a stale fetch cache shows up. A partial file is fine and reported as a note;
   add `--require-complete` when a file is meant to cover everything.

6. Apply, which validates again and writes nothing if any language fails:
   ```
   python3 .claude/skills/translate/scripts/strings_sync.py apply --all --out-dir <dir>
   ```
   or `apply --lang <code> --file <path> --out-dir <dir>`. Existing entries are replaced
   in place; new ones are inserted after their nearest English neighbour.

7. Check the diff (`git diff --stat`, and spot-read a few languages), then run the
   truncation check from the Notes. Commit on main, one commit for the pass, describing
   what was translated. Don't push — the user does that ([[commit-directly-on-main]],
   [[never-push-upstream]]).

8. Confirm with `strings_sync.py languages`: the languages you did should now read 0
   (the stale check only sees committed changes). Report a short table: language,
   untranslated/stale strings done, anything skipped or acknowledged.

## Notes

- **Translate the whole string, never just the part that changed** (rule C16 in
  `translations/guidance/_common.md`). After a bulk pass, run
  `python3 .claude/skills/translate/scripts/truncation_check.py <dir> <code>` and
  check every flag against the English.
- **Write quotes bare or typographic, never escaped.** Compose Resources shows `\"` and
  `\'` literally ([[composeresources-no-quote-escaping]]); `validate` refuses them. French
  no-break spaces before `: ; ? !` are added at build time (`composeResourcesForBuild`), so
  plain spaces are fine.
- `find-stale-translations.py` dates a translation by its commit's author time. Commits
  from Weblate's old squash add-on carry the squash's time; see the script's docstring.
- Weblate now only mirrors the repo and collects suggestions; it accepts no direct
  translations and never commits back. `weblate_sync.py` is kept for reading from it
  (suggestions, statistics). Never upload through it: an upload would be overwritten by
  Weblate's next update from the repo.
