# Polish (pl) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1598 units (2026-09-30) |
| Last native-speaker input | 2026-09-30: answers to all ten questions on `questions-pl.md` (Q1–Q10). Earlier: 2026-09-29 written feedback on *beacon*, plus two Weblate edits by `trc695`, probably the same person |
| Reporter platform | — |

Read with [`_common.md`](_common.md).

---

## Status of this file

**A native speaker answered the whole questionnaire on 2026-09-30.** The core
glossary is now `confirmed` or `agreed`. Three answers were handed back to the
developer to choose (the beacon place and Waypoint in Q3a, Marker in Q5,
movement verbs in Q8). Dave chose on 2026-09-30, and those rows say so. Rows
nobody has looked at are still `unconfirmed`.

A review pack was prepared for the first reviewer:

- `docs/translation-questions/questions-pl.md` — the numbered open questions below
  (Q1…Q9, round 2 since 2026-09-30), in the same layout as every other language.
- `translations/review/pl-full/07-nowe-teksty.md` — the per-text review of the 27
  strings added 2026-09-23, which was the published sheet until 2026-09-25.
- `translations/review/pl-full/` — the whole corpus split by area, plus
  `_numbering.tsv` mapping each number back to its Weblate key. Feedback will
  arrive citing those numbers.

---

## Glossary

| English | Polish | Status | Why |
|---|---|---|---|
| Callout (noun) | powiadomienie | `confirmed` | Reviewer 2026-09-30: the right word in both phone-OS and navigation UI (Q2) |
| Call out (verb) | powiadamiać o (+ locative) | `agreed` | Not «ogłaszać», which "sounds a bit artificial" (Q2). See PL-V1 |
| Beacon: **sound** | dźwięk naprowadzający | `confirmed` | Only for the audio signal. Reviewer: accurate, not too long, don't shorten it (Q3c). Never for the place or the feature, see PL-B1 and C19 |
| Beacon: **place/target** | cel | `agreed` | Reviewer: «cel» is the Polish for *destination*, and «punkt docelowy» is needlessly long. Dave chose «cel» for the beacon place over «punkt trasy» on 2026-09-30, so the Waypoint collision is gone (Q3a) |
| Beacon: **feature** | naprowadzanie | `confirmed` | Reviewer: "the exact equivalent of *guidance*" (Q3b). For docs and tutorial prose they suggest «adaptacyjne naprowadzanie dźwiękowe» as the full descriptive name. Not swept |
| Marker | znacznik | `agreed` | The reviewer called «znacznik» the professional word and «pinezka» the colloquial one. Dave chose «znacznik» on 2026-09-30. Never «(pinezka)» as a gloss (PL-I1) |
| Waypoint | punkt trasy | `confirmed` | Reviewer: the usual Polish navigation term for an intermediate point. «punkt pośredni» is an acceptable stylistic alternative, but Dave kept «punkt trasy» on 2026-09-30. Not for the beacon place |
| Landmarks | punkty orientacyjne | `unconfirmed` | Consistent with `callouts_places_and_landmarks` |
| Intersection / Junction | skrzyżowanie | `unconfirmed` | — |
| Sleep / Snooze | Tryb uśpienia / Tryb drzemki | `unconfirmed` | The pair is distinct, which is the main requirement |
| Callout Detail | Szczegółowość powiadomień | `unconfirmed` | Coined 2026-09-23 with the new setting |
| Detailed / Simplified / Essential / Silent | Szczegółowy / Uproszczony / Podstawowy / Wyciszony | `agreed` (Dave), `unconfirmed` (speaker) | English renamed 2026-09-30 (C22). Literal names on Dave's call; the reviewer had put «Uproszczony» on level 3. See PL-T1 |
| Streets and Junctions | Ulice i skrzyżowania | `unconfirmed` | Coined 2026-09-23 |
| Places to Call Out | Powiadamiaj o miejscach | `unconfirmed` | AI rewording 2026-09-30 to drop «ogłaszać» (PL-V1). Mirrors «Uwzględniaj odległość…». Not seen by the reviewer |
| Everything / No Places | Wszystko / Brak miejsc | `unconfirmed` | Distinct from `filter_all` «Wszystkie miejsca», which is a different setting |
| dead end | ślepa ulica | `agreed` | Reviewer: drop the diminutive, which sounds more professional. «droga bez przejazdu» is correct but too long (Q6) |
| Beacon (OSM map feature, `osm_beacon`) | Znak nawigacyjny | `agreed` | Reviewer (Q9). Not «znacznik», which is Marker |
| Form of address | ty | `confirmed` | Reviewer (Q4). See PL-R1 |

---

## Rules

### PL-G1 — `confect_name_dead_end` must be in the genitive (case fixed 2026-09-24; word `agreed` 2026-09-30)

