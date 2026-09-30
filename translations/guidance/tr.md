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
`docs/translation-questions/questions-tr.md` (Q1…Q8).

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
| Detailed / Simplified / Essential / Silent | Ayrıntılı / Sadeleştirilmiş / Temel / Sessiz | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Ayrıntılı / Dengeli / Sakin / Sessiz |
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

### TR-B1 — Mixed hint forms (`agreed` defect, `fixed` 2026-09-29, wording `unconfirmed`)

The template «%1$s için çift dokunun» needs a «-mek/-mak» verbal noun before
«için», and 21 of the 43 hints have one («ilerlemek»). The others are
imperatives («sesli işareti sessize al»), which give «…sessize al için çift
dokunun». This is the rare case where the **hints** should change, to the
«-mek» form. That form likely also suits Android TalkBack's Turkish frame, but ask a
TalkBack user to confirm before sweeping, since the hints feed both
platforms (C13).

**Swept 2026-09-29** at the maintainer's request. All 21 imperative hints are
now «-mek/-mak» forms, so the 43 hints are consistent. **Correction:** the
sweep was justified at the time by saying `talkback_double_tap_template` is the
Android TalkBack frame. It isn't: it is iOS-only, and Android TalkBack wraps
the hint in its own Turkish phrasing (C13). The «-mek» form is still the one
that fits «… için çift dokunun» on iOS, and probably TalkBack's Turkish frame
too, but that part is unconfirmed. **New hints must use the «-mek» form.**
Open question 3 still asks a speaker, ideally a TalkBack user, how it sounds.

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
7. Motorway junctions: «5. Kavşak» reads as "the 5th junction". Better «5 numaralı kavşak» or «Kavşak 5»? Also: is «Vapur İskelesi» or «Feribot İskelesi» right for a ferry terminal, and what is a *rigger* (a craft trade) in Turkish?
8. Anything else.

## Provenance

**2026-02-09 — Toro Inoue** added Turkish and made small edits. **2026-08-21
— Oğuz Ersen**, one line. **2026 — AI passes.** **2026-09-24 — corpus
sweep.**

**2026-09-25 — truncation repaired (C16).** `faq_why_does_beacon_disappear_answer`, `faq_turn_beacon_back_on_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-25 — TR-G1 resolved in code.** All 18 suffixed templates rewritten to
'{DA}/'{DAn}/'{A}/'{I}/'{In} markers, uploaded and verified live, together with
the resolver in `GrammarMarkers.kt`.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `help_text_automatic_callouts_when_2` had lost its bold heading; restored as «**Belirli bir konuma yürürken:**». Beacon styles translated (Dave's decision, as for Romanian): Parıltı, Işıltı, Dokunsal, Çınlama, Düşüş, Sinyal, Tokmak, all `unconfirmed`. Uploaded live.

**2026-09-29 — full review.** 34 fixes, live strings re-checked before upload, uploaded with `--skip-validate` and verified live. Meaning: `directions_approaching_name` «%1$s yaklaşıyor» (the place approaching you) → «%1$s'{A} yaklaşılıyor»; `faq_sleep_mode_battery_question` «Sessiz Mod» → «Uyku Modu»; `help_text_my_location_when` "facing" as «ilerlediğinizi» → «baktığınızı»; `faq_battery_impact_answer` («Pilinizin en çok tükenen kısmı», «Kullanımda olmadığınız»); `faq_tip_beacon_quiet` «susar» → «kısılır»; `tour_start_beacon` past tense «sekmesindeydiniz»; `faq_holding_phone_flat_answer` «tam hacme» → «tam ses düzeyine»; `general_error_add_marker_error` dropped "later"; `first_launch_headphones_message_1` «şimdi alın» → «şimdi takın». Also a typo, a lost `*…*`, and two help texts renamed to the real labels («İşaretin Sesini Aç», «Tamam»). 21 hints swept to «-mek» (TR-B1). **Held:** `osm_rigger` still English; `directions_junction_with_ref` «%1$s. Kavşak» turns a junction ref into an ordinal; ferry terminal «Vapur İskelesi» vs «Feribot İskelesi».
