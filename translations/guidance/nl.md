# Dutch (nl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional nl-NL iOS localisation (C14). 209 of 359 shared keys still verbatim, 29 drifted |
| Last native-speaker input | 2026-09-28: one reviewer's answers to `questions-nl.md` Q1–Q5 (applied 2026-09-30, Dave's call not to wait for more). The pasted answer was cut off in Q3 |
| Register | Formal «u», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Dutch started from Microsoft's professional translation (C14). One core
term has **changed** away from Microsoft (NL-T1). The Siri phrases (`nl.lproj`)
match the help text. Questions: `docs/translation-questions/questions-nl.md` (Q1…Q4, round 2 since 2026-09-30).

## Glossary

| English | Dutch | Status | Note |
|---|---|---|---|
| Callout | aankondiging | `agreed` (Dave, 2026-09-25; see NL-T1) | **Microsoft said «waarschuwing»**. See NL-T1 |
| Audio Beacon | audiobaken | `confirmed` | Microsoft; reviewer Q5 «prima» |
| Marker | markering | `confirmed` | Microsoft; reviewer Q5: fine if used consistently. «opgeslagen plek» might read better in help prose (`provisional`, not swept) |
| Waypoint | routepunt | `confirmed` | Microsoft |
| Intersection | kruispunt | `confirmed` | Microsoft |
| Sleep / Snooze | Slapen / Slaapstand ; Sluimerstand | `confirmed` | Microsoft |
| Traveling / Heading | U rijdt / U loopt naar het noorden | `confirmed` | Microsoft. The vehicle/walking split, spelled out |
| Detail levels | Uitgebreid / Vereenvoudigd / Essentieel / Stil | `agreed` (Dave), `unconfirmed` (speaker) | English renamed 2026-09-30 (C22); literal on Dave's call. The reviewer had Normaal / Beperkt. See NL-L1 |
| Landmarks | herkenningspunten | `unconfirmed` | AI |
| Way to a dead end | %1$s, doodlopend | `agreed` | New template `confect_name_to_dead_end` (code, 2026-09-30). See NL-G1 |

## Rules

### NL-T1 — Callout moved from «waarschuwing» to «aankondiging» (`agreed`, Dave 2026-09-25)

**Decided 2026-09-25: Dutch keeps «aankondiging».** This was weighed against real counter-evidence. Microsoft's «waarschuwing» is still used 79 times in the Soundscape Community Dutch file, and its native translators (Nathanja and Bram Duvigneau, 2023) kept it in all 21 strings they edited that contain it. Dave's view is that Community edits were piecemeal in Weblate and don't settle a term, and that «waarschuwing» ("warning") frames every callout as an alert. Don't restore «waarschuwing» from Microsoft's or the Community's files (C8).

Microsoft's word was «waarschuwingen» ("warnings"), which frames every
callout as an alert. A later AI pass switched all of them (70 now, 0 left) to
«aankondigingen» ("announcements"), which fits the concept better. That
makes it a C14 case where the drift is probably right, but it replaced a
professional choice, so get it confirmed and then defend it.

### NL-B1 — VoiceOver: «om» needs «te» (`agreed`; template «Dubbeltik: %1$s» 2026-09-30)

Reviewer (Q1): would prefer «Tik dubbel om het audiobaken te dempen», but accepts «Dubbeltik: %1$s», dropping the article in the hints. The preferred form needs «te» in about 40 hints, and TalkBack on Android reads those hints too (C13), so the template-only fix was applied. Dropping articles from the hints isn't swept, because it's cosmetic and it would change what TalkBack says as well. The text below is the original diagnosis.

«Dubbel tik om %1$s» + bare-infinitive hints («het audiobaken dempen») gives
«Dubbel tik om het audiobaken dempen» (should be «…te dempen»). ~40 of the 43
hints are affected. Fix the **template only** (C13): «Dubbeltik: %1$s». While
there, «Dubbel tik» should be «Dubbeltik», which is Microsoft's spelling and
Apple's.

### NL-G1 — Dead ends: «Pad, doodlopend» (`agreed`, code 2026-09-30)

Reviewer (Q2): «Doodlopend pad». The path itself is the dead end, not somewhere it leads to. Dave chose a code fix. `confect_name_dead_end` and its use as the `%2$s` of `confect_name_to` are replaced by `confect_name_to_dead_end` («%1$s to dead end») and `confect_name_to_dead_end_via` («%1$s to dead end via %2$s») in `WayGenerator.kt` (`deadEndName`).

The reviewer's exact «Doodlopend pad» was not possible. `%1$s` is either the way type **or a road's own name** («Ladywood to dead end»), so «Doodlopend Ladywood» would be wrong. And «doodlopend»/«doodlopende» would have to agree with the gender of the way type. Dutch uses **«%1$s, doodlopend»** / **«%1$s via %2$s, doodlopend»**, which keeps the reviewer's point and works for both. Every other language got its old output back, built from its own `confect_name_to` plus `confect_name_dead_end`. The text below is the original diagnosis.

`confect_name_to` «%1$s naar %2$s» gives «Pad naar Doodlopende weg»: a
capital letter mid-sentence and no article. Candidates: dead end →
«een doodlopende weg», or the template «Doodlopend %1$s» for this case.

### NL-C1 — Siri phrases are Dutch and live outside Weblate (`agreed`)

