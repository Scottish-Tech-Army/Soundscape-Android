package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry

/**
 * Decides whether somebody who has stopped travelling by vehicle has got out and started walking,
 * so that the walking callouts don't have to wait out AutoCallout's whole vehicle sticky window.
 *
 * Neither of the obvious signals can tell. GPS speed is the same standing as walking (see
 * StationaryDetector), and distance covered is no better: a bus crawling in a queue moves 15-17m
 * every ten seconds, which is walking pace, while walking only gets clear of standing-still GPS
 * drift after 20s or so.
 *
 * What does separate them is how unbroken the GPS course is. Somebody walking has a course Android
 * rates as trustworthy on nearly every fix, while a vehicle has one in bursts: as it brakes, as it
 * pulls away, and on and off in a queue. So this asks for an unbroken run at walking speed, some
 * time after the vehicle was last at speed.
 *
 * The thresholds were swept against the recordings in app/src/test/res. They gave no false verdicts
 * in any of the 274 spells where a journey carried on after dropping below vehicle speed, and on
 * the walk-only recordings, iPhone included, they were satisfied within 20s 87-97% of the time,
 * typically in 5s. The detector's own "two of the last six" is far too eager for this: it fired
 * wrongly in 19 of the 49 spells it was first tried on.
 *
 * Only ever an early release. Failing to spot a walk costs the rest of the sticky window, which is
 * how every walk used to behave, so a phone whose fixes are too sparse to satisfy this just gets
 * the longer wait.
 */
class OnFootDetector {

    private class Sample(val timestampMilliseconds: Long, val speed: Double, val course: Boolean)

    /**
     * Fixes this soon after the vehicle was last at speed aren't evidence: they are it braking,
     * when the course is at its best. Filling [minimumCourseFixes] from here takes another 5s at
     * a fix a second, which makes 20s the soonest a verdict can come.
     */
    private val graceMillis = 15_000L

    private val windowMillis = 6_000L

    /** The window has to really span most of its length, not be a burst of fixes. */
    private val minimumWindowSpanMillis = 3_600L

    private val minimumCourseFixes = 6

    private val minimumCourseFraction = 0.8

    /** Faster than this is a vehicle pulling away, which has a good course at 3-5 m/s. */
    private val maximumWalkingSpeedMps = 2.5

    private val samples = ArrayDeque<Sample>()
    private var lastVehicleMillis: Long? = null

    /**
     * Every fix's speed over the last [stepSpeedWindowMillis], for the steps verdict. Kept apart
     * from [samples], which only start filling once the grace period is over.
     */
    private val recentSpeeds = ArrayDeque<Pair<Long, Double>>()

    /**
     * How long the speed must have stayed at walking pace for steps to count. The same as
     * StepTracker's window, so the steps and the speed describe the same few seconds.
     */
    private val stepSpeedWindowMillis = 5_000L

    /** Latched until the next fix at vehicle speed, so a break in the course can't flip it back. */
    var isOnFoot: Boolean = false
        private set

    /**
     * Call once per location update the geoengine has accepted. [courseIsTrustworthy] means the
     * same as it does for StationaryDetector.update, and has the same caveats.
     */
    fun update(
        speed: Double,
        courseIsTrustworthy: Boolean,
        timestampMilliseconds: Long,
        stepping: Boolean = false,
    ): Boolean {
        recentSpeeds.addLast(timestampMilliseconds to speed)
        while (timestampMilliseconds - recentSpeeds.first().first > stepSpeedWindowMillis) {
            recentSpeeds.removeFirst()
        }
        if (speed > UserGeometry.VEHICLE_SPEED_THRESHOLD_MPS) {
            lastVehicleMillis = timestampMilliseconds
            samples.clear()
            isOnFoot = false
            return false
        }
        val lastVehicle = lastVehicleMillis ?: return false
        if (isOnFoot) return true

        // Steps at walking speed, which needs no grace period: braking gives the course a burst
        // of accuracy, but it doesn't make the pedometer count. Walking speed because steps alone
        // aren't enough - the pedometer counts on rough roads and while braking (see StepTracker).
        // On shops-steps this calls the walk 28s sooner at Tesco and 14s sooner at Waitrose. At
        // the end of the journey it changes nothing: the car was still doing 2.6 m/s within the
        // last few seconds of the first steps, and the course-based test below got there first.
        if (stepping && recentSpeeds.all { it.second <= maximumWalkingSpeedMps }) {
            isOnFoot = true
            return true
        }
        if (timestampMilliseconds - lastVehicle < graceMillis) return false

        samples.addLast(Sample(timestampMilliseconds, speed, courseIsTrustworthy))
        while (timestampMilliseconds - samples.first().timestampMilliseconds > windowMillis) {
            samples.removeFirst()
        }

        val span = timestampMilliseconds - samples.first().timestampMilliseconds
        val courseFixes = samples.count { it.course }
        isOnFoot = (span >= minimumWindowSpanMillis) &&
            (courseFixes >= minimumCourseFixes) &&
            (courseFixes >= samples.size * minimumCourseFraction) &&
            samples.all { it.speed <= maximumWalkingSpeedMps }
        return isOnFoot
    }
}
