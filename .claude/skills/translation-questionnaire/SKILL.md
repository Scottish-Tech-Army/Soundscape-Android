---
name: translation-questionnaire
description: Write, refresh or roll out the per-language native-speaker questionnaires published on the docs site (docs/translation-questions/questions-<code>.md, listed on /help-translate/). Use when the user wants a question sheet for a language, wants existing sheets updated after feedback or new uploads, or wants the sheet format changed across languages. Sheets are written for people who have never used the app, in the reviewer's own language, and quote only real app strings.
---

# Translation questionnaires

The questionnaires are the **outgoing** half of the native-speaker loop:
`translations/guidance/<code>.md` records decisions (inbound), and
`docs/translation-questions/questions-<code>.md` asks the open questions
(outgoing). They are **public web pages** on the docs site, so they go to
people who have never seen Soundscape. Replies come by email to the help desk.

The Polish full-corpus review pack (`translations/review/pl-full/`) is a
different, unpublished format; this skill doesn't cover it.

## Before writing anything

1. Run the context helper for the language:
   ```
   python3 .claude/skills/translation-questionnaire/scripts/questionnaire_context.py <code> [extra keys]
   ```
   It prints the terms and example strings every sheet quotes (English and
   current translation), the guidance file's open questions and glossary, and
   the current sheet. Add string keys for anything a question quotes.
2. The guidance file's **Open questions** list is the source of the questions.
   The sheet and the list use the **same numbers**, so a reply of "Q3: …" maps
   straight back to a guidance item.
3. Read `translations/guidance/_common.md` rules for anything the question
   touches (C9 case agreement, C10 `confect_name_to`, C17 labels, C18 grammar
   markers).

## Page contract

- **Path and front matter.** `docs/translation-questions/questions-<code>.md`,
  with:
  ```
  ---
  title: "<native language name>"
  layout: default
  nav_exclude: true
  search_exclude: true
  permalink: /translation-questions/questions-<code>/
  ---
  ```
  The `questions-` prefix is required: jekyll-polyglot silently drops any page
  whose file name or URL segment is a bare language code (`de.md`,
  `/translation-questions/de/`). The folder is in `exclude_from_localization`,
  so each sheet is built once, unprefixed. A new sheet also needs a line in
  `docs/help-translate.md`'s list, sorted by English name.
- **H1 and contact line** in the page's language, directly under it:
  `> <Send your answers by email to> **soundscapeAndroid@scottishtecharmy.support** <…language in the subject>.`
  When editing an existing sheet, keep both unchanged.
- **Arabic, Persian, Urdu:** wrap everything after the front matter in
  `<div dir="rtl" markdown="1">` … `</div>`, with blank lines inside.
- **Machine-written sheets for low-resource languages** (Hausa, Swahili) are
  bilingual on purpose: an italic English line after each paragraph.
- **Never name a reviewer** or say who answered what. The pages are public.
  "A native speaker" is fine; a name, handle or issue number is not.
- **Register:** the sheet addresses the reader in the register the app uses (or
  the one the guidance has settled on), and stays in it throughout.

## Layout

In order, all in the reviewer's language (the English glosses in italics are
for the maintainer):

1. Greeting, one line on why their view matters, and **"You don't need to know
   or install the app: each question says when you hear the text, what it says
   in English and how it sounds now."**
2. **"What is Soundscape?"**, ~150 words. Translate the English version in
   `questions-en_GB.md`:
   - free phone app for blind and partially sighted people, used walking, with
     headphones, phone in a pocket; not turn-by-turn, it says what's nearby;
   - **callout**, **audio beacon**, **marker** and **route**, each under the
     name the app currently uses (from the context helper), with an English
     italic gloss;
   - blind users hear everything through a screen reader (TalkBack /
     VoiceOver), often in street noise, so short, clear and natural *when
     heard* is what matters;
   - a link to `{{ "/developers/translation-terminology.html" | relative_url }}`.
3. **How to reply**: email, quote the question number, "OK" is a useful answer.
4. `---`, then one `### Qn — <title> *(English gloss)*` per question, with these
   fields (drop a field only when it has nothing to say):
   - **When you hear it**: the situation, in one or two sentences. For Siri
     phrases: "never; you *say* these".
   - **In English**: the English source, quoted exactly.
   - **How it sounds now**: the current translation, quoted exactly.
   - **What we're unsure about**: the real reason for asking, e.g. the word
     also means a car horn, it clashes with phone notifications, it's a
     loanword, the machine chose it.
   - **The question**, with options if there are any.
5. The last question is always "Anything else?". Then thanks.

Every sheet uses this layout, except Spanish: all its questions are answered, so
it is a "what we decided" summary, still with the "What is Soundscape?"
section. (Polish, Icelandic and Ukrainian were converted to it on 2026-09-25; the
old Polish per-text review lives unpublished in
`translations/review/pl-full/07-nowe-teksty.md`.)

## Rules that caught real mistakes

- **Quote only real strings.** Every quoted app text must come from the context
  helper or a grep of the same `strings.xml`. An English quote that doesn't
  exist in `values/strings.xml` is a bug, however plausible it sounds.
- **Examples must show the app's real output.** Map names are substituted
  undeclined, so write «po Hlavní ulice», «ao lado de Rua Augusta», «à côté de
  Rue de la République», not the grammatically correct form. A corrected example
  hides exactly the defect a reviewer should hear. Use the language's own word
  for the substituted part (`osm_path` for "Path"), not an invented one.
- **Resolve grammar markers on a static page.** The app resolves «a(z)»,
  «을(를)», «(으)로», «'{DA}» at runtime; a web page doesn't. Write the resolved
  form, including in the contact line (Korean «…support로», not «…support(으)로»).
- **AI-only languages ask about "audio beacon" and "callout".** If the guidance
  glossary has either term as `unconfirmed` and no question covers it, add one
  before "Anything else", and add it to the guidance open questions with
  "(AI-only term, asked for confirmation)".
- **Numbering is continuous.** When a question is answered, remove it,
  renumber the sheet, renumber the guidance open questions to match, and
  update the "(Q1…Qn)" range in the guidance file. Move answered items to a
  "Settled" note instead of leaving gaps.
- **New doubts belong in the guidance too.** If writing a sheet surfaces a
  reason for doubt (a term mix such as «Titik Rute» ×21 / «titik jalan» ×3),
  add it to the guidance open question, not just the public page.

## Checking

```
python3 .claude/skills/translation-questionnaire/scripts/check_questionnaires.py [code ...]
```
checks the front matter and permalink, that the "What is Soundscape?" section
exists, continuous numbering, RTL wrappers, unresolved grammar markers and the
guidance "(Q1…Qn)" range. Then build the site from `docs/`
(`bundle exec jekyll build -d <scratch dir>`, ~90 s) and confirm the page exists
at `translation-questions/questions-<code>/index.html`, contains no `{{`, and
links to the terminology page.

Commit with the guidance changes. The pages go live once the change is merged
upstream, which happens only through a GitHub pull request the maintainer
reviews: never push to the upstream remote yourself. "Push" means origin.
