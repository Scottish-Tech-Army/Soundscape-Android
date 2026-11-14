package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.filters.StepTracker
import org.scottishtecharmy.soundscape.locationprovider.StepSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StepTrackerTest {

    /** Detector-style snapshot: [count] steps so far, the last at [lastMillis]. */
    private fun detected(count: Long, lastMillis: Long) =
        StepSnapshot(detectedSteps = count, lastStepMillis = lastMillis)

    @Test
    fun walkingIsSteppingAndStoppingEndsItAfterTheWindow() {
        val tracker = StepTracker()
        // Two steps a second for five seconds
        for (second in 1..5) {
            tracker.update(detected(second * 2L, second * 1000L - 300L), second * 1000L)
        }
        assertTrue(tracker.isStepping)
        assertEquals(10L, tracker.recentSteps)

        // Then no more steps: the snapshot keeps reporting the same count. The last 5s still
        // hold four steps at 8s, but only two at 9s.
        assertTrue(tracker.update(detected(10L, 4_700L), 8_000L))
        assertEquals(4L, tracker.recentSteps)
        assertFalse(tracker.update(detected(10L, 4_700L), 9_000L))
        tracker.update(detected(10L, 4_700L), 10_000L)
        assertEquals(0L, tracker.recentSteps)
    }

    @Test
    fun aFewStrayStepsAreNotStepping() {
        val tracker = StepTracker()
        tracker.update(detected(2L, 900L), 1_000L)
        tracker.update(detected(3L, 1_900L), 2_000L)
        assertFalse(tracker.isStepping)
    }

    @Test
    fun noPedometerIsNeverStepping() {
        val tracker = StepTracker()
        for (second in 1..10) assertFalse(tracker.update(null, second * 1000L))
    }

    /** iOS has only CMPedometer's counter, timed by the end of each batch. */
    @Test
    fun theCounterIsUsedWhenThereIsNoDetector() {
        val tracker = StepTracker()
        tracker.update(StepSnapshot(counterSteps = 0L), 1_000L)
        tracker.update(StepSnapshot(counterSteps = 7L, counterMillis = 2_500L), 3_000L)
        assertTrue(tracker.isStepping)
    }

    /** Steps the snapshot says were taken long ago don't count as walking now. */
    @Test
    fun stepsAreTimedByThePedometerNotTheSnapshot() {
        val tracker = StepTracker()
        tracker.update(detected(20L, 1_000L), 30_000L)
        assertFalse(tracker.isStepping)
    }

    @Test
    fun aRestartedCountStartsAgain() {
        val tracker = StepTracker()
        tracker.update(detected(50L, 900L), 1_000L)
        tracker.update(detected(2L, 1_900L), 2_000L)
        assertFalse(tracker.isStepping)
        tracker.update(detected(6L, 2_900L), 3_000L)
        assertTrue(tracker.isStepping)
        assertEquals(4L, tracker.recentSteps)
    }
}
