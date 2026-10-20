# French (fr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units, 769 excluding `osm_*` (2026-09-24) |
| Last native-speaker input | 2026-10-20: JMB's round-4 answers (Q1–Q3) plus a new rule on compass directions (FR-D1). 2026-10-19: a **second reviewer, JMB**, answered round 3 and commented on every round-1/2 decision. Dave trusts JMB's judgement well above the first reviewer's: where they conflict, JMB wins. Before that, 2026-10-01: the first reviewer's round-2 answers (Q1–Q5). Before that, 2026-09-30: one reviewer's answers to `questions-fr.md` Q1–Q11 (received 2026-09-28, applied 2026-09-30 without waiting for more reviewers, Dave's call) |
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

**Two native speakers have reviewed French.** JMB (2026-10-19) outranks the first reviewer, by Dave's call: JMB reversed «croisement», «Minimal», the Snooze status label and «impasse» (finally «sans issue», round 4), chose «Placer la balise sonore» (Dave dropped JMB's «ici»), and confirmed «annonce», «balise sonore» and «Vous vous déplacez». Each decision below says which reviewer settled it.

**The first native speaker reviewed French** on 2026-09-30, through the questionnaire. Dave chose to act on that single reviewer rather than wait for more. Where the reviewer's suggestion conflicted with how the app works, Dave decided, and the rule says so. The paragraphs below are the pre-review state.

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
cite its numbered questions (round 5 since 2026-10-20, Q1…Q3 with "anything else" last), which match the round-5 list below.

---

## Glossary

| English | French | Status | Why |
|---|---|---|---|
| Callout | annonce | `confirmed` (JMB, 2026-10-19: *"Notification is more for written announcement whereas 'Annonce' is more of a verbal announcement"*) | Reviewer (Q1): «annonce» is best for messages spoken aloud. Replaces Microsoft's «notification», which now means only real system notifications (`first_launch_permissions_notification`). Same gender, so the swap was mechanical, except «de notifications» → «d’annonces» |
| Callout (verb, "call out") | annoncer | `agreed` | Pairs with the noun (FR-T1) |
| Audio Beacon | balise sonore; «balise» after the first mention and in action labels | `confirmed` (JMB, round 3 Q1: *"Balise sonore. Definitely."*) | Full form where nothing else says it's a sound (titles, status, first mention in a text); bare «balise» with a verb («Désactiver le son de la balise») or once named. Never «balise audio», which reads as an HTML/programming tag (JMB). See FR-B2 |
| Start Audio Beacon | Placer la balise sonore | `agreed` (JMB, round 4 Q1, 2026-10-20; «ici» dropped by Dave) | Replaces «Démarrer la balise». The audio menu reuses the label, where «ici» would point nowhere (FR-B3). The verb plus the full «balise sonore» is JMB's own form, an exception to FR-B2's bare «balise» after a verb. See FR-B3. Unmute is «Réactiver le son de la balise» (`confirmed`, JMB) |
| Marker | marqueur | `unconfirmed` | 84 occurrences, consistent |
| Waypoint | étape | `confirmed` (round 2, Q1: «cela se suffit à lui même») | Reviewer (Q2): their apps use «étape de parcours». Dave chose the short «étape» («Étape suivante»), which also ends the Landmark collision. Feminine: «l’étape suivante», «la première étape». See FR-T2 |
| Landmarks | repères / point de repère | `unconfirmed` | No longer collides with Waypoint. `osm_generic_landmark` «Point de repère» stays |
| Traveling (vehicle) | Vous vous déplacez vers… | `confirmed` (JMB, 2026-10-19) | See FR-S2 |
| Route | itinéraire | `unconfirmed` | Often capitalised mid-sentence. See FR-S1 |
| Intersection (and Junction, as in «Rues et intersections») | intersection | `agreed` (JMB, 2026-10-19, reverses round 1) | The general term, the one the French highway code uses: any place where roads join or cross, a T junction included. «croisement» is only an X-shaped crossing. Feminine: «l’intersection suivante», «à l’intersection la plus proche», «d’une intersection». See FR-I1 |
| Junction (motorway, with ref) | sortie | `unconfirmed` | `directions_junction_with_ref` «Sortie %1$s». Correct for French motorways, where junctions are numbered exits |
| Sleep | pause (Mettre en pause / En pause / Reprendre); mode pause | `confirmed` (round 2 Q2; JMB 2026-10-19 dropped «maintenant») | See FR-T3 |
| Snooze | pause à l’arrêt (En pause à l’arrêt / Reprendre au déplacement) | `agreed` (JMB, 2026-10-19; button kept in round 4 Q3) | See FR-T3. «en pause jusqu’à votre départ» stays acceptable in prose |
| Callout Detail | Détail des annonces | `agreed` | Moved with FR-T1 |
| Detailed / Simplified / Essential / Silent | Détaillé / Simplifié / Essentiel / Silencieux | `agreed` (JMB, 2026-10-19, reverses round 2 Q4) | «Minimal» reads as "minimalist"; «Essentiel» gives «les détails essentiels». See FR-L1 |
| (superseded) | Détaillé / Synthétique / Simplifié / Silencieux | — | Reviewer (Q9): «Équilibré» doesn't work as a translation, and «Discret» is too close to «Silencieux». See FR-L1 |
| dead end | sans issue («Chemin sans issue») | `agreed` (JMB, round 4 Q2, 2026-10-20) | JMB's answer to «menant à un cul-de-sac» vs «en cul-de-sac». Replaces «une impasse». See FR-G1 |
| North, north-west… (direction) | nord, nord-ouest… (lowercase) | `agreed` (JMB, 2026-10-20) | See FR-D1 |

