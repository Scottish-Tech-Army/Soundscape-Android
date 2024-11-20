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
import org.scottishtecharmy.soundscape.network.ProtomapsTileClient
import org.scottishtecharmy.soundscape.network.SoundscapeBackendTileClient
import org.scottishtecharmy.soundscape.network.TileClient
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.checkIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.geoengine.utils.deduplicateFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.geoengine.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionLabel
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.processTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.processTileString
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import retrofit2.awaitResponse
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

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

    // HTTP connection to soundscape-backend or protomaps tile server
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

        tileClient = if(SOUNDSCAPE_TILE_BACKEND) {
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
                                                  centralBoundingBox)
                    ) {
                        Log.d(TAG, "Update central grid area")
                        // The current location has moved from within the central area, so get the
                        // new grid and the new central area.
                        val tileGrid = getTileGrid(
                            locationProvider.getCurrentLatitude() ?: 0.0,
                            locationProvider.getCurrentLongitude() ?: 0.0
                        )

                        // We have a new centralBoundingBox, so update the tiles
                        if(updateTileGrid(tileGrid)) {
                            // We have got a new grid, so create our new central region
                            centralBoundingBox = tileGrid.centralBoundingBox

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
                }
            }
        }
    }

    private suspend fun updateTileFromProtomaps(x : Int, y: Int, quadkey: String, update: Boolean) : Boolean {
        var ret = false
        withContext(Dispatchers.IO) {
            try {
                val service =
                    tileClient.retrofitInstance?.create(ITileDAO::class.java)
                val tileReq = async {
                    service?.getVectorTileWithCache(x, y, ZOOM_LEVEL)
                }
                val result = tileReq.await()?.awaitResponse()?.body()
                if (result != null) {
                    val tileFeatureCollection = vectorTileToGeoJson(x, y, result)
                    val tileData = processTileFeatureCollection(tileFeatureCollection, quadkey)
                    if (update)
                        tilesRepository.updateTile(tileData)
                    else
                        tilesRepository.insertTile(tileData)

                    ret = true
                }
                else {
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

    private suspend fun updateTileFromSoundscapeBackend(x : Int, y: Int, quadkey: String, update: Boolean) : Boolean {
        var ret = false

        withContext(Dispatchers.IO) {
            try {
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

                    ret = true
                }
                else {
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

    private suspend fun updateTile(x : Int, y: Int, quadkey: String, update: Boolean) : Boolean {
        if(!SOUNDSCAPE_TILE_BACKEND) {
            return updateTileFromProtomaps(x, y, quadkey, update)
        }
        return updateTileFromSoundscapeBackend(x, y, quadkey, update)
    }

    private suspend fun updateTileGrid(tileGrid : TileGrid) : Boolean {
        for (tile in tileGrid.tiles) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            var fetchTile = false
            var update = false
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db
            if (frozenResult.isEmpty()) {
                fetchTile = true
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
                    fetchTile = true
                    update = true
                }
            }
            if(fetchTile) {
                var ret = false
                for(retry in 1..5) {
                    ret = updateTile(tile.tileX, tile.tileY, tile.quadkey, update)
                    if(ret) {
                        break
                    }
                }
                if(!ret) {
                    return false
                }
            }
        }
        return true
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
            roadGridFeatureCollection.features.addAll(gridFeatureCollections[Fc.ROADS.id])

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
                if(nearestRoad.features.isNotEmpty()) {
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
            gridPoiFeatureCollection.features.addAll(gridFeatureCollections[Fc.POIS.id])

            if (gridPoiFeatureCollection.features.isNotEmpty()) {
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
                                    // We never want to add a feature more than once
                                    var found = false
                                    for (property in feature.properties!!) {
                                        for (featureType in superCategoryList) {
                                            if (property.value == featureType) {
                                                tempFeatureCollection.features.add(feature)
                                                found = true
                                            }
                                            if(found) break
                                        }
                                        if(found) break
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
                        } else if (feature.geometry is Point) {
                            if (feature.properties?.get("name") != null) {
                                val point = feature.geometry as Point
                                val d = distance(locationProvider.getCurrentLatitude() ?: 0.0,
                                                 locationProvider.getCurrentLongitude() ?: 0.0,
                                                  point.coordinates.latitude,
                                                  point.coordinates.longitude).toInt()
                                val text = "${feature.properties?.get("name")}. $d meters."
                                results.add(PositionedString(text, point.coordinates))
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

            roadsGridFeatureCollection.features.addAll(gridFeatureCollections[Fc.ROADS.id])
            intersectionsGridFeatureCollection.features.addAll(gridFeatureCollections[Fc.INTERSECTIONS.id])
            crossingsGridFeatureCollection.features.addAll(gridFeatureCollections[Fc.CROSSINGS.id])
            busStopsGridFeatureCollection.features.addAll(gridFeatureCollections[Fc.BUS_STOPS.id])

            if (roadsGridFeatureCollection.features.isNotEmpty()) {
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

                if (fovRoadsFeatureCollection.features.isNotEmpty()) {
                    val nearestRoad = getNearestRoad(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovRoadsFeatureCollection
                    )
                    // TODO check for Settings, Unnamed roads on/off here
                    if (nearestRoad.features.isNotEmpty()) {
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
                    }

                    if (fovIntersectionsFeatureCollection.features.isNotEmpty()) {

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
                        if (intersectionsNeedsFurtherCheckingFC.features.isNotEmpty()) {
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
                            if(newIntersectionFeatureCollection.features.isNotEmpty()) {
                                val intersectionLocation =
                                    newIntersectionFeatureCollection.features[0].geometry as Point
                                val intersectionRelativeDirections = getRelativeDirectionsPolygons(
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
                                    newIntersectionFeatureCollection,
                                    fovRoadsFeatureCollection
                                )
                                results.add(
                                    "${localizedContext.getString(R.string.intersection_approaching_intersection)} ${
                                        localizedContext.getString(
                                            R.string.distance_format_meters,
                                            distanceToNearestIntersection.toInt().toString()
                                        )
                                    }"
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
                    }
                }
                // detect if there is a crossing in the FOV
                if (fovCrossingsFeatureCollection.features.isNotEmpty()) {

                    val nearestCrossing = getNearestIntersection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovCrossingsFeatureCollection
                    )
                    if (nearestCrossing.features.isNotEmpty()) {
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
                        if (nearestRoadToCrossing.features.isNotEmpty()) {
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
                                if (nearestRoadToCrossing.features[0].properties?.get("name") != null) {
                                    append(nearestRoadToCrossing.features[0].properties?.get("name"))
                                }
                            }
                            results.add(crossingText)
                        }
                    }
                }

                // detect if there is a bus_stop in the FOV
                if (fovBusStopsFeatureCollection.features.isNotEmpty()) {
                    val nearestBusStop = getNearestIntersection(
                        LngLatAlt(
                            locationProvider.getCurrentLongitude() ?: 0.0,
                            locationProvider.getCurrentLatitude() ?: 0.0
                        ),
                        fovBusStopsFeatureCollection
                    )
                    if (nearestBusStop.features.isNotEmpty()) {
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
                        if(nearestRoadToBus.features.isNotEmpty()) {
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
                                if (nearestRoadToBus.features[0].properties?.get("name") != null) {
                                    append(nearestRoadToBus.features[0].properties?.get("name"))
                                }
                            }
                            results.add(busText)
                        }
                    }
                }
            } else {
                results.add(
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                )

            }
        }
        return results
    }

    private enum class Fc(val id: Int) {
        ROADS(0),
        INTERSECTIONS(1),
        CROSSINGS(2),
        POIS(3),
        BUS_STOPS(4),
        INTERPOLATIONS(5),
        MAX_COLLECTION_ID(6)
    }

    private fun getGridFeatureCollections(): List<FeatureCollection> {
        val results : MutableList<FeatureCollection> = mutableListOf()
        val tileGrid = getTileGrid(
            locationProvider.getCurrentLatitude() ?: 0.0,
            locationProvider.getCurrentLongitude() ?: 0.0
        )
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()

        val processedOsmIds = Array(Fc.MAX_COLLECTION_ID.id) { mutableSetOf<Any>() }
        val gridFeatureCollection = Array(Fc.MAX_COLLECTION_ID.id) { FeatureCollection() }

        val joiner = InterpolatedPointsJoiner()
        for (tile in tileGrid.tiles) {
            //Check the db for the tile
            val frozenTileResult =
                tileDataRealm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
            if (frozenTileResult != null) {
                val featureCollection = Array<FeatureCollection?>(Fc.MAX_COLLECTION_ID.id) { null }
                featureCollection[Fc.ROADS.id] = frozenTileResult.roads.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                featureCollection[Fc.INTERSECTIONS.id] = frozenTileResult.intersections.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                featureCollection[Fc.CROSSINGS.id] = frozenTileResult.crossings.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                featureCollection[Fc.POIS.id] = frozenTileResult.pois.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                featureCollection[Fc.BUS_STOPS.id] = frozenTileResult.busStops.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                featureCollection[Fc.INTERPOLATIONS.id] = frozenTileResult.interpolations.let {
                    moshi.adapter(FeatureCollection::class.java).fromJson(it)
                }
                for(ip in featureCollection[Fc.INTERPOLATIONS.id]!!) {
                    joiner.addInterpolatedPoints(ip)
                }

                if(SOUNDSCAPE_TILE_BACKEND) {
                    for ((index, fc) in featureCollection.withIndex())
                        deduplicateFeatureCollection(
                            gridFeatureCollection[index],
                            fc,
                            processedOsmIds[index]
                        )
                } else {
                    for ((index, fc) in featureCollection.withIndex())
                        gridFeatureCollection[index].plusAssign(fc!!)
                }
            }
        }
        for(fc in gridFeatureCollection)
            results.add(fc)
        joiner.addJoiningLines(results[Fc.ROADS.id])

        return results
    }

    companion object {
        private const val TAG = "GeoEngine"

        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60
    }
}