# Estonian (et) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2026-01-21 (Priit Jõerüüt, Weblate, ~30 lines, including «Helimajakas») |
| Register | **Mixed** «sina» / «teie». See ET-R1 |

Read with [`_common.md`](_common.md).

## Status of this file

Priit Jõerüüt added Estonian in Weblate (2025-11-08) and made a few edits,
including Beacon = «Helimajakas», which is `confirmed`. Everything else is
AI. The hints are «da»-infinitives («summutada helimajakas») that compose
correctly with «Topeltkoputa, et %1$s». There is no `et.lproj`, so the Siri
phrases stay in English. Questions: `docs/translation-questions/questions-et.md` (Q1…Q6).

## Glossary

| English | Estonian | Status | Note |
|---|---|---|---|
| Audio Beacon | helimajakas | `confirmed` | Priit Jõerüüt |
| Callout | häälteade | `unconfirmed` | |
| Marker | marker | `unconfirmed` | |
| Waypoint | teekonnapunkt | `unconfirmed` | |
| Intersection | ristmik | `unconfirmed` | |
| Sleep / Snooze | Unerežiim ; Uinak | `unconfirmed` | |
| Detail levels | Üksikasjalik / Tasakaalustatud / Vaikne / Hääletu | `unconfirmed` | |
| dead end | ummiktee | `unconfirmed` | See ET-G1 |

## Rules

### ET-R1 — Mixed register (`unconfirmed`)

«sina/sa/sinu» in errors, hints, the tutorial and the assistant help;
«teie/Olete» in the welcome, the iOS permission prompts and parts of the
help. Pick one and sweep. Estonian apps are commonly informal, but ask.

### ET-G1 — «kuni» reads as "up until" (`unconfirmed`)

`confect_name_to` «%1$s kuni %2$s» gives «Rada kuni ummiktee», a range
("path up to the dead end"), close to the C10 misreading. «%2$s viiv %1$s»
("path leading to…") may be better, but «viiv» then needs the name in the
illative. Ask.

### ET-S1 — «Liigub põhja suunas» has no subject (`unconfirmed`)

The same as RU-S1. Traveling (in a vehicle) is third person without a
subject, while Heading «Kõnnib…» is also third person. «Sõidate põhja
suunas» / «Kõnnite…» would address the user.

## Rejected

Nothing yet.

## Open questions

1. Register: «sina» or «teie»? The app currently mixes them. (ET-R1)
2. «Rada kuni ummiktee»: how should "path to a dead end" be said? (ET-G1)
3. «Liigub põhja suunas»: natural, or «Sõidate…»? (ET-S1)
4. Callout «häälteade»: natural?
5. Snooze «Uinak»: clear?
6. Anything else.

## Provenance

**2025-11-08 → 2026-01-21 — Priit Jõerüüt** (Weblate). **2026 — AI passes.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
