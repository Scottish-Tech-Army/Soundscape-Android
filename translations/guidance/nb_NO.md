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

Questions are confirmations. Questions: `translations/review/nb_NO.md`
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
| Detail levels | Detaljert / Balansert / Stille / Lydløs | `unconfirmed` | AI. Distinct |
| dead end | blindvei | `unconfirmed` | AI |

## Rules

### NB-C1 — Siri phrases are Norwegian and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «melding»: confused with text messages or notifications?
2. The four detail levels (Detaljert / Balansert / Stille / Lydløs): clear?
3. Siri phrases «Soundscape omgivelser / rute / lydsignal / stopp lydsignal…»: natural?
4. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`nb-NO.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 22 were restored to Microsoft's wording and 3 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 22 uploaded and verified live.
