package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.DESTINATION_KIND
import org.scottishtecharmy.soundscape.geoengine.utils.DESTINATION_KIND_ROAD
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.featureIsInFilterGroup
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import kotlin.math.abs

/**
 * How important a road is to somebody walking along it, from its highway class (Way.featureValue -
 * see translateProperties). OpenMapTiles folds residential, unclassified and living_street into
 * "minor", and a pedestrianised street such as Buchanan Street arrives as a "path" whose subclass
 * is "pedestrian" - which is a street to walk along, not a footpath, so it counts as MINOR.
 * Everything else - service roads, driveways, tracks, footways, steps - is OTHER.
 *
 * Declared in increasing order of importance so that tiers compare with < and >.
 */
enum class RoadTier {
    OTHER,
    MINOR,
    MAJOR;

    companion object {
        private val major = setOf("motorway", "trunk", "primary", "secondary", "tertiary")
        private val minor = setOf(
            "minor", "residential", "unclassified", "living_street", "pedestrian", "busway"
        )

        fun of(way: Way): RoadTier {
            if (way.featureType != "highway") return OTHER
            return when (way.featureValue) {
                in major -> MAJOR
                in minor -> MINOR
                else -> OTHER
            }
        }
    }
}

// What a path can lead to that makes it as worth announcing as a street - see pathTier.
private val streetLikeDestinations = setOf(
    DESTINATION_KIND_ROAD,
    SuperCategoryId.LANDMARK.name,
    SuperCategoryId.MARKER.name,
)

/**
 * The tier of [member] as a way out of [intersection]. A road's tier is its class, but a path,
 * service road or track is promoted to MINOR - as good as a street - when it is somewhere a
 * pedestrian navigates by:
 *
 * - it has a real name of its own, e.g. the West Highland Way through Milngavie. The names given
 *   to pavements ("Pavement next to...") are confected and don't count.
 * - going away from the junction it leads to a named street ("Path to Main Street via steps"), a
 *   landmark ("Path to Lennox Park") or one of the user's markers. A path to a shop, a car park or
 *   a dead end stays OTHER.
 *
 * The destinations are confected lazily, so with a [gridState] this confects them first, the same
 * as describing the way would.
 */
private fun pathTier(
    member: Way,
    intersection: Intersection,
    gridState: GridState?,
    strings: LocalizedStrings?
): RoadTier {
    val tier = RoadTier.of(member)
    if ((tier != RoadTier.OTHER) || (member.featureType != "highway")) return tier

    if ((member.name != null) && (member.properties?.get("pavement") == null))
        return RoadTier.MINOR

    if (member.name == null && gridState != null)
        confectNamesForRoad(member, gridState, strings)
    val away = if (member.intersections[WayEnd.START.id] === intersection) "forward" else "backward"
    val kind = member.properties?.get("$DESTINATION_KIND:$away")
    return if (kind in streetLikeDestinations) RoadTier.MINOR else RoadTier.OTHER
}

/**
 * True if [intersection] joins a way of at least [minimumTier] to the one the user is on - see
 * [pathTier] for when a path counts as a street. The road the user is on doesn't count towards
 * it, nor do pavements and crossings: the point is whether the user is being offered somewhere
 * else to go that's worth hearing about.
 *
 * A road the user is on that has been split into several Ways at the junction is still the same
 * road, so members sharing its name don't count either. An unnamed path can't be recognised by
 * name, and the Way the user is matched to often isn't the one that reaches the junction, so
 * instead any member heading back within 45 degrees of [comingFromBearing] - the bearing from the
 * junction to the user - is taken to be the way they arrived by. Without that, a path always
 * "leads to a street": the one the user has just walked from.
 */
fun intersectionMeetsRoadTier(
    intersection: Intersection,
    nearestRoad: Way?,
    minimumTier: RoadTier,
    gridState: GridState? = null,
    strings: LocalizedStrings? = null,
    comingFromBearing: Double? = null,
): Boolean {
    if (minimumTier == RoadTier.OTHER) return true
    val currentName = nearestRoad?.name
    return intersection.members.any { member ->
        (member !== nearestRoad) &&
                !member.isSidewalkOrCrossing() &&
                ((currentName == null) || (member.name != currentName)) &&
                ((currentName != null) || (comingFromBearing == null) ||
                        (angleBetween(member.heading(intersection), comingFromBearing) > 45.0)) &&
                (pathTier(member, intersection, gridState, strings) >= minimumTier)
    }
}

private fun angleBetween(a: Double, b: Double): Double = abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)

