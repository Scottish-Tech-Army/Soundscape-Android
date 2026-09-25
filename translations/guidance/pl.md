# Polish (pl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Reporter platform | — |

Read with [`_common.md`](_common.md).

---

## Status of this file

**No native speaker has reviewed Polish.** Everything below was decided by an AI
translation pass, so almost nothing here is `confirmed` or `agreed` — the entries
are `unconfirmed` and exist to give the first reviewer something to react to,
not to be swept across the corpus.

Two entries are marked `agreed`, and only because neither turns on taste: PL-G1
is grammar (Polish case government is not an opinion), and PL-C1 is a fact
about the repo (there is no Polish `AppShortcuts.strings`). Even in PL-G1, the
replacement *wording* stays `unconfirmed`.

A review pack was prepared for the first reviewer:

- `translations/review/pl.md` — the 27 strings added 2026-09-23, with context.
- `translations/review/pl-full/` — the whole corpus split by area, plus
  `_numbering.tsv` mapping each number back to its Weblate key. Feedback will
  arrive citing those numbers.

---

## Glossary

| English | Polish | Status | Why |
|---|---|---|---|
| Callout | powiadomienie | `unconfirmed` | Shipping since the first pass. Collides conceptually with system notifications — see Q2 |
| Audio Beacon | dźwięk naprowadzający | `unconfirmed` | Accurate but long for something spoken often — see Q3 |
| Marker | znacznik | `unconfirmed` | `markers_title` says «Znaczniki (pinezki)» — see PL-I1 |
| Waypoint | punkt trasy | `unconfirmed` | Not a calque of our own "route point" gloss (rule C1), but no speaker has confirmed it is what Polish mapping apps use |
| Landmarks | punkty orientacyjne | `unconfirmed` | Consistent with `callouts_places_and_landmarks` |
| Intersection / Junction | skrzyżowanie | `unconfirmed` | — |
| Sleep / Snooze | Tryb uśpienia / Tryb drzemki | `unconfirmed` | The pair is distinct, which is the main requirement |
| Callout Detail | Szczegółowość powiadomień | `unconfirmed` | Coined 2026-09-23 with the new setting |
| Detailed / Balanced / Quiet / Silent | Szczegółowy / Zrównoważony / Cichy / Wyciszony | `unconfirmed` | Four level names that must stay distinct **by ear** — see PL-T1 and Q1 |
| Streets and Junctions | Ulice i skrzyżowania | `unconfirmed` | Coined 2026-09-23 |
| Places to Call Out | Miejsca do ogłaszania | `unconfirmed` | Coined 2026-09-23 |
| Everything / No Places | Wszystko / Brak miejsc | `unconfirmed` | Distinct from `filter_all` «Wszystkie miejsca», which is a different setting |
| dead end | ślepa uliczka | `unconfirmed` | Possibly wrong register — see PL-G1 |

---

## Rules

### PL-G1 — `confect_name_dead_end` must be in the genitive (case `confirmed` fixed 2026-09-24, word `unconfirmed`)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «ślepej uliczki», uploaded and verified live. This applied the genitive to the *existing* noun only. The register question below stays open, and if the noun changes, the genitive has to change with it.

`confect_name_dead_end` is never shown on its own. `WayGenerator.kt` substitutes it
as the `%2$s` of `confect_name_to` and `confect_name_to_via` — and only those two.

Both Polish templates render that slot as «**do** %2$s», and *do* governs the
**genitive**. The string is currently in the nominative, so the app says:

> Droga **do ślepa uliczka**

It should say «do **ślepej uliczki**». This is the same defect `_common.md` rule C9
records for uk, ru, cs, sk, hr, sr and sl — Polish is named in that list. The case
is not a matter of opinion, so the diagnosis is `agreed`; it is one string.

Separately and **`unconfirmed`**: «ślepa uliczka» may be the wrong register.
In Polish it reads as a metaphorical impasse more readily than as a street type;
a road sign would say «ślepa ulica» or «droga bez przejazdu». Ask before applying —
if the noun changes, the genitive changes with it.

> Open side: the same `%2$s` slot also receives destination names straight from
> OpenStreetMap, in the nominative, giving «Droga do ulica Główna». Same unresolved
> question as Ukrainian's.

### PL-R1 — Informal second person throughout (`unconfirmed`)

The corpus addresses the user as «ty» — «możesz», «twoje trasy», imperatives
«Naciśnij», «Wybierz». It is consistent, so this is a real convention rather than
drift, but no speaker has endorsed it. Polish accessibility apps are split between
this and formal «Pan/Pani». See Q4.

### PL-T1 — The four detail levels must be distinguishable by ear (`unconfirmed`)

*Szczegółowy / Zrównoważony / Cichy / Wyciszony*. The user picks between these
without looking, often while walking, and hears them read by a speech synthesiser.
*Cichy* and *Wyciszony* share a root and may be too close. If either changes, it
changes in eleven places: `callouts_verbosity_level_*`, `callouts_verbosity_set`,
`callouts_verbosity_description`, `action_no_such_callout_detail`, and the four
help/FAQ texts that recite the ladder.

### PL-C1 — Siri command phrases stay in English (`agreed`)

