package org.scottishtecharmy.soundscape.screens.home.data

data class LocationDescription(
    val addressName: String? = null,
    val streetNumberAndName: String? = null,
    val postcodeAndLocality: String? = null,
    val country: String? = null,
    val distance: String? = null,
    val latitude: Double,
    val longitude: Double,
)
