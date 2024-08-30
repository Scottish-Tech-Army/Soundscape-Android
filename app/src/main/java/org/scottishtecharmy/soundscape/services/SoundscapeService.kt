package org.scottishtecharmy.soundscape.services

import android.app.Application
import android.app.Service
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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.activityrecognition.ActivityTransition
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.TilesDao
import org.scottishtecharmy.soundscape.database.repository.TilesRepository
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.OkhttpClientInstance
import org.scottishtecharmy.soundscape.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.utils.getTilesForRegion
import org.scottishtecharmy.soundscape.utils.processTileString
import io.realm.kotlin.ext.query
import io.realm.kotlin.Realm
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.database.local.model.TileData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.AndroidDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import org.scottishtecharmy.soundscape.utils.getCompassLabelFacing
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getQuadKey
import org.scottishtecharmy.soundscape.utils.getXYTile
import retrofit2.awaitResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that provides location updates, device orientation updates, requests tiles, data persistence with realmDB.
 */
@AndroidEntryPoint
class SoundscapeService : Service() {
    private val binder = LocalBinder()

    private val coroutineScope = CoroutineScope(Job())

    lateinit var locationProvider : LocationProvider
    lateinit var directionProvider : DirectionProvider

    // The intentLatitude is passed in when we want to use a StaticLocationProvider to report this
    // location. Purely for testing purposes.
    private var intentLatitude : Double? = null
    private var intentLongitude : Double? = null

    // secondary service
    private var timerJob: Job? = null

    // GeoJSON tiles job
    private var tilesJob: Job? = null

    // Audio engine
    private var audioEngine = NativeAudioEngine()
    private var audioBeacon: Long = 0

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow<LngLatAlt?>(null)
    var beaconFlow: StateFlow<LngLatAlt?> = _beaconFlow

    // OkhttpClientInstance
    private lateinit var okhttpClientInstance: OkhttpClientInstance

    // Realm
    private lateinit var realm: Realm

    // Activity recognition
    private lateinit var activityTransition: ActivityTransition

    private var running : Boolean = false

