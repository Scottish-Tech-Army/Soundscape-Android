# Hungarian (hu) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | **Mixed**: «te» in the UI, «Ön» in the help pages. See HU-R1 |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Hungarian.** It has been AI-only since
2026-08-21. The VoiceOver template «Duplán koppintva: %1$s» with verbal-noun
hints is already the colon frame that C13 recommends. There is no
`hu.lproj`, so the Siri phrases stay in English. Questions:
`translations/review/hu.md` (Q1…Q5).

## Glossary

| English | Hungarian | Status | Note |
|---|---|---|---|
| Callout | bejelentés | `unconfirmed` | |
| Audio Beacon | hangjelző | `unconfirmed` | |
| Marker | jelölő | `unconfirmed` | |
| Waypoint | útvonalpont | `unconfirmed` | |
| Landmarks | nevezetességek | `unconfirmed` | "Sights" |
| Intersection | kereszteződés | `unconfirmed` | |
| Sleep / Snooze | Alvás ; Szundikálás | `unconfirmed` | |
| Detail levels | Részletes / Kiegyensúlyozott / Csendes / Néma | `unconfirmed` | Distinct |
| dead end | zsákutca | `unconfirmed` | |

## Rules

### HU-A1 — «a(z)» is read aloud (`agreed` defect, `unconfirmed` wording)

42 strings write the article as «a(z)» before a placeholder («%1$s a(z)
%2$s felé»), because the right form (a/az) depends on the substituted word.
A screen reader either reads the brackets or spells out «a z», and neither is an article. (How exactly depends on the TTS engine. Ask a user.) Fixes, per
string: restructure so no article is needed, or pick «a» or «az» where the
following word is known. It can also be done in code, by choosing «a»/«az»
from the first letter of the substituted text. Ask before sweeping.

### HU-R1 — «te» vs «Ön» (`unconfirmed`)

The UI says «Készen állsz!» (te), while ~14 strings, mostly `help_text_*`
plus one `siri_desc_*`, use «Ön». This is the same split Spanish had (ES-R1). Pick one.

## Rejected

Nothing yet.

## Open questions

1. How does «a(z)» sound when a screen reader reads it? How should it be
   removed? (HU-A1)
2. Register: «te» everywhere, or «Ön» everywhere? (HU-R1)
3. Beacon «hangjelző»: natural?
4. Callout «bejelentés»: natural?
5. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.
