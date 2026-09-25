# Vietnamese (vi) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | «bạn» (433), the standard for apps, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Vietnamese.** It has been AI-only since
2026-08-21. There is one `agreed` meaning defect (VI-G1) and one term with
the wrong sense (VI-T1). Questions: `docs/translation-questions/questions-vi.md` (Q1…Q7).

## Glossary

| English | Vietnamese | Status | Note |
|---|---|---|---|
| Callout | thông báo (âm thanh) | `unconfirmed` | 102. Also means system notification |
| Audio Beacon | Đèn hiệu âm thanh | `unconfirmed` | **«đèn» = lamp**. See VI-T1 |
| Marker | Điểm đánh dấu | `unconfirmed` | |
| Waypoint | Điểm dừng | `unconfirmed` | "Stop". Matches what uk chose via Google Maps (C1) |
| Landmarks | Điểm mốc | `unconfirmed` | |
| Intersection | giao lộ | `unconfirmed` | |
| Sleep / Snooze | Ngủ / Tạm nghỉ | `unconfirmed` | |
| Detail levels | Chi tiết / Cân bằng / Yên tĩnh / Im lặng | `unconfirmed` | Distinct |
| dead end | đường cụt | `unconfirmed` | |
| Traveling / Heading | Di chuyển / Đi bộ | `agreed` | Correct vehicle/walking split |

## Rules

### VI-G1 — "goes left" became "turn left" (`agreed` defect, `unconfirmed` wording)

`directions_name_goes_left` «%1$s, rẽ trái» is an instruction. See
`_common.md` C11. Candidates: «%1$s, đi về bên trái», «%1$s ở bên trái».
It's two strings: `_goes_left` «rẽ trái» and `_goes_right` «rẽ phải». `_continues_ahead`
«tiếp tục phía trước» is already descriptive.

### VI-T1 — Beacon «Đèn hiệu» is a light (`unconfirmed`)

C12. 116 occurrences, so ask first. Candidates: «tín hiệu âm thanh»,
«âm báo hướng».

### VI-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Intersection descriptions: «%1$s, rẽ trái» sounds like "turn left". Is
   «%1$s, đi về bên trái» better? (VI-G1)
2. Beacon «Đèn hiệu âm thanh»: odd for a sound? (VI-T1)
3. Waypoint «Điểm dừng»: right?
4. Callout «thông báo»: confused with phone notifications?
5. Snooze «Tạm nghỉ»: clear?
6. Is «bạn» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
