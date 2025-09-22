package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.VOICE_TYPE_DEFAULT
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AudioType(val type: Int) {
    STANDARD(0),
    LOCALIZED(1),
    RELATIVE(2),
    COMPASS(3)
}

@Singleton
class NativeAudioEngine @Inject constructor(val service: SoundscapeService? = null): AudioEngine {

    private var engineHandle : Long = 0
    private val engineMutex = Object()
    private var beaconType = MainActivity.BEACON_TYPE_DEFAULT

    lateinit var ttsEngine : TtsEngine

    private external fun create() : Long
    private external fun destroy(engineHandle: Long)
    private external fun createNativeBeacon(engineHandle: Long, audioType: Int, headingOnly: Boolean, latitude: Double, longitude: Double, heading: Double) :  Long
    private external fun destroyNativeBeacon(beaconHandle: Long)
    private external fun toggleNativeBeaconMute(engineHandle: Long) : Boolean
    external fun createNativeTextToSpeech(engineHandle: Long,
                                          mode: Int,
                                          latitude: Double,
                                          longitude: Double,
                                          heading: Double,
                                          ttsSocket: Int,
                                          utteranceId: String) : Long
    private external fun audioConfigTextToSpeech(engineHandle: Long,
                                                 utteranceId: String,
                                                 sampleRate: Int,
                                                 format: Int,
                                                 channelCount: Int)
    private external fun createNativeEarcon(engineHandle: Long, asset:String, mode: Int, latitude: Double, longitude: Double, heading: Double) :  Long
    private external fun clearNativeTextToSpeechQueue(engineHandle: Long)
    private external fun getQueueDepth(engineHandle: Long) : Long
    private external fun updateGeometry(engineHandle: Long, latitude: Double, longitude: Double, heading: Double, focusGained: Boolean, duckingAllowed: Boolean, proximityNear: Double)
    private external fun setBeaconType(engineHandle: Long, beaconType: String)
    private external fun getListOfBeacons() : Array<String>

    private var _ttsRunningStateChange = MutableStateFlow(false)
    val ttsRunningStateChange = _ttsRunningStateChange.asStateFlow()

    fun ttsRunningStateChanged(value: Boolean) {
        _ttsRunningStateChange.value = value
    }

    private val engineCoroutineScope = CoroutineScope(Dispatchers.Default)
    private var geometryUpdateJob: Job? = null // Job to manage the periodic update task
    private var isActive = true
    init {
        if(service == null) {
            geometryUpdateJob = engineCoroutineScope.launch {
                while (isActive) { // Loop while the coroutine is active
                    updateGeometry(0.0, 0.0, 0.0, true, true, 15.0)
                    delay(100L) // Wait for 100 milliseconds
                }
            }
        }
    }

