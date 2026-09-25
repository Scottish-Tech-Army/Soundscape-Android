# Ukrainian (uk) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1495 translated units (2026-09-21) |
| Last native-speaker input | 2026-09-21 |
| Reporter platform | iOS v2.0.49 (strings are shared KMP — see rule C6) |

Read with [`_common.md`](_common.md).

---

## Glossary

| English | Ukrainian | Status | Not | Why |
|---|---|---|---|---|
| Audio Beacon | звуковий маячок | `confirmed` | — | Already in use across 73 strings; native speaker confirmed it unchanged |
| Marker | мітка | `confirmed` | — | Already in use across 80 strings; native speaker confirmed it unchanged |
| Waypoint | зупинка | `agreed` | ~~маршрутна точка~~ | Google Maps Ukrainian uses «зупинка» for a stop added to a route. «маршрутна точка» was a calque of our own doc's "route point" gloss (rule C1) |
| Callout | оголошення | `confirmed` | ~~підказка~~ | Kept unchanged on the reporter's second look — see "Rejected". Ukrainian has no good word for this; «оголошення» is the settled least-bad choice, not an oversight |
| Guided tutorial | Інтерактивний тур | `agreed` | ~~Керований навчальний посібник~~, ~~Навчання~~ | Reporter proposed it 2026-09-18 as provisional ("cannot find any real examples of usage in Ukrainian applications"), confirmed OK 2026-09-21. Also replaces «Навчання» in `tour_finish` |
| Dead end | тупик | `agreed` | ~~кінець дороги~~ | «кінець дороги» is "end of the road", not the street type. Reporter, 2026-09-21. See UK-G1 for the case it must take |

### Waypoint declension map (`agreed`, mechanical)

Both nouns are feminine, so the swap is regular and needs no agreement changes:

| маршрутна точка | зупинка |
|---|---|
| маршрутна точка | зупинка |
| маршрутну точку | зупинку |
| маршрутної точки | зупинки |
| маршрутній точці | зупинці |
| маршрутною точкою | зупинкою |
| маршрутні точки | зупинки |
| маршрутних точок | зупинок |

Known collision: `faq_tip_create_marker_at_bus_stop` already uses «зупинки» for
literal bus stops. Accepted — context disambiguates (rule C5).

---

## Rules

### UK-S1 — No «нове оновлення» (`agreed`)
«release» and «update» both render as «оновлення», so "new release" becomes
"new update". Drop «нове». Applies to `new_version_info_text` and its sibling
`new_version_info_completed`.

### UK-R1 — Formal register, second-person plural, throughout (`confirmed`)
Address the user as «ви» everywhere — UI, TalkBack/VoiceOver hints, audio
callouts, help pages and the tutorial: «ви можете», «ваш маршрут», plural
imperatives «Натисніть», «Оберіть». Never «ти»/«твій» or singular
imperatives («натисни», «обери»). Confirmed by the reporter 2026-09-21.

A sweep on 2026-09-21 found no informal forms in the 1495 translated units
(«ти»/«тебе»/«твій» and common singular imperatives): the rule confirms what
ships. Its job now is to keep new strings in line.

### UK-G1 — Strings slotted into a template take the template's case (`agreed` as a principle; **the dead-end application is superseded by UK-G2**)
Some strings are never shown on their own; the code substitutes them into a
template. They must be translated in the grammatical case that template's
preposition governs, not in the dictionary form.

`confect_name_dead_end` is only ever the `%2$s` of `confect_name_to`
(«%1$s у напрямку %2$s») and `confect_name_to_via` (see `WayGenerator.kt`).
«у напрямку» governs the genitive, so it is «тупика», not «тупик»:
«дорога у напрямку тупика». The reporter heard «дорога у напрямку кінець
дороги» — wrong word *and* wrong case.

Open side: the same `%2$s` slot is also filled with destination names from map
data, which arrive undeclined (question 1 below).

### UK-G2 — Direction callouts use the label form «напрямок: X» (`agreed`, reporter 2026-09-25)

The reporter, answering the open question about undeclined destinations:

