# Icelandic (is) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units, 769 excluding `osm_*` (2026-09-24) |
| Last native-speaker input | 2025-10-21 (Þorkell Steindal, direct Weblate edits to the legacy `android-app` component) |
| Reporter platform | Weblate, legacy Android app |
| Register | «þú», the only real option in Icelandic. Not a question |

Read with [`_common.md`](_common.md).

---

## Status of this file

Icelandic is between the French and Spanish cases. **One native speaker has
worked on it**: Þorkell Jóhann Steindal (keli@sjonstodin.is; Sjónstöðin is
Iceland's national centre for blind and visually impaired people). He made
five translation commits to the *legacy* `app/src/main/res/values-is` between
2025-09-05 and 2025-10-21, about 390 lines in total. That was before the KMP
migration. His strings were carried into `androidkmp`, then corrected and
extended by AI passes.

His work reads as a fast first pass. It had frequent typos («tvisvr», «pukt»,
«fjalægð», «leið lokiðleið lokið»), all of which have since been fixed, and
nearly everything in lowercase (see IS-S1). His **term choices** are the
valuable part. They still ship, consistently, and are the one thing here a
native speaker actually chose. They are marked `confirmed` in the Spanish
file's sense: "confirmed then, not re-checked since." Everything else is
`unconfirmed`.

The questions for reviewers are in `docs/translation-questions/questions-is.md`, numbered
Q1…Q11 to match the Open questions below.

---

## Glossary

| English | Icelandic | Status | Why |
|---|---|---|---|
| Audio Beacon | hljóðviti | `confirmed` | Þorkell's choice. 71 occurrences, consistent |
| Marker | merki | `confirmed` | Þorkell's choice. 77 occurrences |
| Waypoint | leiðarpunktur | `confirmed` | Þorkell's choice («næsti leiðarpunktur»). 26 occurrences |
| Callout | tilkynning | `confirmed` | Þorkell's choice. 38 occurrences. But Android's Icelandic UI also uses «tilkynningar» for system notifications, the same collision flagged for fr/pl. See Q1 |
| Callouts panel | lýsing á umhverfi | `confirmed` | Þorkell's `callouts_panel_title` |
| Landmarks | kennileiti | `confirmed` | Þorkell's «staðir og kennileiti» |
| Intersection | gatnamót | `confirmed` | Þorkell's choice. 22 occurrences |
| Junction (motorway, with ref) | vegamót | `unconfirmed` | `directions_junction_with_ref` «Vegamót %1$s». AI pass |
| Sleep | dvali (fara í dvala / í dvala) | `confirmed` | Þorkell's choice |
| Snooze | lúr («Í lúra-ham») | `unconfirmed` | Þorkell wrote «Dvala eða Lúra» once in the FAQ, but «Í lúra-ham» is AI-built. See IS-T1 / Q3 |
| Callout Detail | Nákvæmni tilkynninga | `unconfirmed` | AI pass, 2026-09-23 |
| Detailed / Balanced / Quiet / Silent | Ítarlegt / Jafnvægi / Hljóðlátt / Þögult | `unconfirmed` | «Jafnvægi» is a noun among three adjectives. See Q7 |
| dead end | blindgata | `unconfirmed` | The word is fine. Case fixed 2026-09-24 («blindgötu»). See IS-G1 |

---

## Rules

### IS-G1 — `confect_name_dead_end` must be in the genitive (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «blindgötu», uploaded to Weblate and verified live.

The same defect as PL-G1 and rule C9 in `_common.md`: `confect_name_dead_end`
is only ever the `%2$s` of `confect_name_to` «%1$s til %2$s» and
`confect_name_to_via` «%1$s til %2$s um %3$s» (see `WayGenerator.kt`). «til»
governs the **genitive**, and the string is nominative, so the app says
«Stígur til **blindgata**». It should be «til **blindgötu**». Icelandic was
missing from C9's list, so add it there.

The fix is one string, and the grammar is not a matter of opinion. The
reviewers should still see it (Q6), in case they prefer a different
construction altogether.

### IS-G2 — Street and place names arrive in the nominative (`unconfirmed`, inventory only)

This is the same issue as PL-G1's open side, but much wider in Icelandic,
because Icelandic street names inflect and speakers hear it immediately. Every
template that puts a map name after a preposition gets it wrong:
`directions_on_road` «Á %1$s» → «Á **Laugavegur**» (should be «Á Laugavegi»,
dative), `directions_near_name` «nærri %1$s», `directions_towards_settlement`
«í átt að %1$s», `directions_along_*` «eftir %1$s», `confect_name_to` «til
%1$s», and more.

No string edit can inflect a runtime name. The options are restructuring the
templates so the name stands alone («Gata: Laugavegur» / «Laugavegur, þú ert
hér»), or accepting it. Ask how much it grates before touching the roughly 40
templates involved. See Q5.

### IS-G3 — The user's gender: three conventions in one corpus (`unconfirmed`)

The corpus handles adjectives that agree with «þú» three different ways:

- **slash forms**: «Þú ert tilbúin/n!» (`first_launch_prompt_title`,
  `first_launch_prompt_message`, `tour_create_marker_done`) and «Velkomin/n»
  (`tour_welcome`). **A speech synthesiser reads the slash aloud**, or reads
  «tilbúin» and then a stray «n». This is the worst option for this app's
  users
- **feminine/plural only**: `first_launch_welcome_title` «Velkomin!»
- **masculine only**: `faq_markers_function_answer` «…sem þú bættir við sjálfur»

The fix is probably to rephrase all of these so they don't agree («Allt er
tilbúið!», «Velkomið í…» / «Gott að sjá þig»), rather than picking a gender.
That wording needs a speaker. See Q4.

