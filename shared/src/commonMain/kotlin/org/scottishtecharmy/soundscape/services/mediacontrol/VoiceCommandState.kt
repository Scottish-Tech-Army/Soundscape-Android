package org.scottishtecharmy.soundscape.services.mediacontrol

sealed class VoiceCommandState {
    object Idle : VoiceCommandState()
    object Listening : VoiceCommandState()
    object Error : VoiceCommandState()
}
