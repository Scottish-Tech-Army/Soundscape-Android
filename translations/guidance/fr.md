# French (fr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units, 769 excluding `osm_*` (2026-09-24) |
| Last native-speaker input | 2026-09-30: one reviewer's answers to `questions-fr.md` Q1–Q11 (received 2026-09-28, applied 2026-09-30 without waiting for more reviewers, Dave's call) |
| Reporter platform | — |
| Register | Formal «vous», consistent across the corpus (`confirmed`, see FR-R1) |

Read with [`_common.md`](_common.md).

`fr_CA` is a separate Weblate language with its own file (`values-fr-rCA`).
About two thirds of its units are identical to `fr`. Nothing here applies to it
automatically: Canadian usage differs on exactly the kind of term this file
covers (courriel, magasiner, and the tu/vous balance). Give it its own
`fr_CA.md` once a Canadian speaker weighs in.

---

## Status of this file

**One native speaker has reviewed French** (2026-09-30, the questionnaire). Dave chose to act on that single reviewer rather than wait for more. Where the reviewer's suggestion conflicted with how the app works, Dave decided, and the rule says so. The paragraphs below are the pre-review state.

~~No native speaker has reviewed French.~~ *Correction (2026-09-24):* an
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

The questions for reviewers are in `docs/translation-questions/questions-fr.md`. Feedback will
cite its numbered questions (Q1…Q12), which match the Open questions list below.

---

## Glossary

| English | French | Status | Why |
|---|---|---|---|
| Callout | annonce | `agreed` | Reviewer (Q1): «annonce» is best for messages spoken aloud. Replaces Microsoft's «notification», which now means only real system notifications (`first_launch_permissions_notification`). Same gender, so the swap was mechanical, except «de notifications» → «d’annonces» |
| Callout (verb, "call out") | annoncer | `agreed` | Pairs with the noun (FR-T1) |
| Audio Beacon | balise sonore | `agreed`, always in full | The reviewer suggested «repère sonore» (Q10/Q11, hedged). Dave kept Microsoft's term, but bare «balise» is gone: to the reviewer it suggests a distress beacon. See FR-B2 |
| Marker | marqueur | `unconfirmed` | 84 occurrences, consistent |
| Waypoint | étape | `agreed` | Reviewer (Q2): their apps use «étape de parcours». Dave chose the short «étape» («Étape suivante»), which also ends the Landmark collision. Feminine: «l’étape suivante», «la première étape». See FR-T2 |
| Landmarks | repères / point de repère | `unconfirmed` | No longer collides with Waypoint. `osm_generic_landmark` «Point de repère» stays |
| Traveling (vehicle) | Vous vous déplacez vers… | `agreed` | See FR-S2 |
| Route | itinéraire | `unconfirmed` | Often capitalised mid-sentence. See FR-S1 |
| Intersection | croisement | `agreed` | Reviewer (Q6): more common and general than «carrefour», less formal than «intersection». Masculine: «le croisement suivant», «au croisement le plus proche». See FR-I1 |
| Junction (motorway, with ref) | sortie | `unconfirmed` | `directions_junction_with_ref` «Sortie %1$s». Correct for French motorways, where junctions are numbered exits |
| Sleep | pause (Mettre en pause / En pause / Reprendre maintenant); mode pause | `agreed` | See FR-T3 |
| Snooze | pause jusqu’au départ (En pause jusqu’au départ / Reprendre quand je pars) | `agreed` | See FR-T3 |
| Callout Detail | Détail des annonces | `agreed` | Moved with FR-T1 |
| Detailed / Balanced / Quiet / Silent | Détaillé / **Synthétique** / **Simplifié** / Silencieux | `agreed` | Reviewer (Q9): «Équilibré» doesn't work as a translation, and «Discret» is too close to «Silencieux». See FR-L1 |
| dead end | une impasse (in «%1$s menant à %2$s») | `agreed` | Reviewer (Q8): «Chemin menant à une impasse»; «sans issue» may sound anxiety-inducing. See FR-G1 |

---

## Rules

### FR-G1 — `confect_name_to` produces «Sentier à impasse» (`agreed`, wording decided 2026-09-30)

