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
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirection
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey

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
                                    text = localizedStrings.get(StringKey.StationaryOnWay, roadName),
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
                            featureTree.getNearestCollectionWithinTriangle(triangle, 4, userGeometry.ruler)
                        if (featureCollection.features.isNotEmpty()) {
                            for (feature in featureCollection) {
                                var duplicate = false
                                val featureName =
                                    getTextForFeature(localizedStrings, feature as MvtFeature).text
                                for (otherFeature in featuresByDirection) {
                                    if (otherFeature == null) continue
                                    val otherName =
                                        getTextForFeature(localizedStrings, otherFeature as MvtFeature).text
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
                    val name = getTextForFeature(localizedStrings, feature as MvtFeature)
                    val text = "${name.text}. ${
                        formatDistanceAndDirection(
                            poiLocation.distance,
                            poiLocation.heading,
                            localizedStrings
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
                    val name = getTextForFeature(localizedStrings, feature as MvtFeature)
                    val text = "${name.text}. ${
                        formatDistanceAndDirection(
                            poiLocation.distance,
                            poiLocation.heading,
                            localizedStrings
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
                            getTextForFeature(localizedStrings, feature as MvtFeature)
                        val markerLocation =
                            getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)
                        val text = "${featureText.text}. ${
                            formatDistanceAndDirection(
                                markerLocation.distance,
                                markerLocation.heading,
                                localizedStrings
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
