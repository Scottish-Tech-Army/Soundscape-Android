# Bengali (bn) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «আপনি» («আপনি প্রস্তুত!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Bengali.** It has been AI-only since
2026-08-21. Structurally sound: the VoiceOver hints are «-তে» infinitives
that compose correctly with «%1$s ডাবল ট্যাপ করুন», and `confect_name_to`
«%2$s পর্যন্ত %1$s» avoids the C10 misreading. The open items are term
choices. Questions: `translations/review/bn.md` (Q1…Q7).

## Glossary

| English | Bengali | Status | Note |
|---|---|---|---|
| Callout | কলআউট (66) / ঘোষণা (33) | `unconfirmed` | **Split corpus**. Even the settings disagree: «ঘোষণা চালু করুন» vs «কলআউট বিবরণ». See BN-T1 |
| Audio Beacon | অডিও বীকন | `unconfirmed` | Loanword (C12) |
| Marker | মার্কার | `unconfirmed` | |
| Waypoint | ওয়েপয়েন্ট | `unconfirmed` | Loanword. See Q3 |
| Landmarks | ল্যান্ডমার্ক | `unconfirmed` | |
| Intersection | মোড় | `unconfirmed` | |
| Sleep / Snooze | ঘুম / স্নুজ | `unconfirmed` | «ঘুম» is a noun on a button. See Q4 |
| Detail levels | বিস্তারিত / ভারসাম্যপূর্ণ / শান্ত / নীরব | `unconfirmed` | |
| dead end | শেষ প্রান্ত | `unconfirmed` | "Far end", which may not mean a dead-end street. «কানাগলি» is the usual word. See BN-T2 |

## Rules

### BN-T1 — Pick one word for Callout (`unconfirmed`)

The corpus uses two words for the same concept (C12). Whichever wins, sweep
the other: about 33 or 66 strings, including verb forms (C3).

### BN-T2 — «শেষ প্রান্ত» for dead end (`unconfirmed`)

This lands in `confect_name_to` as «শেষ প্রান্ত পর্যন্ত পথ», which reads as
"path up to the far end". «কানাগলি» (a blind alley) is the usual term. Ask.

### BN-C1 — Siri phrases stay in English (`agreed`)

There is no `bn.lproj`. See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout: «কলআউট» or «ঘোষণা»? (BN-T1)
2. Beacon «অডিও বীকন»: understood?
3. Waypoint: «ওয়েপয়েন্ট» or a Bengali word?
4. Sleep/Snooze: «ঘুম» / «স্নুজ»: natural as button and status labels?
5. Dead end: «শেষ প্রান্ত» or «কানাগলি»? (BN-T2)
6. Is «আপনি» right?
7. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
