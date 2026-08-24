package org.scottishtecharmy.soundscape

/**
 * JUnit4 category marker for JVM unit tests that are too slow to run on every PR (e.g. they
 * replay every fixture through the full grid/callout engine rather than asserting against a
 * single case). Tests annotated `@Category(NightlyOnlyTest::class)` are excluded from the
 * regular `test` task and only run via the `nightlyUnitTest` task, see app/build.gradle.kts.
 */
interface NightlyOnlyTest
