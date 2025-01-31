package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.squareup.otto.Subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.ComplexIntersectionApproach
import org.scottishtecharmy.soundscape.geoengine.callouts.addIntersectionCalloutFromDescription
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.services.getOttoBus
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.utils.toLocationDescriptions
import java.util.Locale
import kotlin.math.abs
import kotlin.time.TimeSource

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

    private lateinit var autoCallout: AutoCallout

    private val streetPreview = StreetPreview()

    private fun getCurrentUserGeometry(headingMode: UserGeometry.HeadingMode) : UserGeometry {
        return UserGeometry(
            location = locationProvider.get(),
            phoneHeading = directionProvider.getCurrentDirection().toDouble(),
            fovDistance = 50.0,
            inVehicle = inVehicle,
            inMotion = inMotion,
            speed = locationProvider.getSpeed(),
            headingMode = headingMode,
            travelHeading = locationProvider.getHeading())
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
        autoCallout = AutoCallout(localizedContext, sharedPreferences)

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
    private fun createSuperCategoriesSet() : Set<String> {
        val enabledCategories = emptySet<String>().toMutableSet()
        if (sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true))
            enabledCategories.add(PLACES_AND_LANDMARKS_KEY)

        if (sharedPreferences.getBoolean(MOBILITY_KEY, true))
            enabledCategories.add(MOBILITY_KEY)

        return enabledCategories
    }
    private fun startMonitoringLocation(soundscapeService: SoundscapeService) {
        Log.e(TAG, "startTileGridService")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            locationProvider.locationFlow.collect { newLocation ->

                newLocation?.let { location ->

                    // Update the grid state
                    gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        createSuperCategoriesSet()
                    )

                    // So long as the AudioEngine is not already busy, run any auto callouts that we
                    // need. Auto Callouts use the direction of travel if there is one, otherwise
                    // falling back to use the phone direction.
                    if(!soundscapeService.isAudioEngineBusy()) {
                        val callouts =
                            autoCallout.updateLocation(getCurrentUserGeometry(UserGeometry.HeadingMode.Auto), gridState)
                        if (callouts.isNotEmpty()) {
                            // Tell the service that we've got some callouts to tell the user about
                            soundscapeService.speakCallout(callouts)
                        }
                    }
                }
            }
        }
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
        // Duplicate original Soundscape behaviour:
        //   In findCalloutsFor it tries to get a POI in each quadrant. It starts off searching
        //   within 200m and keeps increasing by 200m until it hits the maximum of 1000m. It only
        //   plays out a single POI per quadrant.
        var results : MutableList<PositionedString> = mutableListOf()
        val timeSource = TimeSource.Monotonic
        val gridStartTime = timeSource.markNow()

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(gridState.treeContext) {

                    val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)

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
                            val featureCollection = featureTree.generateNearestFeatureCollectionWithinTriangle(triangle, 4)
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
                        val poiLocation = getDistanceToFeature(userGeometry.location, feature)
                        val name = getTextForFeature(localizedContext, feature)
                        val text = "${name.text}. ${formatDistance(poiLocation.distance, localizedContext)}"
                        list.add(
                            PositionedString(
                                text,
                                poiLocation.point,
                                NativeAudioEngine.EARCON_SENSE_POI
                            )
                        )
                    }
                    list
                }
            }
        }
        val gridFinishTime = timeSource.markNow()
        Log.e(GridState.TAG, "Time to calculate AroundMe: ${gridFinishTime - gridStartTime}")

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
                    val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)

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
                    val triangle = getFovTriangle(userGeometry)
                    val nearestCrossing = gridState.getNearestFeatureOnRoadInFov(
                        TreeId.CROSSINGS,
                        triangle
                    )
                    appendNearestFeatureCallout(nearestCrossing, R.string.osm_tag_crossing, list)

                    // Detect if there is a bus_stop in the FOV
                    val nearestBusStop = gridState.getNearestFeatureOnRoadInFov(
                        TreeId.BUS_STOPS,
                        triangle
                    )
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

    fun streetPreviewGo() : List<StreetPreviewChoice> {
        return streetPreviewGoInternal()
    }

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
            return@withContext PhotonSearchProvider
                .getInstance()
                .reverseGeocodeLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                ).execute()
                .body()
        }

    /**
     * getLocationDescription returns a LocationDescription object for the current location. This
     * is basically a reverse geocode. It initially tries to generate it from local tile data, but
     * falls back to geocoding via the Photon server if network is available.
     * @param location to reverse geocode
     * @param preserveLocation ensures that the returned LocationDescription contains the passed in
     * location rather than overwriting it with the location of a POI that it geocded to.
     * @return a LocationDescription object containing an address of the location
     */
    fun getLocationDescription(location: LngLatAlt,
                               preserveLocation: Boolean = true) : LocationDescription? {

        var geocode: LocationDescription?
        val currentLocation = locationProvider.get()
        // If the location is within our current TileGrid, then we can make our own description of
        // the location.
        geocode = runBlocking {
            withContext(gridState.treeContext) {
                localReverseGeocode(location, gridState, localizedContext)
            }
        }
        if(geocode != null) {
            val distance = locationProvider.get().distance(geocode.location)
            geocode.distance = formatDistance(distance, localizedContext)
            return geocode
        }

        // If we have network, then we should be able to do a reverse geocode via the photon server
        // TODO: Check for network first
        if(true) {
            geocode = runBlocking {
                withContext(Dispatchers.IO) {
                    val result = reverseGeocodeResult(location)

                    // The geocode result includes the location for the POI. In the case of something
                    // like a park this could be a long way from the point that was passed in.
                    val ld = result?.features?.toLocationDescriptions(currentLocation, localizedContext)
                    if (!ld.isNullOrEmpty()) {
                        if(preserveLocation) {
                            val overwritten = ld.first()
                            overwritten.location = location
                            if(overwritten.addressName != null) {
                                overwritten.addressName = localizedContext.getString(R.string.directions_near_name).format(overwritten.addressName)
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

        // If we don't have network, and the location is outside of our current TileGrid, then we
        // could see if the tiles are cached and we can create a temporary TileGrid just for this
        // but for now just return null.
        return null
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
fun getTextForFeature(localizedContext: Context, feature: Feature) : TextForFeature {
    var generic = false
    val name = feature.properties?.get("name") as String?
    val featureValue = feature.foreign?.get("feature_value")

    var text = name
    when(featureValue) {
        "bus_stop" -> {
            text = if (name != null)
                localizedContext.getString(R.string.osm_tag_bus_stop_named).format(name)
            else
                localizedContext.getString(R.string.osm_tag_bus_stop)
        }
        "station" -> {
            text = if (name != null)
                localizedContext.getString(R.string.osm_tag_train_station_named).format(name)
            else
                localizedContext.getString(R.string.osm_tag_train_station)
        }
    }
    if (text == null) {
        val osmClass =
            feature.properties?.get("class") as String? ?: return TextForFeature("", true)

        val id = ResourceMapper.getResourceId(osmClass)
        text = if (id == null) {
            osmClass
        } else {
            localizedContext.getString(id)
        }
        generic = true
    }

    return TextForFeature(text, generic)
}

fun formatDistance(distance: Double, localizedContext: Context) : String {
    if(distance > 1000) {
        val km = (distance.toInt() / 100).toFloat() / 10
        return localizedContext.getString(R.string.distance_format_km, km.toString())
    } else {
        val metres = distance.toInt()
        return  localizedContext.getString(R.string.distance_format_meters, metres.toString())
    }
}

fun localReverseGeocode(location: LngLatAlt,
                        gridState: GridState,
                        localizedContext: Context): LocationDescription? {

    if(!gridState.isLocationWithinGrid(location)) return null

    // Check if we're inside a POI
    val gridPoiCollection = gridState.getFeatureCollection(TreeId.POIS, location, 200.0)
    val gridPoiFeatureCollection = removeDuplicateOsmIds(gridPoiCollection)
    for(poi in gridPoiFeatureCollection) {
        // We can only be inside polygons
        if(poi.geometry.type == "Polygon") {
            val polygon = poi.geometry as Polygon

            if(polygonContainsCoordinates(location, polygon)) {
                // We've found a POI that contains the location. We could return the c
                val name = poi.properties?.get("name")
                if(name != null) {
                    return LocationDescription(
                        addressName = localizedContext.getString(R.string.directions_at_poi).format(name as String),
                        location = location,
                    )
                }
            }
        }
    }

    // Check if the location is alongside a road/path.
    val nearestRoad = gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, location, 100.0)
    if(nearestRoad != null) {
        val properties = nearestRoad.properties
        if (properties != null) {
            var roadName = properties["name"]
            if (roadName == null) {
                roadName = properties["highway"]
            }
            return LocationDescription(
                addressName = localizedContext.getString(R.string.directions_near_name).format(roadName as String),
                location = location,
            )
        }
    }

    return null
}

/** Reverse geocodes a location into 1 of 4 possible states
 * - within a POI
 * - alongside a road
 * - general location
 * - unknown location).
 */
fun reverseGeocode(userGeometry: UserGeometry,
                   gridState: GridState,
                   localizedContext: Context): PositionedString {

    val location = localReverseGeocode(userGeometry.location, gridState, localizedContext)
    location?.let { l ->
        l.addressName?.let { name ->
            return PositionedString(name, userGeometry.location)
        }
    }
    return PositionedString(localizedContext.getString(R.string.poi_unknown_place))
}