/**
 * The "how much should Soundscape say" setting. Each level is a bundle of thresholds over the
 * walking callouts - which junctions are worth a callout, which POIs, how long something already
 * mentioned stays quiet, and how closely POI callouts may follow each other. Travel (vehicle)
 * callouts, beacons, routes, markers and manual callouts are unaffected.
 *
 * DETAILED is the behaviour from before the setting existed, and is what a null
 * PreferencesProvider gets, so that the tests which build AutoCallout directly are unchanged.
 *
 * SILENT makes no automatic callouts at all - it replaced the Allow Callouts switch, see
 * [migrate]. Its thresholds are never consulted.
 */
enum class CalloutVerbosity(
    val preferenceValue: String,
    /** The least important road that makes a junction worth announcing - see [RoadTier]. */
    val minimumIntersectionTier: RoadTier,
    /**
     * The POI super-categories announced automatically, or null for every category the Places and
     * Landmarks / Mobility switches have selected. Markers are always announced regardless.
     */
    val poiCategories: Set<SuperCategoryId>?,
    /** How long an announced POI stays quiet, and how far the user must go to re-arm it. */
    val poiHistoryExpiryMs: Long,
    val poiHistoryTrimRadiusMetres: Double,
    /**
     * How long an announced junction or "Ahead <road>" stays quiet. The road description isn't a
     * point so it is only ever forgotten by time; at 30s it is repeated twice a minute along a
     * long street with no junction of interest.
     */
    val roadHistoryExpiryMs: Long,
    /** The shortest gap allowed between two POI callouts (markers excepted). */
    val minimumPoiGapMs: Long,
) {
    SILENT(
        preferenceValue = "Silent",
        minimumIntersectionTier = RoadTier.MINOR,
        poiCategories = emptySet(),
        poiHistoryExpiryMs = 20 * 60_000L,
        poiHistoryTrimRadiusMetres = 200.0,
        roadHistoryExpiryMs = 5 * 60_000L,
        minimumPoiGapMs = 30_000L,
    ),
    // Quiet keeps every street junction, the same as Balanced: in a city-centre grid such as
    // Glasgow's nearly every street is "minor" in OSM - Gordon Street, St Vincent Street, West
    // George Street - and requiring a MAJOR road left a walk up Buchanan Street with two junction
    // callouts from Central Station to the bus station. Those junctions are exactly what someone
    // walking needs, so Quiet is quieter about places instead.
    QUIET(
        preferenceValue = "Quiet",
        minimumIntersectionTier = RoadTier.MINOR,
        poiCategories = setOf(SuperCategoryId.LANDMARK),
        poiHistoryExpiryMs = 20 * 60_000L,
        poiHistoryTrimRadiusMetres = 200.0,
        roadHistoryExpiryMs = 5 * 60_000L,
        minimumPoiGapMs = 30_000L,
    ),
    BALANCED(
        preferenceValue = "Balanced",
        minimumIntersectionTier = RoadTier.MINOR,
        poiCategories = null,
        poiHistoryExpiryMs = 5 * 60_000L,
        poiHistoryTrimRadiusMetres = 100.0,
        roadHistoryExpiryMs = 2 * 60_000L,
        minimumPoiGapMs = 15_000L,
    ),
    DETAILED(
        preferenceValue = "Detailed",
        minimumIntersectionTier = RoadTier.OTHER,
        poiCategories = null,
        poiHistoryExpiryMs = 60_000L,
        poiHistoryTrimRadiusMetres = 50.0,
        roadHistoryExpiryMs = 30_000L,
        minimumPoiGapMs = 0L,
    );

    companion object {
        fun fromPreference(value: String?): CalloutVerbosity =
            entries.firstOrNull { it.preferenceValue == value } ?: DETAILED

        /**
         * Carries the Allow Callouts switch over once: switched off, it becomes SILENT. The
         * switch is then set back to its default so that this never runs again - nothing else
         * reads it now, though the iOS LegacyMigrator still writes it on first launch.
         *
         * A level the user has already chosen wins. This only runs from GeoEngine.start, and
         * Settings is reachable with the engine down - sleeping, waiting on permissions, before
         * the service is up on first launch - so somebody who set Balanced there would otherwise
         * find it back at Silent the next time the engine started.
         */
        fun migrate(preferences: PreferencesProvider) {
            if (preferences.getBoolean(PreferenceKeys.LEGACY_ALLOW_CALLOUTS, true)) return
            preferences.putBoolean(PreferenceKeys.LEGACY_ALLOW_CALLOUTS, true)
            if (preferences.getString(PreferenceKeys.CALLOUT_VERBOSITY, "").isNotEmpty()) return
            preferences.putString(PreferenceKeys.CALLOUT_VERBOSITY, SILENT.preferenceValue)
        }
    }
}

fun readCalloutVerbosity(preferences: PreferencesProvider?): CalloutVerbosity =
    CalloutVerbosity.fromPreference(
        preferences?.getString(
            PreferenceKeys.CALLOUT_VERBOSITY,
            PreferenceDefaults.CALLOUT_VERBOSITY
        )
    )

/**
 * Steps the Callout Detail one quieter, and from Silent back to the most detailed - the
 * headphone button, where there is no list to choose from and each press has to do one thing.
 * The order is the order of the enum, which is loudest last, so it steps backwards through it.
 *
 * @return the level now in use
 */
fun cycleCalloutVerbosity(preferences: PreferencesProvider): CalloutVerbosity {
    val current = readCalloutVerbosity(preferences)
    val next = if (current == CalloutVerbosity.SILENT) {
        CalloutVerbosity.entries.last()
    } else {
        CalloutVerbosity.entries[CalloutVerbosity.entries.indexOf(current) - 1]
    }
    preferences.putString(PreferenceKeys.CALLOUT_VERBOSITY, next.preferenceValue)
    return next
}

/**
 * Which places the walking and travel POI callouts announce - the "Places to Call Out" setting.
 * It replaced three switches (Places and Landmarks, Mobility's POIs, Bus and Tram Stops) which
 * overlapped with it; see [migrate] for how their values carry over. Junctions have their own
 * switch (PreferenceKeys.STREETS_AND_JUNCTIONS) and markers are always announced.
 *
 * The narrowing choices are the Places Nearby folders (see featureIsInFilterGroup and
 * placesNearbyFolders), so that "Food and Drink" means the same thing in both places. With one of
 * those chosen, a POI in that group is always eligible, whatever the verbosity - the user has asked
 * for it by name. Other shops and amenities, and bus and tram stops, are dropped. Landmarks,
 * information and the rest of mobility (crossings, steps, lifts) are left to the verbosity
 * setting, since they are about finding the way rather than about what the user is out to find.
 */
enum class PlacesToCallOut(val preferenceValue: String, val filterGroup: String?) {
    /** Everything the Callout Detail allows, with nothing narrowed out. */
    EVERYTHING("Everything", null),
    LANDMARKS("Landmarks", null),
    TRANSIT("Transit", "transit"),
    FOOD_AND_DRINK("FoodAndDrink", "food_and_drink"),
    GROCERIES("Groceries", "groceries"),
    BANKS("Banks", "banks"),
    /** No places at all. Markers are still called out, as they always are. */
    NOTHING("Nothing", null);

    companion object {
        /**
         * The kinds chosen, from the stored list. Unknown names are ignored, and nothing at all
         * means Everything - the default, and what an empty or unreadable setting should do
         * rather than falling silent.
         */
        fun fromPreference(value: String?): Set<PlacesToCallOut> {
            val chosen = value.orEmpty().split(SEPARATOR)
                .mapNotNull { name -> entries.firstOrNull { it.preferenceValue == name.trim() } }
                .toSet()
            return chosen.ifEmpty { setOf(EVERYTHING) }
        }

        fun read(preferences: PreferencesProvider?): Set<PlacesToCallOut> = fromPreference(
            preferences?.getString(
                PreferenceKeys.PLACES_TO_CALL_OUT,
                PreferenceDefaults.PLACES_TO_CALL_OUT
            )
        )

        /**
         * Sets the selector once from the switches it replaced, so that somebody who had turned
         * places off doesn't find them all back on. Places and Landmarks on is Everything - it
         * was the switch for shops. With it off, Mobility on was left announcing bus stops and
         * crossings, which Transit is the nearest to; both off is Nothing. The old Mobility key
         * lives on as the Streets and Junctions switch, which is the part of it that was about
         * junctions. Bus and Tram Stops has no equivalent: a user who had only that off gets
         * Everything, and can choose a narrower setting.
         */
        fun migrate(preferences: PreferencesProvider) {
            if (preferences.getString(PreferenceKeys.PLACES_TO_CALL_OUT, "").isNotEmpty()) return
            val places = when {
                preferences.getBoolean(PreferenceKeys.LEGACY_PLACES_AND_LANDMARKS, true) ->
                    EVERYTHING
                preferences.getBoolean(PreferenceKeys.STREETS_AND_JUNCTIONS, true) -> TRANSIT
                else -> NOTHING
            }
            preferences.putString(PreferenceKeys.PLACES_TO_CALL_OUT, setOf(places).toPreference())
        }

        private const val SEPARATOR = ','
    }
}

