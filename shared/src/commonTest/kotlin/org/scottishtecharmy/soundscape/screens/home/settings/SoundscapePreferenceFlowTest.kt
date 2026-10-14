package org.scottishtecharmy.soundscape.screens.home.settings

import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [rememberSoundscapePreferenceFlow] itself can't be tested here - it is @Composable, and each
 * actual needs a real SharedPreferences or NSUserDefaults, neither of which this source set has
 * a harness for. What both actuals share is [publishExternalChange], which is plain Kotlin.
 */
class SoundscapePreferenceFlowTest {

    private fun flowOf(vararg pairs: Pair<String, Any>): MutableStateFlow<Preferences> =
        MutableStateFlow(MapPreferences(mapOf(*pairs)))

    /** A setting changed from the headphone button, the audio menu, Siri or Gemini. */
    @Test
    fun anOutsideChangeReachesTheFlow() {
        val flow = flowOf("CalloutVerbosity" to "Detailed")

        assertTrue(flow.publishExternalChange(mapOf("CalloutVerbosity" to "Quiet")))
        assertEquals("Quiet", flow.value.get<String>("CalloutVerbosity"))
    }

    /**
     * The store reports back every write the flow itself made. Publishing those again would be a
     * new MapPreferences each time - the class has no equals - and the flow and the store would
     * chase each other round for as long as the screen was open.
     */
    @Test
    fun theFlowsOwnWriteComingBackIsIgnored() {
        val flow = flowOf("CalloutVerbosity" to "Quiet", "MeasurementUnits" to "Metric")
        val before = flow.value

        assertFalse(
            flow.publishExternalChange(
                mapOf("MeasurementUnits" to "Metric", "CalloutVerbosity" to "Quiet")
            )
        )
        assertSame(before, flow.value)
    }

    /** A key appearing or disappearing is a change like any other. */
    @Test
    fun keysAddedAndRemovedAreChangesToo() {
        val flow = flowOf("CalloutVerbosity" to "Quiet")

        assertTrue(
            flow.publishExternalChange(
                mapOf("CalloutVerbosity" to "Quiet", "StreetsAndJunctions" to false)
            )
        )
        assertTrue(flow.publishExternalChange(emptyMap()))
        assertEquals(emptyMap(), flow.value.asMap())
    }

    /** Only what changed is written, so keys this screen knows nothing about are left alone. */
    @Test
    fun onlyChangedKeysAreWritten() {
        val edits = preferenceEdits(
            known = mapOf(
                "CalloutVerbosity" to "Detailed",
                "MeasurementUnits" to "Metric",
                "sleep_resume_route_id" to 7L,
            ),
            desired = mapOf(
                "CalloutVerbosity" to "Quiet",
                "MeasurementUnits" to "Metric",
                "sleep_resume_route_id" to 7L,
            ),
        )

        assertEquals(mapOf("CalloutVerbosity" to "Quiet"), edits.changed)
        assertTrue(edits.removed.isEmpty())
    }

    /**
     * A key the flow never held is not the screen's to remove: the service and the migrations
     * write to the same store, and one of those can land between the screen reading it and its
     * listener being registered.
     */
    @Test
    fun aKeyTheScreenNeverSawIsLeftAlone() {
        val edits = preferenceEdits(
            known = mapOf("CalloutVerbosity" to "Quiet"),
            desired = mapOf("CalloutVerbosity" to "Balanced"),
        )

        assertTrue(edits.removed.isEmpty())
        assertEquals(mapOf("CalloutVerbosity" to "Balanced"), edits.changed)
    }

    @Test
    fun aKeyThatHasGoneIsRemovedAndANewOneIsWritten() {
        val edits = preferenceEdits(
            known = mapOf("CalloutVerbosity" to "Quiet", "BeaconType" to "Classic"),
            desired = mapOf("CalloutVerbosity" to "Quiet", "StreetsAndJunctions" to false),
        )

        assertEquals(setOf("BeaconType"), edits.removed)
        assertEquals(mapOf("StreetsAndJunctions" to false), edits.changed)
    }

    @Test
    fun nothingToDoWhenTheyMatch() {
        val same = mapOf("CalloutVerbosity" to "Quiet", "sleep_resume_route_id" to 7L)
        val edits = preferenceEdits(same, same)

        assertTrue(edits.removed.isEmpty())
        assertTrue(edits.changed.isEmpty())
    }
}
