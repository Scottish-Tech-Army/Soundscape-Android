# Persian (fa) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last human input | 2025-11-01 (Khashayar Hamidzadeh, Weblate, ~490 lines). **Possibly machine-assisted**, so don't treat it as confirmed (see below) |
| Register | Formal plural («آماده‌اید!», «بزنید»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

A human, Khashayar Hamidzadeh, submitted a large Persian translation in
Weblate (2025-09-21 → 2025-11-01), after mahmood hozhabri added the language
(2025-08-28). **Dave's note (2026-09-24): this may itself have been AI
output, so give it no special credence.** His terms are therefore
`unconfirmed`, like everything else. He revised several terms more than once
(«نشانه ی صوتی» → «جهت‌نمای صوتی» for Beacon, «اعلامیه» → «اعلان» for Callout),
which is the only real evidence of human judgement.

Things that already work:
- The template «دو بار ضربه بزنید تا %1$s» composes with the hints, because
  the imperative and subjunctive 2pl are the same form («قطع کنید»).
- «مختصر» (brief) for Quiet keeps it apart from «بی‌صدا» (silent).
- There is no `fa.lproj`, so the Siri phrases stay in English.

Questions: `docs/translation-questions/questions-fa.md` (Q1…Q5).

## Glossary

| English | Persian | Status | Note |
|---|---|---|---|
| Callout | اعلان | `unconfirmed` | «اعلان» is also Android's word for a notification. See Q1 |
| Audio Beacon | جهت‌نمای صوتی | `unconfirmed` | "Audio direction-pointer". Descriptive, which is good |
| Marker | نشانه | `unconfirmed` | |
| Waypoint | نقطه‌ی بین‌راهی | `unconfirmed` | |
| Landmarks | نقاط شاخص | `unconfirmed` | |
| Intersection | تقاطع | `unconfirmed` | |
| Sleep / Snooze | حالت خواب ; حالت چرت | `unconfirmed` | |
| Detail levels | مفصل / متعادل / مختصر / بی‌صدا | `unconfirmed` | Distinct |
| dead end | بن‌بست | `unconfirmed` | |

## Rules

None yet beyond the glossary. The corpus has no structural defects that
this sweep could find.

## Rejected

Nothing yet.

## Open questions

1. Callout «اعلان»: confused with phone notifications?
2. Beacon «جهت‌نمای صوتی»: clear?
3. Snooze «حالت چرت»: natural?
4. Is the formal plural register right?
5. Anything else.
6. Points of interest vs Landmarks: «نقاط شاخص» is used for both. It appears in
   13 strings for "points of interest" and is also the Landmarks category
   (`callouts_places_landmarks`, the Quiet-mode description in
   `callouts_verbosity_description`, `osm_generic_landmark`). So "all places" and
   "landmarks only" sound the same. Should one of them get a different word
   (e.g. «مکان‌های دیدنی» or «مکان‌ها» for points of interest)? Raised by the
   2026-09-29 review, and unchanged until answered.

## Provenance

**2025-08-28 — mahmood hozhabri** added the language. **2025-09-21 →
2025-11-01 — Khashayar Hamidzadeh**, ~490 lines (possibly machine-assisted).
**2026 — AI passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 24 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** «تا تا زمانی» → «تا زمانی» in `faq_controlling_what_you_hear_answer`. Rugby → «زمین راگبی لیگ» / «زمین راگبی یونیون», `osm_ferry_terminal` → «پایانه کشتی», `osm_dock` → «بارانداز» (all three had been «اسکله»). Uploaded live.

**2026-09-29 — full review (1586 units).** 30 flagged, and 29 were uploaded and verified live.
The leftover old Beacon term in `faq_what_can_I_set_question` («نشانه‌ی صوتی» → «جهت‌نمای صوتی»), a typo,
the reverse-route hint rewritten as a verb so it fits the TalkBack template, `osm_lock_gate`
«آب‌بند», `osm_wreck` «لاشهٔ کشتی», «خواهید دید» → «متوجه خواهید شد» for a sound change,
`osm_religion` without the slash, both named ferry terminals → «پایانه کشتی %1$s». Also stray `*`
removed from five `tour_*` strings, because the tutorial dialog shows plain text. And Latin «Soundscape» →
«ساند‌اسکیپ» in the running text of 15 strings (voice-command help, spoken action/Siri replies,
migration messages, GPS help). The Siri command phrases inside `*…*` stay English, because there is no
`fa.lproj`. Open: Q6 (points of interest vs Landmarks).
