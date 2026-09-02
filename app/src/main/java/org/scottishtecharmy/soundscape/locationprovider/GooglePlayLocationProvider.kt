package org.scottishtecharmy.soundscape.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class GooglePlayLocationProvider(context: Context) :
    LocationProvider() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback

    private val filter = KalmanLocationFilter()

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
        val filteredLocation = filter.process(
            LngLatAlt(location.longitude, location.latitude),
            System.currentTimeMillis(),
            location.accuracy.toDouble()
        )
        location.latitude = filteredLocation.latitude
        location.longitude = filteredLocation.longitude

        return location
    }

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        publishLocation(location)
                    }
                }
                .addOnFailureListener { _: Exception ->
                }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    publishLocation(location)
                }
            }
        }
    }

    override fun destroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun start(accuracy: Accuracy) {

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                when (accuracy) {
                    Accuracy.Balanced -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    Accuracy.High -> Priority.PRIORITY_HIGH_ACCURACY
                },
                accuracy.updateInterval.inWholeMilliseconds
            ).apply {
                setMinUpdateDistanceMeters(accuracy.minimumDistanceM)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build(),
            locationCallback,
            Looper.getMainLooper(),
        )
    }
}
