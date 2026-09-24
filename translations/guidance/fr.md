# French (fr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units, 769 excluding `osm_*` (2026-09-24) |
| Last native-speaker input | **none yet** |
| Reporter platform | — |
| Register | Formal «vous», consistent across the corpus (`unconfirmed`, see FR-R1) |

Read with [`_common.md`](_common.md).

`fr_CA` is a separate Weblate language with its own file (`values-fr-rCA`).
About two thirds of its units are identical to `fr`. Nothing here applies to it
automatically: Canadian usage differs on exactly the kind of term this file
covers (courriel, magasiner, and the tu/vous balance). Give it its own
`fr_CA.md` once a Canadian speaker weighs in.

---

## Status of this file

**No native speaker has reviewed French.** *Correction (2026-09-24):* an
earlier draft said every French string came from AI passes. That's wrong.
The oldest ~360 strings were copied from **Microsoft's professional fr-FR
localisation** of the iOS app (see `_common.md` C14), and 213 of them are
still Microsoft's exact wording. 27 have drifted even though their English
is unchanged. Everything Microsoft never had (callout detail, confected way
names, voice commands…) is AI. Like `pl.md`, this file gives the first
reviewers something to react to. Most entries are `unconfirmed`, and nothing
should be swept from it.

Checked 2026-09-24: **«notification» (Callout), «point de repère»
(Waypoint), «repères» (Landmarks) and «balise sonore» (Beacon) are all
Microsoft's own terms.** FR-T1 and FR-T2 therefore challenge a professional
translator's choice that long-time users have heard for years, not an AI
slip. The collisions are still real, but changing them costs familiarity.
Put that to the reviewers, and record the outcome under C8 either way.

Two entries are `agreed` because neither is a matter of taste. FR-G1 is a
grammar defect you can check against `WayGenerator.kt`. FR-B1 was a missing
space that turned two words into one, and is now fixed. In FR-G1 the *replacement wording* stays
`unconfirmed`.

The questions for reviewers are in `translations/review/fr.md`. Feedback will
cite its numbered questions (Q1…Q11), which match the Open questions list below.

---

## Glossary

| English | French | Status | Why |
|---|---|---|---|
| Callout | notification | `unconfirmed` | Used in the UI and settings. On a phone the word means a *system* notification. The same collision as Polish «powiadomienie». See FR-T1 / Q1 |
| Callout (verb, "call out") | annoncer / annonce | `unconfirmed` | 23 strings already use «annonce(r)», so the corpus is split. See FR-T1 |
| Audio Beacon | balise sonore | `unconfirmed` | 73 occurrences, consistent. Short and idiomatic |
| Marker | marqueur | `unconfirmed` | 84 occurrences, consistent |
| Waypoint | point de repère | `unconfirmed` | **Collides with Landmark.** See FR-T2 / Q2 |
| Landmarks | repères | `unconfirmed` | `callouts_places_and_landmarks` «Lieux et repères». See FR-T2 |
| Route | itinéraire | `unconfirmed` | Often capitalised mid-sentence. See FR-S1 |
| Intersection | intersection | `unconfirmed` | 23 occurrences. «carrefour» is never used. See Q6 |
| Junction (motorway, with ref) | sortie | `unconfirmed` | `directions_junction_with_ref` «Sortie %1$s». Correct for French motorways, where junctions are numbered exits |
| Sleep | veille (Mettre en veille / En veille) | `unconfirmed` | See FR-T3 |
| Snooze | désactivation temporaire | `unconfirmed` | See FR-T3 / Q3 |
| Callout Detail | Détail des notifications | `unconfirmed` | Moves with Callout if FR-T1 changes |
| Detailed / Balanced / Quiet / Silent | Détaillé / Équilibré / Discret / Silencieux | `unconfirmed` | Four names chosen by ear. Unlike Polish, none of them share a root, so this looks fine. Still worth one confirmation |
| dead end | impasse | `unconfirmed` | The word is right. The template it goes into is not. See FR-G1 |

---

## Rules

### FR-G1 — `confect_name_to` produces «Sentier à impasse» (`agreed` for the defect, `unconfirmed` for the wording)

`confect_name_dead_end` never appears on its own. `WayGenerator.kt` substitutes
it as the `%2$s` of `confect_name_to` and `confect_name_to_via` («%1$s à %2$s»
and «%1$s à %2$s via %3$s»), where `%1$s` is the way type ("Sentier",
"Trottoir"…). The app says:

> Sentier **à impasse**

French needs an article here, and «à» is the wrong preposition for a way
that *leads to* somewhere. Likely candidates: «Sentier **menant à une**
impasse», «Sentier **sans issue**», or a template with «vers» plus the article
in the substituted string («une impasse»). Rule C9 in `_common.md` recorded the
same kind of defect for the Slavic languages. French is the non-inflecting
version of it: the problem is the article, not the case.

The same `%2$s` slot also receives destination names straight from
OpenStreetMap. «Sentier à Rue de la Paix» has the same problem and no article
can fix it. See FR-G2.

