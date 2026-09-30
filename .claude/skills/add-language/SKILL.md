---
name: add-language
description: Onboard a brand-new translation language for Soundscape-Android — create its values-<qualifier>/strings.xml, translate every string for it, and (separately) wire it into the app's language whitelist and localized docs. Use when the user asks to add a new language/locale that isn't translated at all yet, as opposed to translating already-untranslated strings in a language the app already supports (that's [[translate]]) or reviewing existing translations (that's [[translation-review]]).
---

# Add a language

Onboards a language that doesn't exist in the project yet, end to end. Two
phases, run separately:

- **Phase 1 — Translate**: create
  `shared/src/commonMain/composeResources/values-<qualifier>/strings.xml` and translate
  every string into it, with `translate`'s `strings_sync.py`. Commit.
- **Phase 2 — Repo wiring**: flip the app-side whitelist and generate the
  small hand-authored docs page so the app and docs site actually offer the
  language. Only when the user asks: whitelisting is what makes the language
  ship, so it stays a deliberate step even though Phase 1 now lands in the repo
  directly.

Read `docs/developers/translations.md` before starting (both phases draw on it).

## Args

The language to add, e.g. `/add-language Welsh` or
`/add-language et` (Estonian). If the user gives only a language
name, work out its ISO code yourself; if the user gives only a code, work out
the English name yourself. Either way, confirm your guess in the summary you
give back rather than silently assuming — a wrong code creates the wrong
`values-*` directory, and Compose Resources matches the qualifier exactly
([[compose-resources-indonesian-in-vs-id]]).

If the user names a language that's *already* fully translated and wired
into the app, tell them there's nothing to add — that's not this skill.

## Phase 1 — Create the file and translate

1. Run `python3 .claude/skills/translate/scripts/strings_sync.py languages` and
   check whether the language already has a `values-*` file.

   - **Not present yet**: continue to step 2.
   - **Present with pending strings**: a previous run stopped partway. Carry on from
     step 3, treating what's there as prior art.
   - **Present and complete**: say so and jump to Phase 2.

2. Work out the language's code and `values-*` qualifier (`cy`, `et`; a region variant
   is `xx-rYY`), and its CLDR plural categories. **Check the plural categories against
   the 220-locale map before starting** ([[compose-plurals-select-on-int-only]]): Compose
   throws on a locale it doesn't know rather than falling back, which is why `arz` was
   dropped. If the code differs from the qualifier, add it to `VALUES_DIR` in both
   `strings_sync.py` and `WEBLATE_CODES` in `scripts/find-stale-translations.py`.

3. Translate every string, following `translate`'s procedure for this one
   language:
   - `strings_sync.py fetch --lang <code> --out-dir /tmp/add-language` — every
     string comes back `untranslated`.
   - Read `docs/developers/translations.md`, `docs/developers/translation-terminology.md`
     and `translations/guidance/_common.md`.
   - Translate in batches of ~25-30 units, preserving placeholders and markdown/line
     breaks exactly. Keep terminology consistent within the language.
   - Plurals take an object with exactly the categories from step 2. With no existing
     plural to compare against, `validate` warns instead of checking them, so check them
     yourself.
   - `strings_sync.py apply --lang <code> --file <batch> --out-dir /tmp/add-language`
     after each batch. The first apply creates the file.
   - This is the whole ~1600-string corpus. Say so up front, keep going rather than
     stopping partway, and report progress every few batches.

4. Commit on main (don't push), then report: the code and qualifier you used, how many
   strings were translated, and anything skipped. Phase 2 can follow whenever the user
   asks for it.

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
already exist in this repo, committed and with nothing pending
(`strings_sync.py languages --lang <code>` shows 0 untranslated)? If not, stop and
tell the user Phase 1 isn't finished, and don't edit any of the files below.
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

1. **Language code** (underscore, as `translations/guidance/` uses) — from Phase 1,
   e.g. `et`, `nb_NO`.
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

Do not touch `values-<qualifier>/strings.xml` in this phase — it's Phase 1's, and it
only changes through `strings_sync.py apply`.

### Wrap-up

Don't commit — leave the changes in the working tree for the user to review
and commit themselves (per the repo's normal git workflow). Report the full
list of files touched and remind the user that the generated help pages
(`docs/users/help-*.<web code>.md`) still need a regeneration run — point
them at "Regenerating the help pages" in `docs/developers/translations.md`
rather than trying to run the instrumented test from here.

## Notes

- If parallelizing Phase 1's translation batches across subagents for a
  large language, follow `translation-review`'s "If parallelizing the review
  across subagents" guidance on why to use fresh (non-`fork`) agents with
  disjoint input/output files — the same failure mode (a fork reverting to
  this skill's own generic instructions and overwriting sibling output)
  applies here.
