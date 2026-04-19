package org.scottishtecharmy.soundscape.locationprovider

import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject

class IosDirectionProvider : DirectionProvider() {

    private val locationManager = CLLocationManager()
    private val delegate = HeadingDelegate(this)

    init {
        locationManager.delegate = delegate
        if (CLLocationManager.headingAvailable()) {
            locationManager.startUpdatingHeading()
        }
    }

    override fun destroy() {
        locationManager.stopUpdatingHeading()
        locationManager.delegate = null
    }

    internal fun onHeadingUpdate(heading: CLHeading) {
        val headingDegrees = if (heading.trueHeading >= 0) {
            heading.trueHeading.toFloat()
        } else {
            heading.magneticHeading.toFloat()
        }

        val uptimeNanos = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000).toLong()

        val direction = DeviceDirection.Builder(
            attitude = floatArrayOf(0f, 0f, 0f, 1f), // Identity quaternion — no attitude data from CLHeading
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
