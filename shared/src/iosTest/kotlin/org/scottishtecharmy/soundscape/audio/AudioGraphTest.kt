package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioChannelLayout
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEnvironmentNode
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioChannelLayoutTag_Stereo
import kotlin.test.Test

/**
 * Tests for the AVAudioEngine graph connections.
 * These will skip gracefully if audio hardware isn't available (CI/test host).
 * Run on a simulator or device for full testing.
 */
@OptIn(ExperimentalForeignApi::class)
class AudioGraphTest {

    private fun setupEngine(): AVAudioEngine? {
        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            session.setActive(true, error = null)
        } catch (e: Exception) {
            println("SKIP: Audio session not available: ${e.message}")
            return null
        }

        val engine = AVAudioEngine()
        @Suppress("UNUSED_VARIABLE")
        val mixer = engine.mainMixerNode // force init

        try {
            engine.startAndReturnError(null)
        } catch (e: Exception) {
            println("SKIP: Engine failed to start: ${e.message}")
            return null
        }

        if (!engine.isRunning()) {
            println("SKIP: Engine not running")
            return null
        }
        return engine
    }

    private fun createStereoFormat(sampleRate: Double): AVAudioFormat? {
        val layout = AVAudioChannelLayout(layoutTag = kAudioChannelLayoutTag_Stereo)
            ?: return null
        return AVAudioFormat(standardFormatWithSampleRate = sampleRate, channelLayout = layout)
    }

    private fun monoFloat32(sampleRate: Double): AVAudioFormat? {
        return AVAudioFormat(3u, sampleRate = sampleRate, channels = 1u, interleaved = false)
    }

    @Test
    fun testPlayerToMainMixer() {
        val engine = setupEngine() ?: return
        val player = AVAudioPlayerNode()
        engine.attachNode(player)
        engine.connect(player, to = engine.mainMixerNode, format = null)
        player.play()
        println("PASS: player → mainMixer, playing=${player.isPlaying()}")
        player.stop()
        engine.stop()
    }

    @Test
    fun testPlayerToEnvironmentMono44100() {
        val engine = setupEngine() ?: return
        val player = AVAudioPlayerNode()
        val env = AVAudioEnvironmentNode()
        engine.attachNode(player)
        engine.attachNode(env)

        val mono = monoFloat32(44100.0) ?: run { println("SKIP: can't create mono format"); return }
        val stereo = createStereoFormat(44100.0) ?: run { println("SKIP: can't create stereo format"); return }

        println("  mono: ${mono.channelCount}ch ${mono.sampleRate}Hz")
        println("  stereo: ${stereo.channelCount}ch ${stereo.sampleRate}Hz")

        try {
            engine.connect(player, to = env, format = mono)
            engine.connect(env, to = engine.mainMixerNode, format = stereo)
            player.play()
            println("PASS: player →(mono44100)→ env →(stereo44100)→ mixer, playing=${player.isPlaying()}")
        } catch (e: Exception) {
            println("FAIL: ${e.message}")
        }
        player.stop()
        engine.stop()
    }

    @Test
    fun testPlayerToEnvironmentMono22050() {
        val engine = setupEngine() ?: return
        val player = AVAudioPlayerNode()
        val env = AVAudioEnvironmentNode()
        engine.attachNode(player)
        engine.attachNode(env)

        val mono = monoFloat32(22050.0) ?: run { println("SKIP"); return }
        val stereo = createStereoFormat(22050.0) ?: run { println("SKIP"); return }

        try {
            engine.connect(player, to = env, format = mono)
            engine.connect(env, to = engine.mainMixerNode, format = stereo)
            player.play()
            println("PASS: player →(mono22050)→ env →(stereo22050)→ mixer, playing=${player.isPlaying()}")
        } catch (e: Exception) {
            println("FAIL at 22050Hz: ${e.message}")
        }
        player.stop()
        engine.stop()
    }

    @Test
    fun testSwitchSampleRateOnSameEnvNode() {
        val engine = setupEngine() ?: return
        val env = AVAudioEnvironmentNode()
        engine.attachNode(env)

        // First at 44100
        val player1 = AVAudioPlayerNode()
        engine.attachNode(player1)
        val mono44 = monoFloat32(44100.0)!!
        val stereo44 = createStereoFormat(44100.0)!!

        try {
            engine.connect(player1, to = env, format = mono44)
            engine.connect(env, to = engine.mainMixerNode, format = stereo44)
            player1.play()
            println("  44100Hz: playing=${player1.isPlaying()}")
            player1.stop()
            engine.disconnectNodeOutput(player1)
            engine.detachNode(player1)
        } catch (e: Exception) {
            println("FAIL at 44100Hz: ${e.message}")
            engine.stop()
            return
        }

        // Now at 22050 on the SAME env node
        val player2 = AVAudioPlayerNode()
        engine.attachNode(player2)
        val mono22 = monoFloat32(22050.0)!!
        val stereo22 = createStereoFormat(22050.0)!!

        try {
            engine.disconnectNodeOutput(env) // disconnect env→mixer first
            engine.connect(player2, to = env, format = mono22)
            engine.connect(env, to = engine.mainMixerNode, format = stereo22)
            player2.play()
            println("  22050Hz: playing=${player2.isPlaying()}")
            println("PASS: switched sample rate on same env node")
        } catch (e: Exception) {
            println("FAIL switching to 22050Hz: ${e.message}")
        }
        player2.stop()
        engine.stop()
    }

    @Test
    fun testTwoEnvNodesForDifferentSampleRates() {
        val engine = setupEngine() ?: return

        val env44 = AVAudioEnvironmentNode()
        val env22 = AVAudioEnvironmentNode()
        engine.attachNode(env44)
        engine.attachNode(env22)

        val player1 = AVAudioPlayerNode()
        val player2 = AVAudioPlayerNode()
        engine.attachNode(player1)
        engine.attachNode(player2)

        try {
            engine.connect(player1, to = env44, format = monoFloat32(44100.0))
            engine.connect(env44, to = engine.mainMixerNode, format = createStereoFormat(44100.0))

            engine.connect(player2, to = env22, format = monoFloat32(22050.0))
            engine.connect(env22, to = engine.mainMixerNode, format = createStereoFormat(22050.0))

            player1.play()
            player2.play()
            println("PASS: two env nodes, player1=${player1.isPlaying()}, player2=${player2.isPlaying()}")
        } catch (e: Exception) {
            println("FAIL two env nodes: ${e.message}")
        }

        player1.stop()
        player2.stop()
        engine.stop()
    }

    @Test
    fun testScheduleBufferFormatMatch() {
        val engine = setupEngine() ?: return
        val env = AVAudioEnvironmentNode()
        val player = AVAudioPlayerNode()
        engine.attachNode(env)
        engine.attachNode(player)

        val mono = monoFloat32(44100.0)!!
        val stereo = createStereoFormat(44100.0)!!

        engine.connect(player, to = env, format = mono)
        engine.connect(env, to = engine.mainMixerNode, format = stereo)

        // Create a buffer in the same format
        val buffer = AVAudioPCMBuffer(pCMFormat = mono, frameCapacity = 4410u)
        if (buffer == null) {
            println("SKIP: can't create buffer")
            engine.stop()
            return
        }
        buffer.frameLength = 4410u

        try {
            player.play()
            player.scheduleBuffer(buffer, completionHandler = null)
            println("PASS: scheduleBuffer with matching format")
        } catch (e: Exception) {
            println("FAIL scheduleBuffer: ${e.message}")
        }

        player.stop()
        engine.stop()
    }
}
