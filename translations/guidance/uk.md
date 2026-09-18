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
| Callout | оголошення | `confirmed` | ~~підказка~~ | Kept unchanged on the reporter's second look — see "Rejected". Ukrainian has no good word for this; «оголошення» is the settled least-bad choice, not an oversight |
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

### UK-T4 — Callout stays «оголошення» (`confirmed`, closed)
29 strings carry «оголошення» and all of them stay as they are. See "Rejected"
below for why, and read that before proposing anything in this area.

---

## Rejected

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

1. **Guided tutorial.** Any precedent found for «Інтерактивний тур»? Holding
   5 strings on this.
2. **Register.** Confirm «ви» (formal plural) throughout is right for the
   audience, including the audio callouts and not just the UI.
3. **Confirm the two locked terms.** «звуковий маячок» and «мітка» are marked
   `confirmed` on the strength of the 2026-09-18 glossary note — please say
   explicitly if either should move, because future passes will now actively
   defend them.

*Answered and closed: the two callout questions — see "Rejected".*

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
| UK-T5 Tutorial | 5 | Inventory only — reporter flagged provisional |

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
