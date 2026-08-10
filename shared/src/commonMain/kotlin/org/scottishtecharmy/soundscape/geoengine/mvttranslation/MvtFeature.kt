package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey

open class MvtFeature : Feature() {
    var osmId: Long = 0L
    var name: String? = null
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
            text = if (name != null)
                localized?.get(namedTransit.first, name) ?: "$name Transit Stop"
            else
                localized?.get(namedTransit.second) ?: "Transit"
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
            "OSM Feature"
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