### FR-G2 — Place names from OpenStreetMap can't contract with «de» / «à» (`unconfirmed`, inventory only)

Many templates put a raw OSM name after a preposition: `directions_near_name`
«À proximité de %1$s», `directions_approaching_name` «Vous approchez de
%1$s», `directions_on_road_at_junction` «Sur %1$s à %2$s», the
`directions_along_*` family «le long de %1$s», and `confect_name_to`. Names
that start with an article or a vowel then come out uncontracted:

- «près de **Le** Bon Marché» (should be «du Bon Marché»)
- «à **Le** Mans» (should be «au Mans»)
- «de **A**venue Foch» (should be «d'Avenue Foch», or better «de l'avenue Foch»)

No string edit can fix this, because the name only arrives at runtime. The
options are code (contraction at substitution time) or restructuring
templates so the name isn't directly governed («Près de : %1$s»). The code
option is an all-Romance-languages decision (Italian, Spanish and Portuguese
have the same contractions), so it belongs in `_common.md` once a speaker
confirms how much it grates. See Q5.

### FR-B1 — `talkback_double_tap_template` was missing its space (`confirmed` fixed, 20 languages, 2026-09-24)

French was «Appuyez deux fois pour%1$s». The fragment is substituted without a
leading space (`TalkbackHelpers.ios.kt:activationHint`), so VoiceOver read
«Appuyez deux fois **pourmettre** Soundscape en veille». The space was lost
in the Weblate translation itself (`cd99e018d`, 2026-08-21). Only iOS is
affected, because TalkBack writes its own hint.

**19 other languages** had the same defect: da, de, el, es, fa, fi, fr_CA, hi,
is, it, nb_NO, nl, pl, pt, pt_BR, ro, ru, sv, uk. All 20 now have the space,
uploaded and verified live on 2026-09-24.

These are correct *without* a space and must not be "fixed": ja «ダブルタップして%1$s»,
zh_Hans «双击以%1$s» and th «แตะสองครั้งเพื่อ%1$s» (scripts written without word
spaces), and ko «%1$s하려면…» and mr «%1$sसाठी…» (verb-final languages where a
particle attaches directly to the fragment, per commit `de8a39bab`). A naive
`[^ ]%1$s` grep counts all five of these, plus the three with `%1$s` at the
start, which is how an earlier draft of this entry arrived at "30".

Still open, for that language's reviewer: hi «दो बार टैप करें %1$s» has
the space now, but the hint grammar is still wrong (see `hi.md` HI-B1).
fi «Kaksoisnapauta %1$s» turned out to be **fine**: its hints are
translative infinitives that carry the "to" themselves (see `fi.md`).

### FR-R1 — Formal «vous» throughout (`unconfirmed`)

141 «vous», 78 «votre», 27 «Appuyez», and no «tu». This is a real convention,
not drift. It is also the usual default for French software (Google Maps,
iOS). Unlike Spanish (ES-R1), the English's friendly tone doesn't obviously
argue for «tu» in French, where «tu» from an app can read as over-familiar.
Keep it unless the reviewers disagree. See Q4.

### FR-T1 — Callout is split between «notification» and «annonce» (`unconfirmed`)

*«notification» is Microsoft's term (C14). «annonce» came later, from AI. fr_CA now uses «annonces» throughout (see `fr_CA.md`).*