    // Binder to allow local clients to Bind to our service
    inner class LocalBinder : Binder() {
        fun getService(): SoundscapeService = this@SoundscapeService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind $intentLatitude,$intentLongitude")

        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intentLatitude = intent?.extras?.getDouble("latitude")
        intentLongitude = intent?.extras?.getDouble("longitude")

        Log.d(TAG, "onStartCommand $running : $intentLatitude,$intentLongitude")

        var restarted = false
        if((intentLatitude != null) && (intentLongitude != null)) {
            // We have a location passed in, so use that is the location provider in place of
            // the AndroidLocationProvider. Restart/start all of the providers
            Log.d(TAG, "Update service location to: $intentLatitude,$intentLongitude")
            locationProvider = StaticLocationProvider(intentLatitude!!, intentLongitude!!)
            locationProvider.start(this)
            directionProvider.start(audioEngine, locationProvider)

            restarted = true
        }

        if(!running) {
            running = true
            startAsForegroundService()

            if(!restarted) {
                locationProvider.start(this)
                directionProvider.start(audioEngine, locationProvider)
            }

            // Reminds the user every hour that the Soundscape service is still running in the background
            startServiceStillRunningTicker()
            startTileGridService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        if(!running) {

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            locationProvider = AndroidLocationProvider(this)
            directionProvider = AndroidDirectionProvider(this)

            // create new RealmDB or open existing
            startRealm()

            // Activity Recognition
            // test
            activityTransition = ActivityTransition(applicationContext)
            activityTransition.startVehicleActivityTracking(
                onSuccess = { },
                onFailure = { },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        audioEngine.destroyBeacon(audioBeacon)
        audioEngine.destroy()

        locationProvider.destroy()
        directionProvider.destroy()

        timerJob?.cancel()
        tilesJob?.cancel()

        coroutineScope.coroutineContext.cancelChildren()

        activityTransition.stopVehicleActivityTracking()

        //Toast.makeText(this, "Foreground Service destroyed", Toast.LENGTH_SHORT).show()
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

    private fun startTileGridService() {
        tilesJob?.cancel()
        tilesJob = coroutineScope.launch {
            tilesFlow(30.seconds)
                .collectLatest {
                    withContext(Dispatchers.IO) {
                        getTileGrid(application)
                    }
                }
        }
    }

    private fun tilesFlow(
        period: Duration
    ) = flow {
        while (true) {
            delay(10.seconds)
            emit(Unit)
            delay(period)
        }
    }

     private fun tickerFlow(
         period: Duration = TICKER_PERIOD_SECONDS,
         initialDelay: Duration = TICKER_PERIOD_SECONDS
     ) = flow {
         while (true){
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

    private suspend fun getTileGrid(application: Application){

        val tileGridQuadKeys = getTilesForRegion(locationProvider.getCurrentLatitude() ?: 0.0, locationProvider.getCurrentLongitude() ?: 0.0, 250.0)
        val tilesDao = TilesDao(realm)
        val tilesRepository = TilesRepository(tilesDao)
        okhttpClientInstance = OkhttpClientInstance(application)

        for(tile in tileGridQuadKeys){
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db
            if(frozenResult.size == 0){
                withContext(Dispatchers.IO) {
                    val service = okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                    val tileReq = async { tile.tileX.let { service?.getTileWithCache(tile.tileX, tile.tileY) } }
                    val result = tileReq.await()?.awaitResponse()?.body()
                    // clean the tile, process the string, perform an insert into db using the clean tile data
                    val cleanedTile =
                        result?.let { cleanTileGeoJSON(tile.tileX, tile.tileY, 16.0, it) }

                    if (cleanedTile != null) {
                        val tileData = processTileString(tile.quadkey, cleanedTile)
                        tilesRepository.insertTile(tileData)
                    }
                }
            }else{
                // get the current time and then check against lastUpdated in frozenResult
                val currentInstant: java.time.Instant = java.time.Instant.now()
                val currentTimeStamp: Long = currentInstant.toEpochMilli() / 1000
                val lastUpdated: RealmInstant = frozenResult[0].lastUpdated!!
                Log.d(TAG, "Current time: $currentTimeStamp Tile lastUpdated: ${lastUpdated.epochSeconds}")
                // How often do we want to update the tile? 24 hours?
                val timeToLive: Long = lastUpdated.epochSeconds.plus(TTL_REFRESH_SECONDS)

                if(timeToLive >= currentTimeStamp) {
                    Log.d(TAG, "Tile does not need updating yet get local copy")
                    // There should only ever be one tile with a unique quad key
                    //val tileDataTest = tilesRepository.getTile(tile.quadkey)

                } else {
                    Log.d(TAG, "Tile does need updating")
                    withContext(Dispatchers.IO) {
                        val service =
                            okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                        val tileReq = async { tile.tileX.let { service?.getTileWithCache(tile.tileX, tile.tileY) } }
                        val result = tileReq.await()?.awaitResponse()?.body()
                        // clean the tile, process the string, perform an update on db using the clean tile
                        val cleanedTile = result?.let { cleanTileGeoJSON(tile.tileX, tile.tileY, 16.0, it) }

                        if (cleanedTile != null) {
                            val tileData = processTileString(tile.quadkey, cleanedTile)
                            // update existing tile in db
                            tilesRepository.updateTile(tileData)
                        }
                    }
                }
            }
        }
    }

    private fun startRealm(){
        realm = RealmConfiguration.getInstance()
    }

/*    fun deleteRealm(){
        // need this to clean up my mess while I work on the db schema, etc.
        val config = io.realm.kotlin.RealmConfiguration.create(setOf(TileData::class))
        // Delete the realm
        Realm.deleteRealm(config)
    }*/

    fun createBeacon(latitude: Double, longitude: Double) {
        if(audioBeacon != 0L)
        {
            audioEngine.destroyBeacon(audioBeacon)
        }
        audioBeacon = audioEngine.createBeacon(latitude, longitude)
        // Report any change in beacon back to application
        _beaconFlow.value = LngLatAlt(longitude, latitude)
    }

    fun destroyBeacon() {
        if(audioBeacon != 0L) {
            audioEngine.destroyBeacon(audioBeacon)
            audioBeacon = 0L
        }
        // Report any change in beacon back to application
        _beaconFlow.value = LngLatAlt(0.0,0.0)
    }

    fun myLocation() {

        val orientation = directionProvider.mutableOrientationFlow.value?.headingDegrees ?: 0.0
        val facingCompassDirection = getCompassLabelFacing(applicationContext, orientation.toInt())
        // fetch the road from Realm
        val xyTilePair = getXYTile(locationProvider.getCurrentLatitude() ?: 0.0, locationProvider.getCurrentLongitude() ?: 0.0)
        Log.d(TAG, "Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
        // just retrieving a single tile for now
        val currentQuadKey = getQuadKey(xyTilePair.first, xyTilePair.second, 16)
        val frozenResult = realm.query<TileData>("quadKey == $0", currentQuadKey).first().find()
        val nearestRoad = frozenResult?.roads
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val nearestRoadFC = nearestRoad?.let {
            moshi.adapter(FeatureCollection::class.java).fromJson(
                it
            )
        }
        val currentRoad =
            nearestRoadFC?.let {
                getNearestRoad(LngLatAlt(locationProvider.getCurrentLongitude() ?: 0.0, locationProvider.getCurrentLatitude() ?: 0.0),
                    it
                )
            }

        if(currentRoad?.features?.get(0)?.properties!!["name"] != "null") {
            val speechText =
                "$facingCompassDirection along ${currentRoad.features[0].properties!!["name"]}"
            audioEngine.createTextToSpeech(
                locationProvider.getCurrentLatitude() ?: 0.0,
                locationProvider.getCurrentLongitude() ?: 0.0,
                speechText
            )
        }
        else {
            Log.e(TAG, "No name property for road")
        }
    }

    companion object {
        private const val TAG = "SoundscapeService"
        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 3600.seconds
        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60


        private const val CHANNEL_ID = "LocationService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundscapeAlpha_LocationService"
        private const val NOTIFICATION_ID = 100000
    }
}