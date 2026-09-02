package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.osmKeyForPoiCategory
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLPlacemark
import platform.Foundation.NSError
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKLocalSearch
import platform.MapKit.MKLocalSearchRequest
import platform.MapKit.MKLocalSearchResponse
import platform.MapKit.MKLocalSearchResultTypeAddress
import platform.MapKit.MKLocalSearchResultTypePointOfInterest
import platform.MapKit.MKMapItem
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The iOS platform geocoder.
 *
 * Forward search uses MapKit's MKLocalSearch, which is a *place* search: it knows about businesses
 * and landmarks by name and returns each result's category. CLGeocoder, which this used to use, is
 * an address geocoder - it has no POI index at all, so a search for "Kelvingrove Museum" got
 * nothing back. MKLocalSearch is asked for address results as well as points of interest, so it
 * covers what CLGeocoder used to do here too.
 *
 * Reverse geocoding stays on CLGeocoder: MKLocalSearch has no reverse equivalent, and CLGeocoder
 * is what produces the house-number-level addresses that [FusedGeocoder] prefers over Photon's.
 */
@OptIn(ExperimentalForeignApi::class)
class IosGeocoder(
    private val analyticsLogger: (String) -> Unit = {},
) : SoundscapeGeocoder() {

    /**
     * MKLocalSearch covers the same ground Photon does for search, so [FusedGeocoder] hands
     * searches to this geocoder alone rather than merging two sets of results.
     */
    override val providesPlaceSearch = true

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?,
    ): List<LocationDescription>? {
        analyticsLogger("iosPlaceSearch")

        val request = MKLocalSearchRequest()
        request.naturalLanguageQuery = locationName
        request.resultTypes =
            MKLocalSearchResultTypePointOfInterest or MKLocalSearchResultTypeAddress
        request.region = MKCoordinateRegionMakeWithDistance(
            centerCoordinate = CLLocationCoordinate2DMake(
                nearbyLocation.latitude,
                nearbyLocation.longitude,
            ),
            latitudinalMeters = SEARCH_REGION_METERS,
            longitudinalMeters = SEARCH_REGION_METERS,
        )

        val search = MKLocalSearch(request)
        val response: MKLocalSearchResponse? = suspendCancellableCoroutine { continuation ->
            // Search runs per keystroke, so an abandoned query has to be told to stop rather than
            // left to complete - MapKit throttles, and spent requests come out of the same budget
            // as the one the user is actually waiting on.
            continuation.invokeOnCancellation { search.cancel() }
            search.startWithCompletionHandler { result: MKLocalSearchResponse?, error: NSError? ->
                if (continuation.isActive) {
                    continuation.resume(if (error != null) null else result)
                }
            }
        }

        val mapItems = response?.mapItems ?: return null
        if (mapItems.isEmpty()) return null

        return mapItems
            .take(MAX_SEARCH_RESULTS)
            .mapNotNull { item -> (item as? MKMapItem)?.toLocationDescription(localizedStrings) }
            .takeIf { it.isNotEmpty() }
    }

    /**
     * MKMapItem carries the place's own name and category alongside a CLPlacemark holding the
     * address, so the address half reuses the same mapping the reverse geocoder uses and only the
     * name and category are filled in from the map item.
     */
    private fun MKMapItem.toLocationDescription(
        localizedStrings: LocalizedStrings?,
    ): LocationDescription? {
        val description = placemark.toLocationDescription(
            name = name,
            preferProvidedName = true,
        ) ?: return null

        osmKeyForPoiCategory(pointOfInterestCategory)
            ?.let { key -> localizedStrings?.resolveFeatureClass(key) }
            ?.let { categoryText ->
                description.typeDescription = TextForFeature(
                    text = description.name,
                    generic = false,
                    additionalText = categoryText,
                )
            }

        return description
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
        private const val MAX_SEARCH_RESULTS = 10

        // Matches the legacy iOS app's MKLocalSearch region. It is only a hint - MapKit will
        // return results from outside it - but it is what biases results towards the user.
        private const val SEARCH_REGION_METERS = 75_000.0
    }
}
