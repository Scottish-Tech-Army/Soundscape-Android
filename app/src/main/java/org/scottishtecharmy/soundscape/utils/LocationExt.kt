package org.scottishtecharmy.soundscape.utils

import android.location.Address
import org.json.JSONObject
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType

private fun setIfLower(newType: LocationType, oldType: LocationType) : LocationType {
    return if(newType < oldType) newType else oldType
}

fun Address.toLocationDescription(name: String?): LocationDescription {

    val formatter = AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
    val jsonFields = mutableMapOf<String, String>()
    var locationType : LocationType = LocationType.Country
    var fallbackCountryCode : String? = null
    if (countryCode != null) {
        jsonFields["country_code"] = countryCode
    }
    else {
        fallbackCountryCode = getCurrentLocale().country
    }
    if (subThoroughfare != null) {
        jsonFields["house_number"] = subThoroughfare
        locationType = setIfLower(LocationType.StreetNumber, locationType)
    }
    if (thoroughfare != null) {
        jsonFields["road"] = thoroughfare
        locationType = setIfLower(LocationType.Street, locationType)
    }
    if (subLocality != null) {
        jsonFields["neighbourhood"] = subLocality
        locationType = setIfLower(LocationType.City, locationType)
    }
    if (locality != null) {
        jsonFields["city"] = locality
        locationType = setIfLower(LocationType.City, locationType)
    }

    val jsonObject = JSONObject()
    for ((k, v) in jsonFields) jsonObject.put(k, v)
    var json = jsonObject.toString()
    json = json.replace("\\/", "/")

    if (fallbackCountryCode?.isEmpty() == true) fallbackCountryCode = "GB"
    val formattedAddress = try {
        formatter.format(json, fallbackCountryCode)
    } catch (e: Throwable) {
        val retryObject = JSONObject()
        for ((k, v) in jsonFields) { if (k != "country_code") retryObject.put(k, v) }
        val retryJson = retryObject.toString().replace("\\/", "/")
        formatter.format(retryJson, "GB")
    }

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