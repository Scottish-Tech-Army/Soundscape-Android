---
title: Language support
layout: page
parent: Information for developers
has_toc: false
---

# Language support

We aim to support as many languages as possible and we're using [Weblate](https://hosted.weblate.org/projects/soundscape-android/) to help with the localization of the app. What that gives us is a place where native speakers can add new translations or just suggest improvements for some of the current strings in the app. We started off supporting the same languages as supported in iOS, but we have since added Ukrainian, Egyptian Arabic, Farsi, Polish, Russian and Icelandic.

A guide to key Soundscape terminology can be found [here]({% link developers/translation-terminology.md %}).

## The translation loop
Weblate follows the Soundscape git repository and generates it's own branch which contains translation edits made from within Weblate. It's a fairly long round trip to do the translations, but it's fairly quick to do. Here's the process:

1. Developer adds a new string or changes an existing one. The string is changed only in the English base language which is in `res/values/strings.xml`. It's important that in the comment above the string there's a good explanation of what the string is used for. This is so that translators have all the context they need to do an accurate translation.
2. Feature is landed in the [Soundscape-Android git repository](https://github.com/Scottish-Tech-Army/Soundscape-Android).
3. In the Weblate UI a project admin clicks on Update [here](https://hosted.weblate.org/projects/soundscape-android/android-app/#repository). This causes the Weblate git repo to rebase/merge changes from the Soundscape-Android repo and update it's lists of strings that need translations for each language.
4. At this point each of the languages in Weblate should show that they have some "Untranslated" strings.
5. The strings are translated either within the Weblate UI or by AI
6. The new strings are committed to the Weblate git repo by an admin clicking on Commit [here](https://hosted.weblate.org/projects/soundscape-android/android-app/#repository)
7. A developer can merge the new translations from the Weblate git into their local tree and then create a pull request containing those changes.
8. The pull request is tested and accepted in Soundscape-Android and we now have all of the translations
9. Final step is to redo step 3 to Update the weblate git repo so that it know that the translations have been landed.

The good thing about this process is that translators, who work in Weblate only, can make additions within the Weblate UI and those changes are integrated in the same way as step 6 onwards. This is also true for new languages being added, but there are some more steps required to integrate those into the app.

**The most important thing here is that the only `strings.xml` file modified directly in Soundscape-Android should be the base English one. All others must come via Weblate merges. Otherwise, there will be git conflicts that require fixing which is a lot more work. For the same reason the base translation cannot be edited within Weblate.**

## Adding a whole new language
Whole new languages can be added via the Weblate UI and generally the translations will appear bit by bit as the translators work their way through it. Merging those into Soundscape-Android will not affect the app as we explicitly whitelist the languages that are to be included. As a result, when we do want to add a new language there are some files to change. They are:
* Add the language to the `resourceConfigurations` list in `app/build.gradle.kts`. Anything not in this list is excluded from the build and this is to block out partial translations.
* Add the language to `getAllLanguages` in `LanguageScreen.kt`. This also requires the name of the language in that language e.g. Español for Spanish.
* Also add the language to `MockLanguagePreviewData` in `LanguageScreen.kt` so that the `@Preview` of the `LanguageScreen` remains accurate.
* Edit `res/xml/locales_config.xml` to include the new language code. This is the list of languages that the app advertises to Android. The list is used within Android to show the user what languages are supported by an app and allow per-app language configuration.

## AI translations
Unfortunately, we don't currently have native speakers helping with translation in the majority of languages that we support and so we needed an additional approach. We want to try and ensure that the translations don't get left behind as we add new features which require new text. It's also quite a big task to add a new translation and so we looked at ways of accelerating that.

*Note that the scripts mentioned below require a couple of keys to be run. A Weblate key to access our server and an OpenAI key to pay for generating new translations.*

### Incremental AI updates
There's now a [Python script](https://github.com/davecraig/weblate-translate/blob/main/weblate-translate-unfinished.py) which queries Weblate to get a list of all of the untranslated strings in each language. It translates them with OpenAI and then uploads the results back into Weblate. As well as the strings to be translated and some context provided from Weblate, the translating process passes in all of the previously translated strings and an explanation of the app. This should give OpenAI enough information to provide an accurate translation, much better than just providing the English source string by itself.

### AI accelerated addition of new language
One of the issues with translating Soundscape is that it has some rather unique terms with very specific meanings e.g. Audio Beacon. Rather than just leave it up to an AI to translate these we've come up with the following approach:

1. Use an interactive ChatGPT session run by a native speaker to generate a glossary of terms (https://chatgpt.com/g/g-68c2c7f92eb4819182fa40ce8fc9f4ff-soundscape-glossary-translation). This suggests various possible translations for each term and at the end generates a JSON file containing the agreed translations.
2. The glossary is passed into OpenAI as context along with all of the terms to be translated by a [Python Script](https://github.com/davecraig/weblate-translate/blob/main/weblate-translation.py). The aim is to maintain a consistency of terms across the translation.

The resulting translation can then be uploaded into Weblate either as Suggestions for the translator or as approved translations.

## Weblate usage tips

There's [extensive documentation](https://docs.weblate.org/en/latest/index.html) available for Weblate. Specific instructions relating to our project are [here](https://hosted.weblate.org/projects/soundscape-android/android-app/#information).

### Zen mode
The main Weblate UI can be quite slow to use when processing strings. Using Zen mode instead shows a much shorter description of the strings to translate and the screen contains is a long list of all those being worked on. Each translation can be edited in turn and they are uploaded in the background which means that there is no UI delay for the translator.

### Bulk edit
Flags can be reset across multiple translations at once using bulk edit. This is useful for moving everything to 'Approved' if we've decided that a translation has been fully completed.

### Failing checks
Weblate compares the translation with the original string and checks that they match in various ways e.g. number of newlines, ending punctuation. It also checks for duplicated words in a translation e.g. "them them" and other things that may be incorrect. It's very important to go through these for each translation as there should be no failures. Any check failure which is an incorrect failure can be dismissed within the UI once it has been double checked by the user.

### Screenshots
Screenshots can be added and associated with strings.