---

## Rules

### FR-G1 — `confect_name_to` produces «Sentier à impasse» (`agreed`, wording «sans issue» decided 2026-10-20 by JMB)

**2026-10-20, JMB (round 4 Q2):** asked «Sentier menant à un cul-de-sac» or «Sentier en cul-de-sac», JMB answered *«Chemin sans issue»*. `osm_path` is «Chemin», so that's exactly `%1$s` + «sans issue»: `confect_name_to_dead_end` → «%1$s sans issue», `_via` → «%1$s sans issue via %2$s» (our word order). The `{fr:à …}` marker goes, since no article is needed. This reverses JMB's own «cul-de-sac» and the first reviewer's «anxiogène» objection to «sans issue»: JMB outranks. `%1$s` can also be a road's own name («Ladywood sans issue»), which reads like the road signs. `GrammarMarkersTest.frenchLeadingToKeepsAnIndefiniteArticle` tests the marker code with a literal string, so it still passes; update its comment, which cites the old wording.

**2026-10-19, JMB:** *"No, Voie sans issue in French is much better known as: 'un cul-de-sac'. That is a typical French expression."* The templates are now `confect_name_to_dead_end` and `_via` («%1$s menant {fr:à une impasse}»), with the article in the template. Swap to «{fr:à un cul-de-sac}». The article code leaves «un» alone, as it does «une». The structure «menant à» stays, and «Sentier en cul-de-sac» is an open question. `GrammarMarkersTest.frenchLeadingToKeepsAnIndefiniteArticle` should then assert the new wording.

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

### FR-T3 — Sleep/Snooze are «pause» / «pause à l’arrêt» (Sleep `confirmed`, Snooze `agreed` from JMB, 2026-10-19)

**2026-10-20, JMB (round 4 Q3):** asked whether the noun phrase «Reprise dès le mouvement» works next to the verb buttons, JMB chose **«Reprendre au déplacement»**, the existing label. So `sleep_wake_on_leave`, `faq_battery_impact_answer`, `help_text_automatic_callouts_how_2` and the release notes keep it. The rest of the 2026-10-19 change stands: «Reprendre» (not «Reprendre maintenant») and «En pause à l’arrêt». In the table below, read «Reprendre au déplacement» for «Reprise dès le mouvement».

**2026-10-19, JMB:** *"'Mettre en pause' or simply 'Pause'. 'Reprendre' is better than 'Reprendre maintenant'. The now (maintenant) is already implied. « En pause jusqu’au départ » is ok but it's better to say « En pause jusqu’à votre départ ». Or simply « En pause à l'arrêt », which sounds better and means paused while you are stopped. « Reprise dès le mouvement » is better than « Reprendre au déplacement ». It means it restarts as soon as you move."* So:

| | Button / status | Mode name in prose |
|---|---|---|
| Sleep | Mettre en pause / En pause / **Reprendre** | le mode pause |
| Snooze | **Reprise dès le mouvement** / **En pause à l’arrêt** | «la pause à l’arrêt»; «en pause jusqu’à votre départ» is fine in a sentence |