> String «Стежка у напрямку вулиця Шевченка» does not work, it is
> grammatically incorrect and sounds broken. In Ukrainian, if we use
> prepositional phrase "у напрямку" we must put "вулиця" in the correct case
> (genitive, instead of nominative) - "вулиці". … Formation of genitive case is
> different for different words, there is no predictable pattern. … The best
> option for all these callouts of such type, including "тупик" is to use
> approach (c) «%1$s, напрямок: %2$s». … Here nominative case sounds natural.

Their second live example was «Далі дорога у напрямку парковка південь». («Далі»
is the Street Preview "Go" button, and «парковка південь» is a map name.)

The genitive can't be generated for runtime map names, so the preposition
goes and the name stands in the nominative after a label:

| Key | Was | Now |
|---|---|---|
| `confect_name_to` | «%1$s у напрямку %2$s» | «%1$s, напрямок: %2$s» |
| `confect_name_to_via` | «%1$s у напрямку %2$s через %3$s» | «%1$s, напрямок: %2$s, через %3$s» |
| `directions_towards_settlement` | «у напрямку %1$s, за %2$s» | «напрямок: %1$s, за %2$s» |
| `confect_name_dead_end` | «тупика» | «тупик» (nominative again, since there is no preposition to govern it) |

`directions_towards_settlement` wasn't named by the reporter, but it is the
same «у напрямку + map name» construction ("…, towards Kyiv, 5 km away"), so
it's in scope (C4). «через %3$s» stays, because %3$s only ever receives
«Сходи», «Міст» or «Тунель», which are inanimate, so their accusative is the
same as the nominative.

### UK-G3 — The same problem in every other preposition + map-name template (`unconfirmed`, inventory only)

A sweep found **54 more templates** where a map name (road, POI, settlement or
marker) follows a case-governing preposition and so arrives nominative. For
example `directions_on_road` «На %1$s» → «На вулиця Шевченка» (needs «вулиці»),
`directions_near_name` «Поблизу %1$s» (genitive), the 24
`directions_along_*` «по/вздовж %1$s», `street_description_*` «між / до / після
%2$s», `directions_approaching_name` «до %1$s», `directions_at_poi` «У %1$s».
These are the main callouts, so this matters more than the four above. The
reporter's rule was stated for the «у напрямку» type only, so these are
inventory: `/tmp/weblate-review/uk-findings.json` rule UK-G3. See question 1.

### UK-T4 — Callout stays «оголошення» (`confirmed`, closed)
29 strings carry «оголошення» and all of them stay as they are. See "Rejected"
below for why, and read that before proposing anything in this area.

---

## Rejected

### Genitive dead end «тупика» — rejected 2026-09-25, superseded by UK-G2

It was the obvious fix, and a correct one in isolation: «у напрямку» governs
the genitive (UK-G1, C9), and the 2026-09-18 feedback asked for it. It was
rejected because the same slot also takes map names that can't be declined.
The label form fixes both at once and is the reporter's choice. Don't put
«тупика» back while `confect_name_to` uses the label form.

### Callout → «підказка» — rejected 2026-09-18 by the reporter who proposed it

**Do not re-propose this.** It will look like an improvement when you find the
evidence for it, because the evidence is real: Google Maps Ukrainian uses
«підказка» in Налаштування → Навігація, and the reporter cited it with a
screenshot in their first round. They withdrew it on a second look, saying
there is no really good Ukrainian translation for *callout* and the existing
«оголошення» should stay.

Both words are imperfect. «Оголошення» ("announcement") is heavier than the
English and «підказка» ("hint, prompt") is lighter — a Soundscape callout is
neither exactly. There is no third option that fixes it, so the cost of
switching 29 strings buys nothing.

