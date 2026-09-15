package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Decides whether the user is standing still, by asking whether they have actually gone anywhere
 * rather than how fast the GPS says they are going.
 *
 * Speed cannot answer this. Measured over four recorded journeys, a stationary user's median GPS
 * speed across a minute is 1.33 m/s against 1.39 m/s for someone walking - the best threshold that
 * exists still misreads 22% of stationary windows and 33% of walking ones, and UserGeometry's old
 * `speed > 0.2` test was true for *every* stationary window measured. That is how standing on the
 * concourse at Glasgow Queen Street came to be read as walking about.
 *
 * Net displacement over the same minute separates the two outright: stationary p90 is 19.4m,
 * walking p10 is 66.4m, and [stationaryMetres] sits in the gap with no measured error either way.
 * It is also the more robust signal when the GPS is struggling - one 341s spell on the Milngavie
 * recording reported a speed of ~0 throughout while the train actually covered two kilometres.
 *
 * The question this answers is deliberately "has the user gone anywhere", not "is the vehicle
 * moving". A passenger sat on a train stopped at a station and a passenger who has stepped off and
 * walked down the platform look identical on speed and completely different on displacement, and
 * telling those two apart is the whole point - see RailMatchArbiter, which used to end the ride at
 * every station because it had no way to.
 *
 * Reports false until the window has filled. Every consumer treats "not stationary" as "carry on
 * as before", so an undecided window is automatically a no-op - which is why this is a Boolean and
 * not a three-state answer with an UNKNOWN that every caller would map onto MOVING anyway.
 */
class StationaryDetector {

    private data class Sample(
        val timestampMilliseconds: Long,
        val location: LngLatAlt,
        val courseIsTrustworthy: Boolean,
    )

    /**
     * How long a stretch of travel the verdict is based on. The separation between standing and
     * walking was measured over a minute and no shorter window has been validated, so this is the
     * window the numbers below belong to.
     *
     * It is also [org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout]'s sticky
     * vehicle/train window, which is the neat part: a spell of stillness too short to be detected
     * here is also too short to outlive the window it exists to protect.
     */
    private val windowMillis = 60_000L

    /**
     * How far the user may have got from where they were a [windowMillis] ago and still count as
     * not having gone anywhere. The measured optimum: stationary windows reach 19.4m at the 90th
     * percentile, walking ones start at 66.4m, and nothing measured sits between.
     */
    private val stationaryMetres = 20.0

    /**
     * How far they have to get before we accept they are moving again, and separately how far they
     * may wander within the window while still counting as standing. Above [stationaryMetres] so
     * the verdict doesn't chatter for a window sitting on the boundary, and far enough below
     * walking's 66.4m that it cannot mistake a walk for a wander.
     *
     * As an excursion limit it stops an out-and-back - forty metres to a departure board and
     * forty back inside the same minute - reading as standing still, since net displacement alone
     * cannot see it. Measured against the recordings it never fires on a genuinely stationary
     * window: labelling those by displacement alone, the furthest any of them strays from the
     * anchor is 23.6m.
     */
    private val movingMetres = 30.0

    /**
     * How good a fix has to be to say anything about a 20m question. A deliberately stricter gate
     * than [org.scottishtecharmy.soundscape.locationprovider.MAXIMUM_USABLE_ACCURACY_METRES],
     * which asks whether a fix can say which street the user is on; a 50m fix answers that and
     * tells us nothing at all about whether they have moved twenty metres. This is the bound the
     * distributions quoted above were measured under, so loosening it invalidates them.
     *
     * A fix carrying no accuracy at all is admitted, for the same reason isAccuracyUsable admits
     * one: that is a synthesized location - Street Preview, a hand-written GPX - rather than a bad
     * one, and rejecting them would mean such a replay never produced a verdict.
     */
    private val usableAccuracyMetres = 25.0

    /**
     * A gap in the record. Two fixes this far apart say nothing about how the user got from one to
     * the other, so the window starts again rather than treating the pair as evidence that nobody
     * moved. Same value and the same reasoning as AutoCallout's sweepMaximumGapMilliseconds.
     *
     * Because the gap is measured between *admitted* fixes, a spell of fixes too poor to use ends
     * the window by itself - a stretch of 30-50m fixes and a stretch of no fixes at all are the
     * same thing as far as this is concerned.
     */
    private val maximumFixGapMillis = 60_000L

    /**
     * How many fixes a full window has to hold before it is allowed to vote. Two fixes a minute
     * apart can be a minute of standing still or a minute of walking out and back, and there is no
     * way to tell which from the pair alone.
     */
    private val minimumSamples = 5

    /**
     * The trailing run of fixes the fast escape looks at, and how many of them have to carry a
     * trustworthy GPS course for it to fire. See [update] - "at least two of the last six" was
     * measured to have no false escapes at all while catching 81% of walking within six seconds.
     */
    private val courseSamples = 6
    private val courseEscapeCount = 2

