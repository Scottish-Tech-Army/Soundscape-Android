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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import java.util.concurrent.Executors
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

    // Flow to return Location objects
    val mutableLocationFlow = MutableStateFlow<Location?>(null)
    var locationFlow: StateFlow<Location?> = mutableLocationFlow
}

abstract class DirectionProvider {
    abstract fun start(audioEngine: NativeAudioEngine, locationProvider: LocationProvider)
    abstract fun destroy()

    fun getCurrentDirection() : Float? {
        return mutableOrientationFlow.value?.headingDegrees
    }

    // Flow to return DeviceOrientation objects
    val mutableOrientationFlow = MutableStateFlow<DeviceOrientation?>(null)
    var orientationFlow: StateFlow<DeviceOrientation?> = mutableOrientationFlow
}

class AndroidDirectionProvider(context : Context) :
    DirectionProvider() {

    private val fusedOrientationProviderClient = LocationServices.getFusedOrientationProviderClient(context)
    private lateinit var listener: DeviceOrientationListener

    override fun destroy() {
        fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }

    @SuppressLint("MissingPermission")
    override fun start(audioEngine: NativeAudioEngine, locationProvider: LocationProvider){
        listener = DeviceOrientationListener { orientation ->
            mutableOrientationFlow.value = orientation  // Emit the DeviceOrientation object
                val location = locationProvider.locationFlow.value
                if(location != null) {
                    audioEngine.updateGeometry(
                        location.latitude,
                        location.longitude,
                        orientation.headingDegrees.toDouble()
                    )
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
                        mutableLocationFlow.value = location
                    }
                }
                .addOnFailureListener { _: Exception ->
                }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    mutableLocationFlow.value = location
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
