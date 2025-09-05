package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.util.Collections
import java.util.Locale

class TtsEngine(val audioEngine: NativeAudioEngine,
                val engineLabelAndName: String?) :
    TextToSpeech.OnInitListener {

    private var ttsSockets = Collections.synchronizedMap(HashMap<String, Array<ParcelFileDescriptor>>())
    private var currentUtteranceId: String? = null
    private var textToSpeechInitialized : Boolean = false
    private var utteranceIncrementingCount : Int = 0

    private lateinit var textToSpeech: TextToSpeech
    private var textToSpeechVoiceType = MainActivity.VOICE_TYPE_DEFAULT
    private var textToSpeechRate = 0.1f

    private var sharedPreferences : SharedPreferences? = null
    private lateinit var sharedPreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener

    fun getCurrentLabelAndName() : String? { return engineLabelAndName }

    fun destroy() {
        Log.d(TAG, "Destroy $engineLabelAndName TTS engine")
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)

        stop()
        textToSpeech.setOnUtteranceProgressListener(null)
        textToSpeech.shutdown()
    }

    fun initialize(context : Context, followPreferences : Boolean = true)
    {
        val configLocale = getCurrentLocale()
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(configLocale)

        audioEngine.ttsRunningStateChanged(false)

        if(followPreferences) {
            val configLocale = getCurrentLocale()
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(configLocale)
            val localizedContext = context.createConfigurationContext(configuration)

            // Listen for changes to shared preference settings so that we can update the audio engine
            // configuration.
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferencesListener =
                SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                    if (sharedPreferences == preferences) {
                        if ((key == MainActivity.VOICE_TYPE_KEY) ||
                            (key == MainActivity.SPEECH_RATE_KEY)
                        ) {
                            if (updateSpeech(preferences)) {
                                audioEngine.updateSpeech(localizedContext)
                            }
                        }
                    }
                }
            sharedPreferences?.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        }

        Log.d(TAG, "Open TTS engine: $engineLabelAndName")
        textToSpeech = if (engineLabelAndName.isNullOrEmpty())
            TextToSpeech(context, this)
        else {
            val bundle = Bundle().apply {
                putString("engine", engineLabelAndName)
                putString("voice", textToSpeechVoiceType)
            }
            // Log an event so that we can get statistics
            Firebase.analytics.logEvent("TTSEngine", bundle)
            // And set a custom key so that any crashes we get we know which TTS engine is in use
            FirebaseCrashlytics.getInstance().setCustomKey("TTSEngine", "$engineLabelAndName - $textToSpeechVoiceType")
            TextToSpeech(context, this, engineLabelAndName.substringAfter(":::"))
        }
    }

    fun checkTextToSpeechInitialization(block: Boolean) : Boolean {
        var timeout = 2000
        while(!textToSpeechInitialized) {
            Thread.sleep(100)
            timeout -= 100
            Log.d(TAG, "$timeout")
            if(!block || (timeout <= 0))
                return false
        }

        return true
    }

    private fun clearOutUtteranceSockets(utteranceId : String) {
        synchronized(ttsSockets) {
            val sockets = ttsSockets[utteranceId]
            if (sockets != null) {
                Log.d(TAG, "Closing socket pair $utteranceId")
                sockets[0].closeWithError("Finished")
                sockets[1].close()
            } else {
                Log.d(TAG, "No socket pair $utteranceId")
            }
            ttsSockets.remove(utteranceId)
        }
    }

    fun updateSpeech(sharedPreferences: SharedPreferences): Boolean {

        var change = false

        // Check for change in voice type preference
        val voiceType = sharedPreferences.getString(MainActivity.VOICE_TYPE_KEY, MainActivity.VOICE_TYPE_DEFAULT)!!
        if(textToSpeech.voices != null) {
            for (voice in textToSpeech.voices) {
                if (voice.name == voiceType) {
                    if (textToSpeechVoiceType != voice.name) {
                        Log.d(
                            TAG,
                            "Voice changed from $textToSpeechVoiceType to ${voice.name} on $this"
                        )
                        textToSpeech.voice = voice
                        textToSpeechVoiceType = voice.name
                        change = true
                    }
                    break
                }
            }
        }
        // Check for change in rate preference
        val rate = sharedPreferences.getFloat(
            MainActivity.SPEECH_RATE_KEY,
            MainActivity.SPEECH_RATE_DEFAULT
        )
        if (rate != textToSpeechRate) {
            textToSpeech.setSpeechRate(rate)
            Log.d(TAG, "Speech rate changed from $textToSpeechRate to $rate on $this")
            textToSpeechRate = rate
            change = change.or(true)
        }
        return change
    }

    fun setSpeechLanguage(language : String) : Boolean {
        Log.d(TAG, "setSpeechLanguage to \"$language\"")
        val result = textToSpeech.setLanguage(Locale(language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "The Language not supported!")
            return false
        }
        return true
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            Log.d(TAG, "Android version " + Build.VERSION.SDK_INT)

            // Get the current locale and initialize the text to speech engine with it
            val languageCode = getCurrentLocale().toLanguageTag()
            setSpeechLanguage(languageCode)

            sharedPreferences?.let {
                updateSpeech(it)
            }
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                override fun onDone(utteranceId: String) {
                    // TODO: This never seems to be called, why?
                    Log.e(TAG, "OnDone $utteranceId")
                }

                @Deprecated(
                    message = "Deprecated function, but needs overridden until it actually goes.",
                    replaceWith = ReplaceWith("")
                )
                override fun onError(utteranceId: String) {
                    Log.e(TAG, "OnError deprecated $utteranceId")
                    clearOutUtteranceSockets(utteranceId)
                }

                override fun onStart(utteranceId: String) {
                    Log.d(TAG, "OnStart $utteranceId")
                    currentUtteranceId?.let {
                        clearOutUtteranceSockets(it)
                    }
                    currentUtteranceId = utteranceId
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    // This path is triggered when shutting down the AudioEngine with multiple
                    // speech queued.
                    Log.d(TAG, "OnError $utteranceId")
                    utteranceId?.let {
                        clearOutUtteranceSockets(it)
                    }
                }

                override fun onBeginSynthesis(
                    utteranceId: String?,
                    sampleRateInHz: Int,
                    audioFormat: Int,
                    channelCount: Int
                ) {
                    Log.d(
                        TAG,
                        "OnBeginSynthesis $utteranceId: $sampleRateInHz, $audioFormat, $channelCount"
                    )

                    utteranceId?.let { id ->
                        val format = when (audioFormat) {
                            AudioFormat.ENCODING_PCM_8BIT -> 0
                            AudioFormat.ENCODING_PCM_16BIT -> 1
                            AudioFormat.ENCODING_PCM_FLOAT -> 2
                            else -> 1
                        }

                        Log.d(TAG, "Configure TTS audio for $utteranceId")
                        audioEngine.textToSpeechAudioConfigCallback(
                            id,
                            sampleRateInHz,
                            format,
                            channelCount
                        )
                    }
                }
            })
            // Tell the flow listeners that the TextToSpeech is ready
            Log.d(TAG, "textToSpeechInitialized")
            textToSpeechInitialized = true
            audioEngine.ttsRunningStateChanged(true)
        }
        else {
            val bundle = Bundle().apply {
                putInt("onInit status", status)
                putString("engine", engineLabelAndName)
                putString("voice", textToSpeechVoiceType)
            }
            Firebase.analytics.logEvent("TTSonInit_error", bundle)
        }
    }

    fun getAvailableEngines() : List<TextToSpeech.EngineInfo> {
        try {
            if (textToSpeechInitialized)
                return textToSpeech.engines
        } catch (e: Exception) {
            Firebase.analytics.logEvent("getAvailableEngines_error", null)
            Log.e(TAG, "getAvailableEngines: $e")
        }
        return emptyList()
    }

    fun getAvailableSpeechLanguages() : Set<Locale> {
        try {
            if (textToSpeechInitialized)
                return textToSpeech.availableLanguages
        } catch (e: Exception) {
            Firebase.analytics.logEvent("getAvailableSpeechLanguages_error", null)
            Log.e(TAG, "getAvailableSpeechVoices: $e")
        }
        return emptySet()
    }

    fun getAvailableSpeechVoices() : Set<Voice> {
        try {
            if (textToSpeechInitialized)
                return textToSpeech.voices
        } catch (e: Exception) {
            Firebase.analytics.logEvent("getAvailableSpeechVoices_error", null)
            Log.e(TAG, "getAvailableSpeechVoices: $e")
        }
        return emptySet()
    }

    fun stop() {
        textToSpeech.stop()
        currentUtteranceId = null

        // Close all of the previously queued sockets to terminate their playback
        synchronized(ttsSockets) {
            for(ttsSocketPair in ttsSockets){
                Log.d("TTS", "Close socket pair for " + ttsSocketPair.value[1].fd.toString())
                ttsSocketPair.value[0].closeWithError("Finished")
                ttsSocketPair.value[1].close()
            }
            ttsSockets.clear()
        }
    }

    fun createTextToSpeech(
        engineHandle: Long,
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double) : Long
    {
        val ttsSocketPair = ParcelFileDescriptor.createReliableSocketPair()
        val ttsSocket = ttsSocketPair[0]

        val params = Bundle()
        // We use the file descriptor as part of the utterance id as that's easy to track
        // in the C++ code. However, because file descriptors get reused we qualify the
        // utteranceId with an incrementing count.
        utteranceIncrementingCount += 1
        val utteranceId = ttsSocketPair[1].fd.toString() + "/" + utteranceIncrementingCount
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        ttsSockets[utteranceId] = ttsSocketPair
        val ttsHandle = audioEngine.createNativeTextToSpeech(
            engineHandle,
            type.type,
            latitude,
            longitude,
            heading,
            ttsSocketPair[1].fd,
            utteranceId
        )
        textToSpeech.synthesizeToFile(text, params, ttsSocket, utteranceId)
        return ttsHandle
    }

    companion object {
        private const val TAG = "TTS"
    }
}