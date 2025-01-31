package org.scottishtecharmy.soundscape.screens.home.data

data class LocationDescription(
    var addressName: String? = null,
    val fullAddress: String? = null,
    val country: String? = null,
    var distance: String? = null,
    var latitude: Double = Double.NaN,
    var longitude: Double = Double.NaN,
    val marker: Boolean = false
)