«Mettre en pause» stays: JMB accepted it, and «Pause» alone collides with the headset's ⏯ Lecture/Pause. «Reprise dès le mouvement» is a noun phrase on a button, unlike the verb labels around it. JMB chose it knowingly over the verb form. 6 strings, plus the release notes. The round-2 text follows.

**Round 2 (Q2):** *"“Mettre en pause” et “en pause” OK et j’aime le fait que les notions soient complémentaires. « Reprendre quand je pars » est très wordy et non naturel en français, on dirait plutôt « Reprendre au déplacement » qui est fluide et qui complémente bien « Reprendre maintenant »."* So `sleep_wake_on_leave` → «Reprendre au déplacement», and the FAQ that quotes it (`faq_snooze_mode_battery_answer`). «En pause jusqu’au départ» stays; the reviewer didn't comment on it.

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

### FR-S2 — «Vous vous déplacez vers le nord» (`confirmed` 2026-10-19, JMB: *"Yes agreed."*)

The reviewer agreed that the participle sounds like "a descriptive translation" and suggested «Vous marchez vers le nord». `directions_traveling_*` is only spoken **in a vehicle**, where the user is a passenger ([[travel-mode-user-is-passenger]]), so Dave chose «Vous vous déplacez…» (16 strings). The walking strings `directions_heading_*` stay «En direction du nord». The text below is the pre-review analysis.

`directions_traveling_*` and `directions_along_traveling_*` (16 strings) front
a present participle: a calque of "Traveling north". Something like «Vous
avancez vers le nord» or «Direction nord» may be what a French speaker
expects. Keep it distinct from `directions_heading_*` «En direction du nord»,
though, since it's a different string with a different trigger. These are
spoken often. See Q7.

### FR-L1 — Detail levels: Détaillé / Simplifié / Essentiel / Silencieux (`agreed` 2026-10-19, JMB)

**2026-10-19, JMB:** *"I am not sure why you opted for 'Minimal' for the Essential option. Minimal in French is mostly used in the context of being minimalist. 'Essentiel' is definitely a better word in this context. It provides: les détails essentiels."* This reverses round 2. Dave ranks JMB above the first reviewer, whose worry was that «Essentiel» doesn't rank against «Simplifié»; that worry is now C22's (the English names), not French's. 7 strings. Regenerate the Siri choice with `scripts/generate-ios-siri-strings.py` (never hand-edit `Localizable.xcstrings`), and update the release notes. The round-2 text follows.

**Round 2 (Q4):** *"Beaucoup mieux concernant le niveau de détail, moins confus. Mon seul doute concerne « essentiel » qui ne se distingue pas facilement de simplifié (on ne sait pas lequel donne plus d’information). Vous pourriez remplacer essentiel par « minimal » par exemple, qui fait clairement référence au niveau le plus bas avant « Silencieux »."* The doubt is firm and «Minimal» ranks itself, so it's taken. 7 strings, including the help texts and voice/Siri lists that recite the levels. The Siri choice in `Localizable.xcstrings` comes from `callouts_verbosity_level_quiet` through `scripts/generate-ios-siri-strings.py`: regenerate, don't hand-edit.

*Possible source problem (C22):* the objection, that you can't tell whether Simplified or Essential says more, may apply to the English names too. Not changed; raise it if another language reports the same.

The earlier history follows.

**Later on 2026-09-30:** the English levels were renamed Simplified / Essential (C22), and Dave chose literal names. The reviewer's «Simplifié» moves up to level 2, «Synthétique» is dropped, and level 3 becomes «Essentiel». This goes against the answer below, so it's round-2 question 4.

Reviewer (Q9): *"Équilibré ne fonctionne pas en termes de traduction"*, and «Discret» vs «Silencieux» may not be distinct enough. 8 strings swept. The Polish reviewer independently renamed Quiet «Uproszczony» ("simplified"), and the Dutch reviewer also rejected Balanced/Quiet. Three languages have now had trouble with them, which suggests the English names themselves are the problem (not changed).

**Gap found, not fixed:** the iOS Siri choices for detail level (`CalloutDetailLevel.caseDisplayRepresentations` in `SoundscapeIntents.swift`) have **no French entries** in `Localizable.xcstrings`, so Siri only knows the English names. `help_text_assistant_commands_ios` tells French users to say «Simplifié». This affects every language, not just French.

