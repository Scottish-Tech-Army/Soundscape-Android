package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
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
    val way = mvt?.nearestWay?.let { it.name ?: it.ref }
    val settlement = mvt?.nearestSettlement

    return when {
        (way != null) && (settlement != null) ->
            strings?.get(StringKey.DirectionsStreetSettlement, way, settlement)
                ?: "$way, $settlement"
        else -> null
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
                // OSM addresses on POIs very often stop at addr:street, so an address built from
                // the tags alone reads as a bare "Kersland Drive" with no town. Fill the gap with
                // the settlement associated at tile load time so the formatter can produce
                // "Kersland Drive, Milngavie".
                if (!jsonFields.containsKey("city")) {
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
                    fallbackCountryCode = getDefaultCountryCode()
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
                    nameLocal = mvt.name
                }

                name = nameLocal ?: formattedAddress.substringBefore('\n')
                description = formattedAddress.replace("\n", ", ").substringBeforeLast(",")
                opposite = oppositeProperty
                locationType = locationTypeProperty
            } else {
                // Bus stops: prefer the NaPTAN-enriched name (e.g. "Main Street, Milngavie
                // Northeastbound") over the plain OSM name - featureName is built with
                // includeTransitTypeSuffix = false here, so no "Bus Stop" suffix is added.
                name = if (mvt?.featureValue == "bus_stop") {
                    featureName?.text?.takeIf { it.isNotEmpty() } ?: mvt.name?.takeIf { it.isNotEmpty() } ?: ""
                } else {
                    mvt?.name?.takeIf { it.isNotEmpty() } ?: featureName?.text ?: ""
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
