package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType

/**
 * Build a [LocationDescription] from the address fields a platform geocoder
 * exposes. Each platform has its own type for the geocoder result
 * (`android.location.Address`, `CLPlacemark`) but the formatting and
 * fall-through logic is identical, so it lives here.
 *
 * [fallbackCountryCode] is consulted only if the geocoder did not provide
 * a country code; an empty fallback is treated as `"GB"`. If the formatter
 * fails (e.g. unsupported country), it is retried without the country code.
 *
 * The chosen name uses [providedName] when the result resolves down to a
 * specific street number; otherwise it falls back to the formatted address.
 *
 * Set [preferProvidedName] when [providedName] is the place's own name rather
 * than the string the user searched for - an MKMapItem's `name`, say. A search
 * hit for "Kelvingrove Art Gallery" should keep that as its name and show the
 * address underneath, whereas a plain address geocoder only has the user's
 * query to offer and must not present it as the name of whatever came back.
 */
fun buildAddressLocationDescription(
    latitude: Double,
    longitude: Double,
    countryCode: String?,
    houseNumber: String?,
    road: String?,
    neighbourhood: String?,
    city: String?,
    fallbackCountryCode: String?,
    providedName: String?,
    source: LocationSource,
    preferProvidedName: Boolean = false,
): LocationDescription {
    val formatter =
        AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
    val fields = mutableMapOf<String, String>()
    var locationType: LocationType = LocationType.Country
    var fallback = fallbackCountryCode

    if (countryCode != null) {
        fields["country_code"] = countryCode
    }
    if (houseNumber != null) {
        fields["house_number"] = houseNumber
        if (LocationType.StreetNumber < locationType) locationType = LocationType.StreetNumber
    }
    if (road != null) {
        fields["road"] = road
        if (LocationType.Street < locationType) locationType = LocationType.Street
    }
    if (neighbourhood != null) {
        fields["neighbourhood"] = neighbourhood
        if (LocationType.City < locationType) locationType = LocationType.City
    }
    if (city != null) {
        fields["city"] = city
        if (LocationType.City < locationType) locationType = LocationType.City
    }

    val json = buildJsonObject {
        for ((k, v) in fields) put(k, v)
    }.toString().replace("\\/", "/")

    if (fallback?.isEmpty() == true) fallback = "GB"
    val formattedAddress = try {
        formatter.format(json, fallback)
    } catch (e: Throwable) {
        try {
            val retryJson = buildJsonObject {
                for ((k, v) in fields) if (k != "country_code") put(k, v)
            }.toString().replace("\\/", "/")
            formatter.format(retryJson, "GB")
        } catch (e2: Throwable) {
            fields.filterKeys { it != "country_code" }.values.joinToString(", ")
        }
    }

    var chosenName = providedName?.takeIf { it.isNotBlank() }
    if (chosenName == null ||
        (!preferProvidedName && locationType != LocationType.StreetNumber)
    ) {
        chosenName = formattedAddress.substringBefore('\n')
    }

    return LocationDescription(
        name = chosenName,
        description = formattedAddress.replace("\n", ", ").substringBeforeLast(","),
        location = LngLatAlt(longitude, latitude),
        locationType = locationType,
        source = source,
    )
}
