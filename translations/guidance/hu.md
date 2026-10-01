# Hungarian (hu) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` |
| Corpus at last sweep | 1598 units (2026-10-01) |
| Last native-speaker input | 2026-10-01 (questionnaire answers, Q1–Q5) |
| Register | Formal «Ön» everywhere (Dave's call on the reviewer's recommendation, 2026-10-01). See HU-R1 |

Read with [`_common.md`](_common.md).

## Status of this file

**One native speaker answered the questionnaire on 2026-10-01** (Budapest
native; answers to Q1–Q5, nothing for Q6). Until then it was AI-only from
2026-08-21. Their answers confirmed the a/az rules and the detail-level
names, picked new words for Beacon and Callout, and leaned towards «Ön»
for the register. The VoiceOver template «Duplán koppintva: %1$s» with verbal-noun
hints is already the colon frame that C13 recommends. There is no
`hu.lproj`, so the Siri phrases stay in English. Questions:
`docs/translation-questions/questions-hu.md` (Q1…Q3).

## Glossary

| English | Hungarian | Status | Note |
|---|---|---|---|
| Callout | bemondás | `agreed` | Was «bejelentés» / «közlés» / «bemondás» mixed. HU-T2 |
| Audio Beacon | irányjelző hang | `agreed` | Was «hangjelző». HU-T1 |
| Marker | jelölő | `unconfirmed` | |
| Waypoint | útvonalpont | `unconfirmed` | |
| Landmarks | nevezetességek | `unconfirmed` | "Sights" |
| Intersection | kereszteződés | `unconfirmed` | |
| Sleep / Snooze | Alvás ; Szundikálás | `unconfirmed` | |
| Detailed / Simplified / Essential / Silent | Részletes / Egyszerűsített / Alapvető / Néma | `confirmed` (2026-10-01, Q5: "These sound good to me.") | English renamed 2026-09-30 (C22), names retranslated to match. Was Részletes / Kiegyensúlyozott / Csendes / Néma |
| dead end | zsákutca | `unconfirmed` | |

## Rules

### HU-A1 — «a(z)» is resolved in code (`confirmed`, 2026-10-01)

47 strings write the article as «a(z)», because a/az depends on the name
that is filled in, and screen readers read the brackets out. Since
2026-09-25 `resolveGrammarMarkers()` (C18) picks «a» or «az» from the
substituted text, so translators should **keep writing «a(z)»**. Don't
restructure strings to avoid it, and don't pick one form. The letter-name
rule (az M7, az SZTE, a BKV) and the number rule (az 1, az 5, az 1000, a 12,
a 100) were put to the reviewer as Q1: *"The rules here look good to me,
can't think of anything else."* Their one note on route numbers is HU-G3.

### HU-G1 — Road templates said the street type twice (`fixed` in code, 2026-09-25)

35 templates wrote «a(z) %1$s úton» (one «vonalon»), but Hungarian street names
already end in their type, so callouts said «az Andrássy út úton», «a Váci utca
úton». The templates now write «a(z) %1$s{úton}» and `resolveGrammarMarkers()`
(C18) puts the name's own street word into the case: «az Andrássy úton», «a Váci
utcán», «a Deák téren», «a Hősök terén», «a Dunakeszi alagútban». The word list
was measured against the Budapest extract and covers 95% of its 16,094 street
names; the rest (route numbers, Slovak names) keep « úton». **New road templates
must use `{úton}`**, never a literal «úton» after a name.

### HU-G2 — «-ig» on a name is resolved in code (`fixed` in code, 2026-09-25)

`street_description_until` wrote «a(z) %3$s-ig», giving «a Váci utca-ig» where
Hungarian writes «a Váci utcáig». It now writes «a(z) %3$s{-ig}» (C18): a final a/e
lengthens, other letters take «ig», numbers and abbreviations keep «-ig» («az
M7-ig»). Use `{-ig}` for any new "as far as X" template.

### HU-G3 — A bare route number takes «-es» before «úton» (`fixed` in code, 2026-10-01)

