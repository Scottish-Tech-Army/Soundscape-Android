package org.scottishtecharmy.soundscape.geoengine.filters

import android.location.Location
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs

class NearestRoadFilter {

    private var nearestRoad: Feature? = null
    private var debounceCount = 0
    fun get(): Feature? {
        return nearestRoad
    }

    fun reset() {
        nearestRoad = null
        headingKalmanFilter.reset()
    }
    private val headingKalmanFilter = KalmanHeadingFilter(20.0)

    fun update(location: Location, gridState: GridState) {
        update(
            LngLatAlt(location.longitude, location.latitude),
            location.accuracy.toDouble(),
            location.bearing.toDouble(),                // Use the travel heading
            location.bearingAccuracyDegrees.toDouble(),
            System.currentTimeMillis(),
            gridState
        )
    }

    fun update(location: LngLatAlt,
               locationAccuracy: Double,
               bearing: Double,
               bearingAccuracy: Double,
               timeInMilliseconds: Long,
               gridState: GridState) {

        // Filter the heading based on its accuracy
        val currentHeading = headingKalmanFilter.process(
            bearing,
            timeInMilliseconds,
            bearingAccuracy
        )

        // Find all roads within 20m, and then get a fitness value based on how far the current
        // location is from a road, and how closely aligned the direction of movement is with it.
        val sensedNearestRoads = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
            .generateNearestFeatureCollection(location, 20.0, 10)
        var bestFitness = 0.0
        var bestIndex = -1
        if(sensedNearestRoads.features.isNotEmpty()) {
            for ((index, sensedRoad) in sensedNearestRoads.withIndex()) {
                val sensedRoadInfo = getDistanceToFeature(location, sensedRoad)
                var headingOffSensedRoad =
                    abs((currentHeading % 180) - (sensedRoadInfo.heading % 180))
                if (headingOffSensedRoad > 90)
                    headingOffSensedRoad = 180 - headingOffSensedRoad

                // At 10m distance, the distance fitness factor is halved.
                // At 30 degrees difference the angle fitness factor is also halved.
                // The two factors are simply added in a ratio of 3:1 with distance being prioritized.
                // The result seems reasonably useful - testNearestRoadIdeas runs this algorithm across
                // a grid of points to see how it categorises different locations.
                val w1 = 300.0
                val w2 = 100.0
                val fitness = (w1 * (10 / (10 + sensedRoadInfo.distance))) +
                        (w2 * 30 / (30 + headingOffSensedRoad))
                if (fitness > bestFitness) {
                    bestFitness = fitness
                    bestIndex = index
                }
//                println("fitness: ${fitness} for ${sensedRoad.properties?.get("name")} (${sensedRoadInfo.distance}m, ${headingOffSensedRoad}deg -> $currentHeading vs. ${sensedRoadInfo.heading}")
            }
            val bestMatch = sensedNearestRoads.features[bestIndex]
            nearestRoad?.let { road ->
                if (road != bestMatch) {
                    // It needs to be easier to stay on the current road than to switch to the new one
                    // so make sure it calculates it more than once.
                    debounceCount--
                    if (debounceCount == 0) {
                        nearestRoad = bestMatch
                        debounceCount = 2
                    }
                    return
                } else {
                    debounceCount = 2
                    return
                }
            }
            // We had no nearestRoad, so use bestMatch immediately
            nearestRoad = bestMatch
            debounceCount = 2
        }
    }
}