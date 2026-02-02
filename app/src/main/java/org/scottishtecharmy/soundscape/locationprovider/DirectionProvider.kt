package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import kotlin.math.acos

open class DirectionProvider {

    var audioEngine : NativeAudioEngine? = null

    open fun start(audio: NativeAudioEngine, locProvider: LocationProvider) {
        audioEngine = audio
    }
    open fun destroy() {}

    // Flow to return DeviceOrientation objects
    val mutableOrientationFlow = MutableStateFlow<DeviceDirection?>(null)
    var orientationFlow: StateFlow<DeviceDirection?> = mutableOrientationFlow
}

// Extension function to convert radians to degrees
private fun Float.toDegrees(): Float {
    return (this * 180 / Math.PI).toFloat()
}
fun phoneHeldFlat(deviceOrientation: DeviceDirection?) : Boolean {

    if(deviceOrientation == null) return false

    val attitude = deviceOrientation.attitude

    // Calculate the vector that we're interested in from the attitude quaternion
    val gravityZ = 1 - 2 * (attitude[0] * attitude[0] + attitude[1] * attitude[1])

    // Calculate the angles
    val angleToZ = acos(gravityZ).toDegrees()

    // angleToZ is in the range 0 to 90 when it's held with the screen facing away from the
    // ground. The lower the value the closer to flat it is. When the screen is facing the
    // ground it's between 90 and 180.
    return (angleToZ < 20)
}