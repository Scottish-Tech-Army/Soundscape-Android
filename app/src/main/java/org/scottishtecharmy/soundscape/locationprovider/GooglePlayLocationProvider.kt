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
import kotlin.time.Duration.Companion.seconds

class GooglePlayLocationProvider(context : Context) :
    LocationProvider() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback

    private val filter = KalmanLocationFilter()

    fun filterLocation(location: Location) : Location {
        // Filter the location through the Kalman filter
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
            // Faster startup for obtaining initial location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Handle the retrieved location here
                    if (location != null) {
                        mutableLocationFlow.value = location
                        mutableFilteredLocationFlow.value = filterLocation(location)
                    }
                }
                .addOnFailureListener { _: Exception ->
                }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    mutableLocationFlow.value = location
                    mutableFilteredLocationFlow.value = filterLocation(location)
                }
            }
        }
    }

    override fun destroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun start(context : Context){

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATES_INTERVAL_MS
            ).apply {
                setMinUpdateDistanceMeters(1f)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build(),
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    companion object {
        // Check for GPS every n seconds
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
    }
}