The same coupling as FR-C1. **2026-09-30:** the reviewer (Q4) wants verbs («Soundscape, start de route», «…stop het baken»). Only the two phrases that already contain a verb changed: «Soundscape start de route» and «Soundscape stop het baken». They were changed in `nl.lproj/AppShortcuts.strings`, in `Localizable.xcstrings` ("You can say…") and in `help_text_assistant_commands_ios`. The group words omgeving / route / baken / lijst / detail stay as they are, because Siri needs the "<app> <group> <choice>" shape.

### NL-L1 — Detail levels: Uitgebreid / Vereenvoudigd / Essentieel / Stil (`agreed` 2026-09-30, after the English rename)

**Later on 2026-09-30:** the English levels were renamed Simplified / Essential (C22), and Dave chose literal names over the reviewer's Normaal / Beperkt. Round-2 question 4.

Reviewer (Q3). «Gebalanceerd» was an anglicism, and «Rustig» sat too close to «Stil». 9 strings swept. This is the third language (after Polish and French) to reject the literal Balanced/Quiet. The iOS Siri detail choices have no Dutch entries at all (see FR-L1).

### NL-S1 — Capitals only for real names (`confirmed` 2026-09-30)

Reviewer (Q5). Common nouns stay lowercase mid-sentence («doodlopende weg», «markeringen»). Quoted UI labels keep their own capital («Tik op *Markeringen en routes*»), and so do the unquoted button names in `tour_*`, which name a button. Two prose uses fixed (`help_text_routes_content_how_1`, `faq_tip_create_marker_at_bus_stop`).

## Rejected

- **«Tik dubbel om … te dempen»** (reviewer's first choice for Q1): it needs «te» in about 40 hints, which TalkBack also reads. Their accepted fallback «Dubbeltik: %1$s» was used instead.
- **«Doodlopend pad»** exactly as written (Q2): it can't agree with road names or grammatical gender. See NL-G1.
- **Verbs for every Siri group** (Q4): they'd break Siri's "<group> <choice>" shape.

**«waarschuwing» for Callout** (2026-09-25). It is attractive: it's Microsoft's term, and the Community's native Dutch translators kept it in 21 edited strings. It was rejected because it means "warning", and because piecemeal Weblate edits don't settle a term. See NL-T1.

## Questionnaire round 1 — answered 2026-09-28

Q1 → NL-B1, Q2 → NL-G1, Q3 → NL-L1, Q4 → NL-C1, Q5 → «u» `confirmed`, NL-S1, glossary notes.

## Open questions for round 2

Numbered as on `questions-nl.md`; Q4 is "anything else".

1. «Pad, doodlopend» / «Dorpsstraat, doodlopend»: natural?
2. Detail levels after the English rename: Uitgebreid / Vereenvoudigd / Essentieel / Stil (reviewer had Normaal / Beperkt). Also: the Q3 answer was cut off after «Stil».
3. In help texts, would «opgeslagen plek» read better than «markering»?

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`nl-NL.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 17 were restored to Microsoft's wording and 11 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 17 uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 24 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Near and at were both «Bij»: `directions_near_name` → «Nabij %1$s» and `directions_near_settlement_inline` → «nabij %1$s», matching `directions_near_road_and_settlement`. Full stop added to `settings_reset_button_hint`. Uploaded live.

**2026-09-29 — Full review of all 1586 units, 32 fixes uploaded.** The Weblate log shows no human Dutch edits. The open questions above were not re-flagged. Fixed:
- **Microsoft errors:**
  - «waarin u loopt» → «kijkt» for "facing" in the four `help_text_*_how` button strings and `help_text_my_location_when`.
  - `terms_of_use_medical_safety_disclaimer`: «moeten» → «moet», «mobiele vaardigheden» → «mobiliteitsvaardigheden».
  - `faq_turn_beacon_back_on_question`: «mij» → «mijn».
- **Other meaning fixes:**
  - `relative_clock_direction` «op %1$s uur» («om» is a time of day).
  - English word order in `osm_train_station_named` («Station %1$s»), `osm_subway_named`, both ferry-terminal names, and the two entrance templates («%2$s van %1$s», «…, vanaf %3$s»).
  - `osm_services` «Verzorgingsplaats».
- **NL-T1 leftover:** `callouts_nothing_to_call_out_now` «waarschuwen» → «aankondigen».
- **Other terminology:**
  - `ui_action_button_nearby_markers` «Markers⏎vlakbij» → «Markeringen⏎in de buurt», the name the help and tutorial use.
  - `tour_continue_hint` «tutorial» → «zelfstudie».
  - `osm_generic_landmark` «Oriëntatiepunt» → «Herkenningspunt».
- **Grammar:**
  - Stray «te» in `location_detail_exit_full_screen_hint` and `all_places_nearby_description`.
  - `help_config_voices_content_ios` («door naar … te gaan en op een stem te tikken»).
  - `help_text_assistant_when` («als u om een aankondiging vraagt, wordt de app niet geopend»; «Bluetooth-koptelefoon»).
- **Formatting:**
  - Lower-case `settings_collapse_section` / `settings_expand_section` hints.
  - *…* restored in `help_text_routes_content_how_1`.
  - Sentence case for six OSM names.

Uploaded with `--skip-validate`; all 32 re-fetched and matched exactly.

**2026-09-30 — first reviewer's questionnaire applied.** Answers received 2026-09-28 and held until Dave chose to proceed. 12 strings in `/tmp/translation-review/nl-findings.json`. Siri phrases changed in the iOS files. NL-G1 needed a code change (new dead-end templates) and values for all 45 languages. The 12 strings were uploaded and verified live the same day. The dead-end values wait until Weblate has the new keys.
