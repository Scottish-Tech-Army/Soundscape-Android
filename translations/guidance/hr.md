# Croatian (hr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «Vi» («Spremni ste!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Croatian.** It has been AI-only since
2026-08-21. The VoiceOver template «Dvaput dodirnite da biste %1$s» with
conditional participle hints («utišali») composes correctly. There is one
`agreed` case defect (C9) and one TTS-audible defect (HR-G2). Questions:
`translations/review/hr.md` (Q1…Q7).

## Glossary

| English | Croatian | Status | Note |
|---|---|---|---|
| Callout | najava | `unconfirmed` | Consistent |
| Audio Beacon | zvučni svjetionik | `unconfirmed` | «svjetionik» = lighthouse, a visual image (C12). See Q2 |
| Marker | oznaka | `unconfirmed` | |
| Waypoint | putna točka | `unconfirmed` | |
| Landmarks | znamenitosti | `unconfirmed` | "Sights", narrower than landmark |
| Intersection | raskrižje | `unconfirmed` | |
| Sleep / Snooze | Mirovanje / U mirovanju ; Odgođeno | `unconfirmed` | «Odgođeno» = "postponed". See Q4 |
| Detail levels | Detaljno / Uravnoteženo / Tiho / Bez zvuka | `unconfirmed` | Distinct |
| dead end | slijepa ulica | `unconfirmed` word; case fixed 2026-09-24. See HR-G1 |

## Rules

### HR-G1 — «do slijepa ulica» must be genitive «do slijepe ulice» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «slijepe ulice», uploaded to Weblate and verified live.

Rule C9, and Croatian is on its list. One string.

### HR-G2 — Gender slashes in first-person FAQ questions (`agreed` defect, `unconfirmed` wording)

Some FAQ questions are phrased in the *user's* voice, with slashed
participles: `faq_when_to_use_soundscape_question` «Kada bih trebao/la
koristiti Soundscape?», `faq_sleep_mode_battery_question`
«…kako bih smanjio/la…», plus four more (`faq_section_what_is_soundscape`,
`faq_when_to_use_soundscape_answer`, `faq_supported_headsets_question`,
`faq_snooze_mode_battery_question`). A screen reader reads the slash aloud.
This is the same problem as Icelandic IS-G3.

Rephrase impersonally («Kada koristiti Soundscape?», «Kako smanjiti…»),
which is also more natural for FAQ headings. Serbian's equivalents avoid the
slash by using the masculine only («смањио»), which is a different trade-off.

### HR-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «najava»: natural?
2. Beacon «zvučni svjetionik»: odd for a sound?
3. FAQ headings: rephrase «Kada bih trebao/la…» as «Kada koristiti…»? (HR-G2)
4. Snooze «Odgođeno»: clear?
5. Landmarks «znamenitosti»: too touristy?
6. Is «Vi» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slijepe ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