    fun destroy()
    {
        isActive = false
        geometryUpdateJob?.cancel()

        engineCoroutineScope.cancel()
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        synchronized(engineMutex)
        {
            if (engineHandle == 0L) {
                return
            }
            clearBeaconEventsListener(engineHandle)
            destroy(engineHandle)
            engineHandle = 0

            Log.d(TAG, "Destroy TTS engine from NativeAudioEngine destroy")
            ttsEngine.destroy()
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
                        if(key == MainActivity.SPEECH_ENGINE_KEY) {
                            // Replace the current TTS engine
                            val engineLabelAndName = preferences?.getString(
                                MainActivity.SPEECH_ENGINE_KEY,
                                MainActivity.SPEECH_ENGINE_DEFAULT)

                            if(ttsEngine.getCurrentLabelAndName() != engineLabelAndName) {
                                Log.d(
                                    TAG,
                                    "Destroy TTS engine due to SPEECH_ENGINE_KEY change: $engineLabelAndName"
                                )
                                ttsEngine.destroy()

                                // Reset the current chosen voice as we've switched engine
                                preferences.edit(true) {
                                    putString(
                                        MainActivity.VOICE_TYPE_KEY,
                                        VOICE_TYPE_DEFAULT
                                    )
                                }

                                ttsEngine = TtsEngine(this, engineLabelAndName)
                                ttsEngine.initialize(context, followPreferences)
                            }
                        }
                        if ((key == MainActivity.VOICE_TYPE_KEY) ||
                            (key == MainActivity.SPEECH_RATE_KEY)
                        ) {
                            Log.d(TAG, "VOICE_TYPE_KEY or SPEECH_RATE_KEY change")
                            if(ttsEngine.checkTextToSpeechInitialization(false)) {
                                if (ttsEngine.updateSpeech(preferences)) {
                                    if (service?.requestAudioFocus() == true) {
                                        // If the voice type preference changes play some test speech
                                        clearTextToSpeechQueue()
                                        val testString =
                                            localizedContext.getString(R.string.first_launch_callouts_example_3)
                                        createTextToSpeech(testString, AudioType.STANDARD)
                                    }
                                }
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
            ttsEngine = TtsEngine(
                this,
                sharedPreferences?.getString(
                    MainActivity.SPEECH_ENGINE_KEY,
                    MainActivity.SPEECH_ENGINE_DEFAULT)
            )
            ttsEngine.initialize(context, followPreferences)

            sharedPreferences?.let {
                updateBeaconType(it)
            }
            if (engineHandle != 0L) {
                setBeaconEventsListener(engineHandle) // Setup the listener
            } else {
                Log.e(TAG, "Failed to create native audio engine instance.")
            }
        }
    }

    override fun textToSpeechAudioConfigCallback(id : String, sampleRateInHz: Int, format: Int, channelCount: Int) {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                audioConfigTextToSpeech(engineHandle, id, sampleRateInHz, format, channelCount)
            }
        }
    }

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                Log.d(TAG, "Call createNativeBeacon")
                return createNativeBeacon(
                    engineHandle,
                    AudioType.LOCALIZED.type,
                    headingOnly,
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

    override fun toggleBeaconMute() : Boolean
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                return toggleNativeBeaconMute(engineHandle)
            }
        }
        return false
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

                if(!ttsEngine.checkTextToSpeechInitialization(true))
                    return 0

                return ttsEngine.createTextToSpeech(
                    engineHandle,
                    text,
                    type,
                    latitude,
                    longitude,
                    heading
                )
            }

            return 0
        }
    }

    fun updateSpeech(context: Context) {
        if (service?.requestAudioFocus() == true) {
            // If the voice type preference changes play some test speech
            clearTextToSpeechQueue()
            val testString = context.getString(R.string.first_launch_callouts_example_3)
            createTextToSpeech(testString, AudioType.STANDARD)
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
                if (!ttsEngine.checkTextToSpeechInitialization(true))
                    return

                // Stop the Text to Speech engine
                ttsEngine.stop()

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

    override fun getAvailableSpeechEngines() : List<TextToSpeech.EngineInfo> {
        return ttsEngine.getAvailableEngines()
    }

    override fun getAvailableSpeechLanguages() : Set<Locale> {
        return ttsEngine.getAvailableSpeechLanguages()
    }

    override fun getAvailableSpeechVoices() : Set<Voice> {
        return ttsEngine.getAvailableSpeechVoices()
    }

    override fun setSpeechLanguage(language : String) : Boolean {
        return ttsEngine.setSpeechLanguage(language)
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

    override fun updateGeometry(listenerLatitude: Double,
                                listenerLongitude: Double,
                                listenerHeading: Double?,
                                focusGained: Boolean,
                                duckingAllowed: Boolean,
                                proximityNear: Double)
    {        synchronized(engineMutex) {
            if(engineHandle != 0L)
                updateGeometry(
                    engineHandle,
                    listenerLatitude,
                    listenerLongitude,
                    listenerHeading ?: 50000.0,
                    focusGained,
                    duckingAllowed,
                    15.0
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

    /**
     * Called from JNI when all beacons have been cleared from the AudioEngine.
     */
    override fun onAllBeaconsCleared() {
        println("JNI Callback: All beacons have been cleared in AudioEngine.")
        service?.abandonAudioFocus()
    }

    /**
     * Sets up this NativeAudioEngine instance as a listener for beacon events in the C++ AudioEngine.
     */
    private external fun setBeaconEventsListener(nativeHandle: Long)

    /**
     * Clears this NativeAudioEngine instance as a listener in the C++ AudioEngine.
     */
    private external fun clearBeaconEventsListener(nativeHandle: Long)


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