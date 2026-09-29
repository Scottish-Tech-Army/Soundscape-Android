# Polish (pl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2026-09-29: written feedback on *beacon* (PL-B1), plus two Weblate edits by `trc695`, probably the same person |
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

- `docs/translation-questions/questions-pl.md` — the numbered open questions below
  (Q1…Q10), in the same layout as every other language (converted 2026-09-25).
- `translations/review/pl-full/07-nowe-teksty.md` — the per-text review of the 27
  strings added 2026-09-23, which was the published sheet until 2026-09-25.
- `translations/review/pl-full/` — the whole corpus split by area, plus
  `_numbering.tsv` mapping each number back to its Weblate key. Feedback will
  arrive citing those numbers.

---

## Glossary

| English | Polish | Status | Why |
|---|---|---|---|
| Callout | powiadomienie | `unconfirmed` | Shipping since the first pass. Collides conceptually with system notifications — see Q2 |
| Beacon: **sound** | dźwięk naprowadzający | `agreed` (meaning) / `unconfirmed` (word) | Only for the audio signal. Never for the place or the feature — see PL-B1, C19, Q3c |
| Beacon: **place/target** | *open*: «punkt trasy» (reporter's Weblate edit), «cel», or «punkt docelowy» | `unconfirmed` | «punkt trasy» is also our Waypoint term — see PL-B1 and Q3a |
| Beacon: **feature** | naprowadzanie | `unconfirmed` | Already used in ~15 strings (`action_beacon_started`, `siri_*`, `route_beacon_progress`). No speaker has endorsed it by name. See Q3b |
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

### PL-B1 — *Beacon* is three meanings; Polish needs a word for each (`agreed`; words `unconfirmed`)

**Source:** written feedback from a Polish user, 2026-09-29, pasted into the
session. It argues the principle in general terms and proposes **no
replacement words**. The same day (02:20–02:45), Weblate user `trc695` joined
the project and changed exactly two strings:
`menu_beacon_info` → «Informacje o punkcie trasy» and
`beacon_action_callout_beacon` → «Powiadom o punkcie trasy». Almost certainly
the reporter. These are their only edits to date.

The reporter's point, in their words:

> *"There is no single Polish term that can safely represent all of these
> meanings without colliding with other existing navigation concepts such as
> marker/pin, waypoint, POI, destination, or audio guidance."*
>
> *"previously reviewed translations should ideally be preserved … A glossary
> rule that globally maps beacon to one Polish term would likely introduce
> errors."*

**Decision (`agreed`):** Polish keeps separate words for the three meanings
in C19. «dźwięk naprowadzający» stays for the **sound**. It is wrong for
the **place** ("distance to the guiding sound") and awkward for the
**feature**. This file previously had one glossary row for Beacon, which
made every review pass push towards one term. That row is replaced.

**Words (`unconfirmed`):** the place-word is the open question. The
reporter chose «punkt trasy», but this file already uses that for
**Waypoint**, and they themselves listed *waypoint* as a concept the
beacon must not collide with. When no route is running, the beacon is not
on a waypoint. So their edit is kept (C8: never revert a native speaker
silently) and asked about (Q3a), not swept.

**Sweep result (80 units mention beacon; full list in
`/tmp/weblate-review/pl-findings.json`):**

- **Sound, correct as is (~45):** styles, mute/unmute and their hints,
  first-launch, the tour, the FAQ answers about volume and holding the phone
  flat, `microsoft_copyright`, *"beacon sounds"* in the assistant help. Guard
  these. A pass that turns them into «naprowadzanie» is also wrong.
- **Place, wrong word (5):** `callouts_audio_beacon`,
  `callouts_audio_beacon_description`, `callouts_audio_beacon_distance`
  (currently «Naprowadzanie jest obecnie w odległości…»: the *feature* is
  not a distance away), plus the reporter's two «punkt trasy» edits,
  pending Q3a. `route_beacon_progress` «Naprowadzanie na %1$s» names the
  place explicitly and is fine.
- **Feature, sound-word where the feature is meant (3):**
  `callouts_no_beacon_active`, `settings_help_section_beacons_and_pois`,
  `help_text_destination_beacons_how_2` («usunąć … dźwięk naprowadzający»).
  Inventory only, because «naprowadzanie» is unconfirmed.
- **Moving the beacon to the next waypoint** (`route_detail_action_*_hint`,
  `routes_no_routes_hint_2`, `help_text_routes_content_what`,
  `help_text_remote_control_how`): «przenieść dźwięk naprowadzający do
  następnego punktu trasy». The sound really does move, so this reads
  correctly. Left alone.
- **Set a beacon on X** (FAQ questions, «ustawić dźwięk naprowadzający na
  adres»): acceptable Polish either way, and the corpus already mixes in
  «ustawić naprowadzanie na». Per the reporter, that mix is not something
  to normalise.
- **Button label `location_detail_action_beacon`** «Uruchom dźwięk
  naprowadzający»: pressing it does start the sound, so it is fine. Five
  help strings quote it verbatim, so it should not change casually.

**Also found in the sweep (one-off errors, `agreed`):**
`help_text_destination_beacons_when` «naprowadzania dzwiękowego» (missing
ź); `help_text_routes_content_what` «a Dźwięk naprowadzający» (capital
mid-sentence).

**Separate: `osm_beacon`** (the OSM map feature, a physical navigation
beacon, not ours) is «Znacznik nawigacyjny». «znacznik» is our **Marker**,
so a map beacon is announced as if it were a saved marker. «Znak
nawigacyjny» or «Stawa» would avoid that (`unconfirmed`, Q9).

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

- **One Polish word for every *beacon* string** (the old single glossary row,
  «dźwięk naprowadzający»). Attractive because it looks consistent, and
  C12's "split corpus" test would have flagged the variation as drift.
  Rejected 2026-09-29 on native-speaker feedback (PL-B1, C19).
- **Leaving "beacon" in English as a neutral fallback.** Attractive because
  it avoids choosing. Rejected by the reporter: Polish speech synthesis
  mispronounces it, and the word is heard more often than it is read.

Once a reviewer turns something else down, record it here **with the evidence
that made it attractive**. Otherwise the next pass reinstates it (rule C8).

---

## Open questions for the first native-speaker round

Numbered as on `docs/translation-questions/questions-pl.md`. The full-corpus pack
(`pl-full/00-przeczytaj-najpierw.md`) asks five of these in its own order (its 1–5 are
Q1, Q2, Q3, Q5 and Q8 here), so map a reply by the pack it cites.

1. **Do *Cichy* and *Wyciszony* differ enough spoken aloud?** (PL-T1)
2. **Is «powiadomienie» right for *callout*?** On a phone the word means a system
   notification. If it misleads, what replaces it, with a natural verb (rule C3)?
   (AI-only term, asked for confirmation)
3. **Beacon has three meanings: what is each called?** (PL-B1, C19)
   (a) the **place**: «punkt trasy» (the reporter's Weblate edit, but it
   collides with Waypoint and those commands work with no route running),
   «cel», «punkt docelowy», or something else? This also decides
   `callouts_audio_beacon`, `callouts_audio_beacon_distance`, `menu_beacon_info`
   and `beacon_action_callout_beacon`. (b) is «naprowadzanie» right for the
   **feature** (`callouts_no_beacon_active`,
   `settings_help_section_beacons_and_pois`)? (c) is «dźwięk naprowadzający»
   fine for the **sound**, or too long? (Originally only (c), as an AI-only term
   asked for confirmation. Reframed 2026-09-29.)
4. **Informal «ty» or formal «Pan/Pani»?** (PL-R1)
5. **«Znaczniki» or «pinezki»?** (PL-I1)
6. **«ślepa uliczka», «ślepa ulica» or «droga bez przejazdu»?** (PL-G1)
7. **Map names after «do», «na», «wzdłuż» arrive undeclined** («wzdłuż
   Marszałkowska»): how bad, and would C17's label form be better? (PL-G1's open
   side)
