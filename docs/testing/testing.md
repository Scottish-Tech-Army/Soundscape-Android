---
title: Testing
layout: page
nav_order: 11
has_toc: true
---

Soundscape has a large automated test suite - over 1200 tests in total - which runs on every pull
request:

* **Shared logic** (~850 tests in `shared/src/commonTest`). These cover the geo-engine that decides
  what to call out and when: tile parsing, road and intersection detection, routing, and the callout
  generators themselves. They live in the Kotlin Multiplatform `shared` module, which is the same
  code the iOS app uses, so this suite covers the heart of both apps. CI also compiles it for
  Kotlin/Native to catch iOS-specific breakage.
* **App unit tests** (~280 tests in `app/src/test`) for the Android-specific layers.
* **Instrumented tests** (~75 tests in `app/src/androidTest`) which run on an emulator in CI and
  exercise code that needs a real Android device.
* **Maestro UI flows** (`maestro/`) which drive the app as a user would - onboarding, the home
  screen, creating markers and routes, Places Nearby, location details and the full screen map.

Many of these tests run against real offline map extracts rather than hand-written fixtures, so the
behaviour they check is the behaviour you'd get in the field.

Alongside the automated tests we still value user testing. Automation is very good at catching
regressions in what the app decides to say; it's much less good at judging whether the result is
genuinely useful when you're out walking. There's a [smoke test](smoke_test.md) for checking a
release by hand, and feedback from people using the app remains an important part of how we test.
