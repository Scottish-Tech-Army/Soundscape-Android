# Norwegian Bokmål (nb_NO) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (Weblate code `nb_NO`, resource dir `values-nb`) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional nb-NO iOS localisation (C14). 212 of 359 shared keys still verbatim, 26 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «du», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Norwegian started from Microsoft's professional translation (C14), and every
core term is still Microsoft's. There are no defects:
- The VoiceOver template composes correctly («Dobbelttrykk for å slå av
  lydsignalet»).
- «blindvei» is lowercase and reads naturally in «Sti til blindvei».
- The Siri phrases (`nb.lproj`) match the help text.

Questions are confirmations. Questions: `docs/translation-questions/questions-nb_NO.md`
(Q1…Q4).

## Glossary

| English | Norwegian | Status | Note |
|---|---|---|---|
| Callout | melding | `confirmed` | Microsoft |
| Audio Beacon | lydsignal | `confirmed` | Microsoft |
| Marker | markør | `confirmed` | Microsoft |
| Waypoint | veipunkt | `confirmed` | Microsoft |
| Intersection | veikryss | `confirmed` | Microsoft |
| Sleep / Snooze | dvalemodus ; pausemodus | `confirmed` | Microsoft |
| Traveling / Heading | Kjører / Du går mot nord | `confirmed` | Microsoft. The vehicle/walking split |
| Detailed / Simplified / Essential / Silent | Detaljert / Forenklet / Grunnleggende / Lydløs | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Detaljert / Balansert / Stille / Lydløs |
| dead end | blindvei | `unconfirmed` | AI |

## Rules

### NB-B1 — Hints are infinitives (`agreed`, fixed 2026-09-29)

Accessibility hints go into «Dobbelttrykk for å %1$s» (and TalkBack's «…for å <label>»), so they must be infinitives («dele», «høre», «legge til»), as Microsoft wrote them, never imperatives («del», «hør», «legg til»). Check any new hint. See DA-B1.

### NB-C1 — Siri phrases are Norwegian and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «melding»: confused with text messages or notifications?
2. The four detail levels (Detaljert / Forenklet / Grunnleggende / Lydløs), renamed 2026-09-30 (C22): clear by ear? Better names for the middle two?
3. Siri phrases «Soundscape omgivelser / rute / lydsignal / stopp lydsignal…»: natural?
4. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`nb-NO.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 22 were restored to Microsoft's wording and 3 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 22 uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Rugby → «Rugby league-bane» / «Rugby union-bane» (both had been «Rugbybane»). Uploaded live.

**2026-09-29 — Full review of all 1586 units, 38 fixes uploaded.** The Weblate log shows no human Norwegian edits. Fixed:
- **30 accessibility hints** had drifted from Microsoft's infinitives to imperatives (NB-B1), including the collapse/expand section hints.
- **`ui_action_button_nearby_markers`:** «Markører⏎I nærhet» → «Markører⏎i nærheten».
- **Typos and parse errors:** «en knappene» → «en av knappene»; «alternativet for å *Del*» → «alternativet *Del*».
- **Markup:** *…* restored in `help_text_routes_content_how_1`.
- **Tunnel callouts:** «går inn» → «kjører inn» (travel mode).
- **`menu_open_source_licenses`:** «Lisenser for åpen kildekode» (it read as a command).
- **`osm_deli`:** «Delikatessebutikk».

Uploaded with `--skip-validate`; all 38 re-fetched and matched exactly.
