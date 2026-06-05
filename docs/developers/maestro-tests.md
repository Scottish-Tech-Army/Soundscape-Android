---
title: Maestro UI tests
layout: page
parent: Information for developers
has_toc: true
---

# Maestro UI tests

[Maestro](https://maestro.mobile.dev/) is a mobile UI test framework. We use it
to drive the app through real user flows on an emulator or device — onboarding,
creating markers and routes, browsing places nearby, and so on. The flows live
in the [`maestro/`](https://github.com/Scottish-Tech-Army/Soundscape-Android/tree/main/maestro)
directory at the root of the repository and run in CI on every manual workflow
dispatch (see [Running in CI](#running-in-ci)).

These are end-to-end tests that exercise the real UI. For host-side logic tests
see the [unit test example]({% link developers/unit-test-example.md %}); for the
manual release checklist see the [smoke test]({% link testing/smoke_test.md %}).

## Maestro Studio

The recommended way to write flows is **Maestro Studio**, the official desktop
application. It connects to a running emulator or device and lets you author
flows interactively — inspect any element on screen to discover its selector,
insert commands from a palette, and use the live REPL to try steps out before
committing them to a `.yaml` file. This is much faster and less error-prone than
writing YAML by hand, especially for finding the right `id`/`text` selector for
a control.

Installing Maestro Studio and the Maestro CLI, and setting up an emulator or
simulator, is covered by the official
[Maestro quickstart](https://docs.maestro.dev/get-started/quickstart) — follow
that to get set up. (CI currently pins Maestro `1.40.3`; using a matching
version locally avoids surprises.)

## Running tests locally

Studio is for authoring; once a flow is saved you run it from the Maestro CLI,
which is also what CI uses.

1. Start an emulator (or plug in a device). The CI uses API level 34, x86_64,
   so an emulator of that level is the closest match to what CI runs.
2. Build and install the debug APK:

   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Run a single flow:

   ```bash
   maestro test maestro/Onboarding.yaml
   ```

### The suite is stateful and ordered

The flows are **not** independent — several depend on state created by earlier
flows, so they must run in this order:

| Order | Flow | Purpose |
|-------|------|---------|
| 1 | `Onboarding.yaml` | Clears state and walks the full onboarding sequence. Grants runtime permissions. |
| 2 | `HomePage.yaml` | Exercises the home screen controls (My Location, Around Me, Ahead of Me, menu, sleep). |
| 3 | `LocationDetails.yaml` | Creates markers **A** and **B** that later flows rely on. |
| 4 | `PlacesNearby.yaml` | Scrolls and opens entries in the Places Nearby list. |
| 5 | `MarkersAndRoutes.yaml` | Tab switching, sorting, and editing marker **A**. |
| 6 | `RouteCreation.yaml` | Builds a route from markers **A** and **B**. |
| 7 | `FullScreenMap.yaml` | Toggles the full-screen map on the home screen. |

`Onboarding.yaml` is the only flow that uses `launchApp: clearState: true`;
every other flow uses `clearState: false` so it picks up where the previous one
left off. Because of this, **don't run an arbitrary flow against a fresh install
and expect it to pass** — run `Onboarding.yaml` first, and run the marker-
dependent flows only after `LocationDetails.yaml`.

## How the flows work

Every flow starts with the app id and a `---` separator, then a list of
commands:

```yaml
appId: org.scottishtecharmy.soundscape
---
- launchApp:
    clearState: false
- tapOn:
    id: homeMyLocation
```

### Shared sub-flows

A few small reusable flows take parameters via `env:` and are called with
`runFlow`. Prefer these over copy-pasting the same command sequences:

- **`SwipeUpAndTap.yaml`** — scrolls **down** until an element with the given
  `ID` is visible, then taps it. The standard way to reach and press a button
  that may be below the fold:

  ```yaml
  - runFlow:
      file: SwipeUpAndTap.yaml
      env:
        ID: "welcomeScreenContinueButton"
  ```

- **`SwipeDownAndTap.yaml`** — same, but scrolls **up**.
- **`Wait.yaml`** — a fixed pause that never fails the flow. It waits for an
  element that intentionally never exists, with `optional: true`, so it simply
  burns the `TIMEOUT` (in ms) and carries on. Used to let map tiles or network
  calls settle:

  ```yaml
  - runFlow:
      file: Wait.yaml
      env:
        TIMEOUT: 10000
  ```

  We use this because `assertVisible` cannot take a custom timeout (it has a
  fixed 7s auto-retry); `extendedWaitUntil` is what lets us control the
  duration.

## testTags: how elements are matched

Maestro's `id:` selector matches against Android resource ids. Compose views
don't have resource ids by default, so we expose them through two pieces:

1. A `testTagsAsResourceId = true` semantics modifier set near the root of a
   screen (see `HomeScreen.kt` and `OnboardingNavGraph.kt`). This makes every
   `testTag` below it visible to Maestro as a resource id.
2. A `Modifier.testTag("...")` on the individual composable.

```kotlin
IconButton(
    onClick = { ... },
    modifier = Modifier.testTag("homeMyLocation"),
) { ... }
```

That tag is then matched in a flow with:

```yaml
- tapOn:
    id: homeMyLocation
```

When you add a new control that a flow needs to interact with, **add a
`testTag` to it** and make sure it sits under a subtree that has
`testTagsAsResourceId = true`. Use a descriptive, stable name — these tags are
the contract between the UI and the tests.

You can also match by visible `text:` (see `RouteCreation.yaml`, which asserts
on `"Test Route"`), but text is language-dependent and more brittle, so prefer
`id:` wherever a tag exists.

### Matching dynamic ids with regex

Some tags include dynamic data — list items are tagged
`LocationItem-${name}-${orderId}`. When the `orderId` suffix isn't predictable
(for example after re-sorting), match by name prefix with a regex:

```yaml
- assertVisible:
    id: "LocationItem-A-.*"
```

## Writing a new flow

A practical recipe:

1. Add `testTag`s to any controls the flow needs (see above), and rebuild/
   reinstall the APK.
2. Open [Maestro Studio](#maestro-studio) against the running app and build the
   flow interactively — inspect the controls you tagged, insert the
   corresponding `tapOn`/`inputText`/`assertVisible`/`swipe`/`scrollUntilVisible`
   commands, and try them in the live REPL until the flow does what you want.
3. Save it as `maestro/MyFlow.yaml`. Make sure it starts with the `appId` header
   and `launchApp: clearState: false` (use `clearState: true` only if your flow
   genuinely needs a clean install — it will wipe markers/routes other flows
   rely on). Replace any repeated scroll-and-tap sequences with the shared
   sub-flows above.
4. Decide where it belongs in the [ordered suite](#the-suite-is-stateful-and-ordered).
   If it depends on markers, it must come after `LocationDetails.yaml`. Document
   any such dependency in a comment at the top of the file, as the existing
   flows do.
5. Add it to the CI script (next section) in the right position.

### Using AI to draft a flow

AI assistants (including coding agents) are good at producing a first draft of a
flow. Maestro's YAML is well represented in their training data, and the
existing flows in `maestro/` give plenty of in-repo examples to point at. A
useful prompt is to describe the user journey in plain English, name the
`testTag`s the relevant controls already have, and ask for a flow that follows
the conventions of the existing files (the `appId` header, `clearState: false`,
the shared `SwipeUpAndTap`/`Wait` sub-flows, regex matching for dynamic ids).

Treat the result as a draft, not finished work: AI will happily invent
`testTag`s or selectors that don't exist, so verify every `id:` against the
actual UI in [Maestro Studio](#maestro-studio) and run the flow locally before
relying on it.

### Handling system dialogs and optional steps

Runtime permission dialogs are handled declaratively in the `launchApp`
`permissions:` block (see `Onboarding.yaml`). But **system dialogs that aren't
runtime permissions** — e.g. the battery-optimisation request
(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — are not covered by that block
and must be tapped explicitly. Because such a dialog only appears in some
states, tap it with `optional: true` so the flow doesn't fail when it's absent:

```yaml
- tapOn:
    id: "android:id/button1"   # the system dialog's positive button
    optional: true
```

The same `optional: true` pattern is used to dismiss the "new release" dialog,
which only appears for certain version/build states.

## Running in CI

The [`Run Maestro tests`](https://github.com/Scottish-Tech-Army/Soundscape-Android/blob/main/.github/workflows/run-maestro-tests.yaml)
workflow is triggered manually from the **Actions** tab (`workflow_dispatch`).
It:

1. Builds the debug APK with the tile/search provider secrets written into
   `local.properties`.
2. Boots an API 34 x86_64 emulator via
   `reactivecircus/android-emulator-runner`.
3. Installs the APK and runs each flow **individually, in suite order**, each
   producing a JUnit report:

   ```bash
   maestro test --format=junit --output=report1.xml \
       --test-output-dir=maestro_outputs --no-ansi maestro/Onboarding.yaml
   maestro test --format=junit --output=report2.xml \
       --test-output-dir=maestro_outputs --no-ansi maestro/HomePage.yaml
   # ... and so on through FullScreenMap.yaml
   ```

4. Uploads the `maestro_outputs` reports as an artifact, and on failure also
   uploads `app/emulator.log` (a full `logcat` capture) to help diagnose what
   went wrong.

The Maestro step uses `continue-on-error: true` so the report-upload steps
always run; a final step re-raises the failure to mark the workflow red.

When you add a new flow, add a matching `maestro test ...` line in the correct
position so CI runs it.
