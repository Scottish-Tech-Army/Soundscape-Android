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
terms with the wrong sense. Questions: `translations/review/id.md` (Q1…Q7).

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
| Detail levels | Rinci / Seimbang / Ringkas / Senyap | `unconfirmed` | Distinct |
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
3. Waypoint «Titik Rute»: natural? What does Google Maps say?
4. Snooze «Menunda»: clear?
5. Landmarks: keep «Landmark» or use «Tengara»?
6. Is «Anda» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
