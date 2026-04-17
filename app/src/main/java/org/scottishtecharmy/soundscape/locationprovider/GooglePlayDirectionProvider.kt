package org.scottishtecharmy.soundscape.locationprovider

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
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

    private fun convertToCustomDeviceOrientation(gmsOrientation: DeviceOrientation): DeviceDirection {
        return DeviceDirection(
            attitude = gmsOrientation.attitude,
            headingDegrees = gmsOrientation.headingDegrees,
            headingAccuracyDegrees = gmsOrientation.headingErrorDegrees,
            elapsedRealtimeNanos = gmsOrientation.elapsedRealtimeNs
        )
    }

    @SuppressLint("MissingPermission")
    fun start() {

        listener = DeviceOrientationListener { orientation ->
            val newHeading = orientation.headingDegrees.toDouble()

            val delta = newHeading - lastUpdateValue
            if (abs(delta) > 1.0) {
                val now = System.currentTimeMillis()
                if ((now - lastUpdateTime) > 20) {
                    mutableOrientationFlow.value = convertToCustomDeviceOrientation(orientation)

                    lastUpdateTime = now
                    lastUpdateValue = newHeading
                }
            }
        }

        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }
}
