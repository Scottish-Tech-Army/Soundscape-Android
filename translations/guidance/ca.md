# Catalan (ca) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Informal «tu» («Ja estàs a punt!», «T'acostes…»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Catalan.** It has been AI-only since
2026-08-23. The hints compose with «Fes doble toc per %1$s». There is no
`ca.lproj`, so the Siri phrases stay in English. Questions:
`docs/translation-questions/questions-ca.md` (Q1…Q7).

## Glossary

| English | Catalan | Status | Note |
|---|---|---|---|
| Callout | avís de veu | `unconfirmed` | |
| Audio Beacon | balisa sonora | `unconfirmed` | |
| Marker | marcador | `unconfirmed` | |
| Waypoint | punt de ruta | `unconfirmed` | |
| Intersection | cruïlla | `unconfirmed` | |
| Sleep / Snooze | Repòs ; En espera | `unconfirmed` | |
| Detail levels | Detallat / Equilibrat / Discret / Silenciós | `unconfirmed` | |
| dead end | carrer sense sortida | `unconfirmed` | See CA-G1 |

## Rules

### CA-G1 — «a carrer sense sortida» (`agreed` defect, `unconfirmed` wording)

«%1$s a %2$s» gives «Camí a carrer sense sortida», with no article and the
wrong preposition (FR-G1 again). Candidate: «%1$s cap a %2$s» + «un carrer
sense sortida».

### CA-S1 — «gira a l'esquerra» (`unconfirmed`)

`directions_name_goes_left` «%1$s, gira a l'esquerra» ("…, turns left", road
as subject). Catalan imperative «gira!» is identical, so the listener may
hear an instruction (C11, like PTBR-S1). «va cap a l'esquerra» avoids it.

### CA-R1 — «Benvingut!» is masculine (`unconfirmed`)

`first_launch_welcome_title`. «Et donem la benvinguda!» is the usual neutral
form (C15 applies to the same pattern).

## Rejected

Nothing yet.

## Open questions

1. «Carrer Major, gira a l'esquerra»: does it sound like an instruction? (CA-S1)
2. «Camí cap a un carrer sense sortida»? (CA-G1)
3. «Et donem la benvinguda!» instead of «Benvingut!»? (CA-R1)
4. Is «tu» right?
5. Callout «avís de veu»: natural?
6. Beacon «balisa sonora»: natural? (AI-only term, asked for confirmation)
7. Anything else.

## Provenance

**2026-08-23 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.
