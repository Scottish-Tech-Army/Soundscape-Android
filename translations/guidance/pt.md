# Portuguese, European (pt) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional pt-PT localisation (C14). 233 of 359 shared keys still verbatim, only 7 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | Formal third person («Está…»), consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

A well-preserved Microsoft baseline. Microsoft's Title Case style («Sinal de
Áudio», «Ponto de Passagem») is carried through, which is a house style and
not an error. The hints compose correctly («Toque duas vezes para desativar
o Sinal de Áudio»), and the Siri phrases (`pt.lproj`) match the help text.
Questions: `translations/review/pt.md` (Q1…Q5).

## Glossary

| English | Portuguese | Status | Note |
|---|---|---|---|
| Callout | aviso | `confirmed` | Microsoft |
| Audio Beacon | Sinal de Áudio | `confirmed` | Microsoft |
| Marker | marco | `confirmed` | Microsoft |
| Waypoint | ponto de passagem | `confirmed` | Microsoft |
| Intersection | cruzamento | `confirmed` | Microsoft |
| Sleep / Snooze | Suspensão ; Pausa | `confirmed` | Microsoft |
| Traveling / Heading | A viajar / A caminhar | `confirmed` | Microsoft. The vehicle/walking split |
| Detail levels | Detalhado / Equilibrado / Discreto / Silencioso | `unconfirmed` | AI |
| dead end | beco sem saída | `unconfirmed` | AI. See PT-G1 |

## Rules

### PT-G1 — «para beco sem saída» (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s para %2$s» gives «Caminho para beco sem saída», with
no article. Candidate: dead end → «um beco sem saída».

### PT-R1 — Drift introduced a gender (`agreed` defect, `unconfirmed` wording)

Microsoft's «Está tudo pronto!» was neutral. It is now «Está pronto!»
(masculine), following our English change (C15). Reverting to «Está tudo
pronto!» is the obvious fix.

## Rejected

Nothing yet.

## Open questions

1. «Caminho para um beco sem saída»? (PT-G1)
2. «Está tudo pronto!» again? (PT-R1)
3. The four detail levels (Detalhado / Equilibrado / Discreto / Silencioso): clear?
4. Siri phrases «Soundscape arredores / rota / sinal / parar sinal…»: natural?
5. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`pt-PT.lproj`). **2025 → 2026 —
AI passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 4 were restored to Microsoft's wording and 2 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 4 uploaded and verified live.