**2026-09-30:** the reviewer chose «ślepa ulica» (Q6), so the string becomes «ślepej **ulicy**» and keeps the genitive. On the open side (undeclined map names), the reviewer says to leave it. See PL-G2.

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

### PL-B1 — *Beacon* is three meanings; Polish needs a word for each (`agreed`; words `agreed` 2026-09-30)

**2026-09-30 resolution:** sound = «dźwięk naprowadzający» (`confirmed`), place = «cel» (`agreed`, chosen by Dave from the reviewer's options), feature = «naprowadzanie» (`confirmed`). The reviewer's answer to Q3a treats «punkt trasy» as the word for an *intermediate* point and «cel» as the word for the destination, so their own 2026-09-29 Weblate edits (`menu_beacon_info`, `beacon_action_callout_beacon` → «punkt trasy») are superseded by that later answer rather than reverted silently. `callouts_audio_beacon_distance` becomes «Do celu: %1$s». This also removes a case bug: «w odległości» needs the genitive, but the 2–4 plural form is nominative («w odległości 2 metry»). `osm_beacon` → «Znak nawigacyjny» (Q9). The text from here down is the 2026-09-29 analysis.

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
`/tmp/translation-review/pl-findings.json`):**

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

### PL-R1 — Informal second person throughout (`confirmed` 2026-09-30)

Reviewer (Q4): stay with «ty». «Pan/Pani» would make Soundscape sound "like an electronic survey from a government office" rather than audio navigation. Phone and computer interfaces already say «Czy chcesz usunąć plik…», so users are used to it. Guard it. «Pan/Pani» must not appear anywhere.

The corpus addresses the user as «ty» — «możesz», «twoje trasy», imperatives
«Naciśnij», «Wybierz». It is consistent, so this is a real convention rather than
drift, but no speaker has endorsed it. Polish accessibility apps are split between
this and formal «Pan/Pani». See Q4.

### PL-T1 — The four detail levels must be distinguishable by ear (`agreed` 2026-09-30)

**Later on 2026-09-30:** the English levels were renamed Simplified / Essential (C22). Dave chose literal names: level 2 «Uproszczony», level 3 **«Podstawowy»**. The reviewer's «Uproszczony» therefore moved up one level. That goes against the answer below, so it's round-2 question 4.

Reviewer (Q1): rename Quiet «Cichy» → **«Uproszczony»** ("simplified"), because it announces only the necessary minimum. This also breaks the shared root with «Wyciszony». Swept: `callouts_verbosity_level_quiet`, `callouts_verbosity_description`, `action_no_such_callout_detail`, and the four help texts that recite the ladder. The generated `docs/users/help-*.pl.md` pages follow at the next release ([[docs-site-translations-need-a-release]]).

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

### PL-I1 — `markers_title` carries a parenthetical gloss (`agreed` 2026-09-30)

Resolved: «znacznik» only (Q5, Dave's choice from the reviewer's two). Remove every «(pinezka)» gloss (43 units). The home button `ui_action_button_nearby_markers` said «Bliskie pinezki», while the help texts called it «Bliskie znaczniki mapy (pinezki)» and `callouts_nearby_markers` said «Bliskie znaczniki». All three now say «Bliskie znaczniki». Keep `help_text_markers_content_1` «jak pinezki na mapie», because it translates the English simile "like pins in a map".

`markers_title` is «Znaczniki (pinezki)» while every other string uses «znacznik»
alone. Reads like an unresolved choice between two candidate terms that was shipped
rather than decided. Pick one. See Q5.

### PL-V1 — The callout verb is «powiadamiać o» (`agreed` 2026-09-30)

Reviewer (Q2): «Tryb Szczegółowy ogłasza wszystko w pobliżu» "sounds a bit
artificial". Use «powiadamia o wszystkim w pobliżu», which also pairs with the
noun: «powiadomienie» → «powiadamia».

This is a derived-form change (C3), not a word swap. «powiadamiać» takes
**o + locative**, so every object phrase has to be re-declined («ogłasza ulice,
skrzyżowania» → «powiadamia o ulicach, skrzyżowaniach»), and it has no natural
passive («są ogłaszane» must become an active sentence). Swept 18 units. Where
the object is a button being run («ogłasza *Wokół mnie*»), use «uruchamia».
Where it is where a sound plays («które powiadomienia ogłaszać po lewej»), use
«odtwarzać». `callouts_places_to_call_out` «Miejsca do ogłaszania» became
«Powiadamiaj o miejscach» (`unconfirmed` wording, see the questions below).

Not swept: generic «komunikat» in help prose ("messages"). The reviewer only
ruled on the noun «powiadomienie» and the verb.

