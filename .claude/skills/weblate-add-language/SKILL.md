---
name: weblate-add-language
description: Onboard a brand-new translation language for Soundscape-Android — add it to the Weblate component (androidkmp), translate every string for it, and (separately) wire it into the app's language whitelist and localized docs. Use when the user asks to add a new language/locale that isn't translated at all yet, as opposed to translating already-untranslated strings in a language the app already supports (that's [[weblate-translate]]) or reviewing existing translations (that's [[weblate-review]]).
---

# Weblate add-language

Onboards a language that doesn't exist in the project yet, end to end. Two
phases, run separately:

- **Phase 1 — Weblate**: add the language to the `androidkmp` component and
  translate every string for it (fully automatable in-session, reuses
  `weblate-translate`'s `weblate_sync.py`).
- **Phase 2 — Repo wiring**: flip the app-side whitelist and generate the
  small hand-authored docs page so the app and docs site actually offer the
  language. This is gated on `values-<qualifier>/strings.xml` already
  existing in the repo (see "Why Phase 2 is gated" below) — it can't be
  automated in the same session as Phase 1 because getting the translated
  strings from Weblate into this repo's git tree is a separate, human,
  Weblate-UI step (see `docs/developers/translations.md`'s "translation
  loop").

Read `docs/developers/translations.md` in full before starting (both phases
draw on it) — it's the canonical description of this whole loop and of
"Adding a whole new language" specifically; this skill automates the pieces
of that section a CLI/session can actually do.

## One-time setup (tell the user if this fails)

Same auth as `weblate-translate` — `wlc`'s standard config path,
`~/.config/weblate`:

```ini
[keys]
https://hosted.weblate.org/api/ = <weblate-api-token>
```

`chmod 600 ~/.config/weblate`. If
`python3 .claude/skills/weblate-translate/scripts/weblate_sync.py languages`
reports no API key, stop and tell the user to add it there — never ask for
the key in chat or write it into any file in this repo.

## Args

The language to add, e.g. `/weblate-add-language Welsh` or
`/weblate-add-language et` (Estonian). If the user gives only a language
name, work out its ISO code yourself; if the user gives only a code, work out
the English name yourself. Either way, confirm your guess in the summary you
give back rather than silently assuming — a wrong code creates the wrong
language in Weblate.

If the user names a language that's *already* fully translated and wired
into the app, tell them there's nothing to add — that's not this skill.

## Phase 1 — Add to Weblate and translate

1. Run
   `python3 .claude/skills/weblate-translate/scripts/weblate_sync.py languages`
   and check whether the language is already a translation of the
   `androidkmp` component (by code or by name in the output).

   - **Not present yet**: continue to step 2.
   - **Already present with untranslated > 0**: it was already added (e.g.
     via the Weblate UI, per `docs/developers/translations.md`) but not
     finished — skip step 2 and go straight to step 3, treating whatever's
     already translated as prior art (same as `weblate-translate` does).
   - **Already present with untranslated == 0**: it's fully translated in
     Weblate already. Say so and jump to Phase 2 (it may still not be wired
     into the repo).

2. Add the language to the component. Weblate's language database covers
   nearly every language Soundscape is likely to add, keyed by a code using
   underscores for region variants (e.g. `et`, `nb_NO`, `fr_CA`, `zh_Hans` —
   check the `languages` output above for this project's existing examples
   of that convention before guessing a new one):
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py add-language --lang <weblate-code>
   ```
   If this fails because Weblate has no such language defined at all (rare —
   only for languages with no ISO 639 entry in Weblate's database), tell the
   user rather than guessing plural rules yourself; only if they confirm the
   details, define it first with:
   ```
   python3 .claude/skills/weblate-translate/scripts/weblate_sync.py create-language \
     --code <code> --name "<English name>" --direction ltr|rtl \
     --plural-number <n> --plural-formula "<CLDR formula>"
   ```
   then retry `add-language`.

3. Translate every string for the language. This is exactly
   `weblate-translate`'s procedure — follow it as written, for this one
   language:
   - Fetch units with `weblate_sync.py fetch --lang <code> --out-dir /tmp/weblate-add-language`.
   - Read `docs/developers/translations.md` and
     `docs/developers/translation-terminology.md` for app context and
     terminology; use a user-supplied glossary if one is pointed at.
   - Translate in batches of ~25-30 units, preserving placeholders and
     markdown/line breaks exactly, using `<translated.json>` (if any) to
     keep terminology/tone consistent within the language.
   - Upload with `weblate_sync.py upload --lang <code> --file <path>`.
   - Since this is a brand-new language, expect this to be the *entire*
     ~1400-string component in one run — this will take many batches. Say
     so up front and keep going rather than stopping partway; report
     progress every few batches so the user can see it's moving.

4. Report: language added (yes/no, or already-present), how many strings
   translated/uploaded, any batches that failed to upload (print the
   script's error, keep going). Tell the user the next Weblate-side step is
   manual and outside this skill: a project admin needs to click **Commit**
   then **Update** on the component's repository page in the Weblate UI (see
   `docs/developers/translations.md` step 6/9) and open a PR from Weblate's
   git branch into this repo — that's what actually produces
   `shared/src/commonMain/composeResources/values-<qualifier>/strings.xml`
   here. Phase 2 depends on that PR having landed.

## Phase 2 — Wire the language into the repo

**Only start this when explicitly asked** (e.g. "wire up Estonian now",
"enable Welsh in the app") — not automatically after Phase 1, since Phase 1's
translations typically haven't made it into this repo's git tree yet (see
above). If asked to do both in one go, do Phase 1, report, then check the
gate below before touching anything in Phase 2.

### Why Phase 2 is gated

`docs/developers/translations.md` explains the app explicitly whitelists
languages so incomplete translations never ship silently. Flipping the
whitelist files below before the translated `strings.xml` exists in the repo
would make the app advertise a language that's actually all-English — worse
than not offering it. So:

**Gate check**: does
`shared/src/commonMain/composeResources/values-<qualifier>/strings.xml`
already exist in this repo (`git ls-files` or a plain check)? If not, stop
and tell the user Phase 1's Weblate merge hasn't landed here yet — point them
at the manual step from Phase 1 §4, and don't edit any of the files below.
(`<qualifier>` is the Android resource-qualifier form — see the table below.)

### Determine the three code forms

Every existing language in this repo appears in three related-but-different
spellings; work out the new language's before editing anything, cross-
checking against the closest existing analogous entry in each file listed
below (a same-family language, e.g. another single-variant vs. a
region-variant language) rather than deriving them from a fixed formula —
the existing files aren't perfectly self-consistent (e.g. `zh-rCN` in
`app/build.gradle.kts` vs. bare `zh` in `locales_config.xml`), so match
what's actually there:

1. **Weblate code** (underscore) — from Phase 1, e.g. `et`, `nb_NO`.
2. **Android resource qualifier** (hyphen + `r`-prefixed region, only when a
   region distinguishes it from another variant of the same base language,
   e.g. `en-rGB`, `fr-rCA`, `pt-rBR`, `zh-rCN` — but plain `nb`, `et` for
   single-variant languages). This is the `<qualifier>` from the gate check,
   and what `app/build.gradle.kts`'s `localeFilters` and
   `DocumentationScreens.kt`'s `localeMap` keys use.
3. **Web/BCP-47 code** (plain hyphen, no `r`) — e.g. `en-GB`, `fr-CA`,
   `pt-BR`, `zh-CN`. Used in `locales_config.xml`, `docs/_config.yml`, and as
   `DocumentationScreens.kt`'s `localeMap` values / `parentLabels` keys.

State your determination for all three forms back to the user before
editing, so a wrong guess is caught before it's baked into six files.

### Edits

Make each of these, inserting alphabetically to match the surrounding list
(all six lists are currently in the same alphabetical order by code — keep
them in sync with each other):

1. `app/build.gradle.kts` — add the Android-qualifier code to the
   `localeFilters` list (`androidResources { ... }` block).
2. `shared/src/commonMain/kotlin/org/scottishtecharmy/soundscape/screens/onboarding/language/Language.kt`
   — add `Language("<name in that language>", "<base code>", "<REGION>")` to
   `supportedLanguages`. The name must be written in the language itself
   (see existing entries, e.g. `"Français (France)"`, `"日本語"`), not English.
3. `app/src/main/res/xml/locales_config.xml` — add
   `<locale android:name="<web code>" /> <!-- <English name> -->` in the
   BCP-47 hyphen form, matching the existing comment style.
4. `app/src/androidTest/java/org/scottishtecharmy/soundscape/DocumentationScreens.kt`
   — add `"<android qualifier>" to "<web code>"` to `localeMap`, and
   `"<web code>" to "<title of docs/users/user.<web-code>.md>"` to
   `parentLabels` (the value must exactly match the `title:` you write in
   the next step — `parentLabels` is what nests the generated help pages
   under the right nav section for that language).
5. `docs/_config.yml` — add the web code to the `languages:` array.
6. `docs/users/user.<web code>.md` — new file, translating
   `docs/users/user.md` (title + two short sentences — small enough to
   translate directly, don't skip it). Match the front matter shape of an
   existing translation like `docs/users/user.de.md` exactly: `title` (in
   the target language), `layout: page`, `has_toc: true`, `nav_order: 1`,
   `lang: <web code>`, `permalink: /users/user.html`,
   `machine-translated: true`.

Do not touch `values-<qualifier>/strings.xml` itself — per
`docs/developers/translations.md`, the only `strings.xml` ever hand-edited
in this repo is the English source; every other language's strings.xml only
ever arrives via a Weblate merge.

### Wrap-up

Don't commit — leave the changes in the working tree for the user to review
and commit themselves (per the repo's normal git workflow). Report the full
list of files touched and remind the user that the generated help pages
(`docs/users/help-*.<web code>.md`) still need a regeneration run — point
them at "Regenerating the help pages" in `docs/developers/translations.md`
rather than trying to run the instrumented test from here.

## Notes

- Component is fixed to `androidkmp`, same as `weblate-translate` and
  `weblate-review`. Don't use the `android-app` component — it's stale
  (pre-KMP-migration).
- Never print or log the Weblate API key.
- If `weblate_sync.py` isn't runnable (e.g. `wlc` not installed), tell the
  user to `pip install wlc` rather than trying to reimplement its HTTP calls
  inline.
- If parallelizing Phase 1's translation batches across subagents for a
  large language, follow `weblate-review`'s "If parallelizing the review
  across subagents" guidance on why to use fresh (non-`fork`) agents with
  disjoint input/output files — the same failure mode (a fork reverting to
  this skill's own generic instructions and overwriting sibling output)
  applies here.
