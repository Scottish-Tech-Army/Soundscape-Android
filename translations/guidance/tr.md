# Turkish (tr) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2026-02-17 (Toro Inoue, ~36 lines) and 2026-08-21 (Oğuz Ersen, 1 line), both via Weblate |
| Register | Formal «siz» («Hazırsınız!»), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

Turkish has two small human contributions (Toro Inoue added the language in
2026-02, and Oğuz Ersen is an active Turkish Weblate translator). The rest
is AI. The corpus has **the most serious structural problem found in any
language**: suffixes hard-coded onto placeholders (TR-G1). The authored Siri
phrases (`tr.lproj`) match the help text. Questions:
`translations/review/tr.md` (Q1…Q6).

## Glossary

| English | Turkish | Status | Note |
|---|---|---|---|
| Callout | anons | `unconfirmed` | |
| Audio Beacon | Sesli İşaret | `unconfirmed` | |
| Marker | Kayıtlı Nokta | `unconfirmed` | "Saved point". A nice descriptive choice |
| Waypoint | Ara Nokta | `unconfirmed` | |
| Landmarks | Simge Yapılar | `unconfirmed` | |
| Intersection | kavşak | `unconfirmed` | |
| Sleep / Snooze | Uyku Modu ; Erteleme Modu | `unconfirmed` | |
| Detail levels | Ayrıntılı / Dengeli / Sakin / Sessiz | `unconfirmed` | |
| dead end | çıkmaz sokak | `unconfirmed` | |

## Rules

### TR-G1 — Suffixes hard-coded onto placeholders (`agreed` defect, design needed)

17 strings attach a fixed case suffix to a substituted name or number:
«%1$s'de», «%2$s'e yakın», «%1$s'den uzaklaşıyorsunuz», «%1$s'ın yanındaki
kaldırım», and route progress «%3$s'ın %2$s. ara noktasında» with a
*number*. Turkish suffixes follow vowel harmony and consonant changes
determined by the word they attach to. A fixed «'de» is wrong for roughly
half of all names («İstanbul'de» should be «İstanbul'da»), and for numbers
it depends on how the number is pronounced («3'ın» should be «3'ün»).

This can't be fixed per string by translation. The options are:
- Restructure each template so the name is never suffixed: postpositions
  after a nominative («%1$s yakınında» already works), or a colon/comma
  frame («Konum: %1$s»).
- Add code that picks the suffix from the substituted word's last vowel.
  That's a real feature, but it's what Turkish apps do.

It's also a C10 case: `confect_name_to` «%1$s'tan %2$s'a» means "from X to
Y". Ask a speaker which templates can be restructured before touching code.

### TR-B1 — Mixed hint forms (`agreed` defect, `unconfirmed` wording)

The template «%1$s için çift dokunun» needs a «-mek/-mak» verbal noun before
«için», and 21 of the 43 hints have one («ilerlemek»). The others are
imperatives («sesli işareti sessize al»), which give «…sessize al için çift
dokunun». This is the rare case where the **hints** should change, to the
«-mek» form. That form likely also suits Android TalkBack's Turkish frame, but ask a
TalkBack user to confirm before sweeping, since the hints feed both
platforms (C13).

### TR-C1 — Siri phrases are Turkish and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet.

## Open questions

1. Place names with fixed suffixes («İstanbul'de»): how bad does it sound,
   and which sentences could be rephrased so the name takes no suffix? (TR-G1)
2. «Moor Road'a giden patika» for "path to Moor Road"? (TR-G1, C10)
3. VoiceOver: «sesli işareti sessize almak için çift dokunun»? (TR-B1)
4. Beacon «Sesli İşaret»: natural?
5. Siri phrases: natural?
6. Anything else.

## Provenance

**2026-02-09 — Toro Inoue** added Turkish and made small edits. **2026-08-21
— Oğuz Ersen**, one line. **2026 — AI passes.** **2026-09-24 — corpus
sweep.** Nothing uploaded.
