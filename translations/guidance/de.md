# German (de) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional de-DE iOS localisation (C14). 218 of 359 shared keys still verbatim, 20 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | Formal «Sie», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

German started from Microsoft's professional translation, and its core
terms are all still Microsoft's. Those are marked `confirmed` in the C14
sense: a professional choice users have heard for years. Everything
Microsoft never had (callout detail, confected way names, voice commands,
travel mode, ~410 keys) is AI. The 20 drifted strings look like
improvements: they fix Microsoft errors such as «Endpunkt» for "Done" and
«Wie verwenden ich». The Siri phrases (`de.lproj`) match the help text.
Questions: `docs/translation-questions/questions-de.md` (Q1…Q5).

## Glossary

| English | German | Status | Note |
|---|---|---|---|
| Callout | Hinweis | `confirmed` | Microsoft |
| Audio Beacon | Audiobeacon | `confirmed` | Microsoft. An anglicism, but established |
| Marker | Markierung | `confirmed` | Microsoft |
| Waypoint | Wegpunkt | `confirmed` | Microsoft |
| Intersection | Kreuzung | `confirmed` | Microsoft |
| Sleep / Snooze | Ruhemodus / Standbymodus | `confirmed` | Microsoft. Button restored to «Ruhemodus aktivieren» 2026-09-25 (C14 parity) |
| Traveling / Heading | Nach Norden fahrend / Richtung Norden | `confirmed` | Microsoft. Matches the vehicle/walking split |
| Detail levels | Ausführlich / Ausgewogen / Leise / Stumm | `unconfirmed` | AI. Distinct |
| Landmarks | Orientierungspunkte | `unconfirmed` | AI |
| dead end | Sackgasse | `unconfirmed` | AI. See DE-G1 |

## Rules

### DE-B1 — VoiceOver: «um» needs a zu-infinitive (`agreed` defect, `unconfirmed` wording)

`talkback_double_tap_template` is «Doppeltippen, um %1$s», but ~37 of the
43 hints are bare infinitives («Audiobeacon stummschalten»). VoiceOver says
«Doppeltippen, um Audiobeacon stummschalten», which is ungrammatical. Fix the
**template only** (C13): «Doppeltippen: %1$s» or «%1$s – doppeltippen».
Microsoft's hints were full sentences («Doppeltippen, um das Audiobeacon
stummzuschalten»), so this appeared when we split the hints into fragments
(`de8a39bab`).

### DE-G1 — «nach Sackgasse» has no article (`agreed` defect, `unconfirmed` wording)

`confect_name_to` «%1$s nach %2$s» + «Sackgasse» gives «Fußweg nach
Sackgasse». This is the French FR-G1 problem. «nach» also suits place names
better than street names. Candidates: «%1$s zur %2$s» / «%1$s Richtung
%2$s» with «Sackgasse», or a dead-end string that carries its own article
(«einer Sackgasse»).

### DE-C1 — Siri phrases are German and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

«Endpunkt» for "Done" (Microsoft's original `first_launch_prompt_button`).
It's a mistranslation, now «Fertig». Don't restore it when comparing against
Microsoft's files.

## Open questions

1. VoiceOver: «Doppeltippen: Audiobeacon stummschalten» or «Audiobeacon
   stummschalten – doppeltippen»? (DE-B1)
2. «Fußweg nach Sackgasse»: better as «Fußweg zur Sackgasse» or «Fußweg
   Richtung Moor Road»? (DE-G1)
3. The four detail levels (Ausführlich / Ausgewogen / Leise / Stumm): clear?
   «Leise» may be heard as volume rather than fewer callouts.
4. Siri phrases «Soundscape Umgebung / Route / Beacon / stoppe Beacon…»: natural?
5. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline**, copied from `de-DE.lproj`.
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations ("Anonymous").
**2026-09-24 — corpus sweep** including a comparison against Microsoft's
file. Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_sleep` «Ruhemodus» → Microsoft's «Ruhemodus aktivieren». This closes Q4.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 11 were restored to Microsoft's wording and 6 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 11 uploaded and verified live.
