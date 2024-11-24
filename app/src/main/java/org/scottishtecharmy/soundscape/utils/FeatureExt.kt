package org.scottishtecharmy.soundscape.utils

import android.location.Location
import org.scottishtecharmy.soundscape.components.SearchItem
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import java.util.Locale

fun ArrayList<Feature>.toSearchItems(
    currentLocationLatitude: Double,
    currentLocationLongitude: Double
): List<SearchItem> =
    mapNotNull { feature ->
        feature.properties?.let { properties ->
            SearchItem(
                adressName = properties["name"]?.toString(),
                streetNumberAndName = listOfNotNull(
                    properties["housenumber"],
                    properties["street"]
                ).joinToString(" ").nullIfEmpty(),
                postcodeAndLocality = listOfNotNull(
                    properties["postcode"],
                    properties["city"]
                ).joinToString(" ").nullIfEmpty(),
                country = properties["country"]?.toString()?.nullIfEmpty(),
                distance = formatDistance(
                    calculateDistance(
                        lat1 = currentLocationLatitude,
                        lon1 = currentLocationLongitude,
                        lat2 = (feature.geometry as Point).coordinates.latitude,
                        lon2 = (feature.geometry as Point).coordinates.longitude
                    )
                ),
                latitude = (feature.geometry as Point).coordinates.latitude,
                longitude = (feature.geometry as Point).coordinates.longitude
            )
        }
    }


private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

private fun formatDistance(distanceInMeters: Float): String {
    val distanceInKm = distanceInMeters / 1000
    return String.format(Locale.getDefault(), "%.1f km", distanceInKm)
}