# Italian (it) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional it-IT localisation (C14). 234 of 359 shared keys still verbatim, only 8 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «tu», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Italian is the best-preserved Microsoft baseline. The hints compose
correctly («Tocca due volte per disattivare l'audiofaro»), «è sulla
sinistra» is descriptive (C11), and the Siri phrases (`it.lproj`) match the
help text. Questions: `docs/translation-questions/questions-it.md` (Q1…Q6).

## Glossary

| English | Italian | Status | Note |
|---|---|---|---|
| Callout | notifica | `confirmed` | Microsoft. It also means a system notification, like fr. Ask once |
| Audio Beacon | audiofaro | `confirmed` | Microsoft |
| Marker | indicatore | `confirmed` | Microsoft |
| Waypoint | waypoint | `confirmed` | Microsoft, English loan (C12) |
| Intersection | incrocio | `confirmed` | Microsoft |
| Sleep / Snooze | Sospendi ; Posponi | `confirmed` | Microsoft |
| Detail levels | Dettagliato / Bilanciato / Discreto / Silenzioso | `unconfirmed` | AI. Distinct |
| dead end | vicolo cieco | `unconfirmed` | AI. See IT-G1 |

## Rules

### IT-G1 — «a vicolo cieco» (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s a %2$s» gives «Sentiero a vicolo cieco», with no
article and the wrong preposition, which is FR-G1 again. Candidates:
«%1$s verso %2$s» with «un vicolo cieco», or «%1$s che porta a %2$s».

### IT-R1 — «Sei pronto!» is masculine (`unconfirmed`)

See C15. «È tutto pronto!» avoids gender.

## Rejected

Nothing yet.

## Open questions

1. Callout «notifica»: confused with phone notifications?
2. Waypoint: keep the English «waypoint», or «tappa» / «punto di passaggio»?
3. «Sentiero verso un vicolo cieco» / «che porta a…»? (IT-G1)
4. «È tutto pronto!» instead of «Sei pronto!»? (IT-R1)
5. Siri phrases «Soundscape dintorni / percorso / audiofaro / ferma audiofaro…»: natural?
6. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`it-IT.lproj`). **2025 → 2026 —
AI passes.** **2026-09-24 — corpus sweep** with a Microsoft comparison.
Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 2 were restored to Microsoft's wording and 4 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 2 uploaded and verified live.
