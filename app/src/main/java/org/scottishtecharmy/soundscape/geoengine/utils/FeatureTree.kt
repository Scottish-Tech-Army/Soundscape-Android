package org.scottishtecharmy.soundscape.geoengine.utils

import com.github.davidmoten.rtree2.Entry
import com.github.davidmoten.rtree2.Iterables
import com.github.davidmoten.rtree2.RTree
import com.github.davidmoten.rtree2.geometry.Geometries
import com.github.davidmoten.rtree2.geometry.Geometry
import com.github.davidmoten.rtree2.geometry.Line
import com.github.davidmoten.rtree2.geometry.Point
import com.github.davidmoten.rtree2.geometry.Rectangle
import com.github.davidmoten.rtree2.internal.EntryDefault
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos

/**
 * FeatureTree is a class which stores FeatureCollections within an rtree which gives us faster
 * spatial searching. The APIs are purely FeatureCollections - pass one in to create the tree
 * and then call one of generateFeatureCollection, generateNearbyFeatureCollection or
 * generateNearestFeatureCollection to return a FeatureCollection trimmed down by location.
 */

data class Triangle(val origin: LngLatAlt, val left: LngLatAlt, val right: LngLatAlt)

class FeatureTree(featureCollection: FeatureCollection?) {

    var tree: RTree<Feature, Geometry?>? = null

    init {
        if(featureCollection != null) {
            tree = createRtree(featureCollection)
        }
    }

    private fun createRtree(entries: List<Entry<Feature, Geometry?>>): RTree<Feature, Geometry?> {
        val tree: RTree<Feature, Geometry?> = RTree.create(entries)
        return tree
    }

    private fun createRtree(featureCollection: FeatureCollection): RTree<Feature, Geometry?> {
        val rtreeList = mutableListOf<Entry<Feature, Geometry?>>()
        for (feature in featureCollection) {
            when (feature.geometry.type) {
                "Point" -> {
                    val point =
                        feature.geometry as org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
                    rtreeList.add(
                        EntryDefault(
                            feature,
                            Geometries.pointGeographic(
                                point.coordinates.longitude,
                                point.coordinates.latitude
                            )
                        )
                    )
                }

                "MultiPoint" -> {
                    val multiPoint =
                        feature.geometry as org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
                    for(location in multiPoint.coordinates) {
                        rtreeList.add(
                            EntryDefault(
                                feature,
                                Geometries.pointGeographic(
                                    location.longitude,
                                    location.latitude
                                )
                            )
                        )
                    }
                }

                "LineString" -> {
                    // We add each line segment as a separate entry into the rtree for more precise
                    // searching, however this does mean that searches in the tree will return
                    // duplicates of the same Feature and so these must be de-duplicated when
                    // retrieving the data from the tree.
                    val line = feature.geometry as LineString
                    for ((index, point) in line.coordinates.withIndex()) {
                        if (index < (line.coordinates.size - 1)) {
                            rtreeList.add(
                                EntryDefault(
                                    feature,
                                    Geometries.line(
                                        point.longitude,
                                        point.latitude,
                                        line.coordinates[index + 1].longitude,
                                        line.coordinates[index + 1].latitude
                                    )
                                )
                            )
                        }
                    }
                }

                "Polygon" -> {
                    val polygon = feature.geometry as Polygon
                    // The rtree only supports points, lines, rectangles and circles. Let's create
                    // a bounding box for the polygon and use that in rtree. We can then validate
                    // the search results in a second pass.
                    val box = getBoundingBoxOfPolygon(polygon)
                    rtreeList.add(
                        EntryDefault(
                            feature,
                            Geometries.rectangleGeographic(
                                box.westLongitude, box.southLatitude,
                                box.eastLongitude, box.northLatitude
                            )
                        )
                    )
                }

                "MultiPolygon" -> {
                    val multiPolygon = feature.geometry as MultiPolygon
                    // The rtree only supports points, lines, rectangles and circles. Let's create
                    // a bounding box for the polygon and use that in rtree. We can then validate
                    // the search results in a second pass.
                    val box = getBoundingBoxOfMultiPolygon(multiPolygon)
                    rtreeList.add(
                        EntryDefault(
                            feature,
                            Geometries.rectangleGeographic(
                                box.westLongitude, box.southLatitude,
                                box.eastLongitude, box.northLatitude
                            )
                        )
                    )
                }

                else -> {
                    assert(false)
                }
            }
        }
        return createRtree(rtreeList)
    }