    /** Bounds the deque if fixes ever arrive far faster than the once a second we expect. */
    private val maximumSamples = 600

    private val samples = ArrayDeque<Sample>()
    private var ruler = CheapRuler(0.0)

    var isStationary: Boolean = false
        private set

    /**
     * How long the user has been *seen* to be standing still, in total. The counterpart of
     * UserGeometry.unobservedMillis, and consumed the same way: a consumer measuring elapsed time
     * can subtract this to stop a window running out while nothing was happening.
     *
     * A running total rather than a per-update delta because not every location update reaches
     * every consumer - AutoCallout.updateLocation is skipped while the audio engine is busy - so
     * consumers track the growth themselves.
     *
     * Only observed stillness counts. Time spent blind is unobservedMillis' business, and since a
     * fix that fails the accuracy gate never reaches the deque at all, the two cannot both be
     * counting the same seconds.
     */
    var stationaryMillis: Long = 0L
        private set

    /** Net displacement across the window, or null before it has filled. For tests and logging. */
    var displacementMetres: Double? = null
        private set

    fun reset() {
        samples.clear()
        isStationary = false
        displacementMetres = null
    }

    /**
     * Call once per location update the geoengine has accepted, before anything that reads the
     * verdict. Returns [isStationary].
     *
     * [courseIsTrustworthy] must be derived from the *GPS course* - the direction of travel
     * Android reports alongside the fix, gated on its own bearing accuracy - and never from the
     * phone's compass or the head tracker. Someone standing still holding the phone up to read the
     * screen produces a perfectly steady compass heading while going nowhere, which would defeat
     * the whole test. The caller is responsible for that distinction; see GeoEngine, which builds
     * it from the same fields it uses for travelHeading.
     */
    fun update(
        location: LngLatAlt,
        accuracyMetres: Double?,
        courseIsTrustworthy: Boolean,
        timestampMilliseconds: Long,
    ): Boolean {
        // Too poor a fix to say anything about twenty metres, so it isn't evidence either way and
        // doesn't advance anything - not the deque, not the clock.
        if ((accuracyMetres != null) && (accuracyMetres > usableAccuracyMetres)) return isStationary

        val newest = samples.lastOrNull()
        if (newest != null) {
            // Replayed or synthesized fixes can arrive out of order; a step backwards in time
            // would corrupt both the window length and the stationary total.
            if (timestampMilliseconds <= newest.timestampMilliseconds) return isStationary

            val step = timestampMilliseconds - newest.timestampMilliseconds
            if (step > maximumFixGapMillis) {
                reset()
            } else if (isStationary) {
                stationaryMillis += step
            }
        }

        if (ruler.needsReplacing(location.latitude)) ruler = CheapRuler(location.latitude)

        samples.addLast(Sample(timestampMilliseconds, location, courseIsTrustworthy))
        while (samples.size > maximumSamples) samples.removeFirst()
        // Keep exactly one sample at or beyond the window's far edge, so the oldest is a true
        // window-length anchor rather than whatever happens to be left in the deque.
        while ((samples.size >= 2) &&
            ((timestampMilliseconds - samples[1].timestampMilliseconds) >= windowMillis)
        ) {
            samples.removeFirst()
        }

        isStationary = decide()
        return isStationary
    }

    private fun decide(): Boolean {
        // Android's own opinion of its course is a far faster answer than displacement can be: of
        // the fixes recorded while genuinely standing still, 2% carried a course it rated accurate
        // to better than 45 degrees, against 80% of those recorded while walking. So a short run of
        // them is strong evidence the user has started moving, and gets us out of a stationary
        // spell in about six seconds rather than the twenty-odd it takes to walk thirty metres.
        //
        // Only ever an escape, never an entry: it can end a stationary spell but cannot start one,
        // so the worst a wrong answer here can do is behave the way the app did before any of this.
        if (isStationary) {
            val trustworthy = samples.takeLast(courseSamples).count { it.courseIsTrustworthy }
            if (trustworthy >= courseEscapeCount) {
                displacementMetres = null
                return false
            }
        }

        val anchor = samples.firstOrNull() ?: return false
        val newest = samples.last()
        val windowReady =
            ((newest.timestampMilliseconds - anchor.timestampMilliseconds) >= windowMillis) &&
                (samples.size >= minimumSamples)
        if (!windowReady) {
            displacementMetres = null
            return false
        }

        val displacement = ruler.distance(anchor.location, newest.location)
        displacementMetres = displacement
        val excursion = samples.maxOf { ruler.distance(anchor.location, it.location) }
        if (excursion > movingMetres) return false

        // Hysteresis: harder to be believed moving again than it was to be believed still, so a
        // window sitting on the boundary doesn't flip back and forth.
        return if (isStationary) {
            displacement <= movingMetres
        } else {
            displacement <= stationaryMetres
        }
    }
}
