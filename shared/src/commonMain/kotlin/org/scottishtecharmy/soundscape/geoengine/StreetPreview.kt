package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation

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
    val choices: List<StreetPreviewChoice> = emptyList(),
    val bestChoice: StreetPreviewChoice? = null
)

class StreetPreview {

    enum class PreviewState(val id: Int) {
        INITIAL(0),
        AT_NODE(1)
    }

    private var previewState = PreviewState.INITIAL
    private var previewRoad: StreetPreviewChoice? = null

    private var lastHeading = Double.NaN
    private var currentBestChoice: StreetPreviewChoice? = null
    var running = false
    fun start() {
        previewState = PreviewState.INITIAL
        currentBestChoice = null
        running = true
    }

    fun stop() {
        running = false
    }

    fun go(userGeometry: UserGeometry, gridState: GridState, locationProvider: LocationProvider) : LngLatAlt? {

        when (previewState) {

            PreviewState.INITIAL -> {
                val road: Way = gridState.getNearestFeature(
                    TreeId.WAYS_SELECTION,
                    userGeometry.ruler,
                    userGeometry.location,
                    Double.POSITIVE_INFINITY
                ) as Way? ?: return null
                var nearestDistance = Double.POSITIVE_INFINITY
                var nearestIntersection : Intersection? = null
                for(intersection in road.intersections) {
                    if(intersection != null) {
                        val distanceToIntersection = userGeometry.ruler.distance(userGeometry.location, intersection.location)
                        if(distanceToIntersection < nearestDistance) {
                            nearestIntersection = intersection
                            nearestDistance = distanceToIntersection
                        }
                    }
                }
                if (nearestIntersection != null) {
                    var heading = userGeometry.phoneHeading
                    if(heading == null) heading = 0.0
                    locationProvider.updateLocation(SoundscapeLocation(
                        latitude = nearestIntersection.location.latitude,
                        longitude = nearestIntersection.location.longitude,
                        bearing = heading.toFloat(),
                        hasAccuracy = true,
                    ))
                    previewState = PreviewState.AT_NODE
                    return nearestIntersection.location
                }
            }

            PreviewState.AT_NODE -> {
                val choices = getDirectionChoices(gridState, userGeometry.location)
                var bestIndex = -1
                var bestHeadingDiff = Double.POSITIVE_INFINITY

                val heading = userGeometry.heading()
                if(heading != null) {
                    for ((index, choice) in choices.withIndex()) {
                        val diff = calculateHeadingOffset(choice.heading, heading)
                        if (diff < bestHeadingDiff) {
                            bestHeadingDiff = diff
                            bestIndex = index
                        }
                    }
                }

                if(bestIndex != -1) {
                    previewRoad = choices[bestIndex]
                    previewRoad?.let { road ->
                        var thisIntersection : Intersection? = null
                        for(intersection in road.way.intersections) {
                            if(intersection?.location == userGeometry.location) {
                                thisIntersection = intersection
                            }
                        }
                        if(thisIntersection != null) {
                            val ways = mutableListOf<Pair<Boolean, Way>>()
                            road.way.followWays(thisIntersection, ways) { way, previousWay ->
                                if(previousWay != null) {
                                    if((way.wayType == WayType.JOINER) ||
                                       (previousWay.wayType == WayType.JOINER)) {
                                        false
                                    } else {
                                        (
                                            way.properties?.get("brunnel") !=
                                            previousWay.properties?.get("brunnel")
                                        ) or (
                                            way.name !=
                                            previousWay.name
                                        ) or (
                                            way.featureClass !=
                                            previousWay.featureClass
                                        )
                                    }
                                } else
                                    false
                            }
                            val nextIntersection = if(ways.last().first)
                                ways.last().second.intersections[WayEnd.END.id]
                            else
                                ways.last().second.intersections[WayEnd.START.id]

                            if(nextIntersection != null) {
                                lastHeading = (road.way.heading(nextIntersection) + 180.0) % 360.0
                                locationProvider.updateLocation(SoundscapeLocation(
                                    latitude = nextIntersection.location.latitude,
                                    longitude = nextIntersection.location.longitude,
                                    bearing = lastHeading.toFloat(),
                                    speed = 1.0F,
                                    hasAccuracy = true,
                                ))
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

    fun getDirectionChoices(gridState: GridState, location: LngLatAlt): List<StreetPreviewChoice> {
        val choices = mutableListOf<StreetPreviewChoice>()

        val ruler = CheapRuler(location.latitude)
        val nearestIntersection = gridState.getNearestFeature(TreeId.INTERSECTIONS, ruler, location, 1.0) as Intersection?
        if(nearestIntersection != null) {
            for(member in nearestIntersection.members) {
                choices.add(
                    StreetPreviewChoice(
                        heading = member.heading(nearestIntersection),
                        name = member.getName(
                            member.intersections[WayEnd.START.id] == nearestIntersection,
                            gridState
                        ),
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

    fun updateBestChoice(
        choices: List<StreetPreviewChoice>,
        heading: Double
    ): StreetPreviewChoice? {
        val best = choices.minByOrNull { calculateHeadingOffset(it.heading, heading) }
            ?: return null
        if (best.name == currentBestChoice?.name && best.heading == currentBestChoice?.heading) {
            return null
        }
        currentBestChoice = best
        return best
    }

    fun resetBestChoice() {
        currentBestChoice = null
    }
}
