package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import platform.CoreMotion.CMAuthorizationStatusDenied
import platform.CoreMotion.CMAuthorizationStatusRestricted
import platform.CoreMotion.CMDeviceMotion
import platform.CoreMotion.CMHeadphoneMotionManager
import platform.CoreMotion.CMHeadphoneMotionManagerDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject
import kotlin.math.PI

private const val RAD_TO_DEG = 180.0 / PI

/**
 * AirPods head tracking. CoreMotion gives the headphones' attitude, whose yaw
 * [CalibratedHeadTrackingProvider] turns into a geographic heading. The yaw is what's recorded
 * for a replay, as CoreMotion can't run off the device.
 */
@OptIn(ExperimentalForeignApi::class)
class IosHeadTrackingProvider(
    directionProvider: DirectionProvider,
    locationProvider: LocationProvider,
) : CalibratedHeadTrackingProvider(
    directionProvider,
    locationProvider,
    inputId = AIRPODS_INPUT_ID,
    deviceWindowSize = AIRPODS_DEVICE_WINDOW_SIZE,
    clock = ::currentTimeMillis,
) {

    private val motionManager: CMHeadphoneMotionManager? =
        if (isHeadphoneMotionSupported()) CMHeadphoneMotionManager() else null

    private val motionQueue = NSOperationQueue().apply {
        name = "HeadphoneMotionUpdatesQueue"
        maxConcurrentOperationCount = 1L
    }

    private val delegate = HeadphoneDelegate(this)

    private var scope: CoroutineScope? = null

    // CoreMotion doesn't say which headphones these are, and they're nearly always AirPods.
    // iOS gives apps no AirPods battery level, so the connection is announced without one.
    override val deviceName = "AirPods"

    override val isRunning: Boolean get() = scope != null

    init {
        if (motionManager == null) {
            mutableStatusFlow.value = HeadTrackingStatus.Unavailable
        }
    }

    override fun start() {
        val mm = motionManager ?: return
        if (mutableStatusFlow.value == HeadTrackingStatus.Unavailable) return

        if (!isAuthorizedOrUndetermined()) {
            mutableStatusFlow.value = HeadTrackingStatus.Unavailable
            return
        }

        if (!mm.isDeviceMotionAvailable()) {
            mutableStatusFlow.value = HeadTrackingStatus.Unavailable
            return
        }

        if (mm.isDeviceMotionActive()) return

        mm.delegate = delegate
        calibrationManager.start()
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected

        mm.startDeviceMotionUpdatesToQueue(motionQueue) { motion: CMDeviceMotion?, _: NSError? ->
            if (motion != null) onDeviceMotion(motion)
        }

        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope
        observeReferences(newScope)
    }

    override fun stop() {
        motionManager?.let {
            if (it.isDeviceMotionActive()) it.stopDeviceMotionUpdates()
            it.delegate = null
        }
        scope?.cancel()
        scope = null
        resetCalibration()
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    private fun onDeviceMotion(motion: CMDeviceMotion) {
        val rawYawRadians = motion.attitude.yaw
        // 180° flip so yaw increases clockwise from the AirPods origin (matches iOS reference).
        // This convention is load-bearing — flipping it requires inverting the offset sign.
        onYawSample(180.0 - rawYawRadians * RAD_TO_DEG, clock())
    }

    internal fun onHeadphonesConnected() {
        if (isTracking()) onSessionConnected(clock())
    }

    internal fun onHeadphonesDisconnected() {
        if (isTracking()) onSessionEnded(clock())
    }

    private fun isTracking() = mutableStatusFlow.value != HeadTrackingStatus.Inactive &&
        mutableStatusFlow.value != HeadTrackingStatus.Unavailable

    private fun isAuthorizedOrUndetermined(): Boolean {
        val status = CMHeadphoneMotionManager.authorizationStatus()
        return status != CMAuthorizationStatusDenied && status != CMAuthorizationStatusRestricted
    }

    companion object {
        fun isHeadphoneMotionSupported(): Boolean {
            // CMHeadphoneMotionManager requires iOS 14.0+; AirPods Max landed in 14.2/14.3,
            // so the iOS reference gates on 14.4. Match that.
            val version = NSProcessInfo.processInfo.operatingSystemVersion
            return version.useContents {
                majorVersion > 14L || (majorVersion == 14L && minorVersion >= 4L)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class HeadphoneDelegate(
    private val provider: IosHeadTrackingProvider,
) : NSObject(), CMHeadphoneMotionManagerDelegateProtocol {

    override fun headphoneMotionManagerDidConnect(manager: CMHeadphoneMotionManager) {
        provider.onHeadphonesConnected()
    }

    override fun headphoneMotionManagerDidDisconnect(manager: CMHeadphoneMotionManager) {
        provider.onHeadphonesDisconnected()
    }
}
