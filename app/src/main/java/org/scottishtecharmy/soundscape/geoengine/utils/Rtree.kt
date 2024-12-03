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

    private fun handleEntry(entry: Entry<Feature, Geometry?>, distance: Double, from: LngLatAlt)  : Boolean {
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

    private fun search(
        lonLat: Point,
        distance: Double
    ): MutableIterable<Entry<Feature, Geometry?>>? {
        // First we need to calculate an enclosing lat long rectangle for this
        // distance then we refine on the exact distance
        val from = LngLatAlt(lonLat.x(), lonLat.y())
        val bounds: Rectangle = createBoundingSquare(from, distance)

        return Iterables.filter(tree!!.search(bounds))
        { entry ->
            handleEntry(entry, distance, from)
        }
    }

    private fun nearest(
        lonLat: Point,
        distance: Double,
        maxCount: Int
    ): MutableIterable<Entry<Feature, Geometry?>>? {
        val from = LngLatAlt(lonLat.x(), lonLat.y())
        return Iterables.filter(tree!!.nearest(lonLat, distance, maxCount))
        { entry ->
            handleEntry(entry, distance, from)
        }
    }

    /**
     * generateFeatureCollection returns a FeatureCollection containing all of the features from
     * within the rtree.
     */
    fun generateFeatureCollection(): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val entries = tree!!.entries()
            for (feature in entries) {
                featureCollection.addFeature(feature.value())
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
            val distanceResults = Iterables.toList(search(
                Geometries.pointGeographic(location.longitude, location.latitude),
                distance))

            for (feature in distanceResults) {
                featureCollection.addFeature(feature.value())
            }
        }
        return featureCollection
    }

    /**
     * generateNearestFeatureCollection returns a FeatureCollection containing the nearest member
     * of the rtree that is also within distance.
     */

    fun generateNearestFeatureCollection(location: LngLatAlt, distance: Double): FeatureCollection {
        val featureCollection = FeatureCollection()
        if(tree != null) {
            val distanceResults = Iterables.toList(nearest(
                Geometries.pointGeographic(location.longitude, location.latitude),
                distance,
                1))

            for (feature in distanceResults) {
                featureCollection.addFeature(feature.value())
            }
        }
        return featureCollection
    }
}
