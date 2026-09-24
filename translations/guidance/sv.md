# Swedish (sv) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional sv-SE iOS localisation (C14). 225 of 359 shared keys still verbatim, 15 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «du», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Swedish started from Microsoft's professional translation (C14) and has
drifted the least of the Nordic languages. The VoiceOver template composes
correctly («Dubbeltryck för att stänga av ljudfyren»). The Siri phrases
(`sv.lproj`) match the help text. There is one real inconsistency (SV-T1).
Questions: `translations/review/sv.md` (Q1…Q5).

## Glossary

| English | Swedish | Status | Note |
|---|---|---|---|
| Callout | informationsljud | `confirmed` | Microsoft |
| Audio Beacon | ljudfyr | `confirmed` | Microsoft |
| Marker | platsmarkör | `confirmed` | Microsoft |
| Waypoint | brytpunkt | `confirmed` | Microsoft |
| Intersection | vägkorsning | `confirmed` | Microsoft |
| Sleep / Snooze | Viloläge / I viloläge ; Snoozeläge | `unconfirmed` | Status aligned 2026-09-24. See SV-T1 |
| Traveling / Heading | Reser / På väg norrut | `confirmed` | Microsoft |
| Detail levels | Detaljerad / Balanserad / Lågmäld / Tyst | `unconfirmed` | AI. Distinct, and «Lågmäld» is a nice choice for "fewer" |
| dead end | återvändsgata | `unconfirmed` | AI |

## Rules

### SV-T1 — The Sleep button and its status no longer match (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `sleep_sleeping` «Inaktiverad» → «I viloläge», uploaded and verified live. «I viloläge» (status) stays distinct from the button «Viloläge». Serbian SR-T1 shows why that matters.

Microsoft's pair was «Inaktivera» (button) / «Inaktiverad» (status). An AI
pass changed the button to «Viloläge» ("sleep mode") and left the status as
«Inaktiverad». «Viloläge» has since spread to 11 strings, including the
sleep message, the a11y hint and every FAQ answer about it. So the outlier
is now the **status alone**: `sleep_sleeping` → «I viloläge» is a one-string
fix. The other «inaktiver-» uses in the corpus mean "turn off" in general
(C5) and stay. This is the case recorded in `_common.md` C14.

### SV-C1 — Siri phrases are Swedish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Sleep: the button is «Viloläge» and the status is now «I viloläge»
   (changed from «Inaktiverad»). Right? (SV-T1)
2. Snooze «Snoozeläge»: natural?
3. «Stig till återvändsgata»: natural, or with «en»?
4. Siri phrases «Soundscape omgivning / rutt / ljudfyr / stoppa ljudfyr…»: natural?
5. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`sv-SE.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-24 — SV-T1 applied.** `sleep_sleeping` → «I viloläge», uploaded and verified live.
