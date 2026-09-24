# Thai (th) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | «คุณ» (438), polite and neutral, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Thai.** It has been AI-only since
2026-08-22. The authored Siri phrases (`iosApp/iosApp/th.lproj/AppShortcuts.strings`)
match the help text. The main risk is two detail levels that sound too alike.
Questions: `translations/review/th.md` (Q1…Q7).

## Glossary

| English | Thai | Status | Note |
|---|---|---|---|
| Callout | การแจ้งเตือน(ด้วยเสียง) | `unconfirmed` | 67. «การแจ้งเตือน» is also Android's word for notifications. See Q1 |
| Audio Beacon | บีคอนเสียง | `unconfirmed` | |
| Marker | หมุด | `unconfirmed` | "Pin". Natural |
| Waypoint | จุดผ่านทาง | `unconfirmed` | |
| Landmarks | จุดสังเกต | `unconfirmed` | |
| Intersection | ทางแยก | `unconfirmed` | |
| Sleep / Snooze | สลีป / สนูซ | `unconfirmed` | Loanwords (C12) |
| Detail levels | ละเอียด / สมดุล / **เงียบ / เงียบสนิท** | `unconfirmed` | See TH-T1 |
| dead end | ทางตัน | `unconfirmed` | |

## Rules

### TH-T1 — Quiet and Silent share a root (`unconfirmed`)

«เงียบ» (quiet) vs «เงียบสนิท» (completely quiet). This is the Polish PL-T1
problem again, and worse, because one is a prefix of the other. English
"Quiet" means *fewer* callouts, not almost no sound. «กระชับ» or «สั้น»
("brief", like mr/te/ur/ko) would separate them. 14 strings contain
«เงียบ»: the two level names, `callouts_verbosity_description`,
`action_no_such_callout_detail`, the `help_text_automatic_callouts_*` and
`help_text_assistant_commands*` help, and three FAQ entries. Some of these
use «เงียบ» in its ordinary sense (`faq_tip_beacon_quiet`,
`help_text_remote_control_how`), so check each one before sweeping (C5). `Localizable.xcstrings` has no Thai level
names (checked 2026-09-24), so nothing outside Weblate moves.

### TH-C1 — Siri phrases are Thai and live outside Weblate (`agreed`)

The same coupling as FR-C1: a phrase change must land in `th.lproj`, in
`Localizable.xcstrings` and in the two `help_text_assistant_*_ios` strings.

## Rejected

Nothing yet.

## Open questions

1. Callout «การแจ้งเตือน»: confused with phone notifications?
2. «เงียบ» vs «เงียบสนิท»: different enough by ear? (TH-T1)
3. Waypoint «จุดผ่านทาง»: natural?
4. Sleep/Snooze «สลีป» / «สนูซ»: understood?
5. Siri phrases «Soundscape รอบตัว / เส้นทาง / บีคอน…»: natural to say?
6. Is «คุณ» right?
7. Anything else.

## Provenance

**2026-08-22 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.
