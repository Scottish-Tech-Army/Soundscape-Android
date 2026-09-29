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
`docs/translation-questions/questions-hr.md` (Q1…Q8).

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

### HR-B1 — Hints are conditional participles (`agreed`, fixed 2026-09-29)

«Dvaput dodirnite da biste %1$s» needs «utišali», «uredili», «dodali», never an imperative («Prikaži») or the informal present («urediš»). Check any new hint.

### HR-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

### HR-G3 — Map names after «cestom» / «Na» (`agreed` defect, fix `unconfirmed`)

24 `directions_along_*` strings say «…cestom %1$s» ("by road X"), which doubles
the street word for names like «Savska cesta», and every «Na %1$s» template
leaves the name undeclined («Na Ilica»). Croatian names carry «ulica», «cesta» or
«trg» at either end and decline with their adjective, so a resolver can't fix
it. Candidate fix is C17's label form; asked as questionnaire Q7.

## Rejected

Nothing yet.

## Open questions

1. Callout «najava»: natural?
2. Beacon «zvučni svjetionik»: odd for a sound?
3. FAQ headings: rephrase «Kada bih trebao/la…» as «Kada koristiti…»? (HR-G2)
4. Snooze «Odgođeno»: clear?
5. Landmarks «znamenitosti»: too touristy?
6. Is «Vi» right?
7. Map names after «cestom» / «Na» (HR-G3): «cestom Savska cesta» doubles the
   street word, and «Na Ilica» is undeclined. Would a label form («…, ulica:
   Ilica») sound better? Not fixable in code: the street word sits at either end
   of the name and the adjective declines with it. See C17.
8. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slijepe ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).

**2026-09-25 — truncation repaired (C16).** `faq_battery_impact_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Byte `*_a11y` plurals: the «few» form had no number; `%1$s` restored. `osm_helipad` → «Sletište za helikoptere» (it had been «Heliodrom», same as heliport). Uploaded live.

**2026-09-29 — Full review of all 1586 units, 23 fixes uploaded.** There are no human Croatian edits in Weblate, and HR-G2 (the FAQ slashes) was left for Q3. Fixed:
- **`faq_how_to_use_beacon_answer`:** translated from an older English with a sailboat-tacking sentence (C16). Now matches the current English.
- **8 hints (HR-B1).**
- **Beacon «far» → «svjetionik»** (4, including the wrong button name in `tour_start_beacon`).
- **Waypoint «točka rute» → «putna točka»** (4).
- **Help text now uses the real button or screen names:** *Oko mene*, *Pojedinosti o lokaciji*, *Trenutačna lokacija*, *Uključi zvuk svjetionika*.
- **Term and register:** «zanimljivoj točki», and «prolaziš» → «prolazite».

Uploaded with `--skip-validate`; all 23 re-fetched and matched exactly.
