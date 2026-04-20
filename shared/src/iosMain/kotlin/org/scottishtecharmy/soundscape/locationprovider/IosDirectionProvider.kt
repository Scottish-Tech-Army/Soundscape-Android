package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosDirectionProvider : DirectionProvider() {

    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionManager()
    private val delegate = HeadingDelegate(this)

    // Latest attitude quaternion from CMMotionManager
    private var currentAttitude = floatArrayOf(0f, 0f, 0f, 1f) // identity = flat

    init {
        locationManager.delegate = delegate
        if (CLLocationManager.headingAvailable()) {
            locationManager.startUpdatingHeading()
        }

        // Start device motion updates for attitude (flat/upright detection)
        if (motionManager.isDeviceMotionAvailable()) {
            motionManager.deviceMotionUpdateInterval = 0.2 // 5 Hz
            motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
                if (motion != null) {
                    val q = motion.attitude.quaternion.useContents {
                        floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
                    }
                    currentAttitude = q
                }
            }
        }
    }

    override fun destroy() {
        locationManager.stopUpdatingHeading()
        locationManager.delegate = null
        if (motionManager.isDeviceMotionActive()) {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    internal fun onHeadingUpdate(heading: CLHeading) {
        val headingDegrees = if (heading.trueHeading >= 0) {
            heading.trueHeading.toFloat()
        } else {
            heading.magneticHeading.toFloat()
        }

        val uptimeNanos = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000).toLong()

        val direction = DeviceDirection.Builder(
            attitude = currentAttitude,
            headingDegrees = headingDegrees,
            headingAccuracyDegrees = heading.headingAccuracy.toFloat(),
            elapsedRealtimeNanos = uptimeNanos
        ).build()

        mutableOrientationFlow.value = direction
    }
}

private class HeadingDelegate(
    private val provider: IosDirectionProvider
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        provider.onHeadingUpdate(didUpdateHeading)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("Heading error: ${didFailWithError.localizedDescription}")
    }
}
