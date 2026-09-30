# Indonesian (id) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (Weblate code `id`, resource dir `values-in`, see [[compose-resources-indonesian-in-vs-id]]) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «Anda» (497, no «kamu»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Indonesian.** It has been AI-only since
2026-08-21. It is grammatically sound: «Ketuk dua kali untuk %1$s» composes
with the hints, and «%1$s ke %2$s» is fine under C10. The problems are two
terms with the wrong sense. Questions: `docs/translation-questions/questions-id.md` (Q1…Q7).

## Glossary

| English | Indonesian | Status | Note |
|---|---|---|---|
| Callout | pemberitahuan | `unconfirmed` | 70. Also the everyday word for phone notifications. See ID-T1 |
| Audio Beacon | Suar Audio | `unconfirmed` | **«suar» is a flare or signal light**, a visual thing. See ID-T2 |
| Marker | Penanda | `unconfirmed` | |
| Waypoint | Titik Rute | `unconfirmed` | |
| Landmarks | Landmark | `unconfirmed` | English left as is. «Tengara» exists but is rarely used |
| Intersection | persimpangan | `unconfirmed` | |
| Sleep / Snooze | Tidur / Menunda | `unconfirmed` | «Sedang Menunda» = "postponing". See Q4 |
| Detailed / Simplified / Essential / Silent | Rinci / Sederhana / Esensial / Senyap | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Rinci / Seimbang / Ringkas / Senyap |
| dead end | jalan buntu | `unconfirmed` | |
| Traveling / Heading | Melaju / Berjalan | `agreed` | Correct vehicle/walking split (BG-T4) |

## Rules

### ID-T1 — Callout «pemberitahuan» vs system notifications (`unconfirmed`)

The same collision as fr/pl/bg. Alternatives: «informasi suara», «keterangan».
Ask before sweeping 70 strings.

### ID-T2 — Beacon «Suar» has a visual meaning (`unconfirmed`)

C12: a blind user is told to follow a "flare". «Suar Audio» may still be
understood, since «mercusuar» (lighthouse) is also a guiding metaphor. It
needs a native ear. There are 140 occurrences, so ask before touching it.

### ID-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «pemberitahuan»: confused with phone notifications? (ID-T1)
2. Beacon «Suar Audio»: does it sound right for a *sound* you follow? (ID-T2)
3. Waypoint «Titik Rute»: natural, or is «titik jalan» better? What does Google Maps say? (The 3 «titik jalan» were unified to «titik rute» on 2026-09-29.)
4. Snooze «Menunda»: clear?
5. Landmarks: keep «Landmark» or use «Tengara»?
6. Is «Anda» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 28 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-29 — full review.** 48 fixes. Help, FAQ and a few UI strings still used English "beacon", "marker" and "waypoint" (38 strings) → «suar», «penanda», «titik rute» like the rest of the UI; button names now match the labels («Nonaktifkan/Aktifkan Suara Suar», «Simpan sebagai Penanda», «Penanda Terdekat»). If ID-T2 replaces «suar», one sweep covers it all. Waypoint «titik jalan» (3) → «titik rute» (ID Q3 still asks which is natural). Home button «Sekitar Saya» → «Di Sekitar Saya», matching its help page, six help texts and Siri; `tour_around_me` follows. Three hints lower-cased / made me- verbs. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
