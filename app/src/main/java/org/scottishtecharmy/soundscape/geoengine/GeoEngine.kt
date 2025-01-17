package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.location.Location
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.squareup.otto.Subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.ALLOW_CALLOUTS_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.ComplexIntersectionApproach
import org.scottishtecharmy.soundscape.geoengine.callouts.addIntersectionCalloutFromDescription
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getFeatureNearestPoint
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.findFeaturesInPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.services.getOttoBus
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

data class PositionedString(val text : String, val location : LngLatAlt? = null, val earcon : String? = null)

class GeoEngine {
    private val coroutineScope = CoroutineScope(Job())

    // Location update job
    private var locationMonitoringJob: Job? = null

    val gridState = if(SOUNDSCAPE_TILE_BACKEND) SoundscapeBackendGridState() else ProtomapsGridState()

    internal lateinit var locationProvider : LocationProvider
    private lateinit var directionProvider : DirectionProvider

    // Resource string locale configuration
    private lateinit var configLocale: Locale
    private lateinit var configuration: Configuration
    private lateinit var localizedContext: Context

    private lateinit var sharedPreferences: SharedPreferences

    private var inVehicle = false
    private var inMotion = false

    private val streetPreview = StreetPreview()

    /** UserGeometry contains all of the data relating to the location and motion of the user. It's
     * aim is simply to reduces the number of arguments to many of the API calls.
     */
    data class UserGeometry(val location: LngLatAlt = LngLatAlt(),
                            var heading: Double = 0.0,
                            val fovDistance: Double = 50.0,
                            val inVehicle: Boolean = false,
                            val inMotion: Boolean = false)
    private fun getCurrentUserGeometry() : UserGeometry {
        return UserGeometry(locationProvider.get(),
            directionProvider.getCurrentDirection().toDouble(),
            50.0,
            inVehicle,
            inMotion)
    }

