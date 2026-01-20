package org.scottishtecharmy.soundscape.utils

import android.location.Address
import org.json.JSONObject
import org.scottishtecharmy.soundscape.components.LocationSource
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

fun Feature.toLocationDescription(source: LocationSource): LocationDescription? =
    properties?.let { properties ->
        // We use the AndroidAddressFormatter library to try and generate addresses which are
        // locale correct e.g. street numbers before/after street name
        // It's got a clunky API that takes in JSON which might be a problem if any of our
        // strings aren't JSON friendly. It would be better to have an API which took in the

        val formatter = AndroidAddressFormatter(false, false, false)
        val jsonObject = JSONObject()
        var opposite = false
        var locationType : LocationType = LocationType.Country

        properties.forEach { (key, value) ->
            when (key) {
                "countrycode" -> jsonObject.put("country_code", value.toString())
                "housenumber" -> {
                    jsonObject.put("house_number", value.toString())
                    locationType = setIfLower(LocationType.StreetNumber, locationType)
                }
                "street" -> {
                    jsonObject.put("road", value.toString())
                    locationType = setIfLower(LocationType.Street, locationType)
                }
                "district" -> {
                    jsonObject.put("neighbourhood", value.toString())
                    locationType = setIfLower(LocationType.City, locationType)
                }
                "city" -> {
                    jsonObject.put(key, value.toString())
                    locationType = setIfLower(LocationType.City, locationType)
                }
                "postcode",
                "county",
                "state",
                "country" -> jsonObject.put(key, value.toString())
                "opposite" -> opposite = (value as Boolean)
            }
        }
        var json = jsonObject.toString()
        json = json.replace("\\/", "/")
        var fallbackCountryCode = getCurrentLocale().country
        if(fallbackCountryCode.isEmpty()) fallbackCountryCode = "GB"
        val formattedAddress = formatter.format(json, fallbackCountryCode)
        var name : String? = properties["name"] as String?
        if(name != null) {
            // Named locations are as good a street number
            locationType = setIfLower(LocationType.StreetNumber, locationType)
        }
        val mvt = (this as? MvtFeature)
        if(mvt != null) {
            name = mvt.name
        }

        LocationDescription(
            name = name ?: formattedAddress.substringBefore('\n'),
            description = formattedAddress,
            location = (geometry as Point?)?.coordinates ?: LngLatAlt(),
            opposite = opposite,
            locationType = locationType,
            source = source
        )
    }

fun Address.toLocationDescription(name: String?): LocationDescription {

    val formatter = AndroidAddressFormatter(false, true, false)
    val jsonObject = JSONObject()
    var locationType : LocationType = LocationType.Country
    if (countryName != null) jsonObject.put("country", countryName)
    if (countryCode != null) jsonObject.put("country_code", countryCode)
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
    if (postalCode != null) jsonObject.put("postcode", postalCode)
    if (subAdminArea != null) jsonObject.put("county", subAdminArea)
    if (adminArea != null) jsonObject.put("state", adminArea)

    var json = jsonObject.toString()
    json = json.replace("\\/", "/")

    var fallbackCountryCode = getCurrentLocale().country
    if(fallbackCountryCode.isEmpty()) fallbackCountryCode = "GB"
    val formattedAddress = formatter.format(json, fallbackCountryCode)

    var chosenName = name
    if ((chosenName == null) || (locationType != LocationType.StreetNumber)) {
        // The results is a street, city or country so doesn't match a POI.
        chosenName = formattedAddress.substringBefore('\n')
    }

    return LocationDescription(
        name = chosenName,
        description = formattedAddress,
        location = LngLatAlt(longitude, latitude),
        locationType = locationType,
        source = LocationSource.AndroidGeocoder
    )
}