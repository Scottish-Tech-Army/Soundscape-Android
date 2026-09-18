# Ukrainian (uk) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1495 translated units (2026-09-18) |
| Last native-speaker input | 2026-09-18 |
| Reporter platform | iOS v2.0.49 (strings are shared KMP — see rule C6) |

Read with [`_common.md`](_common.md).

---

## Glossary

| English | Ukrainian | Status | Not | Why |
|---|---|---|---|---|
| Audio Beacon | звуковий маячок | `confirmed` | — | Already in use across 73 strings; native speaker confirmed it unchanged |
| Marker | мітка | `confirmed` | — | Already in use across 80 strings; native speaker confirmed it unchanged |
| Waypoint | зупинка | `agreed` | ~~маршрутна точка~~ | Google Maps Ukrainian uses «зупинка» for a stop added to a route. «маршрутна точка» was a calque of our own doc's "route point" gloss (rule C1) |
| Callout | підказка | `unconfirmed` | оголошення (current) | Google Maps Ukrainian Налаштування → Навігація uses «підказка». Blocked on the verb-form question below |
| Guided tutorial | Інтерактивний тур | `provisional` | ~~Керований навчальний посібник~~ | Reporter's own words: "sounds natural … cannot find any real examples of usage in Ukrainian applications, will update as soon as I find" |

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

### UK-R1 — Formal register, second-person plural (`confirmed by observation`)
The whole corpus addresses the user as «ви» / «Оберіть» / «ви можете». Keep
it. Not yet explicitly confirmed by the reporter — on the question list below.

### UK-T4 — Callout term change is *not* mechanical (`unconfirmed`)
29 strings carry «оголошення». 23 are noun forms and would swap cleanly; 6 use
the verb «оголошувати»/«оголошуючи» ("Soundscape automatically announces…"),
which «підказка» has no natural verb for. Do not sweep this term until the
question below is answered.

---

## Rejected

*(nothing yet — record turned-down suggestions here with the reasoning, so a
later pass doesn't reintroduce them)*

---

## Open questions for the next native-speaker round

1. **Callout verb forms.** With Callout = «підказка», how should clauses where
   Soundscape is the subject of the verb read? e.g.
   `help_text_markers_content_3`: «Soundscape автоматично оголошуватиме мітки,
   коли ви пройдете повз них». Options: (a) keep «оголошувати» as the verb and
   use «підказка» only as the noun; (b) rephrase to «даватиме підказки про…»;
   (c) keep «оголошення» after all. Affects 6 strings directly and decides
   whether the other 23 move.
2. **Callout history.** Does «підказка» still read correctly in
   `faq_miss_a_callout_answer` — "a list of your recent callouts" as «список
   ваших останніх підказок»? A missed announcement and a missed hint are
   different things.
3. **Guided tutorial.** Any precedent found for «Інтерактивний тур»? Holding
   5 strings on this.
4. **Register.** Confirm «ви» (formal plural) throughout is right for the
   audience, including the audio callouts and not just the UI.
5. **Confirm the two locked terms.** «звуковий маячок» and «мітка» are marked
   `confirmed` on the strength of the 2026-09-18 glossary note — please say
   explicitly if either should move, because future passes will now actively
   defend them.

---

## Provenance

**2026-09-18 — native speaker, via `localisation_issues.md` + screenshots.**
Reported: a four-term glossary, two hamburger-menu strings, and two New Route
strings — 4 strings in total. Sweep found **62 affected units**:

| Rule | Units | Disposition |
|---|---|---|
| UK-T3 Waypoint | 26 | Concrete fixes generated, `high` confidence |
| UK-S1 tautology | 2 | Concrete fixes generated, `high` confidence |
| UK-T4 Callout | 29 | Inventory only — blocked on question 1 |
| UK-T5 Tutorial | 5 | Inventory only — reporter flagged provisional |

Two of the four glossary terms turned out to already match what ships, which
is why they are recorded as `confirmed` rather than ignored.

Also raised upstream, not Ukrainian-specific: `docs/developers/translation-terminology.md`
had its Waypoint alternate reworded (rule C1), and `new_version_info_text` got a
translator comment warning about the tautology (rule C7).
