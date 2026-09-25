package org.scottishtecharmy.soundscape.geoengine.callouts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.Earcons
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

/**
 * The "Beacon is currently <distance> away" callout, spoken from the beacon's own direction.
 *
 * Shared by the automatic distance updates (see AutoCallout, which decides *when* one is due) and
 * by the beacon card's "Call out Beacon" accessibility action, which is the user asking for it
 * now - so this function itself neither checks the DISTANCE_TO_BEACON preference nor throttles.
 * The legacy iOS app treats the two the same way, issuing a DestinationCallout in both cases.
 */
fun buildBeaconCallout(
    userGeometry: UserGeometry,
    localizedStrings: LocalizedStrings?,
): TrackedCallout? {
    val beacon = userGeometry.currentBeacon ?: return null

    val distance = userGeometry.ruler.distance(userGeometry.location, beacon)
    val distanceString =
        formatDistanceAndDirection(distance, null, localizedStrings, speed = userGeometry.speed)
    val text = localizedStrings?.get(StringKey.CalloutsAudioBeaconDistance, distanceString)
        ?: "Distance to beacon $distanceString"
    return TrackedCallout(
        userGeometry = userGeometry,
        trackedText = "",
        location = beacon,
        isPoint = true,
        isGeneric = true,
        filter = false,
        positionedStrings = List(1) {
            PositionedString(
                text = text,
                location = beacon,
                type = AudioType.LOCALIZED
            )
        }
    )
}

/**
 * "No beacon active", for when the user asks to hear about the beacon - Call out Beacon or More
 * Info, e.g. from the audio menu - and there isn't one. Better than silence, which on the audio
 * menu leaves the user wondering whether the selection registered at all.
 */
fun buildNoBeaconCallout(
    userGeometry: UserGeometry,
    localizedStrings: LocalizedStrings?,
): TrackedCallout =
    TrackedCallout(
        userGeometry = userGeometry,
        filter = false,
        positionedStrings = listOf(
            PositionedString(
                text = localizedStrings?.get(StringKey.CalloutsNoBeaconActive)
                    ?: "No beacon active",
                type = AudioType.STANDARD
            )
        )
    )

/**
 * The beacon card's "More Info" callout, e.g. "Starbucks is currently 700 metres, north west.
 * Street address is 123 Main Street.", spoken from the beacon's own direction. The address is
 * dropped when the offline geocoder has nothing for the location.
 *
 * Spoken through TTS rather than announced by the screen reader, as the legacy iOS app did, so
 * that it can also be triggered where there is no screen at all, e.g. from media controls.
 */
