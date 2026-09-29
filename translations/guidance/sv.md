# Swedish (sv) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional sv-SE iOS localisation (C14). 225 of 359 shared keys still verbatim, 15 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «du», consistent with Microsoft, `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

Swedish started from Microsoft's professional translation (C14) and has
drifted the least of the Nordic languages. The VoiceOver template composes
correctly («Dubbeltryck för att stänga av ljudfyren»). The Siri phrases
(`sv.lproj`) match the help text. There is one real inconsistency (SV-T1).
Questions: `docs/translation-questions/questions-sv.md` (Q1…Q4).

## Glossary

| English | Swedish | Status | Note |
|---|---|---|---|
| Callout | informationsljud | `confirmed` | Microsoft |
| Audio Beacon | ljudfyr | `confirmed` | Microsoft |
| Marker | platsmarkör | `confirmed` | Microsoft |
| Waypoint | brytpunkt | `confirmed` | Microsoft |
| Intersection | vägkorsning | `confirmed` | Microsoft |
| Sleep / Snooze | Inaktivera / Inaktiverad ; Snoozar | `confirmed` | Microsoft, restored 2026-09-25 (C14 parity). See SV-T1 |
| Traveling / Heading | Reser / På väg norrut | `confirmed` | Microsoft |
| Detail levels | Detaljerad / Balanserad / Lågmäld / Tyst | `unconfirmed` | AI. Distinct, and «Lågmäld» is a nice choice for "fewer" |
| dead end | återvändsgata | `unconfirmed` | AI |

## Rules

### SV-T1 — The Sleep button and its status no longer match (`confirmed` fixed; **superseded 2026-09-25**)

**Superseded 2026-09-25:** under Dave's iOS-parity policy (C14), the whole family went back to Microsoft's «Inaktivera» (button) / «Inaktiverad» (status) / «Snoozar» (snooze), and the three help/FAQ strings that quote the button (*"Viloläge"* → *"Inaktivera"*) followed. Unquoted «viloläge» still names the mode, which Microsoft did too. The 2026-09-24 fix below («I viloläge») is kept for the record but no longer ships.

**Fixed 2026-09-24:** `sleep_sleeping` «Inaktiverad» → «I viloläge», uploaded and verified live. «I viloläge» (status) stays distinct from the button «Viloläge». Serbian SR-T1 shows why that matters.

Microsoft's pair was «Inaktivera» (button) / «Inaktiverad» (status). An AI
pass changed the button to «Viloläge» ("sleep mode") and left the status as
«Inaktiverad». «Viloläge» has since spread to 11 strings, including the
sleep message, the a11y hint and every FAQ answer about it. So the outlier
is now the **status alone**: `sleep_sleeping` → «I viloläge» is a one-string
fix. The other «inaktiver-» uses in the corpus mean "turn off" in general
(C5) and stay. This is the case recorded in `_common.md` C14.

### SV-B1 — Hints are infinitives (`agreed`, fixed 2026-09-29)

Accessibility hints go into «Dubbeltryck för att %1$s» (and TalkBack's «…för att <label>»), so they must be infinitives: «lägga till», «göra», «välja», never «lägg till», «Gör», «välj». Check any new hint. See DA-B1.

### SV-G1 — «ljudfyr» is common gender (`agreed`, fixed 2026-09-29)

«en ljudfyr», «ljudfyren», matching «fyr». The assistant and Siri strings had drifted to «ett ljudfyr» / «ljudfyret».

### SV-C1 — Siri phrases are Swedish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

Numbered as on the questionnaire.

1. Sleep/Snooze are back to Microsoft's «Inaktivera» / «Inaktiverad» / «Snoozar». OK?
2. «Stig till återvändsgata»: natural, or with «en»?
3. Siri phrases «Soundscape omgivning / rutt / ljudfyr / stoppa ljudfyr…»: natural?
4. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`sv-SE.lproj`).
**2025 → 2026-09 — AI passes**, plus Weblate bulk operations.
**2026-09-24 — corpus sweep** with a Microsoft comparison. Nothing uploaded.

**2026-09-24 — SV-T1 applied.** `sleep_sleeping` → «I viloläge», uploaded and verified live.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_sleep` → «Inaktivera», `sleep_sleeping` → «Inaktiverad», `sleep_snoozing` → «Snoozar», plus the quoted button name in `help_text_automatic_callouts_how_1`, `faq_controlling_what_you_hear_answer` and `faq_tip_turning_off_auto_callouts`. 6 units, verified live.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 9 were restored to Microsoft's wording and 4 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 9 uploaded and verified live.

**2026-09-25 — truncation repaired (C16).** `help_text_my_location_what`, `help_text_my_location_how`, `tour_start_beacon` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 25 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Loading indicator → «Läser in». Uploaded live.

**2026-09-29 — Full review of all 1586 units, 31 fixes uploaded.** The Weblate log shows no human Swedish edits. Fixed:
- **Microsoft errors:**
  - "facing" rendered as walking / «färdriktningen» in `help_text_my_location_what`/`_when` and the four `help_text_*_how`, plus three times in `faq_why_does_beacon_disappear_answer` → «det håll du är vänd (åt)».
  - «bland att» → «bland annat»; «*Min plats mig*» → «*Min plats*».
  - `terms_of_use_medical_safety_disclaimer`: «medicinsk utrustning» → «rådgivning» for "medical advice".
  - `help_creating_markers_page_title` «Skapa».
- **Other:**
  - `faq_supported_phones_answer`: a stale Android-only sentence, now with iOS 16 (C16).
  - `relative_clock_direction` «på klockan %1$s».
  - The two travel-mode tunnel callouts: «går in» → «åker in».
  - 9 imperative hints (SV-B1).
  - 7 neuter «ljudfyr» slips (SV-G1).
  - Two OSM name capitals.

Uploaded with `--skip-validate`; all 31 re-fetched and matched exactly.
