package org.scottishtecharmy.soundscape.services

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.content.pm.PackageManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.MEDIA_CONTROLS_MODE_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.MEDIA_CONTROLS_MODE_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_ON
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_MODE_ENTER
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_MODE_EXIT
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.hasPlayServices
import org.scottishtecharmy.soundscape.locationprovider.AndroidDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.GooglePlayDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.GooglePlayLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenu
import org.scottishtecharmy.soundscape.services.mediacontrol.AudioMenuMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControlTarget
import org.scottishtecharmy.soundscape.services.mediacontrol.OriginalMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.SoundscapeDummyMediaPlayer
import org.scottishtecharmy.soundscape.services.mediacontrol.SoundscapeMediaSessionCallback
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandManager
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandMediaControls
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds



/**
 * Foreground service that provides location updates, device orientation updates, requests tiles,
 * data persistence with realmDB. It inherits from MediaSessionService so that we can receive
 * Media Transport button presses to act as a remote control whilst the phone is locked.
 */
data class BeaconState(val location: LngLatAlt? = null, val muteState: Boolean = false)

@AndroidEntryPoint
class SoundscapeService : MediaSessionService() {

    private val coroutineScope = CoroutineScope(Job())

    lateinit var locationProvider: LocationProvider
    lateinit var directionProvider: DirectionProvider
    lateinit var routePlayer: RoutePlayer

    // secondary service
    private var timerJob: Job? = null

    // Wake lock — keeps CPU running while screen is off so audio callbacks continue
    private var wakeLock: PowerManager.WakeLock? = null

    // Audio engine
    var audioEngine = NativeAudioEngine(this)
    private var audioBeacon: Long = 0

    // Audio menu (navigated via media buttons when no route is active)
    var audioMenu : AudioMenu? = null

    /** True while the user is actively navigating the audio menu. Suppresses auto callouts. */
    var menuActive: Boolean = false

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

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow(BeaconState())
    var beaconFlow: StateFlow<BeaconState> = _beaconFlow