fun buildBeaconMoreInfoCallout(
    userGeometry: UserGeometry,
    localizedStrings: LocalizedStrings?,
    beaconName: String,
    address: LocationDescription?,
): TrackedCallout? {
    val beacon = userGeometry.currentBeacon ?: return null

    val distance = userGeometry.ruler.distance(userGeometry.location, beacon)
    val bearing = userGeometry.ruler.bearing(userGeometry.location, beacon)
    val distanceString = formatDistanceAndDirection(
        distance, bearing, localizedStrings, speed = userGeometry.speed
    )
    val street = address?.description?.takeIf { it.isNotEmpty() }
        ?: address?.name?.takeIf { it.isNotEmpty() }
    val text = if (street != null) {
        localizedStrings?.get(
            StringKey.DirectionsNameIsCurrentlyStreetAddress, beaconName, distanceString, street
        ) ?: "$beaconName is currently $distanceString. Street address is $street."
    } else {
        localizedStrings?.get(StringKey.DirectionsNameIsCurrently, beaconName, distanceString)
            ?: "$beaconName is currently $distanceString."
    }
    return TrackedCallout(
        userGeometry = userGeometry,
        trackedText = "",
        location = beacon,
        isPoint = true,
        isGeneric = true,
        filter = false,
        positionedStrings = List(1) {
            PositionedString(
                text = text,
                location = beacon,
                type = AudioType.LOCALIZED
            )
        }
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun buildMyLocationCallout(
    userGeometry: UserGeometry,
    hasValidLocation: Boolean,
    geocoder: SoundscapeGeocoder,
    localizedStrings: LocalizedStrings,
    gridState: GridState,
): TrackedCallout? {

    var results: MutableList<PositionedString> = mutableListOf()
    if (!hasValidLocation) {
        results.add(
            PositionedString(
                text = localizedStrings.get(StringKey.GeneralErrorFindLocationError),
                type = AudioType.STANDARD
            )
        )
    } else {
        val orientation = userGeometry.heading()
        results = runBlocking {
            withContext(gridState.treeContext) {

                val list: MutableList<PositionedString> = mutableListOf()

                val ld = geocoder.getAddressFromLngLat(userGeometry, localizedStrings, false)
                if (ld != null) {
                    if (orientation != null) {
                        val facingDirection =
                            getCompassLabelFacingDirection(
                                localizedStrings,
                                orientation.toInt(),
                                userGeometry.inMotion(),
                                userGeometry.inVehicle()
                            )
                        list.add(
                            PositionedString(
                                text = facingDirection,
                                type = AudioType.STANDARD
                            )
                        )
                    }
                    list.add(
                        PositionedString(
                            text = ld.name,
                            type = AudioType.STANDARD
                        )
                    )
                    list
                } else {
                    val nearestRoad = userGeometry.mapMatchedWay
                    val roadName =
                        nearestRoad?.getName(null, gridState, localizedStrings)
                    if (orientation != null) {
                        if (roadName != null) {
                            val facingDirectionAlongRoad =
                                getCompassLabelFacingDirectionAlong(
                                    localizedStrings,
                                    orientation.toInt(),
                                    roadName,
                                    userGeometry.inMotion(),
                                    userGeometry.inVehicle()
                                )
                            list.add(
                                PositionedString(
                                    text = facingDirectionAlongRoad,
                                    type = AudioType.STANDARD
                                )
                            )
                        } else {
                            val facingDirection =
                                getCompassLabelFacingDirection(
                                    localizedStrings,
                                    orientation.toInt(),
                                    userGeometry.inMotion(),
                                    userGeometry.inVehicle()
                                )
                            list.add(
                                PositionedString(
                                    text = facingDirection,
                                    type = AudioType.STANDARD
                                )
                            )
                        }
                    } else {
                        if (roadName != null) {
                            list.add(
                                PositionedString(
                                    text = localizedStrings.get(
                                        StringKey.StationaryOnWay,
                                        roadName
                                    ),
                                    type = AudioType.STANDARD
                                )
                            )
                        } else {
                            list.add(
                                PositionedString(
                                    text = localizedStrings.get(StringKey.GeneralErrorFindLocationError),
                                    type = AudioType.STANDARD
                                )
                            )
                        }
                    }
                    list
                }
            }
        }
    }
    if (results.isEmpty())
        return null

    return TrackedCallout(
        userGeometry = userGeometry,
        filter = false,
        positionedStrings = results
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun buildWhatsAroundMeCallout(
    userGeometry: UserGeometry,
    hasValidLocation: Boolean,
    localizedStrings: LocalizedStrings,
    gridState: GridState,
): TrackedCallout {

    var results: MutableList<PositionedString> = mutableListOf()

    if (!hasValidLocation) {
        results.add(
            PositionedString(
                text = localizedStrings.get(StringKey.GeneralErrorFindLocationError),
                type = AudioType.STANDARD
            )
        )
    } else {
        results = runBlocking {
            withContext(gridState.treeContext) {

                val featuresByDirection: Array<Feature?> = arrayOfNulls(4)
                val directionsNeeded = setOf(0, 1, 2, 3).toMutableSet()

                val featureTree = gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)
                for (distance in 200..1000 step 200) {

                    val individualRelativePolygons = getRelativeDirectionsPolygons(
                        UserGeometry(
                            userGeometry.location,
                            userGeometry.heading(),
                            distance.toDouble()
                        ), RelativeDirections.INDIVIDUAL
                    )

                    val direction = directionsNeeded.iterator()
                    while (direction.hasNext()) {

                        val dir = direction.next()
                        val triangle = getTriangleForDirection(individualRelativePolygons, dir)
                        val featureCollection =
                            featureTree.getNearestCollectionWithinTriangle(
                                triangle,
                                4,
                                userGeometry.ruler
                            )
                        if (featureCollection.features.isNotEmpty()) {
                            for (feature in featureCollection) {
                                var duplicate = false
                                val featureName =
                                    (feature as MvtFeature).getText(localizedStrings).text
                                for (otherFeature in featuresByDirection) {
                                    if (otherFeature == null) continue
                                    val otherName =
                                        (otherFeature as MvtFeature).getText(localizedStrings).text
                                    if (featureName == otherName) duplicate = true
                                }
                                if (!duplicate) {
                                    featuresByDirection[dir] = feature
                                    direction.remove()
                                    break
                                }
                            }
                        }
                    }
                    if (directionsNeeded.isEmpty()) break
                }

                val list: MutableList<PositionedString> = mutableListOf()
                for (feature in featuresByDirection) {

                    if (feature == null) continue
                    val poiLocation =
                        getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                    val name = (feature as MvtFeature).getText(localizedStrings)
                    val text = "${name.text}. ${
                        formatDistanceAndDirection(
                            poiLocation.distance,
                            poiLocation.heading,
                            localizedStrings,
                            speed = userGeometry.speed
                        )
                    }"
                    list.add(
                        PositionedString(
                            text,
                            poiLocation.point,
                            Earcons.SENSE_POI,
                            AudioType.LOCALIZED,
                        )
                    )
                }
                list
            }
        }
    }

    return TrackedCallout(
        userGeometry = userGeometry,
        filter = false,
        positionedStrings = results
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun buildAheadOfMeCallout(
    userGeometry: UserGeometry,
    hasValidLocation: Boolean,
    localizedStrings: LocalizedStrings,
    gridState: GridState,
): TrackedCallout? {

    var results: MutableList<PositionedString> = mutableListOf()

    if (!hasValidLocation) {
        results.add(
            PositionedString(
                text = localizedStrings.get(StringKey.GeneralErrorFindLocationError),
                type = AudioType.STANDARD
            )
        )
    } else {
        results = runBlocking {
            withContext(gridState.treeContext) {

                userGeometry.fovDistance = 1000.0
                val triangle = getFovTriangle(userGeometry)
                val featureTree = gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)

                val featuresAhead =
                    featureTree.getNearestCollectionWithinTriangle(triangle, 5, userGeometry.ruler)
                val list: MutableList<PositionedString> = mutableListOf()
                for (feature in featuresAhead) {

                    val poiLocation =
                        getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                    val name = (feature as MvtFeature).getText(localizedStrings)
                    val text = "${name.text}. ${
                        formatDistanceAndDirection(
                            poiLocation.distance,
                            poiLocation.heading,
                            localizedStrings,
                            speed = userGeometry.speed
                        )
                    }"
                    list.add(
                        PositionedString(
                            text,
                            poiLocation.point,
                            Earcons.SENSE_POI,
                            AudioType.LOCALIZED,
                        )
                    )
                }
                if (list.isEmpty()) {
                    list.add(
                        PositionedString(
                            text = localizedStrings.get(StringKey.CalloutsNothingToCallOutNow),
                            type = AudioType.STANDARD
                        )
                    )
                }
                list
            }
        }
    }
    if (results.isEmpty())
        return null

    return TrackedCallout(
        userGeometry = userGeometry,
        filter = false,
        positionedStrings = results
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun buildNearbyMarkersCallout(
    userGeometry: UserGeometry,
    hasValidLocation: Boolean,
    localizedStrings: LocalizedStrings,
    gridState: GridState,
): TrackedCallout {

    var results: MutableList<PositionedString> = mutableListOf()

    if (!hasValidLocation) {
        results.add(
            PositionedString(
                text = localizedStrings.get(StringKey.GeneralErrorFindLocationError),
                type = AudioType.STANDARD
            )
        )
    } else {
        results = runBlocking {
            withContext(gridState.treeContext) {

                val nearestMarkers = gridState.markerTree?.getNearestCollection(
                    userGeometry.location,
                    2000.0,
                    4,
                    userGeometry.ruler
                )

                val list: MutableList<PositionedString> = mutableListOf()
                if (nearestMarkers != null) {
                    for (feature in nearestMarkers.features) {
                        val featureText =
                            (feature as MvtFeature).getText(localizedStrings)
                        val markerLocation =
                            getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                        val text = "${featureText.text}. ${
                            formatDistanceAndDirection(
                                markerLocation.distance,
                                markerLocation.heading,
                                localizedStrings,
                                speed = userGeometry.speed
                            )
                        }"
                        list.add(
                            PositionedString(
                                text,
                                markerLocation.point,
                                Earcons.SENSE_POI,
                                AudioType.LOCALIZED,
                            )
                        )
                    }
                }
                list
            }
        }
    }

    if (results.isEmpty()) {
        results.add(
            PositionedString(
                text = localizedStrings.get(StringKey.CalloutsNoNearbyMarkers),
                type = AudioType.STANDARD
            )
        )
    }

    return TrackedCallout(
        userGeometry = userGeometry,
        filter = false,
        positionedStrings = results
    )
}
