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

class AndroidLocationProvider(context: Context) : LocationProvider() {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val filter = KalmanLocationFilter()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            publishLocation(location)
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            for (location in locations) {
                publishLocation(location)
            }
        }

        @Deprecated("Deprecated in API level 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

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
            val lastNetworkLocation =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val lastLocation = when {
                lastGpsLocation != null && lastNetworkLocation != null -> {
                    if (lastGpsLocation.time > lastNetworkLocation.time) lastGpsLocation else lastNetworkLocation
                }

                lastGpsLocation != null -> lastGpsLocation
                lastNetworkLocation != null -> lastNetworkLocation
                else -> null
            }

            lastLocation?.let { location ->
                publishLocation(location)
            }
        }
    }

    /**
     * Publishes a fix on both flows. Every fix is published, however inaccurate: dropping the ones
     * that aren't good enough to act on is GeoEngine's job (see LocationProvider.isLocationUsable),
     * so that the GPX recorder and the map still see what the receiver actually reported.
     */
    private fun publishLocation(location: Location) {
        mutableLocationFlow.value = location.toSoundscapeLocation()
        mutableFilteredLocationFlow.value = filterLocation(location).toSoundscapeLocation()
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
    override fun start(accuracy: Accuracy) {
        when (accuracy) {
            Accuracy.High -> {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        accuracy.updateInterval.inWholeMilliseconds,
                        accuracy.minimumDistanceM,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
            }

            Accuracy.Balanced -> {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        accuracy.updateInterval.inWholeMilliseconds,
                        accuracy.minimumDistanceM,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        }
    }
}
