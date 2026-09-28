# Romanian (ro) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Informal «tu» (only 1 «dumneavoastră»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Romanian.** It has been AI-only since
2026-02-08. Things that already work:
- The hints are «a»-infinitives that compose with «Atinge de două ori pentru
  %1$s».
- `confect_name_to` «%1$s spre %2$s» + «fundătură» is grammatical.
- «continuă la stânga» is descriptive (C11).
- There is no `ro.lproj`, so the Siri phrases stay in English (PL-C1).

Questions: `docs/translation-questions/questions-ro.md` (Q1…Q6).

## Glossary

| English | Romanian | Status | Note |
|---|---|---|---|
| Callout | anunț | `unconfirmed` | |
| Audio Beacon | baliză audio | `unconfirmed` | |
| Marker | marcaj | `unconfirmed` | |
| Waypoint | punct de traseu | `unconfirmed` | |
| Landmarks | repere | `unconfirmed` | |
| Intersection | intersecție | `unconfirmed` | |
| Sleep / Snooze | Repaus ; Amânare | `unconfirmed` | |
| Detail levels | Detaliat / Echilibrat / Discret / Silențios | `unconfirmed` | Distinct |
| dead end | fundătură | `unconfirmed` | |

## Rules

### RO-R1 — «tu» and a masculine default (`unconfirmed`)

The app is informal («Atinge», «Ești pregătit!»). Formal «dumneavoastră» is
more usual in Romanian software, so ask. «Ești pregătit!» is masculine
(C15). «Totul este gata!» avoids gender.

## Rejected

Nothing yet.

## Open questions

1. Register: «tu» or «dumneavoastră»? (RO-R1)
2. «Totul este gata!» instead of «Ești pregătit!»? (RO-R1)
3. Callout «anunț»: natural?
4. Snooze «În amânare»: clear?
5. Beacon «baliză audio»: natural?
6. Anything else.

## Provenance

**2026-02-08 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `faq_turn_beacon_back_on_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Beacon style names translated (Dave's decision).** All 13 `beacon_styles_*` had been left in English. They are now translated, following the English translator notes and the es/it/fr/pt pattern: Actual, Original, Scânteie (flare, a bright burst), Licărire (shimmer, a softer flare), Tactil, Clinchet (ping, a short high ring), Cădere (drop), Semnal, Ciocănel (a xylophone mallet), with «(lent)» / «(foarte lent)» in lowercase. `unconfirmed`, so check with a native speaker, especially Scânteie and Clinchet. Also on 2026-09-28 during the Weblate checks pass: `osm_religion` → «Religie» (it had been a copy of place of worship), loading indicator → «Se încarcă» without «…». Uploaded live.
