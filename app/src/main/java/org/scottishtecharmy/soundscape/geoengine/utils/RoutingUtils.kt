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

var dijkstraRunCount = 0

fun dijkstraOnWaysWithLoops(
    start: Intersection,
    end: Intersection,
    // TODO: ADD PARAMETER HERE WHICH PASSES IN SOME 'PATCHED IN' NODES THAT ARE OUR START AND
    //  END LOCATIONS. THE START INTERSECTION SHOULD BE EASY AS IT CAN BE ADDED TO THE PRIORITY QUEUE
    //  DIRECTLY - I.E. A FAKE START INTERSECTION WITH WAYS THAT FOLLOW THE EXISTING WAYS. THE
    //  EXISTING WAYS SHOULD EFFECTIVELY BE IGNORED BY THE ALGORITHM. THE END INTERSECTION IS SLIGHTLY
    //  TRICKIER - WE WOULD HAVE UP TO TWO HARD CODED INTERSECTIONS WITH NEW WAYS THAT LINK TO THE
    //  MADE UP END INTERSECTION.
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

fun findShortestDistance(
    startLocation: LngLatAlt,
    endLocation: LngLatAlt,
    startWay: Way,
    endWay: Way,
    debugFeatureCollection: FeatureCollection?,
    maxDistance: Double = Double.MAX_VALUE,
) : Double {

    // Find the distance to the START intersection of each way
    val startDistance = startWay.distanceToStart(startLocation)
    val endDistance = endWay.distanceToStart(endLocation)

    // Find the shortest path
    val startIntersections = startWay.intersections
    val endIntersections = endWay.intersections

//    Debug.startMethodTracing("Predict2")

    // Run through all the combinations of intersections and find the shortest distance. There are
    // a couple of ways that we could do this:
    //  1. Run an A* type search between all 4 combinations.
    //  2. Run two Dijkstra searches, one for each end of the start Way, bailing out when we have
    //  a distance value for both ends of the end Way.
    //
    // In testing, option 1 seems faster as it has the benefit of a directed search. I don't think
    // it's possible to optimize the direction of the search based on both ends of the end Way.
    var shortestIndex = -1
    var shortestDistance = Double.MAX_VALUE
    for(startI in 0 until 2) {
        val addOnStartDistance = if(startI == WayEnd.START.id) startDistance else startWay.length - startDistance
        for (endI in 0 until 2) {
            val addOnEndDistance = if(endI == WayEnd.START.id) endDistance else endWay.length - endDistance
            val startIntersection = startIntersections[startI]
            val endIntersection = endIntersections[endI]
            if((startIntersection != null) && (endIntersection != null)) {
                val shortestPathDistance = dijkstraOnWaysWithLoops(
                    startIntersection,
                    endIntersection,
                    maxDistance
                )
                val totalDistance = shortestPathDistance + addOnStartDistance + addOnEndDistance
                if (totalDistance < shortestDistance) {
                    shortestDistance = totalDistance
                    shortestIndex = (startI * 2) + endI

                    if (debugFeatureCollection != null) {
                        debugFeatureCollection.features.clear()
                        val ways = getPathWays(
                            endIntersections[endI]!!,
                            startIntersections[startI]!!
                        )
                        for (way in ways) {
                            debugFeatureCollection.addFeature(way as Feature)
                        }
                        // Add connections at the start and end
                        val startFeature = Feature()
                        startFeature.geometry =
                            LineString(startLocation, startIntersections[startI]!!.location)
                        debugFeatureCollection.addFeature(startFeature)

                        val endFeature = Feature()
                        endFeature.geometry =
                            LineString(endLocation, endIntersections[endI]!!.location)
                        debugFeatureCollection.addFeature(endFeature)
                    }
                }
            }
        }
    }
//    Debug.stopMethodTracing()
    return shortestDistance
}


fun dijkstraOnWaysWithLoops2(
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
 * findShortestDistance2 inserts the start and end locations as connected nodes in the graph so that
 * it only needs to call dijkstraOnWaysWithLoops2 a single time.
 * TODO: Investigate whether we can improve "members" speed as toMutableList seems expensive.
 */
fun findShortestDistance2(
    startLocation: LngLatAlt,
    endLocation: LngLatAlt,
    startWay: Way,
    endWay: Way,
    debugFeatureCollection: FeatureCollection?,
    maxDistance: Double = Double.MAX_VALUE,
) : Double {

    val newStartIntersection = startWay.createTemporaryIntersectionAndWays(startLocation)
    val newEndIntersection = endWay.createTemporaryIntersectionAndWays(endLocation)

    val shortestDistance = dijkstraOnWaysWithLoops2(
        newStartIntersection,
        newEndIntersection,
        maxDistance
    )

    if (debugFeatureCollection != null) {
        debugFeatureCollection.features.clear()
        val ways = getPathWays(
            newEndIntersection,
            newStartIntersection
        )
        for (way in ways) {
            debugFeatureCollection.addFeature(way as Feature)
        }
    }

    // TODO: We can't remove these yet as they are in the returned debugFeatureCollection
    startWay.removeIntersection(newStartIntersection)
    endWay.removeIntersection(newEndIntersection)

    return shortestDistance
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