Reviewer, Q1: *"'az M7 úton' (az emhét úton) would sound more naturally as
'az M7-es úton' (az emhetes úton)."* Hungarian names a road by its number
with the adjectival suffix (az M7-es, a 8-as főút, az M0-s), so a name
without a street word must not just get « úton» appended. This is the
fallback branch of `{úton}` in `GrammarMarkers.kt` (HU-G1), so **no
translation changes**: when the name ends in a digit the code now writes
«M7-es úton» (`hungarianNumberSuffix()`). The suffix follows the last spoken number word (vowel
harmony):

| ends in | said | suffix |
|---|---|---|
| 1, 2, 4, 7, 9 | egy, kettő, négy, hét, kilenc | -es |
| 3, 8 | három, nyolc | -as |
| 5 | öt | -ös |
| 6 | hat | -os |
| 10, 40, 50, 70, 90 | tíz, negyven, ötven, hetven, kilencven | -es |
| 20, 30, 60, 80 | húsz, harminc, hatvan, nyolcvan | -as |
| 100 (and x00) | száz | -as |
| 1000 (and x000) | ezer | -es |
| 0 alone (M0) | nulla | -s |

The last non-zero digit decides (M35 → -ös, M70 → -es, 300 → -as). Only the
digit-final case was raised; names like «M7-es» that OSM already writes with
the suffix must not get it twice. `{-ig}` stays as it is («az M7-ig»).

### HU-R1 — Formal «Ön» everywhere (`agreed`, Dave 2026-10-01)

The UI says «Készen állsz!» (te), while the help and FAQ pages use «Ön»; at
least 66 units carry clear «te» forms (`tour_*` 16, `first_launch_*` 9,
`voice_cmd_*` 5, settings, hints, and 4 help strings that slipped into «te»:
`help_gps_accuracy_description`, `help_config_voices_content`,
`help_config_voices_content_ios`, `help_offline_troubleshooting_description`).
This is the same split Spanish had (ES-R1).

Reviewer, Q2: *"It's a very tricky question. I'd say if the target audience
is adults, especially seniors, they might find the informal tone
(tegeződés) inappropriate. It really depends on the person's preference.
For example, I, as a 41-year-old born and raised in Budapest, would prefer
the informal tone in mobile applications. On the other hand, the formal
tone (magázódás) may result slightly longer sentences. If I had to make a
decision, I'd say use the formal tone (magázódás). Nobody will find it
offending, for sure."*

Dave took the recommendation: **«Ön» everywhere**. Applied 2026-10-01 to 95
strings: verbs (koppints → koppintson, hallhatod → hallhatja), possessives
(célod → célja, telefonod → telefonja), pronouns («előtted» → «Ön előtt»,
«általad» → «Ön által»), «Üdvözlünk» → «Üdvözöljük», «Kérlek» → «Kérjük».
New strings must use «Ön» forms; short labels and hints stay verbal nouns,
which have no register.

Three deliberate «te» exceptions, where the user isn't the one addressed:
- `universal_links_marker_share_message` («Megosztottam veled…») is sent by
  the user to a friend.
- `voice_cmd_start_route` and `voice_cmd_start_beacon_at_marker_with_name`
  («Indítsd el…») are phrases the user says *to the app*.

«Lásd a *…* súgótémakört» is the usual cross-reference wording and stays.

### HU-T1 — Beacon is «irányjelző hang» (`agreed`, 2026-10-01)

