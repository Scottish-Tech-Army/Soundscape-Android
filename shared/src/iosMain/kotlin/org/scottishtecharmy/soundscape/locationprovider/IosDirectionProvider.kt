package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLDeviceOrientationLandscapeLeft
import platform.CoreLocation.CLDeviceOrientationLandscapeRight
import platform.CoreLocation.CLDeviceOrientationPortrait
import platform.CoreLocation.CLDeviceOrientationPortraitUpsideDown
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol

@OptIn(ExperimentalForeignApi::class)
class IosDirectionProvider : DirectionProvider() {

    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionManager()
    private val delegate = HeadingDelegate(this)

    // Latest attitude quaternion from CMMotionManager
    private var currentAttitude = floatArrayOf(0f, 0f, 0f, 1f) // identity = flat

    // Non-null while we are observing device rotation, which also marks the
    // matching beginGeneratingDeviceOrientationNotifications call as owed an end.
    private var orientationObserver: NSObjectProtocol? = null

    init {
        start()
    }

    fun start() {
        locationManager.delegate = delegate
        if (CLLocationManager.headingAvailable()) {
            startTrackingDeviceOrientation()
            locationManager.startUpdatingHeading()
        }

        // Start device motion updates for attitude (flat/upright detection)
        if (motionManager.isDeviceMotionAvailable() && !motionManager.isDeviceMotionActive()) {
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

    fun pause() {
        locationManager.stopUpdatingHeading()
        locationManager.delegate = null
        stopTrackingDeviceOrientation()
        if (motionManager.isDeviceMotionActive()) {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    override fun destroy() {
        pause()
    }

    /**
     * Keeps CLLocationManager's headingOrientation in step with how the device
     * is being held.
     *
     * CoreLocation reports heading relative to the top of the device in portrait
     * until it is told otherwise, so a device held in landscape would swing every
     * heading 90 degrees round - the difference between "on your left" and
     * "straight ahead". iPads are routinely held in landscape.
     */
    private fun startTrackingDeviceOrientation() {
        // beginGeneratingDeviceOrientationNotifications is reference counted and
        // start() is called again on each foreground, so only ever hold one.
        if (orientationObserver != null) return

        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        updateHeadingOrientation()
        orientationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceOrientationDidChangeNotification,
            `object` = UIDevice.currentDevice,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> updateHeadingOrientation() }
    }

    private fun stopTrackingDeviceOrientation() {
        val observer = orientationObserver ?: return
        NSNotificationCenter.defaultCenter.removeObserver(observer)
        orientationObserver = null
        UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
    }

    private fun updateHeadingOrientation() {
        locationManager.headingOrientation = when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                CLDeviceOrientationPortrait

            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                CLDeviceOrientationPortraitUpsideDown

            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                CLDeviceOrientationLandscapeLeft

            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                CLDeviceOrientationLandscapeRight

            // Face up, face down or not yet known. Held flat - a normal way to
            // use this app - the device says nothing about which way the user is
            // facing, so keep the last upright orientation rather than guessing.
            else -> return
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
