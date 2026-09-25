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
- «%1$s하려면 두 번 탭하세요» composes correctly (C9-safe, verb-final).
- The Siri phrases (`ko.lproj`) match the help text.

The questions are confirmation, not repair. Questions:
`docs/translation-questions/questions-ko.md` (Q1…Q6).

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
| Detail levels | 상세 / 균형 / 간략 / 무음 | `unconfirmed` | Distinct |
| dead end | 막다른 길 | `unconfirmed` | |

## Rules

### KO-C1 — Siri phrases are Korean and live outside Weblate (`agreed`)

The same coupling as FR-C1.

### KO-S1 — "On X" said the user was driving (`fixed`, 2026-09-25)

The four `directions_on_road*` strings («On %1$s», also heard on foot) said
«%1$s에서 주행 중» ("driving on X"). Now «%1$s에 있음», matching the terse
«…움직이지 않음» style. The `*_traveling_*` strings keep «주행 중»: they are only
used in a vehicle.

## Rejected

Nothing yet.

## Open questions

1. Callout «안내»: right for short spoken descriptions?
2. Beacon «오디오 비콘»: understood?
3. Snooze «스누즈»: understood?
4. Siri phrases «Soundscape 주변 / 경로 / 비콘…»: natural to say?
5. Register: is 합니다체 right?
6. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