There is no `pl.lproj/AppShortcuts.strings`, so Siri has no authored Polish phrases
for Soundscape. `help_text_assistant_how_ios` and `help_text_assistant_commands_ios`
therefore quote the **English** commands (`*"Soundscape surroundings"*` etc.), while
the choices after each dash are translated.

Do not "fix" this by translating the quoted phrases: a phrase Siri does not
recognise is worse than an English one that works. See
[[ios-siri-phrases-are-outside-weblate]]. If Polish Siri phrases are ever authored,
these two strings must change with them.

### PL-I1 — `markers_title` carries a parenthetical gloss (`unconfirmed`)

`markers_title` is «Znaczniki (pinezki)» while every other string uses «znacznik»
alone. Reads like an unresolved choice between two candidate terms that was shipped
rather than decided. Pick one. See Q5.

### PL-Q1 — Quote convention (`unconfirmed`, cosmetic)

Help and FAQ strings use Polish quotes «„…"»; the two iOS/Siri strings use the
escaped `\"` form, matching what the rest of the corpus does in those keys. Worth
normalising eventually, but it is invisible to a listener and low priority.

---

## Rejected

Nothing yet. Once the first reviewer turns something down, record it here **with
the evidence that made it attractive** — otherwise the next pass reinstates it
(rule C8).

---

## Open questions for the first native-speaker round

These are the questions put to the reviewer in `translations/review/pl-full/00-przeczytaj-najpierw.md`.

1. **Do *Cichy* and *Wyciszony* differ enough spoken aloud?** They are separate
   levels chosen by ear. (PL-T1)
2. **Is «powiadomienie» right for *callout*?** On a phone the word means a system
   notification; here it is a spoken description of the surroundings. If it misleads,
   what replaces it — and note the verb forms, since a term that has no natural verb
   is a rewrite rather than a swap (rule C3).
3. **Is «dźwięk naprowadzający» too long for *beacon*?** It appears in many strings
   and is often spoken. Is there something shorter that stays clear?
4. **Informal «ty» or formal «Pan/Pani»?** (PL-R1)
5. **«Znaczniki» or «pinezki»?** (PL-I1)
6. **Is «ślepa uliczka» the right register for a dead-end street,** or should it be
   «ślepa ulica» / «droga bez przejazdu»? (PL-G1)
7. **Directions and distances** — `directions_*` is 108 strings spoken many times a
   day. Can they be shorter without losing clarity?

---

## Provenance

**2026-09-23 — AI translation pass.** 27 new strings covering the Callout Detail
feature translated and uploaded to Weblate as part of a 45-language batch, taking
Polish to 1521/1521. Terminology for the new setting was coined in this pass and has
never been reviewed.

**2026-09-24 — corpus sweep, no speaker involved.** Built the review packs and, in
doing so, found PL-G1 (dead-end genitive, checked against `WayGenerator.kt`),
PL-N1 (byte-size strings not plural-aware) and PL-I1 (`markers_title` double-naming).
All three predate the 2026-09-23 pass.

The first draft of PL-N1 also claimed the distance strings were broken. They are
not — they are plurals and Polish fills every form. Corrected the same day after
Dave queried it: Weblate's API returns only the *first* form of a plural unit in
`source`/`target`, so a correct four-form Polish plural arrives looking like a
bare singular ("%1$s metr"). [[weblate-plural-units-need-patch-api]] warns about
exactly this and the sweep walked into it anyway — read `values-pl/strings.xml`
for any unit that looks like it has a number-agreement problem, rather than
trusting the API's flat view. No plural unit was touched by the 2026-09-23
upload, which was checked afterwards, so nothing was half-written in any of the
45 languages.

No feedback has been received yet. When it arrives it will cite numbers from the
review pack; `translations/review/pl-full/_numbering.tsv` maps those to Weblate keys.

**2026-09-24 — plural-unit fix pass, no speaker involved.** `intersection_approaching_intersection_distance`
(new string, «Skrzyżowanie za %1$s») translated normally. Separately,
`bytes_format_{b,kb,mb,gb,tb}` and their `_a11y` variants — split into real
`<plurals>` resources by commit `d3c2dab25` — had their Polish `few`/`other`
forms filled in via direct Weblate PATCH (see [[weblate-plural-units-need-patch-api]]);
the `one` slot was left untouched. This narrowed PL-N1 from "not plural-aware"
to "the `one` form is still wrong". Corpus now 1522/1522, untranslated=0.

**2026-09-24 — `one`-form fix, no speaker involved.** Dave caught that the
previous pass left `bytes_format_*_a11y`'s `one` slot wrong ("1 bajtów")
despite flagging it in PL-N1, and asked for it to be fixed outright rather than
just documented — across all languages, not only Polish. All five Polish units'
`one` slot corrected to the true singular (bajt/kilobajt/megabajt/gigabajt/terabajt)
via direct Weblate PATCH. Mid-run the Weblate component got locked (by Dave,
for an unrelated reason) and the first attempt failed outright on every
request; re-run cleanly after he unlocked it. Fully resolved and grammatically
uncontroversial (no reviewer decision needed), so PL-N1 was removed from
Rules rather than kept as `fixed` — this entry is the record of it.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «ślepej uliczki» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).

**2026-09-25 — truncation repaired (C16).** `terms_of_use_medical_safety_disclaimer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
