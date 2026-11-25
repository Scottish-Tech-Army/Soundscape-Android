package org.scottishtecharmy.soundscape

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.actions.ActionResult
import org.scottishtecharmy.soundscape.actions.SoundscapeAction
import org.scottishtecharmy.soundscape.actions.SoundscapeActionExecutor
import org.scottishtecharmy.soundscape.actions.executeForDeepLink
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.backup.IosCloudBackup
import org.scottishtecharmy.soundscape.migration.hasPendingLegacyMigration
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.BeaconPreviewController
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.CalloutController
import org.scottishtecharmy.soundscape.audio.IosAudioEngine
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GeoEngineListener
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.callouts.CalloutVerbosity
import org.scottishtecharmy.soundscape.geoengine.callouts.cycleCalloutVerbosity
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.IosGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.IosDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosStepProvider
import org.scottishtecharmy.soundscape.locationprovider.IosCompositeHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.IosLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import org.scottishtecharmy.soundscape.network.IosFileDownloader
import org.scottishtecharmy.soundscape.network.KmpPhotonSearch
import org.scottishtecharmy.soundscape.network.OfflineMapManager
import org.scottishtecharmy.soundscape.network.createIosManifestClient
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
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadTrackerAnnouncer
import org.scottishtecharmy.soundscape.geoengine.headtracking.text
import org.scottishtecharmy.soundscape.services.mediacontrol.OriginalMediaControls
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.IosMarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.IosNetworkUtils
import org.scottishtecharmy.soundscape.utils.MarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.routeToShareJson
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.preferredLanguages
import platform.Foundation.writeToFile

