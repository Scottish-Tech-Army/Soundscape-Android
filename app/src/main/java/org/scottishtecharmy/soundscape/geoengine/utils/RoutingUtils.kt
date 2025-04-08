package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.util.PriorityQueue

fun dijkstraWithLoops(
    graph: Map<Int, List<Pair<Int, Int>>>,
    start: Int,
): Pair<Map<Int, Int>, Map<Int, Int>> {
    // Return distances and previous nodes
    val distances = mutableMapOf<Int, Int>().withDefault { Int.MAX_VALUE }
    val previousNodes = mutableMapOf<Int, Int>()
    val priorityQueue = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
    val visited = mutableSetOf<Pair<Int, Int>>()

    priorityQueue.add(start to 0)
    distances[start] = 0

    while (priorityQueue.isNotEmpty()) {
        val (node, currentDist) = priorityQueue.poll()!!
        if (visited.add(node to currentDist)) {
            graph[node]?.forEach { (adjacent, weight) ->
                val totalDist = currentDist + weight
                if (totalDist < distances.getValue(adjacent)) {
                    distances[adjacent] = totalDist
                    previousNodes[adjacent] = node
                    priorityQueue.add(adjacent to totalDist)
                }
            }
        }
    }

    return Pair(distances, previousNodes)
}

fun dijkstraOnWaysWithLoops(
    start: Intersection,
    end: Intersection,
    maxDistance: Double = Double.MAX_VALUE
): Pair<Double, Map<Intersection, Intersection>> {
    // Return distances and previous nodes
    val distances = mutableMapOf<Intersection, Double>().withDefault { Double.MAX_VALUE }
    val previousNodes = mutableMapOf<Intersection, Intersection>()
    val priorityQueue = PriorityQueue<Pair<Intersection, Double>>(compareBy { it.second })
    val visited = mutableSetOf<Pair<Intersection, Double>>()
    var distanceToEnd = maxDistance

    priorityQueue.add(start to 0.0)
    distances[start] = 0.0

    while (priorityQueue.isNotEmpty()) {
        val (node, currentDist) = priorityQueue.poll()!!
        if (visited.add(node to currentDist)) {
            node.members.sortedBy { way ->
                way.length
            }.forEach { way ->
                val weight = way.length
                val adjacent = if (node == way.intersections[WayEnd.START.id])
                    way.intersections[WayEnd.END.id]
                else
                    way.intersections[WayEnd.START.id]

                if (adjacent != null) {
                    val totalDist = currentDist + weight
                    // If the distance + the shortest direct distance to the end is more than the
                    // current shortest distance to the end, then we don't need to process this
                    // option. This is A* rather than Djikstra
                    val directDistanceToEnd = adjacent.location.distance(end.location)
                    if ((totalDist + directDistanceToEnd) < distanceToEnd) {
                        if (totalDist < distances.getValue(adjacent)) {
                            distances[adjacent] = totalDist
                            previousNodes[adjacent] = node
                            priorityQueue.add(adjacent to totalDist)
                            if (adjacent == end)
                                distanceToEnd = totalDist
                        }
                    }
                }
            }
        }
    }

    return Pair(distanceToEnd, previousNodes)
}


fun getPathCoordinates(
    endNode: Int,
    startNode: Int,
    previousNodes: Map<Int, Int>,
    nodeMap: Map<LngLatAlt, Int>
): List<LngLatAlt> {
    val pathCoordinates = mutableListOf<LngLatAlt>()
    var currentNode = endNode

    while (previousNodes.containsKey(currentNode)) {
        val coordinate = nodeMap.entries.find { it.value == currentNode }?.key
        coordinate?.let { pathCoordinates.add(0, it) }
        currentNode = previousNodes.getValue(currentNode)
    }

    val startCoordinate = nodeMap.entries.find { it.value == startNode }?.key
    startCoordinate?.let { pathCoordinates.add(0, it) }

    return pathCoordinates
}

fun getPathWays(
    endNode: Intersection,
    startNode: Intersection,
    previousNodes: Map<Intersection, Intersection>
): List<Way> {
    val ways = mutableListOf<Way>()
    var currentNode = endNode

    while (previousNodes.containsKey(currentNode)) {
        val previousNode = previousNodes.getValue(currentNode)
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

fun nearestNodeToCoordinate(
    nodeMap: Map<LngLatAlt, Int>,
    coordinate: LngLatAlt
): Int {
    var nearestNode = -1 // Initialize with an invalid node ID
    var minDistance = Double.MAX_VALUE

    for ((nodeCoordinate, nodeId) in nodeMap) {
        val distanceToNode = distance(
            coordinate.latitude, coordinate.longitude,
            nodeCoordinate.latitude, nodeCoordinate.longitude
        )

        if (distanceToNode < minDistance) {
            minDistance = distanceToNode
            nearestNode = nodeId
        }
    }

    return nearestNode
}

/**
 * Given a start and end location and a FeatureCollection that contains connected roads
 * it will return a FeatureCollection with a LineString that represents the shortest route
 * between the start and end locations. The shortest route is calculated using Dijkstra's algorithm.
 * @param startLocation
 * A LngLatAlt object representing the start location.
 * @param endLocation
 * A LngLatAlt object representing the end location.
 * @param roads
 * A FeatureCollection containing the roads that make up the map.
 * @return A FeatureCollection containing the shortest route between the start and end location represented as a LineString.
 * The Feature also contains a "length" property that represents the distance of the route.
 */
fun getShortestRoute(
    startLocation: LngLatAlt,
    endLocation: LngLatAlt,
    roads: FeatureCollection
): FeatureCollection {
    // Get both graph and nodeMap
    val (graph, nodeMap) = featureCollectionToGraphWithNodeMap(roads)
    // Get nearest node to current location
    val nearestNodeToStartLocation = nearestNodeToCoordinate(nodeMap, startLocation)
    val nearestNodeToEndLocation = nearestNodeToCoordinate(nodeMap, endLocation)
    // Pass nodeMap to dijkstraWithLoops and start node
    val (shortestPath, previousNodes) = dijkstraWithLoops(graph, nearestNodeToStartLocation)
    // get the path coordinates to the end node (destination)
    val shortestPathCoordinates = getPathCoordinates(
        nearestNodeToEndLocation,
        nearestNodeToStartLocation,
        previousNodes,
        nodeMap
    )

    // generate a LineString from the shortestPathCoordinates
    val arrayList: ArrayList<LngLatAlt> = shortestPathCoordinates.toCollection(ArrayList())
    val lineStringRoute = LineString().also {
        it.coordinates = arrayList

    }
    val featureRoute = Feature().also {
        val ars3: HashMap<String, Any?> = HashMap()
        ars3 += Pair("route", "shortest path")
        ars3 += Pair("length", shortestPath.getValue(nearestNodeToEndLocation))
        it.properties = ars3
    }
    featureRoute.geometry = lineStringRoute
    val lineStringRouteCollection = FeatureCollection()
    lineStringRouteCollection.addFeature(featureRoute)
    return lineStringRouteCollection
}