# English, UK (en_GB) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (resource dir `values-en-rGB`) |
| Corpus at last sweep | 1522 units, 249 differing from the US source (2026-09-24) |
| Baseline | Microsoft's en-GB iOS localisation (C14). 232 of 358 shared keys still verbatim, only 2 drifted |
| Last human input | 2026-09-25 (maintainer, a UK English speaker: EN-T1, EN-T2, EN-T3) and 2026-05 (JJ Gatchalian: VoiceOver hint casing, as in `es.md`) and 2026-04 (Luke Duncan: navigating-screen text) |
| Register | — |

Read with [`_common.md`](_common.md).

## Status of this file

en_GB is a spelling-and-vocabulary layer over the US source: «Metres»,
«favourite», «Travelling», «Public Transport», «Pavement». It's in good
shape, with only «program» ×2 and «license» ×3 remaining, and all three
«license» hits are GitHub URLs, which are correct. The one real question is
vocabulary Microsoft itself left American (EN-T1), now settled. Questions:
`docs/translation-questions/questions-en_GB.md` (Q1…Q2).

## Rules

### EN-T1 — Keep «intersection» (`confirmed`, 2026-09-25)

28 × «intersection», 5 × «junction». Microsoft's en-GB also kept
«intersection» (46 vs 1). Confirmed by a UK English speaker: "A junction is
really roads only, and a path meeting a road wouldn't normally be called a
junction." Soundscape's crossings are often path-meets-road, so «intersection»
is the right general word. Don't sweep to «junction».

### EN-T2 — Shop words follow UK usage per case (`confirmed`, 2026-09-25)

There is no general «store» → «shop» swap. The UK words are:
- **grocery store** stays: "there's no such thing as a grocery shop"
  (`groceries_places_nearby_description` «see a list of nearby grocery stores»);
- **supermarket** for a large one, **corner shop** for a small one, and
  **grocer's / greengrocer** for a fruit-and-vegetable shop
  (`osm_greengrocer` «Greengrocer» and `osm_supermarket` «Supermarket» already
  match);
- «App Store» / «Play Store» and the verb «store» stay.

Open: whether «Convenience Store» (`osm_convenience_store`, and
`filter_groceries` «Groceries and Convenience Stores») should become «Corner
Shop». It is the OSM `shop=convenience` category, which in the UK covers
corner shops but also chain convenience stores. Not changed.

### EN-T3 — «pavement», never «sidewalk»; «transit» is fine (`confirmed`, 2026-09-25)

"Sidewalk should never be used, it should always be pavement." A sweep found
no «sidewalk» in `values-en-rGB`, and both US strings containing it
(`confect_name_pavement`, `confect_name_pavement_next_to`) have GB overrides
saying «Pavement». New US strings with "sidewalk" need a GB override.
«transit» (9 uses) is acceptable in UK English and stays.

### EN-S1 — Title Case drift (`unconfirmed`, cosmetic)

A few strings were capitalised differently from the US source (`beacon_settings_style`
«Audio Beacon Styles» vs «Audio beacon styles»; `settings_collapse_section`
«Collapse section» vs «collapse section»). The lowercase first letter in hint
strings is deliberate: it's JJ's VoiceOver convention (#889). Don't "fix" it.

## Rejected

Nothing yet.

## Open questions

Numbered as on the questionnaire.

1. Anything else that reads as American to a UK ear?
2. Anything else.

*Settled 2026-09-25: «intersection» stays (EN-T1); shop words per case, with
«grocery store» kept (EN-T2); «pavement» always, «transit» fine (EN-T3).*

## Provenance

**2024-12 — Microsoft baseline** (`en-GB.lproj`).
**2026-04 — Luke Duncan**, navigating-screen text.
**2026-05 — JJ Gatchalian**, hint casing and trailing periods (VoiceOver
convention, #889).
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 0 were restored to Microsoft's wording and 2 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). Nothing to upload.

**2026-09-25 — questionnaire answered by a UK English speaker.** Q1: keep
«intersection» (EN-T1). Q2: «grocery store» stays; supermarket / corner shop /
grocer's by size and kind (EN-T2). Q3: «pavement» always, «transit» fine
(EN-T3). Nothing to upload: the corpus already matches.
