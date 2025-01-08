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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.ALLOW_CALLOUTS_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.MAP_DEBUG_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.geoengine.utils.deduplicateFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getFeatureNearestPoint
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionLabel
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.processTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.processTileString
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.network.ProtomapsTileClient
import org.scottishtecharmy.soundscape.network.SoundscapeBackendTileClient
import org.scottishtecharmy.soundscape.network.TileClient
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.services.getOttoBus
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import retrofit2.awaitResponse
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

data class PositionedString(val text : String, val location : LngLatAlt? = null, val earcon : String? = null)

class GeoEngine {
    private val coroutineScope = CoroutineScope(Job())

    // GeoJSON tiles job
    private var tilesJob: Job? = null

    // Flow to return current tile grid GeoJSON
    private val _tileGridFlow = MutableStateFlow(TileGrid(mutableListOf(), BoundingBox()))
    var tileGridFlow: StateFlow<TileGrid> = _tileGridFlow

    // HTTP connection to soundscape-backend or protomaps tile server
    private lateinit var tileClient: TileClient

    internal lateinit var locationProvider : LocationProvider
    private lateinit var directionProvider : DirectionProvider

    // Resource string locale configuration
    private lateinit var configLocale: Locale
    private lateinit var configuration: Configuration
    private lateinit var localizedContext: Context

    private lateinit var sharedPreferences: SharedPreferences

    private var centralBoundingBox = BoundingBox()
    private var inVehicle = false
    private var featureTrees = Array(Fc.MAX_COLLECTION_ID.id) { FeatureTree(null) }
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val treeContext = newSingleThreadContext("TreeContext")

    private val streetPreview = StreetPreview()

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

        tileClient =
            if (SOUNDSCAPE_TILE_BACKEND) {
                SoundscapeBackendTileClient(application)
            } else {
                ProtomapsTileClient(application)
            }

        configLocale = getCurrentLocale()
        configuration = Configuration(application.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = application.applicationContext.createConfigurationContext(configuration)

        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        streetPreview.start()

        startTileGridService(soundscapeService)

        getOttoBus().register(this)
    }

