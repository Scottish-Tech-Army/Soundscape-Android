package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadphoneCalibration
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadphoneCalibrationManager
import org.scottishtecharmy.soundscape.platform.PlatformLock
import org.scottishtecharmy.soundscape.platform.withLock
import kotlin.concurrent.Volatile

/**
 * What every head tracker shares: turning the device's relative yaw into a geographic heading by
 * calibrating it against the phone's compass and the walking course (see
 * [HeadphoneCalibrationManager]), and recording all of its input so that a walk can be replayed
 * exactly (see [HeadTrackerInputLog] and [HeadTrackerReplay]).
 *
 * A subclass connects to its device, calls [onSessionConnected] and [onSessionEnded] as the
 * connection comes and goes, and passes each sample in through [input], which records it and then
 * acts on it, ending in [processYaw]. Every input - the device's, the phone heading and the
 * location - is recorded and acted on under one lock, so the recording holds them in the order
 * they were used.
 */
abstract class CalibratedHeadTrackingProvider(
    private val directionProvider: DirectionProvider,
    private val locationProvider: LocationProvider,
    /** Names this tracker's input in a recording, so a replay runs it through the right code. */
    val inputId: String,
    /** The device calibrator's window, in samples: about 8-10 seconds at the device's rate. */
    deviceWindowSize: Int,
    /** The time every input is stamped with. A replay passes the recorded times instead. */
    protected val clock: () -> Long,
) : HeadTrackingProvider() {

    protected val calibrationManager = HeadphoneCalibrationManager(deviceWindowSize = deviceWindowSize)

    @Volatile
    private var lastDeviceHeadingDegrees: Double? = null

    @Volatile
    private var lastCourseDegrees: Double? = null

    @Volatile
    private var lastCourseAccuracyDegrees: Double? = null

    @Volatile
    private var lastCourseTimestampMillis: Long = 0L

    // What takeDebugSnapshot reports
    @Volatile
    private var lastPhoneDirection: DeviceDirection? = null

    @Volatile
    private var lastYawDegrees: Double? = null

    @Volatile
    private var lastCourseGivenDegrees: Double? = null

    private val calibrationsLock = PlatformLock()
    private val recentCalibrations = mutableListOf<HeadphoneCalibration>()

    private val inputLock = PlatformLock()
    private val inputLog = HeadTrackerInputLog()
    private var replaying = false

    /** Whether the tracker has been started; a snapshot is only taken while it has. */
    protected abstract val isRunning: Boolean

    init {
        calibrationManager.onCalibration = { calibration ->
            calibrationsLock.withLock {
                // Nothing drains this unless a recording is being made, so keep it bounded
                if (recentCalibrations.size >= MAX_RECENT_CALIBRATIONS) recentCalibrations.removeAt(0)
                recentCalibrations.add(calibration)
            }
        }
    }

    // Under inputLock, so the state matches the input drained with it exactly: a replay of the
    // input up to here ends where this snapshot says it did
    override fun takeDebugSnapshot(includeInput: Boolean): HeadTrackingDebug? = inputLock.withLock {
        if (!isRunning && !replaying) return null
        val phone = lastPhoneDirection
        val input = if (includeInput) inputLog.drain(clock()) else null.also { inputLog.stop() }
        HeadTrackingDebug(
            status = statusFlow.value,
            yawDegrees = lastYawDegrees,
            offsetDegrees = calibrationManager.offsetDegrees,
            headingDegrees = headHeadingFlow.value?.degrees,
            phoneHeadingDegrees = phone?.headingDegrees?.toDouble(),
            phoneFlat = phoneHeldFlat(phone),
            courseDegrees = lastCourseGivenDegrees,
            calibrations = calibrationsLock.withLock {
                recentCalibrations.toList().also { recentCalibrations.clear() }
            },
            device = inputId,
            inputs = if (input != null) mapOf(inputId to input) else emptyMap(),
        )
    }

    /** Follows the phone's heading and the walking course in [scope], for start(). */
    protected fun observeReferences(scope: CoroutineScope) {
        scope.launch { directionProvider.orientationFlow.collect { onDirection(it, clock()) } }
        scope.launch { locationProvider.filteredLocationFlow.collect { onLocation(it, clock()) } }
    }

    /** Forgets the calibration and everything it was given, for stop(). */
    protected fun resetCalibration() = inputLock.withLock {
        calibrationManager.stop()
        lastDeviceHeadingDegrees = null
        lastCourseDegrees = null
        lastCourseAccuracyDegrees = null
        lastCourseTimestampMillis = 0L
        lastPhoneDirection = null
        lastYawDegrees = null
        lastCourseGivenDegrees = null
        calibrationsLock.withLock { recentCalibrations.clear() }
        mutableHeadHeadingFlow.value = null
    }

    /** Records one of the device's own inputs with [record], then acts on it with [act]. */
    protected fun input(record: HeadTrackerInputLog.() -> Unit, act: () -> Unit) =
        inputLock.withLock {
            inputLog.record()
            act()
        }

    internal fun onDirection(dir: DeviceDirection?, nowMillis: Long) = inputLock.withLock {
        inputLog.direction(nowMillis, dir)
        lastPhoneDirection = dir
        // The phone's heading only says where the user faces while it's held flat in front of
        // them. In a pocket it can point anywhere, often straight backwards, and a still
        // phone and head would calibrate to that within seconds, leaving the head heading
        // 180° out. Away from flat, calibration waits for the walking course instead.
        lastDeviceHeadingDegrees =
            if (dir != null && dir.headingAccuracyDegrees >= 0f && phoneHeldFlat(dir)) {
                dir.headingDegrees.toDouble()
            } else {
                null
            }
    }

    internal fun onLocation(loc: SoundscapeLocation?, nowMillis: Long) = inputLock.withLock {
        inputLog.location(nowMillis, loc)
        // Below walking pace a GPS course says little about which way the user is going
        val usable = loc != null &&
            loc.hasBearing &&
            loc.hasSpeed &&
            loc.speed >= COURSE_MIN_SPEED_MPS
        if (usable) {
            lastCourseDegrees = loc.bearing.toDouble()
            lastCourseAccuracyDegrees =
                if (loc.hasBearingAccuracy) loc.bearingAccuracyDegrees.toDouble() else null
            lastCourseTimestampMillis = nowMillis
        } else {
            lastCourseDegrees = null
        }
    }

    /** The device has connected. Calibration starts again, as the yaw's zero will have moved. */
    internal fun onSessionConnected(nowMillis: Long) = inputLock.withLock {
        inputLog.connected(nowMillis)
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Connected
        calibrationManager.stop()
        calibrationManager.start()
        onSessionStarted()
    }

    internal fun onSessionEnded(nowMillis: Long) = inputLock.withLock {
        inputLog.ended(nowMillis)
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected
        onSessionFinished()
    }

    /** For a subclass to reset its own decoding when a connection starts, under the input lock. */
    protected open fun onSessionStarted() {}

    /** For a subclass to clear its own state when a connection ends, under the input lock. */
    protected open fun onSessionFinished() {}

    /** One sample of the device's yaw, in degrees clockwise from wherever its zero is. */
    protected fun processYaw(yawDegrees: Double, nowMillis: Long) {
        lastYawDegrees = yawDegrees
        calibrationManager.pushDeviceReference(yawDegrees, lastDeviceHeadingDegrees, nowMillis)

        val course = lastCourseDegrees
        val courseFresh = course != null &&
            (nowMillis - lastCourseTimestampMillis) <= COURSE_STALENESS_MILLIS
        val courseGiven = if (courseFresh) course else null
        lastCourseGivenDegrees = courseGiven
        calibrationManager.pushCourseReference(
            yawDegrees,
            courseGiven,
            nowMillis,
            courseAccuracyDegrees = if (courseFresh) lastCourseAccuracyDegrees else null,
        )

        val heading = calibrationManager.headingFor(yawDegrees) ?: return
        mutableHeadHeadingFlow.value = HeadHeading(
            degrees = heading,
            accuracyDegrees = REPORTED_ACCURACY_DEGREES,
            timestampMillis = nowMillis,
        )
        if (mutableStatusFlow.value == HeadTrackingStatus.Connected) {
            mutableStatusFlow.value = HeadTrackingStatus.Calibrated
        }
    }

    /** A yaw sample from a device that works out its own yaw, recorded as it is. */
    protected fun onYawSample(yawDegrees: Double, nowMillis: Long) =
        input({ yaw(nowMillis, yawDegrees) }) { processYaw(yawDegrees, nowMillis) }

    // Replay. A provider that is never started is driven straight from a recording.

    internal fun startReplay() = inputLock.withLock {
        replaying = true
        calibrationManager.start()
        onSessionStarted()
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected
    }

    /**
     * Acts on one recorded event. Returns true if it was one of the device's samples, after which
     * a replay reports the tracker's state.
     */
    internal fun replay(fields: List<String>): Boolean {
        val t = fields[1].toLong()
        when (fields[0]) {
            "S" -> onSessionConnected(t)
            "E" -> onSessionEnded(t)
            "P" -> onDirection(HeadTrackerInputLog.parseDirection(fields), t)
            "L" -> onLocation(HeadTrackerInputLog.parseLocation(fields), t)
            "Y" -> {
                onYawSample(fields[2].toDouble(), t)
                return true
            }
            else -> return replayDeviceInput(fields, t)
        }
        return false
    }

    /** Acts on one of the device's own recorded events, returning true if it was a sample. */
    protected open fun replayDeviceInput(fields: List<String>, t: Long): Boolean = false

    internal val replayYawDegrees: Double? get() = lastYawDegrees
    internal val replayOffsetDegrees: Double? get() = calibrationManager.offsetDegrees

    private companion object {
        const val COURSE_MIN_SPEED_MPS = 0.4f
        const val COURSE_STALENESS_MILLIS = 3_000L
        const val REPORTED_ACCURACY_DEGREES = 10.0
        const val MAX_RECENT_CALIBRATIONS = 20
    }
}
