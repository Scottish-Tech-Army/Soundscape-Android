package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.rtree.Entry
import org.scottishtecharmy.soundscape.rtree.Geometry
import org.scottishtecharmy.soundscape.rtree.Line
import org.scottishtecharmy.soundscape.rtree.Point
import org.scottishtecharmy.soundscape.rtree.RTree
import org.scottishtecharmy.soundscape.rtree.Rectangle
import kotlin.math.PI
import kotlin.math.cos

/**
 * FeatureTree is a class which stores FeatureCollections within an rtree which gives us faster
 * spatial searching. The APIs all return either FeatureCollections or Features.
 */
class FeatureTree(featureCollection: FeatureCollection?) {

    var tree: RTree<Feature, Geometry>? = null

    init {
        if (featureCollection != null) {
            tree = createRtree(featureCollection)
        }
    }

    private fun createRtree(featureCollection: FeatureCollection): RTree<Feature, Geometry> {
        val rtreeList = mutableListOf<Entry<Feature, Geometry>>()
        for (feature in featureCollection) {
            when (feature.geometry.type) {
                "Point" -> {
                    val point =
                        feature.geometry as org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
                    rtreeList.add(
                        Entry(feature, Point(point.coordinates.longitude, point.coordinates.latitude))
                    )
                }

                "MultiPoint" -> {
                    val multiPoint =
                        feature.geometry as org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
                    for (location in multiPoint.coordinates) {
                        rtreeList.add(
                            Entry(feature, Point(location.longitude, location.latitude))
                        )
                    }
                }

                "LineString" -> {
                    // Add each line segment as a separate entry for precise searching;
                    // callers deduplicate features on retrieval.
                    val line = feature.geometry as LineString
                    for ((index, point) in line.coordinates.withIndex()) {
                        if (index < (line.coordinates.size - 1)) {
                            val next = line.coordinates[index + 1]
                            rtreeList.add(
                                Entry(
                                    feature,
                                    Line(point.longitude, point.latitude, next.longitude, next.latitude)
                                )
                            )
                        }
                    }
                }

                "Polygon" -> {
                    val polygon = feature.geometry as Polygon
                    val box = getBoundingBoxOfPolygon(polygon)
                    rtreeList.add(
                        Entry(
                            feature,
                            Rectangle(box.westLongitude, box.southLatitude, box.eastLongitude, box.northLatitude)
                        )
                    )
                }

                "MultiPolygon" -> {
                    val multiPolygon = feature.geometry as MultiPolygon
                    val boxes = getBoundingBoxesOfMultiPolygon(multiPolygon)
                    for (box in boxes) {
                        rtreeList.add(
                            Entry(
                                feature,
                                Rectangle(box.westLongitude, box.southLatitude, box.eastLongitude, box.northLatitude)
                            )
                        )
                    }
                }

                else -> {
                    // unsupported geometry; skip
                }
            }
        }
        return RTree.create(rtreeList)
    }

    private fun createBoundingSquare(center: LngLatAlt, radius: Double): Rectangle {
        val latOffset = (radius) / EARTH_RADIUS_METERS * (180 / PI)
        val lngOffset = (radius) / (EARTH_RADIUS_METERS * cos(toRadians(center.latitude))) * (180 / PI)
        return Rectangle(
            center.longitude - lngOffset,
            center.latitude - latOffset,
            center.longitude + lngOffset,
            center.latitude + latOffset
        )
    }

    private fun distanceToEntry(
        entry: Entry<Feature, Geometry>,
        from: LngLatAlt,
        ruler: Ruler
    ): Double {
        when (val p = entry.geometry) {
            is Point -> {
                return ruler.distance(from, LngLatAlt(p.x, p.y))
            }

            is Line -> {
                return ruler.pointToSegmentDistance(
                    from,
                    LngLatAlt(p.x1, p.y1),
                    LngLatAlt(p.x2, p.y2)
                )
            }

            is Rectangle -> {
                if (entry.value.geometry.type == "Polygon") {
                    return distanceToPolygon(from, entry.value.geometry as Polygon, ruler)
                } else if (entry.value.geometry.type == "MultiPolygon") {
                    return distanceToMultiPolygon(from, entry.value.geometry as MultiPolygon, ruler)
                }
            }
        }
        return Double.POSITIVE_INFINITY
    }

