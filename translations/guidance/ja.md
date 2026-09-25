# Japanese (ja) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none recorded** |
| Register | Polite です/ます, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

Japanese is the oldest non-English language in the repo. The first strings
arrived in 2024-07 with the prototype (`53d4f20d5`), then grew through
developer commits (Adam Ward, Fanny Demey) and AI passes. **None of those
authors is a recorded Japanese translator.** The early strings **come from Microsoft's
professional ja-JP localisation** of the iOS app (verified 2026-09-24, see
`_common.md` C14). 209 of those ~360 keys are still Microsoft's wording, and
27 have drifted even though their English is unchanged. Microsoft's terms
are the default when a term is disputed. Everything Microsoft never had is
AI. Treat everything as `unconfirmed` until a speaker weighs in.

The authored Siri phrases (`ja.lproj`) match the help text. Questions:
`docs/translation-questions/questions-ja.md` (Q1…Q8).

## Glossary

| English | Japanese | Status | Note |
|---|---|---|---|
| Callout | コールアウト (89) / 読み上げ (20) / 案内 (14) | `unconfirmed` | **Three words** (C12). See JA-T1 |
| Audio Beacon | 音声ビーコン | `unconfirmed` | |
| Marker | マーカー | `unconfirmed` | |
| Waypoint | ウェイポイント | `unconfirmed` | Japanese map apps usually say «経由地». See Q3 |
| Landmarks | ランドマーク | `unconfirmed` | |
| Intersection | 交差点 | `unconfirmed` | |
| Sleep / Snooze | スリープ / スヌーズ | `unconfirmed` | Standard loanwords |
| Detail levels | 詳細 / バランス / 控えめ / 無音 | `unconfirmed` | Distinct |
| dead end | 行き止まり | `unconfirmed` | |

## Rules

### JA-G1 — `confect_name_to` says "from … to" (`agreed` defect, `unconfirmed` wording)

«%1$s から %2$s へ» means "from the path to Moor Road". See C10. Candidate:
«%2$s へ続く%1$s». The same applies to `_via`.

### JA-T1 — Three words for Callout (`unconfirmed`)

«コールアウト» is an English UI term that most Japanese users won't know.
«読み上げ» (reading aloud) and «案内» (guidance) are both natural. Pick one
and sweep, noting that «読み上げ» also means screen-reader output.

### JA-C1 — Siri phrases are Japanese and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout: «コールアウト», «読み上げ» or «案内»? (JA-T1)
2. «%2$s へ続く%1$s» for "path to Moor Road"? (JA-G1)
3. Waypoint: «ウェイポイント» or «経由地»?
4. Do the four levels 詳細 / バランス / 控えめ / 無音 work by ear?
5. Siri phrases «Soundscape 周辺 / ルート / ビーコン…»: natural to say?
6. Is the です/ます register right?
7. Beacon «音声ビーコン»: natural? (AI-only term, asked for confirmation)
8. Anything else.

## Provenance

**2024-07-21 → 2026-09-24.** Prototype strings (source unknown), developer
commits, then AI passes. No recorded translator.
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-25 — Microsoft drift pass (C14), approved by Dave.** Of the strings whose English is unchanged from Microsoft's but whose translation had drifted, 22 were restored to Microsoft's wording and 5 kept (Microsoft errors, recorded decisions, or unifications of Microsoft's own inconsistent terms). 22 uploaded and verified live.

**2026-09-25 — truncation repaired (C16).** `help_text_automatic_callouts_what` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
