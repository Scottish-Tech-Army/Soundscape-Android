package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

fun featureHasEntrances(feature: Feature): Boolean {
    return (feature.properties?.get("has_entrances") == "yes")
}

fun getDistanceToFeature(
    currentLocation: LngLatAlt,
    feature: Feature,
    ruler: Ruler
): PointAndDistanceAndHeading {
    when (feature.geometry.type) {
        "Point" -> {
            val point = feature.geometry as Point
            val distanceToFeaturePoint = ruler.distance(currentLocation, point.coordinates)
            val heading = ruler.bearing(currentLocation, point.coordinates)
            return PointAndDistanceAndHeading(
                point.coordinates,
                distanceToFeaturePoint,
                heading)
        }

        "MultiPoint" -> {
            val multiPoint = feature.geometry as MultiPoint
            var shortestDistance = Double.MAX_VALUE
            var nearestPoint = LngLatAlt()

            for (point in multiPoint.coordinates) {
                val distanceToPoint = ruler.distance(currentLocation, point)
                if (distanceToPoint < shortestDistance) {
                    shortestDistance = distanceToPoint
                    nearestPoint = point
                }
            }
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance, heading)
        }

        "LineString" -> {
            val lineString = feature.geometry as LineString
            return ruler.distanceToLineString(currentLocation, lineString)
        }

        "MultiLineString" -> {
            val multiLineString = feature.geometry as MultiLineString
            var nearest = PointAndDistanceAndHeading()

            for (arrCoordinates in multiLineString.coordinates) {
                val segmentNearest = ruler.distanceToLineString(
                    currentLocation,
                    LineString(arrCoordinates)
                )
                if (segmentNearest.distance < nearest.distance) {
                    nearest = segmentNearest
                }
            }
            return nearest
        }

        "Polygon" -> {
            val polygon = feature.geometry as Polygon
            val nearestPoint = LngLatAlt()
            val distance = distanceToPolygon(
                currentLocation,
                polygon,
                ruler,
                nearestPoint
            )
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, distance, heading)
        }

        "MultiPolygon" -> {
            val multiPolygon = feature.geometry as MultiPolygon
            var shortestDistance = Double.MAX_VALUE
            var nearestPoint = LngLatAlt()

            for (arrCoordinates in multiPolygon.coordinates) {
                val pointOnPolygon = LngLatAlt()
                val distance = distanceToPolygon(
                    currentLocation,
                    Polygon(arrCoordinates[0]),
                    ruler,
                    pointOnPolygon
                )
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    nearestPoint = pointOnPolygon
                }
            }
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance, heading)
        }

        else -> {
            println("Unknown type ${feature.geometry.type}")
            require(false)
            return PointAndDistanceAndHeading()
        }
    }
}

fun getDistanceToFeatureCollection(
    currentLocation: LngLatAlt,
    featureCollection: FeatureCollection
): FeatureCollection {
    for (feature in featureCollection) {
        feature.properties?.put(
            "distance_to",
            getDistanceToFeature(currentLocation, feature, currentLocation.createCheapRuler()).distance
        )
    }
    return featureCollection
}

fun sortedByDistanceTo(
    currentLocation: LngLatAlt,
    featureCollection: FeatureCollection
): FeatureCollection {

    val featuresWithDistance = getDistanceToFeatureCollection(
        currentLocation,
        featureCollection
    )
    val featuresSortedByDistanceList = featuresWithDistance.features
        .sortedBy {(it.properties?.get("distance_to") as? Number)?.toDouble() ?: Double.MAX_VALUE
        }
    val featuresSortedByDistance = FeatureCollection()
    for (feature in featuresSortedByDistanceList) {
        featuresSortedByDistance.addFeature(feature)
    }
    return featuresSortedByDistance
}
