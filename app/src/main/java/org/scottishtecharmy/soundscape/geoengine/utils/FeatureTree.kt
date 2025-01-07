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
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos

/**
 * FeatureTree is a class which stores FeatureCollections within an rtree which gives us faster
 * spatial searching. The APIs are purely FeatureCollections - pass one in to create the tree
 * and then call one of generateFeatureCollection, generateNearbyFeatureCollection or
 * generateNearestFeatureCollection to return a FeatureCollection trimmed down by location.
 */
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

    private fun entryWithinDistance(entry: Entry<Feature, Geometry?>, distance: Double, from: LngLatAlt)  : Boolean {
        when (val p = entry.geometry()) {
            is Point -> {
                val position = LngLatAlt(p.x(), p.y())
                return from.distance(position) < distance
            }

            is Line -> {
                val thisDistance =
                    distanceToLineString(
                        from, LineString(
                            LngLatAlt(p.x1(), p.y1()),
                            LngLatAlt(p.x2(), p.y2())
                        )
                    )
                return thisDistance < distance
            }

            is Rectangle -> {
                val box = BoundingBox(p.x1(), p.y1(), p.x2(), p.y2())
                val nearestPoint = nearestPointOnBoundingBox(box, from)
                return from.distance(nearestPoint) < distance
            }
        }
        return false
    }

    private fun searchWithinDistance(
        lonLat: Point,
        distance: Double
    ): MutableIterable<Entry<Feature, Geometry?>>? {
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
     * getNearestFeature returns a Feature that is the nearest member of the rtree
     * that is also within distance.
     */
    fun getNearestFeature(location: LngLatAlt, distance: Double = Double.POSITIVE_INFINITY): Feature? {
        val distanceResults = Iterables.toList(nearestWithinDistance(
            Geometries.pointGeographic(location.longitude, location.latitude),
            distance,
            1))

        for (feature in distanceResults) return feature.value()

        return null
    }

    /**
     * generateFeatureCollectionWithinFov returns a FeatureCollection containing all members
     * of the rtree that are contained within the supplied triangle
     */
    private fun createBoundingSquareContainingTriangle(triangle: ArrayList<LngLatAlt>) : Rectangle {

        // Create a bounding rectangle that contains the triangle
        val minLatitude = minOf(triangle[0].latitude, triangle[1].latitude, triangle[2].latitude)
        val maxLatitude = maxOf(triangle[0].latitude, triangle[1].latitude, triangle[2].latitude)
        val minLongitude = minOf(triangle[0].longitude, triangle[1].longitude, triangle[2].longitude)
        val maxLongitude = maxOf(triangle[0].longitude, triangle[1].longitude, triangle[2].longitude)

        return Geometries.rectangle(minLongitude,minLatitude,maxLongitude,maxLatitude)
    }

    private fun lineSegmentPassesWithinTriangle(p1: LngLatAlt, p2: LngLatAlt,
                                                triangle: ArrayList<LngLatAlt>): Boolean {
        // Check if the line segment intersects any of the triangle's edges
        if (straightLinesIntersect(p1, p2, triangle[0], triangle[1]) ||
            straightLinesIntersect(p1, p2, triangle[1], triangle[2]) ||
            straightLinesIntersect(p1, p2, triangle[2], triangle[0])) {
            return true
        }

        // Then check that the line segment isn't completely inside the triangle
        val polygon = Polygon(arrayListOf(triangle[0], triangle[1], triangle[2], triangle[0]))
        return (polygonContainsCoordinates(p1, polygon) && polygonContainsCoordinates(p2, polygon))
    }

    private fun entryWithinTriangle(entry: Entry<Feature, Geometry?>,
                                    triangle: ArrayList<LngLatAlt>): Boolean {

        when (val p = entry.geometry()) {
            is Point -> {
                val testPoint = LngLatAlt(p.x(), p.y())
                // Create a closed polygon
                val polygon = Polygon(arrayListOf(triangle[0], triangle[1], triangle[2], triangle[0]))
                return polygonContainsCoordinates(testPoint, polygon)
            }

            is Line -> {
                return lineSegmentPassesWithinTriangle(LngLatAlt(p.x1(), p.y1()),
                    LngLatAlt(p.x2(), p.y2()),
                    triangle)
            }

            is Rectangle -> {
                // The rtree entry is a bounding box for a more complex polygon. We return true if
                // any of the polygon coordinates are within the FOV triangle
                val feature = entry.value()
                for (geometry in (feature.geometry as Polygon).coordinates) {
                    for (point in geometry) {
                        val polygonTriangleFOV = Polygon(arrayListOf(triangle[0], triangle[1], triangle[2], triangle[0]))
                        val containsCoordinate =
                            polygonContainsCoordinates(point, polygonTriangleFOV)
                        if (containsCoordinate) {
                            return true
                        }
                    }
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
        triangleCoordinates: ArrayList<LngLatAlt>
    ): MutableIterable<Entry<Feature, Geometry?>>? {

        // First we need to calculate an enclosing lat long rectangle for this
        // triangle then we refine on the exact contents
        assert(triangleCoordinates.size == 3)
        val bounds: Rectangle = createBoundingSquareContainingTriangle(triangleCoordinates)

        return Iterables.filter(tree!!.search(bounds))
        { entry ->
            entryWithinTriangle(entry, triangleCoordinates)
        }
    }

    private fun nearestWithinTriangle(
        triangleCoordinates: ArrayList<LngLatAlt>,
        maxCount: Int
    ): Iterable<Entry<Feature, Geometry?>>? {

        // First find the features within the triangle
        val resultsWithinTriangle = searchWithinTriangle(triangleCoordinates) ?: return null

        // Check if we already have few enough results
        if(resultsWithinTriangle.count() < maxCount) return resultsWithinTriangle

        // We have too many results, so trim them down to the nearest ones
        val nearestResults = resultsWithinTriangle.sortedBy {
            entry ->
            when (val p = entry.geometry()) {
                is Point -> {
                    val position = LngLatAlt(p.x(), p.y())
                    triangleCoordinates[0].distance(position)
                }

                is Line -> {
                    val thisDistance =
                        distanceToLineString(
                            triangleCoordinates[0], LineString(
                                LngLatAlt(p.x1(), p.y1()),
                                LngLatAlt(p.x2(), p.y2())
                            )
                        )
                    thisDistance
                }

                is Rectangle -> {
                    val box = BoundingBox(p.x1(), p.y1(), p.x2(), p.y2())
                    val center = getCenterOfBoundingBox(getBoundingBoxCorners(box))
                    triangleCoordinates[0].distance(center)
                }
                else -> Double.POSITIVE_INFINITY
            }
        }

        return nearestResults.subList(
            fromIndex = 0,
            toIndex = maxCount
        )
    }

    fun generateNearestFeatureCollectionWithinTriangle(location: LngLatAlt,
                                                       left: LngLatAlt,
                                                       right: LngLatAlt,
                                                       maxCount: Int): FeatureCollection {

        val featureCollection = FeatureCollection()
        if(tree != null) {

            val results =
                nearestWithinTriangle(arrayListOf(location, left, right), maxCount) ?: return featureCollection

            for (feature in results) {
                featureCollection.addFeature(feature.value())
            }
        }
        return featureCollection
    }


    fun generateFeatureCollectionWithinTriangle(location: LngLatAlt,
                                                left: LngLatAlt,
                                                right: LngLatAlt): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val results = Iterables.toList(searchWithinTriangle(arrayListOf(location, left, right)))

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

    fun getNearestFeatureWithinTriangle(location: LngLatAlt,
                                        left: LngLatAlt,
                                        right: LngLatAlt): Feature? {

        if (tree == null)
            return null

        val results =
            nearestWithinTriangle(arrayListOf(location, left, right), 1) ?: return null

        return results.first().value()
    }
}
