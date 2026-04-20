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
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GeoEngineListener
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.speakCalloutCommon
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.network.IosFileDownloader
import org.scottishtecharmy.soundscape.network.KmpPhotonSearch
import org.scottishtecharmy.soundscape.network.ManifestClient
import org.scottishtecharmy.soundscape.network.OfflineMapManager
import org.scottishtecharmy.soundscape.network.createIosPhotonSearchClient
import org.scottishtecharmy.soundscape.network.createIosVectorTileClient
import org.scottishtecharmy.soundscape.preferences.IosPreferencesProvider
import org.scottishtecharmy.soundscape.utils.Analytics
import platform.Foundation.NSHomeDirectory

/**
 * iOS equivalent of the Android SoundscapeServiceConnection + SoundscapeService.
 * Manages location/direction/audio providers and the GeoEngine.
 * Background operation via iOS's UIBackgroundModes (audio + location).
 */
class IosSoundscapeService : GeoEngineListener {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Providers
    val locationProvider: LocationProvider = IosLocationProvider()
    val directionProvider: DirectionProvider = IosDirectionProvider()
    val audioEngine = IosAudioEngine()
    private val preferencesProvider = IosPreferencesProvider()

    // GeoEngine
    val geoEngine = GeoEngine()
    private var geoEngineStarted = false

    // Database
    val routeDao: RouteDao by lazy {
        MarkersAndRoutesDatabaseProvider.getInstance().routeDao()
    }

    // Offline maps
    private val documentsPath = platform.Foundation.NSHomeDirectory() + "/Documents"
    val offlineMapManager by lazy {
        val manifestClient = ManifestClient(
            io.ktor.client.HttpClient(io.ktor.client.engine.darwin.Darwin) { expectSuccess = false },
            EXTRACT_PROVIDER_URL
        )
        OfflineMapManager(
            manifestClient = manifestClient,
            fileDownloader = IosFileDownloader(),
            extractBasePath = documentsPath,
            extractBaseUrl = EXTRACT_PROVIDER_URL,
        )
    }

    // Grid state flow for UI
    private val _gridStateFlow = MutableStateFlow<GridState?>(null)
    val gridStateFlow: StateFlow<GridState?> = _gridStateFlow.asStateFlow()

    // Places Nearby state
    private val _placesNearbyUiState = MutableStateFlow(PlacesNearbyUiState())
    val placesNearbyUiState: StateFlow<PlacesNearbyUiState> = _placesNearbyUiState.asStateFlow()

    fun placesNearbyClickFolder(filter: String, title: String) {
        _placesNearbyUiState.value = _placesNearbyUiState.value.copy(
            level = 1,
            filter = filter,
            title = title,
        )
    }

    fun placesNearbyClickBack() {
        val current = _placesNearbyUiState.value
        if (current.level > 0) {
            _placesNearbyUiState.value = current.copy(level = 0, filter = "", title = "")
        }
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

    // Service bound state (always true on iOS)
    private val _serviceBoundState = MutableStateFlow(true)
    val serviceBoundState: StateFlow<Boolean> = _serviceBoundState.asStateFlow()

    // Convenience flow accessors
    fun getLocationFlow(): StateFlow<SoundscapeLocation?> = locationProvider.locationFlow
    fun getOrientationFlow(): StateFlow<DeviceDirection?> = directionProvider.orientationFlow
    fun getGridStateFlow(): StateFlow<GridState?> = gridStateFlow

    init {
        startGeoEngine()
    }

    private fun startGeoEngine() {
        if (geoEngineStarted) return

        val tileClient = createIosVectorTileClient(
            baseUrl = TILE_PROVIDER_URL
        )

        val photonClient = createIosPhotonSearchClient(
            baseUrl = SEARCH_PROVIDER_URL
        )
        val photonSearch = KmpPhotonSearch(photonClient)

        val documentsPath = NSHomeDirectory() + "/Documents"

        geoEngine.start(
            newLocationProvider = locationProvider,
            newDirectionProvider = directionProvider,
            listener = this,
            localizedStrings = ComposeLocalizedStrings(),
            preferencesProvider = preferencesProvider,
            analytics = NoOpAnalytics,
            tileClient = tileClient,
            routeDao = routeDao,
            offlineExtractPath = documentsPath,
            hasNetwork = { true }, // TODO: check reachability
            photonSearch = photonSearch,
            platformGeocoder = null,
            streetPreviewEnabled = false,
        )
        geoEngineStarted = true
    }

    // --- GeoEngineListener ---

    override fun isAudioEngineBusy(): Boolean {
        return audioEngine.getQueueDepth() > 0
    }

    private var lastGeometry: UserGeometry? = null
    private var ruler = CheapRuler(0.0)

    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        return speakCalloutCommon(callout, addModeEarcon, audioEngine, lastGeometry, ruler)
    }

