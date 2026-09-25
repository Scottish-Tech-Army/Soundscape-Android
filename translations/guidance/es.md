# Spanish (es) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1522 translated units (2026-09-24) |
| Last native-speaker input | 2026-05-12 (GitHub issue #881) |
| Reporter platform | GitHub issues, Weblate comments — Android and general |
| Register | Informal «tú» everywhere, no exceptions (Dave's call, 2026-09-24 — see ES-R1) |

Read with [`_common.md`](_common.md).

---

## Status of this file

Unlike `pl.md`, this is **not** a fresh AI pass reacting to nothing. JJ
Gatchalian, a native Spanish speaker and assistive-technology instructor who
also maintains translations for Soundscape Community, did real work on this
corpus directly in Weblate through May 2026 (GitHub issue #881 and four
related issues), converting large parts of the UI from formal to informal
register and fixing specific strings. He marked his own fixes **Approved** in
Weblate, which exempts them from being overwritten by later AI translation
passes.

This file was built by reading that thread (not a fresh review) and sweeping
the *current* corpus to check how completely his decisions actually landed —
they didn't, entirely: see ES-R1. Nothing here has been re-confirmed by JJ
since May; treat `confirmed` as "confirmed then, unswept since" rather than
"confirmed today."

---

## Glossary

| English | Spanish | Status | Why |
|---|---|---|---|
| Callout | aviso | `confirmed` | 40 occurrences, consistent |
| Audio Beacon | señal (de audio) | `confirmed` | 77 occurrences, consistent |
| Marker | marcador | `confirmed` | 84 occurrences, consistent |
| Waypoint | punto de ruta | `confirmed` | 24 occurrences, consistent |
| Sleep Mode | modo de suspensión | `confirmed` | 8 occurrences; lowercase mid-sentence is correct Spanish style, not a drift from the English title case |
| Snooze Mode | modo de aplazamiento | `confirmed` | 5 occurrences, consistent |
| Manage Callouts | Administrar avisos | `confirmed` | From JJ's FAQ edit (issue #892 attachment) |

---

## Rules

### ES-R1 — Informal «tú» everywhere, no exceptions (`confirmed` by JJ, 2026-09-24)

**JJ, #881 (2026-09-24):** *"thanks for making the entire text informal. I liked your decision because the default Android Spain Spanish hints are written with informal Spanish."* The original rationale follows.

JJ's stated plan (issue #881, opening comment): convert the app from formal to
informal/friendly pronouns and conjugations "to match the more friendly tone
of the English." He later carved out one exception in issue #892: FAQ
answers should **stay formal**, in his own words *"so that I could tell which
version of Soundscape I use since I contribute to both projects"* — i.e. a
bookkeeping reason specific to him personally, not a claim about what's
better for users.

**2026-09-24: Dave overruled that exception.** His reasoning for keeping FAQ
formal isn't a consideration for this project — the whole app is informal,
full stop. This supersedes the two-register version of this rule that an
earlier pass of this file recorded.

- **Everything: informal «tú»** («puedes», «tu», «pulsa»). No `faq_*`/`help_*`
  carve-out.

**A 2026-09-24 sweep of the live 1522-unit corpus found two kinds of drift
from this rule, now both fixed with concrete rewrites in
`/tmp/weblate-review/es-findings.json` (95 units total, `high` confidence —
this is regular tú-conjugation, not a term decision):**

1. **26 general-UI strings** (onboarding, errors, hints, settings
   descriptions, the About screen, release notes) were still formal «usted» —
   e.g. `talkback_double_tap_template` ("Pulse dos veces para%1$s", the
   template every TalkBack hint is built from — highest-leverage single fix,
   since every hint currently reads formal-verb + informal-object mid-sentence:
   "Pulse dos veces para moverte"), `routes_no_routes_hint_1` ("Cree una ruta
   para usted..."), `legacy_migration_title`, `new_version_info_details`, and
   others. Likely added or touched by an AI pass after JJ's May sweep, since
   they read exactly like the "before" state he described.
2. **69 `faq_*`/`help_*` strings** were formal, matching JJ's now-overruled
   exception — the bulk of this sweep. Converted to «tú» throughout: possessives
   (su → tu), object pronouns (le → te), and verb conjugations across every
   mood these FAQ answers use (imperative, present, future, subjunctive).
   `faq_supported_phones_answer` and `help_offline_page_title` were checked
   and left alone — their apparent "usted" markers turned out to be
   third-person references to Soundscape itself ("Soundscape **está**
   disponible..."), not the reader. `help_text_remote_control_how` similarly
   needed no change: its button-by-button descriptions are impersonal
   ("Reproducir/Pausa: Activa o desactiva...", describing what the button
   does, not addressing the reader) and read the same in either register.

The **`help_text_assistant_*` cluster (9 strings, the Gemini/Siri voice-command
help)** was already informal before this sweep, despite living under `help_*`
— it just happened to already match where the rule ended up. No longer an
inconsistency to flag; left as-is.

### ES-R2 — The informal "tap" verb is «Pulsa», not «Toca» (`confirmed` by JJ, 2026-09-24)

**JJ, #881:** *"it was a good thing you chose 'pulsa' to match the Microsoft heritage. Toca will still be the default in Android because that's what TalkBack uses, and we can't change that. It doesn't matter if iOS doesn't match Android as long as everyone can understand."* So iOS VoiceOver (our template) says «Pulsa» and Android TalkBack (its own frame, C13) says «Toca». That divergence is **expected**, so don't "align" it.

JJ's messages about this are genuinely ambiguous on their own: he discusses
matching Android's own TalkBack Spanish ("Toca dos veces para", informal
"touch") but also says *"The friendly conjugation that I'll end up using is
'Pulsa'"* ("press", matching the original Microsoft Soundscape heritage
wording). Taken alone, it's unclear which verb he settled on.

The live corpus resolves it: every already-informal string that needed this
verb uses **«Pulsa»** (`tour_around_me`, `tour_ahead`: *"Pulsa
\"...\"."*), never «Toca». Treating `talkback_double_tap_template`'s fix
(ES-R1, "Pulse" → "Pulsa") as consistent with this existing choice rather
than introducing a third verb.

---

## Rejected

- **«Te damos la bienvenida» for Welcome** (2026-09-25). It's attractive:
  it's gender-neutral (C15), and it's close to Microsoft's VoiceOver label
  «Le damos la bienvenida». JJ rejected it after asking native speakers, as
  less "fun and snappy" than «¡Bienvenido!», whose masculine form most
  speakers don't mind. See ES-W1.
- **«¡Todo listo!» for "You're ready!"** (2026-09-25). Microsoft's phrase,
  and gender-neutral, but JJ says it means "all done", not "you're ready".

Nothing yet — no feedback in this thread was proposed and then turned down.

---

## Open questions for the reporter (JJ / next native-speaker round)

None open. JJ answered Q3–Q5 on 2026-09-25 (see ES-G1, ES-W1 and the
Rejected section).

*Answered and closed:* **Q1 «Pulsa»**, confirmed (ES-R2). **Q2, the FAQ
button name** (`faq_snooze_mode_battery_answer`): JJ explained that
Microsoft's FAQ said «Reactivar cuando salga» because Microsoft's iOS button
had a *separate VoiceOver label*, `sleep_wake_up_when_i_leave` ("Wake Up When
I Leave"), while sighted users saw `sleep_ui_wake_on_leave` ("Wake On\nLeave").
That's true of Microsoft's app and of Soundscape Community. **It isn't true
of ours:** `SharedSleepScreen.kt` puts `sleep_wake_on_leave` on the button as
plain text, so VoiceOver and TalkBack both read «Reactivar al salir». The
English FAQ was corrected to match (2026-09-25), and the Spanish FAQ should
say «Reactivar al salir» (`agreed`, to be done when Weblate flags the string). *Also closed:* «sobre de» in `first_launch_callouts_listen`, fixed in the 2026-09-24 upload («sobre lo que»). The two-register question — Dave decided informal
everywhere, no FAQ exception (2026-09-24, see ES-R1). The
`help_text_assistant_*` register — already informal, already correct under
the new rule.*

---

## ES-G1 — Dead end: «Sendero hacia un callejón sin salida» (`confirmed`, JJ 2026-09-25)

JJ: *"For the first case about dead ends, let's go with the AI. A
capitalization in the middle doesn't follow the conventional Spanish
rules."* `confect_name_dead_end` → «un callejón sin salida» (lowercase, with
the article). `confect_name_to` / `_to_via` → «%1$s hacia %2$s» / «%1$s
hacia %2$s vía %3$s». «hacia» also reads naturally with the map names that
fill the same slot («Sendero hacia Calle Mayor»).

## ES-W1 — «¡Bienvenido!» stays masculine; «¡Ya estás listo!» (`confirmed`, JJ 2026-09-25)

A deliberate exception to C15. In JJ's words: *"I think I wrote
'Bienvenido' because it sounds catchier to native speakers … 'Te damos la
bienvenida' isn't as fun and snappy. Although the shorter 'bienvenido' is
masculine, and the user could be female, most native speakers wouldn't
really care."* He also updated Soundscape Community's audio label to
«Bienvenido» to match the visual one. `first_launch_welcome_title` and
`tour_welcome` keep «Bienvenido».

`first_launch_prompt_title` becomes **«¡Ya estás listo!»**. The English
changed from "You're all set!" to "You're ready!", and the AI then produced
«¡Ya está listo!» without converting it to «tú». «Todo listo» was rejected,
because it means "all done" rather than "you're ready".

## ES-R1 exception: the Terms of Use screen is formal

The legal screen (`terms_of_use_message`, `terms_of_use_medical_safety_disclaimer`,
and the rest of `terms_of_use_*`) is formal «usted», in both our app and
JJ's Soundscape Community translation (e.g. «Confirma que Soundscape…»,
«Tenga cuidado…»). It was kept formal when the truncated disclaimer was
restored on 2026-09-25. Legal text in formal register is normal, and it
matches the Community baseline. Don't convert it to «tú» without asking Dave.

## Baseline: Soundscape Community, not Microsoft

**Dave's decision (2026-09-25):** Spanish follows JJ's Soundscape Community
translation (`github.com/soundscape-community/soundscape`,
`apps/ios/GuideDogs/Assets/Localization/es-ES.lproj/Localizable.strings`),
which JJ has maintained since Microsoft's release (latest commit
2026-07-24). It **replaces Microsoft's es-ES as the reference under C14**.
Use `es-ES` for our `es`, not `es-419`: JJ keeps the Latin American file
formal on purpose, and ES-R1 is informal.

