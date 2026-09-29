# French, Canada (fr_CA) — translation decisions

| | |
|---|---|
| Weblate component | `androidkmp` (resource dir `values-fr-rCA`) |
| Corpus at last sweep | 1522 units (2026-09-24) |
| Baseline | Microsoft's professional fr-CA localisation (C14). 170 of 359 shared keys still verbatim, **69 drifted** |
| Last native-speaker input | **none recorded** since Microsoft |
| Register | «vous», `confirmed` (baseline) |

Read with [`_common.md`](_common.md) and [`fr.md`](fr.md).

## Status of this file

**Baseline decided 2026-09-25 (Dave): fr_CA = our French + a Canadian layer,
with Microsoft's fr-CA wherever it applies.** Before this, fr_CA had drifted
into a second, independent AI translation: 272 of 769 interface strings
differed from fr, only 8 of them because Microsoft's professional
translators had made them differ. The rule now:

1. **Interface and help strings Microsoft's fr-CA had, with the same
   English** → Microsoft's fr-CA text (iOS parity, C14), except where a
   Microsoft error was already identified.
2. **Every other interface string** → our French, plus the Canadian layer:
   - no space before ? ! ; (a space stays before : and »), as Microsoft's
     fr-CA did
   - «appli(s)» for "app(s)" (≈60 uses in Microsoft's fr-CA)
   - «dépanneur(s)» for «supérette(s)», «guichet(s) automatique(s)
     bancaire(s)» for «distributeur(s) de billets», «balado(s)» for
     «podcast(s)»
3. **POI names (`osm_*`) are left as they are.** They already carry
   Québec usage that French lacks («Dépanneur», «Stationnement», «Hôtel de
   ville», «Guichet automatique bancaire», «Centre de jardinage»).
4. **Terms follow French** (Dave): «point de repère» (Waypoint), «balise
   sonore» (Beacon), and French's Callout usage. fr_CA's own «point de
   cheminement», «annonce» and «balise audio» are gone. «balise sonore» is
   unified even where Microsoft's fr-CA said «balise audio», matching what
   French did.

**Rule for future passes:** translate fr first, then derive fr_CA by the
layer above. Don't translate fr_CA independently, or the two drift apart
again. Open French questions (FR-T1 callout, FR-T2 waypoint/landmark, FR-G1
dead end) now apply to both, and should be decided once for both.

Applied 2026-09-25: 277 + 3 units uploaded and verified live. Held back:
`faq_sleep_mode_battery_answer` and `faq_snooze_mode_battery_answer` (their
paragraph breaks differ from Microsoft's; the snooze one is re-flagged by
the English button-name fix anyway), and `confect_name_to` /
`confect_name_to_via`, which keep fr_CA's «vers» because French's «à» is
the FR-G1 defect.

## Rules

### FRCA-T1 — Sleep and Snooze statuses are both «En veille» (`confirmed` fixed 2026-09-25)

**Fixed 2026-09-25:** `sleep_snoozing` restored to Microsoft's «Désactivé temporairement» (C14 parity), which also ends the collision.

`sleep_sleeping` and `sleep_snoozing` are both «En veille». The user can't
tell whether Soundscape will wake by itself. This is the same kind of
collision as Serbian SR-T1. Microsoft had «Désactivé temporairement» for
Snooze. Something like «En veille automatique» / «En pause jusqu'au
départ» is needed.

### FRCA-G1 — «vers impasse» (`agreed` defect, `unconfirmed` wording)

«%1$s vers %2$s» gives «Sentier vers impasse», with no article. The same fix
as FR-G1: «une impasse».

### FRCA-R1 — «Vous êtes prêt!» is masculine (`unconfirmed`)

Microsoft's «La configuration est terminée.» was neutral (C15).

### FRCA-S1 — Typography (`unconfirmed`, cosmetic)

«Vous êtes prêt!» has no space before «!». That's correct Québec usage and
differs from fr, where a thin space is used. Don't "fix" it from `fr.md`.

## Rejected

Nothing yet.

## Open questions

Numbered as on the questionnaire.

1. Callout «notification»: natural, or «annonce»? It clashes with the phone's
   own notifications. The term came over from French, which is AI-only.
2. Beacon «balise sonore»: natural? Also from French, AI-only.
3. «Sentier vers une impasse»? The app now says «Sentier vers l’impasse» (FR-G2
   adds the article). (FRCA-G1)
4. «Tout est prêt!» instead of «Vous êtes prêt!»? (FRCA-R1)
5. Articles now added before street names («sur la rue Sainte-Catherine», «le long
   du boulevard Saint-Laurent»). Right for Quebec? (FR-G2)
6. Anything else.

## Provenance

**2024-12 — Microsoft baseline** (`fr-CA.lproj`). **2025 → 2026 — AI passes**,
which moved the core terms (see the table above). **2026-09-24 — corpus
sweep.** Nothing uploaded.

**2026-09-25 — Sleep/Snooze iOS parity (Dave's decision, C14).** `sleep_snoozing` «En veille» → «Désactivé temporairement».

**2026-09-25 — Microsoft drift pass (C14): on hold.** Dave asked to re-examine fr_CA before anything is applied: 24 restores, 1 fix («de annonces» → «d'annonces») and three whole-language term questions (waypoint «cheminement», callout «annonce», beacon «audio»). Nothing uploaded.

**2026-09-25 — rebuilt on the French baseline (Dave's decision).** 277 interface strings uploaded (54 Microsoft fr-CA restores, 223 our French + Canadian layer), plus 3 «balise audio» → «balise sonore». All verified live. This supersedes the drift-pass rows that were on hold.

**2026-09-28 — JJ's English rewording and UI-name markup (33 help/FAQ strings).** The existing translations were edited to follow the new English, not retranslated: 26 changed. Each whole string was checked against its English (C16). Where an edited sentence named a button differently from its real label, the text now uses the label. The UI names in `help_text_assistant_commands`, `help_text_assistant_commands_ios` and `help_text_remote_control_how` are now wrapped in `*…*` like the English (commit 2842d5a00). `help_config_voices_content_ios` keeps the localized iOS menu names this translation already used and drops JJ's "(In iOS versions prior to 26…)" note (`unconfirmed`: check the iOS 26 menu name on a device in this language). Uploaded and validated.

**2026-09-28 — Weblate checks pass.** The unit-symbol plurals had empty slots, now filled. `osm_dock` → «Bassin portuaire». FR-T2 is still open. Uploaded live.

**2026-09-29 — Full review of all 1586 units, 13 fixes uploaded.** Checked against the French review of the same day. 1487 units match fr apart from the Canadian layer, which is applied everywhere. Of the 99 real differences, nearly all are Microsoft fr-CA wording, kept under rule 1: «Voyageant vers le Nord» / «Vers le Nord» next to «Se déplaçant vers l'est» / «Face à l'est», «Forum Aux Questions», «Démarrer un Itinéraire», «%1$s tout près». The inconsistent direction phrasing is Microsoft's and sits with FR-S2. 12 fixes carry today's French fixes across, because fr_CA had the same defects: quadrants, facing, wayfinding skills, «rues commerçantes», «commerce devant lequel je passe?», `osm_subway_named`, the two entrance templates → «{fr:de %1$s}» (C18), «notification spatiale», «annoncée» + *Terminé*, *Réactiver le son de la balise*, «libres». `callouts_panel_title` was already Microsoft fr-CA's «Écouter mon environnement». One fix is fr_CA's own: `osm_ferry_terminal_named` «%1$s terminal de ferry» → «Terminal de traversier %1$s» (word order, and matches `osm_tag_ferry_terminal_named`). `osm_drugstore` «Pharmacie» (same as pharmacy) was left as normal Québec usage. Uploaded with `--skip-validate`; all 13 re-fetched and matched exactly.
