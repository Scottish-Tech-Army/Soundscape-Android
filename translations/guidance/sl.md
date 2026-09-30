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

Questions: `docs/translation-questions/questions-sl.md` (Q1…Q7).

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
| Detailed / Simplified / Essential / Silent | Podrobno / Poenostavljeno / Osnovno / Brez zvoka | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Podrobno / Uravnoteženo / Tiho / Brez zvoka |
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
6. **The four detail levels** (Podrobno / Poenostavljeno / Osnovno / Brez zvoka), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
7. Anything else.

## Provenance

**2026-08-23 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slepe ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Byte `*_a11y` plurals: the «two»/«few» forms had no number; `%1$s` restored. `osm_bowls` → «Balinanje na travi» (boules keeps «Balinanje»). Uploaded live.

**2026-09-29 — Stale beacon FAQ fixed (C16).** `faq_how_to_use_beacon_answer` had been translated from an older English. Three passages were replaced to match the current source:
- the sailboat-tacking sentence, now "you may still need to make navigation choices along the way to work around obstacles";
- "turn the phone slowly", now "slowly turn in a circle";
- "the lighthouse metaphor … has natural implications", now "This design has a few natural results".

The extra *…* pair around the "tacks" word went with the sentence. The rest of the text was left unchanged. The new wording is `unconfirmed`. Found by a cross-language check after the bg/hr reviews. Uploaded and verified live.

**2026-09-29 — European batch review.** 16 hints were infinitives, imperatives or 2pl verbs instead of the verbal noun (accusative) that follows «za» («dodati», «Preklopi», «izvedeti o…», «odpreti meni» → «dodajanje», «preklop», «informacije o…», «odpiranje menija»); `places_nearby_selection_description` «izbira» → accusative «izbiro». Help texts named the stop button «Ustavi pot» (label «Zaustavi pot», 3 strings) and the mute pair «izklopi/vklopi zvok svetilnika» (labels «Utišaj/Odtišaj svetilnik», 2 strings). 21 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
