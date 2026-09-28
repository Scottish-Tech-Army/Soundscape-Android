# Hausa (ha) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Plural/respectful «Kun…» («Kun shirya!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Hausa.** It has been AI-only since
2026-08-21. This sweep can check structure and consistency but not
idiomaticity, because Hausa is the language here where machine translation
is least reliable. **Treat every term as a guess until a speaker has seen
it.** The template «Danna sau biyu don %1$s» composes with the hints. There
is no `ha.lproj`, so the Siri phrases stay in English. Questions:
`docs/translation-questions/questions-ha.md` (Q1…Q6).

## Glossary

| English | Hausa | Status | Note |
|---|---|---|---|
| Callout | sanarwa | `unconfirmed` | |
| Audio Beacon | Siginar Sauti | `unconfirmed` | |
| Marker | alamomi | `unconfirmed` | |
| Waypoint | matsayi | `unconfirmed` | Means "position/status", so it may not convey a route stop. See Q2 |
| Landmarks | shahararrun wurare | `unconfirmed` | "Famous places" |
| Intersection | mahaɗar hanya | `unconfirmed` | |
| Sleep / Snooze | Barci ; Hutawa | `unconfirmed` | |
| dead end | titin da babu fita | `unconfirmed` | |

## Rules

### HA-S1 — «ta juya hagu» (`unconfirmed`)

`directions_name_goes_left` «%1$s, ta juya hagu» ("…, it turned/turns left").
Check that it reads as a description, not an instruction (C11).

## Rejected

Nothing yet.

## Open questions

1. Is the Hausa generally understandable, or does it read as machine translation?
2. Waypoint «matsayi»: does it mean a stop on a route?
3. Callout «sanarwa» and Beacon «Siginar Sauti»: natural?
4. «Titin X, ta juya hagu»: a description, or an instruction? (HA-S1)
5. Register: respectful plural «ku» or singular «ka»? The app mixes them («Kun shirya!» vs «Yanzu kana iya jin…»).
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 25 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_parking_space` → «Gurbin Ajiye Mota», `osm_clinic` → «Karamin Asibiti» (each had been a copy of a sibling). Uploaded live.
