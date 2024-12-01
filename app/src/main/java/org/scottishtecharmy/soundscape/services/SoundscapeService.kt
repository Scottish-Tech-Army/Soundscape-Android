package org.scottishtecharmy.soundscape.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.squareup.otto.Bus
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
import org.scottishtecharmy.soundscape.activityrecognition.ActivityTransition
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.AndroidDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Publish/Subscribe Otto bus used for some types of communication within the service.
 */
private var ottoBus : Bus? = null
fun getOttoBus() : Bus {
    if(ottoBus == null) {
        ottoBus = Bus()
    }
    return ottoBus!!
}

/**
 * Foreground service that provides location updates, device orientation updates, requests tiles,
 * data persistence with realmDB. It inherits from MediaSessionService so that we can receive
 * Media Transport button presses to act as a remote control whilst the phone is locked.
 */
@AndroidEntryPoint
class SoundscapeService : MediaSessionService() {

    private val coroutineScope = CoroutineScope(Job())

    lateinit var locationProvider: LocationProvider
    lateinit var directionProvider: DirectionProvider

    // secondary service
    private var timerJob: Job? = null

    // Audio engine
    var audioEngine = NativeAudioEngine()
    private var audioBeacon: Long = 0

    // Geo engine
    var geoEngine = GeoEngine()

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow<LngLatAlt?>(null)
    var beaconFlow: StateFlow<LngLatAlt?> = _beaconFlow

    // Flow to return street preview mode
    private val _streetPreviewFlow = MutableStateFlow(false)
    var streetPreviewFlow: StateFlow<Boolean> = _streetPreviewFlow

    // Activity recognition
    private lateinit var activityTransition: ActivityTransition

    // Media control button code
    private var mediaSession: MediaSession? = null
    private val mediaPlayer = SoundscapeDummyMediaPlayer()

    private var running: Boolean = false

    private var binder : SoundscapeBinder? = null
    @SuppressLint("MissingSuperCall")
    override fun onBind(intent: Intent?): IBinder {
        if(binder == null) {
            // Create binder if we don't have one already
            binder = SoundscapeBinder(this@SoundscapeService)
        }
        return binder!!
    }

    fun setStreetPreviewMode(on : Boolean, latitude: Double, longitude: Double) {
        directionProvider.destroy()
        locationProvider.destroy()
        geoEngine.stop()
        if(on) {
            // Use static location, but phone's direction
            locationProvider = StaticLocationProvider(latitude, longitude)
            directionProvider = AndroidDirectionProvider(this)
        } else
        {
            // Switch back to phone's location and direction
            locationProvider = AndroidLocationProvider(this)
            directionProvider = AndroidDirectionProvider(this)
        }
        locationProvider.start(this)
        directionProvider.start(audioEngine, locationProvider)
        geoEngine.start(application, locationProvider, directionProvider, this)

        _streetPreviewFlow.value = on
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            startAsForegroundService()

            locationProvider.start(this)
            directionProvider.start(audioEngine, locationProvider)

            // Reminds the user every hour that the Soundscape service is still running in the background
            startServiceStillRunningTicker()
            geoEngine.start(application, locationProvider, directionProvider, this)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        if (!running) {

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            locationProvider = AndroidLocationProvider(this)
            directionProvider = AndroidDirectionProvider(this)

            // create new RealmDB or open existing
            startRealms()

            // Activity Recognition
            // test
            activityTransition = ActivityTransition()
            activityTransition.startVehicleActivityTracking(
                applicationContext,
                onSuccess = { },
                onFailure = { },
            )

            mediaSession = MediaSession.Builder(this, mediaPlayer)
                .setCallback(SoundscapeMediaSessionCallback(this))
                .build()
        }
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

        timerJob?.cancel()
        geoEngine.stop()

        coroutineScope.coroutineContext.cancelChildren()

        activityTransition.stopVehicleActivityTracking(applicationContext)

        // Clear service reference in binder so that it can be garbage collected
        binder?.reset()
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     *
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService() {

        // promote service to foreground service
        // FOREGROUND_SERVICE_TYPE_LOCATION needs to be in AndroidManifest.xml
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            getNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        destroyBeacon()
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

    private fun startRealms() {
        RealmConfiguration.getMarkersInstance()
    }

    /*    fun deleteRealm(){
            // need this to clean up my mess while I work on the db schema, etc.
            val config = io.realm.kotlin.RealmConfiguration.create(setOf(TileData::class))
            // Delete the realm
            Realm.deleteRealm(config)
        }*/

    fun createBeacon(latitude: Double, longitude: Double) {
        if (audioBeacon != 0L) {
            audioEngine.destroyBeacon(audioBeacon)
        }
        audioBeacon = audioEngine.createBeacon(latitude, longitude)
        // Report any change in beacon back to application
        _beaconFlow.value = LngLatAlt(longitude, latitude)
    }

    fun destroyBeacon() {
        if (audioBeacon != 0L) {
            audioEngine.destroyBeacon(audioBeacon)
            audioBeacon = 0L
        }
        // Report any change in beacon back to application
        _beaconFlow.value = LngLatAlt(0.0, 0.0)
    }

    fun myLocation() {
        audioEngine.clearTextToSpeechQueue()
        val results = geoEngine.myLocation()
        speakCallout(results)
    }

    fun whatsAroundMe() {
        audioEngine.clearTextToSpeechQueue()
        val results = geoEngine.whatsAroundMe()
        speakCallout(results)
    }

    fun aheadOfMe() {
        audioEngine.clearTextToSpeechQueue()
        val results = geoEngine.aheadOfMe()
        speakCallout(results)
    }

    private lateinit var routePlayer : RoutePlayer
    fun setupCurrentRoute() {
        routePlayer = RoutePlayer(this)
        routePlayer.setupCurrentRoute()
    }

    fun speakCallout(callouts: List<PositionedString>) {
        audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_ENTER)
        for(result in callouts) {
            if(result.location == null) {
                audioEngine.createTextToSpeech(result.text)
            }
            else {
                audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_POI, result.location.latitude, result.location.longitude)
                audioEngine.createTextToSpeech(
                    result.text,
                    result.location.latitude,
                    result.location.longitude
                )
            }
        }
        audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_EXIT)
    }

    companion object {
        private const val TAG = "SoundscapeService"

        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 3600.seconds

        private const val CHANNEL_ID = "SoundscapeService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "Soundscape_SoundscapeService"
        private const val NOTIFICATION_ID = 100000
    }
}

// Binder to allow local clients to Bind to our service
class SoundscapeBinder(newService : SoundscapeService?) : Binder() {
    var service : SoundscapeService? = newService
    fun getSoundscapeService(): SoundscapeService {
        return service!!
    }
    fun reset() {
        service = null
    }
}