    private fun createBoundingSquare(center: LngLatAlt, radius: Double): Rectangle {

        // Create a bounding square for our search
        val latOffset = (radius) / EARTH_RADIUS_METERS * (180 / PI)
        val lngOffset = (radius) / (EARTH_RADIUS_METERS * cos(Math.toRadians(center.latitude))) * (180 / PI)
        val rect = Geometries.rectangle(
            center.longitude - lngOffset,
            center.latitude - latOffset,
            center.longitude + lngOffset,
            center.latitude + latOffset
        )
        return rect
    }

    private fun distanceToEntry(entry: Entry<Feature, Geometry?>, from: LngLatAlt) : Double {
        when (val p = entry.geometry()) {
            is Point -> {
                val position = LngLatAlt(p.x(), p.y())
                return from.distance(position)
            }

            is Line -> {
                return from.distanceToLine(LngLatAlt(p.x1(), p.y1()),LngLatAlt(p.x2(), p.y2()))
            }

            is Rectangle -> {
                return distanceToPolygon(from, entry.value().geometry as Polygon)
            }
        }
        return Double.POSITIVE_INFINITY
    }

    private fun entryWithinDistance(entry: Entry<Feature, Geometry?>, distance: Double, from: LngLatAlt)  : Boolean {
        return distanceToEntry(entry, from) < distance
    }

    private fun searchWithinDistance(
        lonLat: Point,
        distance: Double
    ): MutableIterable<Entry<Feature, Geometry?>>? {

        // This should not be called if the tree is null
        assert(tree != null)

        // First we need to calculate an enclosing lat long rectangle for this
        // distance then we refine on the exact distance
        val from = LngLatAlt(lonLat.x(), lonLat.y())
        val bounds: Rectangle = createBoundingSquare(from, distance)

        return Iterables.filter(tree!!.search(bounds))
        { entry ->
            entryWithinDistance(entry, distance, from)
        }
    }

    private fun nearestWithinDistance(
        lonLat: Point,
        distance: Double,
        maxCount: Int
    ): MutableIterable<Entry<Feature, Geometry?>>? {

        // This should not be called if the tree is null
        assert(tree != null)

        val from = LngLatAlt(lonLat.x(), lonLat.y())
        return Iterables.filter(tree!!.nearest(lonLat, distance, maxCount))
        { entry ->
            entryWithinDistance(entry, distance, from)
        }
    }

