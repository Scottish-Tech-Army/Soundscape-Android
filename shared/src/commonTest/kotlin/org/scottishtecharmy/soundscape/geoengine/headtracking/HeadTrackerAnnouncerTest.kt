package org.scottishtecharmy.soundscape.geoengine.headtracking

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadTrackerAnnouncement.BatteryLow
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadTrackerAnnouncement.Calibrated
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadTrackerAnnouncement.Connected
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadTrackerAnnouncement.Disconnected
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerState
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HeadTrackerAnnouncerTest {

    private val state = MutableStateFlow<HeadTrackerState?>(null)
    private val announced = mutableListOf<HeadTrackerAnnouncement>()

    private fun TestScope.startAnnouncer() {
        val dispatcher = StandardTestDispatcher(testScheduler)
        HeadTrackerAnnouncer(state, { announced.add(it) }, dispatcher).launchIn(backgroundScope)
        runCurrent()
    }

    private fun TestScope.set(status: HeadTrackingStatus, name: String? = null, battery: Int? = null) {
        state.value = HeadTrackerState(status, name, battery)
        runCurrent()
    }

    private fun TestScope.searching() = set(HeadTrackingStatus.Disconnected)

    @Test
    fun connectWaitsForTheBatteryAndAnnouncesItWithTheConnection() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "WitMotion")
        assertEquals(emptyList(), announced)

        advanceTimeBy(800)
        set(HeadTrackingStatus.Connected, "WitMotion", 70)
        assertEquals(listOf<HeadTrackerAnnouncement>(Connected("WitMotion", 70)), announced)
    }

    @Test
    fun connectWithoutABatteryIsAnnouncedOnceTheWaitRunsOut() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "Bose Frames")
        advanceTimeBy(HeadTrackerAnnouncer.BATTERY_WAIT_MILLIS - 1)
        runCurrent()
        assertEquals(emptyList(), announced)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf<HeadTrackerAnnouncement>(Connected("Bose Frames", null)), announced)
    }

    @Test
    fun calibrationIsAnnounced() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 80)
        assertEquals(listOf(Connected("WitMotion", 80), Calibrated), announced)
    }

    @Test
    fun disconnectIsAnnouncedAfterTheDebounce() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Calibrated, "WitMotion", 80)
        announced.clear()

        searching()
        advanceTimeBy(HeadTrackerAnnouncer.DISCONNECT_DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertEquals(emptyList(), announced)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf<HeadTrackerAnnouncement>(Disconnected("WitMotion")), announced)
    }

    @Test
    fun aQuickReconnectSaysNothingUntilItRecalibrates() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Calibrated, "WitMotion", 80)
        announced.clear()

        searching()
        advanceTimeBy(2_000)
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        advanceTimeBy(HeadTrackerAnnouncer.DISCONNECT_DEBOUNCE_MILLIS * 2)
        runCurrent()
        assertEquals(emptyList(), announced)

        set(HeadTrackingStatus.Calibrated, "WitMotion", 80)
        assertEquals(listOf<HeadTrackerAnnouncement>(Calibrated), announced)
    }

    @Test
    fun aDifferentDeviceReconnectingIsAnnounced() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        announced.clear()

        searching()
        advanceTimeBy(1_000)
        set(HeadTrackingStatus.Connected, "Bose Frames")
        advanceTimeBy(HeadTrackerAnnouncer.BATTERY_WAIT_MILLIS + 1)
        runCurrent()
        assertEquals(listOf<HeadTrackerAnnouncement>(Connected("Bose Frames", null)), announced)
    }

    @Test
    fun lowBatteryIsAnnouncedOncePerThreshold() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Calibrated, "WitMotion", 40)
        announced.clear()

        set(HeadTrackingStatus.Calibrated, "WitMotion", 30)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 20)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 20)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 30) // a reading bouncing back up
        set(HeadTrackingStatus.Calibrated, "WitMotion", 20)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 10)
        set(HeadTrackingStatus.Calibrated, "WitMotion", 0)
        assertEquals(listOf<HeadTrackerAnnouncement>(BatteryLow(20), BatteryLow(10)), announced)
    }

    @Test
    fun alreadyLowAtConnectIsOnlyGivenInTheConnection() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 20)
        set(HeadTrackingStatus.Connected, "WitMotion", 20)
        assertEquals(listOf<HeadTrackerAnnouncement>(Connected("WitMotion", 20)), announced)

        set(HeadTrackingStatus.Connected, "WitMotion", 10)
        assertEquals(listOf(Connected("WitMotion", 20), BatteryLow(10)), announced)
    }

    @Test
    fun switchingOffSaysNothingAndCancelsWhatIsPending() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "Bose Frames")
        set(HeadTrackingStatus.Inactive)
        advanceTimeBy(HeadTrackerAnnouncer.BATTERY_WAIT_MILLIS * 2)
        runCurrent()
        assertEquals(emptyList(), announced)

        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        announced.clear()
        searching()
        set(HeadTrackingStatus.Inactive)
        advanceTimeBy(HeadTrackerAnnouncer.DISCONNECT_DEBOUNCE_MILLIS * 2)
        runCurrent()
        assertEquals(emptyList(), announced)
    }

    @Test
    fun turningBackOnAnnouncesTheConnectionAgain() = runTest {
        startAnnouncer()
        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        set(HeadTrackingStatus.Inactive)
        announced.clear()

        searching()
        set(HeadTrackingStatus.Connected, "WitMotion", 80)
        assertEquals(listOf<HeadTrackerAnnouncement>(Connected("WitMotion", 80)), announced)
    }
}
