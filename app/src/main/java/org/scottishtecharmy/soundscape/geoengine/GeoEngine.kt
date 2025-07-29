package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.location.DeviceOrientation
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.phoneHeldFlat
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.utils.toLocationDescriptions
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlin.time.measureTime
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import java.lang.String.format


data class PositionedString(
    val text : String,
    val location : LngLatAlt? = null,
    val earcon : String? = null,
    val type: AudioType = AudioType.STANDARD,
    val heading: Double? = null
)

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class GeoEngine {
    private val coroutineScope = CoroutineScope(Job())

    // Location update job
    private var locationMonitoringJob: Job? = null
    private var audioEngineUpdateJob: Job? = null
    private var markerMonitoringJob: Job? = null

    val gridState = ProtomapsGridState()
    val settlementGrid = ProtomapsGridState(zoomLevel = 12, gridSize = 3, gridState.treeContext)

    internal lateinit var locationProvider : LocationProvider
    private lateinit var directionProvider : DirectionProvider
    private var mapMatchFilter = MapMatchFilter()

    // Resource string locale configuration
    private lateinit var configLocale: Locale
    private lateinit var configuration: Configuration
    lateinit var localizedContext: Context

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener

    // Flag to indicate that the app is running on screen
    var appInForeground = false

    private lateinit var autoCallout: AutoCallout
    private var autoCalloutDisabled = false
    fun toggleAutoCallouts() {
        autoCalloutDisabled.xor(true)
    }
    private val streetPreview = StreetPreview()

    var phoneHeldFlat = false
    var lastPhoneHeading : Double? = null

    var beaconLocation: LngLatAlt? = null
    fun updateBeaconLocation(location: LngLatAlt?) {
        beaconLocation = location
    }

    var ruler = CheapRuler(0.0)

    /**
     * Create a UserGeometry data object using the passed in location and orientation values
     * @param location The Android location to use
     * @param orientation The Android DeviceOrientation to use
     */
    private fun createUserGeometry(
        location: Location?,
        orientation: DeviceOrientation?,
        headingMode: UserGeometry.HeadingMode,
        mapMatchFilter: MapMatchFilter? = null,
    ) : UserGeometry {

        var latLng = LngLatAlt(0.0, 0.0)
        if(location != null) {
            latLng = LngLatAlt(location.longitude, location.latitude)
        }
        if(ruler.needsReplacing(latLng.latitude))
            ruler = CheapRuler(latLng.latitude)

        phoneHeldFlat = phoneHeldFlat(orientation)
        lastPhoneHeading = orientation?.headingDegrees?.toDouble()
        val phoneHeading =
            if (appInForeground or phoneHeldFlat)
                lastPhoneHeading
            else
                null

        // TODO: The travelHeading and speed calculations are important when trying
        //  to work out if the phone is moving or not. With these changes, there are
        //  still times when the phone is deemed to be moving, purely due to GPS
        //  jumps causing a "speed" and a "heading". It may be possible to Kalman
        // filter these, a better method is required.
        var travelHeading: Double? = null
        if(location?.bearing != null) {
            if(location.hasBearingAccuracy()) {
                if(location.bearingAccuracyDegrees < 45.0)
                    travelHeading = location.bearing.toDouble()
            } else {
                travelHeading = location.bearing.toDouble()
            }
        }

        var speed = 0.0
        if(location?.speed != null) {
            if(location.hasSpeedAccuracy()) {
                // If the accuracy range encompasses zero, then we can't use it.
                val lowestSpeed = location.speed - location.speedAccuracyMetersPerSecond
                if(lowestSpeed > 0.1) {
                    speed = location.speed.toDouble()
                }
            } else {
                // No accuracy available
                speed = location.speed.toDouble()
            }
        }

        return UserGeometry(
            location = latLng,
            phoneHeading = phoneHeading,
            fovDistance = 50.0,
            speed = speed,
            headingMode = headingMode,
            ruler = ruler,
            travelHeading = travelHeading,
            mapMatchedWay = mapMatchFilter?.matchedWay,
            mapMatchedLocation = mapMatchFilter?.matchedLocation,
            currentBeacon = beaconLocation,
            inStreetPreview = streetPreview.running,
            timestampMilliseconds = System.currentTimeMillis()
        )
    }

    private fun getCurrentUserGeometry(
        headingMode: UserGeometry.HeadingMode
    ) : UserGeometry {
        return createUserGeometry(
            location = locationProvider.locationFlow.value,
            orientation = directionProvider.orientationFlow.value,
            headingMode = headingMode,
            mapMatchFilter = mapMatchFilter
        )
    }

    fun updateRecordingState(recordState: Boolean) {
        gpxRecorder = if(recordState) {
            GpxRecorder()
        } else {
            null
        }
    }

    fun getRecordingShareUri(context: Context) : Uri? {
        val ret = gpxRecorder?.getShareUri(context)
        return ret
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(
        application: Application,
        newLocationProvider: LocationProvider,
        newDirectionProvider: DirectionProvider,
        soundscapeService: SoundscapeService,
    ) {
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
        recordTravel = sharedPreferences.getBoolean(MainActivity.RECORD_TRAVEL_KEY, false)
        updateRecordingState(recordTravel)

        sharedPreferencesListener =
            SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                if (sharedPreferences == preferences) {
                    if(key == MainActivity.RECORD_TRAVEL_KEY) {
                        Log.e(TAG, "RECORD_TRAVEL_KEY changed")
                        recordTravel = sharedPreferences.getBoolean(MainActivity.RECORD_TRAVEL_KEY, false)
                        updateRecordingState(recordTravel)
                    }
                }
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        val recordingsStoragePath = application.applicationContext.filesDir.toString() + "/recordings/"
        val recordingsStorageDir = File(recordingsStoragePath)
        if (!recordingsStorageDir.exists()) {
            recordingsStorageDir.mkdirs()
        }

        gridState.start(application)
        settlementGrid.start(application)

        configLocale = getCurrentLocale()
        configuration = Configuration(application.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = application.applicationContext.createConfigurationContext(configuration)
        autoCallout = AutoCallout(localizedContext, sharedPreferences)


        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        startMonitoringLocation(soundscapeService)

        streetPreview.start()

        // Monitor markers in the database so we can create a tree to search
        markerMonitoringJob?.cancel()
        markerMonitoringJob = coroutineScope.launch {

            val realm = MarkersAndRoutesDatabase.getMarkersInstance(application.applicationContext)
            val routeDao = realm.routeDao()
            routeDao.getAllMarkersFlow().collect { markers ->

                val featureCollection = FeatureCollection()
                for (marker in markers) {
                    val geoFeature = Feature()
                    geoFeature.geometry =
                        Point(marker.longitude, marker.latitude)
                    val properties : HashMap<String, Any?> = hashMapOf()
                    properties["name"] = marker.name
                    properties["description"] = marker.fullAddress
                    geoFeature.properties = properties
                    val foreign : HashMap<String, Any?> = hashMapOf()
                    foreign["category"] ="marker"
                    geoFeature.foreign = foreign
                    featureCollection.addFeature(geoFeature)
                }
                runBlocking {
                    withContext(gridState.treeContext) {
                        gridState.markerTree = FeatureTree(featureCollection)
                        Log.e(TAG, "Marker tree size ${featureCollection.features.size}")
                    }
                }
            }
        }
    }

    fun stop() {
        streetPreview.stop()

        locationMonitoringJob?.cancel()
        audioEngineUpdateJob?.cancel()
        markerMonitoringJob?.cancel()

        settlementGrid.stop()
        gridState.stop()
        locationProvider.destroy()
        directionProvider.destroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
   }
    private var recordTravel = false
    private var gpxRecorder: GpxRecorder? = null

    /**
     * The gridState is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    fun createSuperCategoriesSet() : Set<String> {
        val enabledCategories = emptySet<String>().toMutableSet()
        if (sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true))
            enabledCategories.add(PLACES_AND_LANDMARKS_KEY)

        if (sharedPreferences.getBoolean(MOBILITY_KEY, true))
            enabledCategories.add(MOBILITY_KEY)

        return enabledCategories
    }

    private fun updateAudioEngineGeometry(
        soundscapeService: SoundscapeService,
        userGeometry: UserGeometry
    ) {
        // Send the update to the audio engine. This affects the direction and sound
        // of the audio beacon.
        soundscapeService.audioEngine.updateGeometry(
            userGeometry.location.latitude,
            userGeometry.location.longitude,
            userGeometry.presentationHeading(),
            soundscapeService.audioFocusGained,
            soundscapeService.duckingAllowed
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoringLocation(soundscapeService: SoundscapeService) {
        Log.e(TAG, "startTileGridService")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            locationProvider.locationFlow.collect { newLocation ->

                newLocation?.let { location ->

                    // Add location to crash dumps
                    FirebaseCrashlytics.getInstance().setCustomKey("latitude", newLocation.latitude)
                    FirebaseCrashlytics.getInstance().setCustomKey("longitude", newLocation.longitude)

                    Firebase.analytics.logEvent("gridUpdate", null)

                    // Update the main grid state
                    val updated = gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        createSuperCategoriesSet()
                    )
                    // and update the settlement grid state
                    settlementGrid.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        createSuperCategoriesSet()
                    )

                    runBlocking {
                        withContext(gridState.treeContext) {
                            // Update the nearest road filter with our new location
                            val mapMatchTime = measureTime {
                                mapMatchFilter.filter(
                                    LngLatAlt(location.longitude, location.latitude),
                                    gridState,
                                    FeatureCollection(),
                                    false
                                )
                            }
                            val matchedWay = mapMatchFilter.matchedWay
                            println("MapMatch: $mapMatchTime to get ${matchedWay?.getName()}")
                        }
                    }

                    if(updated) {
                        // The grid updated, if we're in StreetPreview and were initializing, the
                        // service needs to update the state to ON.
                        soundscapeService.tileGridUpdated()
                    }

                    // So long as the AudioEngine is not already busy, run any auto callouts that we
                    // need. Auto Callouts use the direction of travel if there is one, otherwise
                    // falling back to use the phone direction.
                    if(!soundscapeService.isAudioEngineBusy() && !autoCalloutDisabled) {
                        val callouts =
                            autoCallout.updateLocation(
                                getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
                                gridState,
                                settlementGrid)
                        if (callouts != null) {
                            // Tell the service that we've got some callouts to tell the user about
                            soundscapeService.speakCallout(callouts, false)
                        }

                        // Save the location data to a file if enabled
                        gpxRecorder?.storeLocation(location)
                    }
                }
            }
        }

        // This job collects the incoming orientation and location flows, combines them into a
        // UserGeometry and uses it to update the audio engine. If no flow change arrives within
        // 100ms, then it updates the audio engine with the last values received.
        audioEngineUpdateJob?.cancel()
        audioEngineUpdateJob = coroutineScope.launch {
            var lastGeometry : UserGeometry? = null
            while(true) {
                val geometry = withTimeoutOrNull(100) {
                    combine(
                        directionProvider.orientationFlow,
                        locationProvider.locationFlow,

                        ) { orientation: DeviceOrientation?, location: Location? ->

                            createUserGeometry(
                                location = location,
                                orientation = orientation,
                                headingMode = UserGeometry.HeadingMode.CourseAuto
                            )

                    }.collect { geometry ->
                        lastGeometry = geometry
                        updateAudioEngineGeometry(soundscapeService, geometry)
                    }
                }
                if(geometry == null) {
                    // Timeout
                    lastGeometry?.let { last ->
                        // We may need to overwrite the phone heading if the phone has been locked
                        // or unlocked but with no heading update.
                        if (appInForeground or phoneHeldFlat)
                            last.phoneHeading = lastPhoneHeading
                        else
                            last.phoneHeading = null

                        updateAudioEngineGeometry(soundscapeService, last) }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun myLocation() : TrackedCallout? {

        Firebase.analytics.logEvent("myLocation", null)

        // getCurrentDirection() from the direction provider has a default of 0.0
        // even if we don't have a valid current direction.
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto)
        var results : MutableList<PositionedString> = mutableListOf()
        if (!locationProvider.hasValidLocation()) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(
                text = noLocationString,
                type = AudioType.STANDARD)
            )
        } else {
            // Check if we have a valid heading
            val orientation = userGeometry.heading()
            orientation?.let { heading ->
                // Run the code within the treeContext to protect it from changes to the trees whilst it's
                // running.
                results = runBlocking {
                    withContext(gridState.treeContext) {

                        val list: MutableList<PositionedString> = mutableListOf()

                        val nearestRoad = userGeometry.mapMatchedWay
                        if (nearestRoad != null) {
                            val roadName = nearestRoad.getName(null, gridState, localizedContext)
                            val facingDirectionAlongRoad =
                                getCompassLabelFacingDirectionAlong(
                                    localizedContext,
                                    heading.toInt(),
                                    roadName,
                                    userGeometry.inMotion(),
                                    userGeometry.inVehicle()
                                )
                            list.add(PositionedString(
                                text = facingDirectionAlongRoad,
                                type = AudioType.STANDARD))
                        } else {
                            val facingDirection =
                                getCompassLabelFacingDirection(
                                    localizedContext,
                                    heading.toInt(),
                                    userGeometry.inMotion(),
                                    userGeometry.inVehicle()
                                )
                            list.add(PositionedString(
                                text = facingDirection,
                                type = AudioType.STANDARD)
                            )
                        }
                        list
                    }
                }
            }
        }
        if(results.isEmpty())
            return null

        return TrackedCallout(
            userGeometry = userGeometry,
            filter = false,
            positionedStrings = results
        )
    }

    suspend fun searchResult(searchString: String) =
        withContext(Dispatchers.IO) {
            val location = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto).location
            try {
                Firebase.analytics.logEvent("geoSearch", null)
                return@withContext PhotonSearchProvider
                    .getInstance()
                    .getSearchResults(
                        searchString = searchString,
                        latitude = location.latitude,
                        longitude = location.longitude,
                    ).execute()
                    .body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting search results:", e)
                Firebase.analytics.logEvent("geoSearchError", null)
                return@withContext null
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun whatsAroundMe() : TrackedCallout? {
        // Duplicate original Soundscape behaviour:
        //   In findCalloutsFor it tries to get a POI in each quadrant. It starts off searching
        //   within 200m and keeps increasing by 200m until it hits the maximum of 1000m. It only
        //   plays out a single POI per quadrant.
        var results : MutableList<PositionedString> = mutableListOf()
        val timeSource = TimeSource.Monotonic
        val gridStartTime = timeSource.markNow()
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto)

        Firebase.analytics.logEvent("whatsAroundMe", null)

        if (!locationProvider.hasValidLocation()) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(
                text = noLocationString,
                type = AudioType.STANDARD)
            )
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {

                    // Direction order is: behind(0) left(1) ahead(2) right(3)
                    val featuresByDirection: Array<Feature?> = arrayOfNulls(4)
                    val directionsNeeded = setOf(0, 1, 2, 3).toMutableSet()

                    // We want to use places and landmarks only
                    // We already have a FeatureTree containing the POI that we wish to search on
                    val featureTree = gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)
                    for (distance in 200..1000 step 200) {

                        // Get Polygons for this FOV distance
                        val individualRelativePolygons = getRelativeDirectionsPolygons(
                            UserGeometry(
                                userGeometry.location,
                                userGeometry.heading(),
                                distance.toDouble()
                            ), RelativeDirections.INDIVIDUAL
                        )

                        val direction = directionsNeeded.iterator()
                        while (direction.hasNext()) {

                            val dir = direction.next()
                            val triangle = getTriangleForDirection(individualRelativePolygons, dir)
                            // Get the 4 nearest features in this direction. This allows us to de-duplicate
                            // across the other directions.
                            val featureCollection = featureTree.getNearestCollectionWithinTriangle(triangle, 4, userGeometry.ruler)
                            if (featureCollection.features.isNotEmpty()) {
                                // We found features in this direction, find the nearest one which
                                // we are not already calling out in another direction.
                                for(feature in featureCollection) {
                                    var duplicate = false
                                    val featureName = getTextForFeature(localizedContext, feature).text
                                    for (otherFeature in featuresByDirection) {
                                        if (otherFeature == null) continue
                                        val otherName = getTextForFeature(localizedContext, otherFeature).text
                                        if (featureName == otherName) duplicate = true
                                    }
                                    if (!duplicate) {
                                        // We've found a new feature, remember it and remove it from
                                        // the set of directions to search
                                        featuresByDirection[dir] = feature
                                        direction.remove()
                                        break
                                    }
                                }
                            }
                        }
                        // We've found all the directions, so no need to look any further afield
                        if (directionsNeeded.isEmpty()) break
                    }

                    // We've tried to get a POI in all directions, now return the ones we have into
                    // callouts
                    val list: MutableList<PositionedString> = mutableListOf()
                    for (feature in featuresByDirection) {

                        if(feature == null) continue
                        val poiLocation = getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                        val name = getTextForFeature(localizedContext, feature)
                        val text = "${name.text}. ${formatDistance(poiLocation.distance, localizedContext)}"
                        list.add(
                            PositionedString(
                                text,
                                poiLocation.point,
                                NativeAudioEngine.EARCON_SENSE_POI,
                                AudioType.LOCALIZED,
                            )
                        )
                    }
                    list
                }
            }
        }
        val gridFinishTime = timeSource.markNow()
        Log.e(GridState.TAG, "Time to calculate AroundMe: ${gridFinishTime - gridStartTime}")

        return TrackedCallout(
            userGeometry = userGeometry,
            filter = false,
            positionedStrings = results
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun aheadOfMe() : TrackedCallout? {
        var results : MutableList<PositionedString> = mutableListOf()
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.HeadAuto)

        Firebase.analytics.logEvent("aheadOfMe", null)

        if (!locationProvider.hasValidLocation()) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(
                text = noLocationString,
                type = AudioType.STANDARD)
            )
        } else {
            results = runBlocking {
                withContext(gridState.treeContext) {

                    // Return the nearest 5 POI within 1000m in the direction that we are heading
                    userGeometry.fovDistance = 1000.0
                    val triangle = getFovTriangle(userGeometry)
                    val featureTree = gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)

                    val featuresAhead = featureTree.getNearestCollectionWithinTriangle(triangle, 5, userGeometry.ruler)
                    val list: MutableList<PositionedString> = mutableListOf()
                    for (feature in featuresAhead) {

                        val poiLocation = getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                        val name = getTextForFeature(localizedContext, feature)
                        val text = "${name.text}. ${formatDistance(poiLocation.distance, localizedContext)}"
                        list.add(
                            PositionedString(
                                text,
                                poiLocation.point,
                                NativeAudioEngine.EARCON_SENSE_POI,
                                AudioType.LOCALIZED,
                            )
                        )
                    }
                    if(list.isEmpty()) {
                        list.add(
                            PositionedString(
                                text = localizedContext.getString(R.string.callouts_nothing_to_call_out_now),
                                type = AudioType.STANDARD
                            )
                        )
                    }
                    list
                }
            }
        }
        if(results.isEmpty())
            return null

        return TrackedCallout(
            userGeometry = userGeometry,
            filter = false,
            positionedStrings = results
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun nearbyMarkers() : TrackedCallout? {

        Firebase.analytics.logEvent("nearbyMarkers", null)

        // Search database for nearby markers and call them out
        var results : MutableList<PositionedString> = mutableListOf()
        val timeSource = TimeSource.Monotonic
        val gridStartTime = timeSource.markNow()
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto)

        if (!locationProvider.hasValidLocation()) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(
                text = noLocationString,
                type = AudioType.STANDARD)
            )
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {

                    // Simply get 4 nearest markers
                    val nearestMarkers = gridState.markerTree?.getNearestCollection(
                        userGeometry.location,
                        2000.0,
                        4,
                        userGeometry.ruler
                    )

                    val list: MutableList<PositionedString> = mutableListOf()
                    if(nearestMarkers != null) {
                        for (feature in nearestMarkers.features) {
                            val markerLocation = getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                            val name = feature.properties?.get("name")
                            val text = "$name. ${
                                formatDistance(
                                    markerLocation.distance,
                                    localizedContext
                                )
                            }"
                            list.add(
                                PositionedString(
                                    text,
                                    markerLocation.point,
                                    NativeAudioEngine.EARCON_SENSE_POI,
                                    AudioType.LOCALIZED,
                                )
                            )
                        }
                    }
                    list
                }
            }
        }
        val gridFinishTime = timeSource.markNow()
        Log.e(GridState.TAG, "Time to calculate NearbyMarkers: ${gridFinishTime - gridStartTime}")

        if(results.isEmpty())
            return null

        return TrackedCallout(
            userGeometry = userGeometry,
            filter = false,
            positionedStrings = results
        )
    }

    fun streetPreviewGo() : List<StreetPreviewChoice> {
        return streetPreviewGoInternal()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoWander() {

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val engine = this
        CoroutineScope(Job()).launch(gridState.treeContext) {
            // Get our current location and figure out what GO means
            val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
            val choices = streetPreview.getDirectionChoices(engine, userGeometry.location)
            var heading = 0.0
            if(choices.isNotEmpty()) {
                // We want to choose a direction roughly in the forward direction if possible. We
                // need to know which road we're coming in on.
                val lastHeading = streetPreview.getLastHeading()
                // Default to a random road
                heading = choices.random().heading
                if(!lastHeading.isNaN()) {
                    // If we came in on a road, then try and keep going in that direction
                    val trimmedChoices = mutableListOf<StreetPreviewChoice>()
                    for (choice in choices) {
                        if ((choice.heading != lastHeading) && (!choice.heading.isNaN())) {
                            // Don't add the road we just came in on, or any with a NaN heading.
                            trimmedChoices.add(choice)
                            // We want to skew results in favour of continuing onwards so add multiple
                            // times if the choice is in a direction that's fairly opposite to the road
                            // we're currently on.
                            if(abs(choice.heading - lastHeading) > 140.0) {
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                            }
                        }
                    }
                    if(trimmedChoices.isNotEmpty()) {
                        heading = trimmedChoices.random().heading
                    }
                }
            }
            // Update the heading with the random one that was chosen
            userGeometry.phoneHeading = heading
            streetPreview.go(userGeometry, engine)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoInternal() : List<StreetPreviewChoice> {
        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.  We want to return the new set of choices so that these can be sent up to the UI.
        val engine = this
        val results = runBlocking {
            withContext(gridState.treeContext) {
                // Get our current location and figure out what GO means
                val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
                val newLocation = streetPreview.go(userGeometry, engine)
                if(newLocation != null) {
                    streetPreview.getDirectionChoices(engine, newLocation)
                } else {
                    streetPreview.getDirectionChoices(engine, userGeometry.location)
                }
            }
        }
        return results
    }

    private suspend fun reverseGeocodeResult(location: LngLatAlt) =
        withContext(Dispatchers.IO) {
            try {
                return@withContext PhotonSearchProvider
                    .getInstance()
                    .reverseGeocodeLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).execute()
                    .body()
            } catch(e: Exception) {
                Log.e(TAG, "Error getting reverse geocode result:", e)
                return@withContext null
            }
        }

    /**
     * getLocationDescription returns a LocationDescription object for the current location. This
     * is basically a reverse geocode. It initially tries to generate it from local tile data, but
     * falls back to geocoding via the Photon server if network is available.
     * @param location to reverse geocode
     * @param preserveLocation ensures that the returned LocationDescription contains the passed in
     * location rather than overwriting it with the location of a POI that it geocoded to.
     * @return a LocationDescription object containing an address of the location
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLocationDescription(location: LngLatAlt,
                               preserveLocation: Boolean = true) : LocationDescription? {

        var geocode: LocationDescription?
        // If the location is within our current TileGrid, then we can make our own description of
        // the location.
        geocode = runBlocking {
            withContext(gridState.treeContext) {
                localReverseGeocode(location, gridState, settlementGrid, localizedContext)
            }
        }
        if(geocode != null) return geocode

        // If we have network, then we should be able to do a reverse geocode via the photon server
        // TODO: Check for network first
        // TODO: The geocode result takes too long to have it done inline like this. We need to
        //  move it into the user of the LocationDescription so that it can update dynamically if
        //  and when the request succeeds. Disable for now.
        if(false) {
            geocode = runBlocking {
                withContext(Dispatchers.IO) {
                    val result = reverseGeocodeResult(location)

                    // The geocode result includes the location for the POI. In the case of something
                    // like a park this could be a long way from the point that was passed in.
                    val ld = result?.features?.toLocationDescriptions()
                    if (!ld.isNullOrEmpty()) {
                        if(preserveLocation) {
                            val overwritten = ld.first()
                            overwritten.location = location
                            if(overwritten.name.isNotEmpty()) {
                                overwritten.name = localizedContext.getString(R.string.directions_near_name).format(overwritten.name)
                                overwritten
                            }
                            else {
                                null
                            }
                        } else {
                            ld.first()
                        }
                    } else
                        null
                }
            }
            if (geocode != null)
                return geocode
        }

// TODO: If we don't have network, and the location is outside of our current TileGrid, then we could see
//  if the tiles are cached and we can create a temporary TileGrid, but this needs some debugging
//
//        // Rustle up a TileGrid for this location
//        val tempGrid = if(SOUNDSCAPE_TILE_BACKEND) SoundscapeBackendGridState() else ProtomapsGridState()
//        geocode = runBlocking {
//            withContext(tempGrid.treeContext) {
//                // TODO: Should we create our own tileClient - we'll need an application context
//                tempGrid.tileClient = gridState.tileClient
//                tempGrid.locationUpdate(location, createSuperCategoriesSet())
//                localReverseGeocode(location, tempGrid, localizedContext)
//            }
//        }
//        if(geocode != null) return geocode

        return LocationDescription(
            name = "New location",
            location = location
        )
    }

    companion object {
        private const val TAG = "GeoEngine"
    }
}

data class TextForFeature(val text: String = "", val generic: Boolean= false)

/**
 * getNameForFeature returns text describing the feature for callouts. Usually it returns a name
 * or if it doesn't have one then a localized description of the type of feature it is e.g. bike
 * parking, or style. Some types of Feature have more info e.g. bus stops and railway stations
 * @param feature to evaluate
 * @return a NameForFeature object containing the name and a flag indicating if it is a generic
 * name from the OSM tag rather than an actual name.
 */
fun getTextForFeature(localizedContext: Context?, feature: Feature) : TextForFeature {
    var generic = false
    val name = feature.properties?.get("name") as String?
    val featureValue = feature.foreign?.get("feature_value")
    val isMarker = feature.foreign?.get("category") == "marker"

    if(localizedContext == null) {
        if(name == null) {
            val osmClass = feature.properties?.get("class") as String?
            return TextForFeature(osmClass ?: "", true)
        }

        return TextForFeature(name, false)
    }

    if(isMarker) {
        // If the feature is a Marker, return the unadulterated name along with prefix indicating
        // that it's a Marker.
        return if(name != null)
                TextForFeature(localizedContext.getString(R.string.markers_marker_with_name, name), false)
            else
                TextForFeature(localizedContext.getString(R.string.markers_generic_name), false)
    }

    var text = name

    val namedTransit = when (featureValue) {
        "bus_stop" -> Pair(R.string.osm_tag_bus_stop_named, R.string.osm_tag_bus_stop)
        "station" -> Pair(R.string.osm_tag_train_station_named, R.string.osm_tag_train_station)
        "tram_stop" -> Pair(R.string.osm_tag_tram_stop_named, R.string.osm_tag_tram_stop)
        "subway" -> Pair(R.string.osm_tag_subway_named, R.string.osm_tag_subway)
        "ferry_terminal" -> Pair(R.string.osm_tag_ferry_terminal_named, R.string.osm_tag_ferry_terminal)
        else -> null
    }
    if(namedTransit != null) {
        text = if (name != null)
            localizedContext.getString(namedTransit.first, name)
        else
            localizedContext.getString(namedTransit.second)
    }

    if (text == null) {
        val osmClass =
            feature.properties?.get("class") as String? ?: return TextForFeature("", true)

        val id = ResourceMapper.getResourceId(osmClass)
        text = if (id == null) {
            osmClass.replace("_", " ")
        } else {
            localizedContext.getString(id)
        }
        generic = true
    }
    val capitalizedText = text.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase(localizedContext.resources.configuration.getLocales().get(0))
        else
            it.toString()
    }

    return TextForFeature(capitalizedText, generic)
}

/**
 * We're going to round metric as documented for iOS:
 *  For metric units, we round all distances less than 1000 meters to the nearest 5 meters and all
 *  distances over 1000 meters to the nearest 50 meters.
 *
 * The iOS imperial docs are wrong, and in fact distances are all in feet and we can round in the
 * same way as metric.
 *
 */

fun formatDistance(distance: Double, localizedContext: Context?) : String {
    // TODO - Add setting for imperial/metric
    val metric = true
    var units = distance
    var bigUnitDivisor = 100
    if(!metric) {
        // Imperial units used are feet
        units = (distance * 1.09361 * 3)
        bigUnitDivisor = (176*3)
    }

    val roundToNearest = if (units < 1000) 5.0 else 50.0
    val roundedDistance =
        ((units + (roundToNearest / 2)) / roundToNearest).toInt() * roundToNearest

    if (roundedDistance < 1000) {
        return localizedContext?.getString(
            if(metric) R.string.distance_format_meters else R.string.distance_format_feet,
            roundedDistance.toInt().toString()
        ) ?: format("%f metres", roundedDistance)
    } else {
        val bigUnits = (roundedDistance.toInt() / 10).toFloat() / bigUnitDivisor
        return localizedContext?.getString(
            if(metric) R.string.distance_format_km else R.string.distance_format_miles,
            "%.2f".format(bigUnits)
        )  ?: format("%f km", bigUnits)
    }
}

fun localReverseGeocode(location: LngLatAlt,
                        gridState: GridState,
                        settlementGrid: GridState,
                        localizedContext: Context?): LocationDescription? {

    if(!gridState.isLocationWithinGrid(location)) return null

    // Check if we're near a bus/tram/train stop. This is useful when travelling on public transport
    val busStopTree = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
    val nearestBusStop = busStopTree.getNearestFeature(location, gridState.ruler, 20.0)
    if(nearestBusStop != null) {
        val busStopText = getTextForFeature(localizedContext, nearestBusStop)
        if(!busStopText.generic) {
            return LocationDescription(
                name = localizedContext?.getString(R.string.directions_near_name)
                    ?.format(busStopText.text) ?: "Near ${busStopText.text}",
                location = location,
            )
        }
    }

    // Check if we're inside a POI
    val gridPoiTree = gridState.getFeatureTree(TreeId.POIS)
    val insidePois = gridPoiTree.getContainingPolygons(location)
    for(poi in insidePois) {
        val name = poi.properties?.get("name")
        if(name != null) {
            return LocationDescription(
                name = localizedContext?.getString(R.string.directions_at_poi)?.format(name as String) ?: "At $name",
                location = location,
            )
        }
    }

    // Get the nearest settlements. Nominatim uses the following proximities, so we do the same:
    //
    // cities, municipalities, islands | 15 km
    // towns, boroughs                 | 4 km
    // villages, suburbs               | 2 km
    // hamlets, farms, neighbourhoods  |  1 km
    //
    var nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET).getNearestFeature(location, settlementGrid.ruler, 1000.0)
    var nearestSettlementName = nearestSettlement?.properties?.get("name")
    if(nearestSettlementName == null) {
        nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE).getNearestFeature(location, settlementGrid.ruler, 2000.0)
        nearestSettlementName = nearestSettlement?.properties?.get("name")
        if(nearestSettlementName == null) {
            nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_TOWN)
                .getNearestFeature(location, settlementGrid.ruler, 4000.0)
            nearestSettlementName = nearestSettlement?.properties?.get("name")
            if (nearestSettlementName == null) {
                nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_CITY)
                    .getNearestFeature(location, settlementGrid.ruler, 15000.0)
                nearestSettlementName = nearestSettlement?.properties?.get("name")
            }
        }
    }

    // Check if the location is alongside a road/path
    val nearestRoad = gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, gridState.ruler, location, 100.0) as Way?
    if(nearestRoad != null) {
        // We only want 'interesting' non-generic names i.e. no "Path" or "Service"
        val roadName = nearestRoad.getName(null, gridState, localizedContext, true)
        if(roadName.isNotEmpty()) {
            if(nearestSettlementName != null) {
                return LocationDescription(
                    name = localizedContext?.getString(R.string.directions_near_road_and_settlement)
                    ?.format(roadName, nearestSettlementName) ?: "Near $roadName and close to $nearestSettlementName",
                    location = location,
                )
            } else {
                return LocationDescription(
                    name = localizedContext?.getString(R.string.directions_near_name)
                        ?.format(roadName) ?: "Near $roadName",
                    location = location,
                )
            }
        }
    }

    if(nearestSettlementName != null) {
        //val distanceToSettlement = settlementGrid.ruler.distance(location, (nearestSettlement?.geometry as Point).coordinates)
        return LocationDescription(
            name = localizedContext?.getString(R.string.directions_near_name)
                ?.format(nearestSettlementName) ?: "Near $nearestSettlementName",
            location = location,
        )
    }


    return null
}

/** Reverse geocodes a location into 1 of 4 possible states
 * - within a POI
 * - alongside a road
 * - general location
 * - unknown location.
 */
fun reverseGeocode(userGeometry: UserGeometry,
                   gridState: GridState,
                   settlementGrid: GridState,
                   localizedContext: Context?): PositionedString? {

    val location = localReverseGeocode(userGeometry.location, gridState, settlementGrid, localizedContext)
    location?.let { l ->
        return PositionedString(
            text = l.name,
            location = userGeometry.location,
            type = AudioType.LOCALIZED)
    }
    // We don't want to call out "Unknown place", so return null and skip this callout
    return null
}