    @Subscribe
    fun onActivityTransitionEvent(event: ActivityTransitionEvent) {
        if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            inVehicle =
                when (event.activityType) {
                    DetectedActivity.ON_BICYCLE,
                    DetectedActivity.IN_VEHICLE,
                    -> true

                    else -> false
                }
            inMotion = (event.activityType != DetectedActivity.STILL)
        }
    }

    fun start(
        application: Application,
        newLocationProvider: LocationProvider,
        newDirectionProvider: DirectionProvider,
        soundscapeService: SoundscapeService,
    ) {
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application.applicationContext)

        gridState.start(application, soundscapeService)

        configLocale = getCurrentLocale()
        configuration = Configuration(application.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = application.applicationContext.createConfigurationContext(configuration)

        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        startMonitoringLocation(soundscapeService)

        streetPreview.start()

        getOttoBus().register(this)
    }

    fun stop() {
        getOttoBus().unregister(this)

        locationMonitoringJob?.cancel()

        gridState.stop()
        locationProvider.destroy()
        directionProvider.destroy()
    }

    /**
     * The gridState is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    private fun startMonitoringLocation(soundscapeService: SoundscapeService) {
        Log.e(TAG, "startTileGridService")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            locationProvider.locationFlow.collect { newLocation ->

                newLocation?.let { location ->

                    // Update the grid state
                    gridState.locationUpdate(LngLatAlt(location.longitude, location.latitude))

                    // Run any auto callouts that we need
                    val callouts = autoCallout(location)
                    if (callouts.isNotEmpty()) {
                        // Tell the service that we've got some callouts to tell the user about
                        soundscapeService.speakCallout(callouts)
                    }
                }
            }
        }
    }

    private val locationFilter = LocationUpdateFilter(10000, 50.0)
    private val poiFilter = LocationUpdateFilter(5000, 5.0)

    /** Reverse geocodes a location into 1 of 4 possible states
     * - within a POI
     * - alongside a road
     * - general location
     * - unknown location).
     */
    private fun reverseGeocode(location: LngLatAlt): List<PositionedString> {
        val results : MutableList<PositionedString> = mutableListOf()

        // Check if we're inside a POI
        val gridPoiCollection = gridState.getFeatureCollection(TreeId.POIS, location, 200.0)
        val gridPoiFeatureCollection = removeDuplicateOsmIds(gridPoiCollection)
        for(poi in gridPoiFeatureCollection) {
            // We can only be inside polygons
            if(poi.geometry.type == "Polygon") {
                val polygon = poi.geometry as Polygon

                if(polygonContainsCoordinates(location, polygon)) {
                    // We've found a POI that contains our location
                    val name = poi.properties?.get("name")
                    if(name != null) {
                        results.add(
                            PositionedString(
                                localizedContext.getString(R.string.directions_at_poi).format(name as String)
                            ),
                        )
                        return results
                    }
                }
            }
        }

        // Check if we're alongside a road/path
        val nearestRoad = gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, location, 100.0)
        if(nearestRoad != null) {
            val properties = nearestRoad.properties
            if (properties != null) {
                val orientation = directionProvider.getCurrentDirection()
                var roadName = properties["name"]
                if (roadName == null) {
                    roadName = properties["highway"]
                }
                val facingDirectionAlongRoad =
                    getCompassLabelFacingDirectionAlong(
                        localizedContext,
                        orientation.toInt(),
                        roadName.toString(),
                        inMotion,
                        inVehicle
                    )
                results.add(PositionedString(facingDirectionAlongRoad))
                return results
            }
        }

        return results
    }

    private fun buildCalloutForRoadSense(location: LngLatAlt): List<PositionedString> {

        // Check that our location/time has changed enough to generate this callout
        if (!locationFilter.shouldUpdate(location)) {
            return emptyList()
        }

        // Check that we're in a vehicle
        if (!inVehicle) {
            return emptyList()
        }

        // Update time/location filter for our new position
        locationFilter.update(location)

        // Reverse geocode the current location (this is the iOS name for the function)
        val results = reverseGeocode(location)

        // Check that the geocode has changed before returning a callout describing it

        return results
    }

    private val intersectionFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionCalloutHistory = CalloutHistory(30000)

    private fun buildCalloutForIntersections(location: LngLatAlt) : List<PositionedString> {
        val results : MutableList<PositionedString> = mutableListOf()

        // Check that our location/time has changed enough to generate this callout
        if (!intersectionFilter.shouldUpdate(location)) {
            return emptyList()
        }

        // Check that we're not in a vehicle
        if (inVehicle) {
            return emptyList()
        }

        // Update time/location filter for our new position
        intersectionFilter.update(location)
        intersectionCalloutHistory.trim(location)

        val userGeometry = getCurrentUserGeometry()
        val roadsDescription = getRoadsDescriptionFromFov(
            gridState,
            userGeometry,
            ComplexIntersectionApproach.NEAREST_NON_TRIVIAL_INTERSECTION)

        addIntersectionCalloutFromDescription(roadsDescription,
                localizedContext,
                results,
                intersectionCalloutHistory)

        return results
    }

    private val poiCalloutHistory = CalloutHistory()
    private fun buildCalloutForNearbyPOI(location: LngLatAlt, speed: Float) : List<PositionedString> {
        if (!poiFilter.shouldUpdateActivity(location, speed, inVehicle)) {
            return emptyList()
        }

        val results: MutableList<PositionedString> = mutableListOf()

        // Trim history based on location and current time
        poiCalloutHistory.trim(location)

        // Make a list of POIs - this has been copied from the whatsAroundMe code, and it definitely
        // needs some work.

        // super categories are "information", "object", "place", "landmark", "mobility", "safety"
        val placesAndLandmarks = sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
        val mobility = sharedPreferences.getBoolean(MOBILITY_KEY, true)
        val gridPoiCollection = gridState.getFeatureCollection(TreeId.POIS, location, 50.0)
        val gridPoiFeatureCollection = removeDuplicateOsmIds(gridPoiCollection)
        val settingsFeatureCollection = FeatureCollection()
        if (gridPoiFeatureCollection.features.isNotEmpty()) {
            if (placesAndLandmarks) {
                if (mobility) {
                    val placeSuperCategory =
                        getPoiFeatureCollectionBySuperCategory("place", gridPoiFeatureCollection)
                    val tempFeatureCollection = FeatureCollection()
                    for (feature in placeSuperCategory.features) {
                        if (feature.foreign?.get("feature_value") != "house") {
                            if (feature.properties?.get("name") != null) {
                                val superCategoryList = getSuperCategoryElements("place")
                                // We never want to add a feature more than once
                                var found = false
                                for (property in feature.properties!!) {
                                    for (featureType in superCategoryList) {
                                        if (property.value == featureType) {
                                            tempFeatureCollection.features.add(feature)
                                            found = true
                                        }
                                        if (found) break
                                    }
                                    if (found) break
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
                            gridPoiFeatureCollection,
                        )
                    for (feature in landmarkSuperCategory.features) {
                        settingsFeatureCollection.features.add(feature)
                    }
                    val mobilitySuperCategory =
                        getPoiFeatureCollectionBySuperCategory(
                            "mobility",
                            gridPoiFeatureCollection,
                        )
                    for (feature in mobilitySuperCategory.features) {
                        settingsFeatureCollection.features.add(feature)
                    }
                } else {
                    val placeSuperCategory =
                        getPoiFeatureCollectionBySuperCategory("place", gridPoiFeatureCollection)
                    for (feature in placeSuperCategory.features) {
                        if (feature.foreign?.get("feature_type") != "building" &&
                            feature.foreign?.get(
                                "feature_value",
                            ) != "house"
                        ) {
                            settingsFeatureCollection.features.add(feature)
                        }
                    }
                    val landmarkSuperCategory =
                        getPoiFeatureCollectionBySuperCategory(
                            "landmark",
                            gridPoiFeatureCollection,
                        )
                    for (feature in landmarkSuperCategory.features) {
                        settingsFeatureCollection.features.add(feature)
                    }
                }
            } else {
                if (mobility) {
                    // Log.d(TAG, "placesAndLandmarks is false and mobility is true")
                    val mobilitySuperCategory =
                        getPoiFeatureCollectionBySuperCategory(
                            "mobility",
                            gridPoiFeatureCollection,
                        )
                    for (feature in mobilitySuperCategory.features) {
                        settingsFeatureCollection.features.add(feature)
                    }
                } else {
                    // Not sure what we are supposed to tell the user here?
                    println("placesAndLandmarks and mobility are both false so what should I tell the user?")
                }
            }
        }

        // Check this type of POI is enabled

        // If the POI is outside the trigger range for that POI category, skip it (see CalloutRangeContext)
        if (settingsFeatureCollection.features.isNotEmpty()) {
            // Original Soundscape doesn't work like this as it doesn't order them by distance
            val sortedByDistanceToFeatureCollection =
                sortedByDistanceTo(
                    location,
                    settingsFeatureCollection,
                )
            for (feature in sortedByDistanceToFeatureCollection) {
                val distance = feature.foreign?.get("distance_to") as Double?
                if (distance != null) {
                    if (distance < 10.0) {
                        var name = feature.properties?.get("name") as String?
                        var generic = false
                        if(name == null) {
                            val osmClass = feature.properties?.get("class") as String?
                            val id = ResourceMapper.getResourceId(osmClass!!)
                            name = if(id == null) {
                                osmClass
                            } else {
                                localizedContext.getString(id)
                            }
                            generic = true
                        }

                        // Check the history and if the POI has been called out recently, skip it (iOS uses 60 seconds)
                        val nearestPoint = getFeatureNearestPoint(location, feature)
                        if( nearestPoint != null) {
                            val callout = TrackedCallout(name, nearestPoint, feature.geometry.type == "Point", generic)
                            if (poiCalloutHistory.find(callout)) {
                                Log.d(TAG, "Discard ${callout.callout}")
                            } else {
                                results.add(
                                    PositionedString(
                                        name,
                                        nearestPoint,
                                        NativeAudioEngine.EARCON_SENSE_POI,
                                    ),
                                )
                                // Add the entries to the history
                                poiCalloutHistory.add(callout)
                            }
                        }
                    }
                }
            }
        }

        return results
    }

    private fun autoCallout(androidLocation: Location) : List<PositionedString> {

        // The autoCallout logic comes straight from the iOS app.

        val location = LngLatAlt(androidLocation.longitude, androidLocation.latitude)
        val speed = androidLocation.speed

        // Check that the callout isn't disabled in the settings
        if (!sharedPreferences.getBoolean(ALLOW_CALLOUTS_KEY, true)) {
            return emptyList()
        }

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val returnList = runBlocking {
            withContext(gridState.treeContext) {
                var list = emptyList<PositionedString>()
                // buildCalloutForRoadSense
                val roadSenseCallout = buildCalloutForRoadSense(location)
                if (roadSenseCallout.isNotEmpty()) {
                    list = roadSenseCallout
                } else {
                    val intersectionCallout = buildCalloutForIntersections(location)
                    if (intersectionCallout.isNotEmpty()) {
                        intersectionFilter.update(location)
                        list = list + intersectionCallout
                    }


                    // Get normal callouts for nearby POIs, for the destination, and for beacons
                    val poiCallout = buildCalloutForNearbyPOI(location, speed)

                    // Update time/location filter for our new position
                    if (poiCallout.isNotEmpty()) {
                        poiFilter.update(location)
                        list = list + poiCallout
                    }
                }

                list
            }
        }
        return returnList
    }

    fun myLocation() : List<PositionedString> {
        // getCurrentDirection() from the direction provider has a default of 0.0
        // even if we don't have a valid current direction.
        var results : MutableList<PositionedString> = mutableListOf()
        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            // Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {
                    val list : MutableList<PositionedString> = mutableListOf()
                    val location = locationProvider.get()

                    val roadGridFeatureCollection = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS,
                        location,
                        100.0
                    )

                    if (roadGridFeatureCollection.features.isNotEmpty()) {
                        //Log.d(TAG, "Found roads in tile")
                        val nearestRoad = getNearestRoad(location, gridState.getFeatureTree(TreeId.ROADS_AND_PATHS))
                        if (nearestRoad != null) {

                            val properties = nearestRoad.properties
                            if (properties != null) {
                                val orientation = directionProvider.getCurrentDirection()
                                var roadName = properties["name"]
                                if (roadName == null) {
                                    roadName = properties["highway"]
                                }
                                val facingDirectionAlongRoad =
                                    getCompassLabelFacingDirectionAlong(
                                        localizedContext,
                                        orientation.toInt(),
                                        roadName.toString(),
                                        inMotion,
                                        inVehicle
                                    )
                                list.add(PositionedString(facingDirectionAlongRoad))
                            } else {
                                Log.e(TAG, "No properties found for road")
                            }
                        }
                    } else {
                        //Log.d(TAG, "No roads found in tile just give device direction")
                        val orientation = directionProvider.getCurrentDirection()
                        val facingDirection =
                            getCompassLabelFacingDirection(
                                localizedContext,
                                orientation.toInt(),
                                inMotion,
                                inVehicle
                            )
                        results.add(PositionedString(facingDirection))
                    }
                    list
                }
            }
        }
        return results
    }

    suspend fun searchResult(searchString: String) =
        withContext(Dispatchers.IO) {
            return@withContext PhotonSearchProvider
                .getInstance()
                .getSearchResults(
                    searchString = searchString,
                    latitude = locationProvider.getCurrentLatitude(),
                    longitude = locationProvider.getCurrentLongitude(),
                ).execute()
                .body()
        }

    fun whatsAroundMe() : List<PositionedString> {
        // TODO:
        //  Setup settings in the menu so we can pass in the filters, etc.
        //  Original Soundscape "in findCalloutsFor and it tries to get a POI in each quadrant.
        //  It starts off searching within 200m and keeps increasing by 200m until it
        //  hits the maximum of 1000m. It only plays out one POI per quadrant"
        var results : MutableList<PositionedString> = mutableListOf()

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {
                    val list: MutableList<PositionedString> = mutableListOf()

                    val userGeometry = getCurrentUserGeometry()

                    // TODO: We could build separate rtrees for each of the super categories i.e.  landmarks,
                    //  places etc. and that would make this code simpler. We could ask for a maximum number
                    //  of results when calling getGridFeatureCollection. For now, we just limit arbitrarily
                    //  to 600m.
                    val gridPoiFeatureCollection = FeatureCollection()
                    val poiFeatureCollections =
                        gridState.getFeatureCollection(TreeId.POIS, userGeometry.location, 1000.0)
                    val removeDuplicatePoisFeatureCollection =
                        removeDuplicateOsmIds(poiFeatureCollections)
                    gridPoiFeatureCollection.features.addAll(removeDuplicatePoisFeatureCollection)

                    if (gridPoiFeatureCollection.features.isNotEmpty()) {
                        // filter POI feature collection by super category
                        val superCategories = listOf("information", "object", "place", "landmark", "mobility", "safety")
                        val poiFeatureCollectionsBySuperCategory = superCategories.associateWith { superCategory ->
                            getPoiFeatureCollectionBySuperCategory(superCategory, gridPoiFeatureCollection)
                        }
                        // Categories from settings
                        val blnInformation = false
                        val blnObject = false
                        val blnPlace = sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
                        val blnLandmark = sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
                        val blnMobility = sharedPreferences.getBoolean(MOBILITY_KEY, true)
                        val blnSafety = false
                        // Specify the super categories we want to check for in the relative directions polygons
                        // based on above
                        val selectedSuperCategories = mutableListOf<String>()
                        if (blnInformation){
                            selectedSuperCategories.add("information")
                        }
                        if (blnObject){
                            selectedSuperCategories.add("object")
                        }
                        if (blnPlace){
                            selectedSuperCategories.add("place")
                        }
                        if (blnLandmark){
                            selectedSuperCategories.add("landmark")
                        }
                        if (blnMobility){
                            selectedSuperCategories.add("mobility")
                        }
                        if (blnSafety){
                            selectedSuperCategories.add("safety")
                        }
                        // Filter the feature collections based on the selected super categories
                        val selectedFeatureCollections = poiFeatureCollectionsBySuperCategory.filterKeys {
                            it in selectedSuperCategories }.values
                        // Process the selected feature collections
                        var distance = 200.0
                        val incrementDistance = 200.0
                        val maxDistance = 1000.0
                        // Direction order is: behind(0) left(1) ahead(2) right(3)
                        val allDirections = listOf(0, 1, 2, 3)

                        val featuresByDirection = mutableMapOf<Int, MutableList<Feature>>()
                        val addedFeatureNames = mutableSetOf<String>()
                        while (true) {
                            val individualRelativePolygons = getRelativeDirectionsPolygons(
                                UserGeometry(
                                    userGeometry.location,
                                    userGeometry.heading,
                                    distance
                                ), RelativeDirections.INDIVIDUAL)

                            for (superCategoryFC in selectedFeatureCollections) {
                                val featureDirections = findFeaturesInPolygons(individualRelativePolygons, superCategoryFC)
                                for ((feature, direction) in featureDirections) {
                                    val featureName = feature.properties?.get("name") as? String
                                    // Check if direction already has a feature and that we don't have a feature with the same name already
                                    if (!featuresByDirection.containsKey(direction!! as Int)
                                        && featureName != null
                                        && !addedFeatureNames.contains(featureName)) {
                                        featuresByDirection.getOrPut(direction as Int) {
                                            mutableListOf() }.add(feature)
                                        addedFeatureNames.add(featureName)
                                        //println("Feature: OSM ID: ${feature.foreign?.get("osm_ids")} Name: ${feature.properties?.get("name")} Direction: $direction")
                                    }
                                }
                            }

                            val directionsWithoutFeatures = allDirections.filter { !featuresByDirection.containsKey(it) }
                            if (directionsWithoutFeatures.isEmpty() || distance >= maxDistance) {
                                val sortedFeaturesByDirection = featuresByDirection.toSortedMap()
                                for ((direction, features) in sortedFeaturesByDirection) {
                                    for (feature in features) {
                                        if (feature.geometry is Polygon) {
                                            // found that if a thing has a name property that ends in a number
                                            // "data 365" then the 365 and distance away get merged into a large number "365200 meters". Hoping a full stop will fix it
                                            if (feature.properties?.get("name") != null) {
                                                val d = distanceToPolygon(userGeometry.location, feature.geometry as Polygon).toInt()
                                                val text = "${feature.properties?.get("name")}. ${localizedContext.getString(R.string.distance_format_meters, d.toString())}"

                                                val poiLocation =
                                                    getFeatureNearestPoint(userGeometry.location, feature)
                                                list.add(
                                                    PositionedString(
                                                        text,
                                                        poiLocation,
                                                        NativeAudioEngine.EARCON_SENSE_POI
                                                    )
                                                )
                                            }
                                        } else if (feature.geometry is Point) {
                                            if (feature.properties?.get("name") != null) {
                                                val point = feature.geometry as Point
                                                val d = userGeometry.location.distance(point.coordinates).toInt()
                                                val text = "${feature.properties?.get("name")}. ${localizedContext.getString(R.string.distance_format_meters, d.toString())}"
                                                list.add(
                                                    PositionedString(
                                                        text,
                                                        point.coordinates,
                                                        NativeAudioEngine.EARCON_SENSE_POI
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                break
                            } else {
                                distance += incrementDistance
                                //println("Increasing distance to $distance meters")
                            }
                        }

                    } else {
                        Log.d(TAG, "No Points Of Interest found in the grid")
                        list.add(PositionedString(localizedContext.getString(R.string.callouts_nothing_to_call_out_now)))
                    }
                    list
                }
            }
        }
        return results
    }

    fun aheadOfMe() : List<PositionedString> {
        // TODO This is just a rough POC at the moment. Lots more to do...
        var results : MutableList<PositionedString> = mutableListOf()

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            // Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {
                    val list: MutableList<PositionedString> = mutableListOf()

                    // get device direction
                    val userGeometry = getCurrentUserGeometry()

                    // Detect if there is a road or an intersection in the FOV
                    val roadsDescription = getRoadsDescriptionFromFov(
                        gridState,
                        userGeometry,
                        ComplexIntersectionApproach.NEAREST_NON_TRIVIAL_INTERSECTION
                    )
                    addIntersectionCalloutFromDescription(roadsDescription,
                        localizedContext,
                        list)

                    // Detect if there is a crossing in the FOV
                    val points = getFovTrianglePoints(userGeometry)
                    val nearestCrossing = gridState.getNearestFeatureOnRoadInFov(TreeId.CROSSINGS,
                        userGeometry.location,
                        points.left,
                        points.right)
                    appendNearestFeatureCallout(nearestCrossing, R.string.osm_tag_crossing, list)

                    // Detect if there is a bus_stop in the FOV
                    val nearestBusStop = gridState.getNearestFeatureOnRoadInFov(TreeId.BUS_STOPS,
                        userGeometry.location,
                        points.left,
                        points.right)
                    appendNearestFeatureCallout(nearestBusStop, R.string.osm_tag_bus_stop, list)

                    if(list.isEmpty()) {
                        list.add(
                            PositionedString(
                                localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                            )
                        )
                    }
                    list
                }
            }
        }
        return results
    }

    private fun appendNearestFeatureCallout(nearestFeature: GridState.FeatureByRoad?,
                                            osmTagType: Int,
                                            list: MutableList<PositionedString>) {
        if(nearestFeature != null) {
            val text = buildString {
                append(localizedContext.getString(osmTagType))
                append(". ")
                append(
                    localizedContext.getString(
                        R.string.distance_format_meters,
                        nearestFeature.distance.toInt().toString()
                    )
                )
                append(". ")
                if (nearestFeature.road.properties?.get("name") != null) {
                    append(nearestFeature.road.properties?.get("name"))
                }
            }
            list.add(PositionedString(text))
        }
    }

    fun streetPreviewGo() {
        if(true) {
            streetPreviewGoInternal()
        } else {
            // Random walker for StreetPreview
            CoroutineScope(Job()).launch {
                repeat(1000) {
                    streetPreviewGoWander()
                    delay(200.milliseconds)
                }
            }
        }
    }

    private fun streetPreviewGoWander() {

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val engine = this
        CoroutineScope(Job()).launch(gridState.treeContext) {
            // Get our current location and figure out what GO means
            val userGeometry = getCurrentUserGeometry()
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
                    val trimmedChoices = mutableListOf<StreetPreview.StreetPreviewChoice>()
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
            userGeometry.heading = heading
            streetPreview.go(userGeometry, engine)
        }
    }

    private fun streetPreviewGoInternal() {
        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val engine = this
        CoroutineScope(Job()).launch(gridState.treeContext) {
            // Get our current location and figure out what GO means
            val userGeometry = getCurrentUserGeometry()
            streetPreview.go(userGeometry, engine)
        }
    }

    companion object {
        private const val TAG = "GeoEngine"
    }
}
