package org.scottishtecharmy.soundscape.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A minimal, fully in-memory [AudioEngine] fake that records every call so tests
 * can assert on what BeaconPreviewController asked it to do, without touching
 * any real audio pipeline. Closely imitates FakeAudioEngine in
 * CalloutControllerTest.kt (renamed here to avoid a same-package clash with it).
 */
private class PreviewFakeAudioEngine : AudioEngine {
    data class CreateBeaconCall(val location: LngLatAlt, val headingOnly: Boolean)

    val createBeaconCalls = mutableListOf<CreateBeaconCall>()
    val destroyBeaconCalls = mutableListOf<Long>()
    val setBeaconTypeCalls = mutableListOf<String>()
    var nextHandle = 1L

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        createBeaconCalls.add(CreateBeaconCall(location, headingOnly))
        return nextHandle++
    }

    override fun destroyBeacon(beaconHandle: Long) {
        destroyBeaconCalls.add(beaconHandle)
    }

    override fun toggleBeaconMute(): Boolean = false

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long = 0L

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long = 0L

    override fun clearTextToSpeechQueue() {}
    override fun getQueueDepth(): Long = 0L
    override fun isHandleActive(handle: Long): Boolean = false

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double,
    ) {}

    override fun setBeaconType(beaconType: String) {
        setBeaconTypeCalls.add(beaconType)
    }

    override fun getListOfBeaconTypes(): Array<String> = emptyArray()
    override fun setSpeechLanguage(language: String): Boolean = true
    override fun onAllBeaconsCleared() {}
    override fun setHrtfEnabled(enabled: Boolean) {}
}

/**
 * A minimal [MediaControllableService] fake, imitating FakeMediaControllableService
 * in CalloutControllerTest.kt (renamed to avoid a same-package clash) but with
 * the flows BeaconPreviewController reads
 * (beaconFlow, filteredLocationFlow, orientationFlow) exposed as mutable so
 * tests can drive them, plus recording of the real-beacon create/destroy calls.
 */
private class PreviewFakeMediaControllableService : MediaControllableService {
    var requestAudioFocusCallCount = 0
    var destroyBeaconCallCount = 0
    val createBeaconCalls = mutableListOf<Pair<LngLatAlt?, Boolean>>()

    override fun routeMute(): Boolean = false
    override fun routeSkipNext(): Boolean = false
    override fun routeSkipPrevious(): Boolean = false
    override fun myLocation() {}
    override fun whatsAroundMe() {}

    override val filteredLocationFlow: MutableStateFlow<SoundscapeLocation?> =
        MutableStateFlow(null)

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) {}

    override fun clearTextToSpeechQueue() {}

    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {
        createBeaconCalls.add(location to headingOnly)
    }

    override fun destroyBeacon() {
        destroyBeaconCallCount++
    }

    override var menuActive: Boolean = false

    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {}
    override fun callbackHoldOff() {}

    override fun requestAudioFocus(): Boolean {
        requestAudioFocusCallCount++
        return true
    }

    override fun aheadOfMe() {}
    override fun nearbyMarkers() {}
    override fun routeStop() {}
    override fun routeStartById(routeId: Long) {}
    override fun startBeacon(location: LngLatAlt, name: String) {}

    override val locationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: MutableStateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = MutableStateFlow(null)
    override val headsetBatteryPercentFlow: StateFlow<Int?> = MutableStateFlow(null)
    override val beaconFlow: MutableStateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> =
        MutableStateFlow(RoutePlayerState())
    override val gridStateFlow: StateFlow<GridState?> = MutableStateFlow(null)
    override val streetPreviewFlow: StateFlow<StreetPreviewState> =
        MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF))
    override val activeCalloutFlow: StateFlow<TourButton?> = MutableStateFlow(null)

    override fun routeStartReverse(routeId: Long) {}
    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        LocationDescription(name = "", location = location)

    override suspend fun searchResult(query: String): List<LocationDescription>? = null
    override fun isAudioEngineBusy(): Boolean = false
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long = 0L
}

/** Simple in-memory [PreferencesProvider] fake, imitating the pattern used by
 *  MarkersViewModelTest.kt / RoutesViewModelTest.kt. */
private class PreviewFakePreferencesProvider : PreferencesProvider {
    private val booleans = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()
    private val floats = mutableMapOf<String, Float>()

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun getString(key: String, default: String): String = strings[key] ?: default
    override fun getFloat(key: String, default: Float): Float = floats[key] ?: default

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun clearAll() {
        booleans.clear()
        strings.clear()
        floats.clear()
    }

    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

class BeaconPreviewControllerTest {

    private lateinit var audioEngine: PreviewFakeAudioEngine
    private lateinit var service: PreviewFakeMediaControllableService
    private lateinit var preferencesProvider: PreviewFakePreferencesProvider

