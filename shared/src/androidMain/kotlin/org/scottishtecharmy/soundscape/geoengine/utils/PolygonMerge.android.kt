package org.scottishtecharmy.soundscape.geoengine.utils

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon as JtsPolygon
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

private fun polygonOuterRingToCoordinateArray(
    polygon: Polygon?,
    geometryFactory: GeometryFactory
): LinearRing? {
    return geometryFactory.createLinearRing(
        polygon?.coordinates?.firstOrNull()
            ?.map { position -> Coordinate(position.longitude, position.latitude) }
            ?.toTypedArray()
    )
}

private fun polygonInteriorRingsToCoordinateArray(
    polygon: Polygon?,
    geometryFactory: GeometryFactory
): Array<LinearRing>? {
    if (polygon == null) return null

    val result = mutableListOf<LinearRing>()
    val innerRings = polygon.getInteriorRings()
    for (ring in innerRings) {
        result.add(
            geometryFactory.createLinearRing(
                ring.map { position -> Coordinate(position.longitude, position.latitude) }
                    .toTypedArray()
            )
        )
    }
    return result.toTypedArray()
}

private fun createJtsPolygonFromPolygon(polygon: Polygon?): JtsPolygon? {
    if (polygon == null) return null

    val geometryFactory = GeometryFactory()
    val outerRing = polygonOuterRingToCoordinateArray(polygon, geometryFactory)
    val innerRings = polygonInteriorRingsToCoordinateArray(polygon, geometryFactory)

    return geometryFactory.createPolygon(outerRing, innerRings)
}

actual fun mergePolygons(polygon1: Feature, polygon2: Feature): Feature {

    val polygon1GeometryJTS = createJtsPolygonFromPolygon(polygon1.geometry as? Polygon)
    val polygon2GeometryJTS = createJtsPolygonFromPolygon(polygon2.geometry as? Polygon)

    val mergedGeometryJTSInitial = polygon1GeometryJTS?.union(polygon2GeometryJTS)
    if (mergedGeometryJTSInitial is org.locationtech.jts.geom.MultiPolygon) {
        return polygon2
    }

    val mergedGeometryJTS = mergedGeometryJTSInitial as JtsPolygon
    val mergedPolygon = MvtFeature().also { feature ->
        feature.properties = polygon1.properties
        feature.type = "Feature"
        feature.copyProperties(polygon1 as MvtFeature)
        feature.geometry = Polygon().also { polygon ->
            val outerRing = mergedGeometryJTS.exteriorRing.coordinates?.map { coordinate ->
                LngLatAlt(coordinate.x, coordinate.y)
            }?.let {
                arrayListOf(arrayListOf(*it.toTypedArray()))
            }
            polygon.coordinates = outerRing ?: arrayListOf()

            val ringCount = mergedGeometryJTS.numInteriorRing
            for (ring in 0 until ringCount) {
                val innerRing = mergedGeometryJTS.getInteriorRingN(ring).coordinates?.map { coordinate ->
                    LngLatAlt(coordinate.x, coordinate.y)
                }?.let {
                    arrayListOf(*it.toTypedArray())
                }
                if (innerRing != null) {
                    polygon.addInteriorRing(innerRing)
                }
            }
        }
    }
    return mergedPolygon
}