    /**
     * generateFeatureCollection returns a FeatureCollection containing all of the features from
     * within the rtree.
     */
    fun generateFeatureCollection(): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val deduplicationSet = mutableSetOf<Feature>()
            val entries = tree!!.entries()
            for (feature in entries) {
                if(!deduplicationSet.contains(feature.value())) {
                    featureCollection.addFeature(feature.value())
                    deduplicationSet.add(feature.value())
                }
            }
        }
        return featureCollection
    }

    /**
     * generateNearbyFeatureCollection returns a FeatureCollection containing all of the features
     * within distance of the location provided
     */
    fun generateNearbyFeatureCollection(location: LngLatAlt, distance: Double): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            // Return only the entries within distance of our location
            val distanceResults = Iterables.toList(searchWithinDistance(
                Geometries.pointGeographic(location.longitude, location.latitude),
                distance))

            val deduplicationSet = mutableSetOf<Feature>()
            for (feature in distanceResults) {
                if(!deduplicationSet.contains(feature.value())) {
                    featureCollection.addFeature(feature.value())
                    deduplicationSet.add(feature.value())
                }
            }
        }
        return featureCollection
    }

    /**
     * generateNearestFeatureCollection returns a FeatureCollection containing the nearest members
     * of the rtree that are also within distance.
     */

    fun generateNearestFeatureCollection(location: LngLatAlt, distance: Double, maxCount: Int): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val distanceResults = Iterables.toList(nearestWithinDistance(
                Geometries.pointGeographic(location.longitude, location.latitude),
                distance,
                maxCount))

            // Deduplicate returned entries and add them to a list ready to sort by distance
            val deduplicationSet = mutableSetOf<Feature>()
            data class EntryWithDistance(val entry: Entry<Feature, Geometry?>, val distance: Double)
            val unsortedList = emptyList<EntryWithDistance>().toMutableList()
            for (entry in distanceResults) {
                if(!deduplicationSet.contains(entry.value())) {
                    unsortedList.add(EntryWithDistance(entry, distanceToEntry(entry, location)))
                    deduplicationSet.add(entry.value())
                }
            }

            // Sort the list
            val sortedList = unsortedList.sortedBy { entryWithinDistance->
                entryWithinDistance.distance
            }

            // Move the sorted items into a FeatureCollection to return
            for(item in sortedList) {
                featureCollection.addFeature(item.entry.value())
            }
        }
        return featureCollection
    }

    /**
     * getNearestFeature returns a Feature that is the nearest member of the rtree
     * that is also within distance.
     */
    fun getNearestFeature(location: LngLatAlt, distance: Double = Double.POSITIVE_INFINITY): Feature? {
        if(tree != null) {
            val distanceResults = Iterables.toList(
                nearestWithinDistance(
                    Geometries.pointGeographic(location.longitude, location.latitude),
                    distance,
                    1
                )
            )

            for (feature in distanceResults) return feature.value()
        }

        return null
    }

    /**
     * generateFeatureCollectionWithinFov returns a FeatureCollection containing all members
     * of the rtree that are contained within the supplied triangle
     */
    private fun createBoundingSquareContainingTriangle(triangle: Triangle) : Rectangle {

        // Create a bounding rectangle that contains the triangle
        val minLatitude = minOf(triangle.origin.latitude, triangle.left.latitude, triangle.right.latitude)
        val maxLatitude = maxOf(triangle.origin.latitude, triangle.left.latitude, triangle.right.latitude)
        val minLongitude = minOf(triangle.origin.longitude, triangle.left.longitude, triangle.right.longitude)
        val maxLongitude = maxOf(triangle.origin.longitude, triangle.left.longitude, triangle.right.longitude)

        return Geometries.rectangle(minLongitude,minLatitude,maxLongitude,maxLatitude)
    }

    private fun lineSegmentPassesWithinTriangle(p1: LngLatAlt, p2: LngLatAlt,
                                                triangle: Triangle): Boolean {
        // Check if the line segment intersects any of the triangle's edges
        if (straightLinesIntersect(p1, p2, triangle.origin, triangle.left) ||
            straightLinesIntersect(p1, p2, triangle.left, triangle.right) ||
            straightLinesIntersect(p1, p2, triangle.right, triangle.origin)) {
            return true
        }

        // Then check that the line segment isn't completely inside the triangle
        val polygon = createPolygonFromTriangle(triangle)
        return (polygonContainsCoordinates(p1, polygon) && polygonContainsCoordinates(p2, polygon))
    }

    private fun testPolygonInFov(polygon: Polygon, triangle: Triangle) : Boolean {

        // Test if any of the polygon edges intersect with the triangle or are wholly within it
        var lastPoint = polygon.coordinates[0][0]
        for (point in polygon.coordinates[0].drop(1)) {
            if(lineSegmentPassesWithinTriangle(lastPoint, point, triangle)) {
                return true
            }
            lastPoint = point
        }

        // Finally check that the triangle isn't wholly inside the polygon
        return (polygonContainsCoordinates(triangle.origin, polygon) ||
                polygonContainsCoordinates(triangle.left, polygon) ||
                polygonContainsCoordinates(triangle.right, polygon))
    }

    private fun testMultiPolygonInFov(polygon: MultiPolygon, triangle: Triangle) : Boolean {

        // Test only the outer ring of the first polygon
        return testPolygonInFov(Polygon(polygon.coordinates[0][0]), triangle)
    }

    private fun entryWithinTriangle(entry: Entry<Feature, Geometry?>,
                                    triangle: Triangle): Boolean {

        when (val p = entry.geometry()) {
            is Point -> {
                val testPoint = LngLatAlt(p.x(), p.y())
                // Create a closed polygon
                val polygon =createPolygonFromTriangle(triangle)
                return polygonContainsCoordinates(testPoint, polygon)
            }

            is Line -> {
                return lineSegmentPassesWithinTriangle(LngLatAlt(p.x1(), p.y1()),
                    LngLatAlt(p.x2(), p.y2()),
                    triangle)
            }

            is Rectangle -> {
                // The rtree entry is a bounding box for a more complex polygon. We return true if
                // any of the polygon coordinates are within the FOV triangle or if any of the
                // FOV triangle coordinates are within the polygon.
                val feature = entry.value()
                if(feature.geometry.type == "Polygon") {
                    return testPolygonInFov(feature.geometry as Polygon, triangle)
                } else if(feature.geometry.type == "MultiPolygon") {
                    // No MultiPolygons exist from MVT translation, so no need to handle
                    return testMultiPolygonInFov(feature.geometry as MultiPolygon, triangle)
                }

                return false
            }
            else -> {
                println("Unknown geometry type: $p")
            }
        }
        return false
    }

    private fun searchWithinTriangle(
        triangle: Triangle
    ): MutableIterable<Entry<Feature, Geometry?>>? {

        // This should not be called if the tree is null
        assert(tree != null)

        // First we need to calculate an enclosing lat long rectangle for this triangle
        // then we refine on the exact contents
        val bounds: Rectangle = createBoundingSquareContainingTriangle(triangle)

        return Iterables.filter(tree!!.search(bounds))
        { entry ->
            entryWithinTriangle(entry, triangle)
        }
    }

    private fun nearestWithinTriangle(
        triangle: Triangle,
        maxCount: Int
    ): FeatureCollection {

        val results = FeatureCollection()

        if(tree != null) {
            // First find the features within the triangle
            val resultsWithinTriangle = searchWithinTriangle(triangle) ?: return results

            // Sort the results based on the distance. The sortedBy algorithm calls the distance
            // calculation every time it compares values in the list and as a result is fairly
            // inefficient. We could either:
            //
            //  1. Cache calculations - this could be done inside the Feature, and as we are single
            //     threaded when using FeatureTree this will be okay, though slightly ugly.
            //  2. Calculate the distances in advance and sort those instead. We'll take this approach.
            //
            data class EntryWithDistance(val entry: Entry<Feature, Geometry?>, val distance: Double)

            val unsortedList = emptyList<EntryWithDistance>().toMutableList()
            for (entry in resultsWithinTriangle) {
                unsortedList.add(EntryWithDistance(entry, distanceToEntry(entry, triangle.origin)))
            }
            val sortedList = unsortedList.sortedBy { entryWithinDistance ->
                entryWithinDistance.distance
            }

            // Move the sorted items into a FeatureCollection to return, breaking out if we reach the
            // maximum number requested.
            for ((index, item) in sortedList.withIndex()) {
                if (index >= maxCount)
                    break
                results.addFeature(item.entry.value())
            }
        }

        return results
    }

    fun generateNearestFeatureCollectionWithinTriangle(triangle: Triangle,
                                                       maxCount: Int): FeatureCollection {

        if(tree == null) return FeatureCollection()

        return nearestWithinTriangle(triangle, maxCount)
    }


    fun generateFeatureCollectionWithinTriangle(triangle: Triangle): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val results = Iterables.toList(searchWithinTriangle(triangle))

            val deduplicationSet = mutableSetOf<Feature>()
            for (feature in results) {
                if(!deduplicationSet.contains(feature.value())) {
                    featureCollection.addFeature(feature.value())
                    deduplicationSet.add(feature.value())
                }
            }
        }
        return featureCollection
    }

    fun getNearestFeatureWithinTriangle(triangle: Triangle): Feature? {

        if (tree == null)
            return null

        val results = nearestWithinTriangle(triangle, 1)
        if(results.features.isEmpty()) return null

        return results.features[0]
    }
}
