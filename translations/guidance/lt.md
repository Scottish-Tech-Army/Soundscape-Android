# Lithuanian (lt) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1697 units (2026-11-27) |
| Last native-speaker input | 2026-11-27: Robertas (Lithuania, GitHub `RobertasTa`) approved the glossary before translation and changed one term (Audio Beacon = «švyturys»), then read the finished text in a three-column review table and changed nothing |
| Register | Polite plural «jūs» throughout. See LT-R1 |
| Reporter platform | Android 15, free Lithuanian TTS engine "Ingutė". See LT-V1 |

Read with [`_common.md`](_common.md).

## Status of this file

Lithuanian arrived on 2026-11-27 as a complete first translation contributed from
outside the project: all 1697 units, written with `strings_sync.py` (fetch →
validate → apply) by a native speaker working with Claude. The glossary rows
marked `confirmed` were approved by the native speaker before translation
started. The rules marked `agreed` were chosen during translation and are
applied across the whole corpus; the native speaker read the result and did not
object, but nobody has yet *heard* them in the app, because Lithuanian is not
enabled. No blind Lithuanian user has tested it. There is no `lt.lproj`, so the
Siri phrases stay in English (the quoted phrases in `help_text_assistant_*`).

Words for what a blind pedestrian meets in the street follow the public audio
guides of the Lithuanian Union of the Blind and Visually Impaired (LASS,
lass.lt): «pėsčiųjų perėja», «orientyras», «taktilinis», «ekrano skaitytuvas».

## Glossary

| English | Lithuanian | Status | Note |
|---|---|---|---|
| Audio Beacon | švyturys | `confirmed` | The native speaker's own choice. Never «garso švyturys», see Rejected |
| Callout | pranešimas | `confirmed` | |
| Marker | žymė | `confirmed` | |
| Route | maršrutas | `confirmed` | |
| Waypoint | maršruto taškas | `confirmed` | Short form «taškas» on buttons («Kitas taškas») |
| Point of interest | vieta | `confirmed` | |
| Intersection | sankryža | `confirmed` | |
| My Location / Around Me / Ahead of Me / Nearby Markers | Mano vieta / Aplink mane / Priešais mane / Artimiausios žymės | `confirmed` | C27: one name each, everywhere |
| Places Nearby | Vietos netoliese | `confirmed` | |
| Sleep | Miego režimas | `confirmed` | |
| Offline map | žemėlapis be interneto | `confirmed` | Menu: «Žemėlapiai be interneto» |
| Detailed / Simplified / Essential / Silent | Išsamus / Supaprastintas / Esminis / Tylus | `agreed` | |
| Landmark | orientyras | `agreed` | LASS word |
| head tracker | galvos sekiklis | `agreed` | |
| screen reader | ekrano skaitytuvas | `agreed` | |
| guided tutorial | mokomoji apžvalga | `agreed` | |

## Rules

### LT-R1 — Polite «jūs», and no form that shows the user's gender (`agreed`)

Second person plural everywhere («galite», «palieskite»), never «tu».
Lithuanian adjectives and participles agree in gender, so C15 applies:
`first_launch_prompt_title` is the impersonal «Viskas paruošta!». Adverbial
forms such as «norėdami», «kurdami» are the ordinary polite plural and are fine.

### LT-G1 — Map names are never declined; use a label (C17) (`agreed`)

Lithuanian has seven cases and every preposition governs one, while map names
arrive in the nominative («Švyturio gatvė», «Maxima»). Every template that
takes a name therefore keeps the name in the nominative after a comma or a
colon:

- `confect_name_to` «%1$s, veda į: %2$s», `confect_name_via` «%1$s, per: %2$s»,
  `confect_name_to_dead_end` «%1$s, aklavietė»
- `directions_at_poi` «Esate čia: %1$s», `directions_approaching_name` «Artėja %1$s»
- `street_description_between` «Esate: %1$s, tarp: %2$s ir %3$s»
- `osm_bus_stop_named` «Autobusų stotelė: %1$s», `osm_train_station_named`
  «Geležinkelio stotis: %1$s»

Do not "fix" these into «%1$s link %2$s» or «ties %1$s»: the name would have to
be in the genitive or instrumental. A C18 marker could do better one day:
Lithuanian street names end in a small closed set of words that decline
regularly (gatvė / g., alėja / al., prospektas / pr., aikštė / a., plentas /
pl., kelias, takas, skersgatvis / skg.). Not written, not in code.

### LT-G2 — `osm_*` names are nominative singular, capitalised (C9) (`agreed`)

They are substituted into the LT-G1 templates as the subject or after a colon.
The only lower-case one is `osm_main_entrance` «pagrindinis įėjimas», which
follows a comma in `osm_entrance_with_destination` «%1$s, %2$s».

