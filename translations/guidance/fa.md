# Persian (fa) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last human input | 2025-11-01 (Khashayar Hamidzadeh, Weblate, ~490 lines). **Possibly machine-assisted**, so don't treat it as confirmed (see below) |
| Register | Formal plural («آماده‌اید!», «بزنید»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

A human, Khashayar Hamidzadeh, submitted a large Persian translation in
Weblate (2025-09-21 → 2025-11-01), after mahmood hozhabri added the language
(2025-08-28). **Dave's note (2026-09-24): this may itself have been AI
output, so give it no special credence.** His terms are therefore
`unconfirmed`, like everything else. He revised several terms more than once
(«نشانه ی صوتی» → «جهت‌نمای صوتی» for Beacon, «اعلامیه» → «اعلان» for Callout),
which is the only real evidence of human judgement.

Things that already work:
- The template «دو بار ضربه بزنید تا %1$s» composes with the hints, because
  the imperative and subjunctive 2pl are the same form («قطع کنید»).
- «مختصر» (brief) for Quiet keeps it apart from «بی‌صدا» (silent).
- There is no `fa.lproj`, so the Siri phrases stay in English.

Questions: `docs/translation-questions/questions-fa.md` (Q1…Q5).

## Glossary

| English | Persian | Status | Note |
|---|---|---|---|
| Callout | اعلان | `unconfirmed` | «اعلان» is also Android's word for a notification. See Q1 |
| Audio Beacon | جهت‌نمای صوتی | `unconfirmed` | "Audio direction-pointer". Descriptive, which is good |
| Marker | نشانه | `unconfirmed` | |
| Waypoint | نقطه‌ی بین‌راهی | `unconfirmed` | |
| Landmarks | نقاط شاخص | `unconfirmed` | |
| Intersection | تقاطع | `unconfirmed` | |
| Sleep / Snooze | حالت خواب ; حالت چرت | `unconfirmed` | |
| Detail levels | مفصل / متعادل / مختصر / بی‌صدا | `unconfirmed` | Distinct |
| dead end | بن‌بست | `unconfirmed` | |

## Rules

None yet beyond the glossary. The corpus has no structural defects that
this sweep could find.

## Rejected

Nothing yet.

## Open questions

1. Callout «اعلان»: confused with phone notifications?
2. Beacon «جهت‌نمای صوتی»: clear?
3. Snooze «حالت چرت»: natural?
4. Is the formal plural register right?
5. Anything else.

## Provenance

**2025-08-28 — mahmood hozhabri** added the language. **2025-09-21 →
2025-11-01 — Khashayar Hamidzadeh**, ~490 lines (possibly machine-assisted).
**2026 — AI passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.