Compared 2026-09-25 against our live corpus. 210 shared strings were already
identical. 13 differed audibly, and all 13 were improvements, including
formal leftovers ES-R1 had missed (`first_launch_welcome_description`
«Navegue», `first_launch_headphones_message_1` «desea … tómelos»,
`first_launch_callouts_message` «le ayuda … se encuentra … va») and
`general_error_add_marker_error` «Vuelve a intentarlo». **All 13 uploaded and
verified live.** JJ's «Métrica (metros)» replaces our «Métrico». 136 strings
couldn't be compared because our English has diverged from the Community's.
None of the 13 conflicted with ES-R1 (no case of Community-formal over
our-«tú»). If one ever does, ES-R1 wins.

## Provenance

**2025-08-27 → 2025-09-22 — Luis Carlos, legacy `android-app` component.**
Seven Weblate commits: FAQ answers, help text and hints, in the formal
register. Superseded by JJ's work and ES-R1, but it was human input.

**2026-09-24 — JJ, GitHub #881 (comment 5821176033).** Confirmed ES-R1 (informal
everywhere) and ES-R2 («Pulsa» on iOS, with «Toca» expected from Android
TalkBack). Said «sobre de» was a Microsoft error that he had also fixed in
Soundscape Community, and explained Microsoft's separate VoiceOver label for
the Wake On Leave button (see the closed Q2). For context, he maintains a
**formal** Latin American Spanish translation in Soundscape Community, both to
match Microsoft's usual style (Soundscape, Seeing AI, Teams) and to avoid
clashing with Android's Latin American hints. That concerns a different
project and a different locale, so it doesn't reopen ES-R1 for our `es`. If
this project ever adds `es_419`, that's where his reasoning applies.

