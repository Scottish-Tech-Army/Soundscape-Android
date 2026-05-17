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
    fun ignoresNonActiveHeader() {
        // 0x55 0x71 is the register-response packet — parser should skip it
        // (no IMU sample available) and pick up the next active frame.
        val parser = WitMotionFrameParser()
        val regFrame = ByteArray(20).also {
            it[0] = 0x55; it[1] = 0x71
        }
        val active = activeFrame(0, 0, 16384)
        val out = parser.consume(regFrame + active, 0L)
        assertEquals(1, out.size)
        assertNear(90.0, out[0].yawDegrees)
    }

    @Test
    fun emptyInputProducesNothing() {
        val parser = WitMotionFrameParser()
        assertTrue(parser.consume(ByteArray(0), 0L).isEmpty())
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.01) {
        assertTrue(
            abs(expected - actual) < tolerance,
            "expected $expected, got $actual"
        )
    }

    private fun activeFrame(rollRaw: Int, pitchRaw: Int, yawRaw: Int): ByteArray {
        val b = ByteArray(20)
        b[0] = 0x55
        b[1] = 0x61
        // accel + gyro left as zero (offsets 2-13)
        writeInt16Le(b, 14, rollRaw)
        writeInt16Le(b, 16, pitchRaw)
        writeInt16Le(b, 18, yawRaw)
        return b
    }

    private fun writeInt16Le(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
