# Translation guidance

Per-language **decisions**, derived from native-speaker feedback. Not a log of
what users said — a record of what we concluded, so that every later
translation and review pass starts from it instead of rediscovering it.

- `_common.md` — rules that apply to every language.
- `<code>.md` — one file per Weblate language code (`uk.md`, `fr_CA.md`, …).

`weblate-translate` and `weblate-review` load `_common.md` plus the relevant
`<code>.md` automatically. `weblate-feedback` is what writes to them.

These files live outside `docs/` on purpose: `docs/` is the Jekyll source tree
and a file added there becomes a published page.

## Why decisions and not a feedback log

A user only ever sees a fraction of the strings, so their report is a *sample*
of a problem, not the problem. One reported string usually implies a rule, and
the rule usually reaches strings they never saw. The file records the rule and
its scope; the sweep finds the rest.

Record rejected suggestions too, with the reasoning. Otherwise the next pass —
human or AI — "fixes" them straight back.

## Statuses

Each decision carries one:

| Status | Meaning | Sweep behaviour |
|---|---|---|
| `confirmed` | A native speaker approved it and it matches what's shipping | Guard it — flag any string that drifts away |
| `agreed` | Decided, sweep it across the corpus | Generate concrete fixes |
| `unconfirmed` | Proposed, but the scope or the grammar needs a second round | Inventory only, no fixes |
| `provisional` | The reporter themselves flagged it as tentative | Inventory only, no fixes |
| `rejected` | Considered and turned down | Flag if someone reintroduces it |

`unconfirmed` and `provisional` findings are written with `suggested` equal to
`current`, which is the existing signal `weblate-review`'s apply step reads as
"skip, don't upload".
