# Russian (ru) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2025-11-11 (Yurt Page, Weblate). There is also a one-line edit by A Berezutskyi, 2026-02-17 |
| Register | Formal «вы» (no «ты»). `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

Russian has real native input. **Yurt Page** translated about 380 lines in the
legacy `android-app` component (commits `65792745f`, `474bdfd39`,
`4f6ea2a58`, `da79e566e`, 2025-08-22 → 2025-11-11) and chose the core
terminology. **A Berezutskyi** reworded `settings_explanation` (`92f1756ef`).
Yurt Page's terms still ship and are marked `confirmed` in the Spanish-file
sense: "confirmed then, not re-checked since." Everything newer is AI.

The authored Siri phrases (`ru.lproj`) match the help text. Questions:
`docs/translation-questions/questions-ru.md` (Q1…Q7).

## Glossary

| English | Russian | Status | Note |
|---|---|---|---|
| Callout | уточнение | `confirmed` | Yurt Page's choice («Разрешить уточнения»). Now consistent (66). His early help text also used «выноски»/«сноски» (the Office "text bubble" sense) and «подсказки». Those are gone except 3 «подсказ-». See RU-T1 |
| Audio Beacon | звуковой маяк | `confirmed` | Yurt Page |
| Marker | отметка | `confirmed` | Yurt Page («Отметки») |
| Waypoint | путевая точка | `confirmed` | Yurt Page |
| Landmarks | достопримечательности | `confirmed` | Yurt Page. Note it means *tourist sights*, which is narrower than "landmark". See Q3 |
| Intersection | перекрёсток | `confirmed` | Yurt Page |
| Sleep / Snooze | Сон / Спящий режим ; Отложенный режим | `unconfirmed` | Yurt Page wrote «Спание» for sleeping, which has since been replaced. «Отложенный режим» is AI. See Q4 |
| Detail levels | Подробный / Сбалансированный / Тихий / Беззвучный | `unconfirmed` | Distinct |
| dead end | тупик | `confirmed` word; case fixed 2026-09-24 («тупику»). See RU-G1 |

## Rules

### RU-G1 — «к тупик» must be dative «к тупику» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «тупику», uploaded to Weblate and verified live.

This is rule C9, and Russian is already on its list. `confect_name_to` is
«%1$s к %2$s», and «к» governs the dative. One string, pure grammar.

### RU-T1 — Callout: «уточнение» (`confirmed`, but ask once)

«Уточнение» means "clarification/refinement", an unusual choice for a
spontaneous spoken description. It was a native speaker's choice, so it is
defended, but it's worth one question. The 3 remaining «подсказ-» uses should
follow whichever word wins.

### RU-S1 — «Движется на север» has no subject (`unconfirmed`)

`directions_traveling_*` «Движется на север» ("is moving north", third
person, no subject) vs `directions_heading_*` «На север». Traveling is
vehicle-only (see `bg.md` BG-T4). «Вы едете на север» or «Едем на север»
may be what a speaker expects.

### RU-S2 — «Вы» vs «вы» (`unconfirmed`, cosmetic)

The corpus mixes both (Вы 29, вы 38; «ваш» almost always lowercase). It is
invisible to speech. Ask which convention to use.

### RU-C1 — Siri phrases are Russian and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

«выноска» / «сноска» for Callout. These appear in Yurt Page's early help
text, but they are Microsoft Office's term for a text-bubble annotation, a
visual thing with no connection to speech. They have been replaced by
«уточнение» and must not come back.

## Open questions

1. Callout «уточнение»: still the best word? (RU-T1)
2. «Движется на север»: how should it be said? (RU-S1)
3. Landmarks «достопримечательности»: too "tourist" for a park or a church?
4. Snooze «Отложенный режим»: clear?
5. «Вы» or «вы»? (RU-S2)
6. Siri phrases «Soundscape окружение / маршрут / маяк / выключи маяк…»: natural?
7. Anything else.

## Provenance

**2025-08-22 → 2025-11-11 — native speaker (Yurt Page)**, legacy component.
**2026-02-17 — A Berezutskyi**, one-line edit.
**2026 — AI passes.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «тупику» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
