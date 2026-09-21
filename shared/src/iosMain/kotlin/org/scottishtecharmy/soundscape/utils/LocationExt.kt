package org.scottishtecharmy.soundscape.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import platform.CoreLocation.CLPlacemark
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

@OptIn(ExperimentalForeignApi::class)
fun CLPlacemark.toLocationDescription(
    name: String?,
    preferProvidedName: Boolean = false,
): LocationDescription? {
    val coord = this.location?.coordinate?.useContents { latitude to longitude } ?: return null
    val (lat, lng) = coord
    val isoCountryCode = this.ISOcountryCode
    return buildAddressLocationDescription(
        latitude = lat,
        longitude = lng,
        countryCode = isoCountryCode,
        houseNumber = this.subThoroughfare,
        road = this.thoroughfare,
        neighbourhood = this.subLocality,
        city = this.locality,
        fallbackCountryCode = if (isoCountryCode == null) NSLocale.currentLocale.countryCode else null,
        providedName = name,
        source = LocationSource.IosGeocoder,
        preferProvidedName = preferProvidedName,
    )
}
