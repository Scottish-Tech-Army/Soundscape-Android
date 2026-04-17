package org.scottishtecharmy.soundscape.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.time.Duration.Companion.seconds

class AndroidLocationProvider(context: Context) : LocationProvider() {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val filter = KalmanLocationFilter()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            mutableLocationFlow.value = location.toSoundscapeLocation()
            mutableFilteredLocationFlow.value = filterLocation(location).toSoundscapeLocation()
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            for (location in locations) {
                mutableLocationFlow.value = location.toSoundscapeLocation()
                mutableFilteredLocationFlow.value = filterLocation(location).toSoundscapeLocation()
            }
        }

        @Deprecated("Deprecated in API level 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val lastLocation = when {
                lastGpsLocation != null && lastNetworkLocation != null -> {
                    if (lastGpsLocation.time > lastNetworkLocation.time) lastGpsLocation else lastNetworkLocation
                }
                lastGpsLocation != null -> lastGpsLocation
                lastNetworkLocation != null -> lastNetworkLocation
                else -> null
            }

            lastLocation?.let { location ->
                mutableLocationFlow.value = location.toSoundscapeLocation()
                mutableFilteredLocationFlow.value = filterLocation(location).toSoundscapeLocation()
            }
        }
    }

    fun filterLocation(location: Location): Location {
        val filteredLocation = Location(location)

        val filtered = filter.process(
            LngLatAlt(location.longitude, location.latitude),
            System.currentTimeMillis(),
            location.accuracy.toDouble()
        )
        filteredLocation.latitude = filtered.latitude
        filteredLocation.longitude = filtered.longitude

        return filteredLocation
    }

    override fun destroy() {
        locationManager.removeUpdates(locationListener)
    }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATES_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATES_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    companion object {
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
        private const val MIN_DISTANCE_METERS = 1f
    }
}