### FR-B2 — «balise sonore» first, then «balise» (`agreed` 2026-10-01, relaxes the 2026-09-30 rule)

**Round 2 (Q3):** *"Le mot « balise » seul peut fonctionner si vous ne voulez pas surcharger de texte, mais faire référence au moins une fois à « balise audio » en introduction pour éviter la confusion avec un objet physique est nécessaire. NB: Associer le mot balise à une action comme « désactiver le son de la balise » ou « suivre la balise » efface toute confusion."* The rule is now:
- **Full «balise sonore»** where nothing else says it is a sound: the setting title «Balise sonore», status lines («Aucune balise sonore active», «La balise sonore se trouve…»), FAQ questions with no verb, and the first mention in any help/FAQ text.
- **Bare «balise»** with an action verb (buttons, hints, voice commands: «Désactiver le son de la balise», «Démarrer la balise») and on every later mention in the same text. A help text quoting a button uses the button's new label.
- The Siri phrase «Soundscape arrête la balise sonore» (`fr.lproj/AppShortcuts.strings`) is left alone: changing it moves three files together (FR-C1), and nothing asked for it.

The reviewer wrote «balise audio» for the introduction; we read that as our «balise sonore». **Settled 2026-10-19 by JMB:** «balise sonore», *"Definitely. A Balise sonore usually refers to a physical accessibility device for blind or visually impaired people whereas a 'Balise audio' is an audio tag used in programming such as a tag in HTML."* JMB also approved «Désactiver le son de la balise» and gave «Réactiver le son de la balise» for turning it back on, which is what Unmute already says. JMB rejected «Démarrer la balise»: see FR-B3. 34 strings, all mechanical. The 2026-09-30 text follows.

Reviewer (Q10/Q11): «balise» alone is *"pas le plus commun en référence à un son. Si vous faites référence à une balise de détresse c’est plus courant"*. They suggested «repère sonore». Dave kept Microsoft's «balise sonore» (Rejected, below), with the full form everywhere. 54 strings swept, and «balise audible/audio» were normalised too. The exceptions: `osm_beacon` «Balise» (a physical map beacon, which is the everyday sense), and the Siri group word «Soundscape balise» (spoken, and must match `AppShortcuts.strings`). The stop phrase is now «Soundscape arrête la balise sonore», changed in `fr.lproj/AppShortcuts.strings` and the Siri help texts together.

### FR-B3 — "Start Audio Beacon" is «Placer la balise sonore» (`agreed`, JMB round 4, 2026-10-20)

**2026-10-20, JMB (round 4 Q1):** *«Placer la balise sonore ici»*. Derived forms, ours, `medium`:
- `location_detail_action_beacon_from_markers` «Placer la balise sonore sur ce marqueur»;
- `voice_cmd_start_beacon_at_marker_with_name` «Placer la balise sonore sur %1$s»;
- `siri_title_start_beacon` «Placer la balise sonore» (no «ici»: the shortcut then asks for a marker). Regenerate `Localizable.xcstrings`. The Siri phrase «Soundscape balise» stays (FR-C1);
- `first_launch_prompt_message` «choisissez une destination proche et placez-y la balise sonore» (was «démarrez la balise sonore»).
The four help/FAQ/tour texts quote the label as *Placer la balise sonore*.

**Dave dropped «ici» (2026-10-20)**, so the French label is «Placer la balise sonore». The reason: the audio menu (`AudioMenu.kt`) reuses `location_detail_action_beacon` as the name of the headset-button group that then lists your markers. There, «Placer la balise sonore ici» is read out before any place is chosen, so «ici» points nowhere. Without «ici» the label works in both places, so the code and the English stay as they are. Any other language that puts "here" into this label has the same problem.

The 2026-10-19 text follows.

JMB: *"« Désactiver le son de la balise »: this is good but « Démarrer la balise » is not. This translates as 'Start the beacon', not as start the sound of the beacon. In this context, if you are activating the sound for the very first time use 'Activer le son de la balise', and if you want to turn the sound back on after it was turned off, you'd say 'Réactiver le son de la balise'."*