private val logger = AppLog.tag("IosSoundscapeService")

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

    // Swift injects a Firebase-backed impl on Release via
    // FirebaseBootstrap.configureIfEnabled(). Default stays NoOp so Debug
    // builds and unit tests do not touch Firebase.
    private val analytics: Analytics = analyticsFactory()

    // GeoEngine
    val geoEngine = GeoEngine()
    private var geoEngineStarted = false
    private val gpxRecorder = GpxRecorder()
    val stepProvider = IosStepProvider()

    // Callout buttons and user-initiated-TTS-interrupt behaviour — implementation
    // lives in shared CalloutController so iOS and Android stay in lockstep. Runs
    // on a scope pinned to [IosAudioEngine.audioDispatcher] — the serial GCD queue
    // that also owns the TTS render callback and DiscretePlayer completion in
    // IosAudioEngine. Keeping the whole pipeline on that queue serializes the
    // AVAudioEngine mutations (avoiding the `[_nodes containsObject: node]` crash
    // we hit previously) *and* keeps callout audio decoupled from main-thread
    // stalls — e.g. the ~100–500 ms hiccup when iOS instantiates the soft keyboard
    // for the first time after the user taps the search bar mid-callout, which
    // used to gap out the audio between utterances.
    //
    // Declared here (before init{}, alongside audioEngine/geoEngine) rather than
    // further down the file: geoEngine.start() below can synchronously deliver a
    // location update that calls back into updateAudioEngineGeometry()/speakCallout()
    // before this object has finished constructing, so calloutController's `by lazy`
    // delegate must already exist by then — declaring it after init{} left the
    // delegate field null during that reentrant call and crashed with SIGSEGV.
    private val calloutScope by lazy {
        CoroutineScope(scope.coroutineContext + audioEngine.audioDispatcher)
    }
    private val calloutController by lazy {
        CalloutController(audioEngine, geoEngine, this, calloutScope)
    }

    // Database
    val routeDao: RouteDao by lazy { MarkersAndRoutesDatabaseProvider.getInstance().routeDao() }
    val markersAndRoutesIo: MarkersAndRoutesIo = IosMarkersAndRoutesIo()

    // Offline maps
    private val documentsPath = platform.Foundation.NSHomeDirectory() + "/Documents"
    val offlineMapManager by lazy {
        OfflineMapManager(
            manifestClient = createIosManifestClient(EXTRACT_PROVIDER_URL),
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
     *
     * StartRouteByName used to be resolved to a concrete StartRoute(routeId) here
     * before publishing. It no longer is: SharedNavGraph hands it to
     * AppCallbacks.onStartRouteByName, which goes through [SoundscapeActionExecutor]
     * like Android's does, so name resolution lives in one place rather than three.
     */
    fun publishPendingIntent(intent: IncomingIntent) {
        _pendingIntent.value = intent
    }

    fun pendingIntentHandled() {
        _pendingIntent.value = null
    }

    // Route player and audio menu
    val routePlayer: RoutePlayer = RoutePlayer(this, routeDao)
    val audioMenu: AudioMenu = AudioMenu(this, routeDao)

    /**
     * Entry point for assistant commands (Siri App Intents). Reached from Swift via
     * IosSoundscapeService.companion.getInstance().actions.
     */
    val actions: SoundscapeActionExecutor = SoundscapeActionExecutor(this, routeDao)

    /**
     * Invoked after a marker or route is added, changed or removed. Swift uses it to
     * refresh the vocabulary Siri matches spoken names against, which otherwise keeps
     * answering from the list as it was when the app last started — a marker saved this
     * session is unrecognisable by voice until something rebuilds it.
     *
     * A callback rather than a Flow because the one consumer is Swift, and bridging a
     * Kotlin Flow across for a bare "something changed" signal buys nothing.
     */
    var onMarkersOrRoutesChanged: (() -> Unit)? = null

    /**
     * Keeps the user's markers and routes in iCloud so that deleting the app or replacing a phone
     * doesn't lose them. See [IosCloudBackup] for what it does and doesn't promise.
     */
    private val cloudBackup = IosCloudBackup(
        dao = routeDao,
        scope = scope,
        // The legacy import fills an empty library too, from a payload staged before the app
        // opened. Whichever of the two would go first, letting both run gives the user two of
        // everything, so the backup stands aside until the import has been dealt with.
        canRestore = { !hasPendingLegacyMigration() },
        onRestored = { onMarkersOrRoutesChanged?.invoke() },
    )

    /**
     * Announces that the user's markers or routes have changed: tells Swift, so it can rebuild the
     * vocabulary Siri matches names against, and refreshes the iCloud backup.
     *
     * Everything that writes markers or routes goes through here rather than calling the two
     * separately, so a new write path can't quietly stop being backed up.
     */
    private suspend fun markersOrRoutesChanged() {
        onMarkersOrRoutesChanged?.invoke()
        cloudBackup.backupNow()
    }

    /**
     * Runs an action and drops the result. For the AppCallbacks lambdas, which are
     * plain (String) -> Unit with no scope of their own — this keeps them off a
     * freshly-allocated CoroutineScope per call. Swift's App Intents await
     * [actions] directly instead, because they need the result to report back.
     */
    fun performAction(action: SoundscapeAction) {
        scope.launch {
            val result = actions.execute(action)
            if (result !is ActionResult.Ok) {
                logger.d { "$action -> $result" }
            }
        }
    }

    /** An audio menu action from a soundscape:// link - see IncomingIntent.PerformAction. */
    fun performDeepLinkAction(action: SoundscapeAction) {
        scope.launch {
            actions.executeForDeepLink(action, this@IosSoundscapeService)
        }
    }

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
    override val headTrackerStateFlow: StateFlow<org.scottishtecharmy.soundscape.locationprovider.HeadTrackerState?>
        get() = headTrackingProvider.stateFlow
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

            PreferenceKeys.FIRST_LAUNCH -> {
                // SharedOnboardingNavHost's onFinish (SharedNavGraph.kt) writes this to
                // false right before navigating to Home — the signal that onboarding has
                // finished and it's now safe to let the geo engine's callouts start.
                releaseGeoEngineStartAfterOnboarding()
            }

            PreferenceKeys.SELECTED_TTS_VOICE_ID -> {
                audioEngine.setSpeechVoice(
                    preferencesProvider.getString(
                        PreferenceKeys.SELECTED_TTS_VOICE_ID,
                        PreferenceDefaults.SELECTED_TTS_VOICE_ID,
                    )
                )
                // No sample here, unlike the speech rate below: the only thing that writes this
                // is the voice picker, and it got here by previewing the voice the user has just
                // confirmed - see previewTtsVoice. Speaking it again would repeat what they heard
                // a moment ago, over the top of whatever a screen reader says as the picker
                // closes.
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
        // Starting the geo engine is what brings up location updates, and its first
        // callouts are TTS. Two things must be out of the way before that's safe: the
        // Double Tap attribution clip the splash plays on a new minor version
        // (SplashView.swift), where Swift asks us to hold the start back until the clip
        // has finished, and - on a first-ever launch - the onboarding flow, whose own
        // screens the engine's callouts would otherwise talk over. (Android doesn't have
        // this second problem: its onboarding runs in a separate Activity that never
        // starts the service at all - see OnboardingActivity.kt.) Nothing holds either on
        // the background launches the system makes to perform an App Intent, where no
        // splash or onboarding UI runs and nothing would release the splash hold; that
        // can't happen for the onboarding hold, since Siri has no vocabulary to invoke an
        // App Intent with until the app has been opened once.
        if (preferencesProvider.getBoolean(PreferenceKeys.FIRST_LAUNCH, PreferenceDefaults.FIRST_LAUNCH)) {
            holdGeoEngineStartForOnboarding()
        }
        if (claimStartHold(this)) {
            // Backstops, mirroring MainActivity's maxSplashDelay: if the splash never
            // reports back (a released AVAudioPlayer, a view that never appeared), or
            // onboarding never flips FIRST_LAUNCH to false (a nav-graph bug, or a deep
            // link that reaches Home without going through FinishScreen), start anyway
            // rather than leaving the app without location or callouts forever. The
            // onboarding one is long because onboarding is legitimately user-paced and
            // must never be cut off mid-flow in ordinary use.
            scope.launch {
                kotlinx.coroutines.delay(SPLASH_HOLD_TIMEOUT_MS)
                releaseGeoEngineStartAfterSplash()
            }
            scope.launch {
                kotlinx.coroutines.delay(ONBOARDING_HOLD_TIMEOUT_MS)
                releaseGeoEngineStartAfterOnboarding()
            }
        } else {
            startGeoEngine()
        }
        geoEngine.setHeadTrackingProvider(headTrackingProvider)
        // Speak head tracker connections, calibration, disconnections and low battery, as the
        // Android service does
        HeadTrackerAnnouncer(
            headTrackingProvider.stateFlow,
            { announcement -> speak2dText(announcement.text()) },
        ).launchIn(scope)
        applyHeadTrackingEnabled()
        observeAppLifecycle()
        cloudBackup.start()
    }

    private fun observeAppLifecycle() {
        val center = platform.Foundation.NSNotificationCenter.defaultCenter
        center.addObserverForName(
            platform.UIKit.UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            logger.d { "App in FOREGROUND" }
            geoEngine.appInForeground = true
        }
        center.addObserverForName(
            platform.UIKit.UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            logger.d { "App NOT in FOREGROUND" }
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

        gpxRecorder.headTracking = {
            headTrackingProvider.takeDebugSnapshot(
                includeInput = preferencesProvider.getBoolean(
                    PreferenceKeys.RECORD_HEAD_TRACKER_INPUT,
                    PreferenceDefaults.RECORD_HEAD_TRACKER_INPUT,
                )
            )
        }
        gpxRecorder.steps = { stepProvider.snapshot() }
        geoEngine.locationRecorder = gpxRecorder
        geoEngine.stepProvider = stepProvider

        geoEngine.start(
            newLocationProvider = locationProvider,
            newDirectionProvider = directionProvider,
            listener = this,
            localizedStrings = ComposeLocalizedStrings(),
            preferencesProvider = preferencesProvider,
            analytics = analytics,
            tileClient = tileClient,
            routeDao = routeDao,
            offlineExtractPath = documentsPath,
            hasNetwork = { networkUtils.hasNetwork() },
            photonSearch = photonSearch,
            platformGeocoder = IosGeocoder(
                analyticsLogger = { name -> analytics.logEvent(name, null) }
            ),
            streetPreviewEnabled = streetPreviewEnabled,
        )
        geoEngineStarted = true
    }

    /**
     * Starts the geo engine unless it is already running. Only used to run a start that
     * [claimStartHold] deferred — every other path goes through [startGeoEngine] directly,
     * because they each want a fresh start with different providers.
     */
    private fun startGeoEngineIfNotStarted() {
        if (geoEngineStarted) return
        startGeoEngine()
    }

    // --- GeoEngineListener ---

    override fun isAudioEngineBusy(): Boolean {
        return audioEngine.getQueueDepth() > 0
    }

    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long =
        calloutController.speakCallout(callout, addModeEarcon)

    override fun updateAudioEngineGeometry(userGeometry: UserGeometry) {
        calloutController.updateGeometry(userGeometry)
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

    /**
     * The user has switched the app's language without relaunching it (see IosAppLanguage.kt).
     * Callout text needs nothing: ComposeLocalizedStrings looks each string up in the current
     * language as it's spoken. What is fixed at start is the Auto voice, which the TTS renderer
     * works out from the language, and which name translation the tiles are read with
     * (GeoEngine.getNameTranslationKeys), so restart the geo engine to reload them, the same way
     * leaving street preview does.
     *
     * Not while the start is still held back for onboarding or the splash clip - it reads the
     * new language when it does start - and not in street preview, which restarts the engine,
     * and so picks up the language, when the user leaves it.
     */
    fun onAppLanguageChanged() {
        audioEngine.clearTextToSpeechQueue()
        // An Auto voice is the default voice for the app's language, so it has to follow it.
        (NSLocale.preferredLanguages.firstOrNull() as? String)?.let {
            audioEngine.setSpeechLanguage(it)
        }
        if (!geoEngineStarted) return
        if (_streetPreviewFlow.value.enabled != StreetPreviewEnabled.OFF) return
        setStreetPreviewMode(false, null)
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
                markersOrRoutesChanged()
                audioEngine.createEarcon(
                    "file:///android_asset/Sounds/sense_poi.wav",
                    org.scottishtecharmy.soundscape.audio.AudioType.STANDARD
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to save route" }
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
                logger.e(e) { "Failed to load route" }
                emptyList()
            }
        }
    }

    fun deleteRoute(routeId: Long) {
        scope.launch {
            try {
                routeDao.removeMarkersForRoute(routeId)
                routeDao.removeRoute(routeId)
                markersOrRoutesChanged()
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete route" }
            }
        }
    }

    // --- Markers ---

    fun saveMarker(locationDescription: LocationDescription) {
        scope.launch {
            try {
                org.scottishtecharmy.soundscape.database.saveMarker(locationDescription, routeDao)
                markersOrRoutesChanged()
                audioEngine.createEarcon(
                    "file:///android_asset/Sounds/sense_poi.wav",
                    org.scottishtecharmy.soundscape.audio.AudioType.STANDARD
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to save marker" }
            }
        }
    }

    fun deleteMarker(markerId: Long) {
        scope.launch {
            try {
                routeDao.removeMarker(markerId)
                markersOrRoutesChanged()
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete marker" }
            }
        }
    }

    // --- GeoEngine Queries ---

    override val activeCalloutFlow: StateFlow<TourButton?>
        get() = calloutController.activeCalloutFlow

    override fun myLocation() = calloutController.myLocation()

    override fun whatsAroundMe() = calloutController.whatsAroundMe()

    override fun aheadOfMe() = calloutController.aheadOfMe()

    override fun nearbyMarkers() = calloutController.nearbyMarkers()

    override fun calloutBeacon() = calloutController.calloutBeacon()

    override fun beaconMoreInfo() =
        calloutController.beaconMoreInfo(routePlayer.currentWaypointName())

    override fun cancelCallout() = calloutController.cancel()

    override fun setCalloutVerbosity(verbosity: CalloutVerbosity) {
        preferencesProvider.putString(PreferenceKeys.CALLOUT_VERBOSITY, verbosity.preferenceValue)
    }

    override fun cycleCalloutDetail() = cycleCalloutVerbosity(preferencesProvider)

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

    /**
     * Speaks a sample in [voiceId] without choosing it, for the voice picker's preview - a user
     * comparing sixty voices has to be able to hear one without committing to it.
     *
     * This leaves the renderer on a voice that isn't the stored one, so every preview ends either
     * in the picker writing the choice to preferences or in [restoreSelectedTtsVoice].
     */
    fun previewTtsVoice(voiceId: String) {
        audioEngine.setSpeechVoice(voiceId)
        playTtsSample()
    }

    /** Puts the renderer back on the stored voice, after previews the user didn't confirm. */
    fun restoreSelectedTtsVoice() {
        audioEngine.setSpeechVoice(
            preferencesProvider.getString(
                PreferenceKeys.SELECTED_TTS_VOICE_ID,
                PreferenceDefaults.SELECTED_TTS_VOICE_ID,
            )
        )
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

    override suspend fun getOfflineAddress(location: LngLatAlt): LocationDescription? {
        return geoEngine.getOfflineAddress(location)
    }

    override suspend fun searchResult(query: String): List<LocationDescription>? {
        return geoEngine.searchResult(query)
    }

    override fun searchIsOfflineWithoutMaps(): Boolean = geoEngine.searchIsOfflineWithoutMaps()

    // --- Recording ---

    /** The travel recording as GPX text, for attaching to a support email. */
    fun recordingGpx(): String = kotlinx.coroutines.runBlocking { gpxRecorder.generateGpx() }

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
            // Sleep only silenced the beacon: RoutePlayer still has the route or beacon and the
            // waypoint it was on, so recreate it there, as Android resumes it when it restarts
            // its service on waking.
            if (routePlayer.isPlaying()) {
                routePlayer.play()
            }
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
        stepProvider.stop()
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

        // Guards construction. The unguarded `INSTANCE ?: IosSoundscapeService()` this
        // replaces was safe while only the UI called it, on the main thread. App Intents
        // changed that: Siri resolves an intent's entity parameter on its own thread and
        // then performs the intent on another, so two threads can call in at once. Losing
        // that race constructs a second service — a second AVAudioEngine, geo engine and
        // location provider, all live at the same time over one database.
        //
        // Lock ordering: this lock is taken before MarkersAndRoutesDatabaseProvider's,
        // via the routeDao that RoutePlayer and AudioMenu take during construction. The
        // database provider never reaches back for the service, so there is no cycle.
        //
        // Construction is not cheap — it starts the geo engine, location updates and the
        // audio engine — so a caller arriving mid-construction now waits rather than
        // building its own. That wait is the point.
        private val lock = NSLock()

        @Volatile
        private var INSTANCE: IosSoundscapeService? = null

        private var analyticsFactory: () -> Analytics = { NoOpAnalytics }

        fun setAnalyticsFactory(factory: () -> Analytics) {
            analyticsFactory = factory
        }

        // --- Geo engine start gate ---
        //
        // Two things can each hold the geo engine's start back, because its first callout
        // is TTS and both would otherwise be talked over or interrupted: the Double Tap
        // attribution clip that plays over the splash on the first launch of a new minor
        // version (splashHold - Swift sets this before the clip starts and releases it
        // when the clip finishes), and the onboarding flow on a first-ever launch
        // (onboardingHold - the service sets this itself from [PreferenceKeys.FIRST_LAUNCH]
        // and releases it when that preference flips to false). The geo engine starts once
        // neither is set.
        //
        // Guarded by its own lock rather than [lock]: the service's init runs while
        // getInstance() holds [lock], and NSLock is not reentrant.
        private val splashGateLock = NSLock()
        private var splashHold = false
        private var onboardingHold = false
        private var serviceAwaitingStart: IosSoundscapeService? = null

        private const val SPLASH_HOLD_TIMEOUT_MS = 8_000L

        // Onboarding is user-paced and can legitimately run for minutes, so this backstop is
        // only insurance against FIRST_LAUNCH never flipping to false - e.g. a bug in the
        // onboarding nav graph, or a deep link that reaches Home without going through
        // FinishScreen's onFinish. It must never fire during an ordinary run-through.
        private const val ONBOARDING_HOLD_TIMEOUT_MS = 10 * 60_000L

        /**
         * Defers the geo engine start of the next service construction until
         * [releaseGeoEngineStartAfterSplash]. Called from Swift's SplashCoordinator, and
         * only when the Double Tap clip is actually going to play.
         *
         * Has to be called before the first [getInstance] — a service already constructed
         * has already started its engine, and this does not stop it again.
         */
        fun holdGeoEngineStartForSplash() {
            splashGateLock.lock()
            try {
                splashHold = true
            } finally {
                splashGateLock.unlock()
            }
        }

        /**
         * Defers the geo engine start of this service construction until
         * [releaseGeoEngineStartAfterOnboarding]. Called from this service's own init, so -
         * unlike the splash hold - it needs no help from Swift: by the time init runs the
         * decision only depends on a Kotlin-side preference.
         */
        private fun holdGeoEngineStartForOnboarding() {
            splashGateLock.lock()
            try {
                onboardingHold = true
            } finally {
                splashGateLock.unlock()
            }
        }

        /**
         * Lets a held geo engine start proceed once the splash hold is satisfied. Called
         * from Swift when the Double Tap clip finishes or errors, from its safety-net
         * timer, and from the service's own timeout. Safe to call repeatedly and when no
         * hold was ever set. The engine only actually starts once [onboardingHold] has
         * also cleared.
         */
        fun releaseGeoEngineStartAfterSplash() {
            splashGateLock.lock()
            val waiting = try {
                splashHold = false
                if (splashHold || onboardingHold) null
                else serviceAwaitingStart.also { serviceAwaitingStart = null }
            } finally {
                splashGateLock.unlock()
            }
            startDeferred(waiting)
        }

        /**
         * Lets a held geo engine start proceed once onboarding is satisfied. Called from
         * this service's own [PreferencesListener] when [PreferenceKeys.FIRST_LAUNCH] flips
         * to false — the write [SharedOnboardingNavHost]'s onFinish makes right before
         * navigating to Home — and from the service's own backstop timeout. Safe to call
         * repeatedly and when no hold was ever set. The engine only actually starts once
         * [splashHold] has also cleared.
         */
        private fun releaseGeoEngineStartAfterOnboarding() {
            splashGateLock.lock()
            val waiting = try {
                onboardingHold = false
                if (splashHold || onboardingHold) null
                else serviceAwaitingStart.also { serviceAwaitingStart = null }
            } finally {
                splashGateLock.unlock()
            }
            startDeferred(waiting)
        }

        /**
         * Starts [service]'s geo engine on its own scope rather than on the caller's stack.
         * [releaseGeoEngineStartAfterOnboarding] can run synchronously inside
         * [IosPreferencesProvider.notifyChangedKeys]'s listener loop (SharedNavGraph's
         * onFinish writes FIRST_LAUNCH on the same call that fires it) — starting the geo
         * engine from there once registered a preferences listener of its own partway
         * through and mutated that same listener list mid-iteration, crashing with
         * ConcurrentModificationException. Dispatching here, as the splash backstop timer
         * already did, keeps every start off whatever stack triggered the release.
         */
        private fun startDeferred(service: IosSoundscapeService?) {
            service?.scope?.launch { service.startGeoEngineIfNotStarted() }
        }

        /**
         * Registers [service] as the one waiting on the splash and/or onboarding, if
         * either hold is in force. Taking both decisions under the one lock is what closes
         * the race between a service still inside its constructor (so [INSTANCE] is still
         * null, and a release would have nothing to start) and a release arriving on
         * another thread.
         */
        private fun claimStartHold(service: IosSoundscapeService): Boolean {
            splashGateLock.lock()
            try {
                if (!splashHold && !onboardingHold) return false
                serviceAwaitingStart = service
                return true
            } finally {
                splashGateLock.unlock()
            }
        }

        fun getInstance(): IosSoundscapeService {
            INSTANCE?.let { return it }
            lock.lock()
            try {
                INSTANCE?.let { return it }
                return IosSoundscapeService().also { INSTANCE = it }
            } finally {
                lock.unlock()
            }
        }
    }
}

private object NoOpAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>?) {}
    override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {}
    override fun crashSetCustomKey(key: String, value: String) {}
    override fun crashLogNotes(name: String) {}
}
