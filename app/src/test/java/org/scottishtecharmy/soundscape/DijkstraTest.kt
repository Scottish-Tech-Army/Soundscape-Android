package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.distance
import java.util.PriorityQueue


class DijkstraTest {
    // I've taken the algorithm example and original graph data (converted it to real GeoJSON
    // coordinates) from here: https://www.baeldung.com/kotlin/dijkstra-algorithm-graphs
    @Test
    fun testDijkstra(){
        // turning example into GeoJSON
        val lineString1 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0, 0.0),
                LngLatAlt(0.0000635204829044694,0.00006352048290443037)
            )
        }
        val lineString2 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0, 0.0),
                LngLatAlt(0.0000952807243567529, -0.00009528072435662113)
            )
        }
        val lineString3 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0000635204829044694, 0.00006352048290443037),
                LngLatAlt(0.00016274891237997067, 2.1400513539974178E-5)
            )
        }
        val lineString4 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0000952807243567529, -0.00009528072435662113),
                LngLatAlt(0.00016274891237997067, 2.1400513539974178E-5)
            )
        }
        val lineString5 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.00016274891237997067, 2.1400513539974178E-5),
                LngLatAlt(2.3897349186531512E-4, -5.4824065945354336E-5)
            )
        }
        val lineString6 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.00016274891237997067, 2.1400513539974178E-5),
                LngLatAlt(2.580296367367894E-4, 1.1668123789656576E-4)
            )
        }
        val feature1 = Feature()
        feature1.geometry = lineString1
        val feature2 = Feature()
        feature2.geometry = lineString2
        val feature3 = Feature()
        feature3.geometry = lineString3
        val feature4 = Feature()
        feature4.geometry = lineString4
        val feature5 = Feature()
        feature5.geometry = lineString5
        val feature6 = Feature()
        feature6.geometry = lineString6
        // I'll tidy this up later but just want some fixed GeoJSON data to play with
        val featureCollection = FeatureCollection()
        featureCollection.addFeature(feature1)
        featureCollection.addFeature(feature2)
        featureCollection.addFeature(feature3)
        featureCollection.addFeature(feature4)
        featureCollection.addFeature(feature5)
        featureCollection.addFeature(feature6)

        //******** The above is the GeoJSON equivalent of the graph below *********
        /*val graph = mapOf<Int, List<Pair<Int, Int>>>(
            1 to listOf(Pair(2, 10), Pair(3, 15)),
            2 to listOf(Pair(4, 12)),
            3 to listOf(Pair(4, 15)),
            4 to listOf(Pair(5, 12), Pair(6, 15)),
            5 to emptyList(),
            6 to emptyList()
        )*/
        /*val shortestPath = dijkstraWithLoops(graph, 1)
        Assert.assertEquals(37, shortestPath.getValue(6))*/
        //val graph = featureCollectionToGraph(featureCollection)
        //val shortestPath = dijkstraWithLoops(graph, 1)
        //Assert.assertEquals(37, shortestPath.getValue(6))

        //val (shortestPath, previousNodes) = dijkstraWithLoops(graph, 1, nodeMap)
        //val pathCoordinates = getPathCoordinates(6, previousNodes, nodeMap)
        //Assert.assertEquals(37, shortestPath.getValue(6))
        // Get both graph and nodeMap
        val (graph, nodeMap) = featureCollectionToGraphWithNodeMap(featureCollection)
        // Pass nodeMap to dijkstraWithLoops and start node
        val (shortestPath, previousNodes) = dijkstraWithLoops(graph, 1)
        // get the path coordinates to the end node (destination)
        val shortestPathCoordinates = getPathCoordinates(6, previousNodes, nodeMap)

        // The shortest path to node 6 should be 37 metres
        Assert.assertEquals(37, shortestPath.getValue(6))
        // Start node 1 location
        Assert.assertEquals(LngLatAlt(0.0,0.0), shortestPathCoordinates[0])

        // generate a LineString from the shortestPathCoordinates to visualise
        val lineStringRoute = LineString().also {
            it.coordinates = arrayListOf(
                shortestPathCoordinates[0],
                shortestPathCoordinates[1],
                shortestPathCoordinates[2],
                shortestPathCoordinates[3]
            )
        }
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val routeString = moshi.adapter(LineString::class.java).toJson(lineStringRoute)
        println(routeString)

    }

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
            val (node, currentDist) = priorityQueue.poll()
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

    fun getPathCoordinates(
        endNode: Int,
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

        val startCoordinate = nodeMap.entries.find { it.value == 1 }?.key
        startCoordinate?.let { pathCoordinates.add(0, it) }

        return pathCoordinates
    }

    fun featureCollectionToGraphWithNodeMap(
        featureCollection: FeatureCollection
    ): Pair<Map<Int, List<Pair<Int, Int>>>, Map<LngLatAlt, Int>> {
        // take the feature collection and explode the linestring coordinates
        // into pairs of coordinates as linestrings
        val explodedFeatureCollection = explodeLineStrings(featureCollection)
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

    private fun getNode(coordinate: LngLatAlt, nodeMap: MutableMap<LngLatAlt, Int>, nodeIdCounter: Int): Int {
        return nodeMap.computeIfAbsent(coordinate) { nodeIdCounter }
    }

    @Test
    fun explodeLineStringTest(){
        val featureCollection = FeatureCollection().also {
            it.addFeature(
                Feature().also { feature ->
                    feature.geometry = LineString().also {
                        lineString ->
                        lineString.coordinates = arrayListOf(
                        LngLatAlt(0.0, 0.0),
                        LngLatAlt(1.0, 1.0),
                        LngLatAlt(2.0, 0.0)
                        )
                    }
                }
            )
        }

        val explodedFeatureCollection = explodeLineStrings(featureCollection)
        Assert.assertEquals(2, explodedFeatureCollection.features.size)
    }

    private fun explodeLineStrings(featureCollection: FeatureCollection): FeatureCollection {
        val explodedFeatureCollection = FeatureCollection()

        for (feature in featureCollection.features) {
            if (feature.geometry is LineString) {
                val lineString = feature.geometry as LineString
                val coordinates = lineString.coordinates

                for (i in 0 until coordinates.size - 1) {
                    val start = coordinates[i]
                    val end = coordinates[i + 1]

                    val segmentLineString = LineString().also {
                        it.coordinates = arrayListOf(start, end)
                    }

                    val segmentFeature = Feature().also {
                        it.geometry = segmentLineString
                    }

                    explodedFeatureCollection.addFeature(segmentFeature)
                }
            }
        }

        return explodedFeatureCollection
    }

}