---
name: weblate-review
description: Review Soundscape-Android's already-translated Weblate strings (androidkmp component) for a given language, checking they make sense against the English source, preserve placeholders/formatting, and match project terminology. Reports findings in-session; never uploads anything to Weblate. Use when the user asks to review, check, audit, or proofread existing translations for a language, as opposed to translating untranslated strings (that's [[weblate-translate]]).
---

# Weblate review

QA pass over strings that are *already* translated in Weblate — sanity-checks
them against the English source instead of producing new translations. This
is read-only against Weblate: it reuses `weblate-translate`'s
`weblate_sync.py` for the deterministic HTTP calls (`languages`, `fetch`),
but never calls `upload`. Nothing is written back to Weblate; findings are
reported to the user, who decides what (if anything) to fix and how.

## One-time setup (tell the user if this fails)

Same auth as `weblate-translate` — `wlc`'s standard config path,
`~/.config/weblate`:

```ini
[keys]
https://hosted.weblate.org/api/ = <weblate-api-token>
```

`chmod 600 ~/.config/weblate`. If `languages` reports no API key, stop and
tell the user to add it there — never ask for the key in chat or write it
into any file in this repo.

## Args

One or more language codes to review, e.g. `/weblate-review de fr`. Language
codes are Weblate's (e.g. `de`, `fr_CA`, `zh_Hans`, `en_GB`), matching what
`languages` prints. If the user gives no language, ask which one(s) to
review rather than guessing — reviewing every language in one pass is a lot
of output for the user to sift through, so don't default to "all" the way
`weblate-translate` does with untranslated strings.

## Procedure

1. Run
   `python3 .claude/skills/weblate-translate/scripts/weblate_sync.py languages`
   to confirm the requested language code(s) exist and see their totals.

2. For each language, in turn:

   a. Fetch its units:
      ```
      python3 .claude/skills/weblate-translate/scripts/weblate_sync.py fetch --lang <code> --out-dir /tmp/weblate-review
      ```
      This writes `<code>-untranslated.json` (ignore it here) and
      `<code>-translated.json` — the set to review. If `-translated.json` is
      empty, say so and skip the language.

   b. Read `docs/developers/translations.md` and
      `docs/developers/translation-terminology.md` from this repo for app
      context and the canonical meaning of Soundscape-specific terms. If the
      user points you at a glossary file for this language, load it too and
      prefer its terms when judging correctness.

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
      - Use `note`/`context` on the unit the same way `weblate-translate`
        does, for where/how the string is used.

   d. For every flagged unit, record `{context, source, current, suggested,
      reason}` — `suggested` is your proposed fix, `reason` is a short
      one-line explanation of what's wrong. Don't invent a `suggested` fix
      you're not reasonably confident in; if you're only flagging something
      as worth a human look (e.g. ambiguous tone call), say so in `reason`
      and you may leave `suggested` equal to `current`.

   e. Write the language's findings (even if empty) to
      `/tmp/weblate-review/<code>-findings.json` as a JSON array of those
      objects, so the user has a durable copy to hand to `weblate-translate`
      (or apply by hand) if they want fixes uploaded — this skill does not
      upload it itself.

3. After all requested languages are done, report a summary to the user:
   for each language, how many units were reviewed and how many were
   flagged, then list the flagged ones (context key, source, current,
   suggested, reason) — grouped by language, most likely genuine errors
   (meaning/placeholders/formatting) before softer calls (terminology/tone).
   If a language had zero findings, just say so in one line.

## Notes

- Component is fixed to `androidkmp`, same as `weblate-translate`. Don't use
  the `android-app` component — it's stale (pre-KMP-migration).
- This skill never runs `weblate_sync.py upload`. If the user wants any of
  the suggested fixes actually applied to Weblate, that's a separate,
  explicit action (e.g. running `weblate-translate` on those specific
  strings, or an upload the user asks for by name) — don't do it as part of
  this review.
- Never print or log the Weblate API key.
- If `weblate_sync.py` isn't runnable (e.g. `wlc` not installed), tell the
  user to `pip install wlc` rather than trying to reimplement its HTTP calls
  inline.
