# Telugu (te) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «మీరు» («మీరు సిద్ధంగా ఉన్నారు!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Telugu.** It has been AI-only since
2026-08-21. Structurally sound: `confect_name_to` «%2$sకు వెళ్ళే %1$s» is
correct under C10, and the double-tap template now composes (fixed 2026-09-29, TE-B1).
It leans heavily on loanwords. Questions: `docs/translation-questions/questions-te.md`
(Q1…Q6).

## Glossary

| English | Telugu | Status | Note |
|---|---|---|---|
| Callout | కాలౌట్ | `unconfirmed` | Loanword, consistent (88). See Q1 |
| Audio Beacon | ఆడియో బీకాన్ | `unconfirmed` | Loanword |
| Marker | మార్కర్ | `unconfirmed` | |
| Waypoint | వేపాయింట్ | `unconfirmed` | Loanword |
| Landmarks | మైలురాళ్లు | `unconfirmed` | Literally "milestones", which may mean achievements rather than places. See Q3 |
| Intersection | కూడలి | `unconfirmed` | |
| Sleep / Snooze | నిద్ర / స్నూజ్ | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | వివరణాత్మకం / సరళీకృతం / ముఖ్యం / నిశ్శబ్దం | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was వివరణాత్మకం / సమతుల్యం / క్లుప్తం / నిశ్శబ్దం |
| dead end | డెడ్ ఎండ్ | `unconfirmed` | Loanword where a Telugu word may exist. See Q4 |

## Rules

### TE-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

### TE-B1 — Hint form (`agreed`, fixed 2026-09-29)

The template is «%1$s రెండుసార్లు నొక్కండి»; the hints carry the «-డానికి» infinitive, so the template must not repeat «చేయడానికి». Check any new hint.

## Rejected

Nothing yet.

## Open questions

1. Callout, beacon, waypoint are all loanwords («కాలౌట్», «బీకాన్»,
   «వేపాయింట్»). Understood, or are there better Telugu words? (C12)
2. Is «మీరు» right?
3. Landmarks «మైలురాళ్లు»: do they sound like places, or like achievements?
4. Dead end: «డెడ్ ఎండ్» or a Telugu word?
5. Sleep/Snooze «నిద్ర» / «స్నూజ్»: natural?
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Full stop added to `settings_reset_button_hint`. Uploaded live.

**2026-09-29 — Full review of all 1586 units.** 14 fixes uploaded. **The double-tap template was broken:** «%1$s చేయడానికి రెండుసార్లు నొక్కండి» plus hints ending «…చేయడానికి» gave «…చేయడానికి చేయడానికి…». The earlier note in this file that it composed well was wrong. The template is now «%1$s రెండుసార్లు నొక్కండి» (TE-B1), and 2 more hints moved to «-డానికి». 11 help strings now use the real button, filter and mode labels («బీకాన్‌ను మ్యూట్ చేయి», «ప్రజా రవాణా», «ఒరిజినల్ మోడ్», …). Uploaded with `--skip-validate` and re-fetched: all matched exactly.
