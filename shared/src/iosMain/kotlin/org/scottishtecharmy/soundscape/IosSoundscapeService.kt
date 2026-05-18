package org.scottishtecharmy.soundscape

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.BeaconPreviewController
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.IosAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GeoEngineListener
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.speakCalloutCommon
import org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.IosGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.intents.resolveRouteByName
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.IosDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosCompositeHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.IosLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
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
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_3
import org.scottishtecharmy.soundscape.resources.preview_go_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayer
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenu
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenuMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import org.scottishtecharmy.soundscape.services.mediacontrol.OriginalMediaControls
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.IosMarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.IosNetworkUtils
import org.scottishtecharmy.soundscape.utils.MarkersAndRoutesIo
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

    // Providers — iosLocationProvider is the device GPS. locationProvider is the
    // currently active provider, which is swapped to a StaticLocationProvider while
    // street preview is on. Head tracking holds a reference to iosLocationProvider
    // directly so it keeps tracking course from real GPS even while previewing.
    val iosLocationProvider: IosLocationProvider = IosLocationProvider()
    var locationProvider: LocationProvider = iosLocationProvider
        private set
    val directionProvider: IosDirectionProvider = IosDirectionProvider()
    val headTrackingProvider: IosCompositeHeadTrackingProvider =
        IosCompositeHeadTrackingProvider(directionProvider, iosLocationProvider)
    val audioEngine = IosAudioEngine()
    val preferencesProvider = IosPreferencesProvider()
    val networkUtils = IosNetworkUtils()

    // GeoEngine
    val geoEngine = GeoEngine()
    private var geoEngineStarted = false
    private val gpxRecorder = GpxRecorder()

    // Database
    val routeDao: RouteDao by lazy { MarkersAndRoutesDatabaseProvider.getInstance().routeDao() }
    val markersAndRoutesIo: MarkersAndRoutesIo = IosMarkersAndRoutesIo()

    // Offline maps
    private val documentsPath = platform.Foundation.NSHomeDirectory() + "/Documents"
    val offlineMapManager by lazy {
        val manifestClient = ManifestClient(
            io.ktor.client.HttpClient(io.ktor.client.engine.darwin.Darwin) {
                expectSuccess = false
            },
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

    // Street preview state
    private val _streetPreviewFlow = MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF))
    override val streetPreviewFlow: StateFlow<StreetPreviewState> = _streetPreviewFlow.asStateFlow()

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
    override val headHeadingFlow: StateFlow<org.scottishtecharmy.soundscape.locationprovider.HeadHeading?>
        get() = headTrackingProvider.headHeadingFlow
    override val currentRouteFlow: StateFlow<RoutePlayerState>
        get() = routePlayer.currentRouteFlow

    // Audio tour — shared with the Compose UI
    val audioTour: AudioTour by lazy {
        AudioTour(object : AudioTourHost {
            override fun isAudioEngineBusy(): Boolean =
                this@IosSoundscapeService.isAudioEngineBusy()

            override fun clearTextToSpeechQueue() {
                this@IosSoundscapeService.clearTextToSpeechQueue()
            }
        })
    }

    // Home state holder is service-scoped because its location/heading flow is
    // consumed across many shared screens. The per-screen holders (markers,
    // routes, places-nearby, add/edit-route) are nav-scoped and instantiated by
    // SharedNavGraph via viewModel { ... } — see MainViewController's create*
    // factory callbacks.
    val homeViewModel by lazy {
        org.scottishtecharmy.soundscape.screens.home.HomeViewModel(this, audioTour)
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

            PreferenceKeys.HEAD_TRACKING_ENABLED -> {
                applyHeadTrackingEnabled()
            }

            PreferenceKeys.SELECTED_TTS_VOICE_ID -> {
                audioEngine.setSpeechVoice(
                    preferencesProvider.getString(
                        PreferenceKeys.SELECTED_TTS_VOICE_ID,
                        PreferenceDefaults.SELECTED_TTS_VOICE_ID,
                    )
                )
                playTtsSample()
            }

            PreferenceKeys.SPEECH_RATE -> {
                audioEngine.setSpeechRate(
                    preferencesProvider.getFloat(
                        PreferenceKeys.SPEECH_RATE,
                        PreferenceDefaults.SPEECH_RATE,
                    )
                )
                playTtsSample()
            }
        }
    }

    private fun applyHeadTrackingEnabled() {
        val enabled = preferencesProvider.getBoolean(
            PreferenceKeys.HEAD_TRACKING_ENABLED,
            PreferenceDefaults.HEAD_TRACKING_ENABLED,
        )
        if (enabled) headTrackingProvider.start() else headTrackingProvider.stop()
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
        audioEngine.setSpeechVoice(
            preferencesProvider.getString(
                PreferenceKeys.SELECTED_TTS_VOICE_ID,
                PreferenceDefaults.SELECTED_TTS_VOICE_ID,
            )
        )
        audioEngine.setSpeechRate(
            preferencesProvider.getFloat(
                PreferenceKeys.SPEECH_RATE,
                PreferenceDefaults.SPEECH_RATE,
            )
        )
        preferencesProvider.addListener(preferencesListener)
        startGeoEngine()
        geoEngine.setHeadTrackingProvider(headTrackingProvider)
        applyHeadTrackingEnabled()
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

    private fun startGeoEngine(streetPreviewEnabled: Boolean = false) {
        val tileClient = createIosVectorTileClient(
            baseUrl = TILE_PROVIDER_URL,
            hasNetwork = { networkUtils.hasNetwork() },
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
            hasNetwork = { networkUtils.hasNetwork() },
            photonSearch = photonSearch,
            platformGeocoder = IosGeocoder(),
            streetPreviewEnabled = streetPreviewEnabled,
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
        if (_streetPreviewFlow.value.enabled == StreetPreviewEnabled.INITIALIZING) {
            val choices = geoEngine.streetPreviewGo()
            _streetPreviewFlow.value = StreetPreviewState(StreetPreviewEnabled.ON, choices)
            geoEngine.recomputeStreetPreviewBestChoice()
        }
        _gridStateFlow.value = geoEngine.gridState
    }

    override fun setStreetPreviewMode(on: Boolean, location: LngLatAlt?) {
        // geoEngine.stop() destroys both providers it was started with — that's
        // the iOS GPS + IosDirectionProvider when entering preview, or the
        // static provider + IosDirectionProvider when leaving it. We replace
        // the location provider as needed and always restart the direction
        // provider, since the phone's compass drives heading in both modes.
        geoEngine.stop()
        geoEngineStarted = false

        if (on) {
            if (location == null) return
            val staticProvider = StaticLocationProvider(location)
            locationProvider = staticProvider
            staticProvider.start()
        } else {
            locationProvider = iosLocationProvider
            iosLocationProvider.start()
        }
        directionProvider.start()

        // Set the StreetPreview state prior to starting the geo engine, otherwise
        // there's a race in the tileGridUpdated callback (mirrors Android).
        _streetPreviewFlow.value =
            StreetPreviewState(if (on) StreetPreviewEnabled.INITIALIZING else StreetPreviewEnabled.OFF)

        startGeoEngine(streetPreviewEnabled = on)
    }

    override fun streetPreviewGo() {
        val choices = geoEngine.streetPreviewGo()
        _streetPreviewFlow.value =
            _streetPreviewFlow.value.copy(choices = choices, bestChoice = null)
        geoEngine.recomputeStreetPreviewBestChoice()
    }

    override fun updateStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {
        _streetPreviewFlow.value = _streetPreviewFlow.value.copy(bestChoice = bestChoice)
    }

    override fun announceStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {
        val compassLabel =
            ComposeLocalizedStrings().get(getCompassLabel(bestChoice.heading.toInt()))
        val go = kotlinx.coroutines.runBlocking { getString(Res.string.preview_go_title) }
        speakText("$go ${bestChoice.name} $compassLabel", AudioType.STANDARD)
    }

    override fun getStreetPreviewChoices(): List<StreetPreviewChoice> =
        _streetPreviewFlow.value.choices

    override fun getStreetPreviewBestChoice(): StreetPreviewChoice? =
        _streetPreviewFlow.value.bestChoice

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

    // Beacon style preview — implementation lives in shared
    // BeaconPreviewController so iOS and Android stay in lockstep.
    private val beaconPreviewController by lazy {
        BeaconPreviewController(audioEngine, this, preferencesProvider)
    }

    fun startBeaconPreview(beaconType: String) =
        beaconPreviewController.start(beaconType)

    fun updateBeaconPreviewType(beaconType: String) =
        beaconPreviewController.update(beaconType)

    fun stopBeaconPreview(commit: Boolean, chosenBeaconType: String?) =
        beaconPreviewController.stop(commit, chosenBeaconType)

    fun toggleBeaconMute() {
        val muted = audioEngine.toggleBeaconMute()
        _beaconFlow.value = _beaconFlow.value.copy(muteState = muted)
    }

    // --- TTS ---

    fun speakCallout(text: String) {
        audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    /**
     * Mirrors NativeAudioEngine on Android: when the user picks a different
     * voice or moves the speech-rate slider, clear any pending callouts and
     * play a short sample so they can hear the change immediately.
     */
    private fun playTtsSample() {
        audioEngine.clearTextToSpeechQueue()
        val sample = kotlinx.coroutines.runBlocking {
            getString(Res.string.first_launch_callouts_example_3)
        }
        audioEngine.createTextToSpeech(sample, AudioType.STANDARD)
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
            iosLocationProvider.pause()
            directionProvider.pause()
            audioEngine.clearTextToSpeechQueue()
            destroyBeacon()
        } else {
            iosLocationProvider.start()
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
        if (locationProvider !== iosLocationProvider) {
            locationProvider.destroy()
        }
        iosLocationProvider.destroy()
        directionProvider.destroy()
    }

    companion object {
        private const val CALLOUT_SUPPRESS_TIMEOUT_MS = 8_000L

        // Read from Info.plist (values set via Local.xcconfig which is gitignored)
        private val TILE_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("TileProviderURL") as? String
                ?: ""
        private val SEARCH_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("SearchProviderURL") as? String
                ?: ""
        private val EXTRACT_PROVIDER_URL: String
            get() = platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("ExtractProviderURL") as? String
                ?: ""

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
