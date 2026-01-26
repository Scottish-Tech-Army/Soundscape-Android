package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The AndroidGeocoder class abstracts away the use of a built in Android Geocoder for geocoding
 * and reverse geocoding.
 */
class AndroidGeocoder(val applicationContext: Context) : SoundscapeGeocoder() {
    private var geocoder: Geocoder = Geocoder(applicationContext)

    /**
     * The main weakness of the AndroidGeocoder is that it doesn't include the names of Points of
     * Interest in the search results. It will include the address, but it won't have the associated
     * business name for that address. All we can do is pass through the "locationName" that was
     * searched for, typos and all.
     */
    override suspend fun getAddressFromLocationName(locationName: String,
                                                    nearbyLocation: LngLatAlt,
                                                    localizedContext: Context?) : List<LocationDescription>? {
        if(!enabled)
            return null

        Analytics.getInstance().logEvent("androidGeocode", null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCoroutine { continuation ->
                try {
                    val geocodeListener = object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            Log.d(
                                TAG,
                                "getFromLocationName results count " + addresses.size.toString()
                            )
                            if (addresses.isNotEmpty()) {
                                continuation.resume(addresses)
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.d(TAG, "AndroidGeocoder error: $errorMessage")
                            continuation.resume(null)
                        }
                    }
                    geocoder.getFromLocationName(
                        locationName,
                        5,
                        nearbyLocation.latitude - 10.0,
                        nearbyLocation.longitude - 10.0,
                        nearbyLocation.latitude + 10.0,
                        nearbyLocation.longitude + 10.0,
                        geocodeListener
                    )
                } catch (e: Exception) {
                    val bundle = Bundle().apply { putString("exception", e.toString()) }
                    Analytics.getInstance().logEvent("androidGeocoderError", bundle)
                    Log.d(TAG, "AndroidGeocoder error: $e")
                    continuation.resume(null)
                }
            }?.map{feature -> feature.toLocationDescription(locationName) }
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocationName(
                    locationName,
                    5,
                    nearbyLocation.latitude - 10.0,
                    nearbyLocation.longitude - 10.0,
                    nearbyLocation.latitude + 10.0,
                    nearbyLocation.longitude + 10.0,
                )
                if (addresses != null) {
                    return addresses.mapNotNull { feature ->
                        feature.toLocationDescription(
                            locationName
                        )
                    }
                }
            } catch (e: Exception) {
                val bundle = Bundle().apply { putString("exception", e.toString()) }
                Analytics.getInstance().logEvent("androidGeocoderError", bundle)
                Log.d(TAG, "AndroidGeocoder error: $e")
            }
        }
        return null
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry, localizedContext: Context?) : LocationDescription? {
        if(!enabled)
            return null

        Analytics.getInstance().logEvent("androidReverseGeocode", null)

        val location = userGeometry.location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(
                        location.latitude, location.longitude, 5,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                Log.d(
                                    TAG,
                                    "getAddressFromLocationName results count " + addresses.size.toString()
                                )
                                val name = userGeometry.mapMatchedWay?.name
                                if (name != null) {
                                    for (address in addresses) {
                                        Log.d(TAG, "$address")
                                        if (address.thoroughfare.fuzzyCompare(name, false) < 0.3) {
                                            continuation.resume(address)
                                            return
                                        }
                                    }
                                }
                                continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                Log.d(TAG, "AndroidGeocoder error: $errorMessage")
                                continuation.resume(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    val bundle = Bundle().apply { putString("exception", e.toString()) }
                    Analytics.getInstance().logEvent("androidGeocoderError", bundle)
                    Log.d(TAG, "AndroidGeocoder error: $e")
                    continuation.resume(null)
                }
            }?.toLocationDescription(null)
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 5)
                return addresses?.firstOrNull()?.toLocationDescription(null)
            } catch (e: Exception) {
                val bundle = Bundle().apply { putString("exception", e.toString()) }
                Analytics.getInstance().logEvent("androidGeocoderError", bundle)
                Log.d(TAG, "AndroidGeocoder error: $e")
            }
            return null
        }
    }

    companion object {
        const val TAG = "AndroidGeocoder"

        // Not all Android platforms have Geocoder capability
        val enabled = Geocoder.isPresent()
    }
}