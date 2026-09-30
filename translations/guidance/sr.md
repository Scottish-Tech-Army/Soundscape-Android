# Serbian (sr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «Ви» («Спремни сте!»), `unconfirmed` |
| Script | Cyrillic, consistent. The only Latin text is genuine UI names (iOS «Enhanced», «Play») and the English Siri phrases |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Serbian.** It has been AI-only since
2026-08-21. The VoiceOver template «Двапут додирните да %1$s» with
present-tense hints («утишате») composes correctly. Traveling/Heading are
«Путовање»/«Ходање», the correct vehicle/walking split. Questions:
`docs/translation-questions/questions-sr.md` (Q1…Q8).

## Glossary

| English | Serbian | Status | Note |
|---|---|---|---|
| Callout | најава | `unconfirmed` | Consistent |
| Audio Beacon | звучни бакен | `unconfirmed` | «бакен» is a river-navigation buoy light. See Q2 |
| Marker | маркер | `unconfirmed` | |
| Waypoint | путна тачка | `unconfirmed` | |
| Landmarks | знаменитости | `unconfirmed` | |
| Intersection | раскрсница | `unconfirmed` | |
| Sleep / Snooze | Спавање / **Спавање** ; Дремање | `unconfirmed` | See SR-T1 |
| Detailed / Simplified / Essential / Silent | Детаљно / Поједностављено / Основно / Без звука | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Детаљно / Уравнотежено / Тихо / Без звука |
| dead end | ћорсокак | `unconfirmed` word; case fixed 2026-09-24. See SR-G1 |

## Rules

### SR-G1 — «до ћорсокак» must be genitive «до ћорсокака» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «ћорсокака», uploaded to Weblate and verified live.

Rule C9, and Serbian is on its list. One string.

### SR-T1 — The Sleep button and the Sleeping status are the same word (`agreed` defect, `unconfirmed` wording)

`sleep_sleep` (the button) and `sleep_sleeping` (the status) are both
«Спавање». A listener can't tell "press to sleep" from "is asleep". The
status could be «На спавању» / «У режиму спавања», and the button a verb such
as «Успавај».

### SR-G2 — FAQ questions default to the masculine (`unconfirmed`)

«…да бих смањио утицај…». There is no slash (compare HR-G2), but it is
masculine-only. An impersonal rephrase («Како смањити…») avoids gender
entirely.

### SR-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «најава»: natural?
2. Beacon «звучни бакен»: does «бакен» work for a sound you follow?
3. Sleep button and status are both «Спавање». What should each say? (SR-T1)
4. FAQ headings: «Како смањити…» instead of «…да бих смањио…»? (SR-G2)
5. Snooze «Дремање»: clear?
6. Is «Ви» right?
7. **The four detail levels** (Детаљно / Поједностављено / Основно / Без звука), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
8. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «ћорсокака» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 28 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Byte `*_a11y` plurals: the «few» form had no number; `%1$s` restored. `osm_helipad` → «Слетиште за хеликоптере», `osm_religion` → «Религија» (it had been a copy of place of worship). Uploaded live.

**2026-09-29 — European batch review.** 5 hints: imperatives «додајте», «Прикажи… уреди» → present «додате», «прикажете… уредите»; `location_detail_action_beacon_hint` «вас звуком воде» ("they guide you") → «покренете звучно навођење до ове локације». Help: «Оближња места» → «Места у близини» (2), «Успавај» → «Спавање» (1). 8 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
