package org.scottishtecharmy.soundscape.utils

import android.content.Context
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAsString
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
            LocationDescription(
                addressName = properties["name"]?.toString(),
                streetNumberAndName =
                    listOfNotNull(
                        properties["housenumber"],
                        properties["street"],
                    ).joinToString(" ").nullIfEmpty(),
                postcodeAndLocality =
                    listOfNotNull(
                        properties["postcode"],
                        properties["city"],
                    ).joinToString(" ").nullIfEmpty(),
                country = properties["country"]?.toString()?.nullIfEmpty(),
                distance =
                    formatDistanceAsString(
                        currentLocation.distance((feature.geometry as Point).coordinates),
                        localizedContext
                    ),
                location = (feature.geometry as Point).coordinates,
            )
        }
    }

fun LocationDescription.buildAddressFormat(): String? {
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
