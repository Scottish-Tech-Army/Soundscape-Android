package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.MAP_DEBUG_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.TilesDao
import org.scottishtecharmy.soundscape.database.local.model.TileData
import org.scottishtecharmy.soundscape.database.repository.TilesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.TileClient
import org.scottishtecharmy.soundscape.utils.RelativeDirections
import org.scottishtecharmy.soundscape.utils.TileGrid
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.ZOOM_LEVEL
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.utils.checkIntersection
import org.scottishtecharmy.soundscape.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionLabel
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.utils.processTileString
import org.scottishtecharmy.soundscape.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.utils.sortedByDistanceTo
import retrofit2.awaitResponse
import java.util.Locale

data class PositionedString(val text : String, val location : LngLatAlt? = null)

class GeoEngine {

    private val coroutineScope = CoroutineScope(Job())

    // GeoJSON tiles job
    private var tilesJob: Job? = null

    // Flow to return current tile grid GeoJSON
    private val _tileGridFlow = MutableStateFlow(TileGrid(mutableListOf(), BoundingBox()))
    var tileGridFlow: StateFlow<TileGrid> = _tileGridFlow

    // Realms
    private var tileDataRealm: Realm = RealmConfiguration.getTileDataInstance()
    private val tilesDao : TilesDao = TilesDao(tileDataRealm)
    private val tilesRepository : TilesRepository = TilesRepository(tilesDao)

    // HTTP connection to soundscape-backend tile server
    private lateinit var tileClient : TileClient

    private lateinit var locationProvider : LocationProvider
    private lateinit var directionProvider : DirectionProvider

    // Resource string locale configuration
    private lateinit var configLocale : Locale
    private lateinit var configuration : Configuration
    private lateinit var localizedContext : Context

    private lateinit var sharedPreferences : SharedPreferences

    private var centralBoundingBox = BoundingBox()

    fun start(application: Application,
              newLocationProvider: LocationProvider,
              newDirectionProvider: DirectionProvider) {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)

        tileClient = TileClient(application)
        configLocale = getCurrentLocale()
        configuration = Configuration(application.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = application.applicationContext.createConfigurationContext(configuration)

        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        startTileGridService()
    }

    fun stop() {
        tilesJob?.cancel()
        locationProvider.destroy()
        directionProvider.destroy()
    }

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    private fun startTileGridService() {
        Log.e(TAG, "startTileGridService")
        tilesJob?.cancel()
        tilesJob = coroutineScope.launch {
            locationProvider.locationFlow.collectLatest { newLocation ->
                // Check if we've moved out of the bounds of the central area
                newLocation?.let { location ->
                    // Check if we're still within the central area of our grid
                    if (!pointIsWithinBoundingBox(LngLatAlt(location.longitude, location.latitude),
                                                  centralBoundingBox)) {
                        Log.d(TAG, "Update central grid area")
                        // The current location has moved from within the central area, so get the
                        // new grid and the new central area.
                        val tileGrid = getTileGrid(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0
                        )
                        centralBoundingBox = tileGrid.centralBoundingBox

                        // We have a new centralBoundingBox, so update the tiles
                        updateTileGrid(tileGrid)

                        // Update the flow with our new tile grid
                        if(sharedPreferences.getBoolean(MAP_DEBUG_KEY, false)) {
                            _tileGridFlow.value = tileGrid
                        }
                        else {
                            _tileGridFlow.value = TileGrid(mutableListOf(), BoundingBox())
                        }

                    }
                }
            }
        }
    }

    private suspend fun updateTile(x : Int, y: Int, quadkey: String, update: Boolean) {
        withContext(Dispatchers.IO) {
            val service =
                tileClient.retrofitInstance?.create(ITileDAO::class.java)
            val tileReq = async {
                    service?.getTileWithCache(x, y)
            }
            val result = tileReq.await()?.awaitResponse()?.body()
            // clean the tile, process the string, perform an insert into db using the clean tile data
            val cleanedTile =
                result?.let { cleanTileGeoJSON(x, y, ZOOM_LEVEL, it) }

            if (cleanedTile != null) {
                val tileData = processTileString(quadkey, cleanedTile)
                if(update)
                    tilesRepository.updateTile(tileData)
                else
                    tilesRepository.insertTile(tileData)
            }
        }
    }

