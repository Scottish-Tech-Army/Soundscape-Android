package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WitMotionFrameParserTest {

    @Test
    fun decodesYawFromCompleteFrame() {
        // Yaw 90.0 deg -> raw int16 = 90/180 * 32768 = 16384 = 0x4000
        // bytes (LE): lo=0x00, hi=0x40
        val frame = activeFrame(rollRaw = 0, pitchRaw = 0, yawRaw = 16384)
        val out = WitMotionFrameParser().consume(frame, timestampMillis = 1L)
        assertEquals(1, out.size)
        assertNear(90.0, out[0].yawDegrees)
        assertEquals(1L, out[0].timestampMillis)
    }

    @Test
    fun decodesNegativeYaw() {
        // Yaw -90 deg -> raw -16384 = 0xC000 (two's complement int16)
        val frame = activeFrame(rollRaw = 0, pitchRaw = 0, yawRaw = -16384)
        val out = WitMotionFrameParser().consume(frame, 0L)
        assertEquals(1, out.size)
        assertNear(-90.0, out[0].yawDegrees)
    }

    @Test
    fun resyncsPastLeadingGarbage() {
        val parser = WitMotionFrameParser()
        val garbage = byteArrayOf(0x55, 0x12, 0x00, 0xFF.toByte(), 0x55)
        val frame = activeFrame(0, 0, 8192) // yaw 45 deg
        val out = parser.consume(garbage + frame, 0L)
        assertEquals(1, out.size)
        assertNear(45.0, out[0].yawDegrees)
    }

    @Test
    fun handlesSplitFrameAcrossChunks() {
        val parser = WitMotionFrameParser()
        val frame = activeFrame(0, 0, -8192) // yaw -45 deg
        val firstHalf = frame.copyOfRange(0, 7)
        val secondHalf = frame.copyOfRange(7, frame.size)
        assertEquals(0, parser.consume(firstHalf, 0L).size)
        val out = parser.consume(secondHalf, 5L)
        assertEquals(1, out.size)
        assertNear(-45.0, out[0].yawDegrees)
        assertEquals(5L, out[0].timestampMillis)
    }

    @Test
    fun decodesMultipleFramesInOneChunk() {
        val parser = WitMotionFrameParser()
        val combined = activeFrame(0, 0, 0) + activeFrame(0, 0, 16384)
        val out = parser.consume(combined, 0L)
        assertEquals(2, out.size)
        assertNear(0.0, out[0].yawDegrees)
        assertNear(90.0, out[1].yawDegrees)
    }

    @Test
    fun registerResponseRoutedToCallback() {
        val seen = mutableListOf<WitMotionRegisterResponse>()
        val parser = WitMotionFrameParser(onRegisterResponse = { seen.add(it) })
        // Battery voltage register (0x64) returning 410 (= 4.10 V at 0.01 V/unit).
        val regFrame = registerFrame(address = 0x64, rawValue = 410)
        val active = activeFrame(0, 0, 16384)
        val out = parser.consume(regFrame + active, 7L)
        assertEquals(1, out.size)
        assertNear(90.0, out[0].yawDegrees)
        assertEquals(1, seen.size)
        assertEquals(0x64, seen[0].address)
        assertEquals(410, seen[0].rawValue)
        assertEquals(7L, seen[0].timestampMillis)
    }

    @Test
    fun emptyInputProducesNothing() {
        val parser = WitMotionFrameParser()
        assertTrue(parser.consume(ByteArray(0), 0L).isEmpty())
    }

    @Test
    fun decodesAccelAndGyroChannels() {
        // 1g on Z (raw = 2048 because scale is 16/32768) and 100 deg/s about X
        // (raw = 1638 because scale is 2000/32768).
        val frame = activeFrame(
            accelXRaw = 0, accelYRaw = 0, accelZRaw = 2048,
            gyroXRaw = 1638, gyroYRaw = 0, gyroZRaw = 0,
            rollRaw = 0, pitchRaw = 0, yawRaw = 0,
        )
        val out = WitMotionFrameParser().consume(frame, 0L)
        assertEquals(1, out.size)
        assertNear(0.0, out[0].accelG.x)
        assertNear(0.0, out[0].accelG.y)
        assertNear(1.0, out[0].accelG.z)
        assertNear(100.0, out[0].gyroDps.x, tolerance = 0.5)
        assertNear(0.0, out[0].gyroDps.y)
        assertNear(0.0, out[0].gyroDps.z)
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.01) {
        assertTrue(
            abs(expected - actual) < tolerance,
            "expected $expected, got $actual"
        )
    }

    private fun activeFrame(rollRaw: Int, pitchRaw: Int, yawRaw: Int): ByteArray =
        activeFrame(0, 0, 0, 0, 0, 0, rollRaw, pitchRaw, yawRaw)

    private fun activeFrame(
        accelXRaw: Int,
        accelYRaw: Int,
        accelZRaw: Int,
        gyroXRaw: Int,
        gyroYRaw: Int,
        gyroZRaw: Int,
        rollRaw: Int,
        pitchRaw: Int,
        yawRaw: Int,
    ): ByteArray {
        val b = ByteArray(20)
        b[0] = 0x55
        b[1] = 0x61
        writeInt16Le(b, 2, accelXRaw)
        writeInt16Le(b, 4, accelYRaw)
        writeInt16Le(b, 6, accelZRaw)
        writeInt16Le(b, 8, gyroXRaw)
        writeInt16Le(b, 10, gyroYRaw)
        writeInt16Le(b, 12, gyroZRaw)
        writeInt16Le(b, 14, rollRaw)
        writeInt16Le(b, 16, pitchRaw)
        writeInt16Le(b, 18, yawRaw)
        return b
    }

    private fun writeInt16Le(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun registerFrame(address: Int, rawValue: Int): ByteArray {
        val b = ByteArray(20)
        b[0] = 0x55
        b[1] = 0x71
        b[2] = address.toByte()
        // offset 3 reserved
        writeInt16Le(b, 4, rawValue)
        // remainder zero
        return b
    }
}
