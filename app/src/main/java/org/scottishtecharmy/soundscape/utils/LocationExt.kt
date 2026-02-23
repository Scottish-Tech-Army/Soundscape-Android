package org.scottishtecharmy.soundscape.utils

import android.location.Address
import org.json.JSONObject
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import org.woheller69.AndroidAddressFormatter.AndroidAddressFormatter

private fun setIfLower(newType: LocationType, oldType: LocationType) : LocationType {
    return if(newType < oldType) newType else oldType
}

fun Feature.deferredToLocationDescription(source: LocationSource,
                                          alternateLocation: LngLatAlt = LngLatAlt(),
                                          featureName: TextForFeature? = null): LocationDescription {
    val location =
        when (geometry.type) {
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

    return ld
}

fun Feature.toLocationDescription(source: LocationSource,
                                  alternateLocation: LngLatAlt = LngLatAlt(),
                                  featureName: TextForFeature? = null): LocationDescription {
    val location =
        when (geometry.type) {
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

/**
  *  Although the formatting of the address is not hugely time consuming, it adds up and so
 *  in Places Nearby the processing is deferred to the point at which the location information
 *  is displayed. The display uses a LazyColumn so even if there are hundreds of items in the
 *  list only the ones appearing on screen will need to call process.
**/
fun LocationDescription.process() {
    if (feature != null) {
        feature?.let { feature ->
            var address = false
            val jsonObject = JSONObject()
            var oppositeProperty = false
            var locationTypeProperty: LocationType = LocationType.Country
            val mvt = (feature as? MvtFeature)
            var nameLocal: String? = null

            feature.properties?.let { properties ->
                // We use the AndroidAddressFormatter library to try and generate addresses which are
                // locale correct e.g. street numbers before/after street name
                // It's got a clunky API that takes in JSON which might be a problem if any of our
                // strings aren't JSON friendly. It would be better to have an API which took in the
                properties.forEach { (key, value) ->
                    when (key) {
                        "countrycode" -> jsonObject.put("country_code", value.toString())
                        "housenumber" -> {
                            jsonObject.put("house_number", value.toString())
                            locationTypeProperty =
                                setIfLower(LocationType.StreetNumber, locationTypeProperty)
                        }

                        "street" -> {
                            jsonObject.put("road", value.toString())
                            locationTypeProperty =
                                setIfLower(LocationType.Street, locationTypeProperty)
                            address = true
                        }

                        "district" -> {
                            jsonObject.put("neighbourhood", value.toString())
                            locationTypeProperty =
                                setIfLower(LocationType.City, locationTypeProperty)
                            address = true
                        }

                        "city" -> {
                            jsonObject.put(key, value.toString())
                            locationTypeProperty =
                                setIfLower(LocationType.City, locationTypeProperty)
                            address = true
                        }

                        "county" -> jsonObject.put(key, value.toString())
                        "opposite" -> oppositeProperty = (value as Boolean)
                        "postcode", "country", "state" -> {} // Don't include the include country or state
                    }
                }
                nameLocal = properties["name"] as String?
                if (mvt != null) {
                    if (mvt.housenumber != null) {
                        jsonObject.put("house_number", mvt.housenumber)
                        address = true
                    }
                    if (mvt.street != null) {
                        jsonObject.put("road", mvt.street)
                        address = true
                    }
                }
            }
            if (address) {
                val formatter = AndroidAddressFormatter(false, false, false)
                var json = jsonObject.toString()
                json = json.replace("\\/", "/")
                var fallbackCountryCode : String? = null
                if(!jsonObject.has("country_code"))
                    fallbackCountryCode = getCurrentLocale().country

                if (fallbackCountryCode?.isEmpty() == true) fallbackCountryCode = "GB"
                val formattedAddress = formatter.format(json, fallbackCountryCode)
                if (nameLocal != null) {
                    // Named locations are as good a street number
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

fun Address.toLocationDescription(name: String?): LocationDescription {

    val formatter = AndroidAddressFormatter(false, true, false)
    val jsonObject = JSONObject()
    var locationType : LocationType = LocationType.Country
    //if (countryName != null) jsonObject.put("country", countryName)  // Don't include the include country
    var fallbackCountryCode : String? = null
    if (countryCode != null) {
        jsonObject.put("country_code", countryCode)
    }
    else {
        // No country code, so set fallback
        fallbackCountryCode = getCurrentLocale().country
    }
    if (subThoroughfare != null) {
        jsonObject.put("house_number", subThoroughfare)
        locationType = setIfLower(LocationType.StreetNumber, locationType)
    }
    if (thoroughfare != null) {
        jsonObject.put("road", thoroughfare)
        locationType = setIfLower(LocationType.Street, locationType)
    }
    if (subLocality != null) {
        jsonObject.put("neighbourhood", subLocality)
        locationType = setIfLower(LocationType.City, locationType)
    }
    if (locality != null) {
        jsonObject.put("city", locality)
        locationType = setIfLower(LocationType.City, locationType)
    }
    //if (postalCode != null) jsonObject.put("postcode", postalCode)
    //if (subAdminArea != null) jsonObject.put("county", subAdminArea)
    //if (adminArea != null) jsonObject.put("state", adminArea)

    var json = jsonObject.toString()
    json = json.replace("\\/", "/")

    if (fallbackCountryCode?.isEmpty() == true) fallbackCountryCode = "GB"
    val formattedAddress = formatter.format(json, fallbackCountryCode)

    var chosenName = name
    if ((chosenName == null) || (locationType != LocationType.StreetNumber)) {
        // The results is a street, city or country so doesn't match a POI.
        chosenName = formattedAddress.substringBefore('\n')
    }

    return LocationDescription(
        name = chosenName,
        description = formattedAddress.replace("\n", ", ").substringBeforeLast(","),
        location = LngLatAlt(longitude, latitude),
        locationType = locationType,
        source = LocationSource.AndroidGeocoder
    )
}