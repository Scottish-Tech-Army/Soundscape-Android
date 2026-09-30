# Chinese, Simplified (zh_Hans) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (Weblate code `zh_Hans`, resource dir `values-zh-rCN`) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | 2026-02-17 (Benjamin Lin, one Weblate edit) |
| Register | **Mixed**: «您» 312, «你» 46. See ZH-R1 |

Read with [`_common.md`](_common.md).

## Status of this file

There is one native-speaker touch. Benjamin Lin (`cecc87ab4`, 2026-02-17)
made a small edit: he tightened `settings_explanation` and rewrote the three
intersection-direction strings (ZH-G1). Everything else comes from AI passes
since 2026-02-08. The authored Siri phrases (`zh-Hans.lproj`) match the help
text. Questions: `docs/translation-questions/questions-zh_Hans.md` (Q1…Q9).

## Glossary

| English | Chinese | Status | Note |
|---|---|---|---|
| Callout | 提示 (54) / 播报 (56) | `unconfirmed` | **Split corpus** (C12). See ZH-T1 |
| Audio Beacon | 音频信标 | `unconfirmed` | |
| Marker | 标记点 | `unconfirmed` | |
| Waypoint | 航点 | `unconfirmed` | An aviation/marine term. Amap and Baidu say «途经点». See ZH-T2 |
| Landmarks | 地标 | `unconfirmed` | |
| Intersection | 路口 | `unconfirmed` | |
| Sleep / Snooze | 休眠 / 小睡 | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | 详细 / 简化 / 基本 / 静音 | `unconfirmed` | English renamed 2026-09-30 (C22), names retranslated to match. Was 详细 / 平衡 / 简略 / 静音 |
| dead end | 死胡同 | `unconfirmed` | |
| goes left / right / ahead | 左转 / 右转 / 直行 | `unconfirmed` | Benjamin's wording. See ZH-G1 |

## Rules

### ZH-G1 — «左转» reads as "turn left" (`unconfirmed`, native speaker's own change)

Benjamin replaced «%s，向左延伸» ("…, extends to the left") with «%s，左转»
("…, turn left"), and did the same for right and ahead. C11 explains why an
instruction is wrong here: Soundscape describes which way each road *goes*
and never tells the user to turn. But «向左延伸» evidently sounded wrong to a
native ear. **Don't revert to it.** Ask for a third option that is both
natural and descriptive, such as «%s，通向左侧» or «%s在左侧». Record whatever
is decided under C8.

### ZH-R1 — «您» vs «你» (`unconfirmed`)

The 46 «你» are almost all in the newer Siri/voice-assistant strings
(`help_text_assistant_*`, `siri_desc_*`, `action_*`). The rest of the app
uses «您». Standardising on «您» is the obvious default, but spoken Siri
responses may be deliberately informal. Ask.

### ZH-T1 — Callout: «提示» or «播报» (`unconfirmed`)

The settings say «提示» («允许提示», «提示详细程度»), and the help/Siri text says
«自动播报». «播报» (announce/broadcast) is what Chinese navigation apps use
for spoken guidance. Pick one.

### ZH-T2 — Waypoint «航点» (`unconfirmed`)

34 occurrences. «途经点» is the consumer-map term (C1). The Siri route choices
in `Localizable.xcstrings` would move with it.

### ZH-C1 — Siri phrases are Chinese and live outside Weblate (`agreed`)

The same coupling as FR-C1.

## Rejected

Nothing yet. «向左延伸» is effectively rejected by Benjamin's edit. Record it
here once ZH-G1 is settled.

## Open questions

1. Intersections: «%s，左转» sounds like an instruction. Which is better:
   «%s，通向左侧», «%s在左侧», or something else? (ZH-G1)
2. «您» or «你»? (ZH-R1)
3. Callout: «提示» or «播报»? (ZH-T1)
4. Waypoint: «航点» or «途经点»? (ZH-T2)
5. «%1$s到%2$s» for "path to Moor Road": clear, or does it sound like "from
   the path to Moor Road"? (C10)
6. Siri phrases «Soundscape 周围 / 路线 / 信标…»: natural to say?
7. Beacon «音频信标»: natural? (AI-only term, asked for confirmation)
8. **The four detail levels** (详细 / 简化 / 基本 / 静音), renamed 2026-09-30 (C22) and retranslated literally: distinct by ear? Better names for the middle two?
9. Anything else.

## Provenance

**2026-02-08 → 2026-09-24 — AI passes**, plus one native edit
(`cecc87ab4`, Benjamin Lin, 2026-02-17).
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 25 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` already gave the iOS menu path in English, so it now reads Accessibility > Read & Speak > Voices, with the "(In iOS versions prior to 26…)" note translated. Uploaded and validated.

**2026-09-28 — Weblate checks pass.** Beacon styles translated (Dave's decision, as for Romanian): 闪耀、微光、触感、叮、掉落、信号、木槌, all `unconfirmed`. In the Siri example, «Soundscape 周围 周围» repeats because the group and the option share a name in the iOS phrases. Uploaded live.

**2026-09-29 — full review.** 5 fixes. Home buttons «我的周围»/«我的前方» → «周围»/«前方», the names in the help page titles, 17 help and tour strings and the Siri phrases (the labels lose the two-line break). «原始模式» → «经典模式», the setting's label. «*取消静音信标按钮*» → «*取消静音信标*按钮» (2). Live strings re-checked before upload; uploaded with `--skip-validate` and verified live. **Held:** ZH-G1, ZH-R1, ZH-T1, ZH-T2.