    fun stop() {
        getOttoBus().unregister(this)

        tilesJob?.cancel()
        locationProvider.destroy()
        directionProvider.destroy()
    }

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    private fun startTileGridService(soundscapeService: SoundscapeService) {
        Log.e(TAG, "startTileGridService")
        tilesJob?.cancel()
        tilesJob = coroutineScope.launch {
            locationProvider.locationFlow.collect { newLocation ->
                val timeSource = TimeSource.Monotonic
                val gridStartTime = timeSource.markNow()

                // Check if we've moved out of the bounds of the central area
                newLocation?.let { location ->
                    // Check if we're still within the central area of our grid
                    if (!pointIsWithinBoundingBox(LngLatAlt(location.longitude, location.latitude),
                                                  centralBoundingBox)) {

                            Log.d(TAG, "Update central grid area")
                            // The current location has moved from within the central area, so get the
                            // new grid and the new central area.
                            val tileGrid =
                                getTileGrid(
                                    locationProvider.getCurrentLatitude() ?: 0.0,
                                    locationProvider.getCurrentLongitude() ?: 0.0,
                                )

                            // We have a new centralBoundingBox, so update the tiles
                            val featureCollections =
                                Array(Fc.MAX_COLLECTION_ID.id) { FeatureCollection() }
                            if (updateTileGrid(tileGrid, featureCollections)) {
                                // We have got a new grid, so create our new central region
                                centralBoundingBox = tileGrid.centralBoundingBox

                                val localTrees = Array(Fc.MAX_COLLECTION_ID.id) { FeatureTree(null) }
                                if (SOUNDSCAPE_TILE_BACKEND) {
                                    // De-duplicate
                                    val deDuplicatedCollection =
                                        Array(Fc.MAX_COLLECTION_ID.id) { FeatureCollection() }
                                    for ((index, fc) in featureCollections.withIndex()) {
                                        val existingSet: MutableSet<Any> = mutableSetOf()
                                        deduplicateFeatureCollection(
                                            deDuplicatedCollection[index],
                                            fc,
                                            existingSet,
                                        )
                                    }
                                    // Create rtrees for each feature collection
                                    for ((index, fc) in deDuplicatedCollection.withIndex()) {
                                        localTrees[index] = FeatureTree(fc)
                                    }
                                } else {
                                    // Join up roads/paths at the tile boundary
                                    val joiner = InterpolatedPointsJoiner()
                                    for (ip in featureCollections[Fc.INTERPOLATIONS.id]) {
                                        joiner.addInterpolatedPoints(ip)
                                    }
                                    // TODO: The joining lines are being added to ROADS, but some of
                                    //  them will be for paths. I don't think this is an issue, but it
                                    //  it's slightly confusing.
                                    joiner.addJoiningLines(featureCollections[Fc.ROADS.id])

                                    // Create rtrees for each feature collection
                                    for ((index, fc) in featureCollections.withIndex()) {
                                        localTrees[index] = FeatureTree(fc)
                                    }
                                }

                                // Assign rtrees to our shared trees from within the treeContext. All
                                // other accesses of featureTrees needs to be from within the same
                                // context.
                                runBlocking {
                                    withContext(treeContext) {
                                        for (fc in featureCollections.withIndex()) {
                                            featureTrees[fc.index] = localTrees[fc.index]
                                        }
                                    }
                                }

                                val gridFinishTime = timeSource.markNow()
                                Log.e(TAG, "Time to populate grid: ${gridFinishTime - gridStartTime}")

                                // Update the flow with our new tile grid
                                if (sharedPreferences.getBoolean(MAP_DEBUG_KEY, false)) {
                                    _tileGridFlow.value = tileGrid
                                } else {
                                    _tileGridFlow.value = TileGrid(mutableListOf(), BoundingBox())
                                }
                            } else {
                                // Updating the tile grid failed, due to a lack of cached tile and then
                                // a lack of network/server issue. There's nothing that we can do, so
                                // simply retry on the next location update.
                            }
                        }
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

    private suspend fun updateTileFromProtomaps(
        x: Int,
        y: Int,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        var ret = false
        withContext(Dispatchers.IO) {
            try {
                val service =
                    tileClient.retrofitInstance?.create(ITileDAO::class.java)
                val tileReq =
                    async {
                        service?.getVectorTileWithCache(x, y, ZOOM_LEVEL)
                    }
                val result = tileReq.await()?.awaitResponse()?.body()
                if (result != null) {
                    Log.e(TAG, "Tile size ${result.serializedSize}")
                    val tileFeatureCollection = vectorTileToGeoJson(x, y, result)
                    val collections = processTileFeatureCollection(tileFeatureCollection)
                    for ((index, collection) in collections.withIndex()) {
                        featureCollections[index].plusAssign(collection)
                    }

                    ret = true
                } else {
                    Log.e(TAG, "No response for protomaps tile")
                }
            } catch (ce: CancellationException) {
                // We have to rethrow cancellation exceptions
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting protomaps tile $e")
            }
        }
        return ret
    }

    private suspend fun updateTileFromSoundscapeBackend(
        x: Int,
        y: Int,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        var ret = false

        withContext(Dispatchers.IO) {
            try {
                val service =
                    tileClient.retrofitInstance?.create(ITileDAO::class.java)
                val tileReq =
                    async {
                        service?.getTileWithCache(x, y)
                    }
                val result = tileReq.await()?.awaitResponse()?.body()
                // clean the tile, process the string, perform an insert into db using the clean tile data
                Log.e(TAG, "Tile size ${result?.length}")
                val cleanedTile =
                    result?.let { cleanTileGeoJSON(x, y, ZOOM_LEVEL, it) }

                if (cleanedTile != null) {
                    val tileData = processTileString(cleanedTile)
                    for ((index, collection) in tileData.withIndex()) {
                        featureCollections[index].plusAssign(collection)
                    }

                    ret = true
                } else {
                    Log.e(TAG, "Failed to get clean soundscape-backend tile")
                }
            } catch (ce: CancellationException) {
                // We have to rethrow cancellation exceptions
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting soundscape-backend tile $e")
            }
        }
        return ret
    }

    private suspend fun updateTile(
        x: Int,
        y: Int,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        if (!SOUNDSCAPE_TILE_BACKEND) {
            return updateTileFromProtomaps(x, y, featureCollections)
        }
        return updateTileFromSoundscapeBackend(x, y, featureCollections)
    }

    private suspend fun updateTileGrid(
        tileGrid: TileGrid,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        for (tile in tileGrid.tiles) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            var ret = false
            for (retry in 1..5) {
                ret = updateTile(tile.tileX, tile.tileY, featureCollections)
                if (ret) {
                    break
                }
            }
            if (!ret) {
                return false
            }
        }
        return true
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
        val gridPoiCollection = getGridFeatureCollection(Fc.POIS.id, location, 200.0)
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

        // Check if we're alongside a road
        var nearestRoad = getNearestFeature(Fc.ROADS.id, location, 100.0)
        if(nearestRoad == null) {
            nearestRoad = getNearestFeature(Fc.PATHS.id, location, 100.0)
        }
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
                        configLocale
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

        // The intersection callout logic is:
        //
        // Find all roads within 60m
        // Find all intersections within 60m
        //
        // Call getFovRoadsFeatureCollection with both FeatureCollections to get collections of roads
        // and intersections in the forwards pointing field of view. Prior to rtrees this involved a
        // straight search. Now we use the rtree code to get the Features within the bounding box of
        // the FOV triangle before performing the more expensive weeding out those outwith the actual
        // triangle. This is all done inside generateFeatureCollectionWithinFov.

        val heading = directionProvider.getCurrentDirection().toDouble()
        val fovDistance = 50.0

        val points = getFovTrianglePoints(location, heading, fovDistance)
        val fovIntersectionsFeatureCollection =
            featureTrees[Fc.INTERSECTIONS.id].generateFeatureCollectionWithinTriangle(location,
                points.left,
                points.right)

        val fovRoadsFeatureCollection =
            featureTrees[Fc.ROADS.id].generateFeatureCollectionWithinTriangle(location,
                points.left,
                points.right)

        if (fovIntersectionsFeatureCollection.features.isNotEmpty() &&
            fovRoadsFeatureCollection.features.isNotEmpty()
        ) {
            val intersectionsSortedByDistance =
                sortedByDistanceTo(
                    locationProvider.getCurrentLatitude() ?: 0.0,
                    locationProvider.getCurrentLongitude() ?: 0.0,
                    fovIntersectionsFeatureCollection,
                )

            val testNearestRoad =
                getNearestRoad(
                    location,
                    fovRoadsFeatureCollection,
                )
            val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

            for (i in 0 until intersectionsSortedByDistance.features.size) {
                val intersectionRoadNames =
                    getIntersectionRoadNames(intersectionsSortedByDistance.features[i], fovRoadsFeatureCollection)
                if (checkWhetherIntersectionIsOfInterest(intersectionRoadNames, testNearestRoad)) {
                    intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[i])
                }
            }
            if (intersectionsNeedsFurtherCheckingFC.features.isNotEmpty()) {
                // Approach 1: find the intersection feature with the most osm_ids and use that?
                val featureWithMostOsmIds: Feature? =
                    intersectionsNeedsFurtherCheckingFC.features.maxByOrNull { feature ->
                        (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                    }
                val nearestIntersection = FeatureTree(fovIntersectionsFeatureCollection).getNearestFeature(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0,
                        ), Double.NaN)
                val nearestRoadBearing =
                    getRoadBearingToIntersection(
                        nearestIntersection,
                        testNearestRoad,
                        heading,
                    )
                if (featureWithMostOsmIds != null) {
                    val intersectionLocation =
                        featureWithMostOsmIds.geometry as Point
                    val intersectionLngLat =
                        LngLatAlt(
                            intersectionLocation.coordinates.longitude,
                            intersectionLocation.coordinates.latitude,
                        )
                    val intersectionRelativeDirections =
                        getRelativeDirectionsPolygons(
                            intersectionLngLat,
                            nearestRoadBearing,
                            // fovDistance,
                            5.0,
                            RelativeDirections.COMBINED,
                        )
                    val distanceToNearestIntersection =
                        distance(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            intersectionLocation.coordinates.latitude,
                            intersectionLocation.coordinates.longitude,
                        )
                    val intersectionRoadNames =
                        getIntersectionRoadNames(
                            featureWithMostOsmIds,
                            fovRoadsFeatureCollection,
                        )

                    val intersectionName =
                        featureWithMostOsmIds.properties?.get("name") as String
                    val callout =
                        TrackedCallout(
                            intersectionName,
                            intersectionLngLat,
                            isPoint = true,
                            isGeneric = false,
                        )
                    if (intersectionCalloutHistory.find(callout)) {
                        Log.d(TAG, "Discard ${callout.callout}")
                        return emptyList()
                    } else {
                        results.add(
                            PositionedString(
                                "${localizedContext.getString(R.string.intersection_approaching_intersection)} ${
                                    localizedContext.getString(
                                        R.string.distance_format_meters,
                                        distanceToNearestIntersection.toInt().toString(),
                                    )
                                }",
                            ),
                        )
                        intersectionCalloutHistory.add(callout)
                    }

                    val roadRelativeDirections =
                        getIntersectionRoadNamesRelativeDirections(
                            intersectionRoadNames,
                            featureWithMostOsmIds,
                            intersectionRelativeDirections,
                        )
                    for (feature in roadRelativeDirections.features) {
                        val direction =
                            feature.properties
                                ?.get("Direction")
                                .toString()
                                .toIntOrNull()
                        // Don't call out the road we are on (0) as part of the intersection
                        if (direction != null && direction != 0) {
                            val relativeDirectionString =
                                getRelativeDirectionLabel(
                                    localizedContext,
                                    direction,
                                    configLocale,
                                )
                            if (feature.properties?.get("name") != null) {
                                val intersectionCallout =
                                    localizedContext.getString(
                                        R.string.directions_intersection_with_name_direction,
                                        feature.properties?.get("name"),
                                        relativeDirectionString,
                                    )
                                results.add(
                                    PositionedString(
                                        intersectionCallout,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
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
        val gridPoiCollection = getGridFeatureCollection(Fc.POIS.id, location, 50.0)
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
                    locationProvider.getCurrentLatitude() ?: 0.0,
                    locationProvider.getCurrentLongitude() ?: 0.0,
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
            withContext(treeContext) {
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
                withContext(treeContext) {
                    val list : MutableList<PositionedString> = mutableListOf()
                    val location = LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    )

                    val roadGridFeatureCollection = FeatureCollection()
                    val roadFeatureCollection =
                        getGridFeatureCollection(Fc.ROADS.id, location, 100.0)
                    val pathFeatureCollection =
                        getGridFeatureCollection(Fc.PATHS.id, location, 100.0)
                    roadGridFeatureCollection.features.addAll(roadFeatureCollection)
                    // Add in paths so we can pick up named paths
                    roadGridFeatureCollection.features.addAll(pathFeatureCollection)

                    if (roadGridFeatureCollection.features.isNotEmpty()) {
                        //Log.d(TAG, "Found roads in tile")
                        val nearestRoad =
                            getNearestRoad(
                                LngLatAlt(
                                    locationProvider.getCurrentLongitude() ?: 0.0,
                                    locationProvider.getCurrentLatitude() ?: 0.0
                                ),
                                roadGridFeatureCollection
                            )
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
                                        configLocale
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
                                configLocale
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
        // TODO This is just a rough POC at the moment. Lots more to do...
        //  setup settings in the menu so we can pass in the filters, etc.
        //  Original Soundscape just splats out a list in no particular order which is odd.
        //  If you press the button again in original Soundscape it can give you the same list but in a different sequence or
        //  it can add one to the list even if you haven't moved. It also only seems to give a thing and a distance but not a heading.
        var results : MutableList<PositionedString> = mutableListOf()

        // super categories are "information", "object", "place", "landmark", "mobility", "safety"
        val placesAndLandmarks = sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
        val mobility = sharedPreferences.getBoolean(MOBILITY_KEY, true)
        // TODO unnamed roads switch is not used yet
        // val unnamedRoads = sharedPrefs.getBoolean(UNNAMED_ROADS_KEY, false)

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {
            // Run the code within the treeContext to protect it from changes to the trees whilst it's
            // running.
            results = runBlocking {
                withContext(treeContext) {
                    val list: MutableList<PositionedString> = mutableListOf()

                    val location = LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    )
                    // TODO: We could build separate rtrees for each of the super categories i.e.  landmarks,
                    //  places etc. and that would make this code simpler. We could ask for a maximum number
                    //  of results when calling getGridFeatureCollection. For now, we just limit arbitrarily
                    //  to 500m.
                    val gridPoiFeatureCollection = FeatureCollection()
                    val poiFeatureCollections =
                        getGridFeatureCollection(Fc.POIS.id, location, 500.0)
                    val removeDuplicatePoisFeatureCollection =
                        removeDuplicateOsmIds(poiFeatureCollections)
                    gridPoiFeatureCollection.features.addAll(removeDuplicatePoisFeatureCollection)

                    if (gridPoiFeatureCollection.features.isNotEmpty()) {
                        val settingsFeatureCollection = FeatureCollection()
                        if (placesAndLandmarks) {
                            if (mobility) {
                                //Log.d(TAG, "placesAndLandmarks and mobility are both true")
                                val placeSuperCategory =
                                    getPoiFeatureCollectionBySuperCategory(
                                        "place",
                                        gridPoiFeatureCollection
                                    )
                                val tempFeatureCollection = FeatureCollection()
                                for (feature in placeSuperCategory.features) {
                                    if (feature.foreign?.get("feature_value") != "house") {
                                        if (feature.properties?.get("name") != null) {
                                            val superCategoryList =
                                                getSuperCategoryElements("place")
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
                                val cleanedPlaceSuperCategory =
                                    removeDuplicateOsmIds(tempFeatureCollection)
                                for (feature in cleanedPlaceSuperCategory.features) {
                                    settingsFeatureCollection.features.add(feature)
                                }

                                val landmarkSuperCategory =
                                    getPoiFeatureCollectionBySuperCategory(
                                        "landmark",
                                        gridPoiFeatureCollection
                                    )
                                for (feature in landmarkSuperCategory.features) {
                                    settingsFeatureCollection.features.add(feature)
                                }
                                val mobilitySuperCategory =
                                    getPoiFeatureCollectionBySuperCategory(
                                        "mobility",
                                        gridPoiFeatureCollection
                                    )
                                for (feature in mobilitySuperCategory.features) {
                                    settingsFeatureCollection.features.add(feature)
                                }

                            } else {

                                val placeSuperCategory =
                                    getPoiFeatureCollectionBySuperCategory(
                                        "place",
                                        gridPoiFeatureCollection
                                    )
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
                                        gridPoiFeatureCollection
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
                                        gridPoiFeatureCollection
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
                        if (settingsFeatureCollection.features.isNotEmpty()) {
                            // Original Soundscape doesn't work like this as it doesn't order them by distance
                            val sortedByDistanceToFeatureCollection = sortedByDistanceTo(
                                locationProvider.getCurrentLatitude() ?: 0.0,
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                settingsFeatureCollection
                            )
                            for (feature in sortedByDistanceToFeatureCollection) {
                                if (feature.geometry is Polygon) {
                                    // found that if a thing has a name property that ends in a number
                                    // "data 365" then the 365 and distance away get merged into a large number "365200 meters". Hoping a full stop will fix it
                                    if (feature.properties?.get("name") != null) {
                                        val userLocation = LngLatAlt(
                                            locationProvider.getCurrentLongitude() ?: 0.0,
                                            locationProvider.getCurrentLatitude() ?: 0.0
                                        )
                                        val text = "${feature.properties?.get("name")}.  ${
                                            distanceToPolygon(
                                                userLocation,
                                                feature.geometry as Polygon
                                            ).toInt()
                                        } meters."

                                        val poiLocation =
                                            getFeatureNearestPoint(userLocation, feature)
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
                                        val d = distance(
                                            locationProvider.getCurrentLatitude() ?: 0.0,
                                            locationProvider.getCurrentLongitude() ?: 0.0,
                                            point.coordinates.latitude,
                                            point.coordinates.longitude
                                        ).toInt()
                                        val text = "${feature.properties?.get("name")}. $d meters."
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
                        } else {
                            list.add(PositionedString(localizedContext.getString(R.string.callouts_nothing_to_call_out_now)))
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
                withContext(treeContext) {
                    val list: MutableList<PositionedString> = mutableListOf()

                    // get device direction
                    val location = LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    )
                    val orientation = directionProvider.getCurrentDirection().toDouble()
                    val fovDistance = 50.0

                    val fovRoadsFeatureCollection = getFovFeatureCollection(
                        location,
                        orientation,
                        fovDistance,
                        featureTrees[Fc.ROADS.id]
                    )
                    val fovIntersectionsFeatureCollection = getFovFeatureCollection(
                        location,
                        orientation,
                        fovDistance,
                        featureTrees[Fc.INTERSECTIONS.id]
                    )
                    val fovCrossingsFeatureCollection = getFovFeatureCollection(
                        location,
                        orientation,
                        fovDistance,
                        featureTrees[Fc.CROSSINGS.id]
                    )
                    val fovBusStopsFeatureCollection = getFovFeatureCollection(
                        location,
                        orientation,
                        fovDistance,
                        featureTrees[Fc.BUS_STOPS.id]
                    )

                    if (fovRoadsFeatureCollection.features.isNotEmpty()) {
                        val nearestRoad = getNearestRoad(
                            location,
                            fovRoadsFeatureCollection
                        )
                        // TODO check for Settings, Unnamed roads on/off here
                        if (nearestRoad != null) {
                            if (nearestRoad.properties?.get("name") != null) {
                                list.add(
                                    PositionedString(
                                        "${localizedContext.getString(R.string.directions_direction_ahead)} ${nearestRoad.properties!!["name"]}"
                                    )
                                )
                            } else {
                                // we are detecting an unnamed road here but pretending there is nothing here
                                list.add(
                                    PositionedString(
                                        localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                                    )
                                )
                            }
                        }

                        if (fovIntersectionsFeatureCollection.features.isNotEmpty() &&
                            fovRoadsFeatureCollection.features.isNotEmpty()
                        ) {

                            val intersectionsSortedByDistance = sortedByDistanceTo(
                                locationProvider.getCurrentLatitude() ?: 0.0,
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                fovIntersectionsFeatureCollection
                            )

                            val testNearestRoad = getNearestRoad(
                                location,
                                fovRoadsFeatureCollection
                            )
                            val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

                            for (i in 0 until intersectionsSortedByDistance.features.size) {
                                val intersectionRoadNames = getIntersectionRoadNames(
                                    intersectionsSortedByDistance.features[i],
                                    fovRoadsFeatureCollection
                                )
                                val intersectionsNeedsFurtherChecking =
                                    checkWhetherIntersectionIsOfInterest(intersectionRoadNames, testNearestRoad)
                                if (intersectionsNeedsFurtherChecking) {
                                    intersectionsNeedsFurtherCheckingFC.addFeature(
                                        intersectionsSortedByDistance.features[i]
                                    )
                                }
                            }
                            if (intersectionsNeedsFurtherCheckingFC.features.isNotEmpty()) {
                                // Approach 1: find the intersection feature with the most osm_ids and use that?
                                val featureWithMostOsmIds: Feature? =
                                    intersectionsNeedsFurtherCheckingFC.features.maxByOrNull { feature ->
                                        (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                                    }

                                val nearestIntersection = FeatureTree(fovIntersectionsFeatureCollection).getNearestFeature(
                                    LngLatAlt(
                                        locationProvider.getCurrentLongitude() ?: 0.0,
                                        locationProvider.getCurrentLatitude() ?: 0.0,
                                    ), Double.NaN)

                                val nearestRoadBearing = getRoadBearingToIntersection(
                                    nearestIntersection,
                                    testNearestRoad,
                                    orientation
                                )
                                if (featureWithMostOsmIds != null) {
                                    val intersectionLocation =
                                        featureWithMostOsmIds.geometry as Point
                                    val intersectionRelativeDirections =
                                        getRelativeDirectionsPolygons(
                                            LngLatAlt(
                                                intersectionLocation.coordinates.longitude,
                                                intersectionLocation.coordinates.latitude
                                            ),
                                            nearestRoadBearing,
                                            //fovDistance,
                                            5.0,
                                            RelativeDirections.COMBINED
                                        )
                                    val distanceToNearestIntersection = distance(
                                        locationProvider.getCurrentLatitude() ?: 0.0,
                                        locationProvider.getCurrentLongitude() ?: 0.0,
                                        intersectionLocation.coordinates.latitude,
                                        intersectionLocation.coordinates.longitude
                                    )
                                    val intersectionRoadNames = getIntersectionRoadNames(
                                        featureWithMostOsmIds,
                                        fovRoadsFeatureCollection
                                    )
                                    list.add(
                                        PositionedString(
                                            "${localizedContext.getString(R.string.intersection_approaching_intersection)} ${
                                                localizedContext.getString(
                                                    R.string.distance_format_meters,
                                                    distanceToNearestIntersection.toInt()
                                                        .toString()
                                                )
                                            }"
                                        )
                                    )

                                    val roadRelativeDirections =
                                        getIntersectionRoadNamesRelativeDirections(
                                            intersectionRoadNames,
                                            featureWithMostOsmIds,
                                            intersectionRelativeDirections
                                        )
                                    for (feature in roadRelativeDirections.features) {
                                        val direction =
                                            feature.properties?.get("Direction").toString()
                                                .toIntOrNull()
                                        // Don't call out the road we are on (0) as part of the intersection
                                        if (direction != null && direction != 0) {
                                            val relativeDirectionString =
                                                getRelativeDirectionLabel(
                                                    localizedContext,
                                                    direction,
                                                    configLocale
                                                )
                                            if (feature.properties?.get("name") != null) {
                                                val intersectionCallout =
                                                    localizedContext.getString(
                                                        R.string.directions_intersection_with_name_direction,
                                                        feature.properties?.get("name"),
                                                        relativeDirectionString
                                                    )
                                                list.add(
                                                    PositionedString(
                                                        intersectionCallout
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // detect if there is a crossing in the FOV
                        val nearestCrossing = FeatureTree(fovCrossingsFeatureCollection).getNearestFeature(
                            LngLatAlt(
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                locationProvider.getCurrentLatitude() ?: 0.0,
                            ), Double.NaN)
                        if (nearestCrossing != null) {
                            val crossingLocation = nearestCrossing.geometry as Point
                            val distanceToCrossing = distance(
                                locationProvider.getCurrentLatitude() ?: 0.0,
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                crossingLocation.coordinates.latitude,
                                crossingLocation.coordinates.longitude
                            )
                            // Confirm which road the crossing is on
                            val nearestRoadToCrossing = getNearestRoad(
                                LngLatAlt(
                                    crossingLocation.coordinates.longitude,
                                    crossingLocation.coordinates.latitude
                                ),
                                fovRoadsFeatureCollection
                            )
                            if (nearestRoadToCrossing != null) {
                                val crossingText = buildString {
                                    append(localizedContext.getString(R.string.osm_tag_crossing))
                                    append(". ")
                                    append(
                                        localizedContext.getString(
                                            R.string.distance_format_meters,
                                            distanceToCrossing.toInt().toString()
                                        )
                                    )
                                    append(". ")
                                    if (nearestRoadToCrossing.properties?.get("name") != null) {
                                        append(
                                            nearestRoadToCrossing.properties?.get(
                                                "name"
                                            )
                                        )
                                    }
                                }
                                list.add(PositionedString(crossingText))
                            }
                        }

                        // detect if there is a bus_stop in the FOV
                        val nearestBusStop = FeatureTree(fovBusStopsFeatureCollection).getNearestFeature(
                            LngLatAlt(
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                locationProvider.getCurrentLatitude() ?: 0.0,
                            ), Double.NaN)
                        if (nearestBusStop != null) {
                            val busStopLocation = nearestBusStop.geometry as Point
                            val distanceToBusStop = distance(
                                locationProvider.getCurrentLatitude() ?: 0.0,
                                locationProvider.getCurrentLongitude() ?: 0.0,
                                busStopLocation.coordinates.latitude,
                                busStopLocation.coordinates.longitude
                            )
                            // Confirm which road the crossing is on
                            val nearestRoadToBus = getNearestRoad(
                                LngLatAlt(
                                    busStopLocation.coordinates.longitude,
                                    busStopLocation.coordinates.latitude
                                ),
                                fovRoadsFeatureCollection
                            )
                            if (nearestRoadToBus != null) {
                                val busText = buildString {
                                    append(localizedContext.getString(R.string.osm_tag_bus_stop))
                                    append(". ")
                                    append(
                                        localizedContext.getString(
                                            R.string.distance_format_meters,
                                            distanceToBusStop.toInt().toString()
                                        )
                                    )
                                    append(". ")
                                    if (nearestRoadToBus.properties?.get("name") != null) {
                                        append(nearestRoadToBus.properties?.get("name"))
                                    }
                                }
                                list.add(PositionedString(busText))
                            }
                        }
                    } else {
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

    enum class Fc(
        val id: Int,
    ) {
        ROADS(0),
        PATHS(1),
        INTERSECTIONS(2),
        ENTRANCES(3),
        CROSSINGS(4),
        POIS(5),
        BUS_STOPS(6),
        INTERPOLATIONS(7),
        MAX_COLLECTION_ID(8),
    }

    internal fun getGridFeatureCollection(id: Int,
                                          location: LngLatAlt = LngLatAlt(),
                                          distance : Double = Double.POSITIVE_INFINITY,
                                          maxCount : Int = 0): FeatureCollection {
        val result = if(distance == Double.POSITIVE_INFINITY) {
            featureTrees[id].generateFeatureCollection()
        } else {
            if(maxCount == 0) {
                featureTrees[id].generateNearbyFeatureCollection(location, distance)
            } else {
                if (maxCount == 0) {
                    featureTrees[id].generateNearbyFeatureCollection(location, distance)
                } else {
                    featureTrees[id].generateNearestFeatureCollection(location, distance, maxCount)
                }
            }
        }
        return result
    }

    internal fun getNearestFeature(id: Int,
                                   location: LngLatAlt = LngLatAlt(),
                                   distance : Double = Double.POSITIVE_INFINITY
    ): Feature? {
        return featureTrees[id].getNearestFeature(location, distance)
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

        val start = System.currentTimeMillis()
        // Get our current location and figure out what GO means
        val location = LngLatAlt(
            locationProvider.getCurrentLongitude() ?: 0.0,
            locationProvider.getCurrentLatitude() ?: 0.0
        )
        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val engine = this
        CoroutineScope(Job()).launch(treeContext) {
            val choices = streetPreview.getDirectionChoices(engine, location)
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
            streetPreview.go(location, heading.toFloat(), engine)
        }
        val end = System.currentTimeMillis()
        Log.d(TAG, "streetPreviewGo: ${end-start}ms")
    }

    private fun streetPreviewGoInternal() {

        val start = System.currentTimeMillis()

        // Get our current location and figure out what GO means
        val location = LngLatAlt(
            locationProvider.getCurrentLongitude() ?: 0.0,
            locationProvider.getCurrentLatitude() ?: 0.0
        )
        val heading = directionProvider.getCurrentDirection()
        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val engine = this
        CoroutineScope(Job()).launch(treeContext) {
            streetPreview.go(location, heading, engine)
        }
        val end = System.currentTimeMillis()
        Log.d(TAG, "streetPreviewGo: ${end-start}ms")
    }

    companion object {
        private const val TAG = "GeoEngine"
    }
}
