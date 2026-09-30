# Swahili (sw) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Singular «wewe» («Uko tayari!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Swahili.** It has been AI-only since
2026-08-21. As with Hausa, idiomaticity can't be judged here. Things that
already work:
- «Uko tayari!» is gender-neutral, because Swahili doesn't mark gender.
- Traveling/Heading are «Unasafiri»/«Unatembea», the correct vehicle/walking
  split.
- «Kituo» for Waypoint is the same "stop" word Ukrainian chose (C1).
- The «ku»-infinitive hints compose with «Gusa mara mbili %1$s».
- There is no `sw.lproj`, so the Siri phrases stay in English.

Questions: `docs/translation-questions/questions-sw.md` (Q1…Q6).

## Glossary

| English | Swahili | Status | Note |
|---|---|---|---|
| Callout | tangazo la sauti | `unconfirmed` | |
| Audio Beacon | beacon ya sauti | `unconfirmed` | **The English word "beacon" left untranslated** in 114 places. See SW-T1 |
| Marker | alama | `unconfirmed` | |
| Waypoint | kituo | `unconfirmed` | |
| Landmarks | vivutio | `unconfirmed` | "Attractions", narrower than landmark |
| Intersection | makutano | `unconfirmed` | |
| Sleep / Snooze | Lala ; Sinzia | `unconfirmed` | |
| dead end | mwisho wa barabara | `unconfirmed` | "End of the road" |

## Rules

### SW-T1 — "beacon" is untranslated (`unconfirmed`)

«Beacon ya Sauti» keeps the English noun. That may be fine if Swahili
speakers know it (C12), or a native word such as «mwongozo wa sauti» (sound
guide) may be clearer. There are 114 occurrences, so ask first.

## Rejected

Nothing yet.

## Open questions

1. Beacon: keep «beacon», or a Swahili word? (SW-T1)
2. Is the Swahili generally natural, or does it read as machine translation?
3. Callout «tangazo la sauti»: natural?
4. Landmarks «vivutio»: too touristy?
5. **The four detail levels** (Kwa Kina / Rahisi / Muhimu / Kimya), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `help_config_voices_content` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_station` → «Stesheni» (it had been «Kituo», the same as Waypoint), `osm_path` → «Kijia» (it had been «Njia», same as Routes), `osm_dock` → «Godi». Uploaded live.

**2026-09-29 — Stale beacon FAQ fixed (C16).** `faq_how_to_use_beacon_answer` had been translated from an older English. Three passages were replaced to match the current source:
- the sailboat-tacking sentence, now "you may still need to make navigation choices along the way to work around obstacles";
- "turn the phone slowly", now "slowly turn in a circle";
- "the lighthouse metaphor … has natural implications", now "This design has a few natural results".

The extra *…* pair around the "tacks" word went with the sentence. The rest of the text was left unchanged. The new wording is `unconfirmed`. Found by a cross-language check after the bg/hr reviews. Uploaded and verified live.

**2026-09-29 — full review.** 29 fixes. Around Me button «Pande Zangu» → «Karibu Nami», the name used in 12 help and tour strings. Voices «Sauti» collided with the Audio section «Sauti» → «Aina za Sauti» (the Android help already said so; iOS help updated). Beacon «mwongozo wa sauti» / «Kiashiria» (5 strings) → «beacon ya sauti» like everywhere else (SW-T1 still asks whether to replace «beacon»). Help button names now match the labels («Zima/Washa Sauti ya Beacon», «Taarifa za Mahali», «Imekamilika»). 11 hints → ku-infinitives. Four crossing callouts «Inapita» ("it passes") → «Unapita». Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
