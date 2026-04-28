package org.scottishtecharmy.soundscape

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.IosAudioEngine
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenu
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenuMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import org.scottishtecharmy.soundscape.services.mediacontrol.OriginalMediaControls
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayer
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GeoEngineListener
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.IosGeocoder
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.speakCalloutCommon
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.intents.resolveRouteByName
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
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.routeToShareJson
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * iOS equivalent of the Android SoundscapeServiceConnection + SoundscapeService.
 * Manages location/direction/audio providers and the GeoEngine.
 * Background operation via iOS's UIBackgroundModes (audio + location).
 */
class IosSoundscapeService : GeoEngineListener, MediaControllableService, ServiceConnection {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var suppressionJob: Job? = null

    // Providers
    val locationProvider: IosLocationProvider = IosLocationProvider()
    val directionProvider: IosDirectionProvider = IosDirectionProvider()
    val audioEngine = IosAudioEngine()
    val preferencesProvider = IosPreferencesProvider()

    // GeoEngine
    val geoEngine = GeoEngine()
    private var geoEngineStarted = false
    private val gpxRecorder = GpxRecorder()

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
    override val gridStateFlow: StateFlow<GridState?> = _gridStateFlow.asStateFlow()

    // Beacon state — uses shared BeaconState from services package
    private val _beaconFlow = MutableStateFlow(BeaconState())
    override val beaconFlow: StateFlow<BeaconState> = _beaconFlow.asStateFlow()
    private var beaconHandle: Long? = null

    // Pending intent flow — populated by Swift IntentBridge from onOpenURL etc.
    private val _pendingIntent = MutableStateFlow<IncomingIntent?>(null)
    val pendingIntent: StateFlow<IncomingIntent?> = _pendingIntent.asStateFlow()

    /**
     * Publishes a parsed inbound intent into the shared navigation pipeline.
     * If the intent is a name-based route launch, the name is resolved against
     * the route DAO on a background coroutine before being republished as a
     * concrete StartRoute(routeId).
     */
    fun publishPendingIntent(intent: IncomingIntent) {
        if (intent is IncomingIntent.StartRouteByName) {
            scope.launch {
                val id = resolveRouteByName(routeDao, intent.name)
                if (id != null) {
                    _pendingIntent.value = IncomingIntent.StartRoute(id)
                }
            }
        } else {
            _pendingIntent.value = intent
        }
    }

    fun pendingIntentHandled() {
        _pendingIntent.value = null
    }

    // Route player and audio menu
    lateinit var routePlayer: RoutePlayer
    lateinit var audioMenu: AudioMenu

    // Service bound state (always true on iOS)
    private val _serviceBoundState = MutableStateFlow(true)
    override val serviceBoundState: StateFlow<Boolean> = _serviceBoundState.asStateFlow()
    override val service: MediaControllableService get() = this

    // Forward MediaControllableService flow surface
    override val locationFlow: StateFlow<SoundscapeLocation?>
        get() = locationProvider.locationFlow
    override val orientationFlow: StateFlow<DeviceDirection?>
        get() = directionProvider.orientationFlow
    override val currentRouteFlow: StateFlow<RoutePlayerState>
        get() = routePlayer.currentRouteFlow

    // Audio tour — shared with the Compose UI
    val audioTour: AudioTour by lazy {
        AudioTour(object : AudioTourHost {
            override fun isAudioEngineBusy(): Boolean = this@IosSoundscapeService.isAudioEngineBusy()
            override fun clearTextToSpeechQueue() { this@IosSoundscapeService.clearTextToSpeechQueue() }
        })
    }

    // Shared state-holders — replace the per-VM iOS reimplementations
    val homeStateHolder by lazy {
        org.scottishtecharmy.soundscape.screens.home.HomeStateHolder(this, audioTour)
    }
    val placesNearbyStateHolder by lazy {
        org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyStateHolder(this, audioTour)
    }
    val markersStateHolder by lazy {
        org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersStateHolder(
            routeDao, preferencesProvider, this,
        )
    }
    val routesStateHolder by lazy {
        org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesStateHolder(
            routeDao, preferencesProvider, this,
        )
    }

