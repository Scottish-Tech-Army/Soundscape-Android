package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class AudioType(val type: Int) {
    STANDARD(0),
    LOCALIZED(1),
    RELATIVE(2),
    COMPASS(3)
}


@Singleton
class NativeAudioEngine @Inject constructor(): AudioEngine, TextToSpeech.OnInitListener {
    private var engineHandle : Long = 0
    private val engineMutex = Object()
    private var ttsSockets = HashMap<String, Array<ParcelFileDescriptor>>()
    private var currentUtteranceId: String? = null
    private var textToSpeechInitialized : Boolean = false
    private var utteranceIncrementingCount : Int = 0

    private lateinit var textToSpeech : TextToSpeech
    private lateinit var ttsSocket : ParcelFileDescriptor
    private var textToSpeechVoiceType = MainActivity.VOICE_TYPE_DEFAULT
    private var textToSpeechRate = MainActivity.SPEECH_RATE_DEFAULT
    private var beaconType = MainActivity.BEACON_TYPE_DEFAULT

    private external fun create() : Long
    private external fun destroy(engineHandle: Long)
    private external fun createNativeBeacon(engineHandle: Long, mode: Int, latitude: Double, longitude: Double, heading: Double) :  Long
    private external fun destroyNativeBeacon(beaconHandle: Long)
    private external fun createNativeTextToSpeech(engineHandle: Long, mode: Int, latitude: Double, longitude: Double, heading: Double, ttsSocket: Int) :  Long
    private external fun createNativeEarcon(engineHandle: Long, asset:String, mode: Int, latitude: Double, longitude: Double, heading: Double) :  Long
    private external fun clearNativeTextToSpeechQueue(engineHandle: Long)
    private external fun getQueueDepth(engineHandle: Long) : Long
    private external fun updateGeometry(engineHandle: Long, latitude: Double, longitude: Double, heading: Double)
    private external fun setBeaconType(engineHandle: Long, beaconType: String)
    private external fun getListOfBeacons() : Array<String>

    private var _textToSpeechRunning = MutableStateFlow(false)
    val textToSpeechRunning = _textToSpeechRunning.asStateFlow()


    fun destroy()
    {
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        synchronized(engineMutex)
        {
            if (engineHandle == 0L) {
                return
            }
            destroy(engineHandle)
            engineHandle = 0

            for(ttsSocketPair in ttsSockets){
                Log.d("TTS", "Close socket pair for " + ttsSocketPair.value[1].fd.toString())
                ttsSocketPair.value[0].close()
                ttsSocketPair.value[1].close()
            }
            ttsSockets.clear()

            textToSpeech.shutdown()
            org.fmod.FMOD.close()
        }
    }

    private var sharedPreferences : SharedPreferences? = null
    private lateinit var sharedPreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener

