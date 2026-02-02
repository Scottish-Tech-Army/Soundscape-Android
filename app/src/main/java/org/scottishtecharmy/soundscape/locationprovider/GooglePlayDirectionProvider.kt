package org.scottishtecharmy.soundscape.locationprovider

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import java.util.concurrent.Executors
import kotlin.math.abs

class GooglePlayDirectionProvider(context : Context) :
    DirectionProvider() {

    private val fusedOrientationProviderClient =
        LocationServices.getFusedOrientationProviderClient(context)
    private lateinit var listener: DeviceOrientationListener
    private var lastUpdateTime = 0L
    private var lastUpdateValue = 0.0

    override fun destroy() {
        if(::listener.isInitialized)
            fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }

    /**
     * Converts a Google Play Services DeviceOrientation to our custom DeviceOrientation class
     */
    private fun convertToCustomDeviceOrientation(gmsOrientation: DeviceOrientation): DeviceDirection {
        return DeviceDirection(
            attitude = gmsOrientation.attitude,
            headingDegrees = gmsOrientation.headingDegrees,
            headingAccuracyDegrees = gmsOrientation.headingErrorDegrees,
            elapsedRealtimeNanos = gmsOrientation.elapsedRealtimeNs
        )
    }

    @SuppressLint("MissingPermission")
    override fun start(audio: NativeAudioEngine, locProvider: LocationProvider) {

        listener = DeviceOrientationListener { orientation ->
            val newHeading = orientation.headingDegrees.toDouble()

            // If the heading has changed by more than 1 degree and hasn't changed in the last
            // 20ms, update all interested parties.
            val delta = newHeading - lastUpdateValue
            if (abs(delta) > 1.0) {
                val now = System.currentTimeMillis()
                if ((now - lastUpdateTime) > 20) {
                    // Convert GMS DeviceOrientation to our custom class and emit
                    mutableOrientationFlow.value = convertToCustomDeviceOrientation(orientation)

                    lastUpdateTime = now
                    lastUpdateValue = newHeading
                }
            }
        }

        // OUTPUT_PERIOD_DEFAULT = 50Hz / 20ms
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }
}