    private suspend fun updateTileGrid(tileGrid : TileGrid) {
        for (tile in tileGrid.tiles) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db
            if (frozenResult.size == 0) {
                updateTile(tile.tileX, tile.tileY, tile.quadkey, false)
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
                    updateTile(tile.tileX, tile.tileY, tile.quadkey, true)
                }
            }
        }
    }

    fun myLocation() : List<String> {
        // getCurrentDirection() from the direction provider has a default of 0.0
        // even if we don't have a valid current direction.
        val results : MutableList<String> = mutableListOf()
        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            //Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(noLocationString)
        } else {
            val roadGridFeatureCollection = FeatureCollection()
            val gridFeatureCollections = getGridFeatureCollections()
            roadGridFeatureCollection.features.addAll(gridFeatureCollections[0])

            if (roadGridFeatureCollection.features.size > 0) {
                //Log.d(TAG, "Found roads in tile")
                val nearestRoad =
                    getNearestRoad(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        roadGridFeatureCollection
                    )
                val properties = nearestRoad.features[0].properties
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
                    results.add(facingDirectionAlongRoad)
                } else {
                    Log.e(TAG, "No properties found for road")
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
                results.add(facingDirection)
            }
        }
        return results
    }

    fun whatsAroundMe() : List<PositionedString> {
        // TODO This is just a rough POC at the moment. Lots more to do...
        //  setup settings in the menu so we can pass in the filters, etc.
        //  Original Soundscape just splats out a list in no particular order which is odd.
        //  If you press the button again in original Soundscape it can give you the same list but in a different sequence or
        //  it can add one to the list even if you haven't moved. It also only seems to give a thing and a distance but not a heading.
        val results : MutableList<PositionedString> = mutableListOf()

        // super categories are "information", "object", "place", "landmark", "mobility", "safety"
        val placesAndLandmarks = sharedPreferences.getBoolean(PLACES_AND_LANDMARKS_KEY, true)
        val mobility = sharedPreferences.getBoolean(MOBILITY_KEY, true)
        // TODO unnamed roads switch is not used yet
        //val unnamedRoads = sharedPrefs.getBoolean(UNNAMED_ROADS_KEY, false)

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(PositionedString(noLocationString))
        } else {

            val gridPoiFeatureCollection = FeatureCollection()
            val gridFeatureCollections = getGridFeatureCollections()
            gridPoiFeatureCollection.features.addAll(gridFeatureCollections[3])

            if (gridPoiFeatureCollection.features.size > 0) {
                val settingsFeatureCollection = FeatureCollection()
                if (placesAndLandmarks) {
                    if (mobility) {
                        //Log.d(TAG, "placesAndLandmarks and mobility are both true")
                        val placeSuperCategory =
                            getPoiFeatureCollectionBySuperCategory("place", gridPoiFeatureCollection)
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
                            getPoiFeatureCollectionBySuperCategory("place", gridPoiFeatureCollection)
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
                if (settingsFeatureCollection.features.size > 0) {
                    for (feature in settingsFeatureCollection) {
                        if (feature.geometry is Polygon) {
                            // found that if a thing has a name property that ends in a number
                            // "data 365" then the 365 and distance away get merged into a large number "365200 meters". Hoping a full stop will fix it
                            if (feature.properties?.get("name") != null) {
                                val text = "${feature.properties?.get("name")}.  ${
                                    distanceToPolygon(
                                            LngLatAlt(
                                                locationProvider.getCurrentLongitude() ?: 0.0,
                                                locationProvider.getCurrentLatitude() ?: 0.0
                                            ),
                                            feature.geometry as Polygon
                                        ).toInt()
                                    } meters."
                                // TODO: We want to play the speech out at a location representing
                                //  the polygon. Because we're calculating the nearest point, it
                                //  should be the location of that. For now we pass no location.
                                results.add(PositionedString(text/*, polygonAsPoint*/))
                            }
                        }
                    }
                } else {
                    results.add(PositionedString(localizedContext.getString(R.string.callouts_nothing_to_call_out_now)))
                }
            } else {
                Log.d(TAG, "No Points Of Interest found in the grid")
                results.add(PositionedString(localizedContext.getString(R.string.callouts_nothing_to_call_out_now)))
            }
        }
        return results
    }

    fun aheadOfMe() : List<String> {
        // TODO This is just a rough POC at the moment. Lots more to do...
        val results : MutableList<String> = mutableListOf()

        if (locationProvider.getCurrentLatitude() == null || locationProvider.getCurrentLongitude() == null) {
            // Should be null but let's check
            //Log.d(TAG, "Airplane mode On and GPS off. Current location: ${locationProvider.getCurrentLatitude()} , ${locationProvider.getCurrentLongitude()}")
            val noLocationString =
                localizedContext.getString(R.string.general_error_location_services_find_location_error)
            results.add(noLocationString)
        } else {
            // get device direction
            val orientation = directionProvider.getCurrentDirection().toDouble()
            val fovDistance = 50.0
            val roadsGridFeatureCollection = FeatureCollection()
            val intersectionsGridFeatureCollection = FeatureCollection()
            val crossingsGridFeatureCollection = FeatureCollection()
            val busStopsGridFeatureCollection = FeatureCollection()

            val gridFeatureCollections = getGridFeatureCollections()

            roadsGridFeatureCollection.features.addAll(gridFeatureCollections[0])
            intersectionsGridFeatureCollection.features.addAll(gridFeatureCollections[1])
            crossingsGridFeatureCollection.features.addAll(gridFeatureCollections[2])
            busStopsGridFeatureCollection.features.addAll(gridFeatureCollections[4])

            if (roadsGridFeatureCollection.features.size > 0) {
                val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation,
                    fovDistance,
                    roadsGridFeatureCollection
                )
                val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation,
                    fovDistance,
                    intersectionsGridFeatureCollection
                )
                val fovCrossingsFeatureCollection = getFovIntersectionFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation,
                    fovDistance,
                    crossingsGridFeatureCollection
                )
                val fovBusStopsFeatureCollection = getFovIntersectionFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation,
                    fovDistance,
                    busStopsGridFeatureCollection
                )

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
                        results.add(
                            "${localizedContext.getString(R.string.directions_direction_ahead)} ${nearestRoad.features[0].properties!!["name"]}"
                        )
                    } else {
                        // we are detecting an unnamed road here but pretending there is nothing here
                        results.add(
                            localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                        )
                    }

                    if (fovIntersectionsFeatureCollection.features.size > 0) {

                        val intersectionsSortedByDistance = sortedByDistanceTo(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            fovIntersectionsFeatureCollection
                        )

                        val testNearestRoad = getNearestRoad(
                            LngLatAlt(locationProvider.getCurrentLongitude() ?: 0.0,
                                locationProvider.getCurrentLatitude() ?: 0.0
                            ),
                            fovRoadsFeatureCollection
                        )
                        val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

                        for (i in 0 until intersectionsSortedByDistance.features.size) {
                            val testNearestIntersection = FeatureCollection()
                            testNearestIntersection.addFeature(intersectionsSortedByDistance.features[i])
                            val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
                            val intersectionsNeedsFurtherChecking = checkIntersection(i, intersectionRoadNames, testNearestRoad)
                            if(intersectionsNeedsFurtherChecking) {
                                intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[i])
                            }
                        }
                        // Approach 1: find the intersection feature with the most osm_ids and use that?
                        val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull {
                                feature ->
                            (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                        }
                        val newIntersectionFeatureCollection = FeatureCollection()
                        if (featureWithMostOsmIds != null) {
                            newIntersectionFeatureCollection.addFeature(featureWithMostOsmIds)
                        }

                        val nearestIntersection = getNearestIntersection(
                            LngLatAlt(locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0),
                            fovIntersectionsFeatureCollection
                        )
                        val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, orientation)
                        val intersectionLocation = newIntersectionFeatureCollection.features[0].geometry as Point
                        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
                            LngLatAlt(intersectionLocation.coordinates.longitude,
                                intersectionLocation.coordinates.latitude),
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
                        val intersectionRoadNames = getIntersectionRoadNames(newIntersectionFeatureCollection, fovRoadsFeatureCollection)
                        results.add(
                            "${localizedContext.getString(R.string.intersection_approaching_intersection)} ${localizedContext.getString(R.string.distance_format_meters, distanceToNearestIntersection.toInt().toString())}"
                        )

                        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
                            intersectionRoadNames,
                            newIntersectionFeatureCollection,
                            intersectionRelativeDirections
                        )
                        for (feature in roadRelativeDirections.features) {
                            val direction =
                                feature.properties?.get("Direction").toString().toIntOrNull()
                            // Don't call out the road we are on (0) as part of the intersection
                            if (direction != null && direction != 0) {
                                val relativeDirectionString =
                                    getRelativeDirectionLabel(
                                        localizedContext,
                                        direction,
                                        configLocale
                                    )
                                if (feature.properties?.get("name") != null) {
                                    val intersectionCallout = localizedContext.getString(
                                        R.string.directions_intersection_with_name_direction,
                                        feature.properties?.get("name"),
                                        relativeDirectionString
                                    )
                                    results.add(
                                        intersectionCallout
                                    )
                                }
                            }
                        }
                    }
                }
                // detect if there is a crossing in the FOV
                if (fovCrossingsFeatureCollection.features.size > 0) {

                    val nearestCrossing = getNearestIntersection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovCrossingsFeatureCollection
                    )
                    val crossingLocation = nearestCrossing.features[0].geometry as Point
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
                    val crossingText = buildString {
                        append(localizedContext.getString(R.string.osm_tag_crossing))
                        append(". ")
                        append(localizedContext.getString(R.string.distance_format_meters, distanceToCrossing.toInt().toString()))
                        append(". ")
                        if (nearestRoadToCrossing.features[0].properties?.get("name") != null){
                            append(nearestRoadToCrossing.features[0].properties?.get("name"))
                        }
                    }
                    results.add(crossingText)
                }

                // detect if there is a bus_stop in the FOV
                if (fovBusStopsFeatureCollection.features.size > 0) {
                    val nearestBusStop = getNearestIntersection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovBusStopsFeatureCollection
                    )
                    val busStopLocation = nearestBusStop.features[0].geometry as Point
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
                    val busText = buildString {
                        append(localizedContext.getString(R.string.osm_tag_bus_stop))
                        append(". ")
                        append(localizedContext.getString(R.string.distance_format_meters, distanceToBusStop.toInt().toString()))
                        append(". ")
                        if (nearestRoadToBus.features[0].properties?.get("name") != null){
                            append(nearestRoadToBus.features[0].properties?.get("name"))
                        }
                    }
                    results.add(busText)
                }

            } else {
                results.add(
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                )

            }
        }
        return results
    }

    private fun getGridFeatureCollections(): List<FeatureCollection> {
        val results : MutableList<FeatureCollection> = mutableListOf()
        val tileGrid = getTileGrid(
            locationProvider.getCurrentLatitude() ?: 0.0,
            locationProvider.getCurrentLongitude() ?: 0.0
        )
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val roadsGridFeatureCollection = FeatureCollection()
        val intersectionsGridFeatureCollection = FeatureCollection()
        val crossingsGridFeatureCollection = FeatureCollection()
        val poiGridFeatureCollection = FeatureCollection()
        val busGridFeatureCollection = FeatureCollection()

        val processedRoadOsmIds = mutableSetOf<Any>()
        val processedIntersectionOsmIds = mutableSetOf<Any>()
        val processedCrossingsOsmIds = mutableSetOf<Any>()
        val processedPoiOsmIds = mutableSetOf<Any>()
        val processedBusOsmIds = mutableSetOf<Any>()

        for (tile in tileGrid.tiles) {
            //Check the db for the tile
            val frozenTileResult =
                tileDataRealm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
            if (frozenTileResult != null) {
                val roadString = frozenTileResult.roads
                val intersectionsString = frozenTileResult.intersections
                val crossingsString = frozenTileResult.crossings
                val poiString = frozenTileResult.pois
                val busString = frozenTileResult.busStops

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
                val crossingsFeatureCollection = crossingsString.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(
                        it
                    )
                }
                val poiFeatureCollection = poiString.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(
                        it
                    )
                }
                val busFeatureCollection = busString.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(
                        it
                    )
                }

                roadFeatureCollection?.let { collection ->
                    for (feature in collection.features) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedRoadOsmIds.contains(osmId)) {
                            processedRoadOsmIds.add(osmId)
                            roadsGridFeatureCollection.features.add(feature)
                        }
                    }
                }

                intersectionsFeatureCollection?.let { collection ->
                    for (feature in collection.features) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedIntersectionOsmIds.contains(osmId)) {
                            processedIntersectionOsmIds.add(osmId)
                            intersectionsGridFeatureCollection.features.add(feature)
                        }
                    }
                }

                crossingsFeatureCollection?.let { collection ->
                    for (feature in collection.features) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedCrossingsOsmIds.contains(osmId)) {
                            processedCrossingsOsmIds.add(osmId)
                            crossingsGridFeatureCollection.features.add(feature)
                        }

                    }
                }
                poiFeatureCollection?.let { collection ->
                    for (feature in collection.features) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedPoiOsmIds.contains(osmId)) {
                            processedPoiOsmIds.add(osmId)
                            poiGridFeatureCollection.features.add(feature)
                        }
                    }
                }
                busFeatureCollection?.let { collection ->
                    for (feature in collection.features) {
                        val osmId = feature.foreign?.get("osm_ids")
                        //Log.d(TAG, "osmId: $osmId")
                        if (osmId != null && !processedBusOsmIds.contains(osmId)) {
                            processedBusOsmIds.add(osmId)
                            busGridFeatureCollection.features.add(feature)
                        }
                    }
                }
            }
        }

        // Not sure if this is my best plan as this is potentially quite a big splodge
        results.add(roadsGridFeatureCollection)
        results.add(intersectionsGridFeatureCollection)
        results.add(crossingsGridFeatureCollection)
        results.add(poiGridFeatureCollection)
        results.add(busGridFeatureCollection)

        return results
    }

    companion object {
        private const val TAG = "GeoEngine"

        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60
    }
}