    fun initialize(context : Context, followPreferences : Boolean = true)
    {
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
                                // If the voice type preference changes play some test speech
                                clearTextToSpeechQueue()
                                val testString =
                                    localizedContext.getString(R.string.first_launch_callouts_example_3)
                                createTextToSpeech(testString, AudioType.STANDARD)
                            }
                        }
                        if (key == MainActivity.BEACON_TYPE_KEY) {
                            updateBeaconType(preferences)
                        }
                    }
                }
            sharedPreferences?.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        }

        synchronized(engineMutex) {
            if (engineHandle != 0L) {
                return
            }
            org.fmod.FMOD.init(context)
            engineHandle = this.create()
            textToSpeech = TextToSpeech(context, this)
            sharedPreferences?.let {
                updateBeaconType(it)
            }
        }
    }

    private fun clearOutUtteranceSockets(utteranceId : String) {
        Log.d("TTS", "Closing socket pair $utteranceId")
        val sockets = ttsSockets[utteranceId]
        if(sockets != null ) {
            sockets[0].closeWithError("Finished")
            sockets[1].close()
        }
        ttsSockets.remove(utteranceId)
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
                    Log.e("TTS", "OnDone $utteranceId")
                }

                @Deprecated(
                    message = "Deprecated function, but needs overridden until it actually goes.",
                    replaceWith = ReplaceWith("")
                )
                override fun onError(utteranceId: String) {
                    Log.e("TTS", "OnError deprecated $utteranceId")
                    clearOutUtteranceSockets(utteranceId)
                }

                override fun onStart(utteranceId: String) {
                    Log.d("TTS", "OnStart $utteranceId")
                    currentUtteranceId?.let {
                        clearOutUtteranceSockets(it)
                    }
                    currentUtteranceId = utteranceId
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    // This path is triggered when shutting down the AudioEngine with multiple
                    // speech queued.
                    Log.d("TTS", "OnError $utteranceId")
                    utteranceId?.let {
                        clearOutUtteranceSockets(it)
                    }
                }
            })
            // Tell the flow listeners that the TextToSpeech is ready
            textToSpeechInitialized = true
            _textToSpeechRunning.value = true
        }
    }

    override fun createBeacon(location: LngLatAlt) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                Log.d(TAG, "Call createNativeBeacon")
                return createNativeBeacon(
                    engineHandle,
                    AudioType.LOCALIZED.type,
                    location.latitude,
                    location.longitude,
                    0.0)
            }

            return 0
        }
    }

    override fun destroyBeacon(beaconHandle: Long)
    {
        synchronized(engineMutex) {
            if(beaconHandle != 0L) {
                Log.d(TAG, "Call destroyNativeBeacon")
                destroyNativeBeacon(beaconHandle)
            }
        }
    }
    private fun awaitTextToSpeechInitialization() : Boolean {
        // Block waiting for the TextToSpeech to initialize before using it. Timeout after
        // waiting for 2 seconds and in that case return false
        var timeout = 2000
        while(!textToSpeechInitialized) {
            Thread.sleep(100)
            timeout -= 100
            Log.d(TAG, "$timeout")
            if(timeout <= 0)
                return false
        }

        return true
    }

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {

                if(!awaitTextToSpeechInitialization())
                    return 0

                val ttsSocketPair = ParcelFileDescriptor.createReliableSocketPair()
                ttsSocket = ttsSocketPair[0]

                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsSocket.toString())
                // We use the file descriptor as part of the utterance id as that's easy to track
                // in the C++ code. However, because file descriptors get reused we qualify the
                // utteranceId with an incrementing count.
                utteranceIncrementingCount += 1
                val utteranceId = ttsSocketPair[1].fd.toString() + "/" + utteranceIncrementingCount
                textToSpeech.synthesizeToFile(text, params, ttsSocket, utteranceId)

                // Store the socket pair in a hashmap indexed by utteranceId
                ttsSockets[ttsSocket.toString()] = ttsSocketPair

                Log.d(TAG, "Call createNativeTextToSpeech: $text")
                return createNativeTextToSpeech(
                    engineHandle,
                    type.type,
                    latitude,
                    longitude,
                    heading,
                    ttsSocketPair[1].fd)
            }

            return 0
        }
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {

                Log.d(TAG, "Call createNativeEarcon: $asset")
                return createNativeEarcon(engineHandle, asset, type.type,  latitude, longitude, heading)
            }

            return 0
        }
    }

    override fun clearTextToSpeechQueue() {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                if (!awaitTextToSpeechInitialization())
                    return

                // Stop the Text to Speech engine
                textToSpeech.stop()
                currentUtteranceId = null

                // Close all of the previously queued sockets to terminate their playback
                for(ttsSocketPair in ttsSockets){
                    Log.d("TTS", "Close socket pair for " + ttsSocketPair.value[1].fd.toString())
                    ttsSocketPair.value[0].closeWithError("Finished")
                    ttsSocketPair.value[1].close()
                }
                ttsSockets.clear()

                // Clear the queue in the engine
                clearNativeTextToSpeechQueue(engineHandle)
            }
        }
    }

    override fun getQueueDepth() : Long {
        synchronized(engineMutex) {
            if (engineHandle != 0L) {
                return getQueueDepth(engineHandle)
            }
        }
        return 0
    }

    override fun getAvailableSpeechLanguages() : Set<Locale> {
        if (!textToSpeechInitialized)
            return emptySet()

        return textToSpeech.availableLanguages
    }

    override fun getAvailableSpeechVoices() : Set<Voice> {
        if (!textToSpeechInitialized)
            return emptySet()
        return textToSpeech.voices
    }

    override fun updateBeaconType(sharedPreferences: SharedPreferences): Boolean {
        val newBeaconType = sharedPreferences.getString(
            MainActivity.BEACON_TYPE_KEY,
            MainActivity.BEACON_TYPE_DEFAULT
        )!!
        if(newBeaconType != beaconType) {
            setBeaconType(newBeaconType)
            Log.d(TAG, "Beacon changed from $beaconType to $newBeaconType on $this")
            beaconType = newBeaconType
            return true
        }
        return false
    }

    override fun updateSpeech(sharedPreferences: SharedPreferences): Boolean {

        var change = false

        // Check for change in voice type preference
        val voiceType = sharedPreferences.getString(MainActivity.VOICE_TYPE_KEY, MainActivity.VOICE_TYPE_DEFAULT)!!
        for (voice in textToSpeech.voices) {
            if (voice.name == voiceType) {
                if(textToSpeechVoiceType != voice.name) {
                    textToSpeech.voice = voice
                    Log.d(TAG, "Voice changed from $textToSpeechVoiceType to ${voice.name} on $this")
                    textToSpeechVoiceType = voice.name
                    change = true
                }
                break
            }
        }

        // Check for change in rate preference
        val rate = sharedPreferences.getFloat(MainActivity.SPEECH_RATE_KEY, MainActivity.SPEECH_RATE_DEFAULT)
        if (rate != textToSpeechRate) {
            textToSpeech.setSpeechRate(rate)
            Log.d(TAG, "Speech rate changed from $textToSpeechRate to $rate on $this")
            textToSpeechRate = rate
            change = change.or(true)
        }

        return change
    }

    override fun setSpeechLanguage(language : String) : Boolean {
        Log.d("TTS", "setSpeechLanguage to \"$language\"")
        val result = textToSpeech.setLanguage(Locale(language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "The Language not supported!")
            return false
        }
        return true
    }

    override fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double?)
    {        synchronized(engineMutex) {
            if(engineHandle != 0L)
                updateGeometry(
                    engineHandle,
                    listenerLatitude,
                    listenerLongitude,
                    listenerHeading ?: 50000.0
                )
        }
    }
    override fun setBeaconType(beaconType: String)
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L)
                setBeaconType(engineHandle, beaconType)
        }
    }

    override fun getListOfBeaconTypes() : Array<String>
    {
        return getListOfBeacons()
    }

    companion object {
        private const val TAG = "NativeAudioEngine"
        init {
            System.loadLibrary("soundscape-audio")
        }

        // Earcon asset filenames
        const val EARCON_CALIBRATION_IN_PROGRESS = "file:///android_asset/earcons/calibration_in_progress.wav"
        const val EARCON_CALIBRATION_SUCCESS = "file:///android_asset/earcons/calibration_success.wav"
        const val EARCON_CALLOUTS_ON = "file:///android_asset/earcons/callouts_on.wav"
        const val EARCON_CALLOUTS_OFF = "file:///android_asset/earcons/callouts_off.wav"
        const val EARCON_CONNECTION_SUCCESS = "file:///android_asset/earcons/connection_success.wav"
        const val EARCON_LOW_CONFIDENCE = "file:///android_asset/earcons/low_confidence.wav"
        const val EARCON_MODE_ENTER = "file:///android_asset/earcons/mode_enter.wav"
        const val EARCON_MODE_EXIT = "file:///android_asset/earcons/mode_exit.wav"
        const val EARCON_OFFLINE = "file:///android_asset/earcons/offline.wav"
        const val EARCON_ONLINE = "file:///android_asset/earcons/online.wav"
        const val EARCON_SENSE_LOCATION = "file:///android_asset/earcons/sense_location.wav"
        const val EARCON_SENSE_MOBILITY = "file:///android_asset/earcons/sense_mobility.wav"
        const val EARCON_SENSE_POI = "file:///android_asset/earcons/sense_poi.wav"
        const val EARCON_SENSE_SAFETY = "file:///android_asset/earcons/sense_safety.wav"
        const val EARCON_INFORMATION_ALERT = "file:///android_asset/earcons/information_alert.wav"
    }
}