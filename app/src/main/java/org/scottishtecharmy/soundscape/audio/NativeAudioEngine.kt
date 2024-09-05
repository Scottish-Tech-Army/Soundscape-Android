package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NativeAudioEngine @Inject constructor(): AudioEngine, TextToSpeech.OnInitListener {
    private var engineHandle : Long = 0
    private val engineMutex = Object()
    private var ttsSockets = HashMap<String, Array<ParcelFileDescriptor>>()
    private var currentUtteranceId: String? = null
    private var textToSpeechInitialized : Boolean = false

    private lateinit var textToSpeech : TextToSpeech
    private lateinit var ttsSocket : ParcelFileDescriptor

    private external fun create() : Long
    private external fun destroy(engineHandle: Long)
    private external fun createNativeBeacon(engineHandle: Long, latitude: Double, longitude: Double) :  Long
    private external fun destroyNativeBeacon(beaconHandle: Long)
    private external fun createNativeTextToSpeech(engineHandle: Long, latitude: Double, longitude: Double, ttsSocket: Int) :  Long
    private external fun updateGeometry(engineHandle: Long, latitude: Double, longitude: Double, heading: Double)
    private external fun setBeaconType(engineHandle: Long, beaconType: String)
    private external fun getListOfBeacons() : Array<String>

    fun destroy()
    {
        synchronized(engineMutex)
        {
            if (engineHandle == 0L) {
                return
            }
            destroy(engineHandle)
            engineHandle = 0

            for(ttsSocketPair in ttsSockets){
                Log.e("TTS", "Close socket pair " + ttsSocketPair.key)
                ttsSocketPair.value[0].close()
                ttsSocketPair.value[1].close()
            }

            textToSpeech.shutdown()
            org.fmod.FMOD.close()
        }
    }
    fun initialize(context : Context)
    {
        synchronized(engineMutex) {
            if (engineHandle != 0L) {
                return
            }
            org.fmod.FMOD.init(context)
            engineHandle = this.create()
            textToSpeech = TextToSpeech(context, this)
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            Log.e("Soundscape", "Android version " + Build.VERSION.SDK_INT)

            // Get the current locale and initialize the text to speech engine with it
            val languageCode = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            setSpeechLanguage(languageCode)

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                // TODO: Need to test what happens with this code on phones with
                //  older API.

                override fun onDone(utteranceId: String) {
                    // TODO: This never seems to be called, why?
                    Log.e("TTS", "OnDone $utteranceId")
                }

                @Deprecated(
                    message = "Deprecated function, but needs overridden until it actually goes.",
                    replaceWith = ReplaceWith("")
                )
                override fun onError(utteranceId: String) {
                    // TODO: Need to test this path and handle it correctly
                    Log.e("TTS", "OnError $utteranceId")
                }

                override fun onStart(utteranceId: String) {
                    Log.e("TTS", "OnStart $utteranceId")
                    if(currentUtteranceId != null){
                        Log.e("TTS", "Closing socket pair $currentUtteranceId")
                        ttsSockets[currentUtteranceId]!![0].closeWithError("Finished")
                        ttsSockets[currentUtteranceId]!![1].close()
                        ttsSockets.remove(currentUtteranceId)
                    }
                    currentUtteranceId = utteranceId
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    // TODO: Need to test this path and handle it correctly
                    Log.e("TTS", "OnError2 $utteranceId")
                }
            })
        }
        textToSpeechInitialized = true
    }

    override fun createBeacon(latitude: Double, longitude: Double) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {
                Log.d(TAG, "Call createNativeBeacon")
                return createNativeBeacon(engineHandle, latitude, longitude)
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
        // waiting for 10 seconds and in that case return false
        var timeout = 10000
        while(!textToSpeechInitialized) {
            Thread.sleep(100)
            timeout -= 100
            if(timeout <= 0)
                return false
        }

        return true
    }

    override fun createTextToSpeech(latitude: Double, longitude: Double, text: String) : Long
    {
        synchronized(engineMutex) {
            if(engineHandle != 0L) {

                if(!awaitTextToSpeechInitialization())
                    return 0

                val ttsSocketPair = ParcelFileDescriptor.createReliableSocketPair()
                ttsSocket = ttsSocketPair[0]

                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ttsSocket.toString())
                textToSpeech.synthesizeToFile(text, params, ttsSocket, ttsSocket.toString())

                // Store the socket pair in a hashmap indexed by utteranceId
                ttsSockets[ttsSocket.toString()] = ttsSocketPair

                Log.d(TAG, "Call createNativeTextToSpeech: $text")
                return createNativeTextToSpeech(engineHandle, latitude, longitude, ttsSocketPair[1].fd)
            }

            return 0
        }
    }

    override fun getAvailableSpeechLanguages() : Set<Locale> {
        if (!awaitTextToSpeechInitialization())
            return emptySet()

        return textToSpeech.availableLanguages
    }

    override fun getAvailableSpeechVoices() : Set<Voice> {
        if (!awaitTextToSpeechInitialization())
            return emptySet()

        return textToSpeech.voices
    }

    override fun setSpeechLanguage(language : String) : Boolean {
        Log.e("TTS", "setSpeechLanguage to \"$language\"")
        val result = textToSpeech.setLanguage(Locale(language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "The Language not supported!")
            return false
        }
        return true
    }

    override fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double)
    {        synchronized(engineMutex) {
            if(engineHandle != 0L)
                updateGeometry(engineHandle, listenerLatitude, listenerLongitude, listenerHeading)
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
    }
}