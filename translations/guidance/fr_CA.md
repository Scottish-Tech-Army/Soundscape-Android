# French, Canada (fr_CA) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (resource dir `values-fr-rCA`) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional fr-CA localisation (C14). 170 of 359 shared keys still verbatim, **69 drifted** |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «vous», `confirmed` (baseline) |

Read with [`_common.md`](_common.md) and [`fr.md`](fr.md).

## Status of this file

fr_CA has drifted a long way from both Microsoft's fr-CA and from fr, and the
drift is interesting: it has **already made the changes `fr.md` asks French
reviewers about**:

| | Microsoft fr-CA | fr_CA now | fr now |
|---|---|---|---|
| Waypoint | Point de repère | **Point de cheminement** | Point de repère |
| Callout | notifications | **annonces** | notifications (mostly) |
| Beacon | Balise sonore | Balise audio | Balise sonore |

So FR-T1 and FR-T2 have a live precedent. Whatever the French reviewers
decide, decide it for both, or record why they differ. Questions:
`translations/review/fr_CA.md` (Q1…Q6).

## Rules

### FRCA-T1 — Sleep and Snooze statuses are both «En veille» (`agreed` defect, `unconfirmed` wording)

`sleep_sleeping` and `sleep_snoozing` are both «En veille». The user can't
tell whether Soundscape will wake by itself. This is the same kind of
collision as Serbian SR-T1. Microsoft had «Désactivé temporairement» for
Snooze. Something like «En veille automatique» / «En pause jusqu'au
départ» is needed.

### FRCA-G1 — «vers impasse» (`agreed` defect, `unconfirmed` wording)

«%1$s vers %2$s» gives «Sentier vers impasse», with no article. The same fix
as FR-G1: «une impasse».

### FRCA-R1 — «Vous êtes prêt!» is masculine (`unconfirmed`)

Microsoft's «La configuration est terminée.» was neutral (C15).

### FRCA-S1 — Typography (`unconfirmed`, cosmetic)

«Vous êtes prêt!» has no space before «!». That's correct Québec usage and
differs from fr, where a thin space is used. Don't "fix" it from `fr.md`.

## Rejected

Nothing yet.

## Open questions

1. Waypoint «point de cheminement» and Callout «annonce»: right? (These are
   what fr is considering too.)
2. Sleep and Snooze both say «En veille». What should Snooze say? (FRCA-T1)
3. «Sentier vers une impasse»? (FRCA-G1)
4. Beacon: «balise audio» or «balise sonore»?
5. «Tout est prêt!» instead of «Vous êtes prêt!»? (FRCA-R1)
6. Anything else.

## Provenance

**2024-12 — Microsoft baseline** (`fr-CA.lproj`). **2025 → 2026 — AI passes**,
which moved the core terms (see the table above). **2026-09-24 — corpus
sweep.** Nothing uploaded.
