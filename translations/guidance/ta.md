# Tamil (ta) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1691 units (2026-11-19) |
| Last native-speaker input | **none yet** |
| Register | Formal «நீங்கள்» («நீங்கள் தயார்!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Tamil.** It has been AI-only since
2026-08-22. Callout is consistently the native «அறிவிப்பு» (65). The
VoiceOver hints are «-க்க» infinitives that compose correctly with «%1$s
இரட்டை தட்டவும்». One `agreed` grammar defect: TA-G1. Questions:
`docs/translation-questions/questions-ta.md` (Q1…Q12).

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
| app | செயலி | `unconfirmed` | Swept 2026-11-19: «பயன்பாடு» and «ஆப்ஸ்» retired except in fixed names such as «பயன்பாட்டு விதிமுறைகள்» (Terms of Use). See Q9 |

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

### TA-G2 — The user is the subject of heading callouts (`agreed` defect, `unconfirmed` wording, fixed 2026-11-19)

`directions_heading_*` and `directions_along_heading_*` said «…நோக்கிச் செல்கிறது» ("it is going"). They are about the user, so they now end in formal «செல்கிறீர்கள்». `directions_name_goes_*` («%1$s, இடதுபுறம் செல்கிறது») is about the road and keeps the neuter. `directions_along_facing_*` said «%1$s வழியாக» ("via"), wrong for someone standing still; now «%1$s இல்», as in `stationary_on_way`. See Q8.

### TA-T3 — Labels vs "Double tap to" hints (`agreed`)

Check the English comment, not the key name. The `location_detail_*full_screen*_hint` keys are button labels (read as the name), so they take the command form («…முழுத்திரையாக்கு»). `settings_keep_value` and `settings_use_value` follow "Double tap to", so they take the «-க்க» infinitive («வைத்திருக்க», «பயன்படுத்த»).

## Rejected

Nothing yet.

## Open questions

1. Callout «அறிவிப்பு»: confused with phone notifications?
2. Beacon «ஒலி பீக்கன்»: understood?
3. Waypoint «வழிப்புள்ளி»: natural?
4. The four detail levels (விரிவு / எளிமை / அத்தியாவசியம் / மௌனம்), renamed 2026-09-30 (C22) partly because «அமைதி» and «மௌனம்» were too close: clear by ear now? (TA-T1)
5. «%2$s நோக்கிச் செல்லும் %1$s» for "path to Moor Road"? (TA-G1)
6. Is «நீங்கள்» right?
7. **"Ahead" + road name** (`directions_ahead_road`, C24): «%1$s முன்னால்» since 2026-09-30, name first like «%1$s அருகில்» (AI choice; was «முன்னால் %1$s»). Natural by ear, or heard as "in front of the road"?
8. **Heading and facing callouts** (TA-G2): «%1$s வழியாக வடக்கு நோக்கிச் செல்கிறீர்கள்» and «%1$s இல் வடக்கு நோக்கி முகம்», both AI changes 2026-11-19. Natural by ear? Is «… நோக்கி முகம்» itself natural?
9. **App** «செயலி» (swept 2026-11-19 over «பயன்பாடு» / «ஆப்ஸ்»): the word people use?
10. **Transport place names**, AI changes 2026-11-19: Subway Station «சுரங்க ரயில் நிலையம்» (was the Tunnel word), Platform «நடைமேடை», Shelter «நிழற்குடை» (does it cover hiking shelters?), Pier «படகுத்துறை».
11. **No fix found:** Parking and Parking Space are both «வாகன நிறுத்துமிடம்»; Racquet «ராக்கெட்» sounds like "rocket"; beacon style Drop «சொட்டு» is a drop of liquid, the sound is an object dropped on a hard floor.
12. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_optician` → «கண் கண்ணாடிக் கடை» (it had been the same as glazier). Full stop added to `settings_reset_button_hint`. Uploaded live.

**2026-09-29 — Full review of all 1586 units.** 40 fixes uploaded. Marker had three words: «குறிப்பான்» (glossary, the Markers screen), «மார்க்கர்» (27, including the Nearby Markers button) and «குறியிடம்» (help text). All noun forms were swept to «குறிப்பான்» (TA-T2). The verb forms «குறியிடலாம்» and «குறியிடப்பட்ட» were kept. 4 hints had drifted to imperatives and now use the infinitive. Help text now uses «பீக்கனை இயக்கு», «வெளியேறும்போது எழுப்பு» and «எனைச் சுற்றி». Uploaded with `--skip-validate` and re-fetched: all matched exactly.

**2026-09-30 — all-language review, no speaker involved.** All 1598 units read by `/translation-review-all`; 28 flagged, 22 applied in `00b6e1a45`. Findings: `translations/review/2026-09-30-all-languages/ta-findings.json`.
Most serious fixed:
- `confect_name_to_via`: TA-G1 applies to _via too: 'from X up to Y via Z' instead of 'X leading to Y via Z'.
- `confect_name_to_dead_end`: Inherited TA-G1 (C21): «%1$s முதல் முட்டுச்சந்து வரை» says 'from the path up to the dead end'.
- `confect_name_to_dead_end_via`: Same TA-G1 'from … to' construction as confect_name_to_dead_end.
Held for a person: `confect_name_to` (reviewer: candidate); `terms_of_use_medical_safety_disclaimer` (legal text).

**2026-09-30 — `osm_path` no longer shares the Routes word.** It said «பாதை», the same word as `routes_title`, so a footpath sounded like a route. Now «ஒற்றையடிப் பாதை», a footpath word (AI choice, `unconfirmed`). The English comment now says Path means a footpath and must not share a word with Routes.

**2026-11-19 — full review of all 1691 units, no speaker involved.** 76 flagged, 70 applied in `518ce72af`. Findings: `translations/review/2026-11-19-ta/ta-findings.json`.
- Heading callouts now take the user as subject, facing callouts lost «வழியாக» (TA-G2, 24 strings).
- Unnamed train and tram lines name the track: «ரயில் பாதை», «டிராம் பாதை».
- Labels and hints sorted by how they are read (TA-T3).
- "App" swept to «செயலி»; typographic quotes restored in two strings; three search-synonym lists no longer list another category's word.
- About 30 place types fixed, including handball (was the volleyball word), dive centre (was "submarine"), subway (was the Tunnel word), `osm_track` (was bare «பாதை», the Routes word), and three pairs that sounded the same (wine/alcohol, beauty/cosmetics, garages/car repair).
Held for a person: `confect_name_to` (Q5), `terms_of_use_medical_safety_disclaimer` (legal text: «எனவே» misplaced in the last sentence), `first_launch_permissions_notification` (Q1), `beacon_styles_drop`, `osm_racquet`, `osm_parking_space` (Q11). Questionnaire Q8…Q11 added.
