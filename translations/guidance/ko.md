# Korean (ko) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal 합니다/하세요체, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Korean.** It has been AI-only since
2026-08-22, and it is the cleanest corpus in this sweep:
- Callout is consistently «안내» (128) with no loanword split.
- Waypoint is the native map term «경유지».
- `confect_name_to` «%2$s(으)로 이어지는 %1$s» is the model C10 answer.
- Particle markers such as «을(를)», «(으)로» and «(이)라» are resolved in code
  (C18). Keep writing them. `street_description_between` hard-coded
  «%2$s와», which is wrong after a consonant; it was changed to «%2$s과(와)» on
  Weblate on 2026-09-25.
- The iOS template was «%1$s하려면 두 번 탭하세요». That was wrong: it needs a 하다-noun, and 39 of 51 hints end in -기, giving «음소거하기하려면». Changed to «두 번 탭하여 %1$s» on 2026-09-29 (C13, KO-B1).
- The Siri phrases (`ko.lproj`) match the help text.

The questions are confirmation, not repair. Questions:
`docs/translation-questions/questions-ko.md` (Q1…Q8).

## Glossary

| English | Korean | Status | Note |
|---|---|---|---|
| Callout | 안내 | `unconfirmed` | Consistent |
| Audio Beacon | 오디오 비콘 | `unconfirmed` | |
| Marker | 마커 | `unconfirmed` | |
| Waypoint | 경유지 | `unconfirmed` | |
| Landmarks | 랜드마크 | `unconfirmed` | |
| Intersection | 교차로 | `unconfirmed` | |
| Sleep / Snooze | 잠자기 / 스누즈 | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | 상세 / 간소화 / 핵심 / 무음 | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was 상세 / 균형 / 간략 / 무음 |
| dead end | 막다른 길 | `unconfirmed` | |

## Rules

### KO-C1 — Siri phrases are Korean and live outside Weblate (`agreed`)

The same coupling as FR-C1.

### KO-S1 — "On X" said the user was driving (`fixed`, 2026-09-25)

The four `directions_on_road*` strings («On %1$s», also heard on foot) said
«%1$s에서 주행 중» ("driving on X"). Now «%1$s에 있음», matching the terse
«…움직이지 않음» style. The `*_traveling_*` strings keep «주행 중»: they are only
used in a vehicle.

### KO-B1 — Hints are -기 forms; the iOS template fits them (`agreed` defect, frame `unconfirmed`)

`talkback_double_tap_template` is now «두 번 탭하여 %1$s», which works with -기, -하기 and bare-noun hints. The three hints that ended in «-합니다» are now -기 forms. **New hints must be -기 forms.** What Android TalkBack says around them is unknown (Q6).

## Rejected

Nothing yet.

## Open questions

1. Callout «안내»: right for short spoken descriptions?
2. Beacon «오디오 비콘»: understood?
3. Snooze «스누즈»: understood?
4. Siri phrases «Soundscape 주변 / 경로 / 비콘…»: natural to say?
5. Register: is 합니다체 right?
6. Screen reader hints: is «두 번 탭하여 오디오 비콘 음소거하기» natural, and what does Android TalkBack say around the hint? (KO-B1)
7. **The four detail levels** (상세 / 간소화 / 핵심 / 무음), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
8. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-29 — full review.** 10 fixes. iOS template «%1$s하려면 두 번 탭하세요» → «두 번 탭하여 %1$s» and three «-합니다» hints → -기 (KO-B1). Help named the mute buttons «비콘 음소거…» (labels «오디오 비콘 음소거…», 3 strings) and the Location Details screen «위치 세부정보» (2). `faq_difference_from_map_apps_answer` «주변적 설명» → «설명». Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
