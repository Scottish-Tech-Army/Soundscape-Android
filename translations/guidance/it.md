# Italian (it) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional it-IT localisation (C14). 234 of 359 shared keys still verbatim, only 8 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «tu», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Italian is the best-preserved Microsoft baseline. The hints compose
correctly («Tocca due volte per disattivare l'audiofaro»), «è sulla
sinistra» is descriptive (C11), and the Siri phrases (`it.lproj`) match the
help text. Questions: `docs/translation-questions/questions-it.md` (Q1…Q8).

## Glossary

| English | Italian | Status | Note |
|---|---|---|---|
| Callout | notifica | `confirmed` | Microsoft. It also means a system notification, like fr. Ask once |
| Audio Beacon | audiofaro | `confirmed` | Microsoft |
| Marker | indicatore | `confirmed` | Microsoft |
| Waypoint | waypoint | `confirmed` | Microsoft, English loan (C12) |
| Intersection | incrocio | `confirmed` | Microsoft |
| Sleep / Snooze | Sospendi ; Posponi | `confirmed` | Microsoft |
| Detailed / Simplified / Essential / Silent | Dettagliato / Semplificato / Essenziale / Silenzioso | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Dettagliato / Bilanciato / Discreto / Silenzioso |
| dead end | vicolo cieco | `unconfirmed` | AI. See IT-G1 |

## Rules

### IT-G1 — «a vicolo cieco» (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s a %2$s» gives «Sentiero a vicolo cieco», with no
article and the wrong preposition, which is FR-G1 again. Candidates:
«%1$s verso %2$s» with «un vicolo cieco», or «%1$s che porta a %2$s».

### IT-R1 — «Sei pronto!» is masculine (`unconfirmed`)

See C15. «È tutto pronto!» avoids gender.

### IT-G1 — Prepositions fuse with a name's own article (`fixed` in code, 2026-09-25)

«a La Scala», «di Il Vittoriano» should be «alla Scala», «del Vittoriano». 23
templates with a, di, da, in or su before a map name now wrap it, «{it:a %1$s}»,
and `resolveGrammarMarkers()` (C18) fuses the article. Nothing is added before
via, piazza or corso, which Italian says without an article («su Via Roma»).
**New templates must wrap map-name prepositions.** `directions_near_name` and
`directions_near_road_and_settlement` said «Vicino %1$s», without the «a» of
«vicino a»; fixed 2026-09-25 as «Vicino {it:a %1$s}» («Vicino a Via Roma»,
«Vicino alla Scala»).

## Rejected

Nothing yet.

## Open questions

1. Callout «notifica»: confused with phone notifications?
2. Waypoint: keep the English «waypoint», or «tappa» / «punto di passaggio»?
3. «Sentiero verso un vicolo cieco» / «che porta a…»? (IT-G1)
4. «È tutto pronto!» instead of «Sei pronto!»? (IT-R1)
5. Siri phrases «Soundscape dintorni / percorso / audiofaro / ferma audiofaro…»: natural?
6. Articulated prepositions are now automatic («alla Scala», «sul Corso»). Right? (IT-G1)
7. **The four detail levels** (Dettagliato / Semplificato / Essenziale / Silenzioso), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
8. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`it-IT.lproj`). **2025 → 2026 —
AI passes.** **2026-09-24 — corpus sweep** with a Microsoft comparison.
Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 2 were restored to Microsoft's wording and 4 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 2 uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 25 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_dock` → «Darsena», `osm_wrestling` → «Lotta». Uploaded live.

**2026-09-29 — Full review of all 1586 units, 21 fixes uploaded.** The Weblate log shows no human Italian edits. Unlike French, the Microsoft help text got quadrants, facing and wayfinding skills right. Fixed:
- **Meaning:**
  - `faq_supported_phones_answer`: a stale Android-only sentence, now with the iPhone/iOS 16 half (C16).
  - `faq_turn_beacon_back_on_answer`: «dopo che Soundscape lo ha disattivato». Microsoft had the user turning it off.
  - `osm_wreck` «Relitto».
  - `relative_clock_direction` «a ore %1$s»: «alle ore» is a time of day.
- **English word order in named places:** `osm_train_station_named`, `osm_subway_named`, `osm_ferry_terminal_named`, `osm_tag_ferry_terminal_named` → «Stazione ferroviaria %1$s» etc.; `osm_entrance_named_with_destination` → «%2$s {it:di %1$s} {it:da %3$s}».
- **IT-G1 templates missed by the 2026-09-25 pass:**
  - `osm_entrance_with_destination`: hard-coded «del» → «{it:di %1$s}».
  - `directions_entering_tunnel_named` → «Ingresso {it:in %1$s}».
  - `confect_name_joins` → «che collega %2$s e %3$s».
- **Grammar:**
  - `offline_maps_storage` → «salvate in %1$s, che ha %2$s liberi».
  - `offline_maps_free_space` → «liberi».
  - `offline_map_details_size_on_phone` → «sul telefono».
  - `settings_theme_contrast` → «Contrasto del tema».
  - `settings_theme_contrast_high` → «Alto».
- **Terms and markup:**
  - `annotation_description_hint`: «marcatore» → «indicatore».
  - `faq_road_names_question`: «chiama» → «annuncia» (Microsoft wording).
  - `help_text_customizing_markers_content_2`: *Modifica*.
  - `beacon_action_callout_beacon`: «Annuncia audiofaro» (Dave took the soft call).

Uploaded with `--skip-validate`; all 21 re-fetched and matched exactly.
