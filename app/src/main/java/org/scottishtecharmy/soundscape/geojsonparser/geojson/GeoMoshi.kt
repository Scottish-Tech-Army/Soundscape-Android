package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.Moshi
import org.scottishtecharmy.soundscape.geojsonparser.moshi.*

/**
 * Entrypoint for generating Moshi parser with required overrides
 */
object GeoMoshi {
    /**
     * Add the required serialization adapters to the Moshi builder
     */
    @JvmStatic
    fun registerAdapters(builder: Moshi.Builder): Moshi.Builder {
        builder.add(Point::class.java, PointJsonAdapter())
        builder.add(MultiPoint::class.java, MultiPointJsonAdapter())
        builder.add(LineString::class.java, LineStringJsonAdapter())
        builder.add(MultiLineString::class.java, MultiLineStringJsonAdapter())
        builder.add(Polygon::class.java, PolygonJsonAdapter())
        builder.add(MultiPolygon::class.java, MultiPolygonJsonAdapter())
        builder.add(Feature::class.java, FeatureJsonAdapter())
        builder.add(FeatureCollection::class.java, FeatureCollectionJsonAdapter())
        builder.add(GeometryCollection::class.java, GeometryCollectionJsonAdapter())
        builder.add(GeoJsonObject::class.java, GeoJsonObjectMoshiAdapter())
        builder.add(LngLatAlt::class.java, LngLatAltMoshiAdapter())
        return builder
    }
}