# Bulgarian (bg) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units, 769 excluding `osm_*` (2026-09-24) |
| Last native-speaker input | **none yet** |
| Reporter platform | — |
| Register | Formal «Вие», consistent (`unconfirmed`, see BG-R1) |

Read with [`_common.md`](_common.md).

---

## Status of this file

**No native speaker has reviewed Bulgarian.** All 14 commits to
`values-bg/strings.xml` (since 2026-08-23) are AI passes through Weblate. As
with `pl.md` and `fr.md`, everything is `unconfirmed` and exists for the first
reviewers to react to. Nothing is swept from it.

The corpus is in better shape than most AI-only languages. It is consistent
on register, every UI label starts with a capital, and the accessibility
hints are verbal nouns («изключване на звука на аудио маяка»), which slot
grammatically into «Двукратно докосване за %1$s». There is no defect
to fix. The open items are all term choices.

The questions for reviewers are in `translations/review/bg.md`, numbered
Q1…Q9 to match the Open questions below.

---

## Glossary

| English | Bulgarian | Status | Why |
|---|---|---|---|
| Callout | аудио съобщение / съобщение | `unconfirmed` | 31 × «аудио съобщение», plus bare «съобщения» in newer strings («Детайлност на съобщенията»). Long for a word spoken this often. See BG-T1 / Q1 |
| Audio Beacon | аудио маяк | `unconfirmed` | 34 occurrences, but also 4 × «звуков маяк». See BG-T2 / Q2 |
| Marker | маркер | `unconfirmed` | 85 occurrences, consistent |
| Waypoint | пътна точка | `unconfirmed` | Common in Bulgarian GPS vocabulary, but unverified (rule C1). See Q3 |
| Landmarks | забележителности | `unconfirmed` | Consistent |
| Intersection | кръстовище | `unconfirmed` | 23 occurrences |
| Junction (motorway, with ref) | възел | `unconfirmed` | `directions_junction_with_ref` «Възел %1$s» |
| Sleep / Snooze | Сън / Заспал ; Дремещ | `unconfirmed` | See BG-T3 / Q4 |
| Callout Detail | Детайлност на съобщенията | `unconfirmed` | AI pass, 2026-09-23 |
| Detailed / Balanced / Quiet / Silent | Подробно / Балансирано / Тихо / Без звук | `unconfirmed` | Distinct roots, so this looks fine by ear. See Q7 |
| Destination | дестинация | `unconfirmed` | 17 occurrences. An anglicism; «местоназначение» or «цел» may be more natural. See Q6 |
| dead end | задънена улица | `unconfirmed` | Grammatically fine in the template. See BG-G1 |

---

## Rules

### BG-G1 — `confect_name_dead_end` is *not* a C9 case (`agreed`, no change)

Rule C9 in `_common.md` lists the Slavic languages whose templates put the
dead-end string in the wrong case. **Bulgarian is not one of them, and the
omission is correct.** Bulgarian has lost noun case, so «Пътека до задънена
улица» (`confect_name_to` «%1$s до %2$s») is grammatical as it stands. OSM
names in the same slot are fine for the same reason. Recorded so that a
reviewer applying C9 across the Slavic languages doesn't "fix" it.

### BG-R1 — Formal «Вие» throughout (`unconfirmed`)

«Докоснете», «Готови сте!», «Добре дошли!». The plural forms also avoid any
gender agreement with the user, which is a real advantage for a spoken
interface (compare IS-G3 in `is.md`). Keep it unless the reviewers say
otherwise.

Cosmetic side issue: the polite pronoun is capitalised «Ви» 90 times and
lowercase «ви» 44 times. Formal Bulgarian writing capitalises it when
addressing one person. It is invisible to speech, so ask which convention to
standardise on rather than sweeping. See Q5.

### BG-T1 — Callout: «аудио съобщение» is long (`unconfirmed`)

It is spoken often and runs to about eight syllables. Newer strings already drop
«аудио» («Детайлност на съобщенията»), so the corpus is quietly moving
toward bare «съобщение». «известие» is shorter still but is what Android
calls system notifications (`first_launch_permissions_notification`
«Известия»), which is the same collision as fr/pl/is. See Q1.

### BG-T2 — Beacon: «аудио маяк» vs «звуков маяк» (`unconfirmed`)

34 against 4. Pick one. Probably the majority form, but a speaker may prefer
«звуков», which is Bulgarian rather than a borrowed prefix. See Q2.

### BG-T3 — Sleep/Snooze labels (`unconfirmed`)

`sleep_sleep` is the noun «Сън» on a *button*, where a verb or verbal noun
(«Заспиване», «Приспиване») would be expected. The status labels «Заспал» /
«Дремещ» are masculine participles agreeing with Soundscape. «Дремещ» in
particular may sound odd. See Q4.

### BG-T4 — «Пътувате» vs «Вървите» is correct as it stands (`agreed`, no change)

`directions_traveling_*` is «Пътувате на север» and `directions_heading_*` is
«Вървите на север». «Вървите» literally means "you are walking", which looks
wrong for a generic "heading", but it isn't. `CompassFacingDirections.kt`
uses *Traveling* only when `inVehicle` and *Heading* only when walking, so
the verbs match the situations exactly. Don't "fix" «Вървите» into something
neutral. The same split is worth checking in other languages, which may not
have noticed it.

### BG-C1 — Siri command phrases stay in English (`agreed`)

There is no `bg.lproj/AppShortcuts.strings`, and Siri doesn't support
Bulgarian, so `help_text_assistant_*_ios` quotes the English phrases. The
same reasoning as PL-C1.

---

## Rejected

Nothing yet (rule C8 when there is).

---

## Open questions for the first native-speaker round

These are the questions in `translations/review/bg.md`, in the same order.

1. **Callout:** «аудио съобщение», «съобщение» or something else? (BG-T1)
2. **Beacon:** «аудио маяк» or «звуков маяк»? (BG-T2)
3. **Waypoint «пътна точка»:** natural? What do your navigation apps say?
4. **Sleep/Snooze:** «Сън», «Заспал», «Дремещ». (BG-T3)
5. **«Вие» OK? And «Ви» or «ви»?** (BG-R1)
6. **«дестинация» or «местоназначение» / «цел»?**
7. **The four detail levels:** clear by ear?
8. **Siri phrases stay English:** OK? (BG-C1)
9. **Anything else.**

---

## Provenance

**2026-08-23 → 2026-09-24 — AI translation passes only.** 14 Weblate commits.

**2026-09-24 — corpus sweep, no speaker involved.** Built this file and the
question sheet. Checked C9 against Bulgarian grammar (it does not apply) and
the double-tap template against the hint strings (it composes correctly).
Nothing uploaded.
