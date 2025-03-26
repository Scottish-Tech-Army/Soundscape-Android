package org.scottishtecharmy.soundscape.geoengine.callouts

import android.content.Context
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicates
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point

data class RoadsDescription(var nearestRoad: Feature? = null,
                            val userGeometry: UserGeometry = UserGeometry(),
                            val intersection: Feature? = null,
                            val intersectionRoads: FeatureCollection = FeatureCollection())

/**
 * getRoadsDescriptionFromFov returns a description of the nearestRoad and also the 'best'
 * intersection within the field of view. The description includes the roads that join the
 * intersection, the location of the intersection and the name of the intersection.
 *
 * @param gridState The current GridState which is the state of the downloaded tiles
 * @param userGeometry This includes location, heading and other data
 * @param approach The algorithm used to pick the best intersection.
 *
 * @return An IntersectionDescription containing all the data required for callouts to describe the
 * intersection.
 */
fun getRoadsDescriptionFromFov(gridState: GridState,
                               userGeometry: UserGeometry
) : RoadsDescription {

    // Create FOV triangle
    val triangle = getFovTriangle(userGeometry)

    val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
    val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

    // Find roads within FOV
    val fovRoads = roadTree.getAllWithinTriangle(triangle)
    if(fovRoads.features.isEmpty()) return RoadsDescription(nearestRoad = userGeometry.nearestRoad)

    // Two roads that we are interested in:
    //  1. The one that we are nearest to. We use this for intersection call outs to decide which
    //     intersection road we're on. This can be slightly behind us, it doesn't have to be in our
    //     FOV.
    //  2. The nearest one in our FOV. We use this to describe 'what's ahead'
    val nearestRoad = userGeometry.nearestRoad
    val nearestRoadInFoV = roadTree.getNearestFeatureWithinTriangle(triangle)

    // Find intersections within FOV
    val fovIntersections = intersectionTree.getAllWithinTriangle(triangle)
    if(fovIntersections.features.isEmpty()) return RoadsDescription(nearestRoad, userGeometry)

    // Sort the FOV intersections by distance
    val sortedFovIntersections = sortedByDistanceTo(userGeometry.location, fovIntersections)

    // Inspect each intersection so as to skip trivial ones
    val nonTrivialIntersections = emptyList<Pair<Int, Feature>>().toMutableList()
    for (i in 0 until sortedFovIntersections.features.size) {
        // Get the roads for the intersection
        val intersectionRoads = getIntersectionRoadNames(sortedFovIntersections.features[i], fovRoads)
        // Skip 'simple' intersections e.g. ones where the only roads involved have the same name
        val priority = checkWhetherIntersectionIsOfInterest(intersectionRoads, nearestRoad)
        nonTrivialIntersections.add(Pair(priority, sortedFovIntersections.features[i]))
    }
    if(nonTrivialIntersections.isEmpty()) {
        return RoadsDescription(nearestRoad, userGeometry)
    }

    var intersection: Feature? = nonTrivialIntersections.firstOrNull { prioritised ->
                prioritised.first > 0
            }?.second
    if(intersection == null) {
        // No intersection with a priority greater than zero, so just pick the highest
        intersection = nonTrivialIntersections.maxByOrNull { prioritised ->
            prioritised.first
        }?.second
    }
    // Use the nearest intersection, but remove duplicated OSM ids from it (those which loop back)
    val nearestIntersection = removeDuplicates(sortedFovIntersections.features[0])

    // Find the bearing that we're coming in at - measured to the nearest intersection
    val heading = userGeometry.heading()
    if(heading != null) {
        val nearestRoadBearing =
            getRoadBearingToIntersection(nearestIntersection, nearestRoad, heading)

        // Create a set of relative direction polygons
        val intersectionLocation = intersection!!.geometry as Point
        val geometry = UserGeometry(
            intersectionLocation.coordinates,
            nearestRoadBearing,
            5.0
        )
        val relativeDirections = getRelativeDirectionsPolygons(
            geometry,
            RelativeDirections.COMBINED
        )

        // And use the polygons to describe the roads at the intersection
        val intersectionRoadNames = getIntersectionRoadNames(intersection, fovRoads)
        return RoadsDescription(
            nearestRoadInFoV,
            userGeometry,
            intersection,
            getIntersectionRoadNamesRelativeDirections(
                intersectionRoadNames,
                intersection,
                relativeDirections
            )
        )
    }
    return RoadsDescription(nearestRoad, userGeometry)
}

