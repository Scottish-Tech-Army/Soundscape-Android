# Punjabi (pa) — translation decisions

| | |
|---|---|
| Resource directory | `values-pa` (Gurmukhi script) |
| Corpus when added | 1598 units (2026-09-30) |
| Last native-speaker input | **none yet** |
| Register | Respectful «ਤੁਸੀਂ» throughout, `unconfirmed` |

Read with [`_common.md`](_common.md).

## Status of this file

**No native speaker has reviewed Punjabi.** The whole corpus was machine-written on
2026-09-30 in one pass, with the Hindi translation as a structural reference (Punjabi
postposition order follows Hindi's closely). **Treat every term as a guess until a
speaker has seen it.** Script is Gurmukhi (Indian Punjabi); Shahmukhi (Pakistani
Punjabi, `pa-rPK`/`pnb`) is not covered.

The a11y template `talkback_double_tap_template` is «%1$s ਦੋ ਵਾਰ ਟੈਪ ਕਰੋ», so every
`*_hint` is a purpose clause ending «…ਲਈ» («ਮਾਰਕਰ ਦਾ ਨਾਮ ਸੋਧਣ ਲਈ» → «ਮਾਰਕਰ ਦਾ ਨਾਮ
ਸੋਧਣ ਲਈ ਦੋ ਵਾਰ ਟੈਪ ਕਰੋ»), per C13.

There is no `pa.lproj`, so Siri phrases stay in English; the help and FAQ text quotes
them in English inside “ ”.

## Plurals

CLDR `pa`: `one` (n = 0..1), `other`. Compose Resources knows the locale.

## Glossary

| English | Punjabi | Status | Note |
|---|---|---|---|
| Callout | ਘੋਸ਼ਣਾ (pl. ਘੋਸ਼ਣਾਵਾਂ) | `unconfirmed` | "Announcement". AI-only term |
| Audio Beacon | ਆਡੀਓ ਬੀਕਨ / ਬੀਕਨ | `unconfirmed` | Loanword. AI-only term |
| Marker | ਮਾਰਕਰ | `unconfirmed` | Loanword |
| Route | ਰੂਟ | `unconfirmed` | Loanword; ਰਸਤਾ is used for a physical path |
| Waypoint | ਮਾਰਗ ਬਿੰਦੂ | `unconfirmed` | |
| Intersection | ਚੌਂਕ | `unconfirmed` | |
| Landmark | ਲੈਂਡਮਾਰਕ | `unconfirmed` | |
| dead end | ਬੰਦ ਗਲੀ | `unconfirmed` | |
| Sleep / Snooze | ਸਲੀਪ / ਸਨੂਜ਼ | `unconfirmed` | Loanwords, as on Android phones in Punjabi |
| Detail levels | ਵਿਸਤ੍ਰਿਤ / ਸਰਲ / ਜ਼ਰੂਰੀ / ਚੁੱਪ | `unconfirmed` | Detailed / Simplified / Essential / Silent, literal per C22 |
| Settings | ਸੈਟਿੰਗਾਂ | `unconfirmed` | |
| Location | ਟਿਕਾਣਾ | `unconfirmed` | |
| Crosswalk | ਪੈਦਲ ਲਾਂਘਾ | `unconfirmed` | |
| Clock position | «%1$s ਵਜੇ ਦੀ ਦਿਸ਼ਾ ਵਿੱਚ», hours as digits | `unconfirmed` | C20: digits read naturally here |

## Rules

### PA-S1 — street templates follow Hindi order (`unconfirmed`)

`confect_name_to` «%1$s, %2$s ਵੱਲ», `confect_name_via` «%1$s, %2$s ਰਾਹੀਂ»,
`confect_name_next_to` «%2$s ਦੇ ਨਾਲ %1$s», dead-end templates «%1$s, ਬੰਦ ਗਲੀ ਵੱਲ».
Map names are substituted undeclined, so postpositions always follow the name.

## Rejected

Nothing yet.

## Open questions

1. Is the Punjabi generally understandable, or does it read as machine translation (or as Hindi in Gurmukhi)?
2. Callout «ਘੋਸ਼ਣਾ» and Audio Beacon «ਆਡੀਓ ਬੀਕਨ»: natural? (AI-only terms, asked for confirmation)
3. Loanwords «ਮਾਰਕਰ», «ਰੂਟ», «ਸਲੀਪ», «ਸਨੂਜ਼»: would native Punjabi words be clearer?
4. **The four detail levels** (ਵਿਸਤ੍ਰਿਤ / ਸਰਲ / ਜ਼ਰੂਰੀ / ਚੁੱਪ): distinct by ear?
5. Anything else.

## Provenance

**2026-09-30 — language added**, whole corpus AI-translated via `/add-language`.
No questionnaire published yet.
