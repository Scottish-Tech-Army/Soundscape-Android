package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.nearestSettlement
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription

/**
 * The OfflineGeocoder class abstracts away the use of map tile data on the phone for geocoding and
 * reverse geocoding. If the map tiles are present on the device already, this can be used without
 * any Internet connection.
 */
class OfflineGeocoder(
    val gridState: GridState,
    val settlementGrid: GridState,
    val tileSearch: TileSearcher? = null,
    private val analyticsLogger: (String) -> Unit = {},
    private val processor: (LocationDescription) -> Unit = {},
) : SoundscapeGeocoder() {

    // Cache of the last StreetDescription built by getAddressFromLngLat(), keyed by street name
    // and the GridState generation it was built from. createDescription() walks the whole
    // street's way graph plus every nearby house-number/POI tree, which isn't cheap - a caller
    // that repeatedly queries the same street against an unchanged grid (e.g. pressing "My
    // Location" more than once while stationary) shouldn't pay that cost again.
    internal var cachedStreetDescription: StreetDescription? = null
    internal var cachedStreetGeneration: Int = -1

    internal fun getOrBuildStreetDescription(
        streetName: String,
        nearbyWay: Way,
        localizedStrings: LocalizedStrings?
    ): StreetDescription {
        val cached = cachedStreetDescription
        if (cached != null &&
            cachedStreetGeneration == gridState.generation &&
            cached.name == streetName &&
            cached.ways.any { it.first == nearbyWay }
        ) {
            return cached
        }

        val description = StreetDescription(streetName, gridState)
        description.createDescription(nearbyWay, localizedStrings)
        cachedStreetDescription = description
        cachedStreetGeneration = gridState.generation
        return description
    }

    fun addNamesFromGrid(treeId: TreeId, names: MutableSet<String>) {
        val features = settlementGrid.getFeatureTree(treeId).getAllCollection()
        for (feature in features) {
            val name = (feature as MvtFeature).name
            if (name != null) {
                names.add(normalizeForSearch(name))
            }
        }
    }

    fun getSettlementNames(): Set<String> {
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
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription>? {
        analyticsLogger("offlineGeocode")

        val settlementNames = withContext(gridState.treeContext) {
            getSettlementNames()
        }
        return tileSearch?.search(nearbyLocation, locationName, localizedStrings, settlementNames)
    }

    private fun getNearestPointOnFeature(
        feature: Feature,
        location: LngLatAlt
    ): LngLatAlt {
        return getDistanceToFeature(location, feature, gridState.ruler).point
    }

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? {

        val location = userGeometry.location
        // We can only use the local geocoder for local locations
        if (!gridState.isLocationWithinGrid(location))
            return null

        analyticsLogger("offlineReverseGeocode")

        var nearbyWay = userGeometry.mapMatchedWay
        if (nearbyWay == null) {
            // We're not map matched, so find the nearest way by searching
            val ways = gridState.getFeatureTree(TreeId.ROADS)
                .getNearestCollection(
                    location,
                    50.0,
                    5,
                    userGeometry.ruler
                )
            for (way in ways) {
                if ((way as Way).name != null) {
                    nearbyWay = way
                    break
                }
            }
        }
        if (nearbyWay != null) {
            val nearbyName = (nearbyWay.properties?.get("pavement") as String?)
                .takeUnless { it.isNullOrEmpty() }
                ?: nearbyWay.getName(null, gridState, localizedStrings)
            if (nearbyName != null) {
                val description = getOrBuildStreetDescription(nearbyName, nearbyWay, localizedStrings)
                val nearestWay = description.nearestWayOnStreet(userGeometry.location)
                if ((nearestWay != null) && !ignoreHouseNumbers) {
                    val houseNumber =
                        description.getStreetNumber(nearestWay.first, userGeometry.location)
                    if (houseNumber.first.isNotEmpty()) {
                        // We've got a street number
                        val houseFeature = MvtFeature()
                        houseFeature.properties = hashMapOf()
                        houseFeature.properties?.let { props ->
                            props["housenumber"] = houseNumber.first
                            props["street"] = nearbyName
                            props["opposite"] = houseNumber.second
                        }
                        houseFeature.geometry = Point(userGeometry.location)
                        return houseFeature.deferredToLocationDescription(LocationSource.OfflineGeocoder)
                            .also(processor)
                    }
                }
                // We couldn't get a street address, so try a descriptive address instead
                val heading = userGeometry.heading()
                val result = description.describeLocation(
                    userGeometry.location,
                    heading,
                    nearestWay?.first,
                    localizedStrings
                )
                var text = ""
                val formattedBehindDistance =
                    formatDistanceAndDirection(result.behind.distance, null, localizedStrings)
                val formattedAheadDistance =
                    formatDistanceAndDirection(result.ahead.distance, null, localizedStrings)
                // "just before"/"just after"/"until"/"since" all place the location relative to
                // the direction of travel, and describeLocation only knows which way that is from
                // a heading - without one it fills in ahead and behind arbitrarily, so those
                // phrasings would be actively misleading. Describing a point rather than someone
                // moving along a street (an address for somewhere the user tapped) therefore only
                // uses the forms which read the same in either direction: "between", and "near"
                // in place of "just before"/"just after".
                val directional = (heading != null)
                val closest = listOf(result.ahead, result.behind)
                    .filter { it.name.isNotEmpty() }
                    .minByOrNull { it.distance }
                if (
                    directional &&
                    (result.ahead.distance < 10.0) &&
                    ((result.ahead.distance < result.behind.distance) || result.behind.name.isEmpty())
                ) {
                    // If this is a street address, then it already includes the street name. Otherwise
                    // we want to add that in.
                    text = localizedStrings?.get(
                        StringKey.StreetDescriptionRelativeBefore, nearbyName, result.ahead.name
                    ) ?: "On $nearbyName just before ${result.ahead.name}"
                } else if (directional && (result.behind.distance < 10.0)) {
                    text = localizedStrings?.get(
                        StringKey.StreetDescriptionRelativeAfter, nearbyName, result.behind.name
                    ) ?: "On $nearbyName just after ${result.behind.name}"
                } else if (!directional && (closest != null) && (closest.distance < 10.0)) {
                    text = localizedStrings?.get(
                        StringKey.StreetDescriptionRelativeNear, nearbyName, closest.name
                    ) ?: "On $nearbyName near ${closest.name}"
                } else if (result.ahead.name.isNotEmpty() && result.behind.name.isNotEmpty()) {
                    text = localizedStrings?.get(
                        StringKey.StreetDescriptionBetween,
                        nearbyName,
                        result.behind.name,
                        result.ahead.name
                    ) ?: "On $nearbyName between ${result.behind.name} and ${result.ahead.name}"
                } else if (directional) {
                    if (result.ahead.name.isNotEmpty()) {
                        text = localizedStrings?.get(
                            StringKey.StreetDescriptionUntil,
                            nearbyName,
                            formattedAheadDistance,
                            result.ahead.name
                        ) ?: "On $nearbyName, $formattedAheadDistance until ${result.ahead.name}"
                    } else if (result.behind.name.isNotEmpty()) {
                        text = localizedStrings?.get(
                            StringKey.StreetDescriptionSince,
                            nearbyName,
                            formattedBehindDistance,
                            result.behind.name
                        ) ?: "On $nearbyName, $formattedBehindDistance since ${result.behind.name}"
                    }
                }
                if (text.isNotEmpty()) {
                    return LocationDescription(text, userGeometry.location)
                }
            }
        }

        // Check if we're near a bus/tram/train stop. This is useful when travelling on public transport
        val busStopTree = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
        val nearestBusStop = busStopTree.getNearestFeature(location, gridState.ruler, 20.0)
        if (nearestBusStop != null) {
            val busStopText = (nearestBusStop as MvtFeature).getText(localizedStrings)
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
            if (!mvt.name.isNullOrEmpty()) {
                val featureText = mvt.getText(localizedStrings)
                return LocationDescription(
                    name = featureText.text,
                    location = getNearestPointOnFeature(mvt, location)
                )
            }
        }

        // See if there are any nearby named POI
        val nearbyPois = gridPoiTree.getNearestCollection(location, 300.0, 10, gridState.ruler)
        nearbyPois.forEach { poi ->
            val mvt = poi as MvtFeature
            if (!mvt.name.isNullOrEmpty()) {
                return LocationDescription(
                    name = mvt.getText(localizedStrings).text,
                    location = getNearestPointOnFeature(mvt, location),
                )
            }
        }

        val nearestSettlementName = nearestSettlement(settlementGrid, location).name

        // Check if the location is alongside a road/path
        val nearestRoad = gridState.getNearestFeature(
            TreeId.WAYS_SELECTION,
            gridState.ruler,
            location,
            100.0
        ) as Way?
        if (nearestRoad != null) {
            // We only want 'interesting' non-generic names i.e. no "Path" or "Service"
            val roadName = nearestRoad.getName(null, gridState, null, true)
            if (roadName.isNotEmpty()) {
                return if (nearestSettlementName != null) {
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

        if (nearestSettlementName != null) {
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