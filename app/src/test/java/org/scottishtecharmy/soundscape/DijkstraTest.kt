package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.filters.IndexedLineString
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.dijkstraOnWaysWithLoops
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.getPathWays
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import kotlin.time.measureTime


class DijkstraTest {

    @Test
    fun testMvtDijkstra(){

        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 2)

        val intersectionsTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

//        val startLocation = LngLatAlt(-4.3187203, 55.9425631)
//        val startLocation = LngLatAlt(-4.3173752, 55.9402158)
        val startLocation = LngLatAlt(-4.3174425, 55.9397239)
        val endLocation = LngLatAlt(-4.3166694, 55.9391411)


        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("shortest-route.geojson")

        val shortestRoutes = FeatureCollection()

        // We should already have these values in the real code, so don't time them
        val startIntersection = intersectionsTree.getNearestFeature(startLocation, gridState.ruler) as Intersection
        val endIntersection = intersectionsTree.getNearestFeature(endLocation, gridState.ruler) as Intersection
        var shortestPath : Double
        val timeTakenUsingNewAlgorithm = measureTime {
            /**
             * We already have a Way/Intersection graph which has all of the lines segmented at
             * intersections and with a length value for each Way. We don't have a node ID, but
             * the intersections are all unique objects, so that should be enough.
             */
            val shortestPathDistance = dijkstraOnWaysWithLoops(startIntersection, endIntersection, gridState.ruler)
            val ways = getPathWays(
                endIntersection
            )
            for(way in ways) {
                shortestRoutes.addFeature(way as Feature)
            }
            shortestPath = shortestPathDistance
        }


        // Visualise the shortest routes
        mapMatchingOutput.write(adapter.toJson(shortestRoutes).toByteArray())
        mapMatchingOutput.close()
        println("getShortestRoute time taken: $timeTakenUsingNewAlgorithm, distance $shortestPath")
    }

    @Test
    fun testMvtArbitraryDijkstra(){

        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 2)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)

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
        val startWay = roadTree.getNearestFeature(startLocation, gridState.ruler) as Way
        val endWay = roadTree.getNearestFeature(endLocation, gridState.ruler) as Way

        var shortestPath : Double
        val shortestRoute = FeatureCollection()
        val timeTaken = measureTime {
            val results = findShortestDistance(startLocation, startWay, endLocation, endWay, null, shortestRoute)
            shortestPath = results.distance
            results.tidy()
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
        val startWay = roadTree.getNearestFeature(startLocation, gridState.ruler) as Way
        val endWay = roadTree.getNearestFeature(endLocation, gridState.ruler) as Way

        var shortestPath : Double
        val shortestRoute = FeatureCollection()
        val shortestRouteAsSingleLine = FeatureCollection()
        val timeTaken = measureTime {
            val results = findShortestDistance(startLocation, startWay, endLocation, endWay, null, shortestRoute)
            shortestPath = results.distance

            val route = getPathWays(results.endIntersection)

            // Convert the ways into a single line string
            val ils = IndexedLineString()
            ils.updateFromRoute(route)
            val singleLine = Feature()
            singleLine.geometry = ils.line!!
            singleLine.properties = hashMapOf()
            shortestRouteAsSingleLine.addFeature(singleLine)

            results.tidy()
        }

        println("time taken: $timeTaken, distance $shortestPath")

        // Visualise the shortest routes
        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("shortest-route.geojson")
        mapMatchingOutput.write(adapter.toJson(shortestRouteAsSingleLine).toByteArray())
        mapMatchingOutput.close()
    }
}