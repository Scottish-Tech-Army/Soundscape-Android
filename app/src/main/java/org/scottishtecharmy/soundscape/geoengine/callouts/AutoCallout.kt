package org.scottishtecharmy.soundscape.geoengine.callouts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.ALLOW_CALLOUTS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.GeoEngine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

class AutoCallout(
    private val localizedContext: Context,
    private val sharedPreferences: SharedPreferences
) {


    private val locationFilter = LocationUpdateFilter(10000, 50.0)
    private val poiFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionCalloutHistory = CalloutHistory(30000)
    private val poiCalloutHistory = CalloutHistory()


    /** Reverse geocodes a location into 1 of 4 possible states
     * - within a POI
     * - alongside a road
     * - general location
     * - unknown location).
     */
    private fun reverseGeocode(userGeometry: UserGeometry,
                               gridState: GridState): List<PositionedString> {
        val results : MutableList<PositionedString> = mutableListOf()

        // Check if we're inside a POI
        val gridPoiCollection = gridState.getFeatureCollection(TreeId.POIS, userGeometry.location, 200.0)
        val gridPoiFeatureCollection = removeDuplicateOsmIds(gridPoiCollection)
        for(poi in gridPoiFeatureCollection) {
            // We can only be inside polygons
            if(poi.geometry.type == "Polygon") {
                val polygon = poi.geometry as Polygon

                if(polygonContainsCoordinates(userGeometry.location, polygon)) {
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
        val nearestRoad = gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, userGeometry.location, 100.0)
        if(nearestRoad != null) {
            val properties = nearestRoad.properties
            if (properties != null) {
                val orientation = userGeometry.heading
                var roadName = properties["name"]
                if (roadName == null) {
                    roadName = properties["highway"]
                }
                val facingDirectionAlongRoad =
                    getCompassLabelFacingDirectionAlong(
                        localizedContext,
                        orientation.toInt(),
                        roadName.toString(),
                        userGeometry.inMotion,
                        userGeometry.inVehicle
                    )
                results.add(PositionedString(facingDirectionAlongRoad))
                return results
            }
        }

        return results
    }

    private fun buildCalloutForRoadSense(userGeometry: UserGeometry,
                                         gridState: GridState): List<PositionedString> {

        // Check that our location/time has changed enough to generate this callout
        if (!locationFilter.shouldUpdate(userGeometry)) {
            return emptyList()
        }

        // Check that we're in a vehicle
        if (!userGeometry.inVehicle) {
            return emptyList()
        }

        // Update time/location filter for our new position
        locationFilter.update(userGeometry)

        // Reverse geocode the current location (this is the iOS name for the function)
        val results = reverseGeocode(userGeometry, gridState)

        // Check that the geocode has changed before returning a callout describing it

        return results
    }

    private fun buildCalloutForIntersections(userGeometry: UserGeometry,
                                             gridState: GridState) : List<PositionedString> {
        val results : MutableList<PositionedString> = mutableListOf()

        // Check that our location/time has changed enough to generate this callout
        if (!intersectionFilter.shouldUpdate(userGeometry)) {
            return emptyList()
        }

        // Check that we're not in a vehicle
        if (userGeometry.inVehicle) {
            return emptyList()
        }

        // Update time/location filter for our new position
        intersectionFilter.update(userGeometry)
        intersectionCalloutHistory.trim(userGeometry)

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

    private fun buildCalloutForNearbyPOI(userGeometry: UserGeometry,
                                         gridState: GridState) : List<PositionedString> {
        if (!poiFilter.shouldUpdateActivity(userGeometry)) {
            return emptyList()
        }

        val results: MutableList<PositionedString> = mutableListOf()

        // Trim history based on location and current time
        poiCalloutHistory.trim(userGeometry)

        // We want to call out up to the 10 nearest POI that are within 50m range
        val pois = gridState.getFeatureTree(TreeId.POIS).generateNearestFeatureCollection(
            userGeometry.location,
            50.0,
            10
        )

        for (feature in pois) {
            val name = getTextForFeature(localizedContext, feature)

            // Check the history and if the POI has been called out recently then we skip it
            val nearestPoint = getDistanceToFeature(userGeometry.location, feature)
            val callout = TrackedCallout(name.text, nearestPoint.point, feature.geometry.type == "Point", name.generic)
            if (poiCalloutHistory.find(callout)) {
                Log.d(TAG, "Discard ${callout.callout}")
            } else {
                results.add(
                    PositionedString(
                        name.text,
                        nearestPoint.point,
                        NativeAudioEngine.EARCON_SENSE_POI,
                    ),
                )
                // Add the entries to the history
                poiCalloutHistory.add(callout)
            }
        }

        return results
    }

    fun updateLocation(userGeometry: UserGeometry,
                       gridState: GridState) : List<PositionedString> {

        // The autoCallout logic comes straight from the iOS app.

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
                val roadSenseCallout = buildCalloutForRoadSense(userGeometry, gridState)
                if (roadSenseCallout.isNotEmpty()) {
                    list = roadSenseCallout
                } else {
                    val intersectionCallout = buildCalloutForIntersections(userGeometry, gridState)
                    if (intersectionCallout.isNotEmpty()) {
                        intersectionFilter.update(userGeometry)
                        list = list + intersectionCallout
                    }


                    // Get normal callouts for nearby POIs, for the destination, and for beacons
                    val poiCallout = buildCalloutForNearbyPOI(userGeometry, gridState)

                    // Update time/location filter for our new position
                    if (poiCallout.isNotEmpty()) {
                        poiFilter.update(userGeometry)
                        list = list + poiCallout
                    }
                }

                list
            }
        }
        return returnList
    }

    companion object {
        private const val TAG = "AutoCallout"
    }
}