# Finnish (fi) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional fi-FI localisation (C14). 212 of 359 shared keys still verbatim, 24 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «sinä» (2sg), `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

A Microsoft baseline in good condition. The VoiceOver template is **fine**.
An earlier note (FR-B1 in `fr.md`) worried that «Kaksoisnapauta %1$s» lacked
a connective, but the hints are translative infinitives («hiljentääksesi
äänimajakan», "in order to mute the beacon"), which carry the "to" meaning
themselves. The Siri phrases (`fi.lproj`) match the help text. Questions:
`translations/review/fi.md` (Q1…Q5).

## Glossary

| English | Finnish | Status | Note |
|---|---|---|---|
| Callout | ilmoitus | `confirmed` | Microsoft. Also Android's word for notifications |
| Audio Beacon | äänimajakka | `confirmed` | Microsoft |
| Marker | merkitsin | `agreed` (Dave, 2026-09-25) | Ricky Tigg's term in the Soundscape Community (2025), now used everywhere. It replaced Microsoft's «merkintä» in 83 Weblate strings and 5 `Localizable.xcstrings` Siri strings. See FI-T1 |
| Waypoint | reittipiste | `confirmed` | Microsoft |
| Intersection | risteys | `confirmed` | Microsoft |
| Sleep / Snooze | lepotila ; odotustila | `confirmed` | Microsoft |
| Detail levels | Yksityiskohtainen / Tasapainoinen / Hiljainen / Äänetön | `unconfirmed` | AI. Distinct |
| dead end | umpikuja | `unconfirmed` | AI. See FI-G1 |

## Rules

### FI-G1 — «kohteeseen umpikuja» (`unconfirmed`)

`confect_name_to` «%1$s kohteeseen %2$s» ("… to the destination X") is a
clever dodge. «kohteeseen» carries the illative, so the substituted name can
stay nominative, which suits OSM street names (compare IS-G2). But for the
dead-end case it gives «Polku kohteeseen umpikuja» where a speaker would say
«Polku umpikujaan». Ask whether it's acceptable, or whether dead end should
get its own phrasing.

### FI-C1 — Siri phrases are Finnish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

### FI-T1 — Soundscape Community is the Finnish reference for Ricky Tigg's edits (`agreed`, Dave 2026-09-25)

Ricky Tigg edited 108 Finnish strings in the Soundscape Community app
(2025-01 → 2025-02, `sc/fi-FI.lproj`). Only 7 of them apply to our app with
the same English. All 7 were adopted, including his «käännösohjeita» in
`help_text_destination_beacons_when`. That overturns this file's earlier
"ambiguous" objection to the word, because a native speaker chose it.

He also began switching Marker from «merkintä» to «merkitsin» but reached
only 22 strings. The Community file uses both (32/38). **Dave's decision:
apply «merkitsin» everywhere.** The case forms follow Ricky's own usage:
merkinnät→merkitsimet, merkintöjä→merkitsimiä, merkinnän→merkitsimen,
merkinnäksi→merkitsimeksi, merkinnöistäsi→merkitsimistäsi, and so on.
**«merkinnyt» is the verb "has marked", not the noun (C3), and stays.**
The Siri strings in `iosApp/iosApp/Localizable.xcstrings` ("Marker",
"Markers", "Nearby Markers" and two descriptions) switched in the same step,
so the help text still names choices Siri recognises (FI-C1).

## Rejected

Nothing yet.

## Open questions

1. «Polku kohteeseen umpikuja» or «Polku umpikujaan»? (FI-G1)
2. Callout «ilmoitus»: confused with phone notifications?
3. The four detail levels: clear?
4. Siri phrases «Soundscape ympäristö / reitti / majakka / pysäytä majakka…»: natural?
5. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`fi-FI.lproj`). **2025 → 2026 —
AI passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` «Odotustilassa» → Microsoft's «Odotustila».

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 15 were restored to Microsoft's wording, 90 fixed or term-changed and 4 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 105 uploaded and verified live.
