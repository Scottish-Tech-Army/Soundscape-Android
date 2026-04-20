package org.scottishtecharmy.soundscape

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.IosAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation

/**
 * iOS equivalent of the Android SoundscapeServiceConnection + SoundscapeService.
 * Unlike Android's foreground Service, this is a simple singleton that manages
 * the location/direction/audio providers and exposes flows for the UI.
 *
 * Background operation is handled by iOS's UIBackgroundModes (audio + location)
 * configured in Info.plist.
 */
class IosSoundscapeService {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Providers
    val locationProvider: LocationProvider = IosLocationProvider()
    val directionProvider: DirectionProvider = IosDirectionProvider()
    val audioEngine = IosAudioEngine()

    // Database
    val routeDao: RouteDao by lazy {
        MarkersAndRoutesDatabaseProvider.getInstance().routeDao()
    }

    // Beacon state
    data class BeaconState(
        val location: LngLatAlt? = null,
        val name: String = "",
        val muteState: Boolean = false
    )

    private val _beaconFlow = MutableStateFlow(BeaconState())
    val beaconFlow: StateFlow<BeaconState> = _beaconFlow.asStateFlow()
    private var beaconHandle: Long? = null

    // Service bound state (always true on iOS — no binding needed)
    private val _serviceBoundState = MutableStateFlow(true)
    val serviceBoundState: StateFlow<Boolean> = _serviceBoundState.asStateFlow()

    // Convenience flow accessors
    fun getLocationFlow(): StateFlow<SoundscapeLocation?> = locationProvider.locationFlow
    fun getOrientationFlow(): StateFlow<DeviceDirection?> = directionProvider.orientationFlow

    // Audio engine geometry updates
    private var geometryJob: Job? = null

    init {
        startGeometryUpdates()
    }

    private fun startGeometryUpdates() {
        geometryJob?.cancel()
        geometryJob = scope.launch {
            // Combine location and heading updates to drive the audio engine
            locationProvider.locationFlow.collect { location ->
                if (location != null) {
                    val heading = directionProvider.orientationFlow.value?.headingDegrees?.toDouble()
                    audioEngine.updateGeometry(
                        listenerLatitude = location.latitude,
                        listenerLongitude = location.longitude,
                        listenerHeading = heading,
                        focusGained = true,
                        duckingAllowed = true,
                        proximityNear = 0.0
                    )
                }
            }
        }
    }

    // --- Beacon Control ---

    fun startBeacon(location: LngLatAlt, name: String) {
        // Destroy existing beacon first
        destroyBeacon()

        beaconHandle = audioEngine.createBeacon(location, headingOnly = false)
        _beaconFlow.value = BeaconState(location = location, name = name, muteState = false)
    }

    fun destroyBeacon() {
        beaconHandle?.let { handle ->
            audioEngine.destroyBeacon(handle)
        }
        beaconHandle = null
        _beaconFlow.value = BeaconState()
    }

    fun toggleBeaconMute() {
        val muted = audioEngine.toggleBeaconMute()
        _beaconFlow.value = _beaconFlow.value.copy(muteState = muted)
    }

    // --- TTS ---

    fun speakCallout(text: String) {
        audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    // --- Lifecycle ---

    fun destroy() {
        geometryJob?.cancel()
        destroyBeacon()
        locationProvider.destroy()
        directionProvider.destroy()
    }

    companion object {
        private var INSTANCE: IosSoundscapeService? = null

        fun getInstance(): IosSoundscapeService {
            return INSTANCE ?: IosSoundscapeService().also { INSTANCE = it }
        }
    }
}