    private val preferencesListener = PreferencesListener { key ->
        when (key) {
            PreferenceKeys.BEACON_TYPE -> {
                val type = preferencesProvider.getString(
                    PreferenceKeys.BEACON_TYPE,
                    PreferenceDefaults.BEACON_TYPE,
                )
                audioEngine.setBeaconType(type)
            }
            PreferenceKeys.MIX_AUDIO -> {
                audioEngine.mixWithOthers = preferencesProvider.getBoolean(
                    PreferenceKeys.MIX_AUDIO,
                    PreferenceDefaults.MIX_AUDIO,
                )
            }
            PreferenceKeys.MEDIA_CONTROLS_MODE -> {
                val mode = preferencesProvider.getString(
                    PreferenceKeys.MEDIA_CONTROLS_MODE,
                    PreferenceDefaults.MEDIA_CONTROLS_MODE,
                )
                updateMediaControls(mode)
            }
        }
    }

    init {
        routePlayer = RoutePlayer(this, routeDao)
        audioMenu = AudioMenu(this, routeDao)
        updateMediaControls(
            preferencesProvider.getString(
                PreferenceKeys.MEDIA_CONTROLS_MODE,
                PreferenceDefaults.MEDIA_CONTROLS_MODE,
            )
        )
        audioEngine.mixWithOthers = preferencesProvider.getBoolean(
            PreferenceKeys.MIX_AUDIO,
            PreferenceDefaults.MIX_AUDIO,
        )
        audioEngine.setBeaconType(
            preferencesProvider.getString(
                PreferenceKeys.BEACON_TYPE,
                PreferenceDefaults.BEACON_TYPE,
            )
        )
        preferencesProvider.addListener(preferencesListener)
        startGeoEngine()
        observeAppLifecycle()
    }

    private fun observeAppLifecycle() {
        val center = platform.Foundation.NSNotificationCenter.defaultCenter
        center.addObserverForName(
            platform.UIKit.UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            println("App in FOREGROUND")
            geoEngine.appInForeground = true
        }
        center.addObserverForName(
            platform.UIKit.UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            println("App NOT in FOREGROUND")
            geoEngine.appInForeground = false
        }
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

        geoEngine.locationRecorder = gpxRecorder

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
            platformGeocoder = IosGeocoder(),
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
    }

    override fun updateStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {}
    override fun announceStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {}
    override fun getStreetPreviewChoices(): List<StreetPreviewChoice> = emptyList()
    override fun getStreetPreviewBestChoice(): StreetPreviewChoice? = null
    override var menuActive: Boolean = false

    // --- Routes ---

    fun saveRoute(name: String, description: String, waypoints: List<LocationDescription>) {
        scope.launch {
            try {
                val route = org.scottishtecharmy.soundscape.database.local.model.RouteEntity(
                    name = name,
                    description = description,
                )
                val markers = waypoints.map { wp ->
                    org.scottishtecharmy.soundscape.database.local.model.MarkerEntity(
                        markerId = wp.databaseId,
                        name = wp.name,
                        fullAddress = wp.description ?: "",
                        longitude = wp.location.longitude,
                        latitude = wp.location.latitude,
                    )
                }
                if (markers.all { it.markerId != 0L }) {
                    routeDao.insertRouteWithExistingMarkers(route, markers)
                } else {
                    routeDao.insertRouteWithNewMarkers(route, markers)
                }
                audioEngine.createEarcon(
                    "file:///android_asset/Sounds/sense_poi.wav",
                    org.scottishtecharmy.soundscape.audio.AudioType.STANDARD
                )
            } catch (e: Exception) {
                println("IosSoundscapeService: Failed to save route: ${e.message}")
            }
        }
    }

