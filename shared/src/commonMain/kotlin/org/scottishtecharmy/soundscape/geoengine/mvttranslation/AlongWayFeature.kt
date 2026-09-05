package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * The sort of thing an [AlongWayFeature] describes. A Way can carry any mixture of these, which
 * is why they're a list on the Way rather than a single set of properties - a viaduct over both a
 * river and a railway is one Way with two entries.
 */
enum class AlongWayKind {
    WATERWAY_CROSSING,
    RAILWAY_CROSSING,
}

/**
 * The user's own relationship to the thing being crossed - never a raw OSM `brunnel` value. That
 * distinction matters because the brunnel evidence arrives from either side and the two invert
 * each other: a road tagged brunnel=bridge is over the river, whereas a waterway tagged
 * brunnel=bridge is an aqueduct, so the road below it goes under.
 */
enum class AlongWayPosition {
    OVER,
    UNDER,
}

/**
 * A feature positioned at a known distance along a [Way] - today a river/canal or railway
 * crossing, in future a bus stop, station or highway junction. Held in [Way.alongWayFeatures],
 * sorted by [distanceFromStart], so that "what's next along this road?" is a lookup rather than a
 * geographic search.
 *
 * @param distanceFromStart metres from the owning Way's START intersection, measured along that
 * Way's own geometry. See [Way.distanceAlongWay] for the caveat about points which don't actually
 * lie on the Way.
 * @param point where the feature actually is. For a crossing this is the point at which the two
 * lines cross, which is not necessarily on the owning Way - see [Way.distanceAlongWay].
 * @param name the name of the thing being crossed (the river, the railway line), or of the
 * feature itself. Null when unnamed.
 * @param position for a crossing, whether the user passes over or under. Null when not applicable.
 * @param feature the POI itself, for kinds which have one. Null for crossings, which are derived
 * from a geometric intersection rather than from a point feature.
 */
data class AlongWayFeature(
    val distanceFromStart: Double,
    val point: LngLatAlt,
    val kind: AlongWayKind,
    val name: String? = null,
    val position: AlongWayPosition? = null,
    val feature: MvtFeature? = null,
)
