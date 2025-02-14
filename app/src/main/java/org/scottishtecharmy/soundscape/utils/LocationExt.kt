package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

fun ArrayList<Feature>.toLocationDescriptions(): List<LocationDescription> =
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
                name = properties["name"]?.toString(),
                fullAddress = fullAddress,
                location = (feature.geometry as Point).coordinates
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
