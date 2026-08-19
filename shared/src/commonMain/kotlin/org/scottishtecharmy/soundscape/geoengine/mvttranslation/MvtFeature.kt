package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey

private data class NaptanBoundDirection(val key: StringKey, val fallback: String)

// NaPTAN's `naptanBearing` tag is a compass letter (N/NE/E/SE/S/SW/W/NW) giving the direction of
// travel the stop serves - far more useful for a rider than the raw indicator above (which just
// describes the stop's position relative to some landmark, not which way the bus is heading).
private val naptanBearingDirections = mapOf(
    "N" to NaptanBoundDirection(StringKey.DirectionsCardinalNorthBound, "Northbound"),
    "NE" to NaptanBoundDirection(StringKey.DirectionsCardinalNorthEastBound, "Northeastbound"),
    "E" to NaptanBoundDirection(StringKey.DirectionsCardinalEastBound, "Eastbound"),
    "SE" to NaptanBoundDirection(StringKey.DirectionsCardinalSouthEastBound, "Southeastbound"),
    "S" to NaptanBoundDirection(StringKey.DirectionsCardinalSouthBound, "Southbound"),
    "SW" to NaptanBoundDirection(StringKey.DirectionsCardinalSouthWestBound, "Southwestbound"),
    "W" to NaptanBoundDirection(StringKey.DirectionsCardinalWestBound, "Westbound"),
    "NW" to NaptanBoundDirection(StringKey.DirectionsCardinalNorthWestBound, "Northwestbound"),
)

private data class NaptanBusStopName(val displayName: String, val landmark: String?)

/**
 * Reformats a NaPTAN-imported bus stop's callout as "CommonName, Locality" - or "CommonName,
 * Locality Directionbound" when the stop's `naptanBearing` tag is present - e.g. "Main Street,
 * Milngavie Northeastbound". `naptanCommonName` is the identifying name people/timetables
 * actually use for a stop, which is often a nearby side-street or junction rather than the
 * literal road it sits on (`naptanStreet` - e.g. this stop's real street is "Woodburn Way", but
 * everyone knows the stop as "Main Street"), so CommonName rather than Street is what's spoken.
 * Only applies to features actually carrying NaPTAN tags (`naptanAtcoCode`) with a
 * `naptanCommonName` - otherwise the original name is returned unchanged, deliberately not
 * parsed for a fallback. `naptanLocalityName` isn't always available (e.g. missing for central
 * Glasgow stops in practice); when it's absent the locality is simply left out rather than
 * falling back to anything parsed from `name` - e.g. "Lynn Drive Westbound" instead of
 * "Lynn Drive, Milngavie Westbound".
 *
 * Also surfaces NaPTAN's optional `naptanLandmark` tag (a notable nearby feature, e.g. a shop,
 * the stop is positioned near) for the caller to append - e.g. "...Bus Stop for Marks & Spencer"
 * - skipped when it's essentially just repeating the CommonName/locality already spoken, which is
 * the common case (`naptanLandmark` is very often just a copy of `naptanCommonName`).
 */