### LT-G3 — Hints are lower-case infinitives (C13) (`agreed`, see Q1)

«nutildyti švyturį», «išgirsti apie vietas priešais jus». They compose with the
iOS template `talkback_double_tap_template` «Dukart palieskite, kad galėtumėte
%1$s».

### LT-G4 — Four plural forms; distances are labels in the nominative (`agreed`)

CLDR categories one / few / many / other: «%1$s metras / metrai / metro /
metrų». `many` is the fraction form (genitive singular, «1,5 metro»); Compose
selects on integers so it is not reached, but it must be present. A distance is
never put after a preposition that would change its case.

### LT-G5 — Clock positions are genitive ordinals (C20) (`agreed`)

`relative_clock_direction` is «%1$s valandos kryptimi», and
`relative_clock_hour_1`…`12` are «pirmos», «antros», «trečios» … «dvyliktos».
A bare digit would be read as a cardinal («trys valandos kryptimi»). Degrees
(C25) are a label: «kryptis %1$s laipsniai / laipsnių».

### LT-G6 — A real lighthouse is «Jūrų švyturys» (C19) (`agreed`)

«Švyturys» alone is the Audio Beacon, so `osm_lighthouse` is «Jūrų švyturys»
and `osm_beacon` is «Signalinis žiburys». Don't shorten either.

### LT-G7 — Movement lines address the user (`agreed`)

Facing «Veidu į šiaurę», Heading «Einate į šiaurę», Traveling «Važiuojate į
šiaurę». The `*_bound` directions are lower case («šiaurės kryptimi») because
they follow a stop name.

### LT-G8 — No slash between alternatives (`agreed`, 2026-11-27)

The Lithuanian voice reads «/» aloud («pasvirasis brūkšnys»), so «Kairė /
dešinė» is heard as three words of punctuation. Write «ir» or «arba»:
`settings_relative_directions_left_right` «Kairė ir dešinė», the media button
«leisti arba pristabdyti». Found by synthesising every spoken string with
Ingutė and transcribing it back with a Lithuanian recogniser (1588 strings);
brackets, dashes and «…» are silent and fine.

### LT-V1 — Speech (`confirmed` by test on 2.0.95, 2026-11-27)

A free neural Lithuanian voice exists as an ordinary Android TTS engine:
"Ingutė", https://github.com/RobertasTa/android-lt . It appears under Settings →
Audio → Text-to-Speech Engine and speaks the callouts well. Source feedback
(C7): after the app language was changed, Soundscape kept the engine it had
stored earlier (Google), and Ingutė had to be chosen by hand.

## Unconfirmed word choices

Picked from several candidates; a second native speaker may prefer another.

| Key | English | Lithuanian | Alternative |
|---|---|---|---|
| `osm_convenience` | Convenience Shop | Smulkių prekių parduotuvė | Krautuvėlė (shorter when spoken) |
| `osm_supermarket` | Supermarket | Maisto prekių parduotuvė | Prekybos centras (kept for Mall) |
| `osm_townhall` | Town Hall | Savivaldybė | Rotušė |
| `osm_pier` | Pier | Lieptas | Prieplauka, Molas |
| `osm_city` / `osm_town` / `osm_village` / `osm_hamlet` | City / Town / Village / Hamlet | Didmiestis / Miestas / Gyvenvietė / Kaimas | Miestas / Miestelis / Kaimas / Viensėdis |
| `osm_information_tactile_map` | Tactile Map | Taktilinis žemėlapis | Liečiamasis, Reljefinis |
| `osm_stile` | Stile | Perlipa | a real but rare word |
| `osm_hostel` | Hostel | Hostelis | «Nakvynės namai» sounds like a shelter |
| `osm_ice_stock`, `osm_bandy`, `osm_pitch_pickleball`, `osm_korfball`, `osm_fire_flapper` | rare sports and objects | Aisštokas, Bendis, Piklbolo aikštelė, Korfbolas, Ugnies plaktuvas | unverified |

## Rejected

### «garso švyturys» for Audio Beacon (`rejected`, 2026-11-27)

The literal "sound lighthouse" was the first proposal. The native speaker cut
it to «švyturys»: it is said dozens of times on a walk, and the short word is
enough once the user has heard the beacon. LT-G6 handles the one clash.

## Open questions

- **Q1.** What does Lithuanian TalkBack say around an `onClick` label? LT-G3
  assumes a frame that takes an infinitive. Check on a phone with TalkBack in
  Lithuanian once the language is enabled.
- **Q2.** Do the label forms of LT-G1 sound acceptable to blind listeners on a
  real walk, or tiring? Needs a walk test in Vilnius or Kaunas.
- **Q3.** The iOS menu names quoted in `help_config_voices_content_ios` follow
  Apple's Lithuanian iOS wording from memory; verify on an iPhone.
