package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.CountryBoundaries
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import org.scottishtecharmy.soundscape.geoengine.utils.address.JapaneseAddress
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.platform.getDefaultCountryCode
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType

private fun setIfLower(newType: LocationType, oldType: LocationType): LocationType {
    return if (newType < oldType) newType else oldType
}

fun Feature.toLocationDescription(
    source: LocationSource,
    alternateLocation: LngLatAlt = LngLatAlt(),
    featureName: TextForFeature? = null,
    strings: LocalizedStrings? = null
): LocationDescription {
    val location = when (geometry.type) {
        "Point" -> (geometry as Point).coordinates
        else -> alternateLocation
    }

    val ld = LocationDescription(
        source = source,
        location = location,
        feature = this,
        alternateLocation = alternateLocation,
        featureName = featureName
    )
    ld.process(strings)
    return ld
}

/**
 * Builds the "street, settlement" line shown for a POI which has no address of its own - e.g.
 * "London Road, Bridgeton" under a post box - from the nearest way and the settlement associated
 * with it at tile load time by GridState.attachNearestWays.
 *
 * Both halves are required: a way with no settlement, or a settlement with no way, isn't worth
 * showing on its own and returns null. The way is identified by its name, or failing that its ref
 * ("A81"), which names a road just as well.
 */
private fun streetForFeature(mvt: MvtFeature?, strings: LocalizedStrings?): String? {
    val way = mvt?.nearestWay?.let { it.displayName ?: it.ref }
    val settlement = mvt?.nearestSettlement

    return when {
        (way != null) && (settlement != null) ->
            strings?.get(StringKey.DirectionsStreetSettlement, way, settlement)
                ?: "$way, $settlement"
        else -> null
    }
}

/**
 * The country whose address conventions an address at [location] is written in. That's the
 * country the map puts it in - a street in Buenos Aires reads "Avenida Corrientes 1155" whichever
 * country the phone is set to - falling back to the phone's country when the location isn't in a
 * country with a code, e.g. the 0,0 of a feature with no point geometry.
 */
internal fun addressCountryCode(location: LngLatAlt): String =
    CountryBoundaries.countryCode(location)?.takeIf { code -> code.length == 2 && code.all { it.isLetter() } }
        ?: getDefaultCountryCode()

/**
 * The address within its ward of a feature in Japan numbered within its block - 梅田三丁目1-1 - or
 * null if it isn't one. The parts of it are the [mvt]'s own for a feature from the grid, and in
 * [properties] for a search result.
 */
private fun japaneseAddress(mvt: MvtFeature?, properties: Map<String, Any?>, location: LngLatAlt): String? {
    fun part(field: String?, key: String) = field ?: (properties[key] as? String)
    val quarter = part(mvt?.quarter, "quarter")
    val neighbourhood = part(mvt?.neighbourhood, "neighbourhood")
    if (((quarter == null) && (neighbourhood == null)) || (addressCountryCode(location) != "JP")) return null
    return JapaneseAddress.address(
        quarter,
        neighbourhood,
        part(mvt?.blockNumber, "block_number"),
        part(mvt?.housenumber, "housenumber")
    )
}

/**
 * The line of [formattedAddress] which names the street. That's usually the first line ("21
 * Kersland Drive"), but not in every country - Iran's addresses start with the city, and put the
 * house number on the line after the road - so look for the road's line, and add the house number
 * to it when the number is on a line of its own next to it.
 */
private fun streetAddressLine(formattedAddress: String, road: String?, houseNumber: String?): String {
    val lines = formattedAddress.lines().filter { it.isNotBlank() }
    val roadIndex = if (road == null) -1 else lines.indexOfFirst { it.contains(road) }
    if (roadIndex == -1) return lines.firstOrNull() ?: ""

    val roadLine = lines[roadIndex]
    if ((houseNumber == null) || roadLine.contains(houseNumber)) return roadLine
    return when (lines.indexOfFirst { it.trim() == houseNumber }) {
        roadIndex + 1 -> "$roadLine $houseNumber"
        roadIndex - 1 -> "$houseNumber $roadLine"
        else -> roadLine
    }
}