    override fun updateAudioEngineGeometry(userGeometry: UserGeometry) {
        lastGeometry = userGeometry
        audioEngine.updateGeometry(
            userGeometry.location.latitude,
            userGeometry.location.longitude,
            userGeometry.presentationHeading(),
            focusGained = true,
            duckingAllowed = true,
            proximityNear = 15.0
        )
    }

    override fun tileGridUpdated() {
        _gridStateFlow.value = geoEngine.gridState

        // Update nearby places from the grid
        scope.launch {
            try {
                val pois = kotlinx.coroutines.withContext(geoEngine.gridState.treeContext) {
                    geoEngine.gridState.getFeatureCollection(TreeId.POIS)
                }
                val intersections = kotlinx.coroutines.withContext(geoEngine.gridState.treeContext) {
                    geoEngine.gridState.getFeatureCollection(TreeId.INTERSECTIONS)
                }
                val location = locationProvider.locationFlow.value
                _placesNearbyUiState.value = _placesNearbyUiState.value.copy(
                    nearbyPlaces = pois,
                    nearbyIntersections = intersections,
                    userLocation = location?.let { LngLatAlt(it.longitude, it.latitude) },
                )
            } catch (e: Exception) {
                println("IosSoundscapeService: Error updating nearby places: ${e.message}")
            }
        }
    }

    override fun updateStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {}
    override fun announceStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {}
    override fun getStreetPreviewChoices(): List<StreetPreviewChoice> = emptyList()
    override fun getStreetPreviewBestChoice(): StreetPreviewChoice? = null
    override val menuActive: Boolean = false

    // --- Beacon Control ---

    fun startBeacon(location: LngLatAlt, name: String) {
        destroyBeacon()
        geoEngine.updateBeaconLocation(location)
        beaconHandle = audioEngine.createBeacon(location, headingOnly = false)
        _beaconFlow.value = BeaconState(location = location, name = name, muteState = false)
    }

    fun destroyBeacon() {
        beaconHandle?.let { audioEngine.destroyBeacon(it) }
        beaconHandle = null
        geoEngine.updateBeaconLocation(null)
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
        if (geoEngineStarted) {
            geoEngine.stop()
            geoEngineStarted = false
        }
        destroyBeacon()
        locationProvider.destroy()
        directionProvider.destroy()
    }

    companion object {
        // Read from Info.plist (values set via Local.xcconfig which is gitignored)
        private val TILE_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("TileProviderURL") as? String ?: ""
        private val SEARCH_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("SearchProviderURL") as? String ?: ""
        private val EXTRACT_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("ExtractProviderURL") as? String ?: ""

        private var INSTANCE: IosSoundscapeService? = null

        fun getInstance(): IosSoundscapeService {
            return INSTANCE ?: IosSoundscapeService().also { INSTANCE = it }
        }
    }
}

private object NoOpAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>?) {}
    override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {}
    override fun crashSetCustomKey(key: String, value: String) {}
    override fun crashLogNotes(name: String) {}
}
