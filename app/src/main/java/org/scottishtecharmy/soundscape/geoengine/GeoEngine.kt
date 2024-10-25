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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.TilesDao
import org.scottishtecharmy.soundscape.database.local.model.TileData
import org.scottishtecharmy.soundscape.database.repository.TilesRepository
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
import org.scottishtecharmy.soundscape.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.utils.distanceToIntersection
import org.scottishtecharmy.soundscape.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.utils.get3x3TileGrid
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
import org.scottishtecharmy.soundscape.utils.processTileString
import org.scottishtecharmy.soundscape.utils.removeDuplicateOsmIds
import retrofit2.awaitResponse
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PositionedString(val text : String, val location : LngLatAlt? = null)

class GeoEngine {

    private val coroutineScope = CoroutineScope(Job())

    // GeoJSON tiles job
    private var tilesJob: Job? = null

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

    private fun startTileGridService() {
        tilesJob?.cancel()
        tilesJob = coroutineScope.launch {
            tilesFlow(30.seconds)
                .collectLatest {
                    withContext(Dispatchers.IO) {
                        getTileGrid()
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

    private suspend fun getTileGrid() {

        val tileGridQuadKeys = get3x3TileGrid(
            locationProvider.getCurrentLatitude() ?: 0.0,
            locationProvider.getCurrentLongitude() ?: 0.0
        )

        for (tile in tileGridQuadKeys) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            val frozenResult = tilesRepository.getTile(tile.quadkey)
            // If Tile doesn't already exist in db go and get it, clean it, process it
            // and insert into db
            if (frozenResult.size == 0) {
                withContext(Dispatchers.IO) {
                    val service =
                        tileClient.retrofitInstance?.create(ITileDAO::class.java)
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
                            tileClient.retrofitInstance?.create(ITileDAO::class.java)
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
                    tileDataRealm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
                if (frozenResult != null) {
                    val roadsString = frozenResult.roads

                    val roadsFeatureCollection = roadsString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }
                    roadsFeatureCollection?.let { collection ->
                        for (feature in collection.features) {
                            val osmId = feature.foreign?.get("osm_ids")
                            //Log.d(TAG, "osmId: $osmId")
                            if (osmId != null && !processedOsmIds.contains(osmId)) {
                                processedOsmIds.add(osmId)
                                gridFeatureCollection.features.add(feature)
                            }
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
        //  decide on how to calculate distance to POI, setup settings in the menu so we can pass in the filters, etc.
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
                    tileDataRealm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
                if (frozenTileResult != null) {
                    val poiString = frozenTileResult.pois
                    val poiFeatureCollection = poiString.let {
                        moshi.adapter(FeatureCollection::class.java).fromJson(
                            it
                        )
                    }

                    poiFeatureCollection?.let { collection ->
                        for (feature in collection.features) {
                            val osmId = feature.foreign?.get("osm_ids")
                            //Log.d(TAG, "osmId: $osmId")
                            if (osmId != null && !processedOsmIds.contains(osmId)) {
                                processedOsmIds.add(osmId)
                                gridFeatureCollection.features.add(feature)
                            }
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
                    tileDataRealm.query<TileData>("quadKey == $0", tile.quadkey).first().find()
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

                    roadFeatureCollection?.let { collection ->
                        for (feature in collection.features) {
                            val osmId = feature.foreign?.get("osm_ids")
                            //Log.d(TAG, "osmId: $osmId")
                            if (osmId != null && !processedRoadOsmIds.contains(osmId)) {
                                processedRoadOsmIds.add(osmId)
                                roadGridFeatureCollection.features.add(feature)
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
                }
            }

            if (roadGridFeatureCollection.features.size > 0) {

                val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation.toDouble(),
                    fovDistance,
                    roadGridFeatureCollection
                )
                val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
                    LngLatAlt(
                        locationProvider.getCurrentLongitude() ?: 0.0,
                        locationProvider.getCurrentLatitude() ?: 0.0
                    ),
                    orientation.toDouble(),
                    fovDistance,
                    intersectionsGridFeatureCollection
                )

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
                        results.add(
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
                            if (direction != null) {
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
                } else {
                    results.add(
                        localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                    )
                }
            } else {
                results.add(
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
                )

            }
        }
        return results
    }

    companion object {
        private const val TAG = "GeoEngine"

        // TTL Tile refresh in local Realm DB
        private const val TTL_REFRESH_SECONDS: Long = 24 * 60 * 60
    }
}