This rejection also settles the practical objection that blocked the change
anyway: 6 of the 29 strings use the verb «оголошувати» ("Soundscape
автоматично оголошуватиме мітки"), and «підказка» has no natural verb, so the
change was never the mechanical swap it appeared to be (rule C3).

---

## Open questions for the next native-speaker round

1. **The other 54 map-name templates (UK-G3).** Should the label form extend to
   them too? For example «На %1$s» → «Вулиця: %1$s»? «Поблизу %1$s» →
   «Поблизу: %1$s»? Or are some acceptable as they are? The biggest groups are
   `directions_on_road*` («На …»), the `directions_along_*` family («по / вздовж
   …») and `street_description_*` («між / до / після …»).
2. **Confirm the two locked terms.** «звуковий маячок» and «мітка» are
   `confirmed`. Say if either should move.
3. **The four detail levels** (Докладний / Збалансований / Тихий / Беззвучний):
   distinct by ear?

*Answered and closed: undeclined destination names → the label form (UK-G2, 2026-09-25). The two callout questions, see "Rejected"; the guided-tutorial term, confirmed OK 2026-09-21; register, formal «ви» confirmed 2026-09-21.*

---

## Provenance

**2026-09-18 — native speaker, via `localisation_issues.md` + screenshots.**
Reported: a four-term glossary, two hamburger-menu strings, and two New Route
strings — 4 strings in total. Sweep found **62 affected units**:

| Rule | Units | Disposition |
|---|---|---|
| UK-T3 Waypoint | 26 | Concrete fixes generated, `high` confidence |
| UK-S1 tautology | 2 | Concrete fixes generated, `high` confidence |
| UK-T4 Callout | 29 | **Rejected** — no change (see below) |
| UK-T5 Tutorial | 6 | Held as provisional, then confirmed 2026-09-21; 6th unit `tour_finish` found on re-sweep |

Two of the four glossary terms turned out to already match what ships, which
is why they are recorded as `confirmed` rather than ignored.

**2026-09-18 (second round) — same reporter.** Withdrew their own Callout
suggestion: keep «оголошення», there is no really good Ukrainian translation.
That closes UK-T4 as `rejected` and takes the corpus-wide impact of this
feedback round from 62 units down to 33 — 28 to change, 5 still held. Three
of the four terms the reporter originally listed ended up confirming what
already ships.

Also raised upstream, not Ukrainian-specific: `docs/developers/translation-terminology.md`
had its Waypoint alternate reworded (rule C1), and `new_version_info_text` got a
translator comment warning about the tautology (rule C7).

**2026-09-21 — same reporter, bug report `localisation_UA_dead_end_translation.md`.**
iOS 26.6.2, Soundscape 2.0 (build 1062). "dead end" → «тупик», in the genitive
«тупика» after «у напрямку». 1 unit in Ukrainian, but the root cause is not
Ukrainian: the English comment calls the string a "road description" and gives
no hint it is substituted mid-sentence, so every case-inflecting language
translated it in the nominative. Ukrainian, Russian, Polish, Czech, Slovak,
Croatian, Serbian and Slovenian all have the same agreement bug (rule C9).

**2026-09-21 — same reporter.** «Інтерактивний тур» confirmed OK, so UK-T5
moves from `provisional` to `agreed`. Re-sweeping for it found a sixth unit,
`tour_finish`, where the same concept had a third rendering («Навчання
завершено») that the original «керован…/навчальн… посібник» search didn't
match (rule C4).

**2026-09-21 — same reporter.** Formal plural «ви» confirmed, to be used
throughout. UK-R1 moves to `confirmed`. The sweep found nothing to change.

**2026-09-21 — applied.** All 35 fixes uploaded to Weblate and verified live:
Waypoint → «зупинка» (26), Guided tutorial → «Інтерактивний тур» (6), the «нове
оновлення» tautology (2) and dead end → «тупика» (1). A re-sweep afterwards
found no «маршрутна точка» or «посібник» left anywhere in the corpus.

**2026-09-25 — truncation repaired (C16).** `faq_background_battery_impact_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-25 — reporter answered question 1 (undeclined destinations).** Chose approach (c), the label form. Recorded as UK-G2 (4 units ready in `/tmp/weblate-review/uk-findings.json`, not uploaded), with UK-G1's genitive dead end rejected. The sweep found 54 more preposition + map-name templates (UK-G3), now question 1.
