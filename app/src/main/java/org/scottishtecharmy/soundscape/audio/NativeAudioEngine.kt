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
    private var utteranceIncrementingCount : Int = 0

    private lateinit var textToSpeech : TextToSpeech
    private lateinit var ttsSocket : ParcelFileDescriptor

    private external fun create() : Long
    private external fun destroy(engineHandle: Long)
    private external fun createNativeBeacon(engineHandle: Long, latitude: Double, longitude: Double) :  Long
    private external fun destroyNativeBeacon(beaconHandle: Long)
    private external fun createNativeTextToSpeech(engineHandle: Long, latitude: Double, longitude: Double, ttsSocket: Int) :  Long
    private external fun clearNativeTextToSpeechQueue(engineHandle: Long)
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
                Log.d("TTS", "Close socket pair for " + ttsSocketPair.value[1].fd.toString())
                ttsSocketPair.value[0].close()
                ttsSocketPair.value[1].close()
            }
            ttsSockets.clear()

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

            Log.e("Soundscape", "Android version " + Build.VERSION.SDK_INT)

            // Get the current locale and initialize the text to speech engine with it
            val languageCode = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            setSpeechLanguage(languageCode)

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

    override fun createTextToSpeech(text: String, latitude: Double, longitude: Double) : Long
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
                return createNativeTextToSpeech(engineHandle, latitude, longitude, ttsSocketPair[1].fd)
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