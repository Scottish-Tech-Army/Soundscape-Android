package org.scottishtecharmy.soundscape.geoengine.utils

import leakcanary.FragmentAndViewModelWatcher
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.util.PriorityQueue

var dijkstraRunCount = 0

fun dijkstraOnWaysWithLoops(
    start: Intersection,
    end: Intersection,
    maxDistance: Double = Double.MAX_VALUE
): Double {

    val cheapRuler = CheapRuler(start.location.latitude, meters)

    dijkstraRunCount++

    // Return distances, previous nodes are in the Intersection internal data
    // The priority queue is sorted by the closest node to the target, but also contains the current
    // accumulated distance to that node
    val priorityQueue = PriorityQueue<Triple<Intersection, Double, Double>>(compareBy { it.third })
    val visited = mutableSetOf<Pair<Intersection, Double>>()
    var distanceToEnd = maxDistance

    priorityQueue.add(Triple(start,0.0,0.0))
    start.dijkstraRunCount = dijkstraRunCount
    start.dijkstraDistance = 0.0
    start.dijkstraPrevious = null
    end.dijkstraRunCount = dijkstraRunCount
    end.dijkstraDistance = Double.MAX_VALUE
    end.dijkstraPrevious = null

    while (priorityQueue.isNotEmpty()) {
        val (node, currentDist) = priorityQueue.poll()!!
        if (visited.add(node to currentDist)) {
            node.members.forEach { way ->
                val weight = way.length
                val adjacent = if (node == way.intersections[WayEnd.START.id])
                    way.intersections[WayEnd.END.id]
                else
                    way.intersections[WayEnd.START.id]

                if (adjacent != null) {
                    val totalDist = currentDist + weight
                    val directDistanceToEnd = cheapRuler.distance(adjacent.location, end.location)
                    if ((totalDist + directDistanceToEnd) < distanceToEnd) {
                        if(adjacent.dijkstraRunCount != dijkstraRunCount) {
                            // Lazy initialization of internal distance
                            adjacent.dijkstraRunCount = dijkstraRunCount
                            adjacent.dijkstraDistance = Double.MAX_VALUE
                            adjacent.dijkstraPrevious = null
                        }
                        if (totalDist < adjacent.dijkstraDistance) {

                            adjacent.dijkstraDistance = totalDist
                            adjacent.dijkstraPrevious = node
                            priorityQueue.add(Triple(adjacent, totalDist, (totalDist + directDistanceToEnd)))
                            if(adjacent == end)
                            {
                                return end.dijkstraDistance
                            }
                        }
                    }
                }
            }
        }
    }
    return end.dijkstraDistance
}

/**
 * findShortestDistance inserts the start and end locations as connected nodes in the graph so that
 * it only needs to call dijkstraOnWaysWithLoops a single time.
 */
class ShortestDistanceResults(val distance: Double,
                              val startIntersection: Intersection,
                              val startWay: Way,
                              val endIntersection: Intersection,
                              val endWay: Way) {

    fun tidy() {
        startWay.removeIntersection(startIntersection)
        endWay.removeIntersection(endIntersection)
    }
}
fun findShortestDistance(
    startLocation: LngLatAlt,
    endLocation: LngLatAlt,
    startWay: Way,
    endWay: Way,
    debugFeatureCollection: FeatureCollection?,
    maxDistance: Double = Double.MAX_VALUE,
) : ShortestDistanceResults  {

    val newStartIntersection = startWay.createTemporaryIntersectionAndWays(startLocation)
    val newEndIntersection = endWay.createTemporaryIntersectionAndWays(endLocation)

    val shortestDistance = dijkstraOnWaysWithLoops(
        newStartIntersection,
        newEndIntersection,
        maxDistance
    )

    if (debugFeatureCollection != null) {
        debugFeatureCollection.features.clear()
        val ways = getPathWays(
            newEndIntersection
        )
        for (way in ways) {
            debugFeatureCollection.addFeature(way as Feature)
        }
    }

    return ShortestDistanceResults(
        shortestDistance,
        newStartIntersection,
        startWay,
        newEndIntersection,
        endWay)
}

fun getPathWays(
    endNode: Intersection
): List<Way> {
    val ways = mutableListOf<Way>()
    var currentNode : Intersection? = endNode

    while (currentNode?.dijkstraPrevious != null) {
        val previousNode = currentNode.dijkstraPrevious
        // Add Way which connects the two nodes
        for(member in currentNode.members){
            if(
                (
                    (member.intersections[WayEnd.START.id] == currentNode) and
                    (member.intersections[WayEnd.END.id] == previousNode)
                ) or
                (
                    (member.intersections[WayEnd.END.id] == currentNode) and
                    (member.intersections[WayEnd.START.id] == previousNode)
                )
            ) {
                ways.add(member)
            }
        }
        currentNode = previousNode
    }

    return ways
}

fun featureCollectionToGraphWithNodeMap(
    featureCollection: FeatureCollection
): Pair<Map<Int, List<Pair<Int, Int>>>, Map<LngLatAlt, Int>> {
    // take the feature collection and explode the linestring coordinates
    // into pairs of coordinates as LineStrings
    val explodedFeatureCollection = explodeLineString(featureCollection)
    val nodeMap = mutableMapOf<LngLatAlt, Int>()
    var nodeIdCounter = 1

    val graph = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()

    for (feature in explodedFeatureCollection.features) {
        if (feature.geometry is LineString) {
            val lineString = feature.geometry as LineString
            val coordinates = lineString.coordinates

            val startNode = getNode(coordinates[0], nodeMap, nodeIdCounter)
            nodeIdCounter = if (startNode == nodeIdCounter) nodeIdCounter + 1 else nodeIdCounter
            val endNode = getNode(coordinates[1], nodeMap, nodeIdCounter)
            nodeIdCounter = if (endNode == nodeIdCounter) nodeIdCounter + 1 else nodeIdCounter


            val weight = distance(
                coordinates[0].latitude,
                coordinates[0].longitude,
                coordinates[1].latitude,
                coordinates[1].longitude
            ).toInt()

            graph.computeIfAbsent(startNode) { mutableListOf() }.add(Pair(endNode, weight))
            // For undirected graph which is what we want for pedestrians as we don't care about one-way streets
            graph.computeIfAbsent(endNode) { mutableListOf() }.add(Pair(startNode, weight))
        }
    }

    return Pair(graph, nodeMap)
}

fun getNode(coordinate: LngLatAlt, nodeMap: MutableMap<LngLatAlt, Int>, nodeIdCounter: Int): Int {
    return nodeMap.computeIfAbsent(coordinate) { nodeIdCounter }
}