    private fun entryWithinDistance(
        entry: Entry<Feature, Geometry>,
        distance: Double,
        from: LngLatAlt,
        ruler: Ruler
    ): Boolean = distanceToEntry(entry, from, ruler) < distance

    private fun searchWithinDistance(
        from: LngLatAlt,
        distance: Double,
        ruler: Ruler
    ): Sequence<Entry<Feature, Geometry>> {
        check(tree != null)
        val bounds: Rectangle = createBoundingSquare(from, distance)
        return tree!!.search(bounds).filter { entry ->
            entryWithinDistance(entry, distance, from, ruler)
        }
    }

    private fun nearestWithinDistance(
        from: LngLatAlt,
        distance: Double,
        maxCount: Int,
        ruler: Ruler
    ): List<Entry<Feature, Geometry>> {
        check(tree != null)
        val k = if (maxCount < 1) Int.MAX_VALUE else maxCount
        return tree!!.nearest(from.longitude, from.latitude, distance, k).filter { entry ->
            entryWithinDistance(entry, distance, from, ruler)
        }
    }

    private fun createBoundingSquareContainingTriangle(triangle: Triangle): Rectangle {
        val minLatitude = minOf(triangle.origin.latitude, triangle.left.latitude, triangle.right.latitude)
        val maxLatitude = maxOf(triangle.origin.latitude, triangle.left.latitude, triangle.right.latitude)
        val minLongitude = minOf(triangle.origin.longitude, triangle.left.longitude, triangle.right.longitude)
        val maxLongitude = maxOf(triangle.origin.longitude, triangle.left.longitude, triangle.right.longitude)
        return Rectangle(minLongitude, minLatitude, maxLongitude, maxLatitude)
    }

    private fun lineSegmentPassesWithinTriangle(
        p1: LngLatAlt, p2: LngLatAlt,
        triangle: Triangle
    ): Boolean {
        if (straightLinesIntersect(p1, p2, triangle.origin, triangle.left) ||
            straightLinesIntersect(p1, p2, triangle.left, triangle.right) ||
            straightLinesIntersect(p1, p2, triangle.right, triangle.origin)
        ) {
            return true
        }
        val polygon = createPolygonFromTriangle(triangle)
        return (polygonContainsCoordinates(p1, polygon) && polygonContainsCoordinates(p2, polygon))
    }

    private fun testPolygonInFov(polygon: Polygon, triangle: Triangle): Boolean {
        var lastPoint = polygon.coordinates[0][0]
        for (point in polygon.coordinates[0].drop(1)) {
            if (lineSegmentPassesWithinTriangle(lastPoint, point, triangle)) return true
            lastPoint = point
        }
        return (polygonContainsCoordinates(triangle.origin, polygon) ||
            polygonContainsCoordinates(triangle.left, polygon) ||
            polygonContainsCoordinates(triangle.right, polygon))
    }

    private fun testMultiPolygonInFov(polygon: MultiPolygon, triangle: Triangle): Boolean =
        testPolygonInFov(Polygon(polygon.coordinates[0][0]), triangle)

    private fun entryWithinTriangle(
        entry: Entry<Feature, Geometry>,
        triangle: Triangle
    ): Boolean {
        when (val p = entry.geometry) {
            is Point -> {
                val polygon = createPolygonFromTriangle(triangle)
                return polygonContainsCoordinates(LngLatAlt(p.x, p.y), polygon)
            }

            is Line -> {
                return lineSegmentPassesWithinTriangle(
                    LngLatAlt(p.x1, p.y1),
                    LngLatAlt(p.x2, p.y2),
                    triangle
                )
            }

            is Rectangle -> {
                val feature = entry.value
                if (feature.geometry.type == "Polygon") {
                    return testPolygonInFov(feature.geometry as Polygon, triangle)
                } else if (feature.geometry.type == "MultiPolygon") {
                    return testMultiPolygonInFov(feature.geometry as MultiPolygon, triangle)
                }
                return false
            }
        }
    }

    private fun searchWithinTriangle(triangle: Triangle): Sequence<Entry<Feature, Geometry>> {
        check(tree != null)
        val bounds: Rectangle = createBoundingSquareContainingTriangle(triangle)
        return tree!!.search(bounds).filter { entry -> entryWithinTriangle(entry, triangle) }
    }

