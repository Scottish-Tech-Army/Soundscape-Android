package org.scottishtecharmy.soundscape.geoengine.callouts

import android.content.Context
import android.content.SharedPreferences
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
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature

class AutoCallout(
    private val localizedContext: Context?,
    private val sharedPreferences: SharedPreferences?
) {
    private val destinationFilter = LocationUpdateFilter(60000, 10.0)
    private val locationFilter = LocationUpdateFilter(10000, 50.0)
    private val poiFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionCalloutHistory = CalloutHistory(30000)
    private val poiCalloutHistory = CalloutHistory()
    private val roadSenseCalloutHistory = CalloutHistory()

    private fun buildCalloutForDestination(userGeometry: UserGeometry): TrackedCallout? {

        // Check that we have a destination
        if(userGeometry.currentBeacon == null)
            return null

        // Check that our location/time has changed enough to generate this callout
        if (!destinationFilter.shouldUpdate(userGeometry)) {
            return null
        }

        val distance = userGeometry.ruler.distance(userGeometry.location, userGeometry.currentBeacon)
        val distanceString = formatDistanceAndDirection(distance, null, localizedContext)
        var text = localizedContext?.getString(R.string.callouts_audio_beacon) ?: "Distance to the audio beacon"
        text += " $distanceString"

        return TrackedCallout(
            userGeometry = userGeometry,
            trackedText = "",
            location = userGeometry.currentBeacon,
            isPoint = true,
            isGeneric = true,
            filter = false,
            positionedStrings = List(1) {
                PositionedString(
                    text = text,
                    location = userGeometry.currentBeacon,
                    type = AudioType.LOCALIZED
                )
            }
        )
    }

    private fun buildCalloutForRoadSense(userGeometry: UserGeometry,
                                         geocoder: SoundscapeGeocoder): TrackedCallout? {

        // Check that our location/time has changed enough to generate this callout
        if (!locationFilter.shouldUpdate(userGeometry)) {
            return null
        }

        // Trim history based on location and current time
        roadSenseCalloutHistory.trim(userGeometry)

        // Check that we're in a vehicle
        if (!userGeometry.inVehicle()) {
            return null
        }

        // Update time/location filter for our new position
        locationFilter.update(userGeometry)

        // Reverse geocode the current location (this is the iOS name for the function)
        val result = runBlocking {
            val geocode = geocoder.getAddressFromLngLat (userGeometry, localizedContext)
            if(geocode == null)
                null
            else
                PositionedString(
                    text = geocode.name,
                    location = userGeometry.location,
                    type = AudioType.LOCALIZED
                )
        }
        if(result != null) {
            val callout = TrackedCallout(
                userGeometry,
                trackedText = result.text,
                location =result.location!!,
                positionedStrings = listOf(result),
                isPoint = false,
                isGeneric = false,
                calloutHistory = roadSenseCalloutHistory
            )

            if (roadSenseCalloutHistory.find(callout)) {
                //println("Discard ${callout.trackedText}")
                // Filter out
                return null
            }

            // Check that the geocode has changed before returning a callout describing it
            return callout
        }

        return null
    }
    fun buildCalloutForIntersections(userGeometry: UserGeometry,
                                     gridState: GridState) : TrackedCallout? {

        // We rely heavily on having map matched our GPS location to a nearby way. If we're not in
        // StreetPreview mode and we don't have that Way, then skip intersection callouts until we
        // do.
        if((userGeometry.mapMatchedWay == null) && !userGeometry.inStreetPreview) {
            return null
        }

        // Check that our location/time has changed enough to generate this callout
        if (!intersectionFilter.shouldUpdate(userGeometry)) {
            return null
        }

        // Check that we're not in a vehicle
        if (userGeometry.inVehicle()) {
            return null
        }

        // Trim callout history based on our location and current time
        intersectionCalloutHistory.trim(userGeometry)

        val roadsDescription = getRoadsDescriptionFromFov(
            gridState,
            userGeometry)

        // Don't describe the road we're on if there's an intersection
        return addIntersectionCalloutFromDescription(
            roadsDescription,
            localizedContext,
            intersectionCalloutHistory,
            gridState
        )
    }

    private fun buildCalloutForNearbyPOI(userGeometry: UserGeometry,
                                         gridState: GridState) : TrackedCallout? {
        if (!poiFilter.shouldUpdateActivity(userGeometry)) {
            return null
        }

        // Trim history based on location and current time
        poiCalloutHistory.trim(userGeometry)

        // Get nearby markers that are ahead of us in our field of view
        val triangle = getFovTriangle(userGeometry)
        val markers = gridState.markerTree?.getNearestCollectionWithinTriangle(
            triangle,
            5,
            userGeometry.ruler
        )

        // Get a list of the 10 nearest POI that are within search range, adding in the markers
        val pois = gridState.getFeatureTree(TreeId.SELECTED_SUPER_CATEGORIES).getNearestCollection(
            userGeometry.location,
            userGeometry.getSearchDistance(),
            10,
            userGeometry.ruler,
            markers
        )

        val uniquelyNamedPOIs = mutableMapOf<String, Feature>()
        pois.features.filter { feature ->

            val name = getTextForFeature(localizedContext, feature as MvtFeature)
            val nearestPoint = getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)

            val callout = TrackedCallout(
                userGeometry,
                name.text,
                nearestPoint.point,
                positionedStrings = emptyList(),
                feature.geometry.type == "Point",
                name.generic
            )
            if(userGeometry.currentBeacon != null) {
                // If the feature is within 1m of the current beacon, don't call it out
                if(getDistanceToFeature(userGeometry.currentBeacon, feature, userGeometry.ruler).distance < 1.0) {
                    // We do want to add it to the POI history though so that when it's no longer
                    // the currentBeacon it doesn't immediately get called out.
                    if (!poiCalloutHistory.find(callout))
                        poiCalloutHistory.add(callout)

                    return@filter true
                }
            }

            if(feature.superCategory == SuperCategoryId.UNCATEGORIZED) {
                true
            } else {
                if (nearestPoint.distance > userGeometry.getTriggerRange(feature.superCategory)) {
                    // The POI is farther away than the category allows
                    true
                } else {
                    // Check the history and if the POI has been called out recently then we skip it
                    if (poiCalloutHistory.find(callout)) {
                        //println("Discard ${callout.trackedText}")
                        // Filter out
                        true
                    } else {
                        if (!uniquelyNamedPOIs.containsKey(name.text)) {
                            // Don't filter out
                            uniquelyNamedPOIs[name.text] = feature
                            val earcon = when(feature.superCategory) {
                                SuperCategoryId.INFORMATION -> NativeAudioEngine.EARCON_INFORMATION_ALERT
                                SuperCategoryId.SAFETY -> NativeAudioEngine.EARCON_SENSE_SAFETY
                                SuperCategoryId.MOBILITY -> NativeAudioEngine.EARCON_SENSE_MOBILITY
                                else -> NativeAudioEngine.EARCON_SENSE_POI
                            }
                            if(nearestPoint.distance == 0.0) {
                                callout.positionedStrings = List(1) {
                                    PositionedString(
                                        text = localizedContext?.getString(
                                            R.string.directions_at_poi,
                                            name.text
                                        ) ?: "At ${name.text}",
                                        earcon = earcon,
                                        type = AudioType.STANDARD
                                    )
                                }
                            } else {
                                callout.positionedStrings = List(1) {
                                    PositionedString(
                                        text = name.text,
                                        location = nearestPoint.point,
                                        earcon = earcon,
                                        type = AudioType.LOCALIZED
                                    )
                                }
                            }
                            poiCalloutHistory.add(callout)
                            return callout
                        } else {
                            true
                        }
                    }
                }
            }
        }
        return null
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
    fun updateLocation(
        userGeometry: UserGeometry,
        gridState: GridState,
        settlementGrid: GridState,
        geocoder: SoundscapeGeocoder
    ) : TrackedCallout? {

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        return runBlocking {
            withContext(gridState.treeContext) {
                var trackedCallout : TrackedCallout? = null

                val destinationCallout = buildCalloutForDestination(userGeometry)
                if (destinationCallout != null) {
                    // Update the destination filter if we're outputting it
                    destinationCallout.locationFilter = destinationFilter
                    trackedCallout = destinationCallout
                } else if (sharedPreferences?.getBoolean(ALLOW_CALLOUTS_KEY, true) != false) {
                    // buildCalloutForRoadSense builds a callout for travel that's faster than
                    // walking
                    val roadSenseCallout =
                        buildCalloutForRoadSense(userGeometry, geocoder)
                    if (roadSenseCallout != null) {
                        trackedCallout = roadSenseCallout
                    } else {
                        val intersectionCallout =
                            buildCalloutForIntersections(userGeometry, gridState)
                        if (intersectionCallout != null) {
                            intersectionCallout.locationFilter = intersectionFilter
                            trackedCallout = intersectionCallout
                        }
                        if((intersectionCallout == null) || userGeometry.inStreetPreview) {
                            // Get normal callouts for nearby POIs, for the destination, and for beacons
                            val poiCallout = buildCalloutForNearbyPOI(userGeometry, gridState)

                            // Update time/location filter for our new position
                            if (poiCallout != null) {
                                poiCallout.locationFilter = poiFilter
                                if(userGeometry.inStreetPreview) {
                                    // In Street Preview we want to add the call outs on to any intersection that
                                    // is being called out.
                                    if(trackedCallout != null) {
                                        trackedCallout.positionedStrings += poiCallout.positionedStrings
                                    }
                                    else
                                        trackedCallout = poiCallout
                                }
                                else
                                    trackedCallout = poiCallout
                            }
                        }
                    }
                }
                trackedCallout
            }
        }
    }
}
