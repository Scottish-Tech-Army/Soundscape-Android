package org.scottishtecharmy.soundscape.geoengine.callouts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.ALLOW_CALLOUTS_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.reverseGeocode
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature

class AutoCallout(
    private val localizedContext: Context,
    private val sharedPreferences: SharedPreferences
) {
    private val locationFilter = LocationUpdateFilter(10000, 50.0)
    private val poiFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionCalloutHistory = CalloutHistory(30000)
    private val poiCalloutHistory = CalloutHistory()

    private fun buildCalloutForRoadSense(userGeometry: UserGeometry,
                                         gridState: GridState): List<PositionedString> {

        // Check that our location/time has changed enough to generate this callout
        if (!locationFilter.shouldUpdate(userGeometry)) {
            return emptyList()
        }

        // Check that we're in a vehicle
        if (!userGeometry.inVehicle()) {
            return emptyList()
        }

        // Update time/location filter for our new position
        locationFilter.update(userGeometry)

        // Reverse geocode the current location (this is the iOS name for the function)
        val geocode = reverseGeocode(userGeometry, gridState, localizedContext)

        // Check that the geocode has changed before returning a callout describing it
        return if(geocode != null) {
            listOf(geocode)
        } else
            emptyList()
    }

    private fun buildCalloutForIntersections(userGeometry: UserGeometry,
                                             gridState: GridState) : List<PositionedString> {
        val results : MutableList<PositionedString> = mutableListOf()

        // We rely heavily on having map matched our GPS location to a nearby way. If we don't
        // have that Way, then skip intersection callouts until we do.
        if(userGeometry.mapMatchedWay == null) {
            return emptyList()
        }

        // Check that our location/time has changed enough to generate this callout
        if (!intersectionFilter.shouldUpdate(userGeometry)) {
            return emptyList()
        }

        // Check that we're not in a vehicle
        if (userGeometry.inVehicle()) {
            return emptyList()
        }

        // Update time/location filter for our new position
        intersectionFilter.update(userGeometry)
        intersectionCalloutHistory.trim(userGeometry)

        val roadsDescription = getRoadsDescriptionFromFov(
            gridState,
            userGeometry)

        // Don't describe the road we're on if there's an intersection
        if(roadsDescription.intersection != null) roadsDescription.nearestRoad = null

        addIntersectionCalloutFromDescription(
            roadsDescription,
            localizedContext,
            results,
            intersectionCalloutHistory
        )

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

        // Get nearby markers
        val markers = gridState.markerTree?.getNearestCollection(
            userGeometry.location,
            userGeometry.getSearchDistance(),
            5
        )

        // Get a list of the 10 nearest POI that are within search range, adding in the markers
        val pois = gridState.getFeatureTree(TreeId.SELECTED_SUPER_CATEGORIES).getNearestCollection(
            userGeometry.location,
            userGeometry.getSearchDistance(),
            10,
            markers
        )

        val uniquelyNamedPOIs = emptyMap<String,Feature>().toMutableMap()
        pois.features.filter { feature ->

            if(userGeometry.currentBeacon != null) {
                // If the feature is within 1m of the current beacon, don't call it out
                if(getDistanceToFeature(userGeometry.currentBeacon, feature).distance < 1.0) {
                    return@filter true
                }
            }

            val name = getTextForFeature(localizedContext, feature)
            val category = feature.foreign?.get("category") as String?

            val nearestPoint = getDistanceToFeature(userGeometry.location, feature)
            if(category == null) {
                true
            } else {
                if (nearestPoint.distance > userGeometry.getTriggerRange(category)) {
                    // The POI is farther away than the category allows
                    true
                } else {
                    // Check the history and if the POI has been called out recently then we skip it
                    val callout = TrackedCallout(
                        name.text,
                        nearestPoint.point,
                        feature.geometry.type == "Point",
                        name.generic
                    )
                    if (poiCalloutHistory.find(callout)) {
                        Log.d(TAG, "Discard ${callout.callout}")
                        // Filter out
                        true
                    } else {
                        if (!uniquelyNamedPOIs.containsKey(name.text)) {
                            // Don't filter out
                            uniquelyNamedPOIs[name.text] = feature
                            val earcon = when(feature.foreign?.get("category")) {
                                "information" -> NativeAudioEngine.EARCON_INFORMATION_ALERT
                                "safety" -> NativeAudioEngine.EARCON_SENSE_SAFETY
                                "mobility" -> NativeAudioEngine.EARCON_SENSE_MOBILITY
                                else -> NativeAudioEngine.EARCON_SENSE_POI
                            }
                            if(nearestPoint.distance == 0.0) {
                                results.add(
                                    PositionedString(
                                        text = localizedContext.getString(R.string.directions_at_poi, name.text),
                                        earcon = earcon,
                                        type = AudioType.STANDARD
                                    ),
                                )
                            } else {
                                results.add(
                                    PositionedString(
                                        text = name.text,
                                        location = nearestPoint.point,
                                        earcon = earcon,
                                        type = AudioType.LOCALIZED
                                    ),
                                )
                            }
                            poiCalloutHistory.add(callout)
                            false
                        } else {
                            true
                        }
                    }
                }
            }
        }
        return results
    }

    /**
     * updateLocation is called whenever the current location changes. It works through the auto
     * callout logic to determine which (if any) callouts need to be made. This is based on the iOS
     * app logic.
     * @param userGeometry The new state of the user location/speed etc.
     * @param gridState The current state of the tile data
     * @return A list of PositionedString callouts to be spoken
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun updateLocation(userGeometry: UserGeometry,
                       gridState: GridState) : List<PositionedString> {

        // Check that the callout isn't disabled in the settings
        if (!sharedPreferences.getBoolean(ALLOW_CALLOUTS_KEY, true)) {
            return emptyList()
        }

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        val returnList = runBlocking {
            withContext(gridState.treeContext) {
                var list = emptyList<PositionedString>()

                // buildCalloutForRoadSense builds a callout for
                val roadSenseCallout = buildCalloutForRoadSense(userGeometry, gridState)
                if (roadSenseCallout.isNotEmpty()) {
                    // If we have som
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
