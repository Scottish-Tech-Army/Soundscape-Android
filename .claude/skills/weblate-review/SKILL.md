---
name: weblate-review
description: Review Soundscape-Android's already-translated Weblate strings (androidkmp component) for a given language, checking they make sense against the English source, preserve placeholders/formatting, and match project terminology. Reports findings in-session; can apply and upload confident fixes back to Weblate, but ONLY in a separate step the user explicitly asks for by name in their own message — the review itself never uploads. Use when the user asks to review, check, audit, or proofread existing translations for a language, as opposed to translating untranslated strings (that's [[weblate-translate]]).
---

# Weblate review

QA pass over strings that are *already* translated in Weblate — sanity-checks
them against the English source instead of producing new translations. It
reuses `weblate-translate`'s `weblate_sync.py` for the deterministic HTTP
calls: `languages` and `fetch` for the review itself, and — only in the
"Applying fixes" step below, only on explicit request — `upload`.

The review is read-only. Findings are reported to the user, who decides what
(if anything) to fix. Do not treat a request to review as implying consent to
upload, no matter how the user phrases wanting fixes "done" — see "Applying
fixes" for exactly what counts as the required ask.

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
      as worth a human look (e.g. ambiguous tone call), set `suggested` equal
      to `current` — the apply step below treats that as "no confident fix,
      skip" and won't upload it.

   e. Write the language's findings (even if empty) to
      `/tmp/weblate-review/<code>-findings.json` as a JSON array of those
      objects. Keep this file around after reporting — it's what "Applying
      fixes" below reads if the user asks for that later, in this session or
      a future one.

3. After all requested languages are done, report a summary to the user:
   for each language, how many units were reviewed and how many were
   flagged, then list the flagged ones (context key, source, current,
   suggested, reason) — grouped by language, most likely genuine errors
   (meaning/placeholders/formatting) before softer calls (terminology/tone).
   If a language had zero findings, just say so in one line. Mention that
   nothing was uploaded and that fixes can be applied on request.

## Applying fixes (separate step — only on the user's own explicit request)

This step uploads to a shared system other translators see, so treat it with
the same care as a `git push` to a shared branch, not as a natural
continuation of a review.

**Trigger.** Only start this step in response to a message the user actually
sent in this turn, naming what to apply — e.g. "apply the French fixes",
"upload the confident ones for de". Do not start it:
- as a follow-on step after finishing a review, even if reporting the
  findings feels incomplete without it;
- because an earlier message implied the user would eventually want fixes
  uploaded ("review these and fix what you find" still means: report first,
  then stop and wait for a separate go-ahead on the fixing part);
- from within a background agent/fork that isn't relaying back to a human
  between the review and the apply — if you're a fork asked only to review,
  finish the review, report, and stop. Don't keep going on your own
  initiative, and don't run `git add`/edit skill files/take any other action
  outside what you were explicitly asked to do.

**Procedure**, once genuinely triggered:

1. Read `/tmp/weblate-review/<code>-findings.json` for the requested
   language. If it's missing, run the review procedure above first — don't
   guess at findings from memory.

2. Split into:
   - **Actionable**: `suggested` is non-empty and differs from `current`.
   - **Skipped**: `suggested` is empty or equals `current` — flagged for a
     human, not for upload. Never invent a value here just to make one
     uploadable.

3. Show the user exactly what's about to change — for each actionable
   finding, `context`: `current` → `suggested` — plus the skipped context
   keys, and get explicit confirmation before uploading. Only skip this
   confirmation if the user's own request already made clear they don't want
   a further check (e.g. "just upload all the fixable ones, don't ask
   again").

4. Build `{context: suggested}` for the confirmed actionable findings (same
   shape `weblate-translate` uploads) and write it to
   `/tmp/weblate-review/<code>-apply.json`.

5. Upload:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py upload --lang <code> --file /tmp/weblate-review/<code>-apply.json
   ```
   This lands as the direct translation for those keys (`conflicts:
   replace-translated`) — there's no review/suggestion queue, so only upload
   findings you're actually confident in.

6. Report back: how many were uploaded, and the full list of still-skipped
   findings (context, reason) so the user knows what still needs a human
   look in the Weblate UI.

## Notes

- Component is fixed to `androidkmp`, same as `weblate-translate`. Don't use
  the `android-app` component — it's stale (pre-KMP-migration).
- The review step (`languages`/`fetch`) never runs `weblate_sync.py upload`
  — only the "Applying fixes" step does, and only when explicitly triggered
  per the rules above.
- Never print or log the Weblate API key.
- If `weblate_sync.py` isn't runnable (e.g. `wlc` not installed), tell the
  user to `pip install wlc` rather than trying to reimplement its HTTP calls
  inline.
