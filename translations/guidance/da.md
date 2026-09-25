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
`docs/translation-questions/questions-da.md` (Q1…Q4).

## Glossary

| English | Danish | Status | Note |
|---|---|---|---|
| Callout | lydbesked | `confirmed` | Microsoft |
| Audio Beacon | lydfyr | `confirmed` | Microsoft |
| Marker | mærke | `confirmed` | Microsoft's majority form, now consistent |
| Waypoint | vejpunkt | `unconfirmed` | Microsoft mixed «vejpunkt» and «waypoint». Now consistent |
| Intersection | (vej)kryds | `confirmed` | Microsoft said «kryds», and some strings now say «vejkryds» |
| Sleep / Snooze | Dvale / I dvale ; Slumrer | `confirmed` | Microsoft. Snooze restored from «I slumretilstand» 2026-09-25 (C14 parity) |
| Detail levels | Detaljeret / Balanceret / Stille / Lydløs | `unconfirmed` | AI. Distinct |
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

### DA-C1 — Siri phrases are Danish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

Numbered as on the questionnaire.

1. «Sti til blindvej»? (DA-G1)
2. The four detail levels (Detaljeret / Balanceret / Stille / Lydløs): clear?
   «Stille» and «Lydløs» sit close together.
3. Siri phrases «Soundscape omgivelser / rute / lydfyr / stop lydfyr…»: natural?
4. Anything else.

*Settled 2026-09-25 (C14 drift pass): beacon sound names restored to
Microsoft's («Hammer», «Dråbe», «Glitre»), DA-T1; compass phrasing restored to
Microsoft's «Kører nord».*

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`da-DK.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` → «Slumrer».

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 63 were restored to Microsoft's wording and 8 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 63 uploaded and verified live.
