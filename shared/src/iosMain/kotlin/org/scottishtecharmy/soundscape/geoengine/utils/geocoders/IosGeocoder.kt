package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLPlacemark
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The IosGeocoder uses Apple's CLGeocoder for forward and reverse geocoding. Like the Android
 * platform geocoder, it returns street addresses but typically not POI/business names, so it
 * passes the searched-for name through unchanged for forward geocoding.
 */
@OptIn(ExperimentalForeignApi::class)
class IosGeocoder(
    private val analyticsLogger: (String) -> Unit = {},
) : SoundscapeGeocoder() {

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?,
    ): List<LocationDescription>? {
        analyticsLogger("iosGeocode")

        val geocoder = CLGeocoder()
        val region = CLCircularRegion(
            center = CLLocationCoordinate2DMake(nearbyLocation.latitude, nearbyLocation.longitude),
            radius = REGION_HINT_RADIUS_METERS,
            identifier = "soundscape-geocoder-region",
        )

        val placemarks: List<CLPlacemark>? = suspendCoroutine { continuation ->
            geocoder.geocodeAddressString(
                addressString = locationName,
                inRegion = region,
            ) { results: List<*>?, error: NSError? ->
                if (error != null) {
                    continuation.resume(null)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(results as? List<CLPlacemark>)
                }
            }
        }

        if (placemarks.isNullOrEmpty()) return null

        return placemarks.take(MAX_RESULTS).mapNotNull { placemark ->
            placemark.toLocationDescription(locationName)
        }
    }

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean,
    ): LocationDescription? {
        analyticsLogger("iosReverseGeocode")

        val location = userGeometry.location
        val geocoder = CLGeocoder()
        val clLocation = CLLocation(latitude = location.latitude, longitude = location.longitude)

        val placemarks: List<CLPlacemark>? = suspendCoroutine { continuation ->
            geocoder.reverseGeocodeLocation(clLocation) { results: List<*>?, error: NSError? ->
                if (error != null) {
                    continuation.resume(null)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(results as? List<CLPlacemark>)
                }
            }
        }

        if (placemarks.isNullOrEmpty()) return null

        // Prefer a placemark whose street name fuzzy-matches the map-matched way.
        val mapMatchedName = userGeometry.mapMatchedWay?.name
        if (mapMatchedName != null) {
            for (placemark in placemarks) {
                val road = placemark.thoroughfare
                if (road != null && road.fuzzyCompare(mapMatchedName, false) < 0.3) {
                    return placemark.toLocationDescription(null)
                }
            }
        }

        return placemarks.firstOrNull()?.toLocationDescription(null)
    }

    companion object {
        private const val MAX_RESULTS = 5

        // Region is just a hint to CLGeocoder. Roughly matches the Android ±10° bounding box.
        private const val REGION_HINT_RADIUS_METERS = 1_000_000.0
    }
}
