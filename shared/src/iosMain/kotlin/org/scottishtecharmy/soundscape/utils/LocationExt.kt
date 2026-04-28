package org.scottishtecharmy.soundscape.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import platform.CoreLocation.CLPlacemark
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

private fun setIfLower(newType: LocationType, oldType: LocationType): LocationType {
    return if (newType < oldType) newType else oldType
}

@OptIn(ExperimentalForeignApi::class)
fun CLPlacemark.toLocationDescription(name: String?): LocationDescription? {
    val coord = this.location?.coordinate?.useContents { latitude to longitude } ?: return null
    val (lat, lng) = coord

    val formatter = AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
    val jsonFields = mutableMapOf<String, String>()
    var locationType: LocationType = LocationType.Country
    var fallbackCountryCode: String? = null

    val isoCountryCode = this.ISOcountryCode
    if (isoCountryCode != null) {
        jsonFields["country_code"] = isoCountryCode
    } else {
        fallbackCountryCode = NSLocale.currentLocale.countryCode
    }

    val houseNumber = this.subThoroughfare
    if (houseNumber != null) {
        jsonFields["house_number"] = houseNumber
        locationType = setIfLower(LocationType.StreetNumber, locationType)
    }
    val road = this.thoroughfare
    if (road != null) {
        jsonFields["road"] = road
        locationType = setIfLower(LocationType.Street, locationType)
    }
    val neighbourhood = this.subLocality
    if (neighbourhood != null) {
        jsonFields["neighbourhood"] = neighbourhood
        locationType = setIfLower(LocationType.City, locationType)
    }
    val city = this.locality
    if (city != null) {
        jsonFields["city"] = city
        locationType = setIfLower(LocationType.City, locationType)
    }

    val json = buildJsonObject {
        for ((k, v) in jsonFields) put(k, v)
    }.toString().replace("\\/", "/")

    if (fallbackCountryCode?.isEmpty() == true) fallbackCountryCode = "GB"
    val formattedAddress = try {
        formatter.format(json, fallbackCountryCode)
    } catch (e: Throwable) {
        val retryJson = buildJsonObject {
            for ((k, v) in jsonFields) if (k != "country_code") put(k, v)
        }.toString().replace("\\/", "/")
        formatter.format(retryJson, "GB")
    }

    var chosenName = name
    if ((chosenName == null) || (locationType != LocationType.StreetNumber)) {
        chosenName = formattedAddress.substringBefore('\n')
    }

    return LocationDescription(
        name = chosenName,
        description = formattedAddress.replace("\n", ", ").substringBeforeLast(","),
        location = LngLatAlt(lng, lat),
        locationType = locationType,
        source = LocationSource.IosGeocoder,
    )
}
