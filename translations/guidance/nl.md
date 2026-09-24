# Dutch (nl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional nl-NL iOS localisation (C14). 209 of 359 shared keys still verbatim, 29 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | Formal «u», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Dutch started from Microsoft's professional translation (C14). One core
term has **changed** away from Microsoft (NL-T1). The Siri phrases (`nl.lproj`)
match the help text. Questions: `translations/review/nl.md` (Q1…Q6).

## Glossary

| English | Dutch | Status | Note |
|---|---|---|---|
| Callout | aankondiging | `unconfirmed` | **Microsoft said «waarschuwing»**. See NL-T1 |
| Audio Beacon | audiobaken | `confirmed` | Microsoft |
| Marker | markering | `confirmed` | Microsoft |
| Waypoint | routepunt | `confirmed` | Microsoft |
| Intersection | kruispunt | `confirmed` | Microsoft |
| Sleep / Snooze | Slapen / Slaapstand ; Sluimerstand | `confirmed` | Microsoft |
| Traveling / Heading | U rijdt / U loopt naar het noorden | `confirmed` | Microsoft. The vehicle/walking split, spelled out |
| Detail levels | Gedetailleerd / Gebalanceerd / Rustig / Stil | `unconfirmed` | AI. Distinct |
| Landmarks | herkenningspunten | `unconfirmed` | AI |
| dead end | Doodlopende weg | `unconfirmed` | AI. See NL-G1 |

## Rules

### NL-T1 — Callout moved from «waarschuwing» to «aankondiging» (`unconfirmed`)

Microsoft's word was «waarschuwingen» ("warnings"), which frames every
callout as an alert. A later AI pass switched all of them (70 now, 0 left) to
«aankondigingen» ("announcements"), which fits the concept better. That
makes it a C14 case where the drift is probably right, but it replaced a
professional choice, so get it confirmed and then defend it.

### NL-B1 — VoiceOver: «om» needs «te» (`agreed` defect, `unconfirmed` wording)

«Dubbel tik om %1$s» + bare-infinitive hints («het audiobaken dempen») gives
«Dubbel tik om het audiobaken dempen» (should be «…te dempen»). ~40 of the 43
hints are affected. Fix the **template only** (C13): «Dubbeltik: %1$s». While
there, «Dubbel tik» should be «Dubbeltik», which is Microsoft's spelling and
Apple's.

### NL-G1 — «naar Doodlopende weg» (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s naar %2$s» gives «Pad naar Doodlopende weg»: a
capital letter mid-sentence and no article. Candidates: dead end →
«een doodlopende weg», or the template «Doodlopend %1$s» for this case.

### NL-C1 — Siri phrases are Dutch and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet. If NL-T1 is confirmed, record «waarschuwing» here with
Microsoft as the evidence for it (C8).

## Open questions

1. Callout: «aankondiging» (now) or «waarschuwing» (Microsoft)? (NL-T1)
2. VoiceOver: «Dubbeltik: het audiobaken dempen»? (NL-B1)
3. «Pad naar een doodlopende weg» or «Doodlopend pad»? (NL-G1)
4. The four detail levels (Gedetailleerd / Gebalanceerd / Rustig / Stil): clear?
5. Siri phrases «Soundscape omgeving / route / baken / stop baken…»: natural?
6. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`nl-NL.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.
