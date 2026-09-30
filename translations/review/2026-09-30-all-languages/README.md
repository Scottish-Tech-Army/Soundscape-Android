# All-language translation review, 2026-09-30

Output of `/translation-review-all` over every language (45 translations plus
en_GB). Each `<code>-findings.json` is a list of
`{context, source, current, suggested, reason, category}`. `suggested` equal to
`current` means "flagged for a person, no confident fix". A reason starting
`CROSS-LANGUAGE:` is a problem in the code or the English, not the language.
`all-summary.md` is the combined report.

What was done with them is recorded in each `translations/guidance/<code>.md`
("2026-09-30 — all-language review"). In short: every confident fix whose
string hadn't changed since the review was applied, one commit per language,
except Terms of Use text and fixes the reviewer marked as pending a native
speaker. The cross-language findings were fixed in code or in the English
comments (`_common.md` C20–C25).

The files are a snapshot. `current` is the text at review time, so after the
fixes most of it no longer matches the repo. To re-apply a finding with
translation-review's apply step, it first checks `current` against the file
and drops anything stale.

`earlier/` holds the two findings files that were set aside before the run:
`pl-findings.json` is the first Polish review of the same day (applied in
`626de0a3a`); `pa-findings.json` is from an earlier session, and its status
isn't recorded.
