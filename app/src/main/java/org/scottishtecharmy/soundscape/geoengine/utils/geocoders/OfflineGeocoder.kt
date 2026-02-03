package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.toLocationDescription

/**
 * The OfflineGeocoder class abstracts away the use of map tile data on the phone for geocoding and
 * reverse geocoding. If the map tiles are present on the device already, this can be used without
 * any Internet connection.
 */
class OfflineGeocoder(
    val gridState: GridState,
    val settlementGrid: GridState,
    val tileSearch: TileSearch? = null
) : SoundscapeGeocoder() {

    fun addNamesFromGrid(treeId: TreeId, names: MutableSet<String>) {
        val features = settlementGrid.getFeatureTree(treeId).getAllCollection()
        for (feature in features) {
            val name = (feature as MvtFeature).name
            if(name != null) {
                names.add(normalizeForSearch(name))
            }
        }
    }

    fun getSettlementNames() : Set<String> {
        val names = mutableSetOf<String>()

        addNamesFromGrid(TreeId.SETTLEMENT_CITY, names)
        addNamesFromGrid(TreeId.SETTLEMENT_TOWN, names)
        addNamesFromGrid(TreeId.SETTLEMENT_VILLAGE, names)
        addNamesFromGrid(TreeId.SETTLEMENT_HAMLET, names)

        return names
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedContext: Context?    ) : List<LocationDescription>? {
        Analytics.getInstance().logEvent("offlineGeocode", null)

        val settlementNames = withContext(gridState.treeContext) {
            getSettlementNames()
        }
        return tileSearch?.search(nearbyLocation, locationName, localizedContext, settlementNames)
    }

    private fun getNearestPointOnFeature(feature: Feature,
                                         location: LngLatAlt) : LngLatAlt {
        return getDistanceToFeature(location, feature, gridState.ruler).point
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry,
                                              localizedContext: Context?,
                                              ignoreHouseNumbers: Boolean) : LocationDescription? {

        val location = userGeometry.location
        // We can only use the local geocoder for local locations
        if(!gridState.isLocationWithinGrid(location))
            return null

        Analytics.getInstance().logEvent("offlineReverseGeocode", null)

        var nearbyWay = userGeometry.mapMatchedWay
        if(nearbyWay == null) {
            // We're not map matched, so find the nearest way by searching
            val ways = gridState.getFeatureTree(TreeId.ROADS)
                .getNearestCollection(
                    location,
                    50.0,
                    5,
                    userGeometry.ruler
                )
            for(way in ways) {
                if((way as Way).name != null) {
                    nearbyWay = way
                    break
                }
            }
        }
        if(nearbyWay != null) {
            val nearbyName = nearbyWay.properties?.get("pavement") as String? ?: nearbyWay.name
            if(nearbyName != null) {
                val description = StreetDescription(nearbyName, gridState)
                description.createDescription(nearbyWay, localizedContext)
                val nearestWay = description.nearestWayOnStreet(userGeometry.location)
                if ((nearestWay != null) && !ignoreHouseNumbers) {
                    val houseNumber =
                        description.getStreetNumber(nearestWay.first, userGeometry.location)
                    if(houseNumber.first.isNotEmpty()) {
                        // We've got a street number
                        val houseFeature = MvtFeature()
                        houseFeature.properties = hashMapOf()
                        houseFeature.properties?.let { props ->
                            props["housenumber"] = houseNumber.first
                            props["street"] = nearbyName
                            props["opposite"] = houseNumber.second
                        }
                        houseFeature.geometry = Point(userGeometry.location)
                        return houseFeature.toLocationDescription(LocationSource.OfflineGeocoder)
                    }
                }
                // We couldn't get a street address, so try a descriptive address instead
                val heading = userGeometry.heading()
                val result = description.describeLocation(
                    userGeometry.location,
                    heading,
                    nearestWay?.first,
                    localizedContext
                )
                var text = ""
                val formattedBehindDistance = formatDistanceAndDirection(result.behind.distance, null, localizedContext)
                val formattedAheadDistance = formatDistanceAndDirection(result.ahead.distance, null, localizedContext)
                if (
                    (result.ahead.distance < 10.0) &&
                    ((result.ahead.distance < result.behind.distance) || result.behind.name.isEmpty()))
                {
                    // If this is a street address, then it already includes the street name. Otherwise
                    // we want to add that in.
                    val formatString = (localizedContext?.getString(R.string.street_description_relative_before) ?:
                    "On %s just before %s")
                    text = formatString.format(nearbyName, result.ahead.name)
                }
                else if (result.behind.distance < 10.0) {
                    val formatString = (localizedContext?.getString(R.string.street_description_relative_after) ?:
                    "On %s just after %s")
                    text = formatString.format(nearbyName, result.behind.name)
                }
                else if(result.ahead.name.isNotEmpty() && result.behind.name.isNotEmpty()) {
                    val formatString = (localizedContext?.getString(R.string.street_description_between) ?:
                                        "On %s between %s and %s")
                    text = formatString.format(nearbyName, result.behind.name, result.ahead.name)
                } else {
                    if(result.ahead.name.isNotEmpty()) {
                        val formatString = (localizedContext?.getString(R.string.street_description_until) ?:
                        "On %s, %s until %s")
                        text = formatString.format(nearbyName, formattedAheadDistance, result.ahead.name)
                    }
                    else if(result.behind.name.isNotEmpty()) {
                        val formatString = (localizedContext?.getString(R.string.street_description_since) ?:
                        "On %s, %s since %s")
                        text = formatString.format(nearbyName, formattedBehindDistance, result.behind.name)
                    }
                }
                if(text.isNotEmpty()) {
                    return LocationDescription(text, userGeometry.location)
                }
            }
        }

        // Check if we're near a bus/tram/train stop. This is useful when travelling on public transport
        val busStopTree = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
        val nearestBusStop = busStopTree.getNearestFeature(location, gridState.ruler, 20.0)
        if(nearestBusStop != null) {
            val busStopText = getTextForFeature(null, nearestBusStop as MvtFeature)
            return LocationDescription(
                name = busStopText.text,
                location = getNearestPointOnFeature(nearestBusStop, location)
            )
        }

        // Check if we're inside a POI
        val gridPoiTree = gridState.getFeatureTree(TreeId.POIS)
        val insidePois = gridPoiTree.getContainingPolygons(location)
        insidePois.forEach { poi ->
            val mvt = poi as MvtFeature
            if(!mvt.name.isNullOrEmpty()) {
                val featureText = getTextForFeature(null, mvt)
                return LocationDescription(
                    name = featureText.text,
                    location = getNearestPointOnFeature(mvt, location)
                )
            }
        }

        // See if there are any nearby named POI
        val nearbyPois = gridPoiTree.getNearestCollection(location, 300.0, 10, gridState.ruler, null)
        nearbyPois.forEach { poi ->
            val mvt = poi as MvtFeature
            if(!mvt.name.isNullOrEmpty()) {
                return LocationDescription(
                    name = getTextForFeature(null, mvt).text,
                    location = getNearestPointOnFeature(mvt, location),
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
        var nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
            .getNearestFeature(location, settlementGrid.ruler, 1000.0) as MvtFeature?
        var nearestSettlementName = nearestSettlement?.name
        if(nearestSettlementName == null) {
            nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE)
                .getNearestFeature(location, settlementGrid.ruler, 2000.0) as MvtFeature?
            nearestSettlementName = nearestSettlement?.name
            if(nearestSettlementName == null) {
                nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_TOWN)
                    .getNearestFeature(location, settlementGrid.ruler, 4000.0) as MvtFeature?
                nearestSettlementName = nearestSettlement?.name
                if (nearestSettlementName == null) {
                    nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_CITY)
                        .getNearestFeature(location, settlementGrid.ruler, 15000.0) as MvtFeature?
                    nearestSettlementName = nearestSettlement?.name
                }
            }
        }

        // Check if the location is alongside a road/path
        val nearestRoad = gridState.getNearestFeature(TreeId.WAYS_SELECTION, gridState.ruler, location, 100.0) as Way?
        if(nearestRoad != null) {
            // We only want 'interesting' non-generic names i.e. no "Path" or "Service"
            val roadName = nearestRoad.getName(null, gridState, null, true)
            if(roadName.isNotEmpty()) {
                return if(nearestSettlementName != null) {
                    LocationDescription(
                        name = roadName,
                        location = location
                    )
                } else {
                    LocationDescription(
                        name = roadName,
                        location = location,
                    )
                }
            }
        }

        if(nearestSettlementName != null) {
            //val distanceToSettlement = settlementGrid.ruler.distance(location, (nearestSettlement?.geometry as Point).coordinates)
            return LocationDescription(
                name = nearestSettlementName,
                location = location,
            )
        }

        return null
    }

    companion object {
        const val TAG = "OfflineGeocoder"
    }
}