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
3. Waypoint «Điểm dừng»: natural? (The waypoint uses of «điểm mốc», which is also the Landmarks word, were unified to «điểm dừng» on 2026-09-29.)
4. Callout «thông báo»: confused with phone notifications?
5. Snooze «Tạm nghỉ»: clear?
6. Is «bạn» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-29 — full review.** 15 fixes. Waypoint «điểm mốc» (the Landmarks word) and Route «tuyến đường» in 5 strings → «điểm dừng», «lộ trình». Help names now match the labels («Chi tiết vị trí», «Quản lý thông báo thoại», «Bật tiếng đèn hiệu», «Phương tiện công cộng»). `faq_tip_beacon_quiet` «sẽ im lặng» → «sẽ nhỏ đi». Three hints lower-cased. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live. **Held:** VI-G1.
