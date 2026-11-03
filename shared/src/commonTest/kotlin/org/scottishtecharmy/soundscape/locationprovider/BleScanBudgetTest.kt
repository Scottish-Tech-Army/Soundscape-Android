package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BleScanBudgetTest {

    private fun TestScope.budget() = BleScanBudget(maxStarts = 5, windowMillis = 30_000L) {
        testScheduler.currentTime
    }

    @Test
    fun startsWithinTheLimitDontWait() = runTest {
        val budget = budget()
        budget.awaitStarts(2)
        budget.awaitStarts(2)
        budget.awaitStarts(1)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun aStartBeyondTheLimitWaitsForTheOldestToLeaveTheWindow() = runTest {
        val budget = budget()
        // A WitMotion failing every 2 s, two scans an attempt
        budget.awaitStarts(2)
        testScheduler.advanceTimeBy(2_000L)
        budget.awaitStarts(2)
        testScheduler.advanceTimeBy(2_000L)
        // Only one more fits: this pair waits until the first pair is 30 s old
        budget.awaitStarts(2)
        assertEquals(30_000L, testScheduler.currentTime)
    }

    @Test
    fun clientsSharingABudgetTakeTurns() = runTest {
        val budget = budget()
        val started = mutableListOf<Pair<String, Long>>()
        budget.awaitStarts(2)
        budget.awaitStarts(2)
        launch { budget.awaitStarts(2); started.add("witmotion" to testScheduler.currentTime) }
        launch { budget.awaitStarts(1); started.add("bose" to testScheduler.currentTime) }
        runCurrent()
        testScheduler.advanceUntilIdle()
        // The WitMotion asked first and waits for room for both its scans; Bose then fits after it
        assertEquals(listOf("witmotion" to 30_000L, "bose" to 30_000L), started)
    }
}
