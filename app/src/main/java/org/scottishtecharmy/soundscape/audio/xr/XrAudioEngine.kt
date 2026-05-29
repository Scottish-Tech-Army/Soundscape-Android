package org.scottishtecharmy.soundscape.audio.xr

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.View
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.scene
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.TtsEngine
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.util.Locale

class XrAudioEngine(val session: Session, val context: Context) : AudioEngine {
    private val scene = session.scene

    private val soundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

    private val pointSourceParams = PointSourceParams()

    // We'll use a invisible panel entity for positioning sounds
    private val audioEntity: PanelEntity by lazy {
        PanelEntity.create(
            session,
            View(context),
            FloatSize2d(0.01f, 0.01f),
            "AudioSource",
            Pose.Identity,
            scene.activitySpace
        )
    }

    private lateinit var ttsEngine: TtsEngine
    private var listenerLocation: LngLatAlt? = null
    private var listenerHeading: Double = 0.0
    private var ruler = CheapRuler(0.0)

    override fun initialize(context: Context) {
        ttsEngine = TtsEngine(this, null)
        ttsEngine.initialize(context)
    }

    override fun destroy() {
        ttsEngine.destroy()
        soundPool.release()
    }

    override fun ttsRunningStateChanged(value: Boolean) {
    }

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        updateEntityPosition(location.latitude, location.longitude)
        return 1L
    }

    override fun destroyBeacon(beaconHandle: Long) {
    }

    override fun toggleBeaconMute(): Boolean {
        return false
    }

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        if (!latitude.isNaN() && !longitude.isNaN()) {
            updateEntityPosition(latitude, longitude)
        } else {
            audioEntity.setPose(Pose.Identity)
        }

        return ttsEngine.createTextToSpeech(0L, text, type, latitude, longitude, heading)
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        if (!latitude.isNaN() && !longitude.isNaN()) {
            updateEntityPosition(latitude, longitude)
        }
        return 0L
    }

    private fun updateEntityPosition(latitude: Double, longitude: Double) {
        val listener = listenerLocation ?: return

        if (ruler.needsReplacing(listener.latitude)) {
            ruler = CheapRuler(listener.latitude)
        }

        val distance = ruler.distance(listener, LngLatAlt(longitude, latitude)) * 1000.0 // meters
        val bearing = ruler.bearing(listener, LngLatAlt(longitude, latitude))

        val relativeBearing = bearing - listenerHeading

        val x = (distance * kotlin.math.sin(Math.toRadians(relativeBearing))).toFloat()
        val z = (-distance * kotlin.math.cos(Math.toRadians(relativeBearing))).toFloat()

        audioEntity.setPose(Pose(Vector3(x, 0f, z), Quaternion.Identity))
    }

    override fun clearTextToSpeechQueue() {
        ttsEngine.stop()
    }

    override fun getQueueDepth(): Long = 0L

    override fun isHandleActive(handle: Long): Boolean = false

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double
    ) {
        this.listenerLocation = LngLatAlt(listenerLongitude, listenerLatitude)
        this.listenerHeading = listenerHeading ?: 0.0
    }

    override fun setBeaconType(beaconType: String) {}

    override fun getListOfBeaconTypes(): Array<String> = emptyArray()

    override fun getAvailableSpeechEngines(): List<TextToSpeech.EngineInfo> =
        ttsEngine.getAvailableEngines()

    override fun getAvailableSpeechLanguages(): Set<Locale> =
        ttsEngine.getAvailableSpeechLanguages()

    override fun getAvailableSpeechVoices(): Set<Voice> = ttsEngine.getAvailableSpeechVoices()

    override fun setSpeechLanguage(language: String): Boolean =
        ttsEngine.setSpeechLanguage(language)

    override fun updateBeaconType(sharedPreferences: SharedPreferences): Boolean = false

    override fun onAllBeaconsCleared() {}

    override fun textToSpeechAudioConfigCallback(
        id: String,
        sampleRateInHz: Int,
        format: Int,
        channelCount: Int
    ) {
    }

    override fun setHrtfEnabled(enabled: Boolean) {}
}
