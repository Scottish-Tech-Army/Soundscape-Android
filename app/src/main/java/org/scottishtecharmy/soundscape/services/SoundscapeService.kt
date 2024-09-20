package org.scottishtecharmy.soundscape.services

import android.app.Application
import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.preference.PreferenceManager
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
import org.scottishtecharmy.soundscape.MainActivity.Companion.FIRST_LAUNCH_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.UNNAMED_ROADS_KEY
import org.scottishtecharmy.soundscape.database.local.model.TileData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.locationprovider.AndroidDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import org.scottishtecharmy.soundscape.utils.RelativeDirections
import org.scottishtecharmy.soundscape.utils.distanceToIntersection
import org.scottishtecharmy.soundscape.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.utils.get3x3TileGrid
import org.scottishtecharmy.soundscape.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getNearestPoi
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionLabel
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.utils.removeDuplicateOsmIds
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

    lateinit var locationProvider: LocationProvider
    lateinit var directionProvider: DirectionProvider

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

    private var running: Boolean = false

    // Binder to allow local clients to Bind to our service
    inner class LocalBinder : Binder() {
        fun getService(): SoundscapeService = this@SoundscapeService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")

        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(TAG, "onStartCommand $running")

        var restarted = false
        if (intent != null) {
            val beaconLatitude = intent.getDoubleExtra("beacon-latitude", Double.NaN)
            val beaconLongitude = intent.getDoubleExtra("beacon-longitude", Double.NaN)

            if ((!beaconLatitude.isNaN()) && (!beaconLongitude.isNaN())) {
                createBeacon(beaconLatitude, beaconLongitude)
            }

            val mockLatitude = intent.getDoubleExtra("mock-latitude", Double.NaN)
            val mockLongitude = intent.getDoubleExtra("mock-longitude", Double.NaN)

            if ((!mockLatitude.isNaN()) && (!mockLongitude.isNaN())) {
                // We have a location passed in, so use that is the location provider in place of
                // the AndroidLocationProvider. Restart/start all of the providers
                Log.d(TAG, "Update service location to: $mockLatitude,$mockLongitude")
                locationProvider = StaticLocationProvider(mockLatitude, mockLongitude)
                locationProvider.start(this)
                directionProvider.start(audioEngine, locationProvider)

                restarted = true
            }
        }

        if (!running) {
            running = true
            startAsForegroundService()

            if (!restarted) {
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

        if (!running) {

            // Initialize the audio engine
            audioEngine.initialize(applicationContext)

            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val beaconType = sharedPreferences.getString(
                MainActivity.BEACON_TYPE_KEY,
                MainActivity.BEACON_TYPE_DEFAULT
            )
            audioEngine.setBeaconType(beaconType!!)

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
        audioBeacon = 0
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

    private suspend fun getTileGrid(application: Application) {

        val tileGridQuadKeys = get3x3TileGrid(
            locationProvider.getCurrentLatitude() ?: 0.0,
            locationProvider.getCurrentLongitude() ?: 0.0
        )
        val tilesDao = TilesDao(realm)
        val tilesRepository = TilesRepository(tilesDao)
        okhttpClientInstance = OkhttpClientInstance(application)

        for (tile in tileGridQuadKeys) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db
            if (frozenResult.size == 0) {
                withContext(Dispatchers.IO) {
                    val service =
                        okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                    val tileReq = async {
                        tile.tileX.let {
                            service?.getTileWithCache(
                                tile.tileX,
                                tile.tileY
                            )
                        }
                    }
                    val result = tileReq.await()?.awaitResponse()?.body()
                    // clean the tile, process the string, perform an insert into db using the clean tile data
                    val cleanedTile =
                        result?.let { cleanTileGeoJSON(tile.tileX, tile.tileY, 16.0, it) }

                    if (cleanedTile != null) {
                        val tileData = processTileString(tile.quadkey, cleanedTile)
                        tilesRepository.insertTile(tileData)
                    }
                }
            } else {
                // get the current time and then check against lastUpdated in frozenResult
                val currentInstant: java.time.Instant = java.time.Instant.now()
                val currentTimeStamp: Long = currentInstant.toEpochMilli() / 1000
                val lastUpdated: RealmInstant = frozenResult[0].lastUpdated!!
                Log.d(
                    TAG,
                    "Current time: $currentTimeStamp Tile lastUpdated: ${lastUpdated.epochSeconds}"
                )
                // How often do we want to update the tile? 24 hours?
                val timeToLive: Long = lastUpdated.epochSeconds.plus(TTL_REFRESH_SECONDS)

                if (timeToLive >= currentTimeStamp) {
                    Log.d(TAG, "Tile does not need updating yet get local copy")
                    // There should only ever be one tile with a unique quad key
                    //val tileDataTest = tilesRepository.getTile(tile.quadkey)

                } else {
                    Log.d(TAG, "Tile does need updating")
                    withContext(Dispatchers.IO) {
                        val service =
                            okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)
                        val tileReq = async {
                            tile.tileX.let {
                                service?.getTileWithCache(
                                    tile.tileX,
                                    tile.tileY
                                )
                            }
                        }
                        val result = tileReq.await()?.awaitResponse()?.body()
                        // clean the tile, process the string, perform an update on db using the clean tile
                        val cleanedTile =
                            result?.let { cleanTileGeoJSON(tile.tileX, tile.tileY, 16.0, it) }

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

    private fun startRealm() {
        realm = RealmConfiguration.getInstance()
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
        // getCurrentDirection() from the direction provider has a default of 0.0
        // even if we don't have a valid current direction.
        val configLocale = AppCompatDelegate.getApplicationLocales()[0]
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        val localizedContext = applicationContext.createConfigurationContext(configuration)

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            //Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            audioEngine.createTextToSpeech(
                0.0,
                0.0,
                noLocationString
            )
        } else {
            // fetch the roads from Realm
            val tileGridQuadKeys = get3x3TileGrid(
                locationProvider.getCurrentLatitude() ?: 0.0,
                locationProvider.getCurrentLongitude() ?: 0.0
            )
            val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            val gridFeatureCollection = FeatureCollection()
            val processedOsmIds = mutableSetOf<Any>()

            for (tile in tileGridQuadKeys) {
                val frozenResult =
                    realm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
                if (frozenResult != null) {
                    val roadsString = frozenResult.roads

                    val roadsFeatureCollection = roadsString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }
                    for (feature in roadsFeatureCollection?.features!!) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedOsmIds.contains(osmId)) {
                            processedOsmIds.add(osmId)
                            gridFeatureCollection.features.add(feature)
                        }
                    }
                }
            }
            if (gridFeatureCollection.features.size > 0) {
                //Log.d(TAG, "Found roads in tile")
                val nearestRoad =
                    getNearestRoad(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        gridFeatureCollection
                    )
                val properties = nearestRoad.features[0].properties
                if (properties != null) {
                    val orientation = directionProvider.getCurrentDirection()
                    var roadName = properties["name"]
                    if (roadName == null) {
                        roadName = properties["highway"]
                    }
                    val facingDirectionAlongRoad = configLocale?.let {
                        getCompassLabelFacingDirectionAlong(
                            applicationContext,
                            orientation.toInt(),
                            roadName.toString(),
                            it
                        )
                    }
                    if (facingDirectionAlongRoad != null) {
                        audioEngine.createTextToSpeech(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            facingDirectionAlongRoad
                        )
                    }
                } else {
                    Log.e(TAG, "No properties found for road")
                }
            } else {
                //Log.d(TAG, "No roads found in tile just give device direction")
                val orientation = directionProvider.getCurrentDirection()
                val facingDirection = configLocale?.let {
                    getCompassLabelFacingDirection(
                        applicationContext,
                        orientation.toInt(),
                        it
                    )
                }
                if (facingDirection != null) {
                    audioEngine.createTextToSpeech(
                        locationProvider.getCurrentLatitude() ?: 0.0,
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        facingDirection
                    )
                }
            }
        }
    }

    fun whatsAroundMe() {
        // TODO This is just a rough POC at the moment. Lots more to do...
        //  decide on how to calculate distance to POI, setup settings in the menu so we can pass in the filters, etc.
        //  Original Soundscape just splats out a list in no particular order which is odd.
        //  If you press the button again in original Soundscape it can give you the same list but in a different sequence or
        //  it can add one to the list even if you haven't moved. It also only seems to give a thing and a distance but not a heading.
        val configLocale = AppCompatDelegate.getApplicationLocales()[0]
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        val localizedContext = applicationContext.createConfigurationContext(configuration)

        // super categories are "information", "object", "place", "landmark", "mobility", "safety"
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val placesAndLandmarks = sharedPrefs.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
        val mobility = sharedPrefs.getBoolean(MOBILITY_KEY, true)
        val unnamedRoads = sharedPrefs.getBoolean(UNNAMED_ROADS_KEY, false)

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            audioEngine.createTextToSpeech(
                0.0,
                0.0,
                noLocationString
            )
        } else {

            val tileGridQuadKeys = get3x3TileGrid(
                locationProvider.getCurrentLatitude() ?: 0.0,
                locationProvider.getCurrentLongitude() ?: 0.0
            )
            val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            val gridFeatureCollection = FeatureCollection()
            val processedOsmIds = mutableSetOf<Any>()

            for (tile in tileGridQuadKeys) {
                //Check the db for the tile
                val frozenTileResult =
                    realm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
                if (frozenTileResult != null) {
                    val poiString = frozenTileResult.pois
                    val poiFeatureCollection = poiString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }

                    for (feature in poiFeatureCollection?.features!!) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedOsmIds.contains(osmId)) {
                            processedOsmIds.add(osmId)
                            gridFeatureCollection.features.add(feature)
                        }
                    }
                }
            }

            if (gridFeatureCollection.features.size > 0) {

                val settingsFeatureCollection = FeatureCollection()
                if (placesAndLandmarks) {
                    if (mobility) {
                        //Log.d(TAG, "placesAndLandmarks and mobility are both true")
                        val placeSuperCategory =
                            getPoiFeatureCollectionBySuperCategory("place", gridFeatureCollection)
                        val tempFeatureCollection = FeatureCollection()
                        for (feature in placeSuperCategory.features) {
                            if (feature.foreign?.get("feature_value") != "house") {
                                if (feature.properties?.get("name") != null) {
                                    val superCategoryList = getSuperCategoryElements("place")
                                    for (property in feature.properties!!) {
                                        for (featureType in superCategoryList) {
                                            if (property.value == featureType) {
                                                tempFeatureCollection.features.add(feature)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val cleanedPlaceSuperCategory = removeDuplicateOsmIds(tempFeatureCollection)
                        for (feature in cleanedPlaceSuperCategory.features) {
                            settingsFeatureCollection.features.add(feature)
                        }

                        val landmarkSuperCategory =
                            getPoiFeatureCollectionBySuperCategory(
                                "landmark",
                                gridFeatureCollection
                            )
                        for (feature in landmarkSuperCategory.features) {
                            settingsFeatureCollection.features.add(feature)
                        }
                        val mobilitySuperCategory =
                            getPoiFeatureCollectionBySuperCategory(
                                "mobility",
                                gridFeatureCollection
                            )
                        for (feature in mobilitySuperCategory.features) {
                            settingsFeatureCollection.features.add(feature)
                        }

                    } else {

                        val placeSuperCategory =
                            getPoiFeatureCollectionBySuperCategory("place", gridFeatureCollection)
                        for (feature in placeSuperCategory.features) {
                            if (feature.foreign?.get("feature_type") != "building" && feature.foreign?.get(
                                    "feature_value"
                                ) != "house"
                            ) {
                                settingsFeatureCollection.features.add(feature)
                            }
                        }
                        val landmarkSuperCategory =
                            getPoiFeatureCollectionBySuperCategory(
                                "landmark",
                                gridFeatureCollection
                            )
                        for (feature in landmarkSuperCategory.features) {
                            settingsFeatureCollection.features.add(feature)
                        }
                    }
                } else {
                    if (mobility) {
                        //Log.d(TAG, "placesAndLandmarks is false and mobility is true")
                        val mobilitySuperCategory =
                            getPoiFeatureCollectionBySuperCategory(
                                "mobility",
                                gridFeatureCollection
                            )
                        for (feature in mobilitySuperCategory.features) {
                            settingsFeatureCollection.features.add(feature)
                        }
                    } else {
                        // Not sure what we are supposed to tell the user here?
                        println("placesAndLandmarks and mobility are both false so what should I tell the user?")
                    }
                }
                // Strings we can filter by which is from original Soundscape (we could more granular if we wanted to):
                // "information", "object", "place", "landmark", "mobility", "safety"
                if (settingsFeatureCollection.features.size > 0) {
                    for (feature in settingsFeatureCollection) {
                        if (feature.geometry is Polygon) {
                            if (feature.properties?.get("name") != null) {
                                audioEngine.createTextToSpeech(
                                    locationProvider.getCurrentLatitude() ?: 0.0,
                                    locationProvider.getCurrentLongitude() ?: 0.0,
                                    "${feature.properties?.get("name")} ${
                                        distanceToPolygon(
                                            LngLatAlt(
                                                locationProvider.getCurrentLongitude() ?: 0.0,
                                                locationProvider.getCurrentLatitude() ?: 0.0
                                            ),
                                            feature.geometry as Polygon
                                        ).toInt()
                                    } meters."
                                )
                            }
                        }
                    }
                } else {
                    audioEngine.createTextToSpeech(
                        locationProvider.getCurrentLatitude() ?: 0.0,
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                    )
                }
            } else {
                Log.d(TAG, "No Points Of Interest found in the grid")
                audioEngine.createTextToSpeech(
                    locationProvider.getCurrentLatitude() ?: 0.0,
                    locationProvider.getCurrentLongitude() ?: 0.0,
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                )
            }
        }
    }

    fun aheadOfMe() {
        // TODO This is just a rough POC at the moment. Lots more to do...
        val configLocale = AppCompatDelegate.getApplicationLocales()[0]
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        val localizedContext = applicationContext.createConfigurationContext(configuration)

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            //Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            audioEngine.createTextToSpeech(
                0.0,
                0.0,
                noLocationString
            )
        } else {
            // get device direction
            val orientation = directionProvider.getCurrentDirection()
            val fovDistance = 50.0
            // start of trying to get a grid of tiles and merge into one feature collection
            val tileGridQuadKeys = get3x3TileGrid(
                locationProvider.getCurrentLatitude() ?: 0.0,
                locationProvider.getCurrentLongitude() ?: 0.0
            )
            val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            val roadGridFeatureCollection = FeatureCollection()
            val intersectionsGridFeatureCollection = FeatureCollection()
            val processedRoadOsmIds = mutableSetOf<Any>()
            val processedIntersectionOsmIds = mutableSetOf<Any>()

            for (tile in tileGridQuadKeys) {
                //Check the db for the tile
                val frozenTileResult =
                    realm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
                if (frozenTileResult != null) {
                    val roadString = frozenTileResult.roads
                    val intersectionsString = frozenTileResult.intersections
                    val roadFeatureCollection = roadString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }
                    val intersectionsFeatureCollection = intersectionsString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }

                    for (feature in roadFeatureCollection?.features!!) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedRoadOsmIds.contains(osmId)) {
                            processedRoadOsmIds.add(osmId)
                            roadGridFeatureCollection.features.add(feature)
                        }
                    }
                    for (feature in intersectionsFeatureCollection?.features!!) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedIntersectionOsmIds.contains(osmId)) {
                            processedIntersectionOsmIds.add(osmId)
                            intersectionsGridFeatureCollection.features.add(feature)
                        }
                    }
                }
            }

            if (roadGridFeatureCollection.features.size > 0) {

                val fovRoadsFeatureCollection = roadGridFeatureCollection.let {
                    getFovRoadsFeatureCollection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        orientation.toDouble(),
                        fovDistance,
                        it
                    )
                }
                val fovIntersectionsFeatureCollection = intersectionsGridFeatureCollection.let {
                    getFovIntersectionFeatureCollection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        orientation.toDouble(),
                        fovDistance,
                        it
                    )
                }

                //TEMP This just returns the roads in the FOV.
                if (fovRoadsFeatureCollection.features.size > 0) {
                    val nearestRoad = getNearestRoad(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovRoadsFeatureCollection
                    )
                    // TODO check for Settings, Unnamed roads on/off here
                    if (nearestRoad.features[0].properties?.get("name") != null) {
                        audioEngine.createTextToSpeech(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            "${localizedContext.getString(R.string.directions_direction_ahead)} ${nearestRoad.features[0].properties!!["name"]}"
                        )
                    } else {
                        // we are detecting an unnamed road here but pretending there is nothing here
                        audioEngine.createTextToSpeech(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                        )
                    }


                    if (fovIntersectionsFeatureCollection.features.size > 0) {
                        val nearestIntersectionFeatureCollection = getNearestIntersection(
                            LngLatAlt(
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                locationProvider.getCurrentLatitude() ?: 0.0
                            ),
                            fovIntersectionsFeatureCollection
                        )
                        val distanceToNearestIntersection = distanceToIntersection(
                            LngLatAlt(
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                locationProvider.getCurrentLatitude() ?: 0.0
                            ),
                            nearestIntersectionFeatureCollection.features[0].geometry as Point
                        )
                        audioEngine.createTextToSpeech(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            "${localizedContext.getString(R.string.intersection_approaching_intersection)} It is ${distanceToNearestIntersection.toInt()} meters away."
                        )
                        // get the roads that make up the intersection based on the osm_ids
                        val nearestIntersectionRoadNames = getIntersectionRoadNames(
                            nearestIntersectionFeatureCollection,
                            fovRoadsFeatureCollection
                        )
                        val nearestRoadBearing = getRoadBearingToIntersection(
                            nearestIntersectionFeatureCollection,
                            nearestRoad,
                            orientation.toDouble()
                        )
                        val intersectionLocation =
                            nearestIntersectionFeatureCollection.features[0].geometry as Point
                        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
                            LngLatAlt(
                                intersectionLocation.coordinates.longitude,
                                intersectionLocation.coordinates.latitude
                            ),
                            nearestRoadBearing,
                            fovDistance,
                            RelativeDirections.COMBINED
                        )
                        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
                            nearestIntersectionRoadNames,
                            nearestIntersectionFeatureCollection,
                            intersectionRelativeDirections
                        )
                        for (feature in roadRelativeDirections.features) {
                            val direction =
                                feature.properties?.get("Direction").toString().toIntOrNull()
                            val relativeDirectionString = configLocale?.let {
                                getRelativeDirectionLabel(
                                    applicationContext,
                                    direction!!,
                                    it
                                )
                            }

                            if (feature.properties?.get("name") != null) {
                                val intersectionCallout = localizedContext.getString(
                                    R.string.directions_intersection_with_name_direction,
                                    feature.properties?.get("name"),
                                    relativeDirectionString
                                )
                                audioEngine.createTextToSpeech(
                                    locationProvider.getCurrentLatitude() ?: 0.0,
                                    locationProvider.getCurrentLongitude() ?: 0.0,
                                    intersectionCallout
                                )
                            }
                        }
                    }
                } else {
                    audioEngine.createTextToSpeech(
                        locationProvider.getCurrentLatitude() ?: 0.0,
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                    )
                }
            } else {
                audioEngine.createTextToSpeech(
                    locationProvider.getCurrentLatitude() ?: 0.0,
                    locationProvider.getCurrentLongitude() ?: 0.0,
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                )

            }
        }
    }


    companion object {
        private const val TAG = "SoundscapeService"

        // Secondary "service" every n seconds
        private val TICKER_PERIOD_SECONDS = 3600.seconds

        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60

        private const val CHANNEL_ID = "SoundscapeService_channel_01"
        private const val NOTIFICATION_CHANNEL_NAME = "Soundscape_SoundscapeService"
        private const val NOTIFICATION_ID = 100000
    }
}