/**
 * addIntersectionCalloutFromDescription adds a callout to the results list for the intersection
 * described in the parameters. This will become more configurable e.g. whether to include the
 * distance or not.
 *
 * @param description The description of the intersection to callout
 * @param localizedContext A context for obtaining localized strings
 * @param results The list of callouts that is appended to
 * @param calloutHistory An optional CalloutHistory to use so as to filter out recently played out
 * callouts
 */
fun addIntersectionCalloutFromDescription(
    description: RoadsDescription,
    localizedContext: Context,
    results: MutableList<PositionedString>,
    userGeometry: UserGeometry,
    calloutHistory: CalloutHistory? = null
) {

    // Report nearby road
    description.nearestRoad?.let { nearestRoad ->
        if (nearestRoad.properties?.get("name") != null) {
            val calloutText = "${localizedContext.getString(R.string.directions_direction_ahead)} ${nearestRoad.properties!!["name"]}"
            var skip = false
            calloutHistory?.checkAndAdd(TrackedCallout(calloutText,
                    LngLatAlt(),
                    isPoint = false,
                    isGeneric = false
                ))?.let { newCallout ->
                    if(!newCallout) skip = true
            }
            if(skip) {
            } else {
                results.add(PositionedString(
                    text = calloutText,
                    type = AudioType.STANDARD))
            }
        } else {
            // we are detecting an unnamed road here but pretending there is nothing here
            results.add(
                PositionedString(
                    text = localizedContext.getString(R.string.callouts_nothing_to_call_out_now),
                    type = AudioType.STANDARD
                )
            )
        }
    }

    if(description.intersection == null) return

    val intersectionNameProperty = description.intersection.properties?.get("name")
    val intersectionName = if(intersectionNameProperty == null)
        ""
    else
        intersectionNameProperty as String


    // Check if we should be filtering out this callout
    val intersectionLocation = (description.intersection.geometry as Point).coordinates
    calloutHistory?.checkAndAdd(TrackedCallout(intersectionName,
        intersectionLocation,
        isPoint = true,
        isGeneric = false
    ))?.let { success ->
        if(!success) return
    }

    // Report intersection is coming up
    results.add(
        PositionedString(
            text =localizedContext.getString(R.string.intersection_approaching_intersection),
            earcon = NativeAudioEngine.EARCON_SENSE_POI,
            type = AudioType.STANDARD)
    )

    // Report roads that join the intersection
    for (feature in description.intersectionRoads.features) {
        val direction = feature.properties?.get("Direction").toString().toIntOrNull()

        // Don't call out the road we are on (0) as part of the intersection
        if (direction != null && direction != 0) {
            if (feature.properties?.get("name") != null) {
                val roadDirectionId = when(direction) {
                    1,2,3 -> R.string.directions_name_goes_left
                    5,6,7 -> R.string.directions_name_goes_right
                    else -> R.string.directions_name_continues_ahead
                }

                var heading = userGeometry.presentationHeading()
                if(heading == null)
                    heading = 0.0

                heading += when(direction) {
                    1,2,3 -> -90.0
                    5,6,7 -> 90.0
                    else -> 0.0
                }

                var destinationModifier: Any? = null
//                if(line.coordinates.first() == intersectionLocation)
                destinationModifier = feature.properties?.get("destination:forward")
//                else if(line.coordinates.last() == intersectionLocation)
//                    destinationModifier = feature.properties?.get("destination:backward").toString()

                val name = feature.properties?.get("name")
                val destinationText =
                    if(destinationModifier != null)
                        "$name to ${destinationModifier.toString()}"
                    else
                        name

                val intersectionCallout =
                    localizedContext.getString(roadDirectionId, destinationText)
                results.add(
                    PositionedString(
                        text = intersectionCallout,
                        type = AudioType.COMPASS,
                        heading = heading
                    )
                )
            }
        }
    }
}
