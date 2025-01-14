package org.scottishtecharmy.soundscape.locationprovider

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import java.util.concurrent.Executors
import kotlin.math.abs

const val updateTimeoutInMilliseconds = 100L

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
    override fun start(audio: NativeAudioEngine, locProvider: LocationProvider) {

        // We need to have a regular timeout so that when the device orientation isn't changing the
        // audio engine is still updated. This ensures that the location is updated and also that
        // the audio engine queue is processed when there's no movement at all.
        listener = object : DeviceOrientationListener {
            val handler = Handler(Looper.getMainLooper())

            var timeoutRunnable: Runnable = object : Runnable {
                override fun run() {
                    // Update the audio engine with the current location and the last heading that
                    // we processed.
                    val location = locProvider.locationFlow.value
                    audio.updateGeometry(
                        location?.latitude ?: 0.0,
                        location?.longitude ?: 0.0,
                        lastUpdateValue
                    )
                    // Re-schedule timeout
                    handler.postDelayed(this, updateTimeoutInMilliseconds)
                }
            }

            init {
                // Start the timeout
                handler.postDelayed(timeoutRunnable, updateTimeoutInMilliseconds)
            }

            override fun onDeviceOrientationChanged(orientation: DeviceOrientation) {

                val newHeading = orientation.headingDegrees.toDouble()

                // If the heading has changed by more than 1 degree and hasn't changed in the last
                // 20ms, update all interested parties.
                val delta = newHeading - lastUpdateValue
                if (abs(delta) > 1.0) {
                    val now = System.currentTimeMillis()
                    if ((now - lastUpdateTime) > 20) {

                        // Cancel timeout as we have processed an update
                        handler.removeCallbacks(timeoutRunnable)

                        // Send the update to the audio engine. This affects the direction and sound
                        // of the audio beacon.
                        val location = locProvider.locationFlow.value
                        audio.updateGeometry(
                            location?.latitude ?: 0.0,
                            location?.longitude ?: 0.0,
                            newHeading
                        )

                        // Emit the DeviceOrientation object
                        mutableOrientationFlow.value = orientation

                        lastUpdateTime = now
                        lastUpdateValue = newHeading

                        // Re-schedule timeout
                        handler.postDelayed(timeoutRunnable, updateTimeoutInMilliseconds)
                    }
                }
            }
        }

        // OUTPUT_PERIOD_DEFAULT = 50Hz / 20ms
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        fusedOrientationProviderClient.requestOrientationUpdates(request, executor, listener)
    }

    companion object {
        private const val TAG = "AndroidDirectionProvider"
    }
}