Our questionnaire listed «Démarrer la balise» next to the mute label as if it were a sound toggle. In the app, though, Start Audio Beacon **places a new beacon on a chosen place** (Location Details, the marker voice command, Siri «Start Beacon»). It doesn't unmute anything. So «Activer le son de la balise» would mislead, since it says the same as Unmute. JMB's objection still stands: «démarrer» suits a machine, not a sound. Candidates to put to JMB with the real context: «Activer la balise sonore», «Lancer la balise sonore», «Placer une balise sonore ici». Nothing is swept until JMB answers. 8 strings depend on it:
- labels: `location_detail_action_beacon`, `location_detail_action_beacon_from_markers`, `voice_cmd_start_beacon_at_marker_with_name`, `siri_title_start_beacon`;
- texts that quote the label: `help_text_destination_beacons_how_1`, `help_text_remote_control_audio_menu`, `faq_beacon_on_address_answer`, `tour_start_beacon`. That last one says «Démarrer la balise sonore», which doesn't match the label: resync it whatever the decision;
- the Siri `Start Beacon` choice in `Localizable.xcstrings` and the phrase «Soundscape balise» (FR-C1 coupling).

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
- ~~**«Sentier sans issue»** for dead end: *"pourrait être un peu anxiogène"* (reviewer, Q8).~~ Reinstated by JMB, round 4 (FR-G1).
- **«carrefour»** (Q6): narrower than «intersection».
- **«croisement»** for Intersection (first reviewer Q6, 2026-09-30, used 2026-09-30 → 2026-10-19). It was attractive as the less formal word. JMB rejected it: it means only an X-shaped crossing, while the app also calls out T junctions. See FR-I1.
- **«Minimal»** for Essential (first reviewer round 2 Q4). It was attractive because it ranks itself below «Simplifié». JMB rejected it: it reads as "minimalist". See FR-L1.
- **«Reprendre maintenant», «En pause jusqu’au départ»** for Sleep/Snooze (round 2). JMB said «maintenant» is implied, and «En pause à l’arrêt» is smoother. See FR-T3.
- **«Reprise dès le mouvement»** for Wake On Leave (JMB, 2026-10-19). JMB went back to «Reprendre au déplacement» in round 4, a verb like the buttons beside it.
- **«une impasse»** for dead end (first reviewer Q8), then **«un cul-de-sac»** (JMB, 2026-10-19). JMB settled on «sans issue» in round 4.
- **«Démarrer la balise»** for Start Audio Beacon (Microsoft/AI): «démarrer» suits a machine, and reads as starting the beacon, not placing it (JMB). See FR-B3.
- **Capitalised compass points** («Nord-Est»): see FR-D1.
- **«balise audio»**: JMB says it reads as an HTML/programming tag.
- **«Activer le son de la balise»** for Start Audio Beacon (JMB's own suggestion, made without seeing the screen): it would say the same as Unmute. See FR-B3.

When a reviewer turns something down, record it here **with the evidence that made it attractive**. Otherwise the next pass reinstates it (rule C8).

---

## Questionnaire round 1 — answered 2026-09-30

Q1 → FR-T1, Q2 → FR-T2, Q3 → FR-T3, Q4 → FR-R1, Q5 (articles before names) → FR-G2 `confirmed` (*"Ça sonne juste"*), Q6 → FR-I1, Q7 → FR-S2, Q8 → FR-G1, Q9 → FR-L1, Q10/Q11 → FR-B2. Q12 had no answer.

### FR-I1 — Intersection is «intersection» (`agreed` 2026-10-19, JMB; reverses 2026-09-30)

JMB: *"I would revert that. 'Intersection' is the most general and technical term and is what is used in the French highway code. It describes a place where two or more roadways join or cross, so it would describe a T junction as well, whereas croisement is very specific: it describes a place where two roads cross each other, typically in the shape of an X, a cross (une croix), hence the derived word croisement."* The app calls out T junctions and forks, so «intersection» is correct and «croisement» actually misdescribes them. 22 strings, with the gender change undone (m → f): «l’intersection suivante», «à l’intersection la plus proche», «d’une intersection», «de l’intersection», «Rues et intersections». «Une rue qui croise la vôtre» (verb) in `faq_road_names_answer` stays: there it is literally a crossing. Also `docs/release-notes.fr.md` (9 lines). The 2026-09-30 text follows.

Reviewer (Q6): *"un mot plus commun et généralisable que carrefour et moins formel qu’intersection."* 24 strings swept, including the label «Rues et croisements». The gender change (f → m) was rewritten throughout: «le croisement suivant», «au croisement le plus proche», «d’un croisement», «jusqu’au croisement suivant».

### FR-S3 — No «ça sonne comme» for "it sounds like" (`agreed` 2026-10-01)

**Round 2 (Q5):** *"en français on ne dit pas « ça sonne comme » suivi d’une liste d’options ou réglages, contrairement à l’anglais et l’espagnol. On dirait plutôt « ce que ça donne » ou « le rendu sonore est … »."* No app string uses «sonner» (checked 2026-10-01). The hit was our own questionnaire: the fr field «Comment ça sonne aujourd’hui :» is now «Ce que ça donne aujourd’hui :», and the fr_CA intro changed the same way (2026-10-01). Applies to any future French user guide.

## Questionnaire round 2 — answered 2026-10-01

Q1 → FR-T2 `confirmed`, Q2 → FR-T3, Q3 → FR-B2 relaxed, Q4 → FR-L1 «Minimal», Q5 → FR-S3.

## Questionnaire round 3 — answered 2026-10-19 by JMB

Q1 → FR-B2 «balise sonore» `confirmed`; Q2 → FR-L1 «Essentiel» (reverses «Minimal»); Q3 (anything else) → comments on every item in «Ce que nous avons décidé»: FR-T1 and FR-S2 `confirmed`, FR-I1 reverted, FR-T3 relabelled, FR-G1 «cul-de-sac», FR-B3 opened.

JMB couldn't parse our sheet's «Étape … sans « de parcours »»: they read it as possibly «sens de parcours» (direction of travel). We meant «étape», without the words «de parcours». They raised no objection to «étape», so FR-T2 stays `confirmed`. The sheet's wording was at fault: say it positively next time («Étape», as in «Étape suivante»).

## Open questions for round 5

The sheet was rewritten 2026-10-20 with these, and its «Ce que nous avons décidé» brought up to date.

1. FR-B3: are our derived forms natural? «Placer la balise sonore sur ce marqueur», «Placer la balise sonore sur %1$s» (voice command), and «choisissez une destination proche et placez-y la balise sonore» (`first_launch_prompt_message`).
2. FR-G1: the long form «%1$s sans issue via %2$s» comes out as «Chemin sans issue via Marches» (`%2$s` is `osm_steps`/`osm_bridge`/`osm_tunnel`, capitalised). Understandable, or «Chemin sans issue, par des marches» or similar? The capital also affects `confect_name_via` and `_to_via`.
3. Anything else.

## Questionnaire round 4 — answered 2026-10-20 by JMB

Q1 → FR-B3 «Placer la balise sonore ici», used without «ici»; Q2 → FR-G1 «Chemin sans issue»; Q3 → FR-T3 «Reprendre au déplacement». Unprompted: FR-D1, compass directions in lowercase.

The round-4 questions as asked:

1. FR-B3: on a place's details screen, the button *Start Audio Beacon* puts a new audio beacon on that place. It doesn't unmute one: that's «Réactiver le son de la balise». Which label: «Activer la balise sonore», «Lancer la balise sonore», «Placer une balise sonore ici», or another?
2. FR-G1: «Sentier menant à un cul-de-sac», or «Sentier en cul-de-sac»? The second needs a template change.
3. FR-T3: the Wake On Leave button «Reprise dès le mouvement» is a noun phrase, next to verb buttons «Mettre en pause» and «Reprendre». Is that fine on a button, or would «Reprendre dès le mouvement» be better?

## FR-D1 — Compass directions are lowercase (`agreed`, JMB 2026-10-20)

JMB: *"In French, we don't capitalise North, South etc when we mean a direction so you'd use: « nord », « nord-ouest »."* The eight `directions_cardinal_*` strings said «Nord», «Nord-Est»… They are never shown alone: `GeoEngineHelpers.kt` appends them after a comma («120 m, nord-est») and street preview speaks «Aller <rue> nord». All eight → lowercase, with a lowercase second part («nord-est», «sud-ouest»). The `_bound`, `directions_heading_*` and `directions_traveling_*` strings were already lowercase, the abbreviations (N, NE, SO…) stay capitals, and proper nouns («Afrique du Nord», «pôle Nord») keep the capital. fr_CA has the same capitals («Nord», «Est», «Nord-ouest»), and this is not a regional difference, so it carries over.

## fr_CA is derived from this file

Since 2026-09-25, Canadian French is built from our French plus a Canadian layer (see `fr_CA.md`). Any change decided here (FR-T1, FR-T2, FR-G1) should be carried to fr_CA in the same pass. **Not yet done for the 2026-09-30 decisions**: fr_CA already had its own «annonces» and «point de cheminement», so each decision needs checking against `fr_CA.md` first.

**JMB's 2026-10-19 decisions:** fr_CA copied two of the reversed choices, «croisement» (22 lines) and «Minimal» (7). «Intersection» is also the standard term in Quebec (the Code de la sécurité routière), so FR-I1 very likely carries over. «Essentiel», «cul-de-sac» and the Snooze labels need a Canadian ear, so record them in `fr_CA.md` before sweeping.

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


**2026-09-30 — first reviewer's questionnaire applied.** Answers received 2026-09-28 and at first held while more reviewers were asked; Dave then chose to proceed on this one. Swept into 154 units in `/tmp/translation-review/fr-findings.json`. The Siri files `fr.lproj/AppShortcuts.strings` and `Localizable.xcstrings` (French only, 10 values) were changed to match. A new `GrammarMarkersTest` case covers «menant à une impasse». All 154 were uploaded with `--skip-validate` the same day and verified live. The 37 that differ only have the U+00A0/U+202F that Weblate's French autofix adds before : and ?, which is correct.

**2026-09-30 — all-language review, no speaker involved.** All 1598 units read by `/translation-review-all`; 78 flagged, 60 applied in `e23801f4b`. Findings: `translations/review/2026-09-30-all-languages/fr-findings.json`.
Most serious fixed:
- `faq_mobile_data_use_answer`: Says the Sleep button force-closes the app («pour mettre Soundscape en pause ou pour forcer la fermeture»);
- `help_text_ahead_of_me_when`: Drops 'ahead' ('coming up on either side of the street ahead'), and 'things' became «repères», the app's Landmarks term.
- `faq_headset_battery_impact_answer`: «ne doit pas être un facteur important» reads as 'must not be';
Also changed the same day:
- `ecf310fc7`: the clock-position template and the twelve `relative_clock_hour_*` words were fixed together so every hour agrees (see `_common.md` C20).
- `f4fd7a092`: `relative_degrees_direction` is a plural now, and this language's forms differ by angle (C25).

**2026-10-01 — round-2 answers (Q1–Q5).** Confirmed «étape» and the Sleep labels; changed Wake On Leave, the third detail level and the bare-«balise» rule; FR-S3 is about our questionnaire, not the app. Swept into 43 units in `/tmp/translation-review/fr-findings.json` (FR-B2 34, FR-L1 7, FR-T3 2, with overlaps). Nothing applied. fr_CA not yet checked against these.

**2026-10-19 — JMB's answers (round 3 plus comments on every decision), on `main`.** Swept the strings.xml corpus (1655 units; the help text is still `help_text_*`/`faq_*` on main): 43 units in `/tmp/translation-review/fr-findings.json`, 35 to apply (FR-I1 22, FR-L1 7, FR-T3 6, FR-G1 2, with overlaps) and 8 FR-B3 held with `suggested = current`. Nothing applied. Still to carry in the apply step: `docs/release-notes.fr.md`, a Siri regeneration for «Essentiel», the GrammarMarkersTest wording, and the questionnaire's «Ce que nous avons décidé». fr_CA not checked.

**2026-10-20 — JMB's round-4 answers plus FR-D1.** Updated `/tmp/translation-review/fr-findings.json` (old copy: `fr-findings.2026-10-19.bak.json`): 52 units, 50 to apply (FR-I1 22, FR-B3 9, FR-D1 8, FR-L1 7, FR-T3 4, FR-G1 2, with overlaps). FR-T3's Wake On Leave rows were set back to `suggested = current`. Nothing applied. «ici» was then dropped from the FR-B3 label (Dave) because the audio menu reuses it. **Applied the same day** (`29c865375`): all 50, with the French Siri choices regenerated and the GrammarMarkersTest comment updated. `docs/release-notes.fr.md` follows FR-I1 and FR-L1, and the questionnaire moved to round 5. fr_CA still not checked; `docs/users/help-*.fr.md` regenerate at release.

**2026-10-01 — release notes (C26).** `docs/release-notes.fr.md`: «Minimal», «balise» after the first mention, *Annoncer la balise*, and the sleep passage quotes *Reprendre au déplacement*.
