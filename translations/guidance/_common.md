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
>
> **Addendum (2026-09-24):** Icelandic has it too. «til blindgata» should be
> genitive «til blindgötu» (`is.md` IS-G1). Bulgarian is correctly absent,
> since it has no noun case (`bg.md` BG-G1). A non-inflecting language can
> still get this slot wrong another way: French «à impasse» lacks its article
> (`fr.md` FR-G1).
>
> **Fixed 2026-09-24** in ru, cs, sk, hr, sr, sl, pl and is: the genitive (dative for
> ru «тупику») was uploaded and verified live in all eight. uk was fixed earlier
> (see `uk.md`). fr FR-G1 is still open, because it needs a wording decision.

## C10 — `confect_name_to` means "a path that leads to", not "from … to"

`confect_name_to` («%1$s to %2$s») and `confect_name_to_via` put a **way
type** in `%1$s` («Path», «Service road») and the place it leads to in
`%2$s`. English "to" is ambiguous, so translators reached for their language's
*from–to* range construction, which makes the path the starting point instead
of the thing being described:

> **Case (2026-09-24 sweep):** hi «%1$s से %2$s तक», ta «%1$s முதல் %2$s
> வரை» and ja «%1$s から %2$s へ» all say "from the path to Moor Road".
> zh «%1$s到%2$s» is ambiguous in the same way. mr, te and ko got it right
> with a relative clause («%2$s कडे जाणारा %1$s», «%2$s(으)로 이어지는
> %1$s»), which is the model to follow.

This is a rule C7 source problem. The English comment ("Road description of
path to another road e.g. "Path to Moor Road"") doesn't say that `%1$s` is a
way type. It should say "%1$s is a kind of way (Path, Track…); %2$s is where
it leads. Translate as 'a %1$s leading to %2$s', not 'from %1$s to %2$s'".
**Edited 2026-09-24**, together with `confect_name_dead_end`'s comment,
which now says it only ever lands in these templates' `%2$s` (C9). A comment
edit reaches translators in Weblate only after the component pulls the new
English source. Existing translations are not re-flagged, so the hi, ta and
ja strings still need fixing through their reviewers (HI-G1, TA-G1, JA-G1).

## C11 — "goes left" describes a road; it is not a turn instruction

`directions_name_goes_left/right` and `directions_name_continues_ahead` tell
the user which way each road at an intersection *leads*. Soundscape never
gives turn-by-turn instructions. A translation that reads as an instruction
(«turn left») tells a blind user to do something the app never decided.

> **Case (2026-09-24 sweep):** vi «%1$s, rẽ trái» and zh «%1$s，左转» both
> mean "turn left". The zh version was a native speaker's change (Benjamin
> Lin, 2026-02-17, replacing «向左延伸», "extends to the left"). So the
> original wording evidently sounded wrong to a native ear, and the fix
> overshot. Treat that as a question, not a revert (C8).

## C12 — A transliterated English term isn't wrong by default

In many languages the phone itself uses the English loanword (Hindi
«स्लीप», Japanese «スヌーズ», Telugu «కాలౌట్»). A native-sounding coinage the
user has never heard on their phone is worse than a loanword they recognise.
This is the same logic as C1. Ask what the reviewer's phone and navigation
apps say before "nativising" a transliteration.

Two things still make a term wrong regardless of origin:

- **the wrong sense**: a term with a *visual* meaning for something audible
  (vi «Đèn hiệu», "signal lamp", and id «Suar», "flare", for Beacon)
- **a split corpus**: two different words for one concept (bn ঘোষণা/কলআউট,
  ur اعلان/کالآؤٹ, zh 提示/播报, ja コールアウト/読み上げ)

## C13 — Accessibility hints serve two platforms; fix the iOS template, not the hints

Every `*_hint` / `*_acc_hint` fragment goes to both platforms:

