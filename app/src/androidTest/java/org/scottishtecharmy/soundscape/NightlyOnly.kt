package org.scottishtecharmy.soundscape

/**
 * Marks an instrumented test that is too slow (real-time audio playback, live network tile
 * fetches, etc.) to run on every PR and mostly just checks that nothing crashes. Excluded from
 * run-tests.yaml's `connectedCheck` via `-e notAnnotation`, and run on its own in
 * nightly.yaml via `-e annotation`.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class NightlyOnly
