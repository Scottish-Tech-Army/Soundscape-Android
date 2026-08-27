package org.scottishtecharmy.soundscape.audio

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    override val voiceCommandStateFlow: StateFlow<VoiceCommandState> =
        MutableStateFlow(VoiceCommandState.Idle)
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
    // launch through it (see the tests at the bottom of this file).
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

    // ----- startCallout() toggle behaviour / init-order regression -----
    //
    // The four button methods (myLocation/whatsAroundMe/aheadOfMe/nearbyMarkers)
    // delegate straight into GeoEngine, which is a concrete (non-open) class
    // with a large amount of `lateinit var` state that only gets populated by
    // GeoEngine.start() (locationProvider, analytics, geocoder, etc.) - there
    // is no lightweight fake for it anywhere in this codebase, and it can't be
    // subclassed. That makes the "happy path" of these four methods (spoken
    // text, cancel-in-flight-on-second-press, switch-active-button) untestable
    // here without either standing up a fully-started GeoEngine (impractical -
    // it also needs a RouteDao, VectorTileClient, PhotonSearch, etc.) or
    // stubbing GeoEngine itself, which its class design does not allow.
    //
    // What *is* directly testable, and squarely in scope given the SIGSEGV
    // history, is what happens when one of these methods is invoked against a
    // GeoEngine that has not been started yet - the JVM-visible analogue of
    // the "callback fires before setup finished" shape of bug that caused the
    // iOS crash (there the not-yet-assigned collaborator was a `by lazy`
    // delegate field; here it's GeoEngine's own lateinit state). Note this
    // body runs via `withContext(Dispatchers.Default)`, a dispatcher no test
    // scheduler controls, so this test uses real dispatchers/real (bounded)
    // waiting rather than kotlinx-coroutines-test virtual time.
    @Test
    fun myLocation_calledBeforeGeoEngineStarted_resetsActiveFlow_andScopeSurvives() = runBlocking {
        val caught = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable -> caught.add(throwable) }
        val rootJob = Job()
        val scope = CoroutineScope(rootJob + handler)
        // An un-started GeoEngine - mirrors "geoEngine.start() hasn't run yet".
        val controller = controller(geoEngine = GeoEngine(), scope = scope)

        // UNDISPATCHED so the collector's StateFlow subscription is
        // established synchronously, before myLocation() runs on a real
        // background thread (Dispatchers.Default) - otherwise the collector
        // could start after the transient MY_LOCATION -> null round trip has
        // already happened and StateFlow would only ever show it the final
        // value.
        val emissions = mutableListOf<TourButton?>()
        // Signalled from the collector each time a new emission lands, so the
        // waits below suspend until notified instead of polling on a delay -
        // deterministic regardless of CI scheduler contention on the real
        // Dispatchers.Default that myLocation()/whatsAroundMe() dispatch onto.
        val emitted = Channel<Unit>(Channel.UNLIMITED)
        val collectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            controller.activeCalloutFlow.collect {
                emissions.add(it)
                emitted.trySend(Unit)
            }
        }

        controller.myLocation()

        // Wait (with a bound, not a fixed sleep) for the flow to record both
        // the initial null, the MY_LOCATION it flips to synchronously, and
        // the null it resets to once the callout body throws and startCallout's
        // catch/finally run.
        withTimeout(20_000) {
            while (emissions.size < 3) emitted.receive()
        }

        assertEquals(listOf(null, TourButton.MY_LOCATION, null), emissions)

        // The crash is caught inside CalloutController.startCallout itself,
        // so it never escapes the launched coroutine to reach scope's handler...
        assertTrue(caught.isEmpty())

        // ...and the plain root Job this mirrors production with (see
        // SoundscapeService's `private val coroutineScope = CoroutineScope(Job())`,
        // which the real CalloutController is constructed with) is still
        // active, unlike before this was fixed.
        assertTrue(rootJob.isActive)

        // So a subsequent button press on the same scope still runs, rather
        // than being a silent permanent no-op on an already-cancelled parent.
        controller.whatsAroundMe()
        withTimeout(20_000) {
            while (emissions.size < 5) emitted.receive()
        }
        collectJob.cancel()

        assertEquals(
            listOf(null, TourButton.MY_LOCATION, null, TourButton.AROUND_ME, null),
            emissions,
        )
    }
}
