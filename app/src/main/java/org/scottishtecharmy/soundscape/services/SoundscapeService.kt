package org.scottishtecharmy.soundscape.services

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.FusedOrientationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.activityrecognition.ActivityTransition
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.TilesDao
import org.scottishtecharmy.soundscape.database.local.model.TileData
import org.scottishtecharmy.soundscape.database.repository.TilesRepository
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.OkhttpClientInstance
import org.scottishtecharmy.soundscape.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.utils.getTilesForRegion
import org.scottishtecharmy.soundscape.utils.processTileString
import io.realm.kotlin.Realm
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import retrofit2.awaitResponse
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that provides location updates, device orientation updates, requests tiles, data persistence with realmDB.
 */
class SoundscapeService : Service() {
    private val binder = LocalBinder()

    private val coroutineScope = CoroutineScope(Job())
    // core GPS service
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // core Orientation service - test
    private lateinit var fusedOrientationProviderClient: FusedOrientationProviderClient
    private lateinit var listener: DeviceOrientationListener

    // secondary service
    private var timerJob: Job? = null

    // Audio engine
    private val audioEngine = NativeAudioEngine()
    private var audioBeacon: Long = 0

    // Flow to return beacon location
    private val _beaconFlow = MutableStateFlow<LngLatAlt?>(null)
    var beaconFlow: StateFlow<LngLatAlt?> = _beaconFlow

    // Flow to return Location objects
    private val _locationFlow = MutableStateFlow<Location?>(null)
    var locationFlow: StateFlow<Location?> = _locationFlow

    // Flow to return DeviceOrientation objects
    private val _orientationFlow = MutableStateFlow<DeviceOrientation?>(null)
    var orientationFlow: StateFlow<DeviceOrientation?> = _orientationFlow

    // OkhttpClientInstance
    private lateinit var okhttpClientInstance: OkhttpClientInstance

    // Realm
    private lateinit var realm: Realm

    // Activity recognition
    private lateinit var activityTransition: ActivityTransition