    @BeforeTest
    fun setUp() {
        audioEngine = PreviewFakeAudioEngine()
        service = PreviewFakeMediaControllableService()
        preferencesProvider = PreviewFakePreferencesProvider()
    }

    private fun controller() = BeaconPreviewController(audioEngine, service, preferencesProvider)

    // The location the preview beacon lands at when there was no real beacon
    // running to reuse the location of: 150m straight ahead of the listener,
    // computed with the same helper the production code uses so the test
    // doesn't need to hand-derive the great-circle math.
    private fun aheadOfListener(location: SoundscapeLocation?, headingDegrees: Float?): LngLatAlt {
        val base = LngLatAlt(location?.longitude ?: 0.0, location?.latitude ?: 0.0)
        return getDestinationCoordinate(base, headingDegrees?.toDouble() ?: 0.0, 150.0)
    }

    // ----- start() with no real beacon running -----

    @Test
    fun start_withNoRealBeaconRunning_doesNotDestroyRealBeacon() {
        // beaconFlow defaults to BeaconState(location = null).
        controller().start("Classic")

        assertEquals(0, service.destroyBeaconCallCount)
    }

    @Test
    fun start_withNoRealBeaconRunning_setsEngineTypeAndCreatesPreviewAheadOfListener() {
        service.filteredLocationFlow.value = SoundscapeLocation(latitude = 55.86, longitude = -4.25)
        service.orientationFlow.value = DeviceDirection(
            attitude = FloatArray(4),
            headingDegrees = 90.0f,
            headingAccuracyDegrees = 0.0f,
            elapsedRealtimeNanos = 0L,
        )

        controller().start("Classic")

        assertEquals(listOf("Classic"), audioEngine.setBeaconTypeCalls)
        assertEquals(1, service.requestAudioFocusCallCount)
        assertEquals(1, audioEngine.createBeaconCalls.size)
        val call = audioEngine.createBeaconCalls.single()
        assertEquals(false, call.headingOnly)
        assertEquals(aheadOfListener(service.filteredLocationFlow.value, 90.0f), call.location)
    }

    @Test
    fun start_withNoListenerLocationOrHeading_fallsBackToOriginAndZeroHeading() {
        controller().start("Classic")

        val call = audioEngine.createBeaconCalls.single()
        assertEquals(aheadOfListener(null, null), call.location)
    }

    // ----- start() with a real beacon already running -----

    @Test
    fun start_withRealBeaconRunning_savesAndStopsIt_andPreviewReusesItsLocation() {
        val realLocation = LngLatAlt(-3.2, 55.9)
        service.beaconFlow.value = BeaconState(location = realLocation, name = "Big Ben")
        preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, "Tactile")

        controller().start("Classic")

        // The real beacon was torn down so the preview is the only thing playing.
        assertEquals(1, service.destroyBeaconCallCount)

        // Engine switched to the preview type, not the saved one.
        assertEquals(listOf("Classic"), audioEngine.setBeaconTypeCalls)

        // Preview beacon reuses the real beacon's location rather than the
        // "150m ahead" fallback.
        val call = audioEngine.createBeaconCalls.single()
        assertEquals(realLocation, call.location)
        assertEquals(false, call.headingOnly)
    }

    @Test
    fun start_withRealBeaconRunning_readsSavedTypeFromPreferencesNotDefault() {
        service.beaconFlow.value = BeaconState(location = LngLatAlt(0.0, 0.0))
        preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, "Tactile")
        val controller = controller()

        controller.start("Classic")
        // Revert path (stop without commit) exposes what was captured as the
        // saved type - "Tactile", not the "Classic" preview type or the
        // built-in default.
        controller.stop(commit = false, chosenBeaconType = null)