Nouns in the UI and settings use «notification» («Autoriser les
notifications», «Gérer les notifications», `siri_type_callout`
«Notification»). Verbs and some descriptions use «annoncer»/«annonce»
(«Lieux à annoncer», «les annonces des points d'intérêt»). «annonce» has the
advantage of a natural verb («annoncer»), so switching to it would be close to
a mechanical noun swap rather than a rewrite (rule C3). It also avoids the
system-notification collision. It is still a term change touching dozens of
strings, so it needs a speaker's yes first. See Q1.

### FR-T2 — Waypoint and Landmark share «repère» (`unconfirmed`, likely a real problem)

*Both terms are Microsoft's (C14): the collision shipped in the original iOS app. **fr_CA has already moved to «point de cheminement»** (see `fr_CA.md`), so there is a live precedent to show the reviewers.*

Waypoint is «point de repère». Landmark is «repère», and in everyday French
«point de repère» *is* the word for a landmark. So «Lieux et repères»
(landmarks) and «Prochain point de repère» (next waypoint) sound like the same
concept to a French listener. «Créez des Marqueurs de vos destinations et
points de repère préférés sur un Itinéraire» (`markers_no_markers_hint_1`)
may use it in both senses in one sentence.

Candidates for Waypoint are «étape», which is what French route planners
generally use for intermediate stops, and «point de passage». Apply rule C1:
ask what the reviewers' own navigation apps call it, rather than picking from
our English glosses. 28 strings would move, plus the Siri route choices (FR-C1). See Q2.

### FR-T3 — Sleep/Snooze use button labels as mode names (`unconfirmed`)

The FAQ writes «le mode **Mettre en veille**» and «le mode **Désactiver
temporairement**», which means an infinitive button label is standing in for a
noun. «Désactivation temporaire» for Snooze is also long and doesn't say
*what* the mode does (it wakes when you move). One candidate pair is «mode
veille» / «veille automatique», but the pair has to stay clearly distinct.
See Q3.

### FR-S1 — English title case leaking mid-sentence (`unconfirmed`, cosmetic)

«Nouvel Itinéraire», «Démarrer la Balise sonore», «Créez un Itinéraire…
Marqueurs», «Arrêter l'Itinéraire». French doesn't capitalise common nouns
mid-sentence. About 45 occurrences, mostly Itinéraire/Marqueur/Balise.
Some are deliberate references to a screen or button name (*Marqueurs et
Itinéraires*), where keeping the label's form is defensible. Invisible to
TTS, so low priority. Sweep only after FR-T2 settles, since both touch the
same strings.

### FR-S2 — «Se déplaçant vers le nord» (`unconfirmed`)

`directions_traveling_*` and `directions_along_traveling_*` (16 strings) front
a present participle: a calque of "Traveling north". Something like «Vous
avancez vers le nord» or «Direction nord» may be what a French speaker
expects. Keep it distinct from `directions_heading_*` «En direction du nord»,
though, since it's a different string with a different trigger. These are
spoken often. See Q7.

### FR-C1 — Siri phrases are French, and live outside Weblate (`agreed`)

Unlike Polish, French *has* authored Siri phrases, in
`iosApp/iosApp/fr.lproj/AppShortcuts.strings` («Soundscape environs»,
«Soundscape itinéraire», «Soundscape balise»…). The file's own header says
they are a first pass that no native speaker wrote. The choices spoken after
each phrase («Point de repère suivant», «Désactiver le son de la balise»…)
come from `Localizable.xcstrings` (the `RouteCommand` etc. enums in
`SoundscapeIntents.swift`), which is also outside Weblate.

`help_text_assistant_how_ios` and `help_text_assistant_commands_ios` recite
both, and they currently match the `.strings` file. That creates two
couplings:

- A change to a phrase has to land in all three places at once, or the help
  text tells users to say something Siri won't recognise.
- **A FR-T2 change (Waypoint) reaches Siri as well.** Its route choices say
  «Point de repère suivant/précédent». Note also that the in-app menu item
  `menu_route_next_waypoint` says «Prochain point de repère», a different word
  order from the Siri choice. Align the two whenever FR-T2 is settled.

---

## Rejected

Nothing yet. When a reviewer turns something down, record it here **with the
evidence that made it attractive**. Otherwise the next pass reinstates it
(rule C8).

---

## Open questions for the first native-speaker round

These are the questions in `translations/review/fr.md`, in the same order.

1. **Callout: «notification» or «annonce»?** (FR-T1)
2. **Waypoint vs Landmark:** does «point de repère» for a route stop clash
   with «repères» for landmarks? What do your navigation apps call a route
   stop: «étape», «point de passage», something else? (FR-T2)
3. **Sleep / Snooze names** — what would you call the two modes? (FR-T3)
4. **«vous» or «tu»?** (FR-R1)
5. **Place names after «de»/«à»** — how bad is «près de Le Bon Marché»
   spoken aloud? Tolerable, or worth code changes? (FR-G2)
6. **«intersection» or «carrefour»?**
7. **«Se déplaçant vers le nord»** — natural, or how would you say it? (FR-S2)
8. **Dead-end way:** «Sentier menant à une impasse», «Sentier sans issue», or
   something else? (FR-G1)
9. **The four detail levels**, Détaillé / Équilibré / Discret / Silencieux:
   clear by ear?
10. **The Siri phrases** «Soundscape environs / itinéraire / balise / liste /
    détails / démarre l'itinéraire / arrête la balise»: would you say them
    naturally? (FR-C1)
11. **Anything that sounds translated** — free-form.

---

## Provenance

**2026-04 → 2026-09-24 — AI translation passes only.** Every commit to
`values-fr/strings.xml` is a Weblate-generated "Translated using Weblate
(French)" commit.

**2026-09-24 — corpus sweep, no speaker involved.** Built this file and the
question sheet from a read of the local `values-fr/strings.xml` (the same
content as Weblate, 1522 units). Found FR-G1 by checking `confect_name_to`'s
call site in `WayGenerator.kt`, and FR-B1 by checking
`talkback_double_tap_template`'s call site, then grepping every language for
the same defect. Nothing was uploaded in that pass.

**2026-09-24 — FR-B1 applied, across 20 languages.** Dave asked for the space
fix in every affected language. The first count said 30, but that included
false positives (see FR-B1), so the real total was 20. Fetched each live value
first (all matched the repo), uploaded one key per language with
`--skip-validate` (the revision path, see [[weblate-revising-existing-translations]]),
checked French round-tripped before doing the other 19, then re-fetched all 20
and confirmed each ends in « %1$s».
