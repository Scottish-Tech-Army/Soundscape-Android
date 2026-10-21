package org.scottishtecharmy.soundscape.screens.home.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scottishtecharmy.soundscape.locationprovider.Accuracy
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SleepScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val _mockLocationProvider = object : LocationProvider() {
        override fun start(accuracy: Accuracy) {
            mutableLocationFlow.value = SoundscapeLocation(47.872, 50.123)
        }

        override fun destroy() {
            // no-op
        }
    }

    // A provider whose fixes the test sets by hand, starting with none at all
    private class ManualLocationProvider : LocationProvider() {
        override fun start(accuracy: Accuracy) {}
        override fun destroy() {}
        fun fix(latitude: Double, longitude: Double) {
            mutableLocationFlow.value = SoundscapeLocation(latitude, longitude)
        }
    }

    // Glasgow, and points roughly 5m, 30m and 50m east of it
    private val startLat = 55.8642
    private val startLon = -4.2518
    private val lon5mEast = -4.25172
    private val lon30mEast = -4.25132
    private val lon50mEast = -4.25100

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onWakeOnLeaveClicked_stateUpdatesToSnoozing() = runTest {
        val vm = SleepScreenViewModel(_mockLocationProvider) {}

        // Initial state should be Sleeping
        assertIs<SleepScreenState.Sleeping>(vm.state.value)

        // Trigger Snoozing
        vm.onWakeOnLeaveClicked()

        // Wait for coroutines in viewModelScope (subscribeToLocation) to process
        testDispatcher.scheduler.advanceUntilIdle()

        // State should now be Snoozing
        assertIs<SleepScreenState.Snoozing>(vm.state.value)
    }

    @Test
    fun onWakeUpNowClicked_callsOnWakeUp() = runTest {
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(_mockLocationProvider) {
            wakeUpCalled = true
        }

        vm.onWakeUpNowClicked()

        assertTrue(wakeUpCalled)
    }

    @Test
    fun onWakeUpNowClicked_wakeUpNowEffectSent() = runTest {
        val vm = SleepScreenViewModel(_mockLocationProvider) {}

        val effects = vm.effects.receiveAsFlow()
        val effectsList: MutableList<SleepScreenEffect> = mutableListOf()
        val collectJob = launch { effects.toCollection(effectsList) }

        vm.onWakeUpNowClicked()

        // Wait for coroutines in viewModelScope (destroy) to process
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<SleepScreenEffect.WakeUpNow>(effectsList[0])
        collectJob.cancel()
    }

    @Test
    fun wakeOnLeave_wakesOnlyOnceFarEnoughFromStart() = runTest {
        val provider = ManualLocationProvider()
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(provider) { wakeUpCalled = true }

        vm.onWakeOnLeaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, startLon)
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, lon5mEast)
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, lon30mEast)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(wakeUpCalled)

        provider.fix(startLat, lon50mEast)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(wakeUpCalled)
    }

    @Test
    fun wakeOnLeave_noFixYetDoesNotBecomeStartLocation() = runTest {
        val provider = ManualLocationProvider()
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(provider) { wakeUpCalled = true }

        // No fix when snoozing starts, then two close together: a made-up (0,0) start would
        // put the second thousands of km away and wake straight away.
        vm.onWakeOnLeaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, startLon)
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, lon5mEast)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(wakeUpCalled)
        val state = assertIs<SleepScreenState.Snoozing>(vm.state.value)
        assertEquals(startLat, state.startLocation?.latitude)
        assertEquals(startLon, state.startLocation?.longitude)
    }

    @Test
    fun wakeOnLeave_stillWakesAfterBeingTurnedOffAndOnAgain() = runTest {
        val provider = ManualLocationProvider()
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(provider) { wakeUpCalled = true }

        vm.onWakeOnLeaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onWakeOnLeaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SleepScreenState.Sleeping>(vm.state.value)

        vm.onWakeOnLeaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, startLon)
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, lon50mEast)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(wakeUpCalled)
    }

    @Test
    fun wakeOnLeave_reportsSnoozeOnAndOff() = runTest {
        val snoozes = mutableListOf<Boolean>()
        val vm = SleepScreenViewModel(
            locationProvider = ManualLocationProvider(),
            onSnooze = { snoozes.add(it) },
            onWakeUp = {},
        )

        vm.onWakeOnLeaveClicked()
        vm.onWakeOnLeaveClicked()

        assertEquals(listOf(true, false), snoozes)
    }

    @Test
    fun wakeOnLeave_currentFixIsStartWhenJobsRunImmediately() = runTest {
        // viewModelScope runs on Dispatchers.Main.immediate, so the jobs start inside
        // onWakeOnLeaveClicked: the fix the provider already has must become the start location.
        Dispatchers.resetMain()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val provider = ManualLocationProvider()
        provider.fix(startLat, startLon)
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(provider) { wakeUpCalled = true }

        vm.onWakeOnLeaveClicked()
        provider.fix(startLat, lon30mEast)
        assertFalse(wakeUpCalled)
        val state = assertIs<SleepScreenState.Snoozing>(vm.state.value)
        assertEquals(startLon, state.startLocation?.longitude)

        provider.fix(startLat, lon50mEast)
        assertTrue(wakeUpCalled)
    }

    @Test
    fun wakeOnLeave_carriesOnWhenRecreatedWhileSnoozing() = runTest {
        val provider = ManualLocationProvider()
        val snoozes = mutableListOf<Boolean>()
        var wakeUpCalled = false
        val vm = SleepScreenViewModel(
            locationProvider = provider,
            onSnooze = { snoozes.add(it) },
            alreadySnoozing = true,
            onWakeUp = { wakeUpCalled = true },
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<SleepScreenState.Snoozing>(vm.state.value)

        provider.fix(startLat, startLon)
        testDispatcher.scheduler.advanceUntilIdle()
        provider.fix(startLat, lon50mEast)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(wakeUpCalled)
        // The platform is already snoozing, so it isn't asked to start again
        assertEquals(emptyList(), snoozes)
    }
}
