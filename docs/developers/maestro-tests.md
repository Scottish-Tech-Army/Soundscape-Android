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
directory at the root of the repository and run in CI as a follow-on to
the [`Run tests`](https://github.com/Scottish-Tech-Army/Soundscape-Android/blob/main/.github/workflows/run-tests.yaml)
workflow, standalone against a published release, and automatically against the
latest release on PRs that change only flows (see
[Running in CI](#running-in-ci)).

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
that to get set up. (CI currently pins Maestro `2.6.0`; using a matching
version locally avoids surprises.)

## Running tests locally

Studio is for authoring; once a flow is saved you run it from the Maestro CLI,
which is also what CI uses.

1. Start an emulator (or plug in a device). CI runs the suite across a matrix of
   API 31 and 35, x86_64 (see [Running in CI](#running-in-ci)), so an emulator at
   one of those levels is the closest match to what CI runs.
2. Build and install the debug APK:

   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Run a single flow:

   ```bash
   maestro test maestro/Onboarding.yaml
   ```

### Offline map data and a fixed GPS location

CI seeds the emulator with a local map extract and pins the GPS to a known
location (the STA office) so the location-dependent flows are deterministic. To
match that locally, push the Glasgow extract and its metadata sidecar into the
app's offline-extracts directory and set the GPS before running the flows:

```bash
# The app reads extracts from its external files dir + "/Download". That dir is
# created when the app first runs, and on Android 11+ scoped storage blocks
# writes there even for the shell user — so launch the app once, then use
# "adb root" (the emulator is a userdebug image) before pushing.
adb root
dest=/storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Download
adb shell mkdir -p "$dest"

adb push app/src/test/res/org/scottishtecharmy/soundscape/20260118-1505-glasgow-gb.pmtiles \
    "$dest/glasgow-gb.pmtiles"
adb push .github/fixtures/glasgow-gb.pmtiles.geojson \
    "$dest/glasgow-gb.pmtiles.geojson"

# adb emu geo fix takes longitude then latitude (here, the STA office).
adb emu geo fix -3.223538 55.955360
```

The `.pmtiles` is the map data the geo engine renders; the matching
`.pmtiles.geojson` is the metadata sidecar (`findExtracts` looks for
`<pmtiles>.geojson`) that makes the extract appear in the app's Offline Maps
list. The `.pmtiles` is large (~168 MB) so it is not committed — CI downloads it
from R2 and locally it comes from `app/src/test/res/...`, which you populate by
following the same download step the unit tests use (see
[The boilerplate required]({% link developers/unit-test-example.md %}#the-boilerplate-required)) —
but the small sidecar is committed under `.github/fixtures/`.

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

- **`SwipeAndTap.yaml`** — scrolls a possibly off-screen or partially-visible
  element into view, then taps it. Pass the `id` and a `direction` (`up`, the
  default, reveals items below the fold; `down` reveals items above). The
  standard way to reach and press a button that may be off-screen:

  ```yaml
  - runFlow:
      file: SwipeAndTap.yaml
      env:
        id: "welcomeScreenContinueButton"
        direction: up
  ```

  It deliberately avoids `scrollUntilVisible`: that command swipes from the
  screen centre (the non-scrollable map on the home screen) and Maestro treats a
  partially-visible element as fully visible, so it would stop before the element
  is tappable. Instead `SwipeAndTap` swipes the scrollable region in small steps
  until the element appears, then nudges it clear of the edge so it's not clipped
  when tapped.
- **`Wait.yaml`** — a fixed pause that never fails the flow. It waits for an
  element that intentionally never exists, with `optional: true`, so it simply
  burns the `timeout` (in ms) and carries on. Used to let map tiles or network
  calls settle:

  ```yaml
  - runFlow:
      file: Wait.yaml
      env:
        timeout: 10000
  ```

  We use this because `assertVisible` cannot take a custom timeout (it has a
  fixed 7s auto-retry); `extendedWaitUntil` is what lets us control the
  duration.

- **`WaitForScreen.yaml`** — blocks until a named screen has fully rendered,
  then returns. It maps the `screen` name to an "anchor" `testTag` that is only
  present once that screen has finished composing, and waits for it with
  `extendedWaitUntil` (up to `timeout` ms, default 20000):

  ```yaml
  - runFlow:
      file: WaitForScreen.yaml
      env:
        screen: Home
  ```

  Supported screens are `Home`, `LocationDetails`, `PlacesNearby`,
  `MarkersAndRoutes`, `RouteDetails` and `Settings`; an unknown name fails the
  flow with a clear message. Add a new entry to the `anchors` map in the file to
  support another screen.

  **Why it's needed:** the app does not draw instantly — the initial screen and
  navigation between screens both take a moment to compose, and tapping a
  control before its screen has rendered is a common source of flaky failures.
  Rather than hand-rolling an `extendedWaitUntil` on a different element in every
  flow (and getting the right "this screen is ready" element each time),
  `WaitForScreen.yaml` centralises that anchor per screen so flows just say which
  screen they're waiting for.

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
the shared `SwipeAndTap`/`Wait`/`WaitForScreen` sub-flows, regex matching for
dynamic ids).

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

## Making tests robust

One of the most important things with maestro tests is that they are made as robus as possible against
running on small/large screens and in different languages. The first two is why we use a lot of calls
to `SwipeAndTap` as with small screens the target may be offscreen. For
multiple languages to work we need to use the target id rather than the text it contains.

We also want to deal with different versions of Android, and as much as possible with different
configuration options. We do insist on onboarding being run first, but some thought must be taken
not to make too many dependencies on further tests e.g. an empty markers database. Lots of
dependencies is fine for CI, but for developers it can be annoying trying to figure out what's
required.

The delay whilst the app initial screen draws can cause problems and adding in an initial wait helps
with that. Use the [`WaitForScreen.yaml`](#shared-sub-flows) sub-flow after `launchApp` (and after
any navigation that changes screen) so the flow blocks until the screen has actually rendered, e.g.

```yaml
# Wait for the app to start and the home screen to render
- runFlow:
    file: WaitForScreen.yaml
    env:
      screen: Home
```

## Running in CI

The [`Run Maestro tests`](https://github.com/Scottish-Tech-Army/Soundscape-Android/blob/main/.github/workflows/run-maestro-tests.yaml)
workflow **does not build the app** — it tests an APK produced elsewhere. It can
get that APK in two ways:

- **As a follow-on to `Run tests`** (`workflow_call`). `run-tests.yaml` builds
  the debug APK, uploads it as the `debug-apk` artifact, and then calls this
  workflow as a `maestro` job (`needs: test`) passing `apk_artifact: debug-apk`.
  A Maestro flow failure fails the `Run tests` run (`fail_on_error` defaults to
  `true`).
- **Standalone** from the **Actions** tab (`workflow_dispatch`). With no inputs
  it downloads the **latest GitHub release**'s `release-apk-*.zip` and tests
  that; an optional `release_tag` input (e.g. `soundscape-1.0.12`) tests an older
  release instead. Note this exercises the minified, signed `release` build
  rather than a debug build — `testTag`s survive R8 because they are string
  literals, and the app id is the same, so the flows are unchanged.
- **Automatically on a maestro-only PR.** The
  [`Maestro-only PR tests`](https://github.com/Scottish-Tech-Army/Soundscape-Android/blob/main/.github/workflows/maestro-only-pr.yaml)
  workflow triggers on a pull request that changes **only** files under
  `maestro/`. There is no app code to rebuild in that case, so rather than run
  the full `Run tests` pipeline it calls `run-maestro-tests.yaml` with no APK
  input — which, as in the standalone case, tests the **latest GitHub release**.
  This gives fast feedback when iterating on flows. A `paths:` filter limits the
  trigger to PRs that touch `maestro/`, and a `check-paths` guard job confirms
  the PR touches *only* `maestro/` files (via a `base...head` diff) before the
  Maestro job runs — a PR that also changes app code falls through to the normal
  `Run tests` pipeline instead.

In every case the whole suite runs across a **matrix** of four emulator
configurations, so the flows are exercised on different API levels, screen sizes
and languages. `fail-fast: false`, so one failing combination does not cancel the
others:

| API level | Screen | Device profile | Language |
|-----------|--------|----------------|----------|
| 31 | small | `small_phone` | English (`en-US`) |
| 31 | large | `pixel_6` | English (`en-US`) |
| 35 | small | `small_phone` | English (`en-US`) |
| 35 | large | `pixel_6` | Japanese (`ja-JP`) |

Screen size comes from the runner's `profile` input, which creates the AVD as
that hardware device (`small_phone` for "small", `pixel_6` for "large"), so the
resolution and density are baked in rather than resized at runtime. Language is
set by writing `persist.sys.locale` and restarting the framework (`adb root` is
available because the emulator is a userdebug image) so the whole UI — including
onboarding — comes up in that language; the restart is skipped on the English
legs because the emulator already boots `en-US`. Because the flows select
controls by `testTag` rather than visible text, they are language-agnostic; the
Japanese run exercises layout and text-wrapping rather than asserting on
translated strings. Each matrix leg uploads its own
`maestro-reports-<api>-<screen>-<locale>` artifact (and `logs-…` on failure).

For each matrix combination it then:

1. Locates the downloaded APK (the debug artifact and the release zip put it at
   different paths, so it is found by glob rather than a fixed name).
2. Boots the matrix API level (31 or 35), x86_64, emulator created with the
   matrix device `profile` via `reactivecircus/android-emulator-runner`, and
   applies the matrix locale before installing the app.
3. Installs the APK, seeds the
   [offline map extract and a fixed GPS location](#offline-map-data-and-a-fixed-gps-location)
   (it downloads the Glasgow `.pmtiles`, pushes it plus its committed `.geojson`
   sidecar into the app's offline-extracts directory, and `adb emu geo fix`es to
   the STA office), then runs each flow **individually, in suite order**, each
   producing a JUnit report. Each flow runs even if an earlier one fails (`||
   status=1`), and the step fails at the end if any flow failed:

   ```bash
   status=0
   maestro test --format=junit --output=report1.xml \
       --test-output-dir=maestro_outputs --no-ansi maestro/Onboarding.yaml || status=1
   maestro test --format=junit --output=report2.xml \
       --test-output-dir=maestro_outputs --no-ansi maestro/HomePage.yaml || status=1
   # ... and so on through FullScreenMap.yaml
   exit $status
   ```

   Running every flow regardless of earlier failures means one broken flow no
   longer hides the results of the flows after it — but note the suite is still
   [stateful and ordered](#the-suite-is-stateful-and-ordered), so a flow that
   fails part-way can still leave later flows without the state they expect.

4. Uploads the `maestro_outputs` reports as an artifact, and on failure also
   uploads `app/emulator.log` (a full `logcat` capture) to help diagnose what
   went wrong.

The Maestro step uses `continue-on-error: true` so the report-upload steps
always run; a final step re-raises the failure to mark the workflow red.

When you add a new flow, add a matching `maestro test ... || status=1` line in
the correct position so CI runs it.
