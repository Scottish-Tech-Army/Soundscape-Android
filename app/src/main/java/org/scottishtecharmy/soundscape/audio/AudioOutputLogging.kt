package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import org.scottishtecharmy.soundscape.logging.AppLog

private val logger = AppLog.tag("AudioOutput")

/**
 * The phone's media volume, which is what controls Soundscape: the Oboe stream is opened with
 * Usage::Media, so it plays on STREAM_MUSIC. Android keeps a separate media volume per output
 * device, and this reads the one for whatever the audio is currently routed to.
 */
fun mediaVolumeDescription(context: Context): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val percent = if (max > 0) volume * 100 / max else 0
    val muted = if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) ", muted" else ""
    return "$volume/$max ($percent%)$muted"
}

@Suppress("NewApi") // Inlined int constants, safe on all API levels
private val typeNames = mapOf(
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE to "Built-in Earpiece",
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER to "Built-in Speaker",
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE to "Built-in Speaker (safe)",
    AudioDeviceInfo.TYPE_WIRED_HEADSET to "Wired Headset",
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES to "Wired Headphones",
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO to "Bluetooth SCO",
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP to "Bluetooth A2DP",
    AudioDeviceInfo.TYPE_USB_DEVICE to "USB Device",
    AudioDeviceInfo.TYPE_USB_ACCESSORY to "USB Accessory",
    AudioDeviceInfo.TYPE_USB_HEADSET to "USB Headset",
    AudioDeviceInfo.TYPE_HEARING_AID to "Hearing Aid",
    AudioDeviceInfo.TYPE_BLE_HEADSET to "BLE Headset",
    AudioDeviceInfo.TYPE_BLE_SPEAKER to "BLE Speaker",
    AudioDeviceInfo.TYPE_BLE_BROADCAST to "BLE Broadcast",
    AudioDeviceInfo.TYPE_TELEPHONY to "Telephony",
    AudioDeviceInfo.TYPE_BUILTIN_MIC to "Built-in Mic",
)

fun deviceTypeName(type: Int) = typeNames[type] ?: "Unknown($type)"

private fun describe(device: AudioDeviceInfo) =
    "${device.productName} (${deviceTypeName(device.type)}, id ${device.id})"

/**
 * Logs output devices (headphones, Bluetooth, USB and so on) arriving and leaving, with the media
 * volume after each change since Android switches to that device's own volume. Together with the
 * native AudioMixer's stream open/disconnect/restart events, which also go to the support log,
 * this shows in a Contact Support log when and where Soundscape had somewhere to play audio.
 */
class AudioOutputLogger(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var registered = false

    private val callback = object : AudioDeviceCallback() {
        // Called once on registration with every current device, which logs the starting state.
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val outputs = addedDevices.filter { it.isSink }
            if (outputs.isEmpty()) return
            logger.i {
                "Output devices added: ${outputs.joinToString { describe(it) }}, " +
                    "media volume ${mediaVolumeDescription(context)}"
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val outputs = removedDevices.filter { it.isSink }
            if (outputs.isEmpty()) return
            logger.i {
                "Output devices removed: ${outputs.joinToString { describe(it) }}, " +
                    "media volume ${mediaVolumeDescription(context)}"
            }
        }
    }

    fun start() {
        if (registered) return
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        registered = true
    }

    fun stop() {
        if (!registered) return
        audioManager.unregisterAudioDeviceCallback(callback)
        registered = false
    }
}
