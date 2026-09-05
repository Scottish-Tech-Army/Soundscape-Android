package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.Side
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * The sort of thing an [AlongWayFeature] describes. A Way can carry any mixture of these, which
 * is why they're a list on the Way rather than a single set of properties - a viaduct over both a
 * river and a railway is one Way with two entries.
 */
enum class AlongWayKind {
    /** A named river or canal this Way crosses. */
    WATERWAY_CROSSING,

    /** A railway this road Way crosses. Recorded on the road, read while driving or walking. */
    RAILWAY_CROSSING,

    /**
     * A road that crosses this railway Way. The mirror of [RAILWAY_CROSSING], recorded on the
     * railway at the same time and from the same geometric test, so that a train passenger's
     * callout is a lookup on the line they're riding rather than a search for roads that happen to
     * be nearby. [AlongWayFeature.position] is the *train's* relationship to the road, already
     * inverted from the road's own.
     */
    ROAD_CROSSING,

    /**
     * A bus/tram stop or station beside this road, recorded on the road it serves. Unlike the
     * crossings this isn't an intersection of two lines - the stop is a point near the road - so
     * [AlongWayFeature.side] says which kerb it is on, which is what tells a stop serving this
     * direction of travel from the one across the street serving the other.
     */
    TRANSIT_STOP,
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
 * @param side which side of the Way the feature sits on, relative to travelling from the Way's
 * START intersection towards its END. Null when it is on the Way itself, as a crossing is.
 * @param feature the other Way or POI involved, for kinds which have one. For [ROAD_CROSSING] this
 * is the road Way itself, so that the callout can call Way.getName with the user's localized
 * strings at callout time rather than baking a name in at tile-load time. Null for the crossings
 * that are named after a feature which isn't a Way in its own right.
 */
data class AlongWayFeature(
    val distanceFromStart: Double,
    val point: LngLatAlt,
    val kind: AlongWayKind,
    val name: String? = null,
    val position: AlongWayPosition? = null,
    val side: Side? = null,
    val feature: MvtFeature? = null,
)