### IS-S1 — 151 labels are lowercase where the English is capitalised (`unconfirmed`, likely `agreed`)

A carry-over from Þorkell's first pass: «hætta við», «lokið», «upphaf leiðar»,
«slökkva á hljóðvita», and even full sentences like «illa gengur að finna
staðsetningu.». The later AI strings are capitalised, so the app now mixes
the two from one screen to the next. Icelandic capitalises the first word of
a sentence or button like English does, so this looks like haste rather than
a style choice. It is invisible to speech, so low priority, but it is a
mechanical fix once a speaker says yes. See Q9.

### IS-T1 — Snooze is «Í lúra-ham» (`unconfirmed`)

«lúra» is the verb (to doze), so «lúra-hamur» is an unusual compound. «lúr»
(a nap) might give «Lúrhamur», or the reviewers may prefer to describe what
it does, since it wakes automatically when you move off. Five strings. See Q3.

### IS-T2 — Two different verbs for "double tap" (`unconfirmed`)

`talkback_double_tap_template` is «Tvíbankaðu til að %1$s», and
`faq_controlling_what_you_hear_answer` also says «Tvíbankaðu skjánum með tveimur
fingrum». *(Corrected 2026-09-25: an earlier note said seventeen strings use
«Ýttu tvisvar»; none do. The seventeen are plain «Ýttu á…», "press".)*
«tvíbanka» is not a common verb, so «Ýttu tvisvar til að %1$s» is the candidate,
but ask first. See Q8.

(The missing space in this template was fixed in Weblate on 2026-09-24, see
FR-B1 in `fr.md`. The local file catches up on the next Weblate merge.)

### IS-C1 — Siri command phrases stay in English (`agreed`)

There is no `is.lproj/AppShortcuts.strings`, and Siri does not support
Icelandic as a language at all, so `help_text_assistant_*_ios` has to quote
the English phrases (`*"Soundscape surroundings"*`). Do not translate them,
for the same reasons as PL-C1.

---

## Rejected

Nothing yet. Record anything turned down here with the evidence that made it
attractive (rule C8).

---

## Open questions for the next native-speaker round

These are the questions in `docs/translation-questions/questions-is.md`, in the same order.

1. **Callout «tilkynning»:** does it get confused with phone notifications?
   (glossary)
2. **Waypoint «leiðarpunktur»:** still right? (glossary; checking an earlier choice)
3. **Snooze name.** (IS-T1)
4. **Gender:** how to rephrase «tilbúin/n», «Velkomin/n». (IS-G3)
5. **Street names in the nominative** («Á Laugavegur»): how bad is it? (IS-G2)
6. **«til blindgötu»:** confirm what now ships (applied 2026-09-24). (IS-G1)
7. **The four detail levels:** is «Jafnvægi» (a noun) OK next to three
   adjectives, and are they distinct by ear? «Hljóðlátt» may also be heard as volume
   rather than fewer callouts.
8. **«Tvíbankaðu» or «Ýttu tvisvar»?** (IS-T2)
9. **Capitalise the 151 lowercase labels?** (IS-S1)
10. **Siri phrases stay English:** OK? (IS-C1)
11. **Anything else.**

---

## Provenance

**2025-09-05 → 2025-10-21 — native speaker, legacy `android-app` component.**
Þorkell Steindal (Sjónstöðin) translated about 390 lines directly in Weblate
(commits `0e774f7d2`, `88372a500`, `c64fe8d03`, `dcf37fd87`, `e63fb8885`).
He chose the core terminology recorded above as `confirmed`.

**2026-04 → 2026-09 — KMP migration and AI passes.** Strings were carried into
`androidkmp` and completed and corrected by AI passes. The two "Anonymous"
commits (`63225c826`, `db573314c`) are mechanical: the plural conversion and
quote escaping.

**2026-09-24 — corpus sweep, no speaker involved.** Built this file and the
question sheet. Found IS-G1 by checking `WayGenerator.kt`, and IS-G3 by
looking for slashes, which a speech synthesiser reads aloud. Checked that
Þorkell's terms still ship and that his typos are gone. Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «blindgötu» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