Reviewer, Q3: *"I'd use 'irányjelző hang' -- 'Most hallhatod az irányjelző
hangot a célod felől.'"* Replaces «hangjelző», which in everyday Hungarian is
a buzzer or horn. 77 units, all in the audio-beacon sense (C12's other Beacon
meanings don't occur in Hungarian).

- Declension goes on «hang»: hangot, hangok, hangokat, hangtól, hangra,
  hangnak, hangból, hangja.
- **The article changes**: «a hangjelző» → «az irányjelző hang», including
  before a starred UI name («az *Irányjelző hang indítása* gombra»).
- **Never shorten to «irányjelző»** on its own: that is a car's indicator.
- Drop «hallható» (audible) in front of it: «hallható irányjelző hang» says
  "sound" twice. The same goes for «a hangjelző hangja» → «az irányjelző
  hang»; five strings were reworded by hand for this.
- `tour_beacon_demo` uses the reviewer's own sentence, with «felől»
  instead of «irányából szól».
- `voice_cmd_start_beacon_at_marker_with_name` is a phrase the user says to
  the in-app recogniser, so the change alters what they say. That's
  intended: it should match the button.
- The one existing «irányjelzés» (`faq_why_does_beacon_disappear_answer`)
  became «iránymutatás», so the sentence doesn't call the beacon an
  «irányjelzés» right after naming it «irányjelző hang».

### HU-T2 — Callout is «bemondás», one word everywhere (`agreed`, 2026-10-01)

Reviewer, Q4: *"I'd use either 'bemondás' or 'hangbemondás' everywhere."*
We took «bemondás»: it's shorter, the corpus already used the verb
«bemondja» for "call out" (12 units), and the settings labels are heard
often. The corpus mixed three words for one concept (C12 split corpus):
«bejelentés» (15 units), «közlés» (30) and «bemondás».

- Nouns: bemondás, -t, -ok, -okat, -okkal, -a, -akor, -aiban, -ait; «közlési
  előzmények» → «bemondási előzmények»; «hangközlései» → «bemondásai».
- Verbs (rewrite, not a noun swap): bejelent → bemond, bejelentse →
  bemondja, bejelenteni → bemondani, «Jelentse be» → «Mondja be». 5 units.
- **Exception, keep:** `settings_voice_command_listening_prompt` «Figyelés
  bejelentése» ("Announce listening") is a cue that the microphone is open,
  not a callout.
- Not callouts, ignore in sweeps: «távközlési» (telecoms), «tömegközlekedés».
- UI names quoted in help text («*Bemondások kezelése*», «*Bemondások
  részletessége*») change together with the labels.

## Rejected

- **«hangbemondás» as the Callout term** (2026-10-01). The reviewer offered
  it as an equal alternative to «bemondás»; it's longer and adds nothing in
  an app where everything is audio. Not wrong, just not the pick.
- **«értesítés» for Callout.** It clashes with the phone's own
  notifications. Ruled out before the questionnaire.
- **«irányjelző» alone for Beacon.** Car indicator (HU-T1).

## Open questions

1. Road numbers (HU-G3): is «az M7-es úton» right for every road class, or
   do motorways want «autópályán» rather than «úton»?
2. Register switch (HU-R1): do the AI-converted «Ön» UI strings sound
   natural? «Készen áll!» on its own can read as "something is ready";
   also «Üdvözöljük!», «Biztos benne?», `tour_create_marker_done`.
3. Anything else.

## Provenance

**2026-08-21 → 2026-09-24 — AI passes only.** **2026-09-24 — corpus sweep.**
Nothing uploaded.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 27 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-29 — European batch review.** 6 hints were «te» verb forms or capitalised («megtekintsd», «kilépj», «válassz», «zárd be…», «Térkép…») → verbal nouns like the rest. Help: «Hely részletei» → «Helyszín adatai» (3), «Közösségi közlekedés» → «Tömegközlekedés» (1). 10 uploaded. Live strings re-checked before upload; uploaded with `--skip-validate` and verified live.

**2026-09-30 — all-language review, no speaker involved.** All 1598 units read by `/translation-review-all`; 49 flagged, 45 applied in `ac6f12f08`. Findings: `translations/review/2026-09-30-all-languages/hu-findings.json`.
Most serious fixed:
- `callouts_verbosity_description`: Wrong article before vowel-initial level names: «A Egyszerűsített», «A Alapvető» must be «Az …» (this string is not run through resolveGrammarMarkers as «a(z)», so it is
- `help_text_automatic_callouts_how_1`: Wrong article: «a *előző*» must be «az *előző*» (vowel-initial word).
- `faq_holding_phone_flat_answer`: Conjugation error: definite object «a telefont» needs the definite form «elteheti», not «eltehet».

**2026-10-01 — questionnaire answers from a native speaker (Q1–Q5).**
Confirmed HU-A1 and the detail-level names; new terms HU-T1 (Beacon) and
HU-T2 (Callout); HU-G3 (route-number suffix, code); HU-R1 «Ön» decided by
Dave. Applied the same day: 186 strings (77 Beacon, 41 Callout, 95 register,
with overlaps). HU-G3 done in code the same day.
