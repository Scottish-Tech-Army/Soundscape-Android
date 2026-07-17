package org.scottishtecharmy.soundscape.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.BeaconPreviewController
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_ENTER
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_EXIT
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.bluetooth.AudioHeadsetBatteryMonitor
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
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
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.AndroidGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.hasPlayServices
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.locationprovider.AndroidDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.GooglePlayDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.GooglePlayLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.GpxDrivenProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.CompositeHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.bleimu.BleImuHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.bose.BoseFramesHeadTrackingProvider
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import org.scottishtecharmy.soundscape.network.createAndroidVectorTileClient
import org.scottishtecharmy.soundscape.preferences.AndroidPreferencesProvider
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.app_name
import org.scottishtecharmy.soundscape.resources.notification_text
import org.scottishtecharmy.soundscape.resources.preview_go_title
import org.scottishtecharmy.soundscape.resources.service_still_running
import org.scottishtecharmy.soundscape.resources.voice_cmd_markers_list
import org.scottishtecharmy.soundscape.resources.voice_cmd_no_markers
import org.scottishtecharmy.soundscape.resources.voice_cmd_no_routes
import org.scottishtecharmy.soundscape.resources.voice_cmd_routes_list
import org.scottishtecharmy.soundscape.resources.voice_cmd_starting_beacon_at_marker
import org.scottishtecharmy.soundscape.resources.voice_cmd_starting_route
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.SoundscapeService.Companion.TICKER_PERIOD_SECONDS
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenu
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenuMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControlTarget
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import org.scottishtecharmy.soundscape.services.mediacontrol.OriginalMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.SoundscapeDummyMediaPlayer
import org.scottishtecharmy.soundscape.services.mediacontrol.SoundscapeMediaSessionCallback
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandManager
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState
import org.scottishtecharmy.soundscape.utils.AnalyticsProvider
import org.scottishtecharmy.soundscape.utils.NetworkUtils
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that provides location updates, device orientation updates, requests tiles,
 * data persistence with realmDB. It inherits from MediaSessionService so that we can receive
 * Media Transport button presses to act as a remote control whilst the phone is locked.
 */
class SoundscapeService : MediaSessionService(), GeoEngineListener, MediaControllableService {

    private val coroutineScope = CoroutineScope(Job())

    lateinit var locationProvider: LocationProvider
    lateinit var directionProvider: DirectionProvider
    lateinit var routePlayer: RoutePlayer

    // External head-tracker — currently a composite of WitMotion (WT9011DCL)
    // and Bose Frames AR. Null until providers are built.
    private var headTrackingProvider: HeadTrackingProvider? = null

