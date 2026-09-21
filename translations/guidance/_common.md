# Cross-language translation rules

Rules that apply to every language. Derived from real feedback — each one
names the case that produced it, because the case is usually more convincing
than the rule.

---

## C1 — The terminology doc lists *glosses*, not preferred translations

`docs/developers/translation-terminology.md` offers alternates like "route
point" for Waypoint and "sound beacon" for Audio Beacon. Those exist to pin
down the **concept in English** for someone who has never used the app. They
are not a shortlist to translate from.

Prefer whatever the target language's own mapping and navigation apps already
use for the concept. A user recognises the word they have already learned
elsewhere; a faithful calque of an English gloss is something they have to
decode.

> **Case (uk, 2026-09-18):** Ukrainian took the doc's "route point" gloss
> literally and shipped «маршрутна точка» in 26 strings. A native speaker
> flagged it and pointed at Google Maps Ukrainian, which uses «зупинка». The
> translation was faithful to our documentation and still wrong for users.

## C2 — Watch for tautologies the English can't show you

An English modifier+noun pair can collapse into a tautology when the target
language's noun already carries the modifier. The English reads fine, so
nothing flags it; only a native speaker hears it.

When a source string pairs a modifier with a domain noun (new/current/existing
+ release, update, version, location, setting), check whether the target noun
already implies it, and drop the modifier if so.

> **Case (uk, 2026-09-18):** "New release information" became «Інформація про
> нове оновлення». Ukrainian renders both *release* and *update* as
> «оновлення», so the string read "information about the new update" —
> a tautology. Fix was to drop «нове».

## C3 — A term decision is only mechanical if the term is never a verb

Before treating a term change as find-and-replace, search the corpus for
**derived forms** — verbs, participles, adverbs built off the current term.
Those need the sentence rewritten, not the word swapped, and they are where a
bulk substitution produces text no native speaker would write.

Split the sweep into "noun forms, mechanical" and "derived forms, needs a
human" and report the two counts separately. A term change that looks like 30
easy edits is often 20 easy ones and 10 rewrites.

> **Case (uk, 2026-09-18):** Callout «оголошення» → «підказка» touches 29
> strings, but 6 use the verb «оголошувати» ("Soundscape will announce…"),
> which has no natural «підказка» verb — those clauses need restructuring.
> Counting that split was what turned "30 quick edits" into a real decision;
> the change was ultimately rejected outright (see C8).

## C4 — Sweep by feature, not by string

A reported string nearly always has siblings the reporter never reached: the
cancel variant, the completion message, the accessibility hint, the help page
paragraph, the FAQ answer. Fixing only what was reported ships an app that
contradicts itself one screen later.

For every reported string, find its feature's other strings by context-key
prefix *and* by the distinctive phrase in the target text.

> **Case (uk, 2026-09-18):** Two menu items were reported. `menu_audio_tutorial`
> had four siblings (`tour_welcome`, `tour_cancel`,
> `menu_audio_tutorial_cancel`, `tour_continue_hint`) carrying the same
> wording, and `new_version_info_text` had one (`new_version_info_completed`)
> with the same tautology.

## C5 — Beware adopting an everyday word as a term of art

When a term decision takes a common word, grep the corpus for that word's
*ordinary* uses. The new term-of-art meaning can make an unrelated string
ambiguous.

> **Case (uk, 2026-09-18):** Adopting «зупинка» for Waypoint collides with
> `faq_tip_create_marker_at_bus_stop`, where «зупинки» means literal bus
> stops. Judged acceptable — context disambiguates — but recorded so the next
> reviewer doesn't treat it as an inconsistency to "fix".

## C6 — Screenshots may be from iOS; the strings are shared

Both apps draw from the same KMP resources
(`shared/src/commonMain/composeResources/values-*/strings.xml`), which is the
`androidkmp` Weblate component. An iOS screenshot is valid Android feedback
and a fix lands for both. Record the reporter's platform and version anyway —
it's what lets you reproduce the screen.

## C7 — Some translation feedback is really source feedback

If a translator or user had to guess at what a string means, the English is
underspecified and **every other language guessed too**. The fix is a better
translator comment (or a better source string) in
`shared/src/commonMain/composeResources/values/strings.xml`, not a per-language
patch.

This is the highest-leverage bucket and the easiest to miss, because the
report arrives labelled as one language's problem.

## C8 — Record rejections *with* the evidence that made them attractive

Some concepts have no good word in a given language, and the existing
translation is the least-bad option rather than a mistake. A reviewer who
doesn't know that will keep rediscovering the same tempting alternative and
re-proposing it, because the case for it is genuinely real — it's the case
against that lives only in someone's head.

So a `rejected` entry has to name the attractive alternative and reproduce the
evidence for it, not just say no. "Rejected: use X" stops nobody. "Rejected:
X, even though <app> uses it and it looks right, because <reason>" does.

Mark the surviving term `confirmed`, not merely "unchanged" — the distinction
is between a decision and an oversight, and only the first one gets defended.

> **Case (uk, 2026-09-18):** Callout «оголошення» → «підказка» was proposed
> with a Google Maps screenshot, then withdrawn by the same reporter: no
> really good Ukrainian word exists, so 29 strings would change for nothing.
> The Google Maps precedent is still there for the next reviewer to find.

## C9 — A string substituted into a template must take the template's case

Some strings never appear on their own: the code drops them into another
string's placeholder. In a case-inflecting language the substituted string has
to be in whatever case the template's preposition governs, and a translator
working from the English can't know that unless the translator comment says
so. Before judging such a string, find its call site and read the template it
lands in.

When sweeping, check the same string across *every* inflected language, not
just the one reported — if the comment didn't warn one translator, it didn't
warn any of them.

> **Case (uk, 2026-09-21):** `confect_name_dead_end` ("dead end") is only ever
> the `%2$s` of `confect_name_to` ("%1$s to %2$s"). Ukrainian rendered the
> template «%1$s у напрямку %2$s» (genitive) and the string «кінець дороги»
> (nominative), producing «дорога у напрямку кінець дороги». The same mismatch
> is in ru «к тупик» (needs dative «тупику»), pl «do ślepa uliczka», cs «do
> slepá ulice», sk «do slepá ulica», hr «do slijepa ulica», sr «до ћорсокак»
> and sl «do slepa ulica» (all need the genitive). The English comment said
> only "Dead end road description".
