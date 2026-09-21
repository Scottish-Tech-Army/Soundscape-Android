package org.scottishtecharmy.soundscape.locationprovider.bose

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoseFramesDataParserTest {

    @Test
    fun decodesRotationQuaternionYaw() {
        // Identity quaternion (w=1, x=y=z=0) -> yaw = 0
        val payload = quatRecord(sensorId = 2, x = 0.0, y = 0.0, z = 0.0, w = 1.0, accuracy = 0)
        val sample = BoseFramesDataParser.parse(payload, timestampMillis = 7L)
        assertNotNull(sample)
        assertNear(0.0, sample.yawDegrees)
        assertEquals(7L, sample.timestampMillis)
    }

    @Test
    fun decodes90DegreeYawAboutZ() {
        // Quaternion for 90° about +Z: w = cos(45°), z = sin(45°) -> yaw = 90°
        val s = kotlin.math.sin(kotlin.math.PI / 4)
        val payload = quatRecord(sensorId = 2, x = 0.0, y = 0.0, z = s, w = s, accuracy = 0)
        val sample = BoseFramesDataParser.parse(payload, 0L)
        assertNotNull(sample)
        assertNear(90.0, sample.yawDegrees, tolerance = 0.1)
    }

    @Test
    fun ignoresVectorRecordsAndReturnsLastQuat() {
        // accel vector then rotation quaternion — should still return the quaternion yaw.
        val accel = vectorRecord(sensorId = 0, x = 0.1, y = 0.2, z = 0.3, accuracy = 1)
        val s = kotlin.math.sin(kotlin.math.PI / 4)
        val quat = quatRecord(sensorId = 2, x = 0.0, y = 0.0, z = s, w = s, accuracy = 0)
        val sample = BoseFramesDataParser.parse(accel + quat, 0L)
        assertNotNull(sample)
        assertNear(90.0, sample.yawDegrees, tolerance = 0.1)
    }

    @Test
    fun returnsNullForVectorOnlyPayload() {
        val accel = vectorRecord(sensorId = 0, x = 0.1, y = 0.2, z = 0.3, accuracy = 1)
        assertNull(BoseFramesDataParser.parse(accel, 0L))
    }

    @Test
    fun returnsNullForEmptyPayload() {
        assertNull(BoseFramesDataParser.parse(ByteArray(0), 0L))
    }

    @Test
    fun bailsOnUnknownSensorIdWithoutCrashing() {
        val bogus = ByteArray(10) { it.toByte() } // first byte = 0 (accel), but length wrong
        // 0 = accel needs 3 (header) + 6 + 1 = 10 bytes — should parse OK.
        // Let's use sensorId 99 (unknown) to trigger the bail path:
        val unknown = ByteArray(10).apply {
            this[0] = 99 // unknown sensor id
        }
        assertNull(BoseFramesDataParser.parse(unknown, 0L))
    }

    @Test
    fun configWriteEncodesRotationOnly() {
        val cfg = boseEnableRotationOnlyConfig(rotationPeriodMs = 40)
        // 7 entries × 3 bytes each
        assertEquals(21, cfg.size)
        for (id in 0..6) {
            val base = id * 3
            assertEquals(id.toByte(), cfg[base])
            val period = ((cfg[base + 1].toInt() and 0xFF) shl 8) or (cfg[base + 2].toInt() and 0xFF)
            val expectedPeriod = if (id == 2) 40 else 0
            assertEquals(expectedPeriod, period, "sensor $id period")
        }
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.05) {
        assertTrue(abs(expected - actual) < tolerance, "expected $expected, got $actual")
    }

    private fun quatRecord(
        sensorId: Int,
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        accuracy: Short,
    ): ByteArray {
        val out = ByteArray(3 + 8 + 2)
        out[0] = sensorId.toByte()
        // timestamp (don't care)
        writeInt16Be(out, 1, 0)
        writeQuatComponent(out, 3, x)
        writeQuatComponent(out, 5, y)
        writeQuatComponent(out, 7, z)
        writeQuatComponent(out, 9, w)
        writeInt16Be(out, 11, accuracy.toInt())
        return out
    }

    private fun vectorRecord(
        sensorId: Int,
        x: Double,
        y: Double,
        z: Double,
        accuracy: Int,
    ): ByteArray {
        val out = ByteArray(3 + 6 + 1)
        out[0] = sensorId.toByte()
        writeInt16Be(out, 1, 0)
        writeVectorComponent(out, 3, x)
        writeVectorComponent(out, 5, y)
        writeVectorComponent(out, 7, z)
        out[9] = accuracy.toByte()
        return out
    }

    private fun writeQuatComponent(out: ByteArray, offset: Int, value: Double) {
        val raw = (value * (1 shl 14)).toInt()
        writeInt16Be(out, offset, raw)
    }

    private fun writeVectorComponent(out: ByteArray, offset: Int, value: Double) {
        val raw = (value * (1 shl 12)).toInt()
        writeInt16Be(out, offset, raw)
    }

    private fun writeInt16Be(out: ByteArray, offset: Int, value: Int) {
        out[offset] = ((value shr 8) and 0xFF).toByte()
        out[offset + 1] = (value and 0xFF).toByte()
    }
}
