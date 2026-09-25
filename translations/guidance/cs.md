# Czech (cs) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Last native-speaker input | **none yet** |
| Register | Formal «vy» (vykání), `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Czech.** It has been AI-only since
2026-08-21. It is good work structurally:
- The hints are second-person future forms («ztlumíte»), which fit «Dvojitým
  klepnutím %1$s» (C13).
- Traveling/Heading are «Jízda»/«Chůze», which is exactly the vehicle/walking
  split (see `bg.md` BG-T4).
- «%1$s, vede doleva» is descriptive (C11).

One `agreed` defect. Questions: `docs/translation-questions/questions-cs.md` (Q1…Q6).

## Glossary

| English | Czech | Status | Note |
|---|---|---|---|
| Callout | hlášení | `unconfirmed` | Consistent |
| Audio Beacon | zvukový maják | `unconfirmed` | |
| Marker | značka | `unconfirmed` | |
| Waypoint | trasový bod | `unconfirmed` | Ask what Czech map apps (Mapy.cz, Google Maps) call it (C1). See Q2 |
| Landmarks | orientační body | `unconfirmed` | |
| Intersection | křižovatka | `unconfirmed` | |
| Sleep / Snooze | Spánek / Spí ; Dřímá | `unconfirmed` | |
| Detail levels | Podrobný / Vyvážený / Tichý / Bez zvuku | `unconfirmed` | Distinct |
| dead end | slepá ulice | `unconfirmed` word; case fixed 2026-09-24. See CS-G1 |

## Rules

### CS-G1 — «do slepá ulice» must be genitive «do slepé ulice» (`confirmed` fixed 2026-09-24)

**Fixed 2026-09-24:** `confect_name_dead_end` is now «slepé ulice», uploaded to Weblate and verified live.

Rule C9, and Czech is on its list. One string.

### CS-R1 — «Jste připraveni!» is plural (`unconfirmed`)

In vykání, adjectives and participles take the *singular* with the
addressee's real gender («Jste připravený/připravená»). The plural
«připraveni» addresses a group. Apps often use it as a gender-neutral dodge,
and it avoids slashes. Ask whether it sounds acceptable, or whether a
rephrase («Vše je připraveno!») is better.

### CS-C1 — Siri phrases stay in English (`agreed`)

There is no `cs.lproj`. See PL-C1.

## Rejected

Nothing yet.

## Open questions

1. Callout «hlášení»: natural?
2. Waypoint «trasový bod»: what do Czech map apps call a stop along a route?
3. «Jste připraveni!»: OK, or «Vše je připraveno!»? (CS-R1)
4. Snooze «Dřímá»: clear?
5. Is vykání right?
6. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.**
**2026-09-24 — corpus sweep.** Nothing uploaded.

**2026-09-24 — dead-end case fix applied.** `confect_name_dead_end` → «slepé ulice» (the C9 batch fix across ru, cs, sk, hr, sr, sl, pl and is).