    private fun nearestWithinTriangle(
        triangle: Triangle,
        maxCount: Int,
        ruler: Ruler
    ): FeatureCollection {
        val results = FeatureCollection()
        if (tree != null) {
            val resultsWithinTriangle = searchWithinTriangle(triangle)

            data class EntryWithDistance(val entry: Entry<Feature, Geometry>, val distance: Double)

            val unsortedList = mutableListOf<EntryWithDistance>()
            for (entry in resultsWithinTriangle) {
                unsortedList.add(EntryWithDistance(entry, distanceToEntry(entry, triangle.origin, ruler)))
            }
            val sortedList = unsortedList.sortedBy { it.distance }

            for ((index, item) in sortedList.withIndex()) {
                if (index >= maxCount) break
                results.addFeature(item.entry.value)
            }
        }
        return results
    }

    fun getAllCollection(): FeatureCollection {
        val featureCollection = FeatureCollection()
        if (tree != null) {
            val deduplicationSet = mutableSetOf<Feature>()
            for (entry in tree!!.entries()) {
                if (deduplicationSet.add(entry.value)) {
                    featureCollection.addFeature(entry.value)
                }
            }
        }
        return featureCollection
    }

    fun getNearbyCollection(location: LngLatAlt, distance: Double, ruler: Ruler): FeatureCollection {
        val featureCollection = FeatureCollection()
        if (tree != null) {
            val deduplicationSet = mutableSetOf<Feature>()
            for (entry in searchWithinDistance(location, distance, ruler)) {
                if (deduplicationSet.add(entry.value)) {
                    featureCollection.addFeature(entry.value)
                }
            }
        }
        return featureCollection
    }

    fun getNearestCollection(
        location: LngLatAlt,
        distance: Double,
        maxCount: Int,
        ruler: Ruler,
        initialCollection: FeatureCollection? = null
    ): FeatureCollection {
        val featureCollection = FeatureCollection()
        if (tree != null) {
            val distanceResults = nearestWithinDistance(location, distance, -1, ruler)

            data class EntryWithDistance(val entry: Entry<Feature, Geometry>, val distance: Double)
            val unsortedList = distanceResults
                .map { entry -> EntryWithDistance(entry, distanceToEntry(entry, location, ruler)) }
                .groupBy { it.entry.value }
                .map { (_, entries) -> entries.minBy { it.distance } }

            val sortedList = unsortedList.sortedBy { it.distance }

            val initialItemIterator = initialCollection?.features?.iterator()
            val newItemIterator = sortedList.iterator()

            var initialItem: Feature? = if (initialItemIterator?.hasNext() == true) initialItemIterator.next() else null
            var newItem: EntryWithDistance? = if (newItemIterator.hasNext()) newItemIterator.next() else null

            while ((initialItem != null) or (newItem != null)) {
                if (featureCollection.features.size > maxCount) break
                if (initialItem != null) {
                    var addInitial = false
                    if (newItem == null) addInitial = true
                    if (!addInitial) addInitial = getDistanceToFeature(location, initialItem, ruler).distance < newItem!!.distance
                    if (addInitial) {
                        featureCollection.addFeature(initialItem)
                        initialItem = if (initialItemIterator?.hasNext() == true) initialItemIterator.next() else null
                        continue
                    }
                }
                featureCollection.addFeature(newItem!!.entry.value)
                newItem = if (newItemIterator.hasNext()) newItemIterator.next() else null
            }
        }
        return featureCollection
    }

    fun getNearestFeature(
        location: LngLatAlt,
        ruler: Ruler,
        distance: Double = Double.POSITIVE_INFINITY
    ): Feature? {
        if (tree != null) {
            val results = nearestWithinDistance(location, distance, 1, ruler)
            for (entry in results) return entry.value
        }
        return null
    }

    fun getNearestCollectionWithinTriangle(
        triangle: Triangle,
        maxCount: Int,
        ruler: Ruler
    ): FeatureCollection {
        if (tree == null) return FeatureCollection()
        return nearestWithinTriangle(triangle, maxCount, ruler)
    }

    fun getAllWithinTriangle(triangle: Triangle): FeatureCollection {
        val featureCollection = FeatureCollection()
        if (tree != null) {
            val deduplicationSet = mutableSetOf<Feature>()
            for (entry in searchWithinTriangle(triangle)) {
                if (deduplicationSet.add(entry.value)) {
                    featureCollection.addFeature(entry.value)
                }
            }
        }
        return featureCollection
    }

