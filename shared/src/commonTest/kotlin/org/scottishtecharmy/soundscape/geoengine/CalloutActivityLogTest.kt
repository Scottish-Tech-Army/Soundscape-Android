package org.scottishtecharmy.soundscape.geoengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalloutActivityLogTest {

    private val lines = mutableListOf<String>()
    private val log = CalloutActivityLog(
        log = { lines += it },
        describeSettings = { "Quiet; places Landmarks,Transit" },
        periodMillis = 60_000L,
    )

    // The once-a-period summaries only, without the first verdict and transitions
    private fun summaries() = lines.filter { it.startsWith("Callouts in last") }

    @Test
    fun summarisesEachPeriodOnTheFirstFixAfterIt() {
        log.fix(0, stationary = true, inVehicle = false, accuracyMetres = 8.0)
        log.fix(1_000, stationary = true, inVehicle = false, accuracyMetres = 12.0)
        log.skipped(audioEngineBusy = true)
        log.fix(2_000, stationary = true, inVehicle = false, accuracyMetres = 30.0)
        log.skipped(audioEngineBusy = false)
        log.unusableFix(3_000, accuracyMetres = 70.0)
        assertTrue(summaries().isEmpty())

        log.fix(60_000, stationary = true, inVehicle = false)
        assertEquals(
            listOf(
                // Median of 8, 12, 30 and 70 - the unusable fix counts too
                "Callouts in last 60s: 3 fixes (+1 unusable), accuracy median 21m, stationary 3, " +
                    "in vehicle 0, audio busy 1, menu open 1, produced 0 " +
                    "[Quiet; places Landmarks,Transit]"
            ),
            summaries()
        )
    }

    @Test
    fun countsFixesWhileTheStationaryDetectorIsUndecided() {
        for (t in 0L until 40_000L step 1_000L) {
            log.fix(
                t, stationary = false, inVehicle = false,
                stationaryDecided = t >= 30_000L, accuracyMetres = 35.0
            )
        }
        log.fix(60_000, stationary = false, inVehicle = false)
        assertEquals(
            listOf(
                "Callouts in last 60s: 40 fixes, accuracy median 35m, stationary 0 " +
                    "(undecided 30), in vehicle 0, audio busy 0, menu open 0, produced 0 " +
                    "[Quiet; places Landmarks,Transit]"
            ),
            summaries()
        )
    }

    @Test
    fun saysWhenNoFixHadAnAccuracy() {
        log.fix(0, stationary = false, inVehicle = false)
        log.fix(60_000, stationary = false, inVehicle = false)
        assertTrue(summaries().single().contains(", no accuracy reported, "), summaries().single())
    }

    @Test
    fun countsStartAgainAfterASummary() {
        log.fix(0, stationary = false, inVehicle = false)
        log.callout()
        log.fix(60_000, stationary = false, inVehicle = false)
        log.callout()
        log.fix(120_000, stationary = false, inVehicle = false)
        val summaries = summaries()
        assertEquals(2, summaries.size)
        assertTrue(summaries[0].contains("1 fixes"), summaries[0])
        assertTrue(summaries[0].contains("produced 1"), summaries[0])
        assertTrue(summaries[1].contains("1 fixes"), summaries[1])
        assertTrue(summaries[1].contains("produced 1"), summaries[1])
    }

    @Test
    fun saysNothingForAPeriodWithNoFixes() {
        log.fix(0, stationary = false, inVehicle = false)
        log.fix(60_000, stationary = false, inVehicle = false)
        lines.clear()
        // The next fix is long after the last: the period it closes held only the fix at 60s
        log.fix(500_000, stationary = false, inVehicle = false)
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("Callouts in last 440s: 1 fixes"), lines[0])
    }

    @Test
    fun logsTheFirstVerdictThenTransitions() {
        log.fix(0, stationary = true, inVehicle = false)
        log.fix(1_000, stationary = true, inVehicle = false)
        log.fix(2_000, stationary = false, inVehicle = false)
        log.fix(3_000, stationary = false, inVehicle = true)
        log.fix(4_000, stationary = true, inVehicle = false)
        assertEquals(
            listOf(
                "User is stationary (first verdict, 0s after the first fix)",
                "User is moving again",
                "User is now in a vehicle",
                "User is now stationary",
                "User is no longer in a vehicle",
            ),
            lines
        )
    }

    @Test
    fun waitsForTheStationaryDetectorToDecide() {
        // Still filling its window: "not stationary" means nothing yet, so nothing is said
        for (t in 0L until 30_000L step 1_000L) {
            log.fix(t, stationary = false, inVehicle = false, stationaryDecided = false)
        }
        assertTrue(lines.isEmpty(), lines.toString())
        log.fix(31_000, stationary = true, inVehicle = false, stationaryDecided = true)
        assertEquals(listOf("User is stationary (first verdict, 31s after the first fix)"), lines)
    }

    @Test
    fun aResetStartsTheFirstVerdictAgain() {
        log.fix(0, stationary = false, inVehicle = false)
        log.reset()
        log.fix(500_000, stationary = true, inVehicle = true)
        assertEquals(
            listOf(
                "User is moving (first verdict, 0s after the first fix)",
                // No summary for the old period, and no transition from the old run's state
                "User is stationary, in a vehicle (first verdict, 0s after the first fix)",
            ),
            lines
        )
    }
}
