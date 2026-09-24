# Arabic (ar) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | 2nd person masculine («أنت جاهز!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Arabic.** It has been AI-only since
2026-08-22. The verbal-noun hints («كتم صوت المنارة الصوتية») compose with
«انقر نقرًا مزدوجًا لـ %1$s». The authored Siri phrases (`ar.lproj`) match the
help text. Questions: `translations/review/ar.md` (Q1…Q6).

## Glossary

| English | Arabic | Status | Note |
|---|---|---|---|
| Callout | النداء الصوتي | `unconfirmed` | 71. «نداء» is also "a call" or "an appeal". See Q1 |
| Audio Beacon | المنارة الصوتية | `unconfirmed` | «منارة» = lighthouse, a visual image (C12) |
| Marker | علامة | `unconfirmed` | |
| Waypoint | نقطة المسار | `unconfirmed` | |
| Landmarks | المعالم | `unconfirmed` | |
| Intersection | تقاطع | `unconfirmed` | |
| Sleep / Snooze | نوم ; غافٍ | `unconfirmed` | |
| Detail levels | مفصّل / متوازن / هادئ / صامت | `unconfirmed` | |
| dead end | طريق مسدود | `unconfirmed` | |

## Rules

### AR-R1 — Masculine by default (`unconfirmed`)

«أنت جاهز!» and other second-person forms are masculine. Arabic apps usually
accept this, but it's worth asking, and C15 suggests an impersonal
«كل شيء جاهز!».

### AR-C1 — Siri phrases are Arabic and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «النداء الصوتي»: natural? Or «التنبيه الصوتي» / «الإعلان الصوتي»?
2. Beacon «المنارة الصوتية»: odd for a sound?
3. «كل شيء جاهز!» instead of «أنت جاهز!»? (AR-R1)
4. Snooze «غافٍ»: clear?
5. Siri phrases «Soundscape المحيط / المسار / المنارة / أوقف المنارة…»: natural?
6. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.