**2026-09-30:** reviewer (Q8): «Chemin menant à une impasse», *"« Sans issue » pourrait être un peu anxiogène."* `confect_name_to` → «%1$s menant {fr:à %2$s}», `_to_via` → «%1$s menant {fr:à %2$s} via %3$s», `confect_name_dead_end` → «une impasse». The article code leaves an indefinite article alone and still adds one to map names («Sentier menant à la rue de Rivoli»). `GrammarMarkersTest.frenchLeadingToKeepsAnIndefiniteArticle` covers both. The text below is the original diagnosis.

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

### FR-G2 — Articles and contractions around map names (`fixed` in code, 2026-09-25)

Templates put a raw map name after a preposition, so the app said «près de Le
Bon Marché», «le long de Rue de Rivoli», «à Boulevard Haussmann». 51 templates
(50 in fr_CA) now wrap the preposition and the name, «le long {fr:de %1$s}» (tagged with the language, because Spanish and Portuguese share «de» and «entre»), and
`resolveGrammarMarkers()` (C18) adds and contracts the article:
- a street type gets its article, lowercased: «de la rue de Rivoli», «au
  boulevard Haussmann», «de l’allée du Bois Ribot»;
- a place type gets its article and keeps its capital: «du Lycée Marie Curie»,
  «de l’École maternelle Brunet»;
- a name's own «Le» / «Les» contracts with de/à: «du Bon Marché», «aux Halles»,
  and stays whole otherwise («vers Le Havre»);
- «de» elides before a vowel: «d’Orléans».

Road words open 96% of the 73,000 street names in the Paris extract. Only
place-name placeholders are wrapped, never distances («à %2$s» stays). **New
templates must wrap map-name prepositions.** Italian, Spanish and Portuguese have
the same kind of contractions and got the same treatment on 2026-09-25 (ES-G2, IT-G1, PT-G1, PTBR-G1).

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

### FR-R1 — Formal «vous» throughout (`confirmed` 2026-09-30, reviewer Q4: «le vouvoiement est correct»)

141 «vous», 78 «votre», 27 «Appuyez», and no «tu». This is a real convention,
not drift. It is also the usual default for French software (Google Maps,
iOS). Unlike Spanish (ES-R1), the English's friendly tone doesn't obviously
argue for «tu» in French, where «tu» from an app can read as over-familiar.
Keep it unless the reviewers disagree. See Q4.

### FR-T1 — Callout is «annonce» (`agreed` 2026-09-30)

Reviewer (Q1): *"« annonce » est le meilleur choix dans le cas de messages parlés à voix haute."* 35 strings swept («notification(s)» → «annonce(s)»), plus the iOS Siri type name «Annonce» in `Localizable.xcstrings`. Keep «Notifications» only where it means a system notification (`first_launch_permissions_notification`). The text below is the pre-review analysis.

*«notification» is Microsoft's term (C14). «annonce» came later, from AI. fr_CA now uses «annonces» throughout (see `fr_CA.md`).*