    // Stable flow that mirrors the current provider's headHeadingFlow. Kept
    // separate so subscribers (HomeViewModel) survive provider rebuilds.
    private val _headHeadingFlow = MutableStateFlow<HeadHeading?>(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = _headHeadingFlow
    private var headHeadingForwarderJob: Job? = null

    // System broadcast-based battery monitor for paired Bluetooth headphones.
    private val headsetBatteryMonitor by lazy { AudioHeadsetBatteryMonitor(this) }
    override val headsetBatteryPercentFlow: StateFlow<Int?>
        get() = headsetBatteryMonitor.batteryPercentFlow

    // Listener instance is field-level so it can be unregistered cleanly in onDestroy.
    private val headTrackingPrefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferenceKeys.HEAD_TRACKING_ENABLED) applyHeadTrackingEnabled()
        }

    override val filteredLocationFlow: StateFlow<SoundscapeLocation?>
        get() = locationProvider.filteredLocationFlow

    override val locationFlow: StateFlow<SoundscapeLocation?>
        get() = locationProvider.locationFlow

    override val orientationFlow: StateFlow<org.scottishtecharmy.soundscape.locationprovider.DeviceDirection?>
        get() = directionProvider.orientationFlow

    override val currentRouteFlow: StateFlow<org.scottishtecharmy.soundscape.services.RoutePlayerState>
        get() = routePlayer.currentRouteFlow

    // secondary service
    private var timerJob: Job? = null

    // Guard to prevent duplicate user-triggered callouts
    private var calloutJob: Job? = null

    // Wake lock — keeps CPU running while screen is off so audio callbacks continue
    private var wakeLock: PowerManager.WakeLock? = null

    // Audio engine
    var audioEngine = NativeAudioEngine(this)
    private var audioBeacon: Long = 0

    // Audio menu (navigated via media buttons when no route is active)
    var audioMenu: AudioMenu? = null

    /** True while the user is actively navigating the audio menu. Suppresses auto callouts. */
    override var menuActive: Boolean = false

    override fun getStreetPreviewChoices(): List<StreetPreviewChoice> =
        streetPreviewFlow.value.choices

    override fun getStreetPreviewBestChoice(): StreetPreviewChoice? =
        streetPreviewFlow.value.bestChoice

    // Audio focus
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    var audioFocusGained: Boolean = false
    var duckingAllowed: Boolean = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN: Focus gained.")
                audioFocusGained = true
                duckingAllowed = false
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK: Focus gained.")
                audioFocusGained = true
                duckingAllowed = false
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS: Focus lost permanently")
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporarily lost focus. Pause playback.
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT: Focus lost temporarily")
                audioFocusGained = false
                duckingAllowed = false
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Temporarily lost focus, but you can duck (lower volume).
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: Ducking allowed")

                audioFocusGained = false
                duckingAllowed = true
            }
        }
    }

    // Geo engine
    private var geoEngine = GeoEngine()
    lateinit var localizedContext: Context
    private var gpxRecorder: GpxRecorder? = null
    private lateinit var networkUtils: NetworkUtils

    private fun startGeoEngine(streetPreviewEnabled: Boolean) {
        val preferencesProvider = AndroidPreferencesProvider(sharedPreferences)

        val extractPath = sharedPreferences.getString(
            MainActivity.SELECTED_STORAGE_KEY,
            MainActivity.SELECTED_STORAGE_DEFAULT
        )!!
        val offlineExtractPath = extractPath + "/" + android.os.Environment.DIRECTORY_DOWNLOADS

        networkUtils = NetworkUtils(application)

        val tileClient = createAndroidVectorTileClient(
            baseUrl = BuildConfig.TILE_PROVIDER_URL,
            cacheDir = application.cacheDir,
            userAgent = UserAgentInterceptor.USER_AGENT,
            hasNetwork = { networkUtils.hasNetwork() },
        )

        val platformGeocoder = if (AndroidGeocoder.enabled) AndroidGeocoder(application) else null
        val routeDao = MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()

        gpxRecorder = GpxRecorder()
        geoEngine.locationRecorder = gpxRecorder

        geoEngine.start(
            newLocationProvider = locationProvider,
            newDirectionProvider = directionProvider,
            listener = this,
            localizedStrings = ComposeLocalizedStrings(),
            preferencesProvider = preferencesProvider,
            analytics = AnalyticsProvider.getInstance(),
            tileClient = tileClient,
            routeDao = routeDao,
            offlineExtractPath = offlineExtractPath,
            hasNetwork = { networkUtils.hasNetwork() },
            photonSearch = PhotonSearchProvider,
            platformGeocoder = platformGeocoder,
            streetPreviewEnabled = streetPreviewEnabled,
        )

        rebuildHeadTrackingProvider()
    }

    /**
     * Replace [headTrackingProvider] with a fresh instance bound to the current
     * location/direction providers. Called whenever those providers are rebuilt
     * (initial start or street-preview toggle). Honours the HEAD_TRACKING_ENABLED
     * preference — only starts streaming if the user has it on.
     */
    private fun rebuildHeadTrackingProvider() {
        headTrackingProvider?.destroy()
        headHeadingForwarderJob?.cancel()
        val provider = CompositeHeadTrackingProvider(
            listOf(
                BleImuHeadTrackingProvider(directionProvider, locationProvider),
                BoseFramesHeadTrackingProvider(directionProvider, locationProvider),
            ),
        )
        headTrackingProvider = provider
        geoEngine.setHeadTrackingProvider(provider)
        headHeadingForwarderJob = coroutineScope.launch {
            provider.headHeadingFlow.collect { _headHeadingFlow.value = it }
        }
        applyHeadTrackingEnabled()
    }

    fun applyHeadTrackingEnabled() {
        val enabled = sharedPreferences.getBoolean(
            PreferenceKeys.HEAD_TRACKING_ENABLED,
            PreferenceDefaults.HEAD_TRACKING_ENABLED,
        )
        val provider = headTrackingProvider ?: return
        if (enabled) {
            // If permissions are missing the start is deferred — MainActivity
            // requests them and re-applies the preference once granted.
            if (hasBluetoothPermissions()) provider.start()
        } else {
            provider.stop()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val scan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        val connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return scan == PackageManager.PERMISSION_GRANTED &&
            connect == PackageManager.PERMISSION_GRANTED
    }

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow(BeaconState())
    override val beaconFlow: StateFlow<BeaconState> = _beaconFlow

    // Flow to return street preview mode
    private val _streetPreviewFlow = MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF))
    override val streetPreviewFlow: StateFlow<StreetPreviewState> = _streetPreviewFlow

    // Flow to return nearby places
    private val _gridStateFlow = MutableStateFlow<GridState?>(null)
    override val gridStateFlow: StateFlow<GridState?> = _gridStateFlow

    // Voice command manager — only initialized when RECORD_AUDIO permission is granted
    private var voiceCommandManager: VoiceCommandManager? = null
    override val voiceCommandStateFlow: StateFlow<VoiceCommandState>
        get() = voiceCommandManager?.state ?: MutableStateFlow(VoiceCommandState.Idle)

    // Media control button code
    private var mediaSession: MediaSession? = null

    private var mediaControlsTarget: MediaControlTarget = OriginalMediaControls(this)
    private val mediaPlayer = SoundscapeDummyMediaPlayer { mediaControlsTarget }

    var running: Boolean = false
    var started: Boolean = false

    private var binder: SoundscapeBinder? = null

    @SuppressLint("MissingSuperCall")
    override fun onBind(intent: Intent?): IBinder {
        if (binder == null) {
            // Create binder if we don't have one already
            binder = SoundscapeBinder(this@SoundscapeService)
        }
        return binder!!
    }

    override fun setStreetPreviewMode(on: Boolean, location: LngLatAlt?) {
        directionProvider.destroy()
        locationProvider.destroy()
        geoEngine.stop()
        if (on) {
            // Use static location, but phone's direction
            if (location != null) {
                locationProvider = StaticLocationProvider(location)
                directionProvider = if (hasPlayServices(this))
                    GooglePlayDirectionProvider(this)
                else
                    AndroidDirectionProvider(this)
            }
        } else {
            // Switch back to phone's location and direction
            if (hasPlayServices(this)) {
                locationProvider = GooglePlayLocationProvider(this)
                directionProvider = GooglePlayDirectionProvider(this)
            } else {
                locationProvider = AndroidLocationProvider(this)
                directionProvider = AndroidDirectionProvider(this)
            }
        }

        // Set the StreetPreview state prior to starting the location provider. Otherwise there's a
        // race in the tileGridUpdated callback.
        _streetPreviewFlow.value =
            StreetPreviewState(if (on) StreetPreviewEnabled.INITIALIZING else StreetPreviewEnabled.OFF)

        startProviders()
        startGeoEngine(on)
    }

    private fun startProviders() {
        when (val lp = locationProvider) {
            is GooglePlayLocationProvider -> lp.start()
            is AndroidLocationProvider -> lp.start()
            is StaticLocationProvider -> lp.start()
        }
        when (val dp = directionProvider) {
            is GooglePlayDirectionProvider -> dp.start()
            is AndroidDirectionProvider -> dp.start()
        }
    }

    override fun tileGridUpdated() {
        if (_streetPreviewFlow.value.enabled == StreetPreviewEnabled.INITIALIZING) {
            val choices = geoEngine.streetPreviewGo()
            _streetPreviewFlow.value = StreetPreviewState(
                StreetPreviewEnabled.ON,
                choices
            )
            geoEngine.recomputeStreetPreviewBestChoice()
        }
        _gridStateFlow.value = geoEngine.gridState
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            // Normally already done at the top of onCreate(); this is a fallback retry in case
            // that attempt failed (e.g. the notification couldn't be built at the time).
            if (startAsForegroundService()) {
                // Reminds the user every hour that the Soundscape service is still running in the background
                startServiceStillRunningTicker()
                running = true
            }
        }

        if (!started) {
            AnalyticsProvider.getInstance().crashLogNotes("Start geo-engine")
            startProviders()
            val configLocale = getCurrentLocale()
            val configuration = Configuration(applicationContext.resources.configuration)
            configuration.setLocale(configLocale)
            localizedContext = applicationContext.createConfigurationContext(configuration)
            voiceCommandManager?.updateContext(localizedContext)
            startGeoEngine(false)
            started = true
        }

        return super.onStartCommand(intent, flags, startId)
    }

    lateinit var sharedPreferences: SharedPreferences

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate $running")

        if (!running) {

            // Promote to a foreground service as the very first thing we do, before any of the
            // slower initialization below (native audio engine, Realm DB, MediaSession). The
            // system starts its "must call startForeground() in time" timer from the client's
            // Context.startForegroundService() call, and onCreate() always runs before
            // onStartCommand() - so if startForeground() is deferred until onStartCommand(), any
            // slowness here can eat the whole grace period and trigger a
            // ForegroundServiceDidNotStartInTimeException.
            if (startAsForegroundService()) {
                // Reminds the user every hour that the Soundscape service is still running in the background
                startServiceStillRunningTicker()
                running = true
            }

            // Hold a partial wake lock for the service lifetime so the CPU stays awake when the
            // screen is off and the Oboe audio callback keeps firing.
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Soundscape::AudioWakeLock")
                .also { it.acquire() }

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioMenu = AudioMenu(
                this,
                MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()
            )
            routePlayer = RoutePlayer(
                this,
                MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()
            )

            if (true) {
                // Normal app behaviour using the phone location and direction providers
                if (hasPlayServices(this)) {
                    locationProvider = GooglePlayLocationProvider(this)
                    directionProvider = GooglePlayDirectionProvider(this)
                } else {
                    locationProvider = AndroidLocationProvider(this)
                    directionProvider = AndroidDirectionProvider(this)
                }
            } else {
                // This is used to replay a recorded GPX file to see how the complete app behaves.
                // Enabled by developers only and currently hard coded to a specific asset.
                val gpxProvider = GpxDrivenProvider()
                gpxProvider.start(this)
                locationProvider = gpxProvider.locationProvider
                directionProvider = gpxProvider.directionProvider
            }
            // create new RealmDB or open existing
            startRealms(applicationContext)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                voiceCommandManager = VoiceCommandManager(
                    service = this
                )
            }

            headsetBatteryMonitor.start()

            // Update the media controls mode
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            sharedPreferences.registerOnSharedPreferenceChangeListener(headTrackingPrefListener)
            val mode = sharedPreferences.getString(
                PreferenceKeys.MEDIA_CONTROLS_MODE,
                PreferenceDefaults.MEDIA_CONTROLS_MODE
            )!!
            updateMediaControls(mode)

            // Resume whatever route/beacon was playing when sleep mode last stopped this service.
            restoreSleepPlaybackState()

            // Keep biasing strings up to date whenever markers or routes change
            val dao = MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()
            coroutineScope.launch {
                dao.getAllMarkersFlow().collect { markers ->
                    voiceCommandManager?.updateMarkers(markers)
                }
            }
            coroutineScope.launch {
                dao.getAllRoutesFlow().collect { routes ->
                    voiceCommandManager?.updateRoutes(routes)
                }
            }

            mediaSession = MediaSession.Builder(this, mediaPlayer)
                .setId("org.scottishtecharmy.soundscape")
                .setCallback(SoundscapeMediaSessionCallback { mediaControlsTarget })
                .build()
        }
    }

    fun updateMediaControls(target: String) {
        val hasRecordAudio =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        mediaControlsTarget = when (target) {
            "VoiceControl" if hasRecordAudio -> {
                voiceCommandManager?.initialize()
                VoiceCommandMediaControls(this)
            }

            "VoiceControl" -> AudioMenuMediaControls(audioMenu)
            "AudioMenu" -> AudioMenuMediaControls(audioMenu)
            "Original" -> OriginalMediaControls(this)
            else -> OriginalMediaControls(this)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved for service - ignoring, as we want to keep running")
    }

    override fun onDestroy() {
        suppressionJob?.cancel()

        // If _mediaSession is not null, run the following block
        mediaSession?.run {
            // Release the player
            player.release()
            // Release the MediaSession instance
            release()
            // Set _mediaSession to null
            mediaSession = null
        }
        super.onDestroy()

        Log.d(TAG, "onDestroy")
        audioMenu?.destroy()
        audioEngine.destroyBeacon(audioBeacon)
        audioBeacon = 0
        audioEngine.destroy()

        headsetBatteryMonitor.stop()
        headHeadingForwarderJob?.cancel()
        headHeadingForwarderJob = null
        headTrackingProvider?.destroy()
        headTrackingProvider = null
        _headHeadingFlow.value = null
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(headTrackingPrefListener)
        }
        locationProvider.destroy()
        directionProvider.destroy()
        started = false

        abandonAudioFocus()

        timerJob?.cancel()
        geoEngine.stop()

        coroutineScope.coroutineContext.cancelChildren()

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        voiceCommandManager?.destroy()

        // Clear service reference in binder so that it can be garbage collected
        binder?.reset()
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     *
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService(): Boolean {

        val analytics = AnalyticsProvider.getInstance()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Code to simulate startForeground failing
                if (startForegroundShouldFail) {
                    startForegroundShouldFail = false
                    throw ForegroundServiceStartNotAllowedException("Simulated startForeground failure")
                }
            }

            // Only claim the microphone type if we actually hold RECORD_AUDIO - the system
            // requires that permission to be granted at promotion time for this type, otherwise
            // it throws a SecurityException.
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                getNotification(),
                serviceType
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                analytics.crashLogNotes("ForegroundServiceStartNotAllowedException caught")
                analytics.logEvent("startAsForegroundServiceError", null)
                AnalyticsProvider.getInstance().crashSetCustomKey("Service start success", "false")
                return false
            }
            // Any other failure (e.g. building or posting the notification) means the service did
            // not actually reach the foreground. Previously this was swallowed and the method went
            // on to report success, leaving `running = true` for a service that never started.
            Log.e(TAG, "startAsForegroundService failed", e)
            analytics.crashLogNotes("startAsForegroundService failed: $e")
            analytics.logEvent("startAsForegroundServiceError", null)
            analytics.crashSetCustomKey("Service start success", "false")
            return false
        }
        analytics.crashSetCustomKey("Service start success", "true")
        return true
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     * @param forSleep If true, the current route/beacon playback state is persisted so it can be
     * automatically resumed when the service is next started (used by sleep mode).
     */
    fun stopForegroundService(forSleep: Boolean = false) {
        if (forSleep) {
            saveSleepPlaybackState()
        }
        destroyBeacon()
        abandonAudioFocus()
        stopSelf()
    }

    /**
     * Persists whatever route/beacon RoutePlayer is currently playing so that
     * [restoreSleepPlaybackState] can restart it after the service is recreated on waking from
     * sleep mode.
     */
    private fun saveSleepPlaybackState() {
        val state = routePlayer.currentRouteFlow.value
        val routeData = state.routeData
        sharedPreferences.edit {
            if (routeData == null) {
                putBoolean(SLEEP_RESUME_ACTIVE_KEY, false)
            } else if (routeData.route.routeId != 0L) {
                // A route from the markers/routes database.
                putBoolean(SLEEP_RESUME_ACTIVE_KEY, true)
                putLong(SLEEP_RESUME_ROUTE_ID_KEY, routeData.route.routeId)
                putBoolean(SLEEP_RESUME_REVERSE_KEY, state.reverse)
                putInt(SLEEP_RESUME_WAYPOINT_KEY, state.currentWaypoint)
            } else {
                // An ad-hoc beacon (RoutePlayer.startBeacon), identified by routeId 0.
                val marker = routeData.markers.firstOrNull()
                if (marker == null) {
                    putBoolean(SLEEP_RESUME_ACTIVE_KEY, false)
                } else {
                    putBoolean(SLEEP_RESUME_ACTIVE_KEY, true)
                    putLong(SLEEP_RESUME_ROUTE_ID_KEY, 0L)
                    putString(SLEEP_RESUME_BEACON_NAME_KEY, marker.name)
                    putString(SLEEP_RESUME_BEACON_LAT_KEY, marker.latitude.toString())
                    putString(SLEEP_RESUME_BEACON_LON_KEY, marker.longitude.toString())
                }
            }
        }
    }

    /**
     * Restarts route/beacon playback saved by [saveSleepPlaybackState], if any. Called once when
     * a fresh service instance is created after sleep mode stopped the previous one. The saved
     * state is consumed (cleared) so it's only ever applied once.
     */
    private fun restoreSleepPlaybackState() {
        if (!sharedPreferences.getBoolean(SLEEP_RESUME_ACTIVE_KEY, false)) return
        sharedPreferences.edit { putBoolean(SLEEP_RESUME_ACTIVE_KEY, false) }

        val routeId = sharedPreferences.getLong(SLEEP_RESUME_ROUTE_ID_KEY, 0L)
        if (routeId != 0L) {
            val reverse = sharedPreferences.getBoolean(SLEEP_RESUME_REVERSE_KEY, false)
            val waypoint = sharedPreferences.getInt(SLEEP_RESUME_WAYPOINT_KEY, 0)
            routePlayer.startRoute(routeId, reverse, waypoint)
        } else {
            val name = sharedPreferences.getString(SLEEP_RESUME_BEACON_NAME_KEY, null)
            val lat = sharedPreferences.getString(SLEEP_RESUME_BEACON_LAT_KEY, null)?.toDoubleOrNull()
            val lon = sharedPreferences.getString(SLEEP_RESUME_BEACON_LON_KEY, null)?.toDoubleOrNull()
            if (name != null && lat != null && lon != null) {
                routePlayer.startBeacon(LngLatAlt(lon, lat), name)
            }
        }
    }

    /**
     * Starts a ticker that shows a toast every [TICKER_PERIOD_SECONDS] seconds to indicate that the service is still running.
     */
    private fun startServiceStillRunningTicker() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            tickerFlow()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SoundscapeService,
                            getString(Res.string.service_still_running),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun tickerFlow(
        period: Duration = TICKER_PERIOD_SECONDS,
        initialDelay: Duration = TICKER_PERIOD_SECONDS
    ) = flow {
        while (true) {
            delay(initialDelay)
            emit(Unit)
            delay(period)
        }
    }

    private fun getNotification(): Notification {
        createServiceNotificationChannel()

        val notifyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(kotlinx.coroutines.runBlocking { getString(Res.string.app_name) })
            .setContentText(kotlinx.coroutines.runBlocking { getString(Res.string.notification_text) })
            // Use a rasterized (PNG) small icon rather than the vector drawable: some OEM System
            // UIs (e.g. Kapsys SmartVision3) fail to inflate vector notification icons, which makes
            // the system reject the foreground-service notification and kill us with a
            // RemoteServiceException.
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(notifyPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Without this, the notification doesn't appear for 10 seconds, we want it to appear
            // immediately.
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun createServiceNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun startRealms(context: Context) {
        MarkersAndRoutesDatabaseProvider.getInstance(context)
    }

    /*    fun deleteRealm(){
            // need this to clean up my mess while I work on the db schema, etc.
            val config = io.realm.kotlin.RealmConfiguration.create(setOf(TileData::class))
            // Delete the realm
            Realm.deleteRealm(config)
        }*/

    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {
        if (location == null) return

        requestAudioFocus()
        val oldBeacon = audioBeacon
        audioBeacon = audioEngine.createBeacon(location, headingOnly)
        if (oldBeacon != 0L) {
            audioEngine.destroyBeacon(oldBeacon)
        }
        // Report any change in beacon back to application
        _beaconFlow.value = _beaconFlow.value.copy(location = location)
        geoEngine.updateBeaconLocation(location)
    }

    override fun destroyBeacon() {
        if (audioBeacon != 0L) {
            audioEngine.destroyBeacon(audioBeacon)
            audioBeacon = 0L
        }
        // Report any change in beacon back to application
        _beaconFlow.value = _beaconFlow.value.copy(location = null)
        geoEngine.updateBeaconLocation(null)
    }

    // Beacon style preview — implementation lives in shared
    // BeaconPreviewController so iOS and Android stay in lockstep.
    private val beaconPreviewController by lazy {
        BeaconPreviewController(audioEngine, this, AndroidPreferencesProvider(sharedPreferences))
    }

    fun startBeaconPreview(beaconType: String) =
        beaconPreviewController.start(beaconType)

    fun updateBeaconPreviewType(beaconType: String) =
        beaconPreviewController.update(beaconType)

    fun stopBeaconPreview(commit: Boolean, chosenBeaconType: String?) =
        beaconPreviewController.stop(commit, chosenBeaconType)

    private suspend fun awaitHandle(handle: Long) {
        while (handle != 0L && audioEngine.isHandleActive(handle)) {
            delay(100)
        }
    }

    /**
     * Start a user-initiated callout.
     *
     * The previous callout (if any) is cancelled and the TTS queue cleared on the background
     * coroutine dispatcher rather than on the calling thread. clearTextToSpeechQueue() makes a
     * blocking binder call into the system TTS service (TextToSpeech.stop()), so running it on the
     * main thread - as the old synchronous cancelCallout() did - could ANR if that service was slow.
     *
     * If a callout was already in progress, the user action simply cancels it (toggle behaviour)
     * and [body] is skipped. Otherwise the TTS queue is cleared and then [body] runs, preserving the
     * clear-before-speak ordering that callouts rely on.
     */
    private fun startCallout(body: suspend CoroutineScope.() -> Unit) {
        val previousJob = calloutJob
        calloutJob = coroutineScope.launch {
            val wasActive = previousJob?.isActive == true
            if (wasActive)
                previousJob?.cancel()

            // Always clear the TTS queue as there's been a user action that requires a response
            audioEngine.clearTextToSpeechQueue()

            // If a callout was already in progress, the user action just cancels it.
            if (wasActive) return@launch

            body()
        }
    }

    override fun myLocation() {
        startCallout {
            if (requestAudioFocus()) {
                // The call to myLocation can take a second or so as it might be doing network
                // based reverse geocoding. Ensure that the user has feedback that the action is
                // taking place by immediately playing the earcon.
                audioEngine.createEarcon(EARCON_MODE_ENTER, AudioType.STANDARD)
                val results = geoEngine.myLocation()
                ensureActive()
                var lastHandle = 0L
                if (results != null) {
                    lastHandle = speakCallout(results, false)
                }
                audioEngine.createEarcon(EARCON_MODE_EXIT, AudioType.STANDARD)
                awaitHandle(lastHandle)
            } else {
                Log.w(TAG, "myLocation: Could not get audio focus.")
            }
        }
    }

    override fun whatsAroundMe() {
        startCallout {
            val results = geoEngine.whatsAroundMe()
            ensureActive()
            var lastHandle = 0L
            if (results.positionedStrings.isNotEmpty()) {
                lastHandle = speakCallout(results, true)
            }
            awaitHandle(lastHandle)
        }
    }

    override fun aheadOfMe() {
        startCallout {
            val results = geoEngine.aheadOfMe()
            ensureActive()
            var lastHandle = 0L
            if (results != null) {
                lastHandle = speakCallout(results, true)
            }
            awaitHandle(lastHandle)
        }
    }

    override fun nearbyMarkers() {
        startCallout {
            val results = geoEngine.nearbyMarkers()
            ensureActive()
            val lastHandle = speakCallout(results, true)
            awaitHandle(lastHandle)
        }
    }

    fun triggerVoiceCommand() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        if (!requestAudioFocus()) {
            Log.w(TAG, "speakText: Could not get audio focus. Aborting callouts.")
            return
        }

        // Stop callbacks whilst we handle voice commands
        callbackHoldOff()

        // Clear the speech queue
        audioEngine.clearTextToSpeechQueue()

        // And start listening for voice commands
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                voiceCommandManager?.startListening()
            }
        }
    }

    override suspend fun searchResult(query: String): List<LocationDescription>? {
        return geoEngine.searchResult(query)
    }

    override fun getLocationDescription(location: LngLatAlt): LocationDescription {
        return geoEngine.getLocationDescription(location)
    }

    override fun startBeacon(location: LngLatAlt, name: String) {
        // RoutePlayer.startBeacon() makes blocking audio engine calls (WAV decode, native
        // lock), so it's dispatched off the caller's thread to avoid blocking the main thread
        // when called directly from a UI click handler.
        coroutineScope.launch {
            routePlayer.startBeacon(location, name)
        }
    }

    override fun routeStartById(routeId: Long) {
        routePlayer.startRoute(routeId)
    }

    override fun routeStartReverse(routeId: Long) {
        routePlayer.startRoute(routeId, reverse = true)
    }

    override fun routeStop() {
        routePlayer.stopRoute()
    }

    override fun routeSkipPrevious(): Boolean {
        return routePlayer.moveToPrevious(true)
    }

    override fun routeSkipNext(): Boolean {
        return routePlayer.moveToNext(true)
    }

    override fun routeMute(): Boolean {
        // Both calls below can block on the audio engine's native lock, so run them off the
        // caller's thread. No caller uses the return value for anything beyond an immediate,
        // best-effort indication of whether a mute toggle was triggered.
        val wasPlaying = routePlayer.isPlaying()
        coroutineScope.launch {
            if (wasPlaying) {
                // Silence any current text-to-speech output
                audioEngine.clearTextToSpeechQueue()

                // Toggle the beacon mute
                val muteState = audioEngine.toggleBeaconMute()
                // Update the beacon flow with the new mute state
                _beaconFlow.value = _beaconFlow.value.copy(muteState = muteState)
            }
        }
        return wasPlaying
    }

    fun routeListRoutes() {
        coroutineScope.launch {
            val routes = MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()
                .getAllRoutes()
            if (routes.isEmpty())
                speak2dText(getString(Res.string.voice_cmd_no_routes))
            else {
                val names = routes.joinToString(". ") { it.name }
                speak2dText(getString(Res.string.voice_cmd_routes_list) + names)
            }
        }
    }

    fun routeStart(route: RouteEntity) {
        speak2dText(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_starting_route) }
            .format(route.name))
        routeStartById(route.routeId)
    }

    fun routeListMarkers() {
        coroutineScope.launch {
            val markers =
                MarkersAndRoutesDatabaseProvider.getInstance(applicationContext).routeDao()
                    .getAllMarkers()
            if (markers.isEmpty()) {
                speak2dText(getString(Res.string.voice_cmd_no_markers))
            } else {
                val names = markers.joinToString(". ") { it.name }
                speak2dText(getString(Res.string.voice_cmd_markers_list) + names)
            }
        }
    }

    fun markerStart(marker: MarkerEntity) {
        speak2dText(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_starting_beacon_at_marker) }
            .format(marker.name))

        val location = LngLatAlt(marker.longitude, marker.latitude)
        startBeacon(location, marker.name)
    }

    /**
     * isAudioEngineBusy returns true if there is more than one entry in the
     * audio engine queue. The queue consists of earcons and text-to-speech.
     */
    override fun isAudioEngineBusy(): Boolean {
        val depth = audioEngine.getQueueDepth()
        //Log.d(TAG, "Queue depth: $depth")
        return (depth > 0)
    }

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ) {

        if (!requestAudioFocus()) {
            Log.w(TAG, "speakText: Could not get audio focus.")
            return
        }
        Log.d(TAG, "speakText $text")
        audioEngine.createTextToSpeech(text, type, latitude, longitude, heading)
    }

    override fun clearTextToSpeechQueue() {
        audioEngine.clearTextToSpeechQueue()
    }

    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {
        if (!requestAudioFocus()) {
            Log.w(TAG, "speak2dText: Could not get audio focus.")
            return
        }
        if (clearQueue)
            audioEngine.clearTextToSpeechQueue()
        if (earcon != null) {
            audioEngine.createEarcon(earcon, AudioType.STANDARD)
        }
        if (text.isNotEmpty())
            audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    private var lastGeometry: UserGeometry? = null
    private var ruler = CheapRuler(0.0)
    override fun updateAudioEngineGeometry(
        userGeometry: UserGeometry
    ) {
        // Send the update to the audio engine. This affects the direction and sound
        // of the audio beacon.
        lastGeometry = userGeometry
        audioEngine.updateGeometry(
            userGeometry.location.latitude,
            userGeometry.location.longitude,
            userGeometry.presentationHeading(),
            audioFocusGained,
            duckingAllowed,
            15.0
        )
    }

    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        if (callout == null) return 0L

        if (!requestAudioFocus()) {
            Log.w(TAG, "SpeakCallout: Could not get audio focus.")
            return 0L
        }

        return speakCalloutCommon(callout, addModeEarcon, audioEngine, lastGeometry, ruler)
    }

    fun toggleAutoCallouts() {
        geoEngine.toggleAutoCallouts()
    }

    /**
     * streetPreviewGo is called when the 'GO' button is pressed when in StreetPreview mode.
     * It indicates that the user has selected the direction of travel in which they which to move.
     */

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

    fun appInForeground(foreground: Boolean) {
//  When running in the emulator it's useful to pretend that the phone is locked as that then uses
//  the GPS heading rather than the non-existent phone heading. Uncomment the check below to enable
//  that behaviour.
//        if(Build.DEVICE.contains("generic")) {
        // Set flag in GeoEngine so that it can adjust it's behaviour
        geoEngine.appInForeground = foreground
//        }
        if (foreground) {
            // If the app has switched to the foreground and we've got an active audio beacon, then
            // we should request audio focus
            if (audioBeacon != 0L)
                requestAudioFocus()
        }
    }

    fun getRecordingShareUri(context: Context): Uri? {
        val recorder = gpxRecorder ?: return null
        val recordingsStorageDir = File("${context.filesDir}/recordings/")
        if (!recordingsStorageDir.exists()) {
            recordingsStorageDir.mkdirs()
        }
        val outputFile = File(recordingsStorageDir, "travel.gpx")
        val gpx = runBlocking { recorder.generateGpx() }
        FileOutputStream(outputFile, false).use { it.write(gpx.toByteArray()) }
        return getUriForFile(context, "${context.packageName}.provider", outputFile)
    }

    override fun requestAudioFocus(): Boolean {
        if (!audioFocusGained) {
            if (audioFocusRequest == null) {
                // Build our audio focus request
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                // We prefer playback over ducked audio, so if music were playing then that will be
                // reduced in volume for our callouts to be heard.
                audioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(true) // If you can wait for focus
                        .setOnAudioFocusChangeListener(focusChangeListener)
                        .build()
            }
            if (audioFocusRequest != null) {
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)

                return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioFocusGained = true
                    Log.d(TAG, "Audio focus request granted.")
                    true
                } else {
                    // Assume loss if not granted
                    Log.e(TAG, "Audio focus request failed.")
                    audioFocusGained = false
                    false
                }
            }
        }

        // We failed to create an audio focus request - return as if it was all successful
        return true
    }

    fun abandonAudioFocus() {
        Log.d(TAG, "Abandoning audio focus.")
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        audioFocusGained = false
    }

    /**
     * Called on every menu interaction. Marks the menu as active (suppressing auto callouts)
     * and resets the 10-second countdown after which auto callouts are re-enabled.
     */
    /** Cancels pending re-enable of auto callouts and restarts the 10-second countdown. */
    private var suppressionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    override fun callbackHoldOff() {
        menuActive = true
        suppressionJob?.cancel()
        suppressionJob = scope.launch {
            delay(CALLOUT_SUPPRESS_TIMEOUT_MS)
            menuActive = false
        }
    }

    companion object {
        private const val TAG = "SoundscapeService"

        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 3600.seconds

        private const val CALLOUT_SUPPRESS_TIMEOUT_MS = 8_000L

        private const val CHANNEL_ID = "SoundscapeService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "Soundscape_SoundscapeService"
        private const val NOTIFICATION_ID = 100000

        // Keys used to persist route/beacon playback across a sleep mode stop/restart.
        private const val SLEEP_RESUME_ACTIVE_KEY = "sleep_resume_active"
        private const val SLEEP_RESUME_ROUTE_ID_KEY = "sleep_resume_route_id"
        private const val SLEEP_RESUME_REVERSE_KEY = "sleep_resume_reverse"
        private const val SLEEP_RESUME_WAYPOINT_KEY = "sleep_resume_waypoint"
        private const val SLEEP_RESUME_BEACON_NAME_KEY = "sleep_resume_beacon_name"
        private const val SLEEP_RESUME_BEACON_LAT_KEY = "sleep_resume_beacon_lat"
        private const val SLEEP_RESUME_BEACON_LON_KEY = "sleep_resume_beacon_lon"

        //      Variable used when simulating startForeground failure - only for debug usage
        private var startForegroundShouldFail = false
    }
}

// Binder to allow local clients to Bind to our service
class SoundscapeBinder(newService: SoundscapeService?) : Binder() {
    var service: SoundscapeService? = newService
    fun getSoundscapeService(): SoundscapeService {
        return service!!
    }

    fun reset() {
        service = null
    }
}
