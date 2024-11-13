package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Assert
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.dijkstraWithLoops
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.explodeLineString
import org.scottishtecharmy.soundscape.utils.featureCollectionToGraphWithNodeMap
import org.scottishtecharmy.soundscape.utils.getPathCoordinates
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getShortestRoute
import java.util.PriorityQueue


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
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONStreetPreviewTest.streetPreviewTest)
        val startLocation = LngLatAlt( -2.695517313268283,
            51.44082881061331)
        val endLocation = LngLatAlt(-2.6930021169370093,51.43942502273583)
        // get the roads Feature Collection.
        val testRoadsCollection = getRoadsFeatureCollectionFromTileFeatureCollection(
            entireFeatureCollectionTest!!
        )
        val shortestRoute = getShortestRoute(startLocation, endLocation, testRoadsCollection)
        Assert.assertEquals(368, shortestRoute.features[0].properties?.get("length"))
        // Visualise the shortest route
        val routeString = moshi.adapter(FeatureCollection::class.java).toJson(shortestRoute)
        println("Shortest path: $routeString")

    }


}