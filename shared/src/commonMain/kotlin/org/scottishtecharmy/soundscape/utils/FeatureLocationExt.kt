package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.platform.getDefaultCountryCode
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType

private fun setIfLower(newType: LocationType, oldType: LocationType): LocationType {
    return if (newType < oldType) newType else oldType
}

fun Feature.toLocationDescription(
    source: LocationSource,
    alternateLocation: LngLatAlt = LngLatAlt(),
    featureName: TextForFeature? = null
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
    ld.process()
    return ld
}

fun LocationDescription.process() {
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
                nameLocal = properties["name"] as String?
                if (mvt != null) {
                    if (mvt.housenumber != null) {
                        jsonFields["house_number"] = mvt.housenumber!!
                        address = true
                    }
                    if (mvt.street != null) {
                        jsonFields["road"] = mvt.street!!
                        address = true
                    }
                }
            }
            if (address) {
                val formatter = AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
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
                    val retryFields = jsonFields.toMutableMap()
                    retryFields.remove("country_code")
                    val retryJson = buildJsonObject {
                        for ((k, v) in retryFields) put(k, v)
                    }.toString().replace("\\/", "/")
                    formatter.format(retryJson, "GB")
                }

                if (nameLocal != null) {
                    locationTypeProperty = setIfLower(LocationType.StreetNumber, locationTypeProperty)
                }
                if (mvt != null) {
                    nameLocal = mvt.name
                }

                name = nameLocal ?: formattedAddress.substringBefore('\n')
                description = formattedAddress.replace("\n", ", ").substringBeforeLast(",")
                opposite = oppositeProperty
                locationType = locationTypeProperty
            } else {
                name = mvt?.name?.takeIf { it.isNotEmpty() } ?: featureName?.text ?: ""
                opposite = oppositeProperty
                locationType = locationTypeProperty
            }
            typeDescription = featureName
        }
        this.feature = null
    }
}
