# Slovak (sk) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «vy» (vykanie), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Slovak.** It has been AI-only since
2026-08-21. There are two `agreed` defects: the C9 dead-end case, and an
iOS-only VoiceOver template that doesn't fit the hints (SK-B1, the case
that produced rule C13). Questions: `docs/translation-questions/questions-sk.md` (Q1…Q6).

## Glossary

| English | Slovak | Status | Note |
|---|---|---|---|
| Callout | hlásenie | `unconfirmed` | Consistent |
| Audio Beacon | zvukový maják | `unconfirmed` | |
| Marker | značka | `unconfirmed` | |
| Waypoint | bod trasy | `unconfirmed` | |
| Landmarks | orientačné body | `unconfirmed` | |
| Intersection | križovatka | `unconfirmed` | |
| Sleep / Snooze | Spánok / Spí ; Drieme | `unconfirmed` | |
| Detail levels | Podrobný / Vyvážený / Tichý / Bez zvuku | `unconfirmed` | Distinct |
| dead end | slepá ulica | `unconfirmed` word; case fixed 2026-09-24. See SK-G1 |

## Rules

### SK-G1 — «do slepá ulica» must be genitive «do slepej ulice» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «slepej ulice», uploaded to Weblate and verified live.

Rule C9, and Slovak is on its list. One string.

### SK-B1 — VoiceOver template needs a frame for infinitives (`agreed` defect, `unconfirmed` wording)

The hints are infinitives («stlmiť zvukový maják», «vypočuť…»), which suits
Android, where TalkBack supplies the frame. The iOS template «Dvojitým
ťuknutím %1$s» ("by double-tapping …") needs a finite verb, so VoiceOver
says «Dvojitým ťuknutím stlmiť zvukový maják». Fix the **template only**
(C13): «Ak chcete %1$s, dvakrát ťuknite» or «Dvojitým ťuknutím môžete %1$s».
One string, iOS-only.

### SK-R1 — «Ste pripravení!» is plural (`unconfirmed`)

The same question as CS-R1.

### SK-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. VoiceOver: «Ak chcete stlmiť zvukový maják, dvakrát ťuknite» or
   «Dvojitým ťuknutím môžete stlmiť zvukový maják»? (SK-B1)
2. Callout «hlásenie»: natural?
3. «Ste pripravení!»: OK, or «Všetko je pripravené!»? (SK-R1)
4. Snooze «Drieme»: clear?
5. Is vykanie right?
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slepej ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
