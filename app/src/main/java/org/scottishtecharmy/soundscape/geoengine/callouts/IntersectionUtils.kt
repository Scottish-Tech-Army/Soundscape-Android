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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point

data class IntersectionDescription(var nearestRoad: Feature? = null,
                                   val userGeometry: UserGeometry = UserGeometry(),
                                   val intersection: Intersection? = null,
)

/**
 * getRoadsDescriptionFromFov returns a description of the nearestRoad and also the 'best'
 * intersection within the field of view. The description includes the roads that join the
 * intersection, the location of the intersection and the name of the intersection.
 *
 * @param gridState The current GridState which is the state of the downloaded tiles
 * @param userGeometry This includes location, heading and other data
 *
 * @return An IntersectionDescription containing all the data required for callouts to describe the
 * intersection.
 */
fun getRoadsDescriptionFromFov(gridState: GridState,
                               userGeometry: UserGeometry
) : IntersectionDescription {

    // Create FOV triangle
    val triangle = getFovTriangle(userGeometry)

    val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
    val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

    // Find roads within FOV
    val fovRoads = roadTree.getAllWithinTriangle(triangle)
    if(fovRoads.features.isEmpty()) return IntersectionDescription(nearestRoad = userGeometry.mapMatchedWay)

    // Two roads that we are interested in:
    //  1. The one that we are nearest to. We use this for intersection call outs to decide which
    //     intersection road we're on. This can be slightly behind us, it doesn't have to be in our
    //     FOV.
    //  2. The nearest one in our FOV. We use this to describe 'what's ahead'
    val nearestRoad = userGeometry.mapMatchedWay
    val nearestRoadInFoV = roadTree.getNearestFeatureWithinTriangle(triangle)

    // Find intersections within FOV
    val fovIntersections = intersectionTree.getAllWithinTriangle(triangle)
    if(fovIntersections.features.isEmpty()) return IntersectionDescription(nearestRoad, userGeometry)

    // Sort the FOV intersections by distance
    val sortedFovIntersections = sortedByDistanceTo(userGeometry.location, fovIntersections)

    // Inspect each intersection so as to skip trivial ones
    val nonTrivialIntersections = emptyList<Pair<Int, Intersection>>().toMutableList()

    for (intersection in sortedFovIntersections.features) {
        val intersectionLocation = (intersection.geometry as Point).coordinates
        val graphIntersection = gridState.gridIntersections[intersectionLocation]
        if(graphIntersection != null) {
            // We aim to skip 'simple' intersections e.g. ones where the only roads involved have
            // the same name.
            val priority = checkWhetherIntersectionIsOfInterest(graphIntersection, nearestRoad)
            nonTrivialIntersections.add(Pair(priority, graphIntersection))
        }
    }
    if(nonTrivialIntersections.isEmpty()) {
        return IntersectionDescription(nearestRoad, userGeometry)
    }

    var intersection: Intersection? = nonTrivialIntersections.firstOrNull { prioritised ->
                prioritised.first > 0
            }?.second
    if(intersection == null) {
        // No intersection with a priority greater than zero, so just pick the highest
        intersection = nonTrivialIntersections.maxByOrNull { prioritised ->
            prioritised.first
        }?.second
    }

    // Use the nearest intersection, but remove duplicated OSM ids from it (those which loop back)
//    val nearestIntersection = removeDuplicates(sortedFovIntersections.features[0])

    // Find the bearing that we're coming in at - measured to the nearest intersection
    val heading = userGeometry.heading()
    if(heading != null) {
        // And use the polygons to describe the roads at the intersection
        return IntersectionDescription(
            nearestRoadInFoV,
            userGeometry,
            intersection
        )
    }
    return IntersectionDescription(nearestRoad, userGeometry)
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
    description: IntersectionDescription,
    localizedContext: Context,
    results: MutableList<PositionedString>,
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

    val intersectionName = description.intersection.name

    // Check if we should be filtering out this callout
    val intersectionLocation = description.intersection.location
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
    val heading = description.userGeometry.heading()
    if(heading == null)
        return

    val directions = getCombinedDirectionSegments(heading)
    for (way in description.intersection.members) {

        val wayHeading = way.heading(description.intersection)
        val direction = directions.indexOfFirst { segment ->
            segment.contains(wayHeading)
        }

        // Don't call out the road we are on (0) as part of the intersection
        if (direction != Direction.BEHIND.value) {
            val roadDirectionId = when(direction) {
                Direction.BEHIND_LEFT.value,Direction.LEFT.value,Direction.AHEAD_LEFT.value ->
                    R.string.directions_name_goes_left
                Direction.BEHIND_RIGHT.value,Direction.RIGHT.value,Direction.AHEAD_RIGHT.value ->
                    R.string.directions_name_goes_right
                else ->
                    R.string.directions_name_continues_ahead
            }

            var presentationHeading = description.userGeometry.presentationHeading()
            if(presentationHeading == null)
                presentationHeading = 0.0

            presentationHeading += when(direction) {
                Direction.BEHIND_LEFT.value,Direction.LEFT.value,Direction.AHEAD_LEFT.value -> -90.0
                Direction.BEHIND_RIGHT.value,Direction.RIGHT.value,Direction.AHEAD_RIGHT.value -> 90.0
                else -> 0.0
            }

            var destinationText = way.getName(way.intersections[WayEnd.START.id] == description.intersection)
            val intersectionCallout =
                localizedContext.getString(roadDirectionId, destinationText)
            results.add(
                PositionedString(
                    text = intersectionCallout,
                    type = AudioType.COMPASS,
                    heading = presentationHeading
                )
            )
        }
    }
}
