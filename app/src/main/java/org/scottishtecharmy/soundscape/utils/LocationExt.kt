package org.scottishtecharmy.soundscape.utils

import android.content.Context
import org.scottishtecharmy.soundscape.geoengine.formatDistance
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

fun ArrayList<Feature>.toLocationDescriptions(
    currentLocation: LngLatAlt,
    localizedContext: Context
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
                        currentLocation.distance((feature.geometry as Point).coordinates),
                        localizedContext
                    ),
                location = (feature.geometry as Point).coordinates,
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
