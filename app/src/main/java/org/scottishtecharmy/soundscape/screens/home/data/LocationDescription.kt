package org.scottishtecharmy.soundscape.screens.home.data

data class LocationDescription(
    val addressName: String? = null,
    val fullAddress: String? = null,
    val distance: String? = null,
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val marker: Boolean = false,
)
