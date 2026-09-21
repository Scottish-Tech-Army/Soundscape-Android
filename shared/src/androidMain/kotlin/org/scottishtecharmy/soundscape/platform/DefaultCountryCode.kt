package org.scottishtecharmy.soundscape.platform

import java.util.Locale

actual fun getDefaultCountryCode(): String {
    val country = Locale.getDefault().country
    return if (country.isNullOrEmpty()) "GB" else country
}
