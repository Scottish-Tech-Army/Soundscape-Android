# Greek (el) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional el-GR localisation (C14). 215 of 359 shared keys still verbatim, 21 drifted |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | Formal plural («Είστε έτοιμοι!»), `confirmed` (baseline) |

Read with [`_common.md`](_common.md).

## Status of this file

A Microsoft baseline. There is no `el.lproj`, so the Siri phrases stay in
English (PL-C1). The formal plural «Είστε έτοιμοι!» is Microsoft's and
gender-neutral enough. There is one VoiceOver defect (EL-B1). Questions:
`docs/translation-questions/questions-el.md` (Q1…Q5).

## Glossary

| English | Greek | Status | Note |
|---|---|---|---|
| Callout | επεξήγηση | `confirmed` | Microsoft ("explanation"). Ask once |
| Audio Beacon | ηχητικό σήμα | `confirmed` | Microsoft |
| Marker | δείκτης | `confirmed` | Microsoft |
| Waypoint | σημείο πορείας | `confirmed` | Microsoft |
| Intersection | διασταύρωση | `confirmed` | Microsoft |
| Sleep / Snooze | αναστολή λειτουργίας ; αναβολή | `confirmed` | Microsoft |
| Detail levels | Λεπτομερές / Ισορροπημένο / Ήσυχο / Σιωπηλό | `unconfirmed` | AI |
| dead end | αδιέξοδο | `unconfirmed` | AI. «%1$s προς αδιέξοδο» reads acceptably |

## Rules

### EL-B1 — «για να» needs a subjunctive (`agreed` defect, `unconfirmed` wording)

`talkback_double_tap_template` «Πατήστε δύο φορές για να %1$s» needs the
subjunctive («για να θέσετε σε σίγαση»), but 28 of the 43 hints are
imperatives («θέστε σε σίγαση», «μετακινηθείτε»). A few are already
subjunctive («σας καθοδηγήσει»), so the hints are mixed. A **template fix
alone** (C13) would be «Πατήστε δύο φορές: %1$s», which reads correctly with
imperatives but oddly with the subjunctive ones. That's the rare case where
the hints themselves need aligning, and they should become imperatives,
which also suit Android TalkBack's own phrasing. Ask first.

### EL-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. VoiceOver: «Πατήστε δύο φορές: θέστε σε σίγαση το ηχητικό σήμα»? (EL-B1)
2. Callout «επεξήγηση»: natural for a short spoken description?
3. The four detail levels: clear?
4. Snooze «αναβολή»: clear?
5. Anything else.

## Provenance

**2024-07 → 2024-09 — Microsoft baseline** (`el-GR.lproj`). **2025 → 2026 —
AI passes.** **2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` «Σε λειτουργία αναβολής» → Microsoft's «Σε αναβολή».

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 10 were restored to Microsoft's wording and 8 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 10 uploaded and verified live.

**2026-09-25 — truncation repaired (C16).** `faq_battery_impact_answer`, `faq_background_battery_impact_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 25 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_platform` → «Πλατφόρμα σταθμού», `osm_dock` → «Δεξαμενή» (both had been «Αποβάθρα»), `osm_pavillion` → «Κιόσκι». Full stops removed where the English has none. Uploaded live.

**2026-09-29 — Full review of all 1586 units, 26 fixes uploaded.** The Weblate log shows no human Greek edits. The hints (EL-B1) were left for the reviewer. Fixed:
- **Microsoft's «αντιμετωπίζετε»** ('you confront') for "facing" → «κοιτάτε» in 4 help strings.
- **`faq_supported_phones_answer`:** a stale Android-only sentence, now with iOS 16 (C16).
- **`faq_battery_impact_answer`:** «Το μεγαλύτερο πρόγραμμα κατανάλωσης» → «Η μεγαλύτερη κατανάλωση».
- **English word order in 4 named-place templates:** «Σιδηροδρομικός σταθμός %1$s» etc.
- **`preview_include_unnamed_roads_title`:** «Δρόμοι χωρίς όνομα».
- **Typos:** «ένα ηχητικό σήμα», «επωφεληθώ», «Πώς», «τοποθετημένο».
- **Agreement:** «τους προσεγγίζετε».
- **`markers_marker_created`:** «Ο δείκτης δημιουργήθηκε».
- **Road templates:** «Στη %1$s» → «Στην %1$s» (4), matching `street_description_*`.
- **`menu_audio_tutorial`:** «Καθοδηγούμενος οδηγός».
- **Formatting:** *…* restored in `help_text_routes_content_how_1`; three stray capitals.

Uploaded with `--skip-validate`; all 26 re-fetched and matched exactly.
