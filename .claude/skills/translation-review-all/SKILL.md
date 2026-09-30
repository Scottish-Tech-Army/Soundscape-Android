---
name: translation-review-all
description: Run [[translation-review]] on every Soundscape-Android language in one go, one fresh subagent per language, then combine the findings into one summary with the cross-language problems pulled out. Review only, it never edits or commits; fixes are applied per language afterwards through translation-review's apply step. Use when the user asks to review, audit or proofread all languages, every translation or the whole corpus at once, rather than one or two named languages (that's [[translation-review]] itself).
---

# Translation review, all languages

A driver for [[translation-review]]. It adds no review rules of its own: each
language is reviewed by a worker that runs `/translation-review <code>`, so
the checks, the guidance files and the findings format stay in one place.
This skill only fans the work out, collects the results and summarises them.

Like translation-review, it is **read-only**. It never runs `strings_sync.py
apply`, never edits a strings file or a guidance file, and never commits.

## Args

- none: every language `strings_sync.py languages` prints.
- a list of codes (`de fr pl`): only those.
- `--skip <codes>`: everything except those, e.g. `--skip pl en_GB` for a
  language reviewed earlier the same day.

## Procedure

### 1. Scope

Run
```
python3 .claude/skills/translate/scripts/strings_sync.py languages
```
and settle the list of codes from the args. Note each language's
untranslated/stale counts: the review skips those strings, so a language
with many of them gets a thin review. Mention that in the summary instead
of holding the run up.

### 2. Keep earlier findings

Each worker writes `/tmp/translation-review/<code>-findings.json`, which is
also the file translation-review's apply step reads. A run over every
language would silently overwrite findings from an earlier session that
haven't been applied yet. Before launching anything, move the existing
findings for the languages in scope aside:
```
d=/tmp/translation-review/archive-$(date +%Y%m%d-%H%M%S)
mkdir -p "$d" && for c in <codes>; do
  f=/tmp/translation-review/$c-findings.json; [ -f "$f" ] && mv "$f" "$d/"
done
```
Say in the summary where they went.

### 3. Launch the workers

One worker per language, using **fresh `general-purpose` agents, never
`fork`**. A fork inherits this conversation, including this skill and the
translation-review skill. That has twice made a worker re-review the whole
corpus and overwrite its siblings' output files. A fresh agent only knows
its prompt.

Run them in the background, **at most 8 at a time**. Start the next language
as each one finishes. Order the queue largest-first (`-translated.json`
size isn't known until the worker fetches, so use the previous run's
counts if you have them, otherwise alphabetical). Tell the user once, at
the start, how many languages are queued. Don't narrate each launch.

Give every worker exactly this prompt, with `<code>` and `<name>` filled in:

> You are reviewing the **<name> (`<code>`)** translations of the
> Soundscape-Android app, in the repository at
> `/home/dave/STA/Soundscape-AndroidTest`.
>
> Invoke the `translation-review` skill with the argument `<code>` and follow
> its review procedure for that one language, from step 1 through writing
> `/tmp/translation-review/<code>-findings.json`.
>
> Limits. These override anything in that skill:
> - Review **only `<code>`**. Don't fetch, read or write any other
>   language's files in `/tmp/translation-review/`.
> - Work through the batches yourself. **Don't start any subagents.**
> - **Review only.** Don't run `strings_sync.py apply`, don't edit any file
>   in the repository, don't commit and don't push. The skill's "Applying
>   fixes" step doesn't apply to you.
> - The only file you may create or change is
>   `/tmp/translation-review/<code>-findings.json`, plus the `fetch` output
>   files for `<code>`.
> - Give every finding a `category` field as well: one of `meaning`,
>   `placeholders`, `formatting`, `gender`, `terminology` or `tone`.
> - If a problem isn't really this language's, start its `reason` with
>   `CROSS-LANGUAGE:`, and set `suggested` equal to `current`. That covers
>   a bug in the code that uses the string, an English source that is wrong
>   or ambiguous (`_common.md` rule C7), or a defect you'd expect in every
>   language.
>
> When the findings file is written, stop. Reply with only: the number of
> units reviewed, the number flagged, the number with a confident fix, the
> untranslated/stale counts from `fetch`, and your three most serious
> findings as one line each (`context`: what's wrong).

### 4. Collect

When every worker has reported, check that each language has a findings
file. For any that doesn't, relaunch that one language once. If it fails
again, list it as not reviewed. Then run
```
python3 .claude/skills/translation-review-all/scripts/summarize_findings.py <codes>
```
It reads every `<code>-findings.json`, checks each `current` still matches
`<code>-translated.json` (a worker quoting a string wrongly shows up here),
and writes `/tmp/translation-review/all-summary.md`: a per-language table
of counts by category, then the `CROSS-LANGUAGE:` findings grouped by key,
then each language's meaning, placeholder and formatting findings.

### 5. Report

Keep the reply short. The detail is in the summary file.

1. One line of scope: languages reviewed, units read, flagged, confident
   fixes. Add any language not reviewed, and where the archived findings
   went.
2. **Cross-language problems first**, merged by key. The same defect in
   twenty languages is one code or source fix, not twenty translation
   fixes, and that's the highest-value output of a run over every language.
   Say for each one whether it looks like a code, source-comment or
   translation problem.
3. The per-language table, sorted by meaning + placeholder + formatting
   findings, highest first.
4. For the three to five worst languages, their most serious findings.
5. What's next: fixes are applied **one language at a time**, by the user
   saying e.g. "apply the German fixes". That runs translation-review's
   "Applying fixes" step, with its confirmation, against the findings file
   this run wrote. Also point to `all-summary.md`.

Nothing is applied as part of this skill, even if the user's request
sounded like they want fixes done ("review everything and fix it"). Report,
then stop, as translation-review requires.

## Notes

- A full run is expensive: every language is a complete review of about
  1,600 strings, roughly as much reading as one single-language
  `/translation-review`. For a quick look, pass a few codes.
- `en_GB` is included by default. Its review is mostly spelling and
  vocabulary, so skip it if the user only cares about real translations.
- Workers can't ask the user anything. A question the worker would have
  asked ends up as a finding with `suggested` equal to `current`.
