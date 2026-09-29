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
help text. Questions: `docs/translation-questions/questions-ar.md` (Q1…Q6).

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

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 28 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_terminal` → «مبنى الركاب» (it had been «محطة», same as station). Uploaded live.

**2026-09-29 — Stale beacon FAQ fixed (C16).** `faq_how_to_use_beacon_answer` had been translated from an older English. Three passages were replaced to match the current source:
- the sailboat-tacking sentence, now "you may still need to make navigation choices along the way to work around obstacles";
- "turn the phone slowly", now "slowly turn in a circle";
- "the lighthouse metaphor … has natural implications", now "This design has a few natural results".

The extra *…* pair around the "tacks" word went with the sentence. The rest of the text was left unchanged. The new wording is `unconfirmed`. Found by a cross-language check after the bg/hr reviews. Uploaded and verified live.
