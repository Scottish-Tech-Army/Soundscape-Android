---
title: Language support
layout: page
parent: Information for developers
has_toc: false
---

# Language support

We aim to support as many languages as possible. Soundscape is translated into 45 languages, and the translations live in this repository: `shared/src/commonMain/composeResources/values-<lang>/strings.xml`, alongside the English source in `values/strings.xml`. **The repository is the source of truth.**

Native speakers help mainly through the [per-language questionnaires]({{ "/help-translate/" | relative_url }}) and by email to soundscapeAndroid@scottishtecharmy.support. [Weblate](https://hosted.weblate.org/projects/soundscape-android/androidkmp/) mirrors the repository so that anyone can browse the translations and leave suggestions there, but it no longer writes to the repository.

A guide to key Soundscape terminology can be found [here]({% link developers/translation-terminology.md %}).

## The translation loop

1. A developer adds a new string or changes an existing one, in the English `values/strings.xml` only. The comment above the string must explain what it's for and where it's heard: translators (human or AI) have nothing else to go on.
2. The change lands on `main`. Every language now has the string as **untranslated** (a new key) or **stale** (the English changed after the translation was last changed).
3. `scripts/find-stale-translations.py` lists them, per language:
   ```
   scripts/find-stale-translations.py                  # counts for every language
   scripts/find-stale-translations.py --lang de --diff # the strings, with how the English changed
   ```
4. A translation pass fills them in and commits them to the language files, like any other code change. This is normally done with the `translate` Claude Code skill (see [AI translations](#ai-translations)), but a translator can equally edit the files by hand and open a pull request.
5. Weblate picks up the new state of the repository on its next update.

Stale detection is worked out from git history, as Weblate's "needs editing" used to be. Quote and whitespace changes are ignored on both sides. If an English change needs nothing from a language (a typo fix, say), acknowledge it rather than rewriting the translation:
```
scripts/find-stale-translations.py --lang de --acknowledge <key> ...
```
This records the current English for that string in `translations/stale-acknowledged.json`, and the string stays quiet until the English changes again.

### Feedback from native speakers

Questionnaire answers, emails and Weblate suggestions are turned into recorded decisions in `translations/guidance/<code>.md`, with a corpus-wide sweep for every other string each decision affects. `translations/guidance/_common.md` holds the rules that apply to every language. These files are what every later translation and review pass starts from, so a correction isn't undone by the next pass. The `translation-feedback` and `translation-review` skills do this work. Weblate suggestions are collected with `.claude/skills/translate/scripts/weblate_sync.py suggestions`, which writes one file per language with open suggestions for the feedback skill to review.

### Format notes

* Quotes and apostrophes go in bare, or better, as typographic quotes. Compose Resources shows `\"` and `\'` literally.
* French no-break spaces before `: ; ? !` are added at build time (`composeResourcesForBuild`), so plain spaces in the files are fine.
* Plurals select on whole numbers only, and each language needs exactly its own CLDR categories.

## Adding a whole new language

Translations for a language that isn't enabled don't affect the app, because we explicitly whitelist the languages to include. The `add-language` skill creates `values-<qualifier>/strings.xml` and translates every string into it; enabling the language is then a separate, deliberate step. The files to change are:
* Add the language to the `resourceConfigurations` list in `app/build.gradle.kts`. Anything not in this list is excluded from the build and this is to block out partial translations.
* Add the language to `getAllLanguages` in `LanguageScreen.kt`. This also requires the name of the language in that language e.g. Español for Spanish.
* Also add the language to `MockLanguagePreviewData` in `LanguageScreen.kt` so that the `@Preview` of the `LanguageScreen` remains accurate.
* Edit `res/xml/locales_config.xml` to include the new language code. This is the list of languages that the app advertises to Android. The list is used within Android to show the user what languages are supported by an app and allow per-app language configuration.
* Add the language to the documentation website so the localized help pages are generated and served (see [The documentation website](#the-documentation-website) below): add an entry to `localeMap` in `DocumentationScreens.kt` (mapping the Android resource qualifier to its web/BCP-47 code) and add the same web code to the `languages` list in `docs/_config.yml`.

Check a new language's locale against the plural rules Compose Resources knows before adding it: an unknown locale throws rather than falling back.

## AI translations

Most of our languages don't yet have a native speaker checking them, so translations are kept up to date with AI. This is done in-session with Claude Code skills in `.claude/skills/`, which translate using the whole existing corpus for that language, the terminology guide and the recorded native-speaker decisions:

* `translate` — translates every untranslated and stale string, validates the result (placeholders, line breaks, plural forms, escaping) and commits it. Its helper `strings_sync.py` does the file work.
* `translation-review` — reviews existing translations for a language and reports findings. It only applies fixes when explicitly asked.
* `translation-feedback` — turns native-speaker feedback into recorded decisions and a sweep.
* `add-language` — adds a new language.
* `translation-questionnaire` — writes and refreshes the published questionnaires.

(They were called `weblate-translate`, `weblate-review`, `weblate-feedback` and `weblate-add-language` until 2026-09-30, when they stopped working through Weblate.)

## The documentation website

This website is built with [Jekyll](https://jekyllrb.com/) and the [just-the-docs](https://just-the-docs.com/) theme, and is published to GitHub Pages by the `jekyll-gh-pages.yml` workflow. It is multilingual via the [`jekyll-polyglot`](https://github.com/untra/polyglot) plugin: every supported language gets its own URL subtree (e.g. `/de/`, `/fr-CA/`) and a language switcher appears in the header. Pages that have no translation in a given language fall back to the English version automatically, so the developer documentation (which is English-only) still appears under every language.

### Localized help pages come from the app's strings

The user help pages (`docs/users/help-*.md`) are **generated**, not hand-written. They are produced by the `getHelp` test in `app/src/androidTest/.../DocumentationScreens.kt`, which walks the `helpPages` structure in `HelpScreen.kt` and emits markdown using `context.getString(...)`. Because `getString` is locale-aware, the test loops over every supported locale (using a context built with `createConfigurationContext`) and emits one markdown file per page per language:

* English is written with no suffix, e.g. `help-routes.md`.
* Each other language is written with its web/BCP-47 code as a suffix, e.g. `help-routes.de.md`, and carries a `lang:` entry plus a `permalink:` pinned to the English page's URL (e.g. `permalink: /users/help-routes.html`). polyglot matches translations by URL, so the shared permalink is what makes it serve the German file at `/de/users/help-routes.html` rather than a separate URL.
* A locale that has nothing translated on a page is skipped, so polyglot serves the English fallback rather than a page mislabelled as translated.

The set of locales lives in `localeMap` in `DocumentationScreens.kt`, which mirrors `resourceConfigurations` in `app/build.gradle.kts` and must stay in sync with the `languages` list in `docs/_config.yml`.

### Regenerating the help pages

The `build-app.yaml` release workflow runs `getHelp` on an emulator, pulls the generated markdown off the device, and commits any changes to `main` (which triggers the Pages deploy). It runs as part of every release, so the pages pick up whatever translations had been committed by then — cut a release to publish a batch of translations.

To regenerate locally instead:

1. Run only the doc-generation test on a connected device/emulator:
   ```
   ./gradlew connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=org.scottishtecharmy.soundscape.DocumentationScreens#getHelp
   ```
2. Pull the generated files into the repo:
   ```
   adb pull /storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Documents/help/. docs/users/
   ```
3. Review the diff and commit. (`help-beacon-styles.md` is hand-authored — it is not produced by the test, so leave it in place.)

### Pages not driven by strings

`index.md` and the legal pages are hand-authored and not part of `strings.xml`, so they are currently English only and rely on polyglot's English fallback under each `/<lang>/` URL. A translation can be added later by committing a per-language variant (e.g. `index.de.md` with `lang: de`). The legal pages (privacy policy, terms and conditions) should only ever be translated by a human reviewer — do not machine-translate them.

### Linking to localized pages

Do **not** use Jekyll's {% raw %}`{% link users/help-….md %}`{% endraw %} tag to link to a help page (or any other page that has translations). The {% raw %}`{% link %}`{% endraw %} tag resolves by *source filename*, but in each non-default language pass polyglot replaces the English page with its localized variant in the document set — so the English source filename is not found and the build fails with `Could not find document … in tag 'link'`.

Instead, link to such pages by URL so polyglot can rewrite it to the active language:

{% raw %}
```
[Help using Media Controls]({{ "/users/help-using-media-controls.html" | relative_url }})
```
{% endraw %}

{% raw %}`{% link %}`{% endraw %} is still fine for English-only pages (developer docs, `user.md`), because their English source stays in every language pass.

## Weblate

Weblate follows the repository, and anyone with an account can leave suggestions. The component no longer accepts direct translations and never commits back, so it can't cause git conflicts, strip quotes or lose no-break spaces, as it used to. Don't unlock it for direct editing without first reinstating a way to merge its changes, because edits made there would otherwise be overwritten by the next update from the repository.
