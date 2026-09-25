# Hindi (hi) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «आप» throughout (394, no «तुम»). Consistent, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Hindi.** The history goes back to 2026-02 and
is all AI passes plus five "Anonymous" Weblate commits, which are mechanical
(copyright placeholders, whitespace). Everything is `unconfirmed` except the
two grammar defects, HI-B1 and HI-G1, whose *diagnosis* is `agreed`.

Questions for reviewers: `docs/translation-questions/questions-hi.md` (Q1…Q8).

## Glossary

| English | Hindi | Status | Note |
|---|---|---|---|
| Callout | कॉलआउट (77) / घोषणा (13) | `unconfirmed` | Split corpus (C12). See Q1 |
| Audio Beacon | ऑडियो बीकन | `unconfirmed` | Loanword. See Q2 |
| Marker | मार्कर | `unconfirmed` | |
| Waypoint | मार्ग बिंदु | `unconfirmed` | Native coinage. See Q3 |
| Landmarks | लैंडमार्क | `unconfirmed` | |
| Intersection | चौराहा | `unconfirmed` | |
| Sleep / Snooze | स्लीप / स्नूज़ मोड | `unconfirmed` | Loanwords. See Q4 |
| Detail levels | विस्तृत / संतुलित / शांत / मौन | `unconfirmed` | Distinct by ear |
| dead end | बंद गली | `unconfirmed` | Common Hindi for a dead-end lane |
| Traveling / Heading | यात्रा / चलते हुए | `agreed` | Correct: Heading is only used when walking (see `bg.md` BG-T4) |

## Rules

### HI-B1 — VoiceOver hints read as two commands (`agreed` defect, `unconfirmed` wording)

`talkback_double_tap_template` is «दो बार टैप करें %1$s» ("tap twice
%1$s"), and the ~40 `*_hint` fragments substituted into it are full
imperatives (`beacon_action_mute_beacon_acc_hint` «ऑडियो बीकन म्यूट करें»).
VoiceOver on iOS therefore says «दो बार टैप करें ऑडियो बीकन म्यूट करें», two
commands back to back. The English comment explicitly asks for an
infinitive fragment placed where the grammar needs it.

**Fix the template first (see `_common.md` C13).** The same hints are
TalkBack's action labels on Android, so rewriting ~40 of them to suit iOS
would change what Android says too. A frame that fits the existing
imperatives, e.g. «%1$s — इसके लिए दो बार टैप करें» ("mute the beacon: tap
twice for this"), is a one-string, iOS-only fix. Rewriting the hints into
the oblique infinitive («ऑडियो बीकन म्यूट करने के लिए दो बार टैप करें», as
bn/ta/ur do) is cleaner, but only do it once a Hindi TalkBack user confirms
it also reads well on Android. See Q5.

### HI-G1 — `confect_name_to` says "from … to" (`agreed` defect, `unconfirmed` wording)

«%1$s से %2$s तक» means "from the path to Moor Road". See `_common.md` C10.
Candidate: «%2$s तक जाने वाला %1$s». The same applies to `confect_name_to_via`.

### HI-C1 — Siri phrases stay in English (`agreed`)

There is no `hi.lproj/AppShortcuts.strings`, so the help text quotes the
English commands. Keep it that way (see PL-C1).

## Rejected

Nothing yet.

## Open questions

1. Callout: «कॉलआउट» or «घोषणा»? The corpus uses both.
2. Beacon: is «ऑडियो बीकन» understood, or is there a Hindi word?
3. Waypoint «मार्ग बिंदु»: natural? What do your map apps say?
4. Sleep/Snooze: «स्लीप» / «स्नूज़» are loanwords. Are they what your phone says?
5. VoiceOver: «… म्यूट करने के लिए दो बार टैप करें» or «… म्यूट करें — इसके लिए दो बार
   टैप करें»? And what does Android TalkBack say around the same hint? (HI-B1, C13)
6. «%2$s तक जाने वाला %1$s» for "path to Moor Road"? (HI-G1)
7. Is «आप» the right register?
8. Anything else.

## Provenance

**2026-02-08 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Found HI-B1 by reading the hint strings
against the template, and HI-G1 as part of the C10 cross-language check.
Nothing uploaded.

**2026-09-25 — truncation repaired (C16).** `faq_what_can_I_set_answer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.
