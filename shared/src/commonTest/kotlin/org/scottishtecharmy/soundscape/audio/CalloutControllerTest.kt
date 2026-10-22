package org.scottishtecharmy.soundscape.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A minimal, fully in-memory [AudioEngine] fake that records every call so tests
 * can assert on what CalloutController asked it to play, without touching any
 * real audio pipeline.
 */
private class FakeAudioEngine : AudioEngine {
    val ttsCalls = mutableListOf<String>()
    val earconCalls = mutableListOf<String>()
    var clearQueueCallCount = 0
    var nextHandle = 1L
    var activeHandle: Long? = null

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long = 0L
    override fun destroyBeacon(beaconHandle: Long) {}
    override fun toggleBeaconMute(): Boolean = false

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long {
        ttsCalls.add(text)
        return nextHandle++
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ): Long {
        earconCalls.add(asset)
        return nextHandle++
    }

    override fun clearTextToSpeechQueue() {
        clearQueueCallCount++
    }

    override fun getQueueDepth(): Long = 0L

    override fun isHandleActive(handle: Long): Boolean = activeHandle == handle

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double,
    ) {}

    override fun setBeaconType(beaconType: String) {}
    override fun getListOfBeaconTypes(): Array<String> = emptyArray()
    override fun setSpeechLanguage(language: String): Boolean = true
    override fun onAllBeaconsCleared() {}
    override fun setHrtfEnabled(enabled: Boolean) {}
}

/**
 * A minimal [MediaControllableService] fake. CalloutController only actually
 * calls [requestAudioFocus], but the interface has no default for most members
 * so they all need a body.
 */
private class FakeMediaControllableService : MediaControllableService {
    var audioFocusGranted = true
    var requestAudioFocusCallCount = 0

    override fun routeMute(): Boolean = false
    override fun routeSkipNext(): Boolean = false
    override fun routeSkipPrevious(): Boolean = false
    override fun myLocation() {}
    override fun whatsAroundMe() {}

    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> =
        MutableStateFlow(null)

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) {}

    override fun clearTextToSpeechQueue() {}
    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {}
    override fun destroyBeacon() {}

    override var menuActive: Boolean = false

    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {}
    override fun callbackHoldOff() {}

    override fun requestAudioFocus(): Boolean {
        requestAudioFocusCallCount++
        return audioFocusGranted
    }

    override fun aheadOfMe() {}
    override fun nearbyMarkers() {}
    override fun routeStop() {}
    override fun routeStartById(routeId: Long) {}
    override fun startBeacon(location: LngLatAlt, name: String) {}

    override val locationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = MutableStateFlow(null)
    override val headsetBatteryPercentFlow: StateFlow<Int?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
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

class CalloutControllerTest {

    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var service: FakeMediaControllableService

    @BeforeTest
    fun setUp() {
        audioEngine = FakeAudioEngine()
        service = FakeMediaControllableService()
    }

    // speakCallout()/updateGeometry() run synchronously with no coroutines
    // involved, so any throwaway scope does for those tests - it's simply
    // unused. The scope only matters for the four button methods, which
    // launch through it.
    private fun controller(
        geoEngine: GeoEngine = GeoEngine(),
        scope: CoroutineScope = CoroutineScope(Job()),
    ) = CalloutController(audioEngine, geoEngine, service, scope)

    // ----- construction / initial state -----

    @Test
    fun activeCalloutFlow_isNullImmediatelyAfterConstruction() {
        val controller = controller()
        assertNull(controller.activeCalloutFlow.value)
    }

    // ----- speakCallout() -----

    @Test
    fun speakCallout_withNullCallout_returnsZeroAndTouchesNothing() {
        val controller = controller()

        val handle = controller.speakCallout(null, addModeEarcon = false)

        assertEquals(0L, handle)
        assertEquals(0, service.requestAudioFocusCallCount)
        assertTrue(audioEngine.ttsCalls.isEmpty())
    }

    @Test
    fun speakCallout_whenAudioFocusDenied_returnsZeroAndDoesNotSpeak() {
        service.audioFocusGranted = false
        val controller = controller()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello")),
        )

        val handle = controller.speakCallout(callout, addModeEarcon = false)

        assertEquals(0L, handle)
        assertEquals(1, service.requestAudioFocusCallCount)
        assertTrue(audioEngine.ttsCalls.isEmpty())
    }

    @Test
    fun speakCallout_whenAudioFocusGranted_speaksAndReturnsHandle() {
        val controller = controller()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "hello world")),
        )

        val handle = controller.speakCallout(callout, addModeEarcon = false)

        assertEquals(listOf("hello world"), audioEngine.ttsCalls)
        assertTrue(handle != 0L)
    }

    /**
     * Regression test for the class of bug fixed in 6963bd4a6 ("Fix SIGSEGV:
     * declare calloutController before init{} on iOS"): a caller invoking a
     * CalloutController method before setup has fully completed. On iOS the
     * concrete failure was a not-yet-assigned `by lazy` delegate field being
     * dereferenced reentrantly during construction; here the JVM-testable
     * analogue is calling speakCallout() before updateGeometry() has ever run,
     * so lastGeometry is still null. CalloutController must degrade gracefully
     * (fall back to the plain callout text) rather than NPE.
     */
    @Test
    fun speakCallout_beforeUpdateGeometryEverCalled_doesNotCrashAndUsesPlainText() {
        val controller = controller()
        val callout = TrackedCallout(
            positionedStrings = listOf(
                PositionedString(
                    text = "Cafe, 20 metres",
                    location = LngLatAlt(-4.25, 55.86),
                    addDistanceAndHeading = true,
                ),
            ),
        )

        val handle = controller.speakCallout(callout, addModeEarcon = false)

        // No crash, and since lastGeometry is null the distance/heading
        // formatting branch is skipped entirely - the plain text is spoken.
        assertEquals(listOf("Cafe, 20 metres"), audioEngine.ttsCalls)
        assertTrue(handle != 0L)
    }

    @Test
    fun updateGeometry_doesNotThrow_andSpeakCalloutStillWorksAfterwards() {
        val controller = controller()

        controller.updateGeometry(UserGeometry())

        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "still works")),
        )
        val handle = controller.speakCallout(callout, addModeEarcon = false)

        assertEquals(listOf("still works"), audioEngine.ttsCalls)
        assertTrue(handle != 0L)
    }

    @Test
    fun speakCallout_withAddModeEarcon_playsEnterAndExitEarcons() {
        val controller = controller()
        val callout = TrackedCallout(
            positionedStrings = listOf(PositionedString(text = "around you")),
        )

        controller.speakCallout(callout, addModeEarcon = true)

        assertEquals(listOf(EARCON_MODE_ENTER, EARCON_MODE_EXIT), audioEngine.earconCalls)
    }
}
