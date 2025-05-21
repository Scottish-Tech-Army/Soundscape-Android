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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getPathWays
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.math.abs

data class IntersectionDescription(var nearestRoad: Way? = null,
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

    var nearestRoad = userGeometry.mapMatchedWay
    if(nearestRoad == null) {
        if (userGeometry.inStreetPreview) {
            // In StreetPreview mode, the road we're on is that matching the heading into the
            // intersection that we're at.
            val intersection = intersectionTree.getNearestFeature(userGeometry.location) as Intersection?
            val userHeading = userGeometry.heading()
            if((userHeading != null) && (intersection != null)) {
                for (member in intersection.members) {
                    val wayHeading = (member.heading(intersection) + 180.0) % 360.0
                    if (abs(wayHeading - userHeading) < 1.0) {
                        nearestRoad = member
                        break
                    }
                }
            }
        } else {
            nearestRoad =
                roadTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler) as Way?
        }
    }

    // If we're on a mapped sidewalk, use the associated road for intersection detection instead of
    // the sidewalk itself.
    if(nearestRoad?.isSidewalkOrCrossing() == true) {
        if(nearestRoad.properties?.get("pavement") == null) {
            // Confect the names for the sidewalk first, this should come up with the name of the
            // associated road.
            confectNamesForRoad(nearestRoad, gridState)
        }
        // There could be multiple Ways which share the same pavement name, and we want to pick the
        // right one to use. We want the Way to be running in the same direction as the pavement is,
        // and the nearest of those.
        var bestRoad: Way? = null
        var bestRoadDistance = Double.MAX_VALUE
        for(road in fovRoads.features) {
            if(nearestRoad.properties?.get("pavement") == road.properties?.get("name")) {
                if(userGeometry.mapMatchedLocation?.point != null) {
                    val roadDistance =
                        userGeometry.ruler.distanceToLineString(userGeometry.mapMatchedLocation.point, road.geometry as LineString)
                    val snappedHeading = userGeometry.snappedHeading()
                    if (snappedHeading != null) {
                        if (calculateHeadingOffset(roadDistance.heading, snappedHeading) > 45.0) {
                            // This way is not at the angle of travel, so skip it
                            continue
                        }
                    }
                    if (roadDistance.distance < bestRoadDistance) {
                        bestRoad = road as Way
                        bestRoadDistance = roadDistance.distance
                    }
                }
            }
        }
        nearestRoad = bestRoad
    }

    // Find intersections within FOV
    val fovIntersections = intersectionTree.getAllWithinTriangle(triangle)
    if(fovIntersections.features.isEmpty()) return IntersectionDescription(nearestRoad, userGeometry)

    // Remove intersections which are:
    //  1. Short paths leading to sidewalks of the road, or
    //  2. Direct intersections with sidewalks.
    //  3. Within a 5m radius of the current location
    val trimmedIntersections = FeatureCollection()
    for(i in fovIntersections.features) {
        val intersection = i as Intersection
        var add = true
        if(!userGeometry.inStreetPreview && userGeometry.ruler.distance(intersection.location, userGeometry.mapMatchedLocation?.point ?: userGeometry.location) < 5.0)
            add = false
        else {
            for (way in i.members) {
                if (way.isSidewalkOrCrossing())
                    add = false
                else if (way.isSidewalkConnector(intersection, nearestRoad, gridState))
                    add = false
            }
        }
        if(add)
            trimmedIntersections.features.add(intersection)
    }

    // Sort the FOV intersections by distance
    val sortedFovIntersections = sortedByDistanceTo(userGeometry.mapMatchedLocation?.point ?: userGeometry.location, trimmedIntersections)

    // Inspect each intersection so as to skip trivial ones
    val nonTrivialIntersections = emptyList<Pair<Int, Intersection>>().toMutableList()

    for (intersection in sortedFovIntersections.features) {
        val intersectionLocation = (intersection.geometry as Point).coordinates
        val graphIntersection = gridState.gridIntersections[intersectionLocation]
        if(graphIntersection != null) {
            if((userGeometry.mapMatchedLocation != null) && (nearestRoad != null)) {
                // If our current matched way ends at this intersection, then we don't need to use
                // more elaborate (Dijkstra) pathfinding to check the connection.
                if(!nearestRoad.intersections.contains(graphIntersection)) {

                    // Check if we can get to the intersection from our current location within a
                    // short distance. If we can, check that we don't go through any other valid
                    // intersections first.
                    val shortestDistanceResults = findShortestDistance(
                        userGeometry.mapMatchedLocation.point,
                        nearestRoad,
                        intersectionLocation,
                        (intersection as Intersection).members.first(),
                        null,
                        null,
                        50.0
                    )
                    if (shortestDistanceResults.distance < 50.0) {
                        var skip = false
                        val ways = getPathWays(graphIntersection)
                        var nextIntersection = graphIntersection
                        for(way in ways) {
                            nextIntersection = way.getOtherIntersection(nextIntersection!!)

                            nextIntersection?.let { next ->
                                var count = 0
                                if (next.members.size > 2) {
                                    for(member in next.members) {
                                        if(member.properties != null) {
                                            if (member.isSidewalkOrCrossing() &&
                                                !member.isSidewalkConnector(intersection, nearestRoad, gridState)
                                            ) {
                                                count++
                                            }
                                        }
                                    }
                                    if(count > 2)
                                        skip = true
                                    return@let
                                }
                            }
                            if(skip)
                                break
                        }
                        if(skip) {
                            // Skip this intersection, as it's not the nearest one of interest
                            shortestDistanceResults.tidy()
                            continue
                        }
                    } else {
                        shortestDistanceResults.tidy()
                        continue
                    }
                    shortestDistanceResults.tidy()
                }
            }

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

    // Find the bearing that we're coming in at - measured to the nearest intersection
    val heading = nearestRoad?.heading(intersection!!)
    //val heading = userGeometry.heading()
    if(heading != null) {
        // And use the polygons to describe the roads at the intersection
        return IntersectionDescription(
            nearestRoad,
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
    localizedContext: Context?,
    results: MutableList<PositionedString>,
    calloutHistory: CalloutHistory? = null,
    gridState: GridState
) {

    // Report nearby road
    if(description.intersection == null) {
        description.nearestRoad?.let { nearestRoad ->

            // Figure out which direction we're travelling along the way
            var direction: Boolean? = null
            if (description.userGeometry.mapMatchedLocation != null) {
                direction = when (description.userGeometry.snappedHeading()) {
                    description.userGeometry.mapMatchedLocation.heading ->
                        true

                    (description.userGeometry.mapMatchedLocation.heading + 180.0) % 360.0 ->
                        false

                    else ->
                        // If the direction of travel is more 'across' than 'along' then skip the
                        // description this time. Once the direction is better aligned we can call it
                        // out. This avoids calling out roads that the user is crossing over as
                        // 'ahead'.
                        null
                }
            }
            if (direction != null) {
                val calloutText = if (localizedContext == null)
                    "Ahead ${(nearestRoad).getName(direction, gridState, localizedContext)}"
                else
                    "${localizedContext.getString(R.string.directions_direction_ahead)} ${
                        (nearestRoad).getName(
                            direction,
                            gridState,
                            localizedContext
                        )
                    }}"

                var skip = false
                calloutHistory?.checkAndAdd(
                    TrackedCallout(
                        calloutText,
                        LngLatAlt(),
                        isPoint = false,
                        isGeneric = false
                    )
                )?.let { newCallout ->
                    if (!newCallout) skip = true
                }
                if (!skip) {
                    results.add(
                        PositionedString(
                            text = calloutText,
                            type = AudioType.STANDARD
                        )
                    )
                }
            }
        }
        return
    }

    val intersectionName = description.intersection.name

    // It's possible to get here and the nearestRoad is NOT a member of the intersection. This is
    // particularly likely where there are sidewalks breaking up the road segments. So we need to
    // follow our nearestRoad to the intersection
    if(description.nearestRoad?.containsIntersection(description.intersection) != true) {
        if(description.nearestRoad == null)
            return

        val shortestDistanceResults = findShortestDistance(
            description.userGeometry.mapMatchedLocation?.point ?: description.userGeometry.location,
            description.nearestRoad!!,
            null, null, description.intersection,
            null,
            50.0
        )
        val ways = getPathWays(shortestDistanceResults.endIntersection)
        description.nearestRoad = ways.firstOrNull()

        shortestDistanceResults.tidy()
    }

    val heading = description.nearestRoad?.heading(description.intersection)
    if(heading == null)
        return
    if(description.intersection.members.size <= 2)
        return

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
            text = localizedContext?.getString(R.string.intersection_approaching_intersection) ?: "Approaching intersection",
            earcon = NativeAudioEngine.EARCON_SENSE_POI,
            type = AudioType.STANDARD)
    )

    // Report roads that join the intersection
    val incomingHeading = (heading.toDouble() + 180.0) % 360.0

    val directions = getCombinedDirectionSegments(incomingHeading)
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
            var unlocalizedDirection = ""
            if(localizedContext == null) {
                unlocalizedDirection = when(direction) {
                    Direction.BEHIND_LEFT.value,Direction.LEFT.value,Direction.AHEAD_LEFT.value ->
                        "goes left"
                    Direction.BEHIND_RIGHT.value,Direction.RIGHT.value,Direction.AHEAD_RIGHT.value ->
                        "goes right"
                    else ->
                        "continues ahead"
                }

            }

            val presentationHeading = incomingHeading + when(direction) {
                Direction.BEHIND_LEFT.value,Direction.LEFT.value,Direction.AHEAD_LEFT.value -> -90.0
                Direction.BEHIND_RIGHT.value,Direction.RIGHT.value,Direction.AHEAD_RIGHT.value -> 90.0
                else -> 0.0
            }

            var destinationText = way.getName(way.intersections[WayEnd.START.id] == description.intersection, gridState, localizedContext)
            val intersectionCallout =
                localizedContext?.getString(roadDirectionId, destinationText) ?: "\t$destinationText $unlocalizedDirection"
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
