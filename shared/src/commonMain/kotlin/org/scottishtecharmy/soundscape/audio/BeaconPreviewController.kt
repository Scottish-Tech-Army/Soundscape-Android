package org.scottishtecharmy.soundscape.audio

import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

/**
 * Controls a temporary "preview" beacon used by the Settings UI when the
 * user is browsing beacon styles. Drives the start / update / stop
 * lifecycle and saves and restores any real beacon that was running before
 * the preview began.
 *
 * Lives in commonMain so Android and iOS share one implementation. The
 * platform service supplies its [audioEngine] (for direct setBeaconType /
 * createBeacon-handle / destroyBeacon-handle), [preferencesProvider] (to
 * snapshot the saved beacon type for revert-on-cancel), and a back-pointer
 * to itself as a [MediaControllableService] (to read filtered location,
 * orientation, and current beacon flow, and to destroy/recreate the real
 * beacon at start/stop).
 *
 * Persistence on commit is handled by the shared `BeaconStylePreference`
 * dialog via [PreferencesProvider]; this controller never writes prefs.
 */
class BeaconPreviewController(
    private val audioEngine: AudioEngine,
    private val service: MediaControllableService,
    private val preferencesProvider: PreferencesProvider,
) {
    private var previewBeaconHandle: Long? = null
    private var savedBeaconLocation: LngLatAlt? = null
    private var savedBeaconType: String? = null

    /**
     * Begin a beacon style preview. Saves the location of any currently
     * running beacon so it can be restarted later, stops that beacon,
     * sets the engine to [beaconType], and creates a fresh preview
     * beacon. The preview reuses the saved beacon location if present;
     * otherwise it sits 150m straight ahead of the listener.
     */
    fun start(beaconType: String) {
        // Defensive: a stale preview from an aborted prior session would
        // otherwise leak its handle and clobber savedBeacon* below.
        previewBeaconHandle?.let { audioEngine.destroyBeacon(it) }
        previewBeaconHandle = null

        // Remember what was playing so we can restore it on close.
        savedBeaconLocation = service.beaconFlow.value.location
        savedBeaconType = preferencesProvider.getString(
            PreferenceKeys.BEACON_TYPE,
            PreferenceDefaults.BEACON_TYPE,
        )

        // Stop the running real beacon so the preview is the only one
        // playing. The transient null state is invisible to the user
        // because they're in Settings, and stop() restores the real
        // beacon at the same location.
        if (savedBeaconLocation != null) service.destroyBeacon()

        audioEngine.setBeaconType(beaconType)
        createPreviewBeacon()
    }

    /**
     * Switch the running preview to [beaconType]. Engine state only —
     * does not write to preferences, so a subsequent cancel reverts
     * without any persisted side effect.
     */
    fun update(beaconType: String) {
        audioEngine.setBeaconType(beaconType)
        previewBeaconHandle?.let { audioEngine.destroyBeacon(it) }
        previewBeaconHandle = null
        createPreviewBeacon()
    }

    /**
     * Stop the preview. If [commit] is true the chosen style has already
     * been persisted by the caller (via the shared PreferencesProvider)
     * and the engine is left using it; otherwise revert the engine to
     * the type that was active when the preview started. In both cases,
     * the real beacon that was running before (if any) is restarted at
     * the same location.
     */
    fun stop(commit: Boolean, chosenBeaconType: String?) {
        previewBeaconHandle?.let { audioEngine.destroyBeacon(it) }
        previewBeaconHandle = null

        if (!commit || chosenBeaconType == null) {
            savedBeaconType?.let { audioEngine.setBeaconType(it) }
        }

        savedBeaconLocation?.let { loc ->
            service.createBeacon(loc, headingOnly = false)
        }

        savedBeaconLocation = null
        savedBeaconType = null
    }

    private fun createPreviewBeacon() {
        service.requestAudioFocus()

        val previewLocation = savedBeaconLocation ?: run {
            // 150m straight ahead in the direction the device is currently pointing.
            val listener = service.filteredLocationFlow.value
            val base = LngLatAlt(listener?.longitude ?: 0.0, listener?.latitude ?: 0.0)
            val heading = service.orientationFlow.value?.headingDegrees?.toDouble() ?: 0.0
            getDestinationCoordinate(base, heading, 150.0)
        }

        previewBeaconHandle = audioEngine.createBeacon(previewLocation, false)
    }
}
