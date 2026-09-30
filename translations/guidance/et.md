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
phrases stay in English. Questions: `docs/translation-questions/questions-et.md` (Q1…Q7).

## Glossary

| English | Estonian | Status | Note |
|---|---|---|---|
| Audio Beacon | helimajakas | `confirmed` | Priit Jõerüüt |
| Callout | häälteade | `unconfirmed` | |
| Marker | marker | `unconfirmed` | |
| Waypoint | teekonnapunkt | `unconfirmed` | |
| Intersection | ristmik | `unconfirmed` | |
| Sleep / Snooze | Unerežiim ; Uinak | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | Üksikasjalik / Lihtsustatud / Põhiline / Hääletu | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was Üksikasjalik / Tasakaalustatud / Vaikne / Hääletu |
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

### ET-G2 — Road templates named the road type before the name (`fixed` in code, 2026-09-25)

11 templates wrote «Teel %1$s» / «Tänaval %1$s» ("on the road X"), a label that
avoids inflecting the name but reads backwards, and doubles the type for names
that carry one: «Teel Pärnu maantee», «Tänaval Metsa». They now write «{Teel
%1$s}» / «{Tänaval %1$s}», and `resolveGrammarMarkers()` (C18) produces «Pärnu
maanteel», «Kalda põigul», «Metsa tänaval». Estonian map data drops «tänav» from
street names, so a single capitalised word is treated as a «tänav» street.
Measured on the Tallinn extract: 93% of 5,765 names covered; the rest keep the
label. **New road templates must use the wrapped form.**

## Open questions

1. Register: «sina» or «teie»? The app currently mixes them. (ET-R1)
2. «Rada kuni ummiktee»: how should "path to a dead end" be said? (ET-G1)
3. «Liigub põhja suunas»: natural, or «Sõidate…»? (ET-S1)
4. Callout «häälteade»: natural?
5. Snooze «Uinak»: clear?
6. Street names are now inflected («Pärnu maanteel», «Metsa tänaval») instead of «Teel X». Right? Especially: is every single-word name a «tänav»? (ET-G2)
7. Anything else.

## Provenance

**2025-11-08 → 2026-01-21 — Priit Jõerüüt** (Weblate). **2026 — AI passes.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** `osm_field_hockey` → «Rohuhoki», `osm_dock` → «Dokk». Uploaded live.

**2026-09-29 — Stale beacon FAQ fixed (C16).** `faq_how_to_use_beacon_answer` had been translated from an older English. Three passages were replaced to match the current source:
- the sailboat-tacking sentence, now "you may still need to make navigation choices along the way to work around obstacles";
- "turn the phone slowly", now "slowly turn in a circle";
- "the lighthouse metaphor … has natural implications", now "This design has a few natural results".

The extra *…* pair around the "tacks" word went with the sentence. The rest of the text was left unchanged. The new wording is `unconfirmed`. Found by a cross-language check after the bg/hr reviews. Uploaded and verified live.

**2026-09-29 — European batch review.** 9 hints were imperatives or 3sg («lisa», «muuda», «Kuva», «muudab», «ahenda», «laienda») → da-infinitives. `help_text_destination_beacons_how_3` «Vaigista helimajakas» → «Summuta helimajakas», the label. 10 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.
