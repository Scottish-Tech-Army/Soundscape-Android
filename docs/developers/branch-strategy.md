---
title: Branch strategy
layout: page
parent: Information for developers
has_toc: false
---

# Branch strategy

This page describes which branches are active, what each is for, and how the CI workflows are wired to them. It also describes the upcoming switch to Kotlin Multiplatform (KMP) on `main`.

## Today

There are three branches a contributor needs to know about:

| Branch          | Purpose                                                                                                    |
|-----------------|------------------------------------------------------------------------------------------------------------|
| `v1.0`          | Active maintenance branch for the 1.0.x line. All bug fixes that need to ship to users land here.          |
| `main`          | The branch the 1.0 release was cut from. Receives no new feature work and is being prepared for the KMP cutover (see below). |
| `multiplatform` | Work-in-progress port to Kotlin Multiplatform. Not yet a release target. Will replace `main` (see below).  |

### Where to send pull requests

* **Bug fixes for the shipped 1.0.x app** — open the PR against `v1.0`.
* **KMP work** — open the PR against `multiplatform` for now. After the cutover described below, KMP PRs will target `main`.

### What CI runs where

The workflow file that GitHub uses to decide whether to trigger is the one on the *target* branch of the event, so each branch's copy of a workflow controls its own triggers.

* `run-tests.yaml` — triggered by PRs targeting `main` *or* `v1.0`. Each branch's own copy of the workflow lists its own name, so a PR is gated by the tests defined on the branch it's merging into. This is the gate for both 1.0 maintenance contributions and ongoing work on `main`.
* `jekyll-gh-pages.yml` — triggered by pushes to `v1.0`. The published docs site is built from `v1.0` while it is the active release branch. Auto-deploy from `main` is intentionally disabled (`workflow_dispatch` only) so that a push to `main` cannot clobber the v1.0 docs.
* `nightly.yaml` — scheduled triggers on GitHub only fire from the repository's default branch (`main`), so the nightly lives on `main` and uses a `strategy.matrix` over `[main, v1.0]` to produce a nightly build per branch.
* `build-app.yaml` and `run-maestro-tests.yaml` — manual (`workflow_dispatch`) only. Pick the branch when you run them.

See [GitHub actions]({% link developers/actions.md %}) for the per-workflow detail.

## Upcoming: KMP cutover

The `multiplatform` branch will soon be **force-pushed onto `main`**. After that:

* `main` becomes the KMP development branch. All future KMP work — new features, refactors, dependency bumps — happens there.
* Every workflow on `main` builds **both Android and iOS** from the same source. PR tests, nightly builds and release builds all produce both platforms, and the matrices/artifact names in `nightly.yaml` and `build-app.yaml` will expand accordingly.
* `v1.0` continues to exist as the Android-only maintenance branch for the 1.0.x line. Critical fixes for shipped users will keep landing on `v1.0` until 1.0 reaches end of life.
* The `multiplatform` branch goes away once the cutover is complete.

### Things to be aware of around the cutover

* It is a **force-push**, not a merge. The history of `main` as it exists today will be overwritten. Anyone with local clones will need to reset their `main` to match the new remote (`git fetch origin && git reset --hard origin/main`). Do not try to rebase old `main` work onto the new `main` — the histories are unrelated.
* Any open PRs targeting `main` at the time of the cutover should be reviewed and either merged before the cutover or closed and re-opened against the appropriate branch afterwards.
* The CI configuration on `main` will be inherited from whatever the `multiplatform` branch had at the moment of the force-push. The nightly matrix and other workflow tweaks made for the v1.0 maintenance window may need to be re-applied to the new `main` afterwards.
