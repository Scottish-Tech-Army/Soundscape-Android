package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.dijkstraOnWaysWithLoops
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.utils.dijkstraWithLoops
import org.scottishtecharmy.soundscape.geoengine.utils.featureCollectionToGraphWithNodeMap
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance2
import org.scottishtecharmy.soundscape.geoengine.utils.getPathCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.getPathWays
import org.scottishtecharmy.soundscape.geoengine.utils.getShortestRoute
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import kotlin.time.measureTime


class DijkstraTest {
    // I've taken the algorithm example and original graph data (converted it to real GeoJSON
    // coordinates) from here: https://www.baeldung.com/kotlin/dijkstra-algorithm-graphs
    @Test
    fun testDijkstra1(){
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
        val shortestPathCoordinates = getPathCoordinates(6, 1, previousNodes, nodeMap)

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

    @Test
    fun testDijkstra2(){
        // Get the data for the entire tile
        val gridState = getGridStateForLocation(woodlandWayTestLocation, 1)
        val startLocation = woodlandWayTestLocation
        val endLocation = LngLatAlt(-2.6930021169370093,51.43942502273583)
        // get the roads Feature Collection.
        val testRoadsCollection = gridState.getFeatureCollection(TreeId.ROADS)

        val shortestRoute = getShortestRoute(startLocation, endLocation, testRoadsCollection)
        Assert.assertEquals(262, shortestRoute.features[0].properties?.get("length"))
        // Visualise the shortest route
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val routeString = moshi.adapter(FeatureCollection::class.java).toJson(shortestRoute)
        println("Shortest path: $routeString")

    }

    @Test
    fun testMvtDijkstra(){

        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 2)

        val testRoadsCollection = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        val intersectionsTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

//        val startLocation = LngLatAlt(-4.3187203, 55.9425631)
//        val startLocation = LngLatAlt(-4.3173752, 55.9402158)
        val startLocation = LngLatAlt(-4.3174425, 55.9397239)
        val endLocation = LngLatAlt(-4.3166694, 55.9391411)


        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("shortest-route.geojson")

        val shortestRoutes = FeatureCollection()
        val timeTakenUsingOldAlgorithm = measureTime {
            getShortestRoute(startLocation, endLocation, testRoadsCollection)
        }

        // We should already have these values in the real code, so don't time them
        val startIntersection = intersectionsTree.getNearestFeature(startLocation) as Intersection
        val endIntersection = intersectionsTree.getNearestFeature(endLocation) as Intersection
        var shortestPath = 0.0
        val timeTakenUsingNewAlgorithm = measureTime {
            /**
             * We already have a Way/Intersection graph which has all of the lines segmented at
             * intersections and with a length value for each Way. We don't have a node ID, but
             * the intersections are all unique objects, so that should be enough.
             */
            val shortestPathDistance = dijkstraOnWaysWithLoops(startIntersection, endIntersection)
            val ways = getPathWays(
                endIntersection,
                startIntersection
            )
            for(way in ways) {
                shortestRoutes.addFeature(way as Feature)
            }
            shortestPath = shortestPathDistance
        }


        // Visualise the shortest routes
        mapMatchingOutput.write(adapter.toJson(shortestRoutes).toByteArray())
        mapMatchingOutput.close()
        println("getShortestRoute time taken: $timeTakenUsingOldAlgorithm")
        println("NEW getShortestRoute time taken: $timeTakenUsingNewAlgorithm, distance $shortestPath")
    }

    @Test
    fun testMvtArbitraryDijkstra(){

        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 2)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
        val intersectionsTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // dijkstraOnWaysWithLoops works from intersection to intersection as those are the nodes
        // in our graph. The original dijkstraWithLoops split the ways of the graph into separate
        // segments between the intersections. That's a lot more nodes, but it got us closer to the
        // location we're interested in.
        // To guarantee that we find the shortest route, we actually need to run the search up to 4
        // times. That's because we have to search between each combination of nodes at the ends of
        // each Way, adding on the distances to each node in each case. The lowest distance wins.
        // We can just run the search 2 times, if we let it find both end nodes in one pass.

        val startLocation = LngLatAlt(-4.3187203, 55.9425631)
//        val startLocation = LngLatAlt(-4.3173752, 55.9402158)
//        val startLocation = LngLatAlt(-4.317351, 55.939856)
        val endLocation = LngLatAlt(-4.316699, 55.939225)

        // Find the nearest ways to each location
        val startWay = roadTree.getNearestFeature(startLocation) as Way
        val endWay = roadTree.getNearestFeature(endLocation) as Way

        var shortestPath = 0.0
        val shortestRoute = FeatureCollection()
        val timeTaken = measureTime {
            shortestPath = findShortestDistance(startLocation, endLocation, startWay, endWay, shortestRoute)
        }

        println("time taken: $timeTaken, distance $shortestPath")

        // Visualise the shortest routes
        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("shortest-route.geojson")
        mapMatchingOutput.write(adapter.toJson(shortestRoute).toByteArray())
        mapMatchingOutput.close()
    }
    @Test
    fun testMvtArbitraryDijkstra2(){

        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 2)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)

        val startLocation = LngLatAlt(-4.3187203, 55.9425631)
//        val startLocation = LngLatAlt(-4.3173752, 55.9402158)
//        val startLocation = LngLatAlt(-4.317351, 55.939856)
        val endLocation = LngLatAlt(-4.316699, 55.939225)

        // Find the nearest ways to each location
        val startWay = roadTree.getNearestFeature(startLocation) as Way
        val endWay = roadTree.getNearestFeature(endLocation) as Way

        var shortestPath = 0.0
        val shortestRoute = FeatureCollection()
        val timeTaken = measureTime {
            shortestPath = findShortestDistance2(startLocation, endLocation, startWay, endWay, shortestRoute)
        }

        println("time taken: $timeTaken, distance $shortestPath")

        // Visualise the shortest routes
        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("shortest-route.geojson")
        mapMatchingOutput.write(adapter.toJson(shortestRoute).toByteArray())
        mapMatchingOutput.close()
    }
}