    // Binder to allow local clients to Bind to our service
    inner class LocalBinder : Binder() {
        fun getService(): SoundscapeService = this@SoundscapeService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        startAsForegroundService()
        startLocationUpdates()

        // test
        startOrientationUpdates()



        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        //Toast.makeText(this, "Foreground Service created", Toast.LENGTH_SHORT).show()

        // Set up the location updates using the FusedLocationProviderClient but doesn't start them
        setupLocationUpdates()

        // Start the orientation updates using the FusedOrientationProviderClient - test
        startOrientationUpdates()

        // create new RealmDB or open existing
        startRealm()

        // Activity Recognition
        // test
        activityTransition = ActivityTransition(applicationContext)
        activityTransition.startVehicleActivityTracking(
            onSuccess = { },
            onFailure = { },
        )


        // Start secondary service
        //startServiceRunningTicker()

        // Start audio engine
        audioEngine.initialize(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        audioEngine.destroyBeacon(audioBeacon)
        audioEngine.destroy()

        fusedLocationClient.removeLocationUpdates(locationCallback)

        fusedOrientationProviderClient.removeOrientationUpdates(listener)

        timerJob?.cancel()
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
     * Sets up the location updates using the FusedLocationProviderClient, but doesn't actually start them.
     * To start the location updates, call [startLocationUpdates].
     */
    private fun setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _locationFlow.value = location
                }
            }
        }
    }

    private fun startOrientationUpdates(){

        fusedOrientationProviderClient =
            LocationServices.getFusedOrientationProviderClient(this)

        /*listener = DeviceOrientationListener { orientation: DeviceOrientation ->
                    // Use the orientation object

                    Log.d(TAG, "Device Orientation: ${orientation.headingDegrees} deg")
                }*/
        listener = DeviceOrientationListener { orientation ->
            _orientationFlow.value = orientation  // Emit the DeviceOrientation object
            val location = locationFlow.value
            if(location != null) {
                audioEngine.updateGeometry(
                    location.latitude,
                    location.longitude,
                    orientation.headingDegrees.toDouble()
                )
            }
        }


        // OUTPUT_PERIOD_DEFAULT = 50Hz / 20ms
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()
        // Thought I could use a Looper here like for location but it seems to want an Executor instead
        // Not clear on what the difference is...
        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)


    }


    /**
     * Starts the location updates using the FusedLocationProviderClient.
     * Suppressing IDE warning with annotation. Will check for this in UI.
     *  TODO: Add permission checks and observe for permission changes by user
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        /*fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                LOCATION_UPDATES_INTERVAL_MS
            ).build(), locationCallback, Looper.getMainLooper()
        )*/
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATES_INTERVAL_MS
            ).apply {
                setMinUpdateDistanceMeters(1f)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build(),
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    /**
     * Starts a ticker that shows a toast every [TICKER_PERIOD_SECONDS] seconds to indicate that the service is still running.
     */
    /*private fun startServiceRunningTicker() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            tickerFlow()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@LocationService,
                            "Foreground Service still running.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }*/

    /* private fun tickerFlow(
         period: Duration = TICKER_PERIOD_SECONDS,
         initialDelay: Duration = TICKER_PERIOD_SECONDS
     ) = flow {
         delay(initialDelay)
         while (true) {
             emit(Unit)
             delay(period)
         }
     }*/

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

    suspend fun getTileGrid(application: Application): MutableList<TileData>{
        //TODO Original Soundscape appears to have a 3 x 3 grid of tiles with the current location being the central tile
        val tileGridQuadKeys = getTilesForRegion(_locationFlow.value!!.latitude, _locationFlow.value!!.longitude, 250.0)
        val tilesDao = TilesDao(realm)
        val tilesRepository = TilesRepository(tilesDao)
        okhttpClientInstance = OkhttpClientInstance(application)

        val tileGrid: MutableList<TileData> = mutableListOf()

        for(tile in tileGridQuadKeys){
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db and add to MutableList to return
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
                        // add to mutableList
                        tileGrid.add(tileData)
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
                    val tileDataTest = tilesRepository.getTile(tile.quadkey)
                    // add to mutableList
                    tileGrid.add(tileDataTest[0])

                } else {
                    Log.d(TAG, "Tile does need updating")
                    withContext(Dispatchers.IO) {
                        val service =
                            okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                        val tileReq = async { tile.tileX.let { service?.getTileWithCache(tile.tileX, tile.tileY) } }
                        val result = tileReq.await()?.awaitResponse()?.body()
                        // clean the tile, process the string, perform an update on db using the clean tile, and return clean tile string
                        val cleanedTile = result?.let { cleanTileGeoJSON(tile.tileX, tile.tileY, 16.0, it) }

                        if (cleanedTile != null) {
                            val tileData = processTileString(tile.quadkey, cleanedTile)
                            // update existing tile in db
                            tilesRepository.updateTile(tileData)
                            // add to mutableList
                            tileGrid.add(tileData)
                        }
                    }
                }
            }
        }
        return tileGrid
    }

    private fun startRealm(){
        realm = RealmConfiguration.getInstance()
    }

    fun deleteRealm(){
        // need this to clean up my mess while I work on the db schema, etc.
        val config = io.realm.kotlin.RealmConfiguration.create(setOf(TileData::class))
        // Delete the realm
        Realm.deleteRealm(config)
    }

    fun createBeacon(latitude: Double, longitude: Double) {
        if(audioBeacon != 0L)
        {
            audioEngine.destroyBeacon(audioBeacon)
        }
        audioBeacon = audioEngine.createBeacon(latitude, longitude)
        // Report any change in beacon back to application
        _beaconFlow.value = LngLatAlt(longitude, latitude)
    }

    companion object {
        private const val TAG = "LocationService"
        // Check for GPS every n seconds
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 30.seconds
        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60


        private const val CHANNEL_ID = "LocationService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundscapeAlpha_LocationService"
        private const val NOTIFICATION_ID = 100000
    }
}