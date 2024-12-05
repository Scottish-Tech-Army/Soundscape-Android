package org.scottishtecharmy.soundscape.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanFilter
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

abstract class LocationProvider {
    abstract fun start(context : Context)
    abstract fun destroy()

    fun getCurrentLatitude() : Double? {
        return mutableLocationFlow.value?.latitude
    }
    fun getCurrentLongitude() : Double? {
        return mutableLocationFlow.value?.longitude
    }
    fun get() : LngLatAlt? {
        mutableLocationFlow.value?.let { location ->
            return LngLatAlt(location.longitude, location.latitude)
        }
        return null
    }

    // Flow to return Location objects
    val mutableLocationFlow = MutableStateFlow<Location?>(null)
    var locationFlow: StateFlow<Location?> = mutableLocationFlow
}

abstract class DirectionProvider {
    abstract fun start(audioEngine: NativeAudioEngine, locationProvider: LocationProvider)
    abstract fun destroy()

    fun getCurrentDirection() : Float {
        var heading = 0.0F
        mutableOrientationFlow.value?.let{ heading = it.headingDegrees }
        return heading
    }

    // Flow to return DeviceOrientation objects
    val mutableOrientationFlow = MutableStateFlow<DeviceOrientation?>(null)
    var orientationFlow: StateFlow<DeviceOrientation?> = mutableOrientationFlow
}

class AndroidDirectionProvider(context : Context) :
    DirectionProvider() {

    private val fusedOrientationProviderClient = LocationServices.getFusedOrientationProviderClient(context)
    private lateinit var listener: DeviceOrientationListener
    private var lastUpdateTime = 0L
    private var lastUpdateValue = 0.0

    override fun destroy() {
        fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }

    @SuppressLint("MissingPermission")
    override fun start(audioEngine: NativeAudioEngine, locationProvider: LocationProvider){
        listener = DeviceOrientationListener { orientation ->
            // Always send the update to the audio engine. This immediately affects the direction
            // and sound of the audio beacon.
            var latitude = 0.0
            var longitude = 0.0
            val newHeading = orientation.headingDegrees.toDouble()
            val location = locationProvider.locationFlow.value
            if(location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
            audioEngine.updateGeometry(
                latitude,
                longitude,
                newHeading
            )
            // Now send the value to all of the other interested parties. These do not require
            // such regular updates, or sub-degree precision, so only send changes in  heading
            // greater than 1 degree and throttle to 50Hz maximum.
            val delta = newHeading - lastUpdateValue
            if(abs(delta) > 1.0) {
                val now = System.currentTimeMillis()
                if ((now - lastUpdateTime) > 20) {
                    mutableOrientationFlow.value =
                        orientation  // Emit the DeviceOrientation object

                    lastUpdateTime = now
                    lastUpdateValue = newHeading
                }
            }
        }

        // OUTPUT_PERIOD_DEFAULT = 50Hz / 20ms
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()
        // Thought I could use a Looper here like for location but it seems to want an Executor instead
        // Not clear on what the difference is...
        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }

    companion object {
        private const val TAG = "AndroidDirectionProvider"
    }
}

class AndroidLocationProvider(context : Context) :
    LocationProvider() {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback

    private val filter = KalmanFilter()

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
            ) == PackageManager.PERMISSION_GRANTED) {
            // Faster startup for obtaining initial location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Handle the retrieved location here
                    if (location != null) {
                        mutableLocationFlow.value = filterLocation(location)
                    }
                }
                .addOnFailureListener { _: Exception ->
                }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    mutableLocationFlow.value = filterLocation(location)
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
        private const val TAG = "AndroidLocationProvider"

        // Check for GPS every n seconds
        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds
    }
}

class StaticLocationProvider(private var latitude: Double, private var longitude: Double) :
    LocationProvider() {

    private val location = Location(LocationManager.PASSIVE_PROVIDER)

    override fun destroy() {
    }

    override fun start(context : Context){
        // Simply set our flow source as the passed in location with 10m accuracy so that it's not ignored
        location.latitude = latitude
        location.longitude = longitude
        location.accuracy = 10.0F
        mutableLocationFlow.value = location
    }

    companion object {
        private const val TAG = "StaticLocationProvider"
    }
}
