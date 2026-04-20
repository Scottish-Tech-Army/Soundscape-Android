package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudio3DPoint
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioNode
import platform.AVFAudio.AVAudioPlayerNode

/**
 * Simplified port of PreparableAudioLayer.swift.
 * Wraps an AVAudioPlayerNode for playback with optional 3D positioning.
 * EQ support omitted for now — can be added later.
 */
@OptIn(ExperimentalForeignApi::class)
class AudioLayer {

    val player = AVAudioPlayerNode()
    var format: AVAudioFormat? = null
    private var engine: AVAudioEngine? = null

    var volume: Float
        get() = player.volume
        set(value) { player.volume = value }

    var position: CValue<AVAudio3DPoint>
        get() = player.position
        set(value) { player.position = value }

    val isPlaying: Boolean
        get() = player.isPlaying()

    fun attach(engine: AVAudioEngine) {
        this.engine = engine
        engine.attachNode(player)
    }

    fun connect(toNode: AVAudioNode, format: AVAudioFormat? = null) {
        val eng = engine ?: return
        val fmt = format ?: this.format
        if (fmt != null) {
            eng.connect(player, to = toNode, format = fmt)
        } else {
            eng.connect(player, to = toNode, format = null)
        }
    }

    fun disconnect() {
        engine?.disconnectNodeOutput(player)
    }

    fun detach() {
        engine?.detachNode(player)
        engine = null
    }

    fun play() {
        if (engine?.isRunning() == true) {
            player.play()
        }
    }

    fun stop() {
        if (isPlaying) {
            player.stop()
        }
    }
}
