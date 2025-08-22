package org.scottishtecharmy.soundscape.screens.onboarding.hearing

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import javax.inject.Inject

@HiltViewModel
class HearingViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    override fun onCleared() {
        super.onCleared()
        audioEngine.destroy()
    }

    fun playSpeech(speechText: String) {
        // Set our listener position, and play the speech
        audioEngine.clearTextToSpeechQueue()
        audioEngine.createTextToSpeech(speechText, AudioType.LOCALIZED)
    }

    fun silenceSpeech() {
        audioEngine.clearTextToSpeechQueue()
    }
}