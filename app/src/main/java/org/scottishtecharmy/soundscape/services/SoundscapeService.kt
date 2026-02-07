package org.scottishtecharmy.soundscape.services

import android.content.Context
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
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
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
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
import org.scottishtecharmy.soundscape.utils.Analytics
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

    // Audio engine
    var audioEngine = NativeAudioEngine(this)
    private var audioBeacon: Long = 0

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

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow(BeaconState())
    var beaconFlow: StateFlow<BeaconState> = _beaconFlow

    // Flow to return street preview mode
    private val _streetPreviewFlow = MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF))
    var streetPreviewFlow: StateFlow<StreetPreviewState> = _streetPreviewFlow

    // Flow to return nearby places
    private val _gridStateFlow = MutableStateFlow<GridState?>(null)
    var gridStateFlow: StateFlow<GridState?> = _gridStateFlow

    // Media control button code
    private var mediaSession: MediaSession? = null
    private val mediaPlayer = SoundscapeDummyMediaPlayer()

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
        geoEngine.start(application, locationProvider, directionProvider, this)
    }

    fun tileGridUpdated() {
        if(_streetPreviewFlow.value.enabled == StreetPreviewEnabled.INITIALIZING) {
            _streetPreviewFlow.value = StreetPreviewState(
                StreetPreviewEnabled.ON,
                geoEngine.streetPreviewGo()
            )
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

                geoEngine.start(application, locationProvider, directionProvider, this)
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

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

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

            mediaSession = MediaSession.Builder(this, mediaPlayer)
                .setCallback(SoundscapeMediaSessionCallback(this))
                .build()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved for service - ignoring, as we want to keep running")
    }
    override fun onDestroy() {
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
                            "Soundscape Service is still running.",
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
            if(results != null) {
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
            Log.w(TAG, "speakText: Could not get audio focus. Aborting callouts.")
            return
        }
        Log.d(TAG, "speakText $text")
        audioEngine.createTextToSpeech(text, type, latitude, longitude, heading)
    }


    fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean) {

        if(callout == null) return

        if (!requestAudioFocus()) {
            Log.w(TAG, "SpeakCallout: Could not get audio focus. Aborting callouts.")
            return
        }

        if(addModeEarcon) audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_ENTER, AudioType.STANDARD)
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
        if(addModeEarcon) audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_EXIT, AudioType.STANDARD)

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
        _streetPreviewFlow.value = _streetPreviewFlow.value.copy(choices = geoEngine.streetPreviewGo())
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

    companion object {
        private const val TAG = "SoundscapeService"

        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 3600.seconds

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
