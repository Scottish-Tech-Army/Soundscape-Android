# Tamil (ta) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «நீங்கள்» («நீங்கள் தயார்!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Tamil.** It has been AI-only since
2026-08-22. Callout is consistently the native «அறிவிப்பு» (65). The
VoiceOver hints are «-க்க» infinitives that compose correctly with «%1$s
இரட்டை தட்டவும்». One `agreed` grammar defect: TA-G1. Questions:
`docs/translation-questions/questions-ta.md` (Q1…Q7).

## Glossary

| English | Tamil | Status | Note |
|---|---|---|---|
| Callout | அறிவிப்பு | `unconfirmed` | Consistent. Also the everyday word for a phone notification. See Q1 |
| Audio Beacon | ஒலி பீக்கன் | `unconfirmed` | Half native, half loanword |
| Marker | குறிப்பான் | `unconfirmed` | |
| Waypoint | வழிப்புள்ளி | `unconfirmed` | |
| Landmarks | அடையாளக் குறிகள் | `unconfirmed` | |
| Intersection | சந்திப்பு | `unconfirmed` | |
| Sleep / Snooze | உறக்கம் / ஸ்னூஸ் | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | விரிவு / எளிமை / அத்தியாவசியம் / மௌனம் | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was விரிவு / சமநிலை / அமைதி / மௌனம் |
| dead end | முட்டுச்சந்து | `unconfirmed` | |

## Rules

### TA-G1 — `confect_name_to` says "from … to" (`agreed` defect, `unconfirmed` wording)

«%1$s முதல் %2$s வரை» means "from the path up to Moor Road". See C10.
Candidate: «%2$s நோக்கிச் செல்லும் %1$s». The same applies to `_via`.

### TA-T1 — Quiet vs Silent may not be distinct enough (`unconfirmed`)

The user picks these by ear. «அமைதி» and «மௌனம்» are both "quiet/silence"
words. English "Quiet" here means *fewer* callouts, not no sound. Something
like «சுருக்கம்» ("brief", which is what mr, te and ur chose) may separate
them better.

### TA-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

### TA-T2 — Marker term (`agreed`, fixed 2026-09-29)

Marker is «குறிப்பான்» everywhere (nouns); «மார்க்கர்» and the noun «குறியிடம்» are retired. The verb «குறியிடு» ("to mark") stays. Check any new hint.

## Rejected

Nothing yet.

## Open questions

1. Callout «அறிவிப்பு»: confused with phone notifications?
2. Beacon «ஒலி பீக்கன்»: understood?
3. Waypoint «வழிப்புள்ளி»: natural?
4. The four detail levels (விரிவு / எளிமை / அத்தியாவசியம் / மௌனம்), renamed 2026-09-30 (C22) partly because «அமைதி» and «மௌனம்» were too close: clear by ear now? (TA-T1)
5. «%2$s நோக்கிச் செல்லும் %1$s» for "path to Moor Road"? (TA-G1)
6. Is «நீங்கள்» right?
7. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_optician` → «கண் கண்ணாடிக் கடை» (it had been the same as glazier). Full stop added to `settings_reset_button_hint`. Uploaded live.

**2026-09-29 — Full review of all 1586 units.** 40 fixes uploaded. Marker had three words: «குறிப்பான்» (glossary, the Markers screen), «மார்க்கர்» (27, including the Nearby Markers button) and «குறியிடம்» (help text). All noun forms were swept to «குறிப்பான்» (TA-T2). The verb forms «குறியிடலாம்» and «குறியிடப்பட்ட» were kept. 4 hints had drifted to imperatives and now use the infinitive. Help text now uses «பீக்கனை இயக்கு», «வெளியேறும்போது எழுப்பு» and «எனைச் சுற்றி». Uploaded with `--skip-validate` and re-fetched: all matched exactly.