- **Android:** `TalkbackHelpers.android.kt` passes it as the `onClick` label,
  and TalkBack wraps it in its *own* system-language phrasing ("Double-tap to
  <label>" in English).
- **iOS:** `TalkbackHelpers.ios.kt` substitutes it into our
  `talkback_double_tap_template`.

So when the iOS output reads badly, the first fix to try is the **template**,
which is one string and iOS-only. The hints are ~40 strings and changing
them also changes what TalkBack says. Pick a template frame that fits the
grammatical form the hints already have:

> **Case (sk, 2026-09-24):** The hints are infinitives («stlmiť zvukový
> maják»), but the template «Dvojitým ťuknutím %1$s» ("by double-tapping …")
> needs a finite verb. «Ak chcete %1$s, dvakrát ťuknite» or «Dvojitým
> ťuknutím môžete %1$s» fits the existing infinitives without touching them.
> By contrast, cs hints are already second-person future («ztlumíte»), which
> «Dvojitým klepnutím %1$s» expects.

Only change the hints if no template frame can work, and then ask a
TalkBack user in that language what Android actually says around them.

## C14 — Fifteen languages started from Microsoft's professional localisation

The original Microsoft Soundscape iOS app shipped professionally translated
strings for **da, de, el, en_GB, es, fi, fr, fr_CA, it, ja, nb_NO, nl, pt,
pt_BR, sv**. They are open source at
`github.com/microsoft/soundscape/tree/main/apps/ios/GuideDogs/Assets/Localization/<locale>.lproj/Localizable.strings`.
The files are UTF-16, and a key `a.b.c` there is our `a_b_c`. The 2024
prototype commits (`53d4f20d5`, then Adam Ward's "Translation strings for …"
series) copied them in verbatim. Verified 2026-09-24: `first_launch_beacon_message_2`
in German is word-for-word Microsoft's.

About 360 of our non-POI keys exist in Microsoft's files. As of 2026-09-24,
roughly 60% of those are still Microsoft's exact wording in each language.
The rest changed, mostly because our English changed. The ones that matter
are **drift**: the English is identical to Microsoft's en-US, but the
translation differs. That means an AI pass replaced a professional
translation for no source reason. Counts: da 73, fr_CA 69, es 34, nl 29,
fr 27, ja 27, nb 26, fi 24, el 21, de 20, sv 15, it 8, pt_BR 8, pt 7,
en_GB 2.

How to use this:

- **Microsoft's term is the default** when a term is disputed and no native
  speaker has spoken. It was chosen by professional translators, and it's
  what long-time Soundscape users have heard for years.
- **But it isn't infallible.** Some drift is an AI fixing Microsoft's errors:
  de «Endpunkt» for "Done", the typo «Wie verwenden ich», «zu, dass» for
  «hin, an dem». Check each drifted string on its merits, not by reverting.
- **Microsoft never saw ~410 of our keys** (callout detail, confected way
  names, voice commands, travel mode…). Those are AI-only in every language.

> **Case (sv, 2026-09-24):** Microsoft's button/status pair was «Inaktivera» /
> «Inaktiverad». An AI pass changed only the button, to «Viloläge», leaving the
> status on Microsoft's word, so the two no longer match.

## C15 — "You're ready!" forces a gender the English doesn't have

`first_launch_prompt_title` was Microsoft's "You're all set!" and became
"You're ready!" in `19af0e0a1` (2026-04-19). "Ready" is an adjective, and in
most gendered languages it has to agree with the user. Translators then pick
the masculine, a slash, or a plural dodge. Microsoft's translators, working
from "all set", mostly found an impersonal form:

> **Case (2026-09-24 sweep):** pt «Está tudo pronto!» → «Está pronto!» (m),
> pt_BR «Tudo pronto!» → «Você está pronto!» (m), fr_CA «La configuration est
> terminée.» → «Vous êtes prêt!» (m). Also it «Sei pronto!» (m), ro «Ești
> pregătit!» (m), ar «أنت جاهز!» (m), is «tilbúin/n» (slash, read aloud),
> cs/sk plural «připraveni»/«pripravení».

This is a rule C7 source problem. Either restore "You're all set!" or add a
translator comment such as "Prefer an impersonal phrasing (e.g. 'All set!',
'Everything is ready') so the text doesn't have to agree with the user's
gender". The same applies to any future English that puts an adjective or
participle on "you".

**Comment edited 2026-09-24** (English text unchanged, to avoid flagging 44
languages at once) for `first_launch_prompt_title`, `first_launch_welcome_title`
and `tour_welcome`: prefer gender-free phrasing, and never use slashes or
brackets. The welcome title had the same problem: es «¡Bienvenido!», pt
«Bem-vindo!» and ca «Benvingut!» are masculine, and pt_BR «Bem-vindo(a)!»
puts brackets in front of a screen reader. it «Salve!» is the model. Existing
translations aren't re-flagged by a comment change, so fix them through each
language's reviewer.