### PL-G2 — Undeclined map names after prepositions: leave them (`confirmed` 2026-09-30)

Reviewer (Q7): «wzdłuż Marszałkowska» sounds "a bit robotic, like many other
navigation apps". It's a minor cosmetic flaw that doesn't stop anyone
understanding the callout. C17's label form was **not** adopted for Polish.
Leave `directions_along_*`, `confect_name_to*` and friends in their
preposition form.

### PL-D1 — Spoken distances and clock positions come out misdeclined (clock: code fixed 2026-09-30, words pending; distance: `unconfirmed`)

Reviewer (Q10): callouts like «Przejście dla pieszych piętnastu metrów na
godzinie dziewięciu» sound "quite nonsensical". The app builds
`name + ", " + distance + ", " + relative_clock_direction`
(`GeoEngineHelpers.formatDistanceAndDirection`), which gives «Przejście dla
pieszych, 15 metrów, na godzinie 9». The *text* is grammatical up to the digits.
The speech engine then declines the bare numbers:

- **Clock:** «na godzinie 9» needs the **ordinal** «dziewiątej». A digit
  can't say that, and the TTS picks the cardinal locative «dziewięciu». No
  Polish wording of `relative_clock_direction` with `%1$s` as a digit can fix
  this. It needs a code change: twelve localized clock positions (or ordinal
  words), not a number. cs «na %1$s hodině», sk and uk have the same shape.
  Slovenian works around it with «%1$s. uri» (an ordinal dot). See `_common.md` C20.
  **Code fixed 2026-09-30:** `relative_clock_hour_1`…`_12`. Polish values
  are «pierwszej … dwunastej» (feminine locative ordinals, agreeing with
  «godzinie»), uploaded once Weblate has the new keys.
- **Distance:** «15 metrów» on its own should read «piętnaście». Why the
  engine said «piętnastu» isn't known. It may be carrying case across the comma
  into «na godzinie». Ask which TTS engine and voice the reviewer uses.
- **Related, found in the sweep:** «w odległości %1$s» templates get the
  nominative 2–4 plural («w odległości 2 metry» should be «2 metrów»).
  `callouts_audio_beacon_distance` is fixed by PL-B1's «Do celu: %1$s». The two
  `behavior_scavenger_hunt_callout_next_flag*` strings are inventoried with the
  same rewording and are not applied.

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

- **«Pan/Pani» formal address** (Q4, 2026-09-30). Attractive because some
  Polish accessibility apps use it and older users are part of the audience.
  Rejected by the reviewer as sounding like official paperwork.
- **«pinezka» for Marker** (Q5). Attractive because it is what people say
  ("I'll send you a pin"). Rejected in favour of the more professional
  «znacznik». It survives only in the simile in `help_text_markers_content_1`.
- **«punkt trasy» for the beacon place** (the reviewer's own 2026-09-29
  Weblate edits). Attractive because a native speaker typed it. Superseded by
  their Q3a answer, which reserves it for intermediate points. It collides
  with Waypoint when no route is running.
- **«punkt docelowy»** for the beacon place or destination. Rejected by the
  reviewer as a padded «cel», like saying "point of destination".
- **«droga bez przejazdu»** for dead end. Correct, but too long for speech (Q6).
- **Shortening `directions_*`** (Q8). The reviewer sees nothing to cut.
- **«poruszasz się» for walking too** (Q8, the reviewer's optional
  simplification). Dave kept the distinction on 2026-09-30. The code already
  picks walking or vehicle strings, and «idziesz» is shorter and natural.
- **C17 label form for map names** (Q7). The reviewer prefers the robotic
  nominative. Declining names with AI at runtime isn't feasible offline, and
  the reviewer accepts the occasional error either way.

Once a reviewer turns something else down, record it here **with the evidence
that made it attractive**. Otherwise the next pass reinstates it (rule C8).

---

## Questionnaire round 1 — answered 2026-09-30

All ten questions on `docs/translation-questions/questions-pl.md` were answered:
Q1 → PL-T1, Q2 → PL-V1, Q3 → PL-B1, Q4 → PL-R1, Q5 → PL-I1, Q6 → PL-G1,
Q7 → PL-G2, Q8 → rejected (no change), Q9 → PL-B1 (`osm_beacon`),
Q10 → PL-D1.

## Open questions for round 2

Numbered as on `questions-pl.md` (round 2, 2026-09-30); Q9 is "anything else". Q5–Q8 were added after the 2026-09-30 full review.

1. **«Do celu: 105 metrów»** (`callouts_audio_beacon_distance`, spoken while
   walking to a beacon). Natural? Should the scavenger-hunt «Ustawiono
   naprowadzanie na %1$s, w odległości %2$s» change the same way, to «…, do
   celu: %2$s»?
