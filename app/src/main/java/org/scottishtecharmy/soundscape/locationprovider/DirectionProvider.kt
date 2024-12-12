package org.scottishtecharmy.soundscape.locationprovider

import com.google.android.gms.location.DeviceOrientation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine

open class DirectionProvider {

    var audioEngine : NativeAudioEngine? = null

    open fun start(audio: NativeAudioEngine, locProvider: LocationProvider) {
        audioEngine = audio
    }
    open fun destroy() {}

    fun getCurrentDirection() : Float {
        var heading = 0.0F
        mutableOrientationFlow.value?.let{ heading = it.headingDegrees }
        return heading
    }

    // Flow to return DeviceOrientation objects
    val mutableOrientationFlow = MutableStateFlow<DeviceOrientation?>(null)
    var orientationFlow: StateFlow<DeviceOrientation?> = mutableOrientationFlow
}