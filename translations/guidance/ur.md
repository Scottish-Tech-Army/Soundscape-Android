# Urdu (ur) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «آپ» («آپ تیار ہیں!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Urdu.** It has been AI-only since
2026-08-21. Structurally sound: «%1$s کے لیے دو بار تھپتھپائیں» with «-نے»
infinitive hints composes correctly, and `confect_name_to` «%1$s %2$s تک»
is acceptable under C10. The main open item is a split term. Questions:
`docs/translation-questions/questions-ur.md` (Q1…Q6).

## Glossary

| English | Urdu | Status | Note |
|---|---|---|---|
| Callout | کالآؤٹ (69) / اعلان (15) | `unconfirmed` | **Split corpus** (C12). See UR-T1 |
| Audio Beacon | آڈیو بیکن | `unconfirmed` | Loanword |
| Marker | مارکر | `unconfirmed` | |
| Waypoint | ویپوائنٹ | `unconfirmed` | Loanword |
| Landmarks | نشانات | `unconfirmed` | |
| Intersection | چوراہا | `unconfirmed` | |
| Sleep / Snooze | نیند / اسنوز | `unconfirmed` | |
| Detail levels | تفصیلی / متوازن / مختصر / خاموش | `unconfirmed` | Distinct |
| dead end | بند گلی | `unconfirmed` | |

## Rules

### UR-T1 — Pick one word for Callout (`unconfirmed`)

«کالآؤٹ» vs «اعلان». Sweep the loser, including verb forms (C3).

### UR-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout: «کالآؤٹ» or «اعلان»? (UR-T1)
2. Beacon and waypoint are loanwords («آڈیو بیکن», «ویپوائنٹ»). Understood?
3. Sleep/Snooze «نیند» / «اسنوز»: natural?
4. Dead end «بند گلی»: right?
5. Is «آپ» right?
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