2. **«Powiadamiaj o miejscach»** for the *Places to Call Out* setting (was
   «Miejsca do ogłaszania»). Would «Miejsca w powiadomieniach» be better?
3. **Q10 detail.** Which speech engine and voice (Google, Samsung, eSpeak,
   Vocalizer…)? Does «na godzinie dziewiątej» sound right, or is «na
   dziewiątej» more natural for clock positions? Did the engine say
   «piętnastu metrów» even with a comma before «na godzinie»?
4. **Detail levels after the English rename**: Szczegółowy / Uproszczony / Podstawowy / Wyciszony. You chose «Uproszczony» for the third level, which now says only the essentials. Is «Podstawowy» right there, with «Uproszczony» one level up?
5. **Facing callouts are masculine only** (C15). `directions_facing_*`
   «Jesteś zwrócony na północ» and `directions_along_facing_*` «Stoisz
   zwrócony na północ wzdłuż %1$s», 16 strings heard every time the user
   stands still. The help texts were changed to «patrzysz» in the same
   review, but these callouts were left for a speaker. Options: «Patrzysz
   na północ (wzdłuż …)», «Stoisz twarzą na północ (wzdłuż …)», «Kierunek:
   północ», or keep. Walking («Idziesz na…») and vehicle («Poruszasz się
   na…») forms are already neutral.
6. **Tunnels** (`directions_entering_tunnel`, `_named`): «Wejście do
   tunelu» reads as a noun, like a sign. Mostly heard in a vehicle, where
   «Wjazd do tunelu» is natural, but the string also covers footpath and
   railway tunnels. Options: «Wjazd do…» always, «Tunel» / «Tunel: %1$s», or
   keep. The named form puts the OSM name after «do» undeclined, which PL-G2
   accepts.
7. **Beacon style «Upadek»** (`beacon_styles_drop`): "a fall", for the sound
   of an object dropped on a hard floor. May suggest a person falling.
   Candidates «Stuknięcie», «Stuk», «Kropla». Must stay distinct from
   «Stukot» (Tactile).
8. **«Droga główna» twice**: `osm_trunk` (Trunk Road) and `osm_highway`
   (Highway) both say it. OSM Poland tags expressways (S roads) as
   highway=trunk, so «Droga ekspresowa» may be right for trunk.

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

**2026-09-29 — gender-neutral hint (C15).** `ui_action_button_nearby_markers_acc_hint` «…miejscach, które oznaczyłeś» was masculine only → «usłyszeć informacje o pobliskich oznaczonych miejscach». Uploaded and verified live.


**2026-09-30 — questionnaire answered (Q1–Q10).** The reviewer answered every
question, and handed three choices back to Dave (beacon place/Waypoint, Marker,
movement verb). He chose «cel» / «punkt trasy» / «znacznik» / keep «idziesz».
Swept into 76 units in `/tmp/translation-review/pl-findings.json`: 74 `agreed`,
2 inventory-only (PL-D1 scavenger hunt). The 2026-09-29 findings file was kept
as `pl-findings.2026-09-29.json`. The 74 `agreed` fixes were uploaded with `--skip-validate` the same day and verified live, all 74 matching. Q10's clock-position fault
is a code problem, recorded as `_common.md` C20.

**2026-09-30 — full AI review, no speaker involved.** All 1598 units read
against the English. 83 flagged, 62 applied (commit `626de0a3a`), 21 left for a
person. Applied: meaning and grammar errors (unnamed train/tram line gave «Na
pociąg», so `directions_generic_train`/`_tram` are now «linii kolejowej» /
«linii tramwajowej»; the two-finger gesture had lost «dwukrotnie»; `osm_chemist`
«Apteka» → «Drogeria» and «drogeria» dropped from the pharmacy search synonyms;
`help_text_automatic_callouts_how_2` broken by the «Powiadamiaj o miejscach»
rename; lost paragraph breaks in `new_version_info_details`), 23 masculine-only
forms in help, FAQ and tutorial (C15, «jesteś zwrócony» → «patrzysz»), and
terminology. `ui_action_button_my_location` «Moja\npozycja» → «Moja\nlokalizacja»:
«pozycja» came from the 2026-08-20 AI pass `03968a047`, replacing a truncated
«lokaliz», not from a speaker. Check that it fits the button at large font
sizes. Left for a person: Q5–Q8 above, plus `number_decimal_separator_a11y`,
which is a code problem in every language (the translations lost the spaces
around « point », so VoiceOver gets «1przecinek5»). Fixed the same day in code: `decimalSeparator()` now trims the word and adds the spaces itself, for every language. Findings are in
`/tmp/translation-review/pl-findings.json`.
