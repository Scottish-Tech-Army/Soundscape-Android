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
`docs/translation-questions/questions-ur.md` (Q1…Q7).

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
| Detailed / Simplified / Essential / Silent | تفصیلی / سادہ / ضروری / خاموش | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was تفصیلی / متوازن / مختصر / خاموش |
| dead end | بند گلی | `unconfirmed` | |

## Rules

### UR-T1 — Pick one word for Callout (`unconfirmed`)

«کالآؤٹ» vs «اعلان». Sweep the loser, including verb forms (C3).

### UR-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

### UR-B1 — Hint form (`agreed`, fixed 2026-09-29)

Hints are oblique infinitives («…کرنے», «سننے») to fit «%1$s کے لیے دو بار تھپتھپائیں»; never imperatives («…کریں», «سنیں»). Check any new hint.

## Rejected

Nothing yet.

## Open questions

1. Callout: «کالآؤٹ» or «اعلان»? (UR-T1)
2. Beacon and waypoint are loanwords («آڈیو بیکن», «ویپوائنٹ»). Understood?
3. Sleep/Snooze «نیند» / «اسنوز»: natural?
4. Dead end «بند گلی»: right?
5. Is «آپ» right?
6. **The four detail levels** (تفصیلی / سادہ / ضروری / خاموش), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-29 — Full review of all 1586 units.** 24 fixes uploaded. 15 hints had drifted to imperatives («…کریں», «سنیں») and now use the oblique infinitive («…کرنے», «سننے») required by «%1$s کے لیے دو بار تھپتھپائیں» (UR-B1). Help text now uses the real labels «بطور مارکر محفوظ کریں», «پبلک ٹرانزٹ» and «میرے اردگرد». Uploaded with `--skip-validate` and re-fetched: all matched exactly.
