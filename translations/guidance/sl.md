# Slovenian (sl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «vi» (vikanje, plural agreement «Pripravljeni ste!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Slovenian.** It has been AI-only since
2026-08-23. Things that already work:
- The VoiceOver template «Dvakrat tapnite za %1$s» with verbal-noun hints
  («utišanje zvočnega svetilnika») composes correctly.
- Traveling/Heading are «Vožnja»/«Hoja», the correct vehicle/walking split.
- Formal Slovenian takes plural agreement, so «Pripravljeni ste!» is correct
  and gender-neutral.

Questions: `translations/review/sl.md` (Q1…Q6).

## Glossary

| English | Slovenian | Status | Note |
|---|---|---|---|
| Callout | zvočno obvestilo | `unconfirmed` | **«obvestilo» is Android's word for a system notification.** See SL-T1 |
| Audio Beacon | zvočni svetilnik | `unconfirmed` | «svetilnik» = lighthouse, a visual image (C12). See Q2 |
| Marker | oznaka | `unconfirmed` | |
| Waypoint | točka poti | `unconfirmed` | |
| Landmarks | znamenitosti | `unconfirmed` | |
| Intersection | križišče | `unconfirmed` | |
| Sleep / Snooze | Spanje / V spanju ; V dremežu | `unconfirmed` | |
| Detail levels | Podrobno / Uravnoteženo / Tiho / Brez zvoka | `unconfirmed` | Distinct |
| dead end | slepa ulica | `unconfirmed` word; case fixed 2026-09-24. See SL-G1 |

## Rules

### SL-G1 — «do slepa ulica» must be genitive «do slepe ulice» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «slepe ulice», uploaded to Weblate and verified live.

Rule C9, and Slovenian is on its list. One string.

### SL-T1 — Callout «zvočno obvestilo» collides with notifications (`unconfirmed`)

The same issue as fr/pl/bg/id. Croatian and Serbian use «najava»
(announcement). The Slovenian equivalent would be «napoved». Ask before
sweeping.

### SL-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout: «zvočno obvestilo» or «napoved»? (SL-T1)
2. Beacon «zvočni svetilnik»: odd for a sound?
3. Waypoint «točka poti»: natural?
4. Snooze «V dremežu»: clear?
5. Is vikanje right?
6. Anything else.

## Provenance

**2026-08-23 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slepe ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
