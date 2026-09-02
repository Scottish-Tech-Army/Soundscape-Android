package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class Accuracy {
    abstract val updateInterval: Duration
    abstract val minimumDistanceM: Float

    object High : Accuracy() {
        override val updateInterval: Duration = 1.seconds
        override val minimumDistanceM: Float = 1.0f

    }

    object Balanced : Accuracy() {
        override val updateInterval: Duration = 30.seconds
        override val minimumDistanceM: Float = 5.0f
    }
}

/**
 * The worst horizontal accuracy a fix can report and still be acted on by the geoengine.
 *
 * Deep cuttings and tunnels don't stop fixes arriving, they just make them wrong: recordings from
 * the Argyle and North Clyde Lines through central Glasgow have runs of fixes reporting 200-700m
 * accuracy which land hundreds of metres off the line, on whatever streets happen to be overhead.
 * The geoengine has no notion of an uncertain position - a fix is map matched, called out and left
 * behind at face value - so a fix that can't say which street the user is on is worse than no fix
 * at all, and holding the previous good one is the better answer.
 *
 * 50m is chosen to sit above the accuracy a phone reports on a normal street (typically 5-25m in
 * these recordings, occasionally drifting to 40m under trees or between tall buildings) and below
 * the 96m-and-worse fixes that turned out to be genuinely wayward.
 *
 * The test is applied by GeoEngine rather than by the providers, so that everything reading the
 * raw flows - the GPX recorder above all - still sees exactly what the receiver reported.
 */
const val MAXIMUM_USABLE_ACCURACY_METRES = 50.0f

/**
 * Whether a fix is accurate enough to act on - see [MAXIMUM_USABLE_ACCURACY_METRES].
 *
 * A fix with no accuracy at all passes: that's a synthesized/debug location rather than a bad one,
 * and the alternative would be to discard every location in a replay or a street preview.
 *
 * The caller is responsible for the other half of the rule - a fix that fails this test is still
 * better than having no position at all, so the first one has to be accepted regardless. The first
 * fix after a cold start regularly comes from the network provider at 100m or worse, with the
 * accurate ones following rather than preceding it.
 */
fun isAccuracyUsable(location: SoundscapeLocation): Boolean =
    !location.hasAccuracy || (location.accuracy <= MAXIMUM_USABLE_ACCURACY_METRES)

abstract class LocationProvider {
    abstract fun start(accuracy: Accuracy = Accuracy.High)
    abstract fun destroy()
    open fun updateLocation(newLocation: SoundscapeLocation) {}

    fun hasValidLocation(): Boolean {
        return mutableLocationFlow.value != null
    }

    val mutableLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var locationFlow: StateFlow<SoundscapeLocation?> = mutableLocationFlow

    val mutableFilteredLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var filteredLocationFlow: StateFlow<SoundscapeLocation?> = mutableFilteredLocationFlow
}
