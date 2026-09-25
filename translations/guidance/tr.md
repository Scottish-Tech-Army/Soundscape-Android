# Turkish (tr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2026-02-17 (Toro Inoue, ~36 lines) and 2026-08-21 (Oğuz Ersen, 1 line), both via Weblate |
| Register | Formal «siz» («Hazırsınız!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

Turkish has two small human contributions (Toro Inoue added the language in
2026-02, and Oğuz Ersen is an active Turkish Weblate translator). The rest
is AI. The corpus has **the most serious structural problem found in any
language**: suffixes hard-coded onto placeholders (TR-G1). The authored Siri
phrases (`tr.lproj`) match the help text. Questions:
`docs/translation-questions/questions-tr.md` (Q1…Q7).

## Glossary

| English | Turkish | Status | Note |
|---|---|---|---|
| Callout | anons | `unconfirmed` | |
| Audio Beacon | Sesli İşaret | `unconfirmed` | |
| Marker | Kayıtlı Nokta | `unconfirmed` | "Saved point". A nice descriptive choice |
| Waypoint | Ara Nokta | `unconfirmed` | |
| Landmarks | Simge Yapılar | `unconfirmed` | |
| Intersection | kavşak | `unconfirmed` | |
| Sleep / Snooze | Uyku Modu ; Erteleme Modu | `unconfirmed` | |
| Detail levels | Ayrıntılı / Dengeli / Sakin / Sessiz | `unconfirmed` | |
| dead end | çıkmaz sokak | `unconfirmed` | |

## Rules

### TR-G1 — Suffixes on placeholders are resolved in code (`fixed`, 2026-09-25)

18 strings attached a fixed case suffix to a substituted name or number
(«%1$s'de», «%2$s'e yakın», «%3$s'ın %2$s. ara noktasında»). Turkish
suffixes follow the vowel harmony and final sound of the word they attach
to, so a fixed form was wrong for about half of all names («İstanbul'de»,
«3'ın»).

On 2026-09-25 all 18 were rewritten on Weblate into archiphoneme markers,
'{DA} '{DAn} '{A} '{I} '{In}, which `resolveGrammarMarkers()` (C18) resolves
once the name is known. **New strings must use the markers**, never a
fixed suffix:

| Case | Write | Becomes |
|---|---|---|
| Locative | `%1$s'{DA}` | 'de 'da 'te 'ta, 'nde 'nda |
| Ablative | `%1$s'{DAn}` | 'den 'dan 'ten 'tan, 'nden 'ndan |
| Dative | `%1$s'{A}` | 'e 'a 'ye 'ya, 'ne 'na |
| Accusative | `%1$s'{I}` | 'ı 'i 'u 'ü, 'yı…, 'nı… |
| Genitive | `%1$s'{In}` | 'ın 'in 'un 'ün, 'nın… |

Known limits, for the reviewer to judge:
- Place names ending in a possessive («Atatürk Caddesi», «Moda Parkı»,
  «Havalimanı») take the extra n. They're recognised by a list of common
  generic nouns plus the -sı/-si ending, so an unusual one («Eminönü») gets
  the plain form.
- Loanwords with front-vowel suffixes («Kemal'e», «saat'e») follow the
  spelling instead, so they'd get «Kemal'a».
- Non-Turkish names go by spelling («Moor Road'a»).
- The apostrophe is kept even where TDK would drop it for an institution
  name. It isn't heard.

`confect_name_to` «%1$s'{DAn} %2$s'{A}» still means "from X to Y", which is
a separate C10 question (open question 2).

### TR-B1 — Mixed hint forms (`agreed` defect, `unconfirmed` wording)

The template «%1$s için çift dokunun» needs a «-mek/-mak» verbal noun before
«için», and 21 of the 43 hints have one («ilerlemek»). The others are
imperatives («sesli işareti sessize al»), which give «…sessize al için çift
dokunun». This is the rare case where the **hints** should change, to the
«-mek» form. That form likely also suits Android TalkBack's Turkish frame, but ask a
TalkBack user to confirm before sweeping, since the hints feed both
platforms (C13).

### TR-C1 — Siri phrases are Turkish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. The automatic suffixes (TR-G1): do they sound right, especially after
   «Caddesi»-type names, numbers and abbreviations?
2. «Moor Road'a giden patika» for "path to Moor Road"? (TR-G1, C10)
3. VoiceOver: «sesli işareti sessize almak için çift dokunun»? (TR-B1)
4. Beacon «Sesli İşaret»: natural?
5. Siri phrases: natural?
6. Callout «anons»: natural? (AI-only term, asked for confirmation)
7. Anything else.

## Provenance

**2026-02-09 — Toro Inoue** added Turkish and made small edits. **2026-08-21
— Oğuz Ersen**, one line. **2026 — AI passes.** **2026-09-24 — corpus
sweep.**

**2026-09-25 — truncation repaired (C16).** `faq_why_does_beacon_disappear_answer`, `faq_turn_beacon_back_on_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-25 — TR-G1 resolved in code.** All 18 suffixed templates rewritten to
'{DA}/'{DAn}/'{A}/'{I}/'{In} markers, uploaded and verified live, together with
the resolver in `GrammarMarkers.kt`.