fun LocationDescription.process(strings: LocalizedStrings? = null) {
    if (feature != null) {
        feature?.let { feature ->
            var address = false
            val jsonFields = mutableMapOf<String, String>()
            var oppositeProperty = false
            var locationTypeProperty: LocationType = LocationType.Country
            val mvt = (feature as? MvtFeature)
            var nameLocal: String? = null
            var blockAddress: String? = null

            feature.properties?.let { properties ->
                properties.forEach { (key, value) ->
                    when (key) {
                        "countrycode" -> jsonFields["country_code"] = value.toString()
                        "housenumber" -> {
                            jsonFields["house_number"] = value.toString()
                            locationTypeProperty =
                                setIfLower(LocationType.StreetNumber, locationTypeProperty)
                        }

                        "street" -> {
                            jsonFields["road"] = value.toString()
                            locationTypeProperty =
                                setIfLower(LocationType.Street, locationTypeProperty)
                            address = true
                        }

                        "district" -> {
                            jsonFields["neighbourhood"] = value.toString()
                            locationTypeProperty =
                                setIfLower(LocationType.City, locationTypeProperty)
                            address = true
                        }

                        "city" -> {
                            jsonFields[key] = value.toString()
                            locationTypeProperty =
                                setIfLower(LocationType.City, locationTypeProperty)
                            address = true
                        }

                        "county" -> jsonFields[key] = value.toString()
                        "opposite" -> oppositeProperty = (value as Boolean)
                        "postcode", "country", "state" -> {}
                    }
                }
                nameLocal = properties["name"] as? String
                mvt?.housenumber?.let {
                    jsonFields["house_number"] = it
                    address = true
                }
                mvt?.street?.let {
                    jsonFields["road"] = it
                    address = true
                }
                // Most Japanese buildings are numbered within their block, and their address is
                // the ward and then that - "北区, 梅田三丁目1-1" - with no street in it
                blockAddress = japaneseAddress(mvt, properties, location)
                blockAddress?.let {
                    jsonFields.remove("road")
                    jsonFields["house_number"] = it
                    (mvt?.suburb ?: properties["suburb"] as? String)?.let { ward -> jsonFields["suburb"] = ward }
                    address = true
                }
                // OSM addresses on POIs very often stop at addr:street, so an address built from
                // the tags alone reads as a bare "Kersland Drive" with no town. Fill the gap with
                // the settlement associated at tile load time so the formatter can produce
                // "Kersland Drive, Milngavie".
                if (!jsonFields.containsKey("city") && !jsonFields.containsKey("suburb")) {
                    mvt?.nearestSettlement?.let { jsonFields["city"] = it }
                }
            }
            if (address) {
                val formatter = AddressFormatter(
                    abbreviate = false,
                    appendCountry = false,
                    appendUnknown = false
                )
                val jsonObject = buildJsonObject {
                    for ((k, v) in jsonFields) put(k, v)
                }
                var json = jsonObject.toString()
                json = json.replace("\\/", "/")

                var fallbackCountryCode: String? = null
                if (!jsonFields.containsKey("country_code"))
                    fallbackCountryCode = addressCountryCode(location)
                if (fallbackCountryCode?.isEmpty() == true) fallbackCountryCode = "GB"

                val formattedAddress = try {
                    formatter.format(json, fallbackCountryCode)
                } catch (e: Throwable) {
                    try {
                        val retryFields = jsonFields.toMutableMap()
                        retryFields.remove("country_code")
                        val retryJson = buildJsonObject {
                            for ((k, v) in retryFields) put(k, v)
                        }.toString().replace("\\/", "/")
                        formatter.format(retryJson, "GB")
                    } catch (e2: Throwable) {
                        jsonFields.filterKeys { it != "country_code" }.values.joinToString(", ")
                    }
                }

                if (nameLocal != null) {
                    locationTypeProperty =
                        setIfLower(LocationType.StreetNumber, locationTypeProperty)
                }
                if (mvt != null) {
                    nameLocal = mvt.displayName
                }

                name = nameLocal
                    ?: streetAddressLine(formattedAddress, jsonFields["road"] ?: blockAddress, jsonFields["house_number"])
                description = formattedAddress.replace("\n", ", ").substringBeforeLast(",")
                opposite = oppositeProperty
                locationType = locationTypeProperty
            } else {
                // Bus stops: prefer the NaPTAN-enriched name (e.g. "Main Street, Milngavie
                // Northeastbound") over the plain OSM name - featureName is built with
                // includeTransitTypeSuffix = false here, so no "Bus Stop" suffix is added.
                name = if (mvt?.featureValue == "bus_stop") {
                    featureName?.text?.takeIf { it.isNotEmpty() } ?: mvt.displayName?.takeIf { it.isNotEmpty() } ?: ""
                } else {
                    mvt?.displayName?.takeIf { it.isNotEmpty() } ?: featureName?.text ?: ""
                }
                opposite = oppositeProperty
                locationType = locationTypeProperty
                // No address of its own, so fall back to the way and settlement the POI was
                // associated with at tile load time.
                street = streetForFeature(mvt, strings)
            }
            typeDescription = featureName
        }
        this.feature = null
    }
}
