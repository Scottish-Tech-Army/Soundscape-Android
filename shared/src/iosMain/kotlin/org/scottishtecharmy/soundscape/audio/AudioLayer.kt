package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudio3DPoint
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioMixerNode
import platform.AVFAudio.AVAudioNode
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioUnitEQ

/**
 * Port of PreparableAudioLayer.swift.
 * Node chain: player → [equalizer → mixer →] target
 * When EQ is present, the mixer handles 3D positioning.
 * Without EQ, the player handles positioning directly.
 */
@OptIn(ExperimentalForeignApi::class)
class AudioLayer(withEq: Boolean = false) {

    val player = AVAudioPlayerNode()
    private val equalizer: AVAudioUnitEQ? = if (withEq) AVAudioUnitEQ(numberOfBands = 0u) else null
    private val mixer: AVAudioMixerNode? = if (withEq) AVAudioMixerNode() else null
    var format: AVAudioFormat? = null
    private var engine: AVAudioEngine? = null

    var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
            mixer?.outputVolume = value
        }

    var position: CValue<AVAudio3DPoint>
        get() = mixer?.position ?: player.position
        set(value) {
            if (mixer != null) {
                mixer.position = value
            } else {
                player.position = value
            }
        }

    val isPlaying: Boolean
        get() = player.isPlaying()

    fun attach(engine: AVAudioEngine) {
        this.engine = engine
        engine.attachNode(player)
        equalizer?.let { engine.attachNode(it) }
        mixer?.let { engine.attachNode(it) }
    }

    /**
     * Connect this layer to a target node using the layer's format.
     * Chain: player → [equalizer → mixer →] targetNode
     * Matches PreparableAudioLayer.connect(to:) from the original iOS app.
     */
    fun connect(toNode: AVAudioNode, format: AVAudioFormat? = null) {
        val eng = engine ?: return
        val fmt = format ?: this.format

        val eq = equalizer
        val mix = mixer
        if (eq != null && mix != null) {
            eng.connect(player, to = eq, format = fmt)
            eng.connect(eq, to = mix, format = fmt)
            eng.connect(mix, to = toNode, format = fmt)
        } else {
            eng.connect(player, to = toNode, format = fmt)
        }
    }

    fun disconnect() {
        engine?.disconnectNodeOutput(player)
        equalizer?.let { engine?.disconnectNodeOutput(it) }
        mixer?.let { engine?.disconnectNodeOutput(it) }
    }

    fun detach() {
        engine?.detachNode(player)
        equalizer?.let { engine?.detachNode(it) }
        mixer?.let { engine?.detachNode(it) }
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
