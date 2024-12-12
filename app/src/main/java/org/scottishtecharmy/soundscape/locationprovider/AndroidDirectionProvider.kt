package org.scottishtecharmy.soundscape.locationprovider

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import java.util.concurrent.Executors
import kotlin.math.abs

class AndroidDirectionProvider(context : Context) :
    DirectionProvider() {

    private val fusedOrientationProviderClient =
        LocationServices.getFusedOrientationProviderClient(context)
    private lateinit var listener: DeviceOrientationListener
    private var lastUpdateTime = 0L
    private var lastUpdateValue = 0.0

    override fun destroy() {
        fusedOrientationProviderClient.removeOrientationUpdates(listener)
    }

    @SuppressLint("MissingPermission")
    override fun start(audio: NativeAudioEngine, locProvider: LocationProvider){
        listener = DeviceOrientationListener { orientation ->
            // Always send the update to the audio engine. This immediately affects the direction
            // and sound of the audio beacon.
            var latitude = 0.0
            var longitude = 0.0
            val newHeading = orientation.headingDegrees.toDouble()
            val location = locProvider.locationFlow.value
            if(location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
            audio.updateGeometry(
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
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT)
            .build()
        // Thought I could use a Looper here like for location but it seems to want an Executor instead
        // Not clear on what the difference is...
        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }

    companion object {
        private const val TAG = "AndroidDirectionProvider"
    }
}