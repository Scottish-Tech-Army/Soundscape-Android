package com.kersnazzle.soundscapealpha.services

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
import com.kersnazzle.soundscapealpha.R
import com.kersnazzle.soundscapealpha.database.local.RealmConfiguration
import com.kersnazzle.soundscapealpha.database.local.dao.TilesDao
import com.kersnazzle.soundscapealpha.database.local.model.TileData
import com.kersnazzle.soundscapealpha.database.repository.TilesRepository
import com.kersnazzle.soundscapealpha.network.ITileDAO
import com.kersnazzle.soundscapealpha.network.OkhttpClientInstance
import com.kersnazzle.soundscapealpha.utils.cleanTileGeoJSON
import com.kersnazzle.soundscapealpha.utils.getQuadKey
import com.kersnazzle.soundscapealpha.utils.getXYTile
import com.kersnazzle.soundscapealpha.utils.processTileString
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
import retrofit2.awaitResponse
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service that provides location updates, device orientation updates, requests tiles, data persistence with realmDB.
 */
class LocationService : Service() {
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



    // Binder to allow local clients to Bind to our service
    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
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


        // Start secondary service
        //startServiceRunningTicker()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        fusedLocationClient.removeLocationUpdates(locationCallback)

        fusedOrientationProviderClient.removeOrientationUpdates(listener)

        timerJob?.cancel()
        coroutineScope.coroutineContext.cancelChildren()

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

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.nearby_markers_24px)
            .setOngoing(true)

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



    suspend fun getTileStringCaching(application: Application): String? {
        val tileXY = _locationFlow.value?.let { getXYTile(it.latitude, _locationFlow.value!!.longitude) }
        // generate the Quad Key for the current tile we are in
        val currentQuadKey = getQuadKey(tileXY!!.first, tileXY.second, 16)
        // check the Realm db to see if the tile already exists using the Quad Key. There should only ever be one result
        // or zero as we are using the Quad Key as the primary key
        // TODO check frozen result and if it already exists use that (need a TTL for the tile in the db?)
        //  if it doesn't exist already go get it from the network and insert into db

        val tilesDao = TilesDao(realm)
        val tilesRepository = TilesRepository(tilesDao)
        val frozenResult = tilesRepository.getTile(currentQuadKey)

        // there isn't a tile matching the current location in the db so go and get it from the backend
        if(frozenResult.size == 0){
            okhttpClientInstance = OkhttpClientInstance(application)

            return withContext(Dispatchers.IO) {

                val service = okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                val tile = async { tileXY?.let { service?.getTileWithCache(it.first, tileXY.second) } }
                val result = tile.await()?.awaitResponse()?.body()
                // clean the tile, process the string, perform an insert into db using the clean tile, and return clean tile string
                val cleanedTile =
                    result?.let { cleanTileGeoJSON(tileXY.first, tileXY.second, 16.0, it) }

                if (cleanedTile != null) {
                    val tileData = processTileString(currentQuadKey, cleanedTile)
                    tilesRepository.insertTile(tileData)
                }

                // checking that I can retrieve it from the realm db
                val tileDataTest = tilesRepository.getTile(currentQuadKey)

                return@withContext tileDataTest[0].tileString
                }
        }else{
            // get the current time and then check against lastUpdated in frozenResult
            val currentInstant: java.time.Instant = java.time.Instant.now()
            val currentTimeStamp: Long = currentInstant.toEpochMilli() / 1000
            val lastUpdated: RealmInstant = frozenResult[0].lastUpdated!!
            Log.d(TAG, "Current time: $currentTimeStamp Tile lastUpdated: ${lastUpdated.epochSeconds}")
            // How often do we want to update the tile? 24 hours?
            val timeToLive: Long =
                lastUpdated.epochSeconds!!.plus((24 * 60 * 60)) // 24 hours in seconds added to last updated
                if(timeToLive <= currentTimeStamp) {
                    Log.d(TAG, "Tile does not need updating yet")
                } else {
                    Log.d(TAG, "Tile does need updating")
                }



            return withContext(Dispatchers.IO){
                // there should only ever be one matching tile so return the tileString
                // checking that I can retrieve it from the realm db
                val tileDataTest = tilesRepository.getTile(currentQuadKey)
                return@withContext tileDataTest[0].tileString
            }

        }

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

    companion object {
        private const val TAG = "LocationService"
        // Check for GPS every n seconds
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 30.seconds


        private const val CHANNEL_ID = "LocationService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "SoundscapeAlpha_LocationService"
        private const val NOTIFICATION_ID = 100000
    }
}