    fun getNearestFeatureWithinTriangle(triangle: Triangle, ruler: Ruler): Feature? {
        if (tree == null) return null
        val results = nearestWithinTriangle(triangle, 1, ruler)
        if (results.features.isEmpty()) return null
        return results.features[0]
    }

    fun getContainingPolygons(location: LngLatAlt): FeatureCollection {
        if (tree == null) return FeatureCollection()

        val result = FeatureCollection()
        val pointBox = Rectangle(location.longitude, location.latitude, location.longitude, location.latitude)
        for (entry in tree!!.search(pointBox)) {
            val geom = entry.value.geometry
            val match = when (geom.type) {
                "Polygon" -> polygonContainsCoordinates(location, geom as Polygon)
                "MultiPolygon" -> multiPolygonContainsCoordinates(location, geom as MultiPolygon)
                else -> false
            }
            if (match) result.addFeature(entry.value)
        }
        return result
    }

    private fun entryNearLine(
        entry: Entry<Feature, Geometry>,
        p1: LngLatAlt,
        p2: LngLatAlt,
        distance: Double,
        ruler: Ruler
    ): Boolean {
        when (val p = entry.geometry) {
            is Point -> {
                return ruler.pointToSegmentDistance(LngLatAlt(p.x, p.y), p1, p2) < distance
            }

            is Line,
            is Rectangle -> {
                val feature = entry.value
                when (feature.geometry.type) {
                    "Polygon" -> {
                        val polygon = feature.geometry as Polygon
                        for (geometry in polygon.coordinates) {
                            for (point in geometry) {
                                if (ruler.pointToSegmentDistance(point, p1, p2) < distance) return true
                            }
                        }
                        return false
                    }

                    "MultiPolygon" -> {
                        val multiPolygon = feature.geometry as MultiPolygon
                        for (polygon in multiPolygon.coordinates) {
                            for (point in polygon[0]) {
                                if (ruler.pointToSegmentDistance(point, p1, p2) < distance) return true
                            }
                        }
                        return false
                    }

                    else -> return false
                }
            }
        }
    }

    private fun createBoundingSquareContainingLine(
        p1: LngLatAlt,
        p2: LngLatAlt,
        distance: Double
    ): Rectangle {
        val latOffset = (distance) / EARTH_RADIUS_METERS * (180 / PI)
        val lngOffset = (distance) / (EARTH_RADIUS_METERS * cos(toRadians(p1.latitude))) * (180 / PI)

        val minLat = minOf(p1.latitude, p2.latitude)
        val maxLat = maxOf(p1.latitude, p2.latitude)
        val minLng = minOf(p1.longitude, p2.longitude)
        val maxLng = maxOf(p1.longitude, p2.longitude)

        return Rectangle(
            minLng - lngOffset,
            minLat - latOffset,
            maxLng + lngOffset,
            maxLat + latOffset
        )
    }

    private fun searchNearLine(
        p1: LngLatAlt,
        p2: LngLatAlt,
        distance: Double,
        deduplicationSet: MutableSet<Feature>,
        ruler: Ruler
    ): Sequence<Entry<Feature, Geometry>> {
        check(tree != null)
        val bounds: Rectangle = createBoundingSquareContainingLine(p1, p2, distance)
        return tree!!.search(bounds).filter { entry ->
            if (!deduplicationSet.contains(entry.value))
                entryNearLine(entry, p1, p2, distance, ruler)
            else
                false
        }
    }

    fun getNearbyLine(line: LineString, distance: Double, ruler: Ruler): FeatureCollection {
        val featureCollection = FeatureCollection()
        if (tree != null) {
            val deduplicationSet = mutableSetOf<Feature>()

            var lastPoint = LngLatAlt()
            for ((index, point) in line.coordinates.withIndex()) {
                if (index > 0) {
                    val results = searchNearLine(lastPoint, point, distance, deduplicationSet, ruler).toList()
                    deduplicationSet += results.map { it.value }
                }
                lastPoint = point
            }

            for (feature in deduplicationSet) {
                featureCollection.addFeature(feature)
            }
        }
        return featureCollection
    }
}
