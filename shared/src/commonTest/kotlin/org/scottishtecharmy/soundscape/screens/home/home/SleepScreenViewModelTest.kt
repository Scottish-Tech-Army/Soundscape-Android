package org.scottishtecharmy.soundscape.screens.home.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scottishtecharmy.soundscape.locationprovider.Accuracy
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
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
}
