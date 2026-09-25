# Portuguese, Brazil (pt_BR) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (resource dir `values-pt-rBR`) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional pt-BR localisation (C14). 232 of 359 shared keys still verbatim, only 8 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «você», `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

A well-preserved Microsoft baseline, but Microsoft made two unusual term
choices (PTBR-T1) that are worth putting to a speaker. The hints compose
correctly. The Siri phrases (`pt-BR.lproj`) match the help text. Questions:
`translations/review/pt_BR.md` (Q1…Q7).

## Glossary

| English | Brazilian Portuguese | Status | Note |
|---|---|---|---|
| Callout | notificação | `confirmed` | Microsoft. Collides with system notifications |
| Audio Beacon | Sinalizador Sonoro | `confirmed` | Microsoft |
| Marker | **Favoritos** | `confirmed` | Microsoft. See PTBR-T1 |
| Waypoint | **Localizador** | `confirmed` | Microsoft ("locator"). See PTBR-T1 |
| Intersection | cruzamento | `confirmed` | Microsoft |
| Sleep / Snooze | Suspensão ; Soneca | `agreed` (exception to C14 parity, Dave 2026-09-25) | Snooze drifted from Microsoft's «Em Ociosidade» ("idle") to «Em Soneca», which is the alarm-clock word and probably better |
| dead end | sem saída | `unconfirmed` | AI. See PTBR-G1 |

## Rules

### PTBR-G1 — «para sem saída» (`agreed` defect, `unconfirmed` wording)

`confect_name_dead_end` is the bare adjective «sem saída» ("with no exit"),
so `confect_name_to` gives «Caminho para sem saída». Candidate: «uma rua sem
saída».

### PTBR-T1 — Microsoft's «Favoritos» and «Localizador» (`confirmed`, but ask once)

Marker = «Favoritos» (favourites) and Waypoint = «Localizador» (locator). Both
are professional choices that users have heard for years, but neither is
the obvious word: a marker isn't necessarily a favourite, and a route stop
isn't a locator. Ask whether they feel natural before defending them.

### PTBR-S1 — «vira à esquerda» can be heard as an instruction (`unconfirmed`)

`directions_name_goes_left` «%1$s, vira à esquerda» means "…, turns left",
with the road as subject. But colloquial Brazilian imperative «vira!» is
identical, so the listener may hear "turn left!" (C11). «segue à esquerda»
or «vai para a esquerda» would avoid it.

### PTBR-R1 — «Você está pronto!» is masculine (`agreed` defect, `unconfirmed` wording)

Microsoft's «Tudo pronto!» was neutral (C15). Revert.

`first_launch_welcome_title` «Bem-vindo(a)!» has the same root problem, and a
screen reader reads the brackets aloud. «Boas-vindas!» or «Olá!» avoids both.

## Rejected

«Em Ociosidade» for Snooze (Microsoft). It means "idle" and was replaced by
«Em Soneca». Don't restore it from Microsoft's file.

## Open questions

1. Marker «Favoritos» and Waypoint «Localizador»: natural? (PTBR-T1)
2. «Rua X, vira à esquerda»: does it sound like an instruction? (PTBR-S1)
3. «Caminho para uma rua sem saída»? (PTBR-G1)
4. «Tudo pronto!» again? And «Boas-vindas!» instead of «Bem-vindo(a)!»? (PTBR-R1)
5. Callout «notificação»: confused with phone notifications?
6. Siri phrases «Soundscape arredores / rota / sinalizador / parar sinalizador…»: natural?
7. Anything else.

## Provenance

**2024-12 — Microsoft baseline** (`pt-BR.lproj`). **2025 → 2026 — AI
passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity.** Dave kept «Em Soneca» as the one exception to the Microsoft-parity restore, because «Em Ociosidade» doesn't describe the mode.
