package org.scottishtecharmy.soundscape.utils

import android.location.Location
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import java.util.Locale

fun ArrayList<Feature>.toLocationDescriptions(
    currentLocationLatitude: Double,
    currentLocationLongitude: Double,
): List<LocationDescription> =
    mapNotNull { feature ->
        feature.properties?.let { properties ->
            val streetNumberAndName =
                listOfNotNull(
                    properties["housenumber"],
                    properties["street"],
                ).joinToString(" ").nullIfEmpty()
            val postcodeAndLocality =
                listOfNotNull(
                    properties["postcode"],
                    properties["city"],
                ).joinToString(" ").nullIfEmpty()
            val country = properties["country"]?.toString()?.nullIfEmpty()

            val fullAddress = buildAddressFormat(streetNumberAndName, postcodeAndLocality, country)
            LocationDescription(
                addressName = properties["name"]?.toString(),
                fullAddress = fullAddress,
                distance =
                    formatDistance(
                        calculateDistance(
                            lat1 = currentLocationLatitude,
                            lon1 = currentLocationLongitude,
                            lat2 = (feature.geometry as Point).coordinates.latitude,
                            lon2 = (feature.geometry as Point).coordinates.longitude,
                        ),
                    ),
                latitude = (feature.geometry as Point).coordinates.latitude,
                longitude = (feature.geometry as Point).coordinates.longitude,
            )
        }
    }

fun buildAddressFormat(
    streetNumberAndName: String?,
    postcodeAndLocality: String?,
    country: String?,
): String? {
    val addressFormat =
        listOfNotNull(
            streetNumberAndName,
            postcodeAndLocality,
            country,
        )
    return when {
        addressFormat.isEmpty() -> null
        else -> addressFormat.joinToString("\n")
    }
}

private fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

private fun formatDistance(distanceInMeters: Float): String {
    val distanceInKm = distanceInMeters / 1000
    return String.format(Locale.getDefault(), "%.1f km", distanceInKm)
}

fun Location.calculateDistanceTo(
    lat: Double,
    lon: Double,
): String {
    val results = FloatArray(1)
    Location.distanceBetween(
        this.latitude,
        this.longitude,
        lat,
        lon,
        results,
    )
    val distanceInMeters = results[0]
    val distanceInKm = distanceInMeters / 1000
    return String.format(Locale.getDefault(), "%.1f km", distanceInKm)
}
