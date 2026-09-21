---
name: weblate-feedback
description: Turn native-speaker feedback about a Soundscape translation into recorded per-language decisions and a corpus-wide sweep. Use when someone sends notes, a bug report, screenshots or a document criticising the translation for a language — it records the decisions in translations/guidance/<code>.md, then finds every other string those decisions affect. Reports findings and never uploads; applying them is [[weblate-review]]'s explicit apply step.
---

# Weblate feedback

A user only ever sees a fraction of the strings, so their report is a *sample*
of a problem, not the problem itself. This skill exists to do the two things
that a straight "fix what they reported" pass misses:

1. **Generalise** — turn each reported string into the decision behind it, and
   record that decision so it survives the session.
2. **Sweep** — find every other string the decision touches. In the Ukrainian
   pilot, 4 reported strings expanded to 62 affected units.

It never uploads. Producing fixes and uploading them are separate, and the
upload lives in [[weblate-review]]'s "Applying fixes" step, behind its own
explicit ask.

## Args

A language code and where the feedback is, e.g.
`/weblate-feedback uk ~/Downloads/localisation_issues.md`. If the feedback is
pasted into the conversation instead, use that. If no language is given, infer
it from the feedback and say which you inferred.

## Procedure

1. **Read the feedback whole, including images.** Screenshots carry things the
   write-up doesn't: the platform and app version, and often strings the
   reporter circled but didn't type up. Check for referenced-but-missing and
   present-but-unreferenced image files — an unreferenced screenshot usually
   means an item they meant to write up.

   Note the platform. iOS screenshots are valid feedback for the `androidkmp`
   component: both apps render the same shared KMP resources.

2. **Load context**: `translations/guidance/_common.md`,
   `translations/guidance/<code>.md` (may not exist yet),
   `docs/developers/translation-terminology.md`. Existing `confirmed`/`agreed`
   entries matter — new feedback may confirm, extend, or contradict them, and
   a contradiction is a question for the reporter, not something to silently
   overwrite.

3. **Fetch the corpus**:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py fetch --lang <code> --out-dir /tmp/weblate-review
   ```

4. **Bucket every item.** This is the core of the skill. Each piece of
   feedback goes into exactly one of:

   | Bucket | Reach | What you record |
   |---|---|---|
   | One-off error | 1 string | A finding. No rule — don't invent one from a typo |
   | **Term decision** | every string using the term | Glossary row + a sweep |
   | **Style/register rule** | a class of strings | Rule + a sweep |
   | **Source problem** | **all languages** | An edit to the English string or its translator comment |

   The last bucket is the highest-leverage and the easiest to miss, because
   the report arrives labelled as one language's problem. If the translator
   had to guess what a string meant, every other language guessed too.

   Also check the reported term against what actually ships. A glossary line
   in the feedback often *matches* current usage — record that as `confirmed`
   rather than skipping it, so later passes defend it instead of churning it.

5. **Sweep each rule across the corpus.** Search the *target* text, not just
   the English source — the whole point is to find strings the reporter never
   saw:
   - all inflected forms of the current term (build the declension map first
     and check it covers every occurrence; an unmatched form means your map is
     incomplete, not that the string is fine)
   - **derived forms** — verbs and participles built off the term. These need
     the sentence rewritten, not the word swapped. Count and report them
     separately from the mechanical noun swaps
   - siblings by context-key prefix (`tour_*`, `new_version_info_*`) — the
     cancel variant, the completion message, the a11y hint, the help page, the
     FAQ answer
   - false positives. Verify every hit against its English source before
     proposing a change; regex sweeps catch unrelated words (the Ukrainian
     pilot's «керован» sweep hit "Remote-Control Car")
   - collisions — if the new term is an everyday word, grep its ordinary uses
     and decide whether the ambiguity is acceptable

6. **Set a status per decision**, per `translations/guidance/README.md`:
   `confirmed`, `agreed`, `unconfirmed`, `provisional`, `rejected`. Be
   honest here rather than optimistic — a decision the reporter themselves
   hedged is `provisional`, and a term change whose grammar you can't fully
   resolve is `unconfirmed`. Do not sweep fixes for either; inventory them.

7. **Write the guidance file** `translations/guidance/<code>.md` (create from
   `uk.md`'s shape if new, otherwise extend). Record decisions and the
   reasoning, not a transcript — but keep the reporter's own words for
   anything you are interpreting, since your reading may be wrong. Always
   record rejected suggestions with why.

8. **Write findings** to `/tmp/weblate-review/<code>-findings.json` in
   [[weblate-review]]'s schema (`context`, `source`, `current`, `suggested`,
   `reason`), so its apply step can consume them unchanged. For
   `unconfirmed`/`provisional` items set `suggested` equal to `current` —
   that is the existing signal for "skip, don't upload". Add `rule` and
   `confidence` fields; the apply step ignores extras.

9. **Report**, and include the piece the user can act on socially: a short
   numbered list of **questions for the reporter**. Make each one specific and
   answerable — name the string, quote the clause, and give the options you
   are choosing between. Ambiguity you resolve by guessing is ambiguity that
   comes back as a second round of feedback.

   Close by telling the user what was applied (nothing), what's ready to apply
   via `weblate-review`, and what's blocked on the reporter.

## Notes

- Never upload from this skill. Not as a natural continuation, not because the
  fixes are obviously right, not because the user said "and fix them" in the
  message that asked for the feedback pass.
- The Weblate project has an empty `glossary` component (TBX,
  `is_glossary=true`, 0 terms in every language). Mirroring `confirmed`/
  `agreed` terms into it makes them appear inline for human translators in the
  Weblate editor — worth offering once a language's glossary settles, but the
  repo file stays the source of truth.
- Editing the English source or its comments (bucket 4) changes what every
  language sees and needs a Weblate "Update" to propagate. Flag it to the user
  rather than treating it as a silent side effect.
- The guidance files are the deliverable that outlives the session. A fix
  uploaded without the rule recorded gets undone by the next translation pass.