        assertEquals(listOf("Classic", "Tactile"), audioEngine.setBeaconTypeCalls)
    }

    @Test
    fun start_withNoStoredPreference_savesBuiltInDefaultType() {
        service.beaconFlow.value = BeaconState(location = LngLatAlt(0.0, 0.0))
        val controller = controller()

        controller.start("Classic")
        controller.stop(commit = false, chosenBeaconType = null)

        assertEquals(listOf("Classic", PreferenceDefaults.BEACON_TYPE), audioEngine.setBeaconTypeCalls)
    }

    // ----- start() re-entrancy (defensive stale-preview cleanup) -----

    @Test
    fun start_calledTwiceWithoutStop_destroysStalePreviewHandleBeforeCreatingNew() {
        val controller = controller()
        controller.start("Classic")
        val firstHandle = audioEngine.nextHandle - 1

        controller.start("Tactile")

        assertEquals(listOf(firstHandle), audioEngine.destroyBeaconCalls)
        assertEquals(2, audioEngine.createBeaconCalls.size)
    }

    // ----- update() -----

    @Test
    fun update_switchesEngineType_andReplacesPreviewHandle() {
        val controller = controller()
        controller.start("Classic")
        val firstHandle = audioEngine.nextHandle - 1

        controller.update("Tactile")

        assertEquals(listOf("Classic", "Tactile"), audioEngine.setBeaconTypeCalls)
        assertEquals(listOf(firstHandle), audioEngine.destroyBeaconCalls)
        assertEquals(2, audioEngine.createBeaconCalls.size)
        // requestAudioFocus is asked for again when the preview is recreated.
        assertEquals(2, service.requestAudioFocusCallCount)
    }

    @Test
    fun update_reusesSameSavedLocation_asOriginalStart() {
        val realLocation = LngLatAlt(-3.2, 55.9)
        service.beaconFlow.value = BeaconState(location = realLocation)
        val controller = controller()
        controller.start("Classic")

        controller.update("Tactile")

        val locations = audioEngine.createBeaconCalls.map { it.location }
        assertEquals(listOf(realLocation, realLocation), locations)
    }

    // ----- stop() -----

    @Test
    fun stop_withCommitTrueAndChosenType_leavesEngineOnChosenType_andDoesNotRevert() {
        service.beaconFlow.value = BeaconState(location = LngLatAlt(0.0, 0.0))
        preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, "Tactile")
        val controller = controller()
        controller.start("Classic")

        controller.stop(commit = true, chosenBeaconType = "Classic")

        // No extra setBeaconType call beyond the original preview switch -
        // the engine is left on the committed type.
        assertEquals(listOf("Classic"), audioEngine.setBeaconTypeCalls)
    }

    @Test
    fun stop_withCommitFalse_revertsEngineToSavedType() {
        service.beaconFlow.value = BeaconState(location = LngLatAlt(0.0, 0.0))
        preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, "Tactile")
        val controller = controller()
        controller.start("Classic")

        controller.stop(commit = false, chosenBeaconType = "Classic")

        assertEquals(listOf("Classic", "Tactile"), audioEngine.setBeaconTypeCalls)
    }

    @Test
    fun stop_withCommitTrueButNullChosenType_stillReverts() {
        service.beaconFlow.value = BeaconState(location = LngLatAlt(0.0, 0.0))
        preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, "Tactile")
        val controller = controller()
        controller.start("Classic")

        controller.stop(commit = true, chosenBeaconType = null)

        assertEquals(listOf("Classic", "Tactile"), audioEngine.setBeaconTypeCalls)
    }

    @Test
    fun stop_destroysThePreviewHandle() {
        val controller = controller()
        controller.start("Classic")
        val handle = audioEngine.nextHandle - 1

        controller.stop(commit = true, chosenBeaconType = "Classic")

        assertTrue(audioEngine.destroyBeaconCalls.contains(handle))
    }

    @Test
    fun stop_withRealBeaconSaved_restartsItAtSameLocation() {
        val realLocation = LngLatAlt(-3.2, 55.9)
        service.beaconFlow.value = BeaconState(location = realLocation)
        val controller = controller()
        controller.start("Classic")

        controller.stop(commit = true, chosenBeaconType = "Classic")

        assertEquals(listOf<Pair<LngLatAlt?, Boolean>>(realLocation to false), service.createBeaconCalls)
    }

    @Test
    fun stop_withNoRealBeaconSaved_doesNotCreateARealBeacon() {
        // beaconFlow.value.location is null by default - nothing was running.
        val controller = controller()
        controller.start("Classic")

        controller.stop(commit = true, chosenBeaconType = "Classic")

        assertTrue(service.createBeaconCalls.isEmpty())
    }

    @Test
    fun stop_clearsSavedStateSoASecondStopIsANoOp() {
        val realLocation = LngLatAlt(-3.2, 55.9)
        service.beaconFlow.value = BeaconState(location = realLocation)
        val controller = controller()
        controller.start("Classic")
        controller.stop(commit = true, chosenBeaconType = "Classic")

        // Calling stop() again (e.g. a duplicate lifecycle callback) must not
        // restart the already-restarted real beacon a second time.
        controller.stop(commit = true, chosenBeaconType = "Classic")

        assertEquals(1, service.createBeaconCalls.size)
    }

    // ----- stop() without start() -----

    @Test
    fun stop_withoutEverStarting_doesNotCrashAndTouchesNothing() {
        val controller = controller()

        controller.stop(commit = true, chosenBeaconType = "Classic")

        assertTrue(audioEngine.destroyBeaconCalls.isEmpty())
        assertTrue(audioEngine.setBeaconTypeCalls.isEmpty())
        assertTrue(service.createBeaconCalls.isEmpty())
    }

    @Test
    fun stop_withoutEverStarting_andCommitFalse_doesNotCrashEvenThoughSavedTypeIsNull() {
        val controller = controller()

        // savedBeaconType is null here (never captured by start()), so the
        // revert branch's `savedBeaconType?.let { ... }` must be a safe no-op.
        controller.stop(commit = false, chosenBeaconType = null)

        assertTrue(audioEngine.setBeaconTypeCalls.isEmpty())
    }
}
