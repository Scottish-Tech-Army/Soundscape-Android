# Tamil (ta) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «நீங்கள்» («நீங்கள் தயார்!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Tamil.** It has been AI-only since
2026-08-22. Callout is consistently the native «அறிவிப்பு» (65). The
VoiceOver hints are «-க்க» infinitives that compose correctly with «%1$s
இரட்டை தட்டவும்». One `agreed` grammar defect: TA-G1. Questions:
`docs/translation-questions/questions-ta.md` (Q1…Q7).

## Glossary

| English | Tamil | Status | Note |
|---|---|---|---|
| Callout | அறிவிப்பு | `unconfirmed` | Consistent. Also the everyday word for a phone notification. See Q1 |
| Audio Beacon | ஒலி பீக்கன் | `unconfirmed` | Half native, half loanword |
| Marker | குறிப்பான் | `unconfirmed` | |
| Waypoint | வழிப்புள்ளி | `unconfirmed` | |
| Landmarks | அடையாளக் குறிகள் | `unconfirmed` | |
| Intersection | சந்திப்பு | `unconfirmed` | |
| Sleep / Snooze | உறக்கம் / ஸ்னூஸ் | `unconfirmed` | |
| Detail levels | விரிவு / சமநிலை / அமைதி / மௌனம் | `unconfirmed` | **அமைதி (calm/quiet) and மௌனம் (silence) are close in meaning**. See TA-T1 |
| dead end | முட்டுச்சந்து | `unconfirmed` | |

## Rules

### TA-G1 — `confect_name_to` says "from … to" (`agreed` defect, `unconfirmed` wording)

«%1$s முதல் %2$s வரை» means "from the path up to Moor Road". See C10.
Candidate: «%2$s நோக்கிச் செல்லும் %1$s». The same applies to `_via`.

### TA-T1 — Quiet vs Silent may not be distinct enough (`unconfirmed`)

The user picks these by ear. «அமைதி» and «மௌனம்» are both "quiet/silence"
words. English "Quiet" here means *fewer* callouts, not no sound. Something
like «சுருக்கம்» ("brief", which is what mr, te and ur chose) may separate
them better.

### TA-C1 — Siri phrases stay in English (`agreed`)

See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «அறிவிப்பு»: confused with phone notifications?
2. Beacon «ஒலி பீக்கன்»: understood?
3. Waypoint «வழிப்புள்ளி»: natural?
4. Quiet «அமைதி» vs Silent «மௌனம்»: different enough? (TA-T1)
5. «%2$s நோக்கிச் செல்லும் %1$s» for "path to Moor Road"? (TA-G1)
6. Is «நீங்கள்» right?
7. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