**2024-07 → 2024-09 — Microsoft baseline.** The oldest ~360 strings were
copied from Microsoft's professional es-ES localisation of the iOS app (see
`_common.md` C14). That baseline was formal «usted», which is what JJ's May
2026 pass converted away from. So ES-R1's informal rule deliberately
overrides the professional baseline, and that should be defended rather than
"restored" by anyone who compares against Microsoft's files.

**2026-05-06 to 2026-05-13 — native speaker, GitHub issues #881, #885, #889,
#891, #892.** JJ Gatchalian (native Spanish speaker, also a Soundscape
Community translator) did an extensive live-Weblate revision pass: converted
much of the UI from formal to informal register, fixed the double "to to" in
TalkBack hints (#885), lowercased hint first letters and removed trailing
periods to match VoiceOver/iOS convention (#889), and submitted two English +
Spanish text documents with FAQ corrections (attached to #881 and #892). He
explicitly decided FAQ text stays formal, as a way to distinguish this
project's translation from Soundscape Community's (#892). He marked his
direct Weblate edits Approved so they survive later AI passes.

**2026-09-24 — corpus sweep, no speaker involved (this session, first pass).**
Re-read the full issue thread plus its two attached edit documents, then
swept the live `es` corpus (1522 units) against the two-register rule those
issues describe (informal UI, formal FAQ). Found the rule holds in `tour_*`
and most `*_hint` strings but not in a 22-unit cluster of onboarding/error/
migration/settings strings that read like the "before" state JJ described —
likely added or touched after his May pass. Also found a 9-unit cluster
(`help_text_assistant_*`) that's informal inside what should be formal FAQ
territory. Wrote concrete fixes for the 22-unit cluster; left the borderline
settings/about strings and the assistant-help cluster unswept pending
questions to the user.

**2026-09-24 — Dave: JJ's formal-FAQ rationale doesn't matter here, all
strings should be informal.** Overrides the FAQ exception this file had
just recorded. Re-swept the corpus for the newly-in-scope `faq_*`/`help_*`
cluster: 69 more units were formal, converted to «tú» throughout. Combined
with the earlier 22 plus the 4 previously-"borderline" settings/about
strings (no longer borderline — now unambiguously in scope), this brings the
total to **95 units** with concrete fixes in
`/tmp/weblate-review/es-findings.json`, all `high` confidence. Verified: no
placeholder or line-break drift, no leftover formal verb forms, and every
remaining «su»/«sus» in the suggested text double-checked as a genuine
third-person possessive (the beacon's sound, the place's distance, the
libraries' licenses) rather than a missed "your". The
`help_text_assistant_*` cluster needed no change — it was already informal
and is now simply correct rather than an outlier.

**2026-09-24 — applied.** Dave asked to apply via `weblate-review`. Published
a review page (word-diff over all 95 findings) for confirmation before
upload, per the skill's own rule that this lands live with no review queue.
Dave confirmed all 95. Upload hit a tooling gap: `weblate_sync.py`'s
validator checks that every uploaded key is currently *untranslated* — a
check built for `weblate-translate`'s fill-in-the-blanks flow, not for
correcting already-translated strings, so it flagged all 95 as errors on a
check that doesn't apply to this operation. `--skip-validate` is itself
blocked by an auto-mode safety classifier ("Safety Bypass Flag"), so Dave ran
the upload command himself. **95/95 uploaded successfully**, spot-checked
live (`talkback_double_tap_template`, `faq_how_to_use_beacon_answer`,
`legacy_migration_complete` all confirmed correct). Corpus stays at
1522/1522, untranslated=0 — these were corrections to existing translations,
not new strings.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` «En modo de aplazamiento» → Microsoft's «Posponiendo», so the status pair is Microsoft's «Suspendiendo» / «Posponiendo» again. That **closes Q5**: «Suspendiendo» is Microsoft's own wording and stays under the parity policy. The FAQ and help text still say «modo de aplazamiento» for the *mode*, which is fine, since only the status label changed.

**2026-09-25 — truncation repaired (C16).** `terms_of_use_medical_safety_disclaimer` had been cut down to a fragment by an AI pass on 2026-08-19 → 22. Restored from the complete pre-damage translation in git history (English unchanged since), uploaded and verified live.

**2026-09-25 — JJ's second reply (#881 follow-up, via Dave).** Answered Q3
(dead end, ES-G1), Q4 (welcome and ready, ES-W1). His note that the AI had
skipped «¡Ya está listo!» prompted a sweep for more ES-R1 leftovers: **21 more** strings still addressed the user formally, all added
after JJ's May pass (settings descriptions, dialogs, voice-command replies,
the legacy-migration messages, and three iOS permission prompts that also
live in `iosApp/iosApp/InfoPlist.xcstrings`). The Terms of Use screen stays
formal (see above). All 25 were uploaded and verified live, and the 3 iOS prompts were changed in
`InfoPlist.xcstrings` at the same time. JJ also asked to join both the Android and iOS beta
programmes, and will compare the informal Spanish here against his formal
Latin American Spanish in Soundscape Community.
