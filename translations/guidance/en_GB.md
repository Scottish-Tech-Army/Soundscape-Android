# English, UK (en_GB) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (resource dir `values-en-rGB`) |
| Corpus at last sweep | 1522 units, 249 differing from the US source (2026-09-24) |
| Baseline | Microsoft's en-GB iOS localisation (C14). 232 of 358 shared keys still verbatim, only 2 drifted |
| Last human input | 2026-05 (JJ Gatchalian: VoiceOver hint casing, as in `es.md`) and 2026-04 (Luke Duncan: navigating-screen text) |
| Register | — |

Read with [`_common.md`](_common.md).

## Status of this file

en_GB is a spelling-and-vocabulary layer over the US source: «Metres»,
«favourite», «Travelling», «Public Transport», «Pavement». It's in good
shape, with only «program» ×2 and «license» ×3 remaining, and all three
«license» hits are GitHub URLs, which are correct. The one real question is
vocabulary Microsoft itself left American (EN-T1). Questions:
`translations/review/en_GB.md` (Q1…Q4).

## Rules

### EN-T1 — «intersection» vs «junction» (`unconfirmed`)

28 × «intersection», 5 × «junction». Microsoft's en-GB also kept
«intersection» (46 vs 1), so it was a deliberate choice, but UK pedestrians
say "junction". A change would touch every callout announcing a crossing
road, and `osm_*` names too. Ask UK users, and Scottish users especially,
since this is the project's home.

### EN-T2 — «store» vs «shop» (`unconfirmed`, check before sweeping)

7 × «store», 4 × «shop». Some «store» uses are "App Store" / "Play Store"
and must stay. Inventory only.

### EN-S1 — Title Case drift (`unconfirmed`, cosmetic)

A few strings were capitalised differently from the US source (`beacon_settings_style`
«Audio Beacon Styles» vs «Audio beacon styles»; `settings_collapse_section`
«Collapse section» vs «collapse section»). The lowercase first letter in hint
strings is deliberate: it's JJ's VoiceOver convention (#889). Don't "fix" it.

## Rejected

Nothing yet.

## Open questions

1. «Approaching intersection» or «Approaching junction»? (EN-T1)
2. «store» or «shop» in general text? (EN-T2)
3. Anything that reads as American to a UK ear?
4. Anything else.

## Provenance

**2024-12 — Microsoft baseline** (`en-GB.lproj`).
**2026-04 — Luke Duncan**, navigating-screen text.
**2026-05 — JJ Gatchalian**, hint casing and trailing periods (VoiceOver
convention, #889).
**2026-09-24 — corpus sweep.** Nothing uploaded.
