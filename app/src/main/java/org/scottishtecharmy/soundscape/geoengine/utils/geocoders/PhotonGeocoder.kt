package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.toLocationDescriptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The PhotonGeocoder class abstracts away the use of photon geo-search server for geocoding and
 * reverse geocoding.
 */
class PhotonGeocoder : SoundscapeGeocoder() {

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
    ) : LocationDescription?{
        val searchResult = withContext(Dispatchers.IO) {
            try {
                PhotonSearchProvider
                    .getInstance()
                    .getSearchResults(
                        searchString = locationName,
                        latitude = nearbyLocation.latitude,
                        longitude = nearbyLocation.longitude,
                    ).execute()
                    .body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting reverse geocode result:", e)
                null
            }
        }
        searchResult?.features?.forEach { Log.d(TAG, "$it") }

        // The geocode result includes the location for the POI. In the case of something
        // like a park this could be a long way from the point that was passed in.
        val ld = searchResult?.features?.toLocationDescriptions()
        return ld?.firstOrNull()
    }

    override suspend fun getAddressFromLngLat(location: LngLatAlt) : LocationDescription? {
        val searchResult = withContext(Dispatchers.IO) {
            try {
                return@withContext PhotonSearchProvider
                    .getInstance()
                    .reverseGeocodeLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).execute()
                    .body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting reverse geocode result:", e)
                return@withContext null
            }
        }
        searchResult?.features?.forEach { Log.d(TAG, "$it") }
        return searchResult?.features?.toLocationDescriptions()?.firstOrNull()
    }

    companion object {
        const val TAG = "PhotonGeocoder"
    }
}