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
`docs/translation-questions/questions-hu.md` (Q1…Q5).

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

### HU-A1 — «a(z)» is resolved in code (`fixed`, 2026-09-25)

47 strings write the article as «a(z)», because a/az depends on the name
that is filled in, and screen readers read the brackets out. Since
2026-09-25 `resolveGrammarMarkers()` (C18) picks «a» or «az» from the
substituted text, so translators should **keep writing «a(z)»**. Don't
restructure strings to avoid it, and don't pick one form. Still worth
confirming with a native speaker: the letter-name rule (az M7, az SZTE,
a BKV) and the number rule (az 1, az 5, az 1000, a 12, a 100).

### HU-G1 — Road templates said the street type twice (`fixed` in code, 2026-09-25)

35 templates wrote «a(z) %1$s úton» (one «vonalon»), but Hungarian street names
already end in their type, so callouts said «az Andrássy út úton», «a Váci utca
úton». The templates now write «a(z) %1$s{úton}» and `resolveGrammarMarkers()`
(C18) puts the name's own street word into the case: «az Andrássy úton», «a Váci
utcán», «a Deák téren», «a Hősök terén», «a Dunakeszi alagútban». The word list
was measured against the Budapest extract and covers 95% of its 16,094 street
names; the rest (route numbers, Slovak names) keep « úton». **New road templates
must use `{úton}`**, never a literal «úton» after a name.

### HU-R1 — «te» vs «Ön» (`unconfirmed`)

The UI says «Készen állsz!» (te), while ~14 strings, mostly `help_text_*`
plus one `siri_desc_*`, use «Ön». This is the same split Spanish had (ES-R1). Pick one.

## Rejected

Nothing yet.

## Open questions

1. Does the automatic a/az choice sound right, especially before road
   numbers, abbreviations and numbers? (HU-A1)
2. Register: «te» everywhere, or «Ön» everywhere? (HU-R1)
3. Beacon «hangjelző»: natural? In everyday use it means a buzzer, beeper or car horn, so it may not suggest a sound that shows a direction.
4. Callout «bejelentés»: natural? It can sound official; «bemondás» (a public-transport announcement) was offered as an alternative. «értesítés» was ruled out because it clashes with phone notifications.
5. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.
