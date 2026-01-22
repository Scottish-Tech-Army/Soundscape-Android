package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.getPhotonLanguage
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import kotlin.collections.fold

/**
 * The PhotonGeocoder class abstracts away the use of photon geo-search server for geocoding and
 * reverse geocoding.
 */
class PhotonGeocoder(val applicationContext: Context) : SoundscapeGeocoder() {

    val sharedPreferences: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedContext: Context?
    ) : List<LocationDescription>?{
        val searchResult = withContext(Dispatchers.IO) {
            try {
                PhotonSearchProvider
                    .getInstance()
                    .getSearchResults(
                        searchString = locationName,
                        latitude = nearbyLocation.latitude,
                        longitude = nearbyLocation.longitude,
                        language = getPhotonLanguage(sharedPreferences)
                    ).execute()
                    .body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting geocode result:", e)
                null
            }
        }
        Analytics.getInstance().logEvent("photonGeocode", null)

        // The geocode result includes the location for the POI. In the case of something
        // like a park this could be a long way from the point that was passed in.

        // Photon sometimes returns the same location twice e.g. "Greggs Milngavi" returns it as
        // a fast food amenity and a bakery shop
        if(searchResult == null) return null

        val ruler = CheapRuler(nearbyLocation.latitude)
        val deduplicate = searchResult.features
            .fold(mutableListOf<Feature>()) { accumulator, result ->
                // Check if we already have this exact name at approximately the same location
                val point = (result.geometry as? Point)
                var isDuplicate = false
                if(point != null) {
                    isDuplicate = accumulator.any {
                        val otherPoint = (it.geometry as? Point)
                        if(otherPoint != null) {
                            it.properties?.get("name") == result.properties?.get("name") &&
                                    ruler.distance(
                                        otherPoint.coordinates,
                                        point.coordinates
                                    ) < 100.0
                        } else
                            false
                    }
                }
                if (!isDuplicate) {
                    accumulator.add(result)
                }
                accumulator
            }

        return deduplicate.mapNotNull{feature -> feature.toLocationDescription(LocationSource.PhotonGeocoder) }
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry, localizedContext: Context?) : LocationDescription? {

        val location = userGeometry.mapMatchedLocation?.point ?: userGeometry.location
        val searchResult = withContext(Dispatchers.IO) {
            try {
                return@withContext PhotonSearchProvider
                    .getInstance()
                    .reverseGeocodeLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        language = getPhotonLanguage(sharedPreferences)
                    ).execute()
                    .body()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting reverse geocode result:", e)
                return@withContext null
            }
        }
        Analytics.getInstance().logEvent("photonReverseGeocode", null)

        searchResult?.features?.forEach { Log.d(TAG, "$it") }
        return searchResult?.features?.firstNotNullOfOrNull { feature ->
            feature.toLocationDescription(LocationSource.PhotonGeocoder)
        }
    }

    companion object {
        const val TAG = "PhotonGeocoder"
    }
}