package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The AndroidGeocoder class abstracts away the use of a built in Android Geocoder for geocoding
 * and reverse geocoding.
 */
class AndroidGeocoder(val applicationContext: Context) : SoundscapeGeocoder() {
    private var geocoder: Geocoder = Geocoder(applicationContext)

    // Not all Android platforms have Geocoder capability
    val enabled = Geocoder.isPresent()

    override suspend fun getAddressFromLocationName(locationName: String, nearbyLocation: LngLatAlt) : LocationDescription? {
        if(!enabled)
            return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCoroutine { continuation ->
                val geocodeListener =
                    Geocoder.GeocodeListener { addresses ->
                        Log.d(
                            TAG,
                            "getAddressFromLocationName results count " + addresses.size.toString()
                        )
                        for (address in addresses) {
                            Log.d(TAG, "$address")
                        }
                        if (addresses.isNotEmpty()) {
                            continuation.resume(
                                LocationDescription(
                                    locationName,
                                    LngLatAlt(addresses[0].longitude, addresses[0].latitude)
                                )
                            )
                        }
                    }
                geocoder.getFromLocationName(
                    locationName,
                    5,
                    nearbyLocation.latitude - 0.1,
                    nearbyLocation.longitude - 0.1,
                    nearbyLocation.latitude + 0.1,
                    nearbyLocation.longitude + 0.1,
                    geocodeListener
                )
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(locationName, 5)
            if(addresses != null) {
                for (address in addresses) {
                    Log.d(TAG, "Address: $address")
                }
            }
        }
        return null
    }

    override suspend fun getAddressFromLngLat(location: LngLatAlt) : LocationDescription? {
        if(!enabled)
            return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCoroutine { continuation ->
                val geocodeListener =
                    Geocoder.GeocodeListener { addresses ->
                        Log.d(
                            TAG,
                            "getAddressFromLocationName results count " + addresses.size.toString()
                        )
                        for (address in addresses) {
                            Log.d(TAG, "$address")
                        }
                        continuation.resume(
                            LocationDescription(
                                addresses[0].getAddressLine(0),
                                LngLatAlt(addresses[0].longitude, addresses[0].latitude)
                            )
                        )
                    }
                geocoder.getFromLocation(location.latitude, location.longitude, 5, geocodeListener)
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 5)
            if(addresses != null) {
                for (address in addresses) {
                    Log.d(TAG, "Address: $address")
                }
                return LocationDescription(
                    addresses[0].getAddressLine(0),
                    LngLatAlt(addresses[0].longitude, addresses[0].latitude)
                )
            }
        }
        return null
    }

    companion object {
        const val TAG = "AndroidGeocoder"
    }
}