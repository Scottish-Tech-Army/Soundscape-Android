package org.scottishtecharmy.soundscape.preferences

class Preferences(
    private val preferencesProvider: PreferencesProvider
) {
    var speechRate: Float = SPEECH_RATE_DEFAULT
        get() = preferencesProvider.getFloat(SPEECH_RATE_KEY, SPEECH_RATE_DEFAULT)
        set(value) {
            field = value
            preferencesProvider.putFloat(SPEECH_RATE_KEY, value)
        }


    companion object {
        private const val SPEECH_RATE_KEY = "SpeechRate"
        private const val SPEECH_RATE_DEFAULT = 1.0f
    }
}