private fun formatNaptanBusStopName(feature: MvtFeature, name: String, localized: LocalizedStrings?): NaptanBusStopName {
    val properties = feature.properties
    if (properties?.containsKey("naptanAtcoCode") != true) return NaptanBusStopName(name, null)
    val commonName = (properties["naptanCommonName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: return NaptanBusStopName(name, null)
    val locality = (properties["naptanLocalityName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    val bound = (properties["naptanBearing"] as? String)?.let { naptanBearingDirections[it] }
    val direction = bound?.let { localized?.get(it.key) ?: it.fallback }
    val displayName = if (locality != null) {
        if (direction != null) {
            localized?.get(StringKey.DirectionsTransitStopStreetLocalityBound, commonName, locality, direction)
                ?: "$commonName, $locality $direction"
        } else {
            localized?.get(StringKey.DirectionsTransitStopStreetLocality, commonName, locality)
                ?: "$commonName, $locality"
        }
    } else if (direction != null) {
        localized?.get(StringKey.DirectionsTransitStopCommonNameBound, commonName, direction)
            ?: "$commonName $direction"
    } else {
        commonName
    }

    val landmark = (properties["naptanLandmark"] as? String)?.trim()?.takeIf {
        it.isNotEmpty() && !it.equals(commonName, ignoreCase = true) && !it.equals(locality, ignoreCase = true)
    }
    return NaptanBusStopName(displayName, landmark)
}

open class MvtFeature : Feature() {
    var osmId: Long = 0L
    var name: String? = null
    var ref: String? = null
    var housenumber: String? = null
    var street: String? = null
    var side: Boolean? = null
    var streetConfidence: Boolean = false
    var featureClass: String? = null
    var featureSubClass: String? = null
    var featureType: String? = null
    var featureValue: String? = null
    var superCategory: SuperCategoryId = SuperCategoryId.UNCATEGORIZED

    fun setProperty(key: String, value: Any) {
        (properties ?: HashMap()).also {
            it[key] = value
            properties = it
        }
    }

    fun copyProperties(other: MvtFeature) {
        osmId = other.osmId
        name = other.name
        ref = other.ref
        housenumber = other.housenumber
        street = other.street
        side = other.side
        streetConfidence = other.streetConfidence
        featureClass = other.featureClass
        featureSubClass = other.featureSubClass
        featureType = other.featureType
        featureValue = other.featureValue
        superCategory = other.superCategory
    }

    /**
     * getText returns text describing the feature for callouts. Usually it returns a name
     * or if it doesn't have one then a localized description of the type of feature it is e.g. bike
     * parking, or style. Some types of Feature have more info e.g. bus stops and railway stations
     * name from the OSM tag rather than an actual name.
     */
    fun getText(localized: LocalizedStrings?): TextForFeature {
        var generic = false
        val name = name
        val entranceType = properties?.get("entrance") as String?
        val featureValue = featureValue
        val isMarker = superCategory == SuperCategoryId.MARKER

        if (superCategory == SuperCategoryId.HOUSENUMBER) {
            return TextForFeature(name ?: housenumber ?: "", false)
        }

        if (isMarker) {
            val description = properties?.get("description")
            var text = name
            if (description != null) {
                if (text != null)
                    text += ", $description"
                else
                    text = description as String
            }
            return if (text != null)
                TextForFeature(
                    localized?.get(StringKey.MarkersMarkerWithName, text) ?: "Marker. $text",
                    false
                )
            else
                TextForFeature(localized?.get(StringKey.MarkersGenericName) ?: "Marker", false)
        }

        var text = name

        // The default OSM descriptor is based on the feature class/subclass, but can be overridden
        // by more complex OSM tagging structures like transit stops.
        var osmFeatureKey: StringKey? = null

        val namedTransit = when (featureValue) {
            "bus_stop" -> Pair(StringKey.OsmBusStopNamed, StringKey.OsmBusStop)
            "station" -> Pair(StringKey.OsmTrainStationNamed, StringKey.OsmTrainStation)
            "tram_stop" -> Pair(StringKey.OsmTramStopNamed, StringKey.OsmTramStop)
            "subway" -> Pair(StringKey.OsmSubwayNamed, StringKey.OsmSubway)
            "ferry_terminal" -> Pair(StringKey.OsmFerryTerminalNamed, StringKey.OsmFerryTerminal)
            else -> null
        }
        if (namedTransit != null) {
            osmFeatureKey = namedTransit.second
            // English fallback text (used when localized == null, e.g. GPX-replay test tooling) needs
            // to match the specific transit type, not a bare "Transit" - a station/subway/ferry
            // terminal isn't a "stop", and a rider needs to know which kind of stop to look for.
            val genericFallback = when (featureValue) {
                "bus_stop" -> "Bus Stop"
                "station" -> "Train Station"
                "tram_stop" -> "Tram Stop"
                "subway" -> "Subway Station"
                "ferry_terminal" -> "Ferry Terminal"
                else -> "Transit"
            }
            val naptanName = if ((featureValue == "bus_stop") && (name != null)) {
                formatNaptanBusStopName(this, name, localized)
            } else {
                null
            }
            val displayName = naptanName?.displayName ?: name
            text = if (displayName != null)
                localized?.get(namedTransit.first, displayName) ?: "$displayName $genericFallback"
            else
                localized?.get(namedTransit.second) ?: genericFallback

            naptanName?.landmark?.let { landmark ->
                text = localized?.get(StringKey.DirectionsTransitStopWithLandmark, text, landmark)
                    ?: "$text for $landmark"
            }
        }

        if (entranceType != null) {
            val entranceName = properties?.get("entrance_name") as String?
            val destinationName = text

            val entranceText =
                if (entranceType == "main")
                    localized?.get(StringKey.OsmMainEntrance) ?: "Main entrance"
                else
                    localized?.get(StringKey.OsmEntrance) ?: "Entrance"

            text = if (entranceName != null) {
                localized?.get(
                    StringKey.OsmEntranceNamedWithDestination,
                    destinationName,
                    entranceText,
                    entranceName,
                ) ?: "$destinationName $entranceText to $entranceName"
            } else {
                localized?.get(StringKey.OsmEntranceWithDestination, destinationName, entranceText)
                    ?: "$destinationName $entranceText"
            }
        }

        if ((featureClass == null) && (featureSubClass == null)) {
            return if (text == null)
                TextForFeature("", true)
            else
                TextForFeature(text, false)
        }

        val osmText = if (localized != null) {
            osmFeatureKey?.let { localized.get(it) }
                ?: featureClass?.let { localized.resolveFeatureClass(it) }
                ?: featureSubClass?.let { localized.resolveFeatureClass(it) }
        } else {
            "OSM Feature $featureClass $featureSubClass"
        }
        var additionalText: String? = null
        if (text == null) {
            text = osmText
            generic = true
        } else {
            additionalText = osmText
        }
        val capitalizedText = text?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        if (capitalizedText == null)
            return TextForFeature("", generic, additionalText)

        return TextForFeature(capitalizedText, generic, additionalText)
    }
}
