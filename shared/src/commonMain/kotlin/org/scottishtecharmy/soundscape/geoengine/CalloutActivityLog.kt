package org.scottishtecharmy.soundscape.geoengine

import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

/**
 * Support-log lines that say why the automatic callouts did or didn't speak, for a user who
 * reports that Soundscape went quiet. The log never records the screen state or (on its own) the
 * reason a fix produced no callout, so without these a silent stretch could be a user standing
 * still, the audio engine being busy, the audio menu being open, or the callout settings having
 * nothing to say - all of which look the same.
 *
 * Four kinds of line, all cheap enough for the support buffer:
 * - a warning when no fix at all has arrived for [fixGapMillis] (see [checkForGap]), and how long
 *   the gap lasted once one does - without it a stretch with no fixes leaves nothing in the log,
 *   not even a summary, since summaries are written as fixes arrive;
 * - the stationary detector's first verdict of each run, once it has seen enough fixes to give one
 *   (StationaryDetector answers "not stationary" until its window fills, so the first fix says
 *   nothing either way);
 * - a transition whenever the user goes stationary or starts moving, or gets into or out of a
 *   vehicle;
 * - once per [periodMillis], if any fixes arrived, a summary of what happened to them: the
 *   median accuracy of the fixes (the stationary detector ignores any worse than 25m, so indoors
 *   it may never decide), and how many came while the detector was still undecided.
 *
 * Fed by GeoEngine's location loop, one call per fix. [log] is where the lines go and
 * [describeSettings] adds the callout settings in force to each summary.
 */
class CalloutActivityLog(
    private val log: (String) -> Unit,
    private val describeSettings: () -> String = { "" },
    private val periodMillis: Long = 60_000L,
    private val warn: (String) -> Unit = log,
    private val fixGapMillis: Long = 30_000L,
) {
    // The fix-gap check runs on its own timer, so these are shared with the location loop
    @Volatile private var gapSinceMillis: Long? = null
    @Volatile private var anyFix = false
    @Volatile private var gapReported = false

    private var periodStartMillis: Long? = null
    private var fixes = 0
    private var unusableFixes = 0
    private var stationaryFixes = 0
    private var undecidedFixes = 0
    private val accuracies = ArrayList<Double>()
    private var vehicleFixes = 0
    private var audioBusy = 0
    private var menuOpen = 0
    private var callouts = 0

    private var lastStationary: Boolean? = null
    private var lastInVehicle: Boolean? = null
    private var firstFixMillis: Long? = null

    /**
     * Starts again for a new run of location updates at [nowMillis]: the next verdict is the first
     * again, and the fix-gap check counts from here.
     */
    fun reset(nowMillis: Long) {
        gapSinceMillis = nowMillis
        anyFix = false
        gapReported = false
        periodStartMillis = null
        lastStationary = null
        lastInVehicle = null
        firstFixMillis = null
        clearCounts()
    }

    /** A fix too inaccurate to use, so nothing downstream ran on it. */
    fun unusableFix(nowMillis: Long, accuracyMetres: Double? = null) {
        begin(nowMillis)
        fixArrived(nowMillis)
        unusableFixes++
        accuracyMetres?.let { accuracies.add(it) }
    }

    /**
     * A usable fix, with the detectors' verdicts for it. [stationaryDecided] is false while the
     * stationary detector is still filling its window, when [stationary] means nothing yet.
     * [accuracyMetres] is the fix's reported accuracy, if it has one.
     */
    fun fix(
        nowMillis: Long,
        stationary: Boolean,
        inVehicle: Boolean,
        stationaryDecided: Boolean = true,
        accuracyMetres: Double? = null,
    ) {
        begin(nowMillis)
        fixArrived(nowMillis)
        fixes++
        if (stationary) stationaryFixes++
        if (!stationaryDecided) undecidedFixes++
        accuracyMetres?.let { accuracies.add(it) }
        if (inVehicle) vehicleFixes++
        val firstFix = firstFixMillis ?: nowMillis.also { firstFixMillis = it }
        if (lastStationary == null) {
            if (stationaryDecided) {
                log(
                    "User is ${if (stationary) "stationary" else "moving"}" +
                        (if (inVehicle) ", in a vehicle" else "") +
                        " (first verdict, ${(nowMillis - firstFix) / 1000}s after the first fix)"
                )
                lastStationary = stationary
            }
        } else if (lastStationary != stationary) {
            log(if (stationary) "User is now stationary" else "User is moving again")
            lastStationary = stationary
        }
        if (lastInVehicle != null && lastInVehicle != inVehicle) {
            log(if (inVehicle) "User is now in a vehicle" else "User is no longer in a vehicle")
        }
        lastInVehicle = inVehicle
    }

    /**
     * Called every few seconds while location updates should be arriving: warns, once, when none
     * has for [fixGapMillis] - or none at all since [reset].
     */
    fun checkForGap(nowMillis: Long) {
        val since = gapSinceMillis ?: return
        if (gapReported || (nowMillis - since < fixGapMillis)) return
        gapReported = true
        val seconds = (nowMillis - since) / 1000
        warn(
            if (anyFix) "No location fix for ${seconds}s"
            else "No location fix in the ${seconds}s since location tracking started"
        )
    }

    /**
     * Restarts the fix-gap check from [nowMillis] without counting it as a fix: for while fixes
     * aren't expected, such as Street Preview, which only moves when the user does.
     */
    fun holdGapCheck(nowMillis: Long) {
        gapSinceMillis = nowMillis
        gapReported = false
    }

    private fun fixArrived(nowMillis: Long) {
        val since = gapSinceMillis
        if (gapReported && since != null) {
            log("Location fixes resumed after ${(nowMillis - since) / 1000}s without one")
        }
        gapSinceMillis = nowMillis
        anyFix = true
        gapReported = false
    }

    /** The callouts didn't run on this fix: the audio engine was busy, or the audio menu open. */
    fun skipped(audioEngineBusy: Boolean) {
        if (audioEngineBusy) audioBusy++ else menuOpen++
    }

    /** The callouts ran on this fix and produced something to say. */
    fun callout() {
        callouts++
    }

    private fun begin(nowMillis: Long) {
        val start = periodStartMillis
        if (start == null) {
            periodStartMillis = nowMillis
            return
        }
        if (nowMillis - start < periodMillis) return
        summarise(nowMillis - start)
        periodStartMillis = nowMillis
    }

    private fun summarise(elapsedMillis: Long) {
        if (fixes + unusableFixes > 0) {
            val settings = describeSettings()
            val accuracy = if (accuracies.isEmpty()) "no accuracy reported" else
                "accuracy median ${median(accuracies).roundToInt()}m"
            log(
                "Callouts in last ${elapsedMillis / 1000}s: $fixes fixes" +
                    (if (unusableFixes > 0) " (+$unusableFixes unusable)" else "") +
                    ", $accuracy, stationary $stationaryFixes" +
                    (if (undecidedFixes > 0) " (undecided $undecidedFixes)" else "") +
                    ", in vehicle $vehicleFixes" +
                    ", audio busy $audioBusy, menu open $menuOpen, produced $callouts" +
                    (if (settings.isNotEmpty()) " [$settings]" else "")
            )
        }
        clearCounts()
    }

    private fun clearCounts() {
        fixes = 0
        unusableFixes = 0
        stationaryFixes = 0
        undecidedFixes = 0
        accuracies.clear()
        vehicleFixes = 0
        audioBusy = 0
        menuOpen = 0
        callouts = 0
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }
}
