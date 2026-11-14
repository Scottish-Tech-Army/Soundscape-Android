package org.scottishtecharmy.soundscape.locationprovider

/**
 * What the phone's pedometer has reported so far, recorded with each fix so that a recording can
 * show whether step data could tell standing from walking better than GPS speed does (see
 * UserGeometry.inMotion and StationaryDetector). Recording only for now: nothing in the geoengine
 * reads it.
 *
 * The platforms report steps differently, so both kinds are kept and left null where a platform
 * has no equivalent:
 *
 *  - [detectedSteps] / [lastStepMillis]: one event per step as the platform recognises it
 *    (Android's TYPE_STEP_DETECTOR). Fast, but a detector may miss steps that the counter later
 *    finds.
 *  - [counterSteps] / [counterMillis]: the platform's own running count (Android's
 *    TYPE_STEP_COUNTER, iOS's CMPedometer), as of [counterMillis]. More accurate, but delivered in
 *    batches, so [counterMillis] can lag the fix by several seconds.
 *  - [cadence]: steps per second, from CMPedometer only.
 *
 * Both counts run from when the provider started, not from boot.
 */
data class StepSnapshot(
    val detectedSteps: Long? = null,
    val lastStepMillis: Long? = null,
    val counterSteps: Long? = null,
    val counterMillis: Long? = null,
    val cadence: Double? = null,
)

interface StepProvider {
    /**
     * The pedometer's state now, or null if it has nothing to report: no step hardware, no
     * permission, or no data yet. Starts the pedometer on first call, so that only somebody
     * recording travel is ever asked for the permission it needs.
     */
    fun snapshot(): StepSnapshot?

    fun stop()
}
