package org.scottishtecharmy.soundscape.geoengine

import android.util.Log
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.normaliseHeading
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class StreetPreviewChoice(
    val heading: Double,
    val name: String,
    val way: Way
)

enum class StreetPreviewEnabled {
    OFF, INITIALIZING, ON
}
data class StreetPreviewState(
    val enabled: StreetPreviewEnabled = StreetPreviewEnabled.OFF,
    val choices: List<StreetPreviewChoice> = emptyList()
)

class StreetPreview {

    enum class PreviewState(val id: Int) {
        INITIAL(0),
        AT_NODE(1)
    }

    private var previewState = PreviewState.INITIAL
    private var previewRoad: StreetPreviewChoice? = null

    private var lastHeading = Double.NaN

    fun start() {
        previewState = PreviewState.INITIAL
    }

    fun go(userGeometry: UserGeometry, engine: GeoEngine) : LngLatAlt? {
        when (previewState) {

            PreviewState.INITIAL -> {
                // Jump to an intersection on the nearest road or path
                val road : Way? = engine.gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, userGeometry.location, Double.POSITIVE_INFINITY) as Way
                if(road == null)
                    return null
                var nearestDistance = Double.POSITIVE_INFINITY
                var nearestIntersection : Intersection? = null
                for(intersection in road.intersections) {
                    if(intersection != null) {
                        val distanceToIntersection = userGeometry.location.distance(intersection.location)
                        if(distanceToIntersection < nearestDistance) {
                            nearestIntersection = intersection
                            nearestDistance = distanceToIntersection
                        }
                    }
                }
                if (nearestIntersection != null) {
                    // We've got a location, so jump to it
                    var heading = userGeometry.phoneHeading
                    if(heading == null) heading = 0.0
                    engine.locationProvider.updateLocation(nearestIntersection.location, heading.toFloat(), 0.0F)
                    previewState = PreviewState.AT_NODE
                    return nearestIntersection.location
                }
            }

            PreviewState.AT_NODE -> {
                // Find which road that we're choosing based on our current heading
                val choices = getDirectionChoices(engine, userGeometry.location)
                var bestIndex = -1
                var bestHeadingDiff = Double.POSITIVE_INFINITY

                // Find the choice with the closest heading to our own
                val heading = userGeometry.heading()
                if(heading != null) {
                    for ((index, choice) in choices.withIndex()) {
                        val diff = calculateHeadingOffset(choice.heading, heading)
                        if (diff < bestHeadingDiff) {
                            bestHeadingDiff = diff
                            bestIndex = index
                        }
                        Log.d(TAG, "Choice: ${choice.name} heading: ${choice.heading}")
                    }
                }

                if(bestIndex != -1) {
                    // We've got a road - let's head down it
                    previewRoad = choices[bestIndex]
                    previewRoad?.let { road ->
                        // We want the heading to be the angle of the road we're coming in on
                        var thisIntersection : Intersection? = null
                        // Get the starting intersection
                        for(intersection in road.way.intersections) {
                            if(intersection?.location == userGeometry.location) {
                                thisIntersection = intersection
                            }
                        }
                        if(thisIntersection != null) {
                            // Now follow the road out of our current intersection, and find the
                            // next intersection.
                            val ways: MutableList<Pair<Boolean, Way>> =
                                emptyList<Pair<Boolean, Way>>().toMutableList()
                            road.way.followWays(thisIntersection, ways)
                            var nextIntersection:Intersection? = null
                            nextIntersection = if(ways.last().first)
                                ways.last().second.intersections[WayEnd.END.id]
                            else
                                ways.last().second.intersections[WayEnd.START.id]

                            if(nextIntersection != null) {
                                // We've found the next intersection. Set the heading to be the
                                // angle of the way arriving at the intersection and move to it.
                                lastHeading = (road.way.heading(nextIntersection) + 180.0) % 360.0
                                engine.locationProvider.updateLocation(
                                    nextIntersection.location,
                                    lastHeading.toFloat(),
                                    1.0F
                                )
                                return nextIntersection.location
                            }
                        }
                    }
                    previewState = PreviewState.AT_NODE
                }
            }
        }
        return null
    }

    /**
     * getDirectionChoices returns a List of possible choices at an intersection
     * Each entry contains a heading, the street name and a list of points that make up the road.
     * The app can choose the road based on the heading and then move the user along it.
     */
    fun getDirectionChoices(engine: GeoEngine, location: LngLatAlt): List<StreetPreviewChoice> {
        val choices = mutableListOf<StreetPreviewChoice>()

        val nearestIntersection = engine.gridState.getNearestFeature(TreeId.INTERSECTIONS, location, 1.0) as Intersection?
        if(nearestIntersection != null) {
            for(member in nearestIntersection.members) {
                choices.add(
                    StreetPreviewChoice(
                        heading = member.heading(nearestIntersection),
                        name = member.properties?.get("name").toString(),
                        way = member
                    )
                )
            }
        }
        return choices
    }

    fun getLastHeading() : Double {
        return lastHeading
    }

    companion object {
        private const val TAG = "StreetPreview"
    }
}