/**
 * The chosen kinds as they are stored: their names, in the order they are declared, so that a
 * setting written by hand or carried over from an older version still reads back the same.
 */
fun Set<PlacesToCallOut>.toPreference(): String =
    PlacesToCallOut.entries.filter { it in this }.joinToString(",") { it.preferenceValue }

/**
 * Keeps a choice of kinds meaningful: Everything and No Places each say something about all the
 * others, so choosing one drops them, and choosing another drops it. Choosing nothing at all is
 * No Places - the user unticked the last box, which is not the same as never having chosen.
 */
fun Set<PlacesToCallOut>.normalized(justAdded: PlacesToCallOut? = null): Set<PlacesToCallOut> {
    val exclusive = setOf(PlacesToCallOut.EVERYTHING, PlacesToCallOut.NOTHING)
    if (justAdded in exclusive) return setOf(justAdded!!)
    val rest = this - exclusive
    return rest.ifEmpty { setOf(PlacesToCallOut.NOTHING) }
}

/** Bus and tram stops are the noisiest thing there is, so only these two include them. */
val Set<PlacesToCallOut>.includesBusAndTramStops: Boolean
    get() = PlacesToCallOut.EVERYTHING in this || PlacesToCallOut.TRANSIT in this

// Not the whole transit group: a station or a ferry terminal is a destination in its own right
// and is passed rarely enough to be worth hearing whatever the setting, whereas these two are what
// make an urban street or a bus route noisy.
private val busAndTramStopValues = setOf("bus_stop", "tram_stop")

fun isBusOrTramStop(feature: MvtFeature) = feature.featureValue in busAndTramStopValues

/**
 * Which POIs the walking callouts may announce, from the two settings that decide it.
 *
 * This is applied when the tile grid is built rather than at each callout: the trees then hold
 * exactly what is searchable, which is what a callout does every second, instead of every POI
 * being fetched and most of them thrown away. Changing either setting rebuilds the grid - see
 * GeoEngine's preferences listener - which is rare next to that.
 */
data class CalloutPoiSelection(
    val verbosity: CalloutVerbosity,
    val places: Set<PlacesToCallOut>,
) {
    /**
     * Below Balanced the level decides on its own and the chosen kinds are ignored: Quiet means
     * landmarks, whatever kinds of place are ticked, and Silent means nothing at all. Narrowing
     * to a kind is for when Soundscape is talking, not for when it has been asked to stop.
     */
    fun allows(feature: MvtFeature): Boolean {
        if (feature.superCategory !in announceableCategories) return false
        verbosity.poiCategories?.let { return feature.superCategory in it }

        if (PlacesToCallOut.NOTHING in places) return false
        if (PlacesToCallOut.EVERYTHING in places) return true
        if (places.any { kind ->
                kind.filterGroup?.let { featureIsInFilterGroup(feature, it) } == true
            }
        ) return true

        return when {
            // Landmarks are a kind of their own to tick, so unticked they go like any other,
            // even though they are the most useful thing to navigate by.
            feature.superCategory == SuperCategoryId.LANDMARK ->
                PlacesToCallOut.LANDMARKS in places
            feature.superCategory == SuperCategoryId.PLACE -> false
            isBusOrTramStop(feature) -> false
            // What is left - guideposts, crossings, steps, lifts - is not a kind of place
            // anybody sets out to find, so it isn't on the list to choose from. It is how you
            // get about, so it stays.
            else -> true
        }
    }

    companion object {
        /**
         * The super-categories a walking callout can ever name. The others share the POI tree
         * and have never been announced as you pass them: street furniture and building parts
         * (OBJECT), car parks and construction (SAFETY), and - in the low-zoom grid, which is
         * classified by the same code - the settlements the reverse geocoder reads.
         */
        private val announceableCategories = setOf(
            SuperCategoryId.PLACE,
            SuperCategoryId.LANDMARK,
            SuperCategoryId.INFORMATION,
            SuperCategoryId.MOBILITY,
        )

        fun read(preferences: PreferencesProvider?) = CalloutPoiSelection(
            readCalloutVerbosity(preferences),
            PlacesToCallOut.read(preferences),
        )

        /** Everything a POI callout could ever name - what the tests that predate this used. */
        val EVERYTHING = CalloutPoiSelection(
            CalloutVerbosity.DETAILED,
            setOf(PlacesToCallOut.EVERYTHING),
        )
    }
}
