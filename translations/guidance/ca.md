# Catalan (ca) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Informal «tu» («Ja estàs a punt!», «T'acostes…»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Catalan.** It has been AI-only since
2026-08-23. The hints compose with «Fes doble toc per %1$s». There is no
`ca.lproj`, so the Siri phrases stay in English. Questions:
`docs/translation-questions/questions-ca.md` (Q1…Q7).

## Glossary

| English | Catalan | Status | Note |
|---|---|---|---|
| Callout | avís de veu | `unconfirmed` | |
| Audio Beacon | balisa sonora | `unconfirmed` | |
| Marker | marcador | `unconfirmed` | |
| Waypoint | punt de ruta | `unconfirmed` | |
| Intersection | cruïlla | `unconfirmed` | |
| Sleep / Snooze | Repòs ; En espera | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | Detallat / Simplificat / Essencial / Silenciós | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Detallat / Equilibrat / Discret / Silenciós |
| dead end | carrer sense sortida | `unconfirmed` | See CA-G1 |

## Rules

### CA-G1 — «a carrer sense sortida» (`agreed` defect, `unconfirmed` wording)

«%1$s a %2$s» gives «Camí a carrer sense sortida», with no article and the
wrong preposition (FR-G1 again). Candidate: «%1$s cap a %2$s» + «un carrer
sense sortida».

### CA-S1 — «gira a l'esquerra» (`unconfirmed`)

`directions_name_goes_left` «%1$s, gira a l'esquerra» ("…, turns left", road
as subject). Catalan imperative «gira!» is identical, so the listener may
hear an instruction (C11, like PTBR-S1). «va cap a l'esquerra» avoids it.

### CA-R1 — «Benvingut!» is masculine (`unconfirmed`)

`first_launch_welcome_title`. «Et donem la benvinguda!» is the usual neutral
form (C15 applies to the same pattern).

### CA-R2 — The FAQ is in «vós», the rest in «tu» (`agreed` defect, fix waits on Q4)

24 long strings use «vós» («Podeu…», «la vostra destinació»): 21 FAQ answers,
`settings_section_media_controls_description`, `settings_head_tracking_description`,
`offline_map_storage_description`, `accessibility_screen_reader_enabled` and
`new_version_info_details`. Everything else, hints included, uses «tu». Once Q4
is answered, convert the minority to match.

## Rejected

Nothing yet.

## Open questions

1. «Carrer Major, gira a l'esquerra»: does it sound like an instruction? (CA-S1)
2. «Camí cap a un carrer sense sortida»? (CA-G1)
3. «Et donem la benvinguda!» instead of «Benvingut!»? (CA-R1)
4. Is «tu» right? The FAQ currently uses «vós» (CA-R2).
5. Callout «avís de veu»: natural?
6. Beacon «balisa sonora»: natural? (AI-only term, asked for confirmation)
7. Anything else.

## Provenance

**2026-08-23 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-29 — European batch review.** 3 hints drifted from «Fes doble toc per %1$s» («Mostra…», «restableix…») → infinitives; `annotation_description_hint` «ajudar-vos» → «ajudar-te». 4 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live. **Held:** 24 long strings (21 FAQ answers, `settings_section_media_controls_description`, `settings_head_tracking_description`, `offline_map_storage_description`, `accessibility_screen_reader_enabled`, `new_version_info_details`) are in «vós» while the rest of the app is «tu» (CA-R2, Q4).