Nouns in the UI and settings use «notification» («Autoriser les
notifications», «Gérer les notifications», `siri_type_callout`
«Notification»). Verbs and some descriptions use «annoncer»/«annonce»
(«Lieux à annoncer», «les annonces des points d'intérêt»). «annonce» has the
advantage of a natural verb («annoncer»), so switching to it would be close to
a mechanical noun swap rather than a rewrite (rule C3). It also avoids the
system-notification collision. It is still a term change touching dozens of
strings, so it needs a speaker's yes first. See Q1.

### FR-T2 — Waypoint is «étape» (`agreed` 2026-09-30)

Reviewer (Q2): *"Mes applications utilisent majoritairement « étape de parcours »"*, with «arrêts en chemin» as a more informal option. Dave chose the short «étape», since it is spoken often and «étape de parcours» is long. 28 strings swept, rewriting agreement («l’étape suivante», «à la première étape», «L’étape %1$s sera retirée»). `route_waypoint_progress` became «Itinéraire %1$s, étape %2$s sur %3$s». The in-app «Prochain point de repère» and Siri's «Point de repère suivant» are now both «Étape suivante» (FR-C1). Landmark keeps «repères». The text below is the pre-review analysis.

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

### FR-T3 — Sleep/Snooze are «pause» / «pause jusqu’au départ» (`agreed` 2026-09-30)

Reviewer (Q3): drop «veille», *"puisqu’on parle d’application et non de l’appareil électronique"*. They proposed Sleep «Désactiver» and Snooze «Suspendre jusqu’au prochain lieu» (*"il est plus logique d’introduire une temporalité"*). Dave adapted both. «Désactiver» sounds like any settings toggle, and Snooze wakes when you **leave**, not when you reach a place. So:

| | Button / status | Mode name in prose |
|---|---|---|
| Sleep | Mettre en pause / En pause / Reprendre maintenant | le mode pause, «mettre Soundscape en pause» |
| Snooze | Reprendre quand je pars / En pause jusqu’au départ | «en pause jusqu’au départ» |

The FAQ's «le mode Mettre en veille» (a button label used as a noun) is gone as well. 18 strings swept. A possible confusion: the headset's ⏯ Lecture/Pause button toggles the beacon, not Sleep. The text below is the pre-review analysis.

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

### FR-S2 — «Vous vous déplacez vers le nord» (`agreed` 2026-09-30)

The reviewer agreed that the participle sounds like "a descriptive translation" and suggested «Vous marchez vers le nord». `directions_traveling_*` is only spoken **in a vehicle**, where the user is a passenger ([[travel-mode-user-is-passenger]]), so Dave chose «Vous vous déplacez…» (16 strings). The walking strings `directions_heading_*` stay «En direction du nord». The text below is the pre-review analysis.

`directions_traveling_*` and `directions_along_traveling_*` (16 strings) front
a present participle: a calque of "Traveling north". Something like «Vous
avancez vers le nord» or «Direction nord» may be what a French speaker
expects. Keep it distinct from `directions_heading_*` «En direction du nord»,
though, since it's a different string with a different trigger. These are
spoken often. See Q7.

### FR-L1 — Detail levels: Détaillé / Synthétique / Simplifié / Silencieux (`agreed` 2026-09-30)

Reviewer (Q9): *"Équilibré ne fonctionne pas en termes de traduction"*, and «Discret» vs «Silencieux» may not be distinct enough. 8 strings swept. The Polish reviewer independently renamed Quiet «Uproszczony» ("simplified"), and the Dutch reviewer also rejected Balanced/Quiet. Three languages have now had trouble with them, which suggests the English names themselves are the problem (not changed).

**Gap found, not fixed:** the iOS Siri choices for detail level (`CalloutDetailLevel.caseDisplayRepresentations` in `SoundscapeIntents.swift`) have **no French entries** in `Localizable.xcstrings`, so Siri only knows the English names. `help_text_assistant_commands_ios` tells French users to say «Simplifié». This affects every language, not just French.

### FR-B2 — Always «balise sonore», never bare «balise» (`agreed` 2026-09-30)

Reviewer (Q10/Q11): «balise» alone is *"pas le plus commun en référence à un son. Si vous faites référence à une balise de détresse c’est plus courant"*. They suggested «repère sonore». Dave kept Microsoft's «balise sonore» (Rejected, below), with the full form everywhere. 54 strings swept, and «balise audible/audio» were normalised too. The exceptions: `osm_beacon` «Balise» (a physical map beacon, which is the everyday sense), and the Siri group word «Soundscape balise» (spoken, and must match `AppShortcuts.strings`). The stop phrase is now «Soundscape arrête la balise sonore», changed in `fr.lproj/AppShortcuts.strings` and the Siri help texts together.

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

- **«repère sonore» for Beacon** (reviewer Q10/Q11, 2026-09-30). Attractive because the reviewer finds «balise» unusual for a sound. Rejected by Dave: it would bring back the collision with Landmark «repères» that FR-T2 had just removed, and long-time users know Microsoft's «balise sonore». FR-B2 addresses the reviewer's actual objection instead.
- **«étape de parcours» in full, and «arrêt en chemin»** for Waypoint. The first is the reviewer's own term, but it's long for something spoken often. «arrêt» suggests stopping.
- **«Vous marchez vers le nord»** for `directions_traveling_*`: those strings are only spoken in a vehicle.
- **Sleep «Désactiver» / Snooze «Suspendre jusqu’au prochain lieu»**: see FR-T3.
- **«Sentier sans issue»** for dead end: *"pourrait être un peu anxiogène"* (reviewer, Q8).
- **«carrefour»** (Q6): less general than «croisement».

When a reviewer turns something down, record it here **with the evidence that made it attractive**. Otherwise the next pass reinstates it (rule C8).

---

## Questionnaire round 1 — answered 2026-09-30

Q1 → FR-T1, Q2 → FR-T2, Q3 → FR-T3, Q4 → FR-R1, Q5 (articles before names) → FR-G2 `confirmed` (*"Ça sonne juste"*), Q6 → FR-I1, Q7 → FR-S2, Q8 → FR-G1, Q9 → FR-L1, Q10/Q11 → FR-B2. Q12 had no answer.

### FR-I1 — Intersection is «croisement» (`agreed` 2026-09-30)

Reviewer (Q6): *"un mot plus commun et généralisable que carrefour et moins formel qu’intersection."* 24 strings swept, including the label «Rues et croisements». The gender change (f → m) was rewritten throughout: «le croisement suivant», «au croisement le plus proche», «d’un croisement», «jusqu’au croisement suivant».

## Open questions for round 2

1. «Étape suivante» / «Ajouter des étapes»: clear enough without «de parcours»?
2. «Mettre en pause» / «Reprendre quand je pars» / «En pause jusqu’au départ»: do they say what the two modes do?
3. «Balise sonore» said in full everywhere: too heavy in long help texts?

## fr_CA is derived from this file

Since 2026-09-25, Canadian French is built from our French plus a Canadian layer (see `fr_CA.md`). Any change decided here (FR-T1, FR-T2, FR-G1) should be carried to fr_CA in the same pass. **Not yet done for the 2026-09-30 decisions**: fr_CA already had its own «annonces» and «point de cheminement», so each decision needs checking against `fr_CA.md` first.

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

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` «Désactivation temporaire» → Microsoft's «Désactivé temporairement». FR-T3 (the mode names in the FAQ) is unaffected and still open.

**2026-09-25 — Microsoft drift pass (C14): declined by Dave.** The sort proposed restoring 10 drifted strings to Microsoft's wording. Dave chose not to take any of them, so French keeps its current wording and nothing was uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (27 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 20 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Another 6 strings that Weblate re-marked as translated when the file was imported were fixed through the API. The French no-break spaces are restored at build time (`composeResourcesForBuild`). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_ice_cream` → «Marchand de glaces» (it had been «Glacier», same as the glacier), `osm_dock` → «Bassin portuaire» (it had been «Quai», same as platform), `osm_drugstore` → «Droguerie». FR-T2 (Waypoint = Landmark «Point de repère») is still open. Uploaded live.

**2026-09-29 — Full review of all 1586 units, 13 fixes uploaded.** The Weblate log shows no human French edits (JJ's entries are English source changes), and the open questions above were not re-flagged. Five of the errors were in Microsoft's own text. They were fixed because each was a real error (C14), not drift: `help_text_around_me_what` «quatre points cardinaux» → «quatre quadrants» (ahead/right/behind/left, as the button hint already said); `help_text_my_location_what` «la direction que vous prenez» → «dans laquelle vous êtes tourné» (facing); `help_text_destination_beacons_when` «votre recherche d'itinéraire» → «vos compétences d'orientation» (wayfinding skills); `faq_when_to_use_soundscape_answer` «explorer les axes» → «explorer les rues commerçantes»; `faq_why_not_every_business_question` → «chaque commerce devant lequel je passe». `callouts_panel_title` «Écoutez mon environnement» → «Écouter mon environnement» (imperative clashed with «mon»; Dave took it). The other seven were:
- `osm_subway_named` → «Station de métro %1$s» (English word order).
- `osm_entrance_named_with_destination` and `osm_entrance_with_destination`: the hard-coded «du %1$s» → «{fr:de %1$s}» (C18).
- `help_text_markers_content_3`: «un appel spatial» → «une notification spatiale».
- `help_text_creating_markers_content_2`: «sera appelée» → «annoncée», plus *Terminé*.
- `faq_turn_beacon_back_on_answer`: *Réactiver le son de la balise*.
- `offline_maps_free_space`: «libres».

Uploaded with `--skip-validate` and re-fetched: all 13 match, except that Weblate turned the spaces before «:» and «?» into no-break spaces. `terms_of_use_message` keeps « Conditions d'utilisation » because the Terms screen renders plain text, where `*…*` would show as asterisks.


**2026-09-30 — first reviewer's questionnaire applied.** Answers received 2026-09-28 and at first held while more reviewers were asked; Dave then chose to proceed on this one. Swept into 154 units in `/tmp/weblate-review/fr-findings.json`. The Siri files `fr.lproj/AppShortcuts.strings` and `Localizable.xcstrings` (French only, 10 values) were changed to match. A new `GrammarMarkersTest` case covers «menant à une impasse». All 154 were uploaded with `--skip-validate` the same day and verified live. The 37 that differ only have the U+00A0/U+202F that Weblate's French autofix adds before : and ?, which is correct.
