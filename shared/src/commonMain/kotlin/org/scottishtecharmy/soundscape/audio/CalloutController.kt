package org.scottishtecharmy.soundscape.audio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.speakCalloutCommon
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

/**
 * Drives the four "hear my surroundings" callout buttons and the user-initiated
 * TTS interrupt behaviour they share. Lives in commonMain so Android and iOS run
 * one implementation instead of two hand-copied ones that can drift (as happened
 * once already, requiring a follow-up fix to bring iOS's mode-earcon behaviour
 * back in line with Android's).
 *
 * Follows the same composition pattern as [BeaconPreviewController]: the
 * platform service supplies its [audioEngine], [geoEngine], a back-pointer to
 * itself as [MediaControllableService] (for [MediaControllableService.requestAudioFocus]),
 * and a [CoroutineScope] that already bakes in whatever dispatcher the platform
 * needs the callout pipeline to run on (Android: the service's default-dispatcher
 * scope; iOS: a scope pinned to the GCD-backed queue that also owns AVAudioEngine
 * mutations, so audio-engine calls stay serialized).
 */
class CalloutController(
    private val audioEngine: AudioEngine,
    private val geoEngine: GeoEngine,
    private val service: MediaControllableService,
    private val scope: CoroutineScope,
) {
    private var calloutJob: Job? = null

    // Which button is currently animating. Set by startCallout on launch and
    // cleared when the callout body finishes or is superseded by another
    // button press (via compareAndSet, so a fresh callout doesn't clobber its
    // own value in the previous coroutine's finally block).
    private val _activeCalloutFlow = MutableStateFlow<TourButton?>(null)
    val activeCalloutFlow: StateFlow<TourButton?> = _activeCalloutFlow.asStateFlow()

    private var lastGeometry: UserGeometry? = null
    private var ruler = CheapRuler(0.0)

    /** Called from the platform's GeoEngineListener.updateAudioEngineGeometry override. */
    fun updateGeometry(userGeometry: UserGeometry) {
        lastGeometry = userGeometry
    }

    fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        if (callout == null) return 0L
        if (!service.requestAudioFocus()) {
            println("CalloutController: speakCallout: Could not get audio focus.")
            return 0L
        }
        return speakCalloutCommon(callout, addModeEarcon, audioEngine, lastGeometry, ruler)
    }

    private suspend fun awaitHandle(handle: Long) {
        while (handle != 0L && audioEngine.isHandleActive(handle)) {
            delay(100)
        }
    }

    /**
     * Start a user-initiated callout. Cancels any previous in-flight callout
     * and clears the TTS queue so a button press interrupts (rather than
     * queues behind) existing audio. If a callout was already in progress,
     * the press just cancels it — pressing the same button twice silences
     * the app.
     */
    private fun startCallout(source: TourButton, body: suspend CoroutineScope.() -> Unit) {
        val previousJob = calloutJob
        calloutJob = scope.launch {
            val wasActive = previousJob?.isActive == true
            if (wasActive) previousJob.cancel()

            audioEngine.clearTextToSpeechQueue()

            if (wasActive) {
                // Toggle-off: previous callout was in flight, this press just
                // cancels it. Clear the animation state directly — the previous
                // job's finally block will also fire, but compareAndSet(source, null)
                // is idempotent so double-clear is a no-op.
                _activeCalloutFlow.value = null
                return@launch
            }

            _activeCalloutFlow.value = source
            try {
                body()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failure here (e.g. a button pressed before geoEngine has
                // finished starting) must not escape this launch: scope is a
                // plain root Job, not a SupervisorJob, so an uncaught
                // exception would cancel it and silently disable every future
                // callout button press for the rest of the service's lifetime.
                println("CalloutController: $source callout failed: $e")
            } finally {
                // Only clear if the flow is still us — if a newer callout
                // started while we were cancelled, its coroutine already set
                // the flow to the new source and we mustn't clobber it.
                _activeCalloutFlow.compareAndSet(source, null)
            }
        }
    }

    /**
     * Cancels any in-flight callout without starting another.
     *
     * [startCallout] treats a repeat of an already-playing callout as a cancel, so
     * that pressing a button twice silences the app. An assistant needs the
     * opposite: asking "what's around me?" twice means say it again. The assistant
     * path calls this first so its next request always starts fresh, and clearing
     * [calloutJob] is what makes that work — the next [startCallout] then sees no
     * previous job and takes the normal start path rather than the toggle-off one.
     */
    fun cancel() {
        calloutJob?.cancel()
        calloutJob = null
        _activeCalloutFlow.value = null
    }

    fun myLocation() {
        startCallout(TourButton.MY_LOCATION) {
            if (service.requestAudioFocus()) {
                // myLocation can take a second or so if it does network reverse
                // geocoding — play the enter earcon immediately so the user hears
                // the action registered.
                audioEngine.createEarcon(EARCON_MODE_ENTER, AudioType.STANDARD)
                val results = withContext(Dispatchers.Default) { geoEngine.myLocation() }
                ensureActive()
                var lastHandle = 0L
                if (results != null) {
                    lastHandle = speakCallout(results, false)
                }
                audioEngine.createEarcon(EARCON_MODE_EXIT, AudioType.STANDARD)
                awaitHandle(lastHandle)
            } else {
                println("CalloutController: myLocation: Could not get audio focus.")
            }
        }
    }

    fun whatsAroundMe() {
        startCallout(TourButton.AROUND_ME) {
            val results = withContext(Dispatchers.Default) { geoEngine.whatsAroundMe() }
            ensureActive()
            var lastHandle = 0L
            if (results.positionedStrings.isNotEmpty()) {
                lastHandle = speakCallout(results, true)
            }
            awaitHandle(lastHandle)
        }
    }

    fun aheadOfMe() {
        startCallout(TourButton.AHEAD_OF_ME) {
            val results = withContext(Dispatchers.Default) { geoEngine.aheadOfMe() }
            ensureActive()
            var lastHandle = 0L
            if (results != null) {
                lastHandle = speakCallout(results, true)
            }
            awaitHandle(lastHandle)
        }
    }

    fun nearbyMarkers() {
        startCallout(TourButton.NEARBY_MARKERS) {
            val results = withContext(Dispatchers.Default) { geoEngine.nearbyMarkers() }
            ensureActive()
            val lastHandle = speakCallout(results, true)
            awaitHandle(lastHandle)
        }
    }
}