8. **Directions and distances**: `directions_*` is 108 strings spoken many times a
   day. Can they be shorter without losing clarity?
9. **`osm_beacon`** (a physical navigation beacon on the map) is
   «Znacznik nawigacyjny», but «znacznik» is our Marker. «Znak nawigacyjny»,
   «Stawa», or something else? (PL-B1)
10. Anything else.

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

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 24 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Byte `*_a11y` plurals: the «few» form had no number; `%1$s` restored. «Wybór głosu TTS» / «Syntezator mowy (TTS)» kept (the capitals check is ignored). Uploaded live.

**2026-09-29 — first native-speaker input: *beacon* (PL-B1).** A written
argument, received in the session, that *beacon* needs different Polish words
for different meanings and that single-term glossaries and AI "consistency"
passes will keep undoing this. Accepted as a principle and recorded
cross-language as `_common.md` C19, with a carve-out added to C12. Swept all 80
beacon units. Only the two one-off typos are ready to apply; the place and
feature strings wait on Q3 and Q9 (numbered Q10–Q12 in the first draft, merged into the published sheet the same day). The repo's `values-pl/strings.xml` was
behind Weblate for the reporter's two edits at the time of the sweep.

The two one-off typos (`help_text_destination_beacons_when`, `help_text_routes_content_what`) were uploaded and verified live the same day.

**2026-09-29 — European batch review.** 5 hints were imperatives («zmień», «wyświetl», «wybierz», «zwiń», «rozwiń») → infinitives after «aby». `help_text_remote_control_how` «Menu dźwiękowe»/«Sterowanie multimediami» → the labels «Menu audio»/«Tryb sterowania przyciskami multimedialnymi»; `help_text_routes_content_how_1` «…i Trasy» → «…i trasy». None of these keys had human edits. 7 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live. **Held:** `ui_action_button_nearby_markers_acc_hint` «oznaczyłeś» is masculine only (C15).
