---
name: translation-review
description: Review Soundscape-Android's already-translated strings (the repo's shared/composeResources values-*/strings.xml) for a given language, checking they make sense against the English source, preserve placeholders/formatting, and match project terminology. Reports findings in-session; can apply confident fixes to the files and commit them, but ONLY in a separate step the user explicitly asks for by name in their own message — the review itself never edits anything. Use when the user asks to review, check, audit, or proofread existing translations for a language, as opposed to translating untranslated strings (that's [[translate]]).
---

# Translation review

QA pass over strings that are *already* translated — sanity-checks them against the
English source instead of producing new translations. It reuses `translate`'s
`strings_sync.py`: `languages` and `fetch` for the review itself, and — only in the
"Applying fixes" step below, only on explicit request — `apply`.

The review is read-only. Findings are reported to the user, who decides what (if
anything) to fix. Do not treat a request to review as implying consent to change the
files, no matter how the user phrases wanting fixes "done" — see "Applying fixes" for
exactly what counts as the required ask.

## Args

One or more language codes to review, e.g. `/translation-review de fr`. Language
codes are the ones `translations/guidance/` uses (e.g. `de`, `fr_CA`, `zh_Hans`,
`en_GB`), matching what `languages` prints. If the user gives no language, ask which one(s) to
review rather than guessing — reviewing every language in one pass is a lot
of output for the user to sift through, so don't default to "all" the way
`translate` does with untranslated strings.

## Procedure

1. Run
   `python3 .claude/skills/translate/scripts/strings_sync.py languages --lang <code> ...`
   to confirm the requested language code(s) exist and see what is still pending.

2. For each language, in turn:

   a. Fetch its units:
      ```
      python3 .claude/skills/translate/scripts/strings_sync.py fetch --lang <code> --out-dir /tmp/translation-review
      ```
      This writes `<code>-translated.json` — the set to review — and
      `<code>-untranslated.json`, the strings that are untranslated or stale. Don't
      review those: they're [[translate]]'s job. Mention their count in the
      summary if it isn't 0. If `-translated.json` is empty, say so and skip the
      language.

   b. Read `docs/developers/translations.md` and
      `docs/developers/translation-terminology.md` from this repo for app
      context and the canonical meaning of Soundscape-specific terms —
      remembering that the alternate wordings in the terminology doc are
      English glosses, not preferred translations.

      Then load `translations/guidance/_common.md` and
      `translations/guidance/<code>.md` if it exists. These are recorded
      native-speaker decisions and they **outrank your own judgement** —
      a translation matching a `confirmed` or `agreed` glossary entry is
      correct even if you would have phrased it differently, and one that
      departs from it is a finding. Check the "Rejected" section before
      proposing anything, so you don't resurface a suggestion that has
      already been turned down.

   c. Go through `<code>-translated.json` in batches of roughly 40-50 units
      (review is lighter-weight per unit than translating, so larger batches
      are fine). For each unit, compare `target` against `source` and flag it
      if any of these hold — otherwise leave it alone, don't report on
      strings that are fine:
      - **Meaning**: the translation doesn't actually convey the source
        meaning, or reads as a mistranslation/false friend.
      - **Placeholders**: positional format args (`%1$s`, `%2$d`, etc.) are
        missing, reordered, or a different one than the source uses.
      - **Formatting**: markdown or literal line breaks present in `source`
        aren't preserved in `target`.
      - **Terminology**: a Soundscape-specific term (Audio Beacon, Marker,
        Waypoint, etc. — see the terminology doc / glossary) is rendered
        inconsistently with how it's used elsewhere in this same
        `<code>-translated.json` batch, or with the glossary if one exists.
      - **Tone/register**: phrasing that would be confusing or awkward
        specifically for an audio-first app used by blind/low-vision users
        (see `docs/developers/translations.md`) — e.g. a translation that
        only makes sense visually.
      - Use `note`/`context` on the unit the same way `translate`
        does, for where/how the string is used.

   d. For every flagged unit, record `{context, source, current, suggested,
      reason}` — `suggested` is your proposed fix, `reason` is a short
      one-line explanation of what's wrong. Don't invent a `suggested` fix
      you're not reasonably confident in; if you're only flagging something
      as worth a human look (e.g. ambiguous tone call), set `suggested` equal
      to `current` — the apply step below treats that as "no confident fix,
      skip" and won't apply it.

   e. Write the language's findings (even if empty) to
      `/tmp/translation-review/<code>-findings.json` as a JSON array of those
      objects. Keep this file around after reporting — it's what "Applying
      fixes" below reads if the user asks for that later, in this session or
      a future one.

3. After all requested languages are done, report a summary to the user:
   for each language, how many units were reviewed and how many were
   flagged, then list the flagged ones (context key, source, current,
   suggested, reason) — grouped by language, most likely genuine errors
   (meaning/placeholders/formatting) before softer calls (terminology/tone).
   If a language had zero findings, just say so in one line. Mention that
   nothing was changed and that fixes can be applied on request.

### If parallelizing the review across subagents

Step 2c's batches are written for one agent working through them
sequentially in a single session — that's the default and is fine for
languages up to a few hundred units. Only reach for parallel subagents when
a corpus is large enough (many hundreds of units) that sequential review
would be too slow.

If you do parallelize, do **not** hand batches to `fork` subagents. A fork
inherits this entire skill file verbatim, including the generic
instructions and file paths above ("go through `<code>-translated.json`",
"write to `/tmp/translation-review/<code>-findings.json`"). A worker holding
both that generic instruction and your narrower per-batch delegation ("only
look at batch N") can end up following the skill's own generic instructions
instead of the delegation — re-reviewing the *entire* corpus on its own
initiative and overwriting every other batch's output file (and even
deleting the batch input files afterwards as "cleanup"). This has happened
more than once in practice.

Instead, launch fresh (non-`fork`, e.g. `general-purpose`) agents. A fresh
agent has no memory of this skill, so it only knows what you put in its
prompt. Give each worker:
- one batch input file to read, distinct from every other worker's input
  file (e.g. a pre-split `<code>-batch-N.json`, not the full
  `<code>-translated.json`)
- one output file it may write, distinct from every other worker's output
  file and distinct from the canonical `<code>-findings.json` used by the
  single-agent flow (e.g. `<code>-findings-N.json`)
- an explicit instruction not to read `<code>-translated.json` or any other
  batch/output file, and not to delete or modify anything else, and to stop
  once its one output file is written

Only after every worker reports back do you concatenate their output files
yourself into the final `<code>-findings.json` for the summary and for the
"Applying fixes" step below.

## Applying fixes (separate step — only on the user's own explicit request)

This step changes the shipped translation files and commits, so treat it as a
deliberate change, not as a natural continuation of a review.

**Trigger.** Only start this step in response to a message the user actually
sent in this turn, naming what to apply — e.g. "apply the French fixes",
"apply the confident ones for de". Do not start it:
- as a follow-on step after finishing a review, even if reporting the
  findings feels incomplete without it;
- because an earlier message implied the user would eventually want fixes
  applied ("review these and fix what you find" still means: report first,
  then stop and wait for a separate go-ahead on the fixing part);
- from within a background agent/fork that isn't relaying back to a human
  between the review and the apply — if you're a fork asked only to review,
  finish the review, report, and stop. Don't keep going on your own
  initiative, and don't run `git add`/edit skill files/take any other action
  outside what you were explicitly asked to do.

**Procedure**, once genuinely triggered:

1. Read `/tmp/translation-review/<code>-findings.json` for the requested
   language. If it's missing, run the review procedure above first — don't
   guess at findings from memory.

2. Split into:
   - **Actionable**: `suggested` is non-empty and differs from `current`.
   - **Skipped**: `suggested` is empty or equals `current` — flagged for a
     human, not for applying. Never invent a value here just to make one
     applicable.

3. Show the user exactly what's about to change — for each actionable
   finding, `context`: `current` → `suggested` — plus the skipped context
   keys, and get explicit confirmation before applying. Only skip this
   confirmation if the user's own request already made clear they don't want
   a further check (e.g. "just apply all the fixable ones, don't ask
   again").

4. Build `{context: suggested}` for the confirmed actionable findings (same
   shape `translate` applies) and write it to
   `/tmp/translation-review/<code>-apply.json`. First check each `current` still matches
   the file — a finding whose string has changed since the review is stale; drop it
   and say so.

5. Apply:
   ```
   python3 .claude/skills/translate/scripts/strings_sync.py apply --lang <code> --file /tmp/translation-review/<code>-apply.json --revise
   ```
   `--revise` allows keys that aren't pending, which review fixes never are. It
   validates first and writes nothing if anything fails. Check `git diff`, then commit
   on main, naming the language and what kind of fixes they were. Don't push.

6. Report back: how many were applied, the commit, and the full list of still-skipped
   findings (context, reason) so the user knows what still needs a human look.

## Notes

- **Translate the whole string, never just the part that changed** (rule C16 in `translations/guidance/_common.md`). After a bulk pass, run `python3 .claude/skills/translate/scripts/truncation_check.py /tmp/translation-review <code>` and check every flag against the English.

- The review step (`languages`/`fetch`) never runs `strings_sync.py apply` — only
  the "Applying fixes" step does, and only when explicitly triggered per the rules
  above.
- Findings files from before the switch away from Weblate (2026-09-30) used the same
  `{context, source, current, suggested, reason}` shape and can still be applied this
  way, once `current` is checked against the file.
