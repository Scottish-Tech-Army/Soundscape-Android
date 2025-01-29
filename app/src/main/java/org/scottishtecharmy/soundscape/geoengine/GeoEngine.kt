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
import kotlinx.coroutines.delay
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
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.services.getOttoBus
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
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

    /** UserGeometry contains all of the data relating to the location and motion of the user. It's
     * aim is simply to reduces the number of arguments to many of the API calls.
     */
    data class UserGeometry(val location: LngLatAlt = LngLatAlt(),
                            var heading: Double = 0.0,
                            val fovDistance: Double = 50.0,
                            val inVehicle: Boolean = false,
                            val inMotion: Boolean = false,
                            val speed: Double = 0.0)
    private fun getCurrentUserGeometry() : UserGeometry {
        return UserGeometry(locationProvider.get(),
            directionProvider.getCurrentDirection().toDouble(),
            50.0,
            inVehicle,
            inMotion,
            locationProvider.getSpeed())
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

                    // Run any auto callouts that we need
                    val callouts = autoCallout.updateLocation(getCurrentUserGeometry(), gridState)
                    if (callouts.isNotEmpty()) {
                        // Tell the service that we've got some callouts to tell the user about
                        soundscapeService.speakCallout(callouts)
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

                    val userGeometry = getCurrentUserGeometry()

                    // Direction order is: behind(0) left(1) ahead(2) right(3)
                    val featuresByDirection: Array<Feature?> = arrayOfNulls(4)
                    val directionsNeeded = setOf(0, 1, 2, 3).toMutableSet()

                    // We already have a FeatureTree containing the POI that we wish to search on
                    val featureTree = gridState.getFeatureTree(TreeId.SELECTED_SUPER_CATEGORIES)
                    for (distance in 200..1000 step 200) {

                        // Get Polygons for this FOV distance
                        val individualRelativePolygons = getRelativeDirectionsPolygons(
                            UserGeometry(
                                userGeometry.location,
                                userGeometry.heading,
                                distance.toDouble()
                            ), RelativeDirections.INDIVIDUAL
                        )

                        val direction = directionsNeeded.iterator()
                        while (direction.hasNext()) {

                            val dir = direction.next()
                            val triangle = getTriangleForDirection(individualRelativePolygons, dir)
                            val feature = featureTree.getNearestFeatureWithinTriangle(triangle)
                            if (feature != null) {
                                // We found a feature in this direction, check whether we're already
                                // calling out e.g. there's an L shaped park wrapping around us
                                var duplicate = false
                                for(otherFeature in featuresByDirection) {
                                    if(otherFeature == null) continue
                                    if(feature == otherFeature) duplicate = true
                                }
                                if(!duplicate) {
                                    // Remember it and remove it from the set of directions to search
                                    featuresByDirection[dir] = feature
                                    direction.remove()
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
                        val text = "${name.text}. ${localizedContext.getString(R.string.distance_format_meters, poiLocation.distance.toString())}"
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