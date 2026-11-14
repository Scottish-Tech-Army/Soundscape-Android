package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.locationprovider.StepSnapshot

/**
 * Decides whether the user is walking right now, from the phone's pedometer.
 *
 * This answers something neither GPS signal can. Speed is the same standing as walking, and
 * StationaryDetector's displacement needs half a minute and 25m to say anything. On the
 * shops-steps recording (Tesco and Waitrose, Pixel 8) the step detector reported each step about
 * 0.7s after it was taken, 4 or more steps in 5s picked out the walking, and inside the shops GPS
 * gave a median 0.73 m/s walking against 0.12 m/s standing - with 23% of standing fixes over
 * UserGeometry.inMotion's 0.2 m/s.
 *
 * Only ever positive evidence. No steps is not evidence of standing still: a wheelchair user takes
 * none, and nor does a phone resting on a pushchair or a guide-dog harness, or one on a phone with
 * no step hardware or no permission to use it. So [isStepping] can end a stationary verdict or
 * start an on-foot one, but its absence decides nothing.
 *
 * Nor is it proof of being on foot. The same recording has a steady stream of steps through over a
 * minute of driving at 5-10 m/s, and more while braking to a stop at 4-5 m/s at the end, so
 * every consumer pairs it with a walking speed rather than trusting it alone.
 *
 * Steps are timed by the pedometer's own clock, not by when the snapshot was taken: the step
 * detector's [StepSnapshot.lastStepMillis] where there is one, otherwise the counter's
 * [StepSnapshot.counterMillis] (iOS, whose CMPedometer has only a counter, delivered in batches
 * some seconds late). Both are wall-clock times, as is the location update time they are compared
 * with.
 */
class StepTracker(
    private val windowMillis: Long = 5_000L,
    private val minimumSteps: Long = 4L,
) {
    private class Event(val timeMillis: Long, val count: Long)

    private val events = ArrayDeque<Event>()

    /** The count before the oldest event still in [events]. */
    private var baselineCount = 0L

    var isStepping: Boolean = false
        private set

    /** Steps counted in the last [windowMillis], as of the last [update]. */
    var recentSteps: Long = 0L
        private set

    /** Call once per location update, with the pedometer's state then (null if there's none). */
    fun update(snapshot: StepSnapshot?, nowMillis: Long): Boolean {
        val (count, time) = when {
            snapshot == null -> null to null
            snapshot.detectedSteps != null -> snapshot.detectedSteps to snapshot.lastStepMillis
            else -> snapshot.counterSteps to snapshot.counterMillis
        }
        if ((count != null) && (time != null)) {
            val latest = events.lastOrNull()?.count ?: baselineCount
            when {
                // The provider was restarted and its count began again from zero
                count < latest -> {
                    events.clear()
                    baselineCount = count
                }
                count > latest -> events.addLast(Event(time, count))
            }
        }
        while (events.isNotEmpty() && (events.first().timeMillis <= nowMillis - windowMillis)) {
            baselineCount = events.removeFirst().count
        }
        recentSteps = (events.lastOrNull()?.count ?: baselineCount) - baselineCount
        isStepping = recentSteps >= minimumSteps
        return isStepping
    }
}