    // Flow to return street preview mode
    private val _streetPreviewFlow = MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF))
    var streetPreviewFlow: StateFlow<StreetPreviewState> = _streetPreviewFlow

    // Flow to return nearby places
    private val _gridStateFlow = MutableStateFlow<GridState?>(null)
    var gridStateFlow: StateFlow<GridState?> = _gridStateFlow

    // Voice command manager
    private lateinit var voiceCommandManager: VoiceCommandManager
    val voiceCommandStateFlow: StateFlow<VoiceCommandState>
        get() = voiceCommandManager.state

    // Media control button code
    private var mediaSession: MediaSession? = null

    private var mediaControlsTarget : MediaControlTarget = OriginalMediaControls(this)
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

    fun setStreetPreviewMode(on: Boolean, location: LngLatAlt?) {
        directionProvider.destroy()
        locationProvider.destroy()
        geoEngine.stop()
        if (on) {
            // Use static location, but phone's direction
            if(location != null) {
                locationProvider = StaticLocationProvider(location)
                directionProvider = if(hasPlayServices(this))
                    GooglePlayDirectionProvider(this)
                else
                    AndroidDirectionProvider(this)
            }
        } else {
            // Switch back to phone's location and direction
            if(hasPlayServices(this)) {
                locationProvider = GooglePlayLocationProvider(this)
                directionProvider = GooglePlayDirectionProvider(this)
            } else {
                locationProvider = AndroidLocationProvider(this)
                directionProvider = AndroidDirectionProvider(this)
            }
        }

        // Set the StreetPreview state prior to starting the location provider. Otherwise there's a
        // race in the tileGridUpdated callback.
        _streetPreviewFlow.value = StreetPreviewState(if(on) StreetPreviewEnabled.INITIALIZING else StreetPreviewEnabled.OFF)

        locationProvider.start(this)
        directionProvider.start(audioEngine, locationProvider)
        geoEngine.start(application, locationProvider, directionProvider, this, localizedContext)
    }

    fun tileGridUpdated() {
        if(_streetPreviewFlow.value.enabled == StreetPreviewEnabled.INITIALIZING) {
            val choices = geoEngine.streetPreviewGo()
            _streetPreviewFlow.value = StreetPreviewState(
                StreetPreviewEnabled.ON,
                choices
            )
            geoEngine.recomputeStreetPreviewBestChoice(this)
        }
        _gridStateFlow.value = geoEngine.gridState
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            if(startAsForegroundService()) {
                // Reminds the user every hour that the Soundscape service is still running in the background
                startServiceStillRunningTicker()
                running = true
            }

            if(!started) {
                Analytics.getInstance().crashLogNotes("Start geo-engine")
                locationProvider.start(this)
                directionProvider.start(audioEngine, locationProvider)
                val configLocale = getCurrentLocale()
                val configuration = Configuration(applicationContext.resources.configuration)
                configuration.setLocale(configLocale)
                localizedContext = applicationContext.createConfigurationContext(configuration)
                if (::voiceCommandManager.isInitialized) voiceCommandManager.updateContext(localizedContext)
                geoEngine.start(application, locationProvider, directionProvider, this, localizedContext)
                started = true
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate $running")

        if (!running) {

            // Hold a partial wake lock for the service lifetime so the CPU stays awake when the
            // screen is off and the Oboe audio callback keeps firing.
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Soundscape::AudioWakeLock")
                .also { it.acquire() }

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioMenu = AudioMenu(this, application)
            routePlayer = RoutePlayer(this, applicationContext)

            if(hasPlayServices(this)) {
                locationProvider = GooglePlayLocationProvider(this)
                directionProvider = GooglePlayDirectionProvider(this)
            } else {
                locationProvider = AndroidLocationProvider(this)
                directionProvider = AndroidDirectionProvider(this)
            }

            // create new RealmDB or open existing
            startRealms(applicationContext)

            // Update the media controls mode
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val mode = sharedPreferences.getString(MEDIA_CONTROLS_MODE_KEY, MEDIA_CONTROLS_MODE_DEFAULT)!!
            updateMediaControls(mode)

            voiceCommandManager = VoiceCommandManager(
                service = this,
                onError = { }
            )

            mediaSession = MediaSession.Builder(this, mediaPlayer)
                .setId("org.scottishtecharmy.soundscape")
                .setCallback(SoundscapeMediaSessionCallback { mediaControlsTarget })
                .build()
        }
    }

    fun updateMediaControls(target: String) {
        mediaControlsTarget = when(target) {
            "Original" -> OriginalMediaControls(this)
            "VoiceControl" -> VoiceCommandMediaControls(this)
            "AudioMenu" -> AudioMenuMediaControls(audioMenu)
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

        locationProvider.destroy()
        directionProvider.destroy()
        started = false

        abandonAudioFocus()

        timerJob?.cancel()
        geoEngine.stop()

        coroutineScope.coroutineContext.cancelChildren()

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        if (::voiceCommandManager.isInitialized) voiceCommandManager.destroy()

        // Clear service reference in binder so that it can be garbage collected
        binder?.reset()
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     *
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService() : Boolean {

        val analytics = Analytics.getInstance()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Code to simulate startForeground failing
                if(startForegroundShouldFail) {
                    startForegroundShouldFail = false
                    throw ForegroundServiceStartNotAllowedException("Simulated startForeground failure")
                }
            }

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                getNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                analytics.crashLogNotes("ForegroundServiceStartNotAllowedException caught")
                analytics.logEvent("startAsForegroundServiceError", null)
                Analytics.getInstance().crashSetCustomKey("Service start success", "false")
                return false
            }
        }
        analytics.crashSetCustomKey("Service start success", "true")
        return true
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        destroyBeacon()
        abandonAudioFocus()
        stopSelf()
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
                            localizedContext.getString(R.string.service_still_running),
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
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.nearby_markers_24px)
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
        MarkersAndRoutesDatabase.getMarkersInstance(context)
    }

    /*    fun deleteRealm(){
            // need this to clean up my mess while I work on the db schema, etc.
            val config = io.realm.kotlin.RealmConfiguration.create(setOf(TileData::class))
            // Delete the realm
            Realm.deleteRealm(config)
        }*/

    fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {
        if(location == null) return

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

    fun destroyBeacon() {
        if (audioBeacon != 0L) {
            audioEngine.destroyBeacon(audioBeacon)
            audioBeacon = 0L
        }
        // Report any change in beacon back to application
        _beaconFlow.value = _beaconFlow.value.copy(location = null)
        geoEngine.updateBeaconLocation(null)
    }

    fun myLocation() {
        coroutineScope.launch {
            val results = geoEngine.myLocation()
            if(results != null) {
                audioEngine.clearTextToSpeechQueue()
                speakCallout(results, true)
            }
        }
    }

    fun whatsAroundMe() {
        coroutineScope.launch {
            val results = geoEngine.whatsAroundMe()
            if(results.positionedStrings.isNotEmpty()) {
                audioEngine.clearTextToSpeechQueue()
                speakCallout(results, true)
            }
        }
    }

    fun aheadOfMe() {
        coroutineScope.launch {
            val results = geoEngine.aheadOfMe()
            if(results != null) {
                audioEngine.clearTextToSpeechQueue()
                speakCallout(results, true)
            }
        }
    }

    fun nearbyMarkers() {
        coroutineScope.launch {
            val results = geoEngine.nearbyMarkers()
            if(results != null) {
                audioEngine.clearTextToSpeechQueue()
                speakCallout(results, true)
            }
        }
    }

    fun triggerVoiceCommand() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        if (!requestAudioFocus()) {
            Log.w(TAG, "speakText: Could not get audio focus. Aborting callouts.")
            return
        }

        // Stop callbacks whilst we handle voice commands
        callbackHoldOff()

        val ctx = if (::localizedContext.isInitialized) localizedContext else this

        // Inform the user that we're listening
        speak2dText(ctx.getString(R.string.voice_cmd_listening), true, EARCON_CALLOUTS_ON)

        // Wait for the TTS to finish before opening the mic
        coroutineScope.launch {
            val deadline = System.currentTimeMillis() + 3_000L
            while (isAudioEngineBusy() && System.currentTimeMillis() < deadline) {
                delay(50)
            }
            withContext(Dispatchers.Main) {
                voiceCommandManager.startListening()
            }
        }
    }

    suspend fun searchResult(searchString: String): List<LocationDescription>? {
        return geoEngine.searchResult(searchString)
    }

    fun getLocationDescription(location: LngLatAlt) : LocationDescription {
        return geoEngine.getLocationDescription(location)
    }

    fun startBeacon(location: LngLatAlt, name: String) {
        routePlayer.startBeacon(location, name)
    }
    fun routeStart(routeId: Long) {
        routePlayer.startRoute(routeId)
    }
    fun routeStop() {
        routePlayer.stopRoute()
    }
    fun routeSkipPrevious(): Boolean {
        return routePlayer.moveToPrevious()
    }
    fun routeSkipNext(): Boolean {
        return routePlayer.moveToNext()
    }
    fun routeMute(): Boolean {
        if(routePlayer.isPlaying()) {
            // Silence any current text-to-speech output
            audioEngine.clearTextToSpeechQueue()

            // Toggle the beacon mute
            val muteState = audioEngine.toggleBeaconMute()
            // Update the beacon flow with the new mute state
            _beaconFlow.value = _beaconFlow.value.copy(muteState = muteState)

            return true
        }
        return false
    }
    fun routeListRoutes() {
        coroutineScope.launch {
            val ctx = if (::localizedContext.isInitialized) localizedContext else this@SoundscapeService
            val routes = MarkersAndRoutesDatabase.getMarkersInstance(applicationContext).routeDao().getAllRoutes()
            if (routes.isEmpty())
                speak2dText(ctx.getString(R.string.voice_cmd_no_routes))
            else {
               val names = routes.joinToString(", ") { it.name }
                speak2dText(ctx.getString(R.string.voice_cmd_routes_list) + names)
            }
        }
    }

    fun routeStartByName(name: String) {
        if (name.isEmpty()) {
            routeListRoutes()
            return
        }
        coroutineScope.launch {
            val ctx = if (::localizedContext.isInitialized) localizedContext else this@SoundscapeService
            val routes = MarkersAndRoutesDatabase.getMarkersInstance(applicationContext).routeDao().getAllRoutes()
            val nameLower = name.lowercase()
            var bestId = -1L
            var bestScore = Double.MAX_VALUE
            for (route in routes) {
                val score = nameLower.fuzzyCompare(route.name.lowercase(), true)
                if (score < 0.4 && score < bestScore) {
                    bestScore = score
                    bestId = route.routeId
                }
            }
            if (bestId != -1L) {
                val routeName = routes.first { it.routeId == bestId }.name
                speak2dText(ctx.getString(R.string.voice_cmd_starting_route).format(routeName))
                routeStart(bestId)
            } else {
                speak2dText(ctx.getString(R.string.voice_cmd_route_not_found).format(name))
            }
        }
    }

    fun routeListMarkers() {
        coroutineScope.launch {
            val ctx = if (::localizedContext.isInitialized) localizedContext else this@SoundscapeService
            val markers = MarkersAndRoutesDatabase.getMarkersInstance(applicationContext).routeDao().getAllMarkers()
            if (markers.isEmpty()) {
                speak2dText(ctx.getString(R.string.voice_cmd_no_markers))
            } else {
                val names = markers.joinToString(", ") { it.name }
                speak2dText(ctx.getString(R.string.voice_cmd_markers_list) + names)
            }
        }
    }

    fun markerStartByName(name: String) {
        if (name.isEmpty()) {
            routeListMarkers()
            return
        }
        coroutineScope.launch {
            val ctx = if (::localizedContext.isInitialized) localizedContext else this@SoundscapeService
            val markers = MarkersAndRoutesDatabase.getMarkersInstance(applicationContext).routeDao().getAllMarkers()
            val nameLower = name.lowercase()
            var bestId = -1L
            var bestScore = Double.MAX_VALUE
            for (marker in markers) {
                val score = nameLower.fuzzyCompare(marker.name.lowercase(), true)
                if (score < 0.4 && score < bestScore) {
                    bestScore = score
                    bestId = marker.markerId
                }
            }
            if (bestId != -1L) {
                val marker = markers.first { it.markerId == bestId }
                speak2dText(ctx.getString(R.string.voice_cmd_starting_beacon_at_marker).format(marker.name))

                val location = LngLatAlt(marker.longitude, marker.latitude)
                startBeacon(location, marker.name)
            } else {
                speak2dText(ctx.getString(R.string.voice_cmd_marker_not_found).format(name))
            }
        }
    }

    /**
     * isAudioEngineBusy returns true if there is more than one entry in the
     * audio engine queue. The queue consists of earcons and text-to-speech.
     */
    fun isAudioEngineBusy() : Boolean {
        val depth = audioEngine.getQueueDepth()
        //Log.d(TAG, "Queue depth: $depth")
        return (depth > 0)
    }

    fun speakText(text: String,
                  type: AudioType,
                  latitude: Double = Double.NaN,
                  longitude: Double = Double.NaN,
                  heading: Double = Double.NaN) {

        if (!requestAudioFocus()) {
            Log.w(TAG, "speakText: Could not get audio focus.")
            return
        }
        Log.d(TAG, "speakText $text")
        audioEngine.createTextToSpeech(text, type, latitude, longitude, heading)
    }

    fun speak2dText(text: String, clearQueue: Boolean = false, earcon: String? = null) {
        if (!requestAudioFocus()) {
            Log.w(TAG, "speak2dText: Could not get audio focus.")
            return
        }
        if(clearQueue)
            audioEngine.clearTextToSpeechQueue()
        if(earcon != null) {
            audioEngine.createEarcon(earcon, AudioType.STANDARD)
        }
        audioEngine.createTextToSpeech(text, AudioType.STANDARD)
    }

    fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean) {

        if(callout == null) return

        if (!requestAudioFocus()) {
            Log.w(TAG, "SpeakCallout: Could not get audio focus.")
            return
        }

        if(addModeEarcon) audioEngine.createEarcon(EARCON_MODE_ENTER, AudioType.STANDARD)
        for(result in callout.positionedStrings) {
            if(result.location == null) {
                var type = result.type
                if(type == AudioType.LOCALIZED) type = AudioType.STANDARD
                if(result.earcon != null) {
                    audioEngine.createEarcon(result.earcon, type, 0.0, 0.0, result.heading?:0.0)
                }
                audioEngine.createTextToSpeech(result.text, type, 0.0, 0.0, result.heading?:0.0)
            }
            else {
                if(result.earcon != null) {
                    audioEngine.createEarcon(
                        result.earcon,
                        result.type,
                        result.location.latitude,
                        result.location.longitude,
                        result.heading?:0.0)
                }
                audioEngine.createTextToSpeech(
                    result.text,
                    result.type,
                    result.location.latitude,
                    result.location.longitude,
                    result.heading?:0.0
                )
            }
        }
        if(addModeEarcon) audioEngine.createEarcon(EARCON_MODE_EXIT, AudioType.STANDARD)

        callout.calloutHistory?.add(callout)
        callout.locationFilter?.update(callout.userGeometry)
    }

    fun toggleAutoCallouts() {
        geoEngine.toggleAutoCallouts()
    }

    /**
     * streetPreviewGo is called when the 'GO' button is pressed when in StreetPreview mode.
     * It indicates that the user has selected the direction of travel in which they which to move.
     */

    fun streetPreviewGo() {
        val choices = geoEngine.streetPreviewGo()
        _streetPreviewFlow.value = _streetPreviewFlow.value.copy(choices = choices, bestChoice = null)
        geoEngine.recomputeStreetPreviewBestChoice(this)
    }

    fun updateStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {
        _streetPreviewFlow.value = _streetPreviewFlow.value.copy(bestChoice = bestChoice)
    }

    fun announceStreetPreviewBestChoice(bestChoice: StreetPreviewChoice) {
        val compassLabel = localizedContext.getString(getCompassLabel(bestChoice.heading.toInt()))
        val go = localizedContext.getString(R.string.preview_go_title)
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
        if(foreground) {
            // If the app has switched to the foreground and we've got an active audio beacon, then
            // we should request audio focus
            if(audioBeacon != 0L)
                requestAudioFocus()
        }
    }

    fun getRecordingShareUri(context: Context): Uri? {
        return geoEngine.getRecordingShareUri(context)
    }

    fun requestAudioFocus(): Boolean {
        if(!audioFocusGained) {
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
    fun callbackHoldOff() {
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
