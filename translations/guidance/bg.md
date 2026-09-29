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
grammatically into «Двукратно докосване за %1$s» (BG-B1: 12 that had
drifted were fixed on 2026-09-29). The open items are all term choices.

The questions for reviewers are in `docs/translation-questions/questions-bg.md`, numbered
Q1…Q11 to match the Open questions below.

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

### BG-B1 — Hints are verbal nouns (`agreed`, fixed 2026-09-29)

«Двукратно докосване за %1$s» ("double tap for …") needs a verbal noun: «отваряне на менюто», «приспиване на Soundscape», «информация за …». Not a «да»-clause («за чуете», «за отворите», which is missing its «да»), an imperative («изберете») or a finite verb («възстановява»). Check any new hint.

### BG-C1 — Siri command phrases stay in English (`agreed`)

There is no `bg.lproj/AppShortcuts.strings`, and Siri doesn't support
Bulgarian, so `help_text_assistant_*_ios` quotes the English phrases. The
same reasoning as PL-C1.

---

## Rejected

Nothing yet (rule C8 when there is).

---

## Open questions for the first native-speaker round

These are the questions in `docs/translation-questions/questions-bg.md`, in the same order.

1. **Callout:** «аудио съобщение», «съобщение» or something else? (BG-T1)
2. **Beacon:** «аудио маяк» or «звуков маяк»? (BG-T2)
3. **Waypoint «пътна точка»:** natural? What do your navigation apps say?
4. **Sleep/Snooze:** «Сън», «Заспал», «Дремещ». (BG-T3)
5. **«Вие» OK? And «Ви» or «ви»?** (BG-R1)
6. **«дестинация» or «местоназначение» / «цел»?**
7. **The four detail levels:** clear by ear?
8. **Siri phrases stay English:** OK? (BG-C1)
9. **Points of interest «забележителности» vs «интересни места»:** the landmarks word is used for both.
10. **Button labels in the informal imperative** («Спри маршрута», «Чуй околността си») under a formal «Вие» register: OK? (BG-R1)
11. **Anything else.**

---

## Provenance

**2026-08-23 → 2026-09-24 — AI translation passes only.** 14 Weblate commits.

**2026-09-24 — corpus sweep, no speaker involved.** Built this file and the
question sheet. Checked C9 against Bulgarian grammar (it does not apply) and
the double-tap template against the hint strings (it composes correctly).
Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `confect_name_to` → «%1$s към %2$s» (and `_to_via`), as its note asks. «до» stays for "next to". Uploaded live.

**2026-09-29 — Full review of all 1586 units, 21 fixes uploaded.** No human Bulgarian edits in Weblate. Fixed:
- **12 hints** had drifted from verbal nouns to «да»-clauses without «да», imperatives or a finite verb (BG-B1), plus 2 with a capital «Показване».
- **`relative_left_right_direction_behind*`:** «Назад» ('backwards') → «Отзад» (3).
- **Help text now names the real button or filter:** «*Изключи звука на маяка*», «бутона *Включи звука на маяка*», «*Обществен транспорт*».
- **`faq_how_to_use_beacon_answer`:** retranslated whole. It was translated from an older English with a sailboat-tacking sentence (C16).

Uploaded with `--skip-validate`; all 21 re-fetched and matched exactly.

Not changed, now questionnaire Q9 and Q10:
- "Points of interest" is mostly «забележителности», the landmarks word (one setting says «интересни места»).
- Button labels use the informal imperative («Спри маршрута», «Чуй околността си») under an otherwise formal «Вие» register (BG-R1).
