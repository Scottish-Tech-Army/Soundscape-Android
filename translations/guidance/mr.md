# Marathi (mr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «तुम्ही» («तुम्ही तयार आहात!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Marathi.** It has been AI-only since
2026-08-22. One of the cleanest AI corpora so far:
- Callout is consistently «ध्वनी सूचना» (77, no loanword split).
- `confect_name_to` «%2$s कडे जाणारा %1$s» is the model C10 answer.
- The VoiceOver template «%1$sसाठी डबल टॅप करा» composes correctly.

Questions: `docs/translation-questions/questions-mr.md` (Q1…Q6).

## Glossary

| English | Marathi | Status | Note |
|---|---|---|---|
| Callout | ध्वनी सूचना | `unconfirmed` | Consistent. «सूचना» alone also means a system notification. See Q1 |
| Audio Beacon | ऑडिओ बीकन | `unconfirmed` | Loanword (C12) |
| Marker | मार्कर | `unconfirmed` | |
| Waypoint | मार्ग बिंदू | `unconfirmed` | |
| Landmarks | खुणा | `unconfirmed` | |
| Intersection | चौक | `unconfirmed` | |
| Sleep / Snooze | झोप मोड / स्नूझ मोड | `unconfirmed` | `sleep_sleep` is «झोप मोड» ("sleep mode") on a *button*. See Q3 |
| Detail levels | तपशीलवार / संतुलित / संक्षिप्त / निःशब्द | `unconfirmed` | Distinct |
| dead end | बंद रस्ता | `unconfirmed` | **Can also mean "road closed"**. See MR-T1 |

## Rules

### MR-T1 — «बंद रस्ता» is ambiguous (`unconfirmed`)

For a blind pedestrian, "this road is closed" and "this road leads nowhere"
are different warnings. If «बंद रस्ता» reads first as *closed*, a dead-end
word («बोळ»?, «पुढे रस्ता नाही») is needed. Ask.

### MR-C1 — Siri phrases stay in English (`agreed`)

There is no `mr.lproj`. See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «ध्वनी सूचना»: clear, or confused with phone notifications?
2. Beacon «ऑडिओ बीकन»: understood?
3. Sleep button «झोप मोड»: natural as something you press?
4. Dead end: does «बंद रस्ता» sound like "road closed"? (MR-T1)
5. Is «तुम्ही» right?
6. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `faq_mobile_data_use_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
