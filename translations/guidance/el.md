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
`translations/review/el.md` (Q1…Q5).

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
