package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

actual fun getDefaultCountryCode(): String {
    // The locale needn't have a region (e.g. just "en"), in which case fall back to GB, as Android does
    val country = NSLocale.currentLocale.countryCode
    return if (country.isNullOrEmpty()) "GB" else country
}