    fun loadRouteWaypoints(routeId: Long): List<LocationDescription> {
        return kotlinx.coroutines.runBlocking {
            try {
                val routeWithMarkers = routeDao.getRouteWithMarkers(routeId)
                routeWithMarkers?.markers?.map { marker ->
                    LocationDescription(
                        name = marker.name,
                        description = marker.fullAddress,
                        location = LngLatAlt(marker.longitude, marker.latitude),
                        databaseId = marker.markerId,
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                println("IosSoundscapeService: Failed to load route: ${e.message}")
                emptyList()
            }
        }
    }

    fun deleteRoute(routeId: Long) {
        scope.launch {
            try {
                routeDao.removeMarkersForRoute(routeId)
                routeDao.removeRoute(routeId)
            } catch (e: Exception) {
                println("IosSoundscapeService: Failed to delete route: ${e.message}")
            }
        }
    }

    // --- Markers ---

    fun saveMarker(locationDescription: LocationDescription) {
        scope.launch {
            try {
                var name = locationDescription.name
                if (name.isEmpty()) {
                    name = locationDescription.description ?: "Unknown"
                }
                val marker = org.scottishtecharmy.soundscape.database.local.model.MarkerEntity(
                    markerId = locationDescription.databaseId,
                    name = name,
                    fullAddress = locationDescription.description ?: "",
                    longitude = locationDescription.location.longitude,
                    latitude = locationDescription.location.latitude
                )
                if (locationDescription.databaseId != 0L) {
                    routeDao.updateMarker(marker)
                } else {
                    routeDao.insertMarker(marker)
                }
                audioEngine.createEarcon(
                    "file:///android_asset/Sounds/sense_poi.wav",
                    org.scottishtecharmy.soundscape.audio.AudioType.STANDARD
                )
            } catch (e: Exception) {
                println("IosSoundscapeService: Failed to save marker: ${e.message}")
            }
        }
    }

    fun deleteMarker(markerId: Long) {
        scope.launch {
            try {
                routeDao.removeMarker(markerId)
            } catch (e: Exception) {
                println("IosSoundscapeService: Failed to delete marker: ${e.message}")
            }
        }
    }

    // --- GeoEngine Queries ---

    override fun myLocation() {
        val callout = geoEngine.myLocation()
        speakCalloutCommon(callout, false, audioEngine, lastGeometry, ruler)
    }

    override fun whatsAroundMe() {
        val callout = geoEngine.whatsAroundMe()
        speakCalloutCommon(callout, false, audioEngine, lastGeometry, ruler)
    }

    override fun aheadOfMe() {
        val callout = geoEngine.aheadOfMe()
        speakCalloutCommon(callout, false, audioEngine, lastGeometry, ruler)
    }

    override fun nearbyMarkers() {
        val callout = geoEngine.nearbyMarkers()
        speakCalloutCommon(callout, false, audioEngine, lastGeometry, ruler)
    }

    // --- Beacon Control ---

    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {
        if (location == null) return
        val oldBeacon = beaconHandle
        beaconHandle = audioEngine.createBeacon(location, headingOnly)
        oldBeacon?.let { audioEngine.destroyBeacon(it) }
        _beaconFlow.value = _beaconFlow.value.copy(location = location)
        geoEngine.updateBeaconLocation(location)
    }

    override fun destroyBeacon() {
        beaconHandle?.let { audioEngine.destroyBeacon(it) }
        beaconHandle = null
        geoEngine.updateBeaconLocation(null)
        _beaconFlow.value = BeaconState()
    }

    override fun startBeacon(location: LngLatAlt, name: String) {
        routePlayer.startBeacon(location, name)
    }

    fun toggleBeaconMute() {
        val muted = audioEngine.toggleBeaconMute()
        _beaconFlow.value = _beaconFlow.value.copy(muteState = muted)
    }

    // --- TTS ---

    fun speakCallout(text: String) {
        audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    // --- Mix Audio Setting ---

    fun setMixAudio(enabled: Boolean) {
        audioEngine.mixWithOthers = enabled
    }

    // --- Media Controls ---

    fun updateMediaControls(target: String) {
        audioEngine.mediaControlTarget = when (target) {
            "AudioMenu" -> AudioMenuMediaControls(audioMenu)
            else -> OriginalMediaControls(this)
        }
    }

    // --- MediaControllableService ---

    override val filteredLocationFlow: StateFlow<SoundscapeLocation?>
        get() = locationProvider.filteredLocationFlow

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) {
        audioEngine.createTextToSpeech(text, type, latitude, longitude, heading)
    }

    override fun clearTextToSpeechQueue() {
        audioEngine.clearTextToSpeechQueue()
    }

    override fun routeMute(): Boolean {
        if (beaconHandle != null) {
            audioEngine.clearTextToSpeechQueue()
            val muted = audioEngine.toggleBeaconMute()
            _beaconFlow.value = _beaconFlow.value.copy(muteState = muted)
            return true
        }
        return false
    }

    override fun routeSkipNext(): Boolean {
        return routePlayer.moveToNext(true)
    }

    override fun routeSkipPrevious(): Boolean {
        return routePlayer.moveToPrevious(true)
    }

    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {
        if (clearQueue) audioEngine.clearTextToSpeechQueue()
        if (earcon != null) audioEngine.createEarcon(earcon, AudioType.STANDARD)
        if (text.isNotEmpty()) audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    override fun callbackHoldOff() {
        menuActive = true
        suppressionJob?.cancel()
        suppressionJob = scope.launch {
            kotlinx.coroutines.delay(CALLOUT_SUPPRESS_TIMEOUT_MS)
            menuActive = false
        }
    }

    override fun requestAudioFocus(): Boolean {
        // iOS handles audio session activation at engine level — always return true
        return true
    }

    override fun routeStop() {
        routePlayer.stopRoute()
    }

    override fun routeStartById(routeId: Long) {
        routePlayer.startRoute(routeId)
    }

    override fun routeStartReverse(routeId: Long) {
        routePlayer.startRoute(routeId, reverse = true)
    }

    override fun getLocationDescription(location: LngLatAlt): LocationDescription {
        return geoEngine.getLocationDescription(location)
    }

    override suspend fun searchResult(query: String): List<LocationDescription>? {
        return geoEngine.searchResult(query)
    }

    // --- Recording ---

    @OptIn(
        kotlinx.cinterop.ExperimentalForeignApi::class,
        kotlinx.cinterop.BetaInteropApi::class,
    )
    fun writeRecordingFile(): NSURL? {
        val recordingsDir = NSHomeDirectory() + "/Documents/recordings"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = recordingsDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val outputPath = "$recordingsDir/travel.gpx"
        val gpx = kotlinx.coroutines.runBlocking { gpxRecorder.generateGpx() }
        val ok = (NSString.create(string = gpx)).writeToFile(
            path = outputPath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        return if (ok) NSURL.fileURLWithPath(outputPath) else null
    }

    @OptIn(
        kotlinx.cinterop.ExperimentalForeignApi::class,
        kotlinx.cinterop.BetaInteropApi::class,
    )
    fun writeRouteFile(routeId: Long): NSURL? {
        val route = kotlinx.coroutines.runBlocking { routeDao.getRouteWithMarkers(routeId) }
            ?: return null
        val routesDir = NSHomeDirectory() + "/Documents/routes"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = routesDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val safeName = route.route.name
            .replace(Regex("[/\\\\:*?\"<>|\\x00]"), "_")
            .take(100)
        val timestamp = platform.Foundation.NSDateFormatter().apply {
            dateFormat = "yyyyMMdd_HHmm"
        }.stringFromDate(platform.Foundation.NSDate())
        val outputPath = "$routesDir/soundscape-route-$safeName-$timestamp.json"
        val ok = (NSString.create(string = routeToShareJson(route))).writeToFile(
            path = outputPath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        return if (ok) NSURL.fileURLWithPath(outputPath) else null
    }

    // --- Sleep mode ---

    fun setSleeping(sleeping: Boolean) {
        // Mirror the Android foreground-service shutdown: stop the location
        // and direction providers so GeoEngine sees no new updates (no grid
        // tile fetches, no automatic callouts), silence in-flight audio, and
        // gate any callouts that slip through via the existing menuActive flag.
        menuActive = sleeping
        if (sleeping) {
            locationProvider.pause()
            directionProvider.pause()
            audioEngine.clearTextToSpeechQueue()
            destroyBeacon()
        } else {
            locationProvider.start()
            directionProvider.start()
        }
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
        private const val CALLOUT_SUPPRESS_TIMEOUT_MS = 8_000L

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
