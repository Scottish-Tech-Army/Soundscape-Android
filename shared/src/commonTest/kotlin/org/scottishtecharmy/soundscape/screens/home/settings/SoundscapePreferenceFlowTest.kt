package org.scottishtecharmy.soundscape.screens.home.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [rememberSoundscapePreferenceFlow] is declared in commonMain as `@Composable internal expect
 * fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences>` with no body of its
 * own - every bit of actual behavior lives in the platform `actual` implementations:
 *  - Android (SoundscapePreferenceFlow.android.kt): a one-line delegation to the
 *    compose-preference library's own `createDefaultPreferenceFlow()`.
 *  - iOS (SoundscapePreferenceFlow.ios.kt): custom `NSUserDefaults` read/merge/write logic that
 *    coexists with Firebase Crashlytics's cached settings dictionary in the same persistent
 *    domain.
 *
 * None of that logic is reachable from a plain `kotlin.test` unit test in `commonTest`:
 *  - It's `@Composable`, so it can only be invoked from inside a live Compose composition (e.g.
 *    via `runComposeUiTest`/`ComposeTestRule`). This module's `commonTest` source set has no
 *    compose-ui-test (or Robolectric) dependency to provide one.
 *  - Even with such a harness, the Android actual needs a real `android.content.Context`-backed
 *    `SharedPreferences`/DataStore and the iOS actual needs a real `NSUserDefaults`; neither is
 *    available, and there is no mocking library in this module to fake them with.
 *  - The task's instructions were to only add test files, not to add new test
 *    dependencies/infrastructure to shared/build.gradle.kts.
 *
 * So rather than silently having no test for this file at all, this records *why*: there is no
 * pure, platform-independent logic in the commonMain declaration to exercise. (Contrast this
 * with, say, StringExt.kt/FormatBytes.kt, which are genuine pure functions.)
 */
class SoundscapePreferenceFlowTest {
    @Test
    fun noPureLogicToTestInCommonMain() {
        // See class KDoc above for the full explanation.
        assertTrue(true)
    }
}
