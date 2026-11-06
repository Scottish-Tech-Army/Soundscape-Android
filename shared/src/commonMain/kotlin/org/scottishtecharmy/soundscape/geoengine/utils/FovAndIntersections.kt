package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

/**
 * The user's field of view: a triangle reaching [UserGeometry.fovDistance] out from where they are,
 * centred on the way they are heading.
 *
 * Null when there is no heading to centre it on. That isn't rare: the GPS course is only trusted
 * while moving and when its accuracy is good enough (see GeoEngine.createUserGeometry), and with
 * the phone locked in a pocket there is no compass heading to fall back on either. This used to
 * point the triangle due north instead, so a user walking south was told about what was behind
 * them as though it were ahead. Each caller decides what "ahead" means without a heading - search
 * all the way round, or say nothing - rather than inherit a guess.
 */
fun getFovTriangle(userGeometry: UserGeometry, forceLocation: Boolean = false): Triangle? {
    val heading = userGeometry.snappedHeading() ?: return null
    val quadrant = Quadrant(heading)
    val location = if (forceLocation) userGeometry.location
    else if (userGeometry.mapMatchedLocation != null) userGeometry.mapMatchedLocation.point
    else userGeometry.location

    return Triangle(
        location,
        getDestinationCoordinate(
            location,
            quadrant.left,
            userGeometry.fovDistance
        ),
        getDestinationCoordinate(
            location,
            quadrant.right,
            userGeometry.fovDistance
        )
    )
}


/**
 * Given an array of Segments and some user geometry with the location and Field of View distance it
 * which represent the FoV triangles it will generate a FeatureCollection of triangles.
 * @param segments
 * An Array<Segment> of degrees to construct triangles
 * @param userGeometry
 * UserGeometry containing the location and Field of View distance
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun makeTriangles(
    segments: Array<Segment>,
    userGeometry: UserGeometry
): FeatureCollection {

    val newFeatureCollection = FeatureCollection()
    for ((count, segment) in segments.withIndex()) {

        val aheadTriangle = createPolygonFromTriangle(
            Triangle(
                userGeometry.location,
                getDestinationCoordinate(
                    userGeometry.location,
                    segment.left,
                    userGeometry.fovDistance
                ),
                getDestinationCoordinate(
                    userGeometry.location,
                    segment.right,
                    userGeometry.fovDistance
                )
            )
        )
        val featureAheadTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Direction", count)
            it.properties = ars3
        }
        featureAheadTriangle.geometry = aheadTriangle
        newFeatureCollection.addFeature(featureAheadTriangle)
    }
    return newFeatureCollection
}

/**
 * A wrapper around:
 * getCombinedDirectionPolygons, getIndividualDirectionPolygons, getAheadBehindDirectionPolygons, getLeftRightDirectionPolygons
 * @param userGeometry
 * Location, heading and FOV distance
 * @param relativeDirectionType
 * Enum for the function you want to use
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getRelativeDirectionsPolygons(
    userGeometry: UserGeometry,
    relativeDirectionType: RelativeDirections
): FeatureCollection {

    // Without a heading the segments are the compass quadrants - ahead is north, right is east.
    // That is a partition of the surroundings rather than a claim about which way the user faces,
    // so it is only fit for a caller that speaks compass directions, as buildWhatsAroundMeCallout
    // does: anything that says "ahead" or "on your left" needs a real heading.
    val heading = userGeometry.heading() ?: 0.0
    val segments =
        when (relativeDirectionType) {
            RelativeDirections.COMBINED -> getCombinedDirectionSegments(heading)
            RelativeDirections.INDIVIDUAL -> getIndividualDirectionSegments(heading)
            RelativeDirections.AHEAD_BEHIND -> getAheadBehindDirectionSegments(heading)
            RelativeDirections.LEFT_RIGHT -> getLeftRightDirectionSegments(heading)
        }

    return makeTriangles(segments, userGeometry)
}

fun checkWhetherIntersectionIsOfInterest(
    intersection: Intersection,
    testNearestRoad: Way?
): Int {
    //println("Number of roads that make up intersection ${intersectionNumber}: ${intersectionRoadNames.features.size}")
    if (testNearestRoad == null)
        return 0

    // We don't announce intersections with only 2 or fewer Ways
    if (intersection.members.size <= 2)
        return -1

    var needsFurtherChecking = 0
    val setOfNames = mutableSetOf<String>()
    for (way in intersection.members) {
        // Pavements and crossings are never called out as roads leaving a junction (see
        // addIntersectionCalloutFromDescription), so they mustn't make one interesting either -
        // their confected "Pavement next to ..." name would otherwise count as a road's. Nor
        // does a tile-edge joiner, which is only an artefact of the tiling.
        if (way.isSidewalkOrCrossing() || (way.wayType == WayType.JOINER)) continue
        // A road is named by its name or, failing that, its route number: an A road or motorway
        // with only a ref is as much a road worth naming as one with a name, and is called out
        // by its ref.
        val roadKey = way.name ?: way.ref
        if (roadKey == null) {
            // Give no points to ways named from their type
            // TODO: give negative points if it's also a dead end i.e. don't call out dead-end
            //  service roads? The current 'priority' isn't good enough, need a better way of
            //  classifying.
            continue
        }
        // Ignore the road we're on - recognised by name or by ref, as AlongWay's sameRoad does -
        // and count each other road once however many of its pieces meet here.
        val isOurRoad = ((way.name != null) && (way.name == testNearestRoad.name)) ||
            ((way.ref != null) && (way.ref == testNearestRoad.ref))
        if (!isOurRoad && setOfNames.add(roadKey)) {
            needsFurtherChecking++
        }
    }
    return needsFurtherChecking
}
