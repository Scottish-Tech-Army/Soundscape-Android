package org.scottishtecharmy.soundscape.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import javax.inject.Inject

@HiltViewModel
class HearingViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    fun playSpeech(speechText: String) {
        // Set our listener position, and play the speech
        // TODO: If updateGeometry isn't called, then the audioEngine doesn't move on to   the next
        //  queued text to speech. That resulted in the Listen button only working one time.
        //  Calling updateGeometry (which in the service is called every 30ms) sorts this out.
        //  We should consider another way of doing this.
        audioEngine.updateGeometry(0.0, 0.0,0.0)
        audioEngine.createTextToSpeech(0.0,0.0, speechText)
    }
}