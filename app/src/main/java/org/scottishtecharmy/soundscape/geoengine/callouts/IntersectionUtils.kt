package org.scottishtecharmy.soundscape.geoengine.callouts

import android.content.Context
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionLabel
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicates
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point

enum class ComplexIntersectionApproach {
    INTERSECTION_WITH_MOST_OSM_IDS,
    NEAREST_NON_TRIVIAL_INTERSECTION
}

data class RoadsDescription(val nearestRoad: Feature? = null,
                            val heading: Double = 0.0,
                            val fovBaseLocation: LngLatAlt = LngLatAlt(),
                            val intersection: Feature? = null,
                            val intersectionRoads: FeatureCollection = FeatureCollection())

/**
 * getRoadsDescriptionFromFov returns a description of the nearestRoad and also the 'best'
 * intersection within the field of view. The description includes the roads that join the
 * intersection, the location of the intersection and the name of the intersection.
 *
 * @param roadTree A FeatureTree of roads to use
 * @param intersectionTree A FeatureTree of intersections to use
 * @param currentLocation The location at the base of the FOV as a LngLatAlt
 * @param deviceHeading The direction for the FOV triangle as a Double
 * @param fovDistance The distance that the FOV triangle covers in metres as a Double
 * @param approach The algorithm used to pick the best intersection.
 *
 * @return An IntersectionDescription containing all the data required for callouts to describe the
 * intersection.
 */
fun getRoadsDescriptionFromFov(gridState: GridState,
                               currentLocation: LngLatAlt,
                               deviceHeading: Double,
                               fovDistance: Double,
                               approach: ComplexIntersectionApproach
) : RoadsDescription {

    // Create FOV triangle
    val points = getFovTrianglePoints(currentLocation, deviceHeading, fovDistance)

    val roadTree = gridState.getFeatureTree(TreeId.ROADS)
    val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

    // Find roads within FOV
    val fovRoads = roadTree.generateFeatureCollectionWithinTriangle(
        currentLocation, points.left, points.right)
    if(fovRoads.features.isEmpty()) return RoadsDescription()

    // Two roads that we are interested in:
    //  1. The one that we are nearest to. We use this for intersection call outs to decide which
    //     intersection road we're on. This can be slightly behind us, it doesn't have to be in our
    //     FOV.
    //  2. The nearest one in our FOV. We use this to describe 'what's ahead'
    val nearestRoad = getNearestRoad(currentLocation, roadTree)
    val nearestRoadInFoV = getNearestRoad(currentLocation, FeatureTree(fovRoads))

    // Find intersections within FOV
    val fovIntersections = intersectionTree.generateFeatureCollectionWithinTriangle(
        currentLocation, points.left, points.right)
    if(fovIntersections.features.isEmpty()) return RoadsDescription(nearestRoadInFoV, deviceHeading, currentLocation)

    // Sort the FOV intersections by distance
    val sortedFovIntersections = sortedByDistanceTo(currentLocation, fovIntersections)

    // Inspect each intersection so as to skip trivial ones
    val nonTrivialIntersections = FeatureCollection()
    for (i in 0 until sortedFovIntersections.features.size) {
        // Get the roads for the intersection
        val intersectionRoads = getIntersectionRoadNames(sortedFovIntersections.features[i], fovRoads)
        // Skip 'simple' intersections e.g. ones where the only roads involved have the same name
        if(checkWhetherIntersectionIsOfInterest(intersectionRoads, nearestRoad)) {
            nonTrivialIntersections.addFeature(sortedFovIntersections.features[i])
        }
    }
    if(nonTrivialIntersections.features.isEmpty()) {
        return RoadsDescription(nearestRoadInFoV, deviceHeading, currentLocation)
    }

    // We have two different approaches to picking the intersection we're interested in
    val intersection: Feature? = when(approach) {
        ComplexIntersectionApproach.INTERSECTION_WITH_MOST_OSM_IDS -> {
            // Pick the intersection feature with the most osm_ids and describe that.
            nonTrivialIntersections.features.maxByOrNull { feature ->
                (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
            }
        }

        ComplexIntersectionApproach.NEAREST_NON_TRIVIAL_INTERSECTION -> {
            // Use the nearest "checked" intersection to the device location?
            nonTrivialIntersections.features[0]
        }
    }

    // Use the nearest intersection, but remove duplicated OSM ids from it (those which loop back)
    val nearestIntersection = removeDuplicates(sortedFovIntersections.features[0])

    // Find the bearing that we're coming in at - measured to the nearest intersection
    val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, nearestRoad, deviceHeading)

    // Create a set of relative direction polygons
    val intersectionLocation = intersection!!.geometry as Point
    val relativeDirections = getRelativeDirectionsPolygons(
        intersectionLocation.coordinates,
        nearestRoadBearing,
        5.0,
        RelativeDirections.COMBINED
    )

    // And use the polygons to describe the roads at the intersection
    val intersectionRoadNames = getIntersectionRoadNames(intersection, fovRoads)
    return RoadsDescription(
        nearestRoadInFoV,
        deviceHeading,
        currentLocation,
        intersection,
        getIntersectionRoadNamesRelativeDirections(
                intersectionRoadNames,
                intersection,
                relativeDirections
        )
    )
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
    calloutHistory: CalloutHistory? = null
) {

    // Report nearby road
    if(description.nearestRoad != null) {

        if (description.nearestRoad.properties?.get("name") != null) {
            val calloutText = "${localizedContext.getString(R.string.directions_direction_ahead)} ${description.nearestRoad.properties!!["name"]}"
            var skip = false
            calloutHistory?.checkAndAdd(TrackedCallout(calloutText,
                    LngLatAlt(),
                    isPoint = false,
                    isGeneric = false
                ))?.let { newCallout ->
                    if(!newCallout) skip = true
            }
            if(!skip) results.add(PositionedString(calloutText))
        } else {
            // we are detecting an unnamed road here but pretending there is nothing here
            results.add(
                PositionedString(
                    localizedContext.getString(R.string.callouts_nothing_to_call_out_now)
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

    // Report distance to intersection
    results.add(
        PositionedString(
            "${localizedContext.getString(R.string.intersection_approaching_intersection)} ${
                localizedContext.getString(
                    R.string.distance_format_meters,
                    description.fovBaseLocation.distance(intersectionLocation).toInt().toString(),
                )
            }",
        ),
    )

    // Report roads that join the intersection
    for (feature in description.intersectionRoads.features) {
        val direction = feature.properties?.get("Direction").toString().toIntOrNull()

        // Don't call out the road we are on (0) as part of the intersection
        if (direction != null && direction != 0) {
            val relativeDirectionString = getRelativeDirectionLabel(
                localizedContext,
                direction
            )
            if (feature.properties?.get("name") != null) {
                val intersectionCallout =
                    localizedContext.getString(
                        R.string.directions_intersection_with_name_direction,
                        feature.properties?.get("name"),
                        relativeDirectionString,
                    )
                results.add(PositionedString(intersectionCallout))
            }
        }
    }
}
