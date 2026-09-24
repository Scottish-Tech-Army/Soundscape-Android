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
`translations/review/sr.md` (Q1…Q7).

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
| Detail levels | Детаљно / Уравнотежено / Тихо / Без звука | `unconfirmed` | Distinct |
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
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «ћорсокака» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
