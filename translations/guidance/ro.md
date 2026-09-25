# Romanian (ro) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Informal «tu» (only 1 «dumneavoastră»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Romanian.** It has been AI-only since
2026-02-08. Things that already work:
- The hints are «a»-infinitives that compose with «Atinge de două ori pentru
  %1$s».
- `confect_name_to` «%1$s spre %2$s» + «fundătură» is grammatical.
- «continuă la stânga» is descriptive (C11).
- There is no `ro.lproj`, so the Siri phrases stay in English (PL-C1).

Questions: `docs/translation-questions/questions-ro.md` (Q1…Q6).

## Glossary

| English | Romanian | Status | Note |
|---|---|---|---|
| Callout | anunț | `unconfirmed` | |
| Audio Beacon | baliză audio | `unconfirmed` | |
| Marker | marcaj | `unconfirmed` | |
| Waypoint | punct de traseu | `unconfirmed` | |
| Landmarks | repere | `unconfirmed` | |
| Intersection | intersecție | `unconfirmed` | |
| Sleep / Snooze | Repaus ; Amânare | `unconfirmed` | |
| Detail levels | Detaliat / Echilibrat / Discret / Silențios | `unconfirmed` | Distinct |
| dead end | fundătură | `unconfirmed` | |

## Rules

### RO-R1 — «tu» and a masculine default (`unconfirmed`)

The app is informal («Atinge», «Ești pregătit!»). Formal «dumneavoastră» is
more usual in Romanian software, so ask. «Ești pregătit!» is masculine
(C15). «Totul este gata!» avoids gender.

## Rejected

Nothing yet.

## Open questions

1. Register: «tu» or «dumneavoastră»? (RO-R1)
2. «Totul este gata!» instead of «Ești pregătit!»? (RO-R1)
3. Callout «anunț»: natural?
4. Snooze «În amânare»: clear?
5. Beacon «baliză audio»: natural?
6. Anything else.

## Provenance

**2026-02-08 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `faq_turn_beacon_back_on_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
