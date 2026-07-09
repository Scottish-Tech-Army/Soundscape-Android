---
title: Branch strategy
layout: page
parent: Information for developers
has_toc: false
---

# Branch strategy

This page describes which branches are active, what each is for, and how the CI workflows are wired to them.

## Today

There are two branches a contributor needs to know about:

| Branch                  | Purpose                                                                                                     |
|-------------------------|-------------------------------------------------------------------------------------------------------------|
| `v1.0`                  | Active maintenance branch for the 1.x.x line. All bug fixes that need to ship to users in v1.x.x land here. |
| `main`                  | Work-in-progress port to Kotlin Multiplatform for iOS and Android.                                          |

### Where to send pull requests

* **Bug fixes for the shipped 1.0.x app** — open the PR against `v1.0`.
* **KMP work** — open the PR against `main` for now.

### What CI runs where

The workflow file that GitHub uses to decide whether to trigger is the one on the *target* branch of the event, so each branch's copy of a workflow controls its own triggers.

* `run-tests.yaml` — triggered by PRs targeting `main` *or* `v1.0`. Each branch's own copy of the workflow lists its own name, so a PR is gated by the tests defined on the branch it's merging into. This is the gate for both 1.0 maintenance contributions and ongoing work on `main`.
* `jekyll-gh-pages.yml` — triggered by pushes to `v1.0`. The published docs site is built from `v1.0` while it is the active release branch. Auto-deploy from `main` is intentionally disabled (`workflow_dispatch` only) so that a push to `main` cannot clobber the v1.0 docs.
* `nightly.yaml` — scheduled triggers on GitHub only fire from the repository's default branch (`main`), so the nightly lives on `main` and uses a `strategy.matrix` over `[main, v1.0]` to produce a nightly build per branch.
* `build-app.yaml` and `run-maestro-tests.yaml` — manual (`workflow_dispatch`) only. Pick the branch when you run them.

See [GitHub actions]({% link developers/actions.md %}) for the per-workflow detail.
