package org.scottishtecharmy.soundscape.utils

import android.location.Address
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

fun Address.toLocationDescription(name: String?): LocationDescription =
    buildAddressLocationDescription(
        latitude = latitude,
        longitude = longitude,
        countryCode = countryCode,
        houseNumber = subThoroughfare,
        road = thoroughfare,
        neighbourhood = subLocality,
        city = locality,
        fallbackCountryCode = if (countryCode == null) getCurrentLocale().country else null,
        providedName = name,
        source = LocationSource.AndroidGeocoder,
    )
