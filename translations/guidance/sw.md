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

Questions: `translations/review/sw.md` (Q1…Q5).

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
5. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `help_config_voices_content` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
