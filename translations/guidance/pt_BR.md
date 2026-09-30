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
`docs/translation-questions/questions-pt_BR.md` (Q1…Q9).

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

### PTBR-G1 — Article chosen from the name, not fixed feminine (`fixed` in code, 2026-09-25)

Same as PT-G1: «ao longo da %1$s», «Na %1$s» assumed a feminine type. 51 templates
now wrap the preposition, «{pt:da %1$s}», and `resolveGrammarMarkers()` (C18)
picks the article from the name: «ao longo do Parque Ibirapuera», «Na Av.
Paulista», «Próximo ao Mercado Municipal». Unknown names keep the template's
wording. Measured on the Rio Grande do Sul extract: 93% of Portuguese-language street names and about 35% of place names (the rest are business names and keep the template wording). Acronyms with a fixed gender are covered: «na UBS», «na EMEF», «no CAPS», «no CTG». **New templates must wrap map-name prepositions.**

## Rejected

«Em Ociosidade» for Snooze (Microsoft). It means "idle" and was replaced by
«Em Soneca». Don't restore it from Microsoft's file.

## Open questions

1. Marker «Favoritos» and Waypoint «Localizador»: natural? (PTBR-T1)
2. «Rua X, vira à esquerda»: does it sound like an instruction? (PTBR-S1)
3. «Caminho para uma rua sem saída»? (PTBR-G1)
4. «Tudo pronto!» again? And «Boas-vindas!» instead of «Bem-vindo(a)!»? (PTBR-R1) The same brackets are in `first_launch_prompt_message` «Você está pronto(a) para…», which is held until this is answered.
5. Callout «notificação»: confused with phone notifications?
6. Siri phrases «Soundscape arredores / rota / sinalizador / parar sinalizador…»: natural?
7. Articles are now chosen from the name («ao longo do Parque Ibirapuera»). Right? (PTBR-G1)
8. **The four detail levels** (Detalhado / Simplificado / Essencial / Silencioso), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
9. Anything else.

## Provenance

**2024-12 — Microsoft baseline** (`pt-BR.lproj`). **2025 → 2026 — AI
passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity.** Dave kept «Em Soneca» as the one exception to the Microsoft-parity restore, because «Em Ociosidade» doesn't describe the mode.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 4 were restored to Microsoft's wording and 4 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 4 uploaded and verified live.

**2026-09-25 — truncation repaired (C16).** `faq_holding_phone_flat_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 28 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `settings_theme_contrast_regular` → «Normal», `osm_village` → «Povoado» (it had been «Vila», same as town). Uploaded live.

**2026-09-29 — full review (1586 units).** 26 flagged, and 25 were uploaded and verified live. Six were
Microsoft's own errors, fixed under C14: «quatro quarteirões» (city blocks) → «quadrantes»,
«wayfinding» left in English (×2), «obter as localizações» → «chegar aos locais próximos», «o que escuto
quando ouço», «linguagem» → «idioma», «e para de se mover» → «e parar». Also fixed: Around Me «vários» →
«quatro», typo «trÊs», «à caminho», «Pesquisar idioma» → «Idioma da pesquisa», the double «para para» and
the imperative hint that broke the TalkBack template, lost `*…*` in the routes help, «chamadas» →
«notificações», «Colocar em Soneca» → «Soneca», «Lixeira de Reciclagem». Word order was fixed in 4 named
stations/terminals, and {pt:…} markers were added in 4 templates (railway, tunnel, both entrances,
which had a fixed «do»). Held: `first_launch_prompt_message` «pronto(a)» (with Q4).
