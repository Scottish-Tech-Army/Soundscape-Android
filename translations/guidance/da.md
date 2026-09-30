# Danish (da) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional da-DK iOS localisation (C14). 165 of 359 shared keys still verbatim, **73 drifted**, the most of any language |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «du», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Danish started from Microsoft's professional translation (C14). It has
drifted the most, but on inspection the drift is mostly tidying:
- It consolidated Microsoft's own «markør»/«mærke» split to «mærke» (130 vs 2).
- «waypoint» became «vejpunkt».
- «Kører nord» became «Kører mod nord» (24 compass strings).
- Beacon sound names were renamed. See DA-T1, since those are names users
  may know.

The VoiceOver template composes correctly («Dobbelttryk for at slå lydfyret
fra»). The Siri phrases (`da.lproj`) match the help text. Questions:
`docs/translation-questions/questions-da.md` (Q1…Q5).

## Glossary

| English | Danish | Status | Note |
|---|---|---|---|
| Callout | lydbesked | `confirmed` | Microsoft |
| Audio Beacon | lydfyr | `confirmed` | Microsoft |
| Marker | mærke | `confirmed` | Microsoft's majority form, now consistent |
| Waypoint | vejpunkt | `unconfirmed` | Microsoft mixed «vejpunkt» and «waypoint». Now consistent |
| Intersection | (vej)kryds | `confirmed` | Microsoft said «kryds», and some strings now say «vejkryds» |
| Sleep / Snooze | Dvale / I dvale ; Slumrer | `confirmed` | Microsoft. Snooze restored from «I slumretilstand» 2026-09-25 (C14 parity) |
| Detailed / Simplified / Essential / Silent | Detaljeret / Forenklet / Essentiel / Lydløs | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Detaljeret / Balanceret / Stille / Lydløs |
| dead end | Blind vej | `unconfirmed` | AI. See DA-G1 |

## Rules

### DA-T1 — Beacon style names were renamed (`unconfirmed`)

`beacon_styles_*`: «Hammer» → «Kølle», «Dråbe» → «Drop», «Glitre» →
«Glimmer», «Igangværende» → «Nuværende», «Oprindelig» → «Original». These
are the names of sounds users choose between, and long-time users may know
the Microsoft ones. Ask whether the renames are improvements or churn.

### DA-G1 — «til Blind vej» (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s til %2$s» gives «Sti til Blind vej», with a capital
letter mid-sentence. Danish usually writes «blindvej» as one word. Candidate:
«blindvej» (lowercase, one word), with or without «en».

### DA-B1 — Hints are infinitives (`agreed`, fixed 2026-09-29)

Accessibility hints (`*_hint`, `*_acc_hint`) are inserted into «Dobbelttryk for at %1$s» on iOS, and TalkBack builds «…for at <label>» from them on Android. They must be infinitives («gemme», «høre», «åbne»), as Microsoft wrote them, never imperatives («gem», «hør», «åbn»). Check any new hint for this.

### DA-T2 — Shop names end in a shop word (`agreed`, fixed 2026-09-29)

An `osm_*` "… Shop" is announced as a place, so it needs -butik, -forretning, -handel or -forhandler («Isbutik», «Dyrehandel»), not the bare goods («Is», «Kæledyr»), which are heard as the object itself.

### DA-C1 — Siri phrases are Danish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

Numbered as on the questionnaire.

1. «Sti til blindvej»? (DA-G1)
2. The four detail levels (Detaljeret / Forenklet / Essentiel / Lydløs), renamed 2026-09-30 (C22): clear by ear?
   Better names for the middle two?
3. Siri phrases «Soundscape omgivelser / rute / lydfyr / stop lydfyr…»: natural?
4. Shop names «Isbutik», «Kaffebutik», «Dyrehandel», «Isenkræmmer»…: natural? (DA-T2, AI coinages)
5. Anything else.

*Settled 2026-09-25 (C14 drift pass): beacon sound names restored to
Microsoft's («Hammer», «Dråbe», «Glitre»), DA-T1; compass phrasing restored to
Microsoft's «Kører nord».*

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`da-DK.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` → «Slumrer».

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 63 were restored to Microsoft's wording and 8 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 63 uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 28 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Full stop removed from `location_detail_full_screen_for_edit_hint`. Uploaded live.

**2026-09-29 — Full review of all 1586 units, 101 fixes uploaded.** The Weblate log shows no human Danish edits. Fixed:
- **24 accessibility hints** had drifted from Microsoft's infinitives to imperatives, which breaks «Dobbelttryk for at %1$s» (DA-B1).
- **66 `osm_*` shop names** were bare nouns (DA-T2). These are my coinages, `unconfirmed`: «Isbutik», «Kaffebutik», «Dyrehandel», «Isenkræmmer», «Spiritusbutik», «Urforretning»…
- **Microsoft errors:**
  - `first_launch_headphones_message_1`: «skal du bruge dem nu» → «så find dem frem nu».
  - `first_launch_beacon_message_1`: «det lyd» → «lyden».
  - `faq_what_can_I_set_question` → «Hvad kan jeg sætte et lydfyr på?».
  - «råbe op» → «annoncere» in the two `first_launch_callouts_*` strings.
- **Other:**
  - `no_language_selected` «Intet sprog».
  - `settings_theme_dark` «Mørkt».
  - `voice_cmd_explain_dynamic_markers`: «markører» → «mærker».
  - `osm_generic_landmark` «Landemærke».
  - *…* restored in `help_text_routes_content_how_1`.
  - `osm_tag_ferry_terminal_named` lower-case «færgeterminal».

Uploaded with `--skip-validate`; all 101 re-fetched and matched exactly.
