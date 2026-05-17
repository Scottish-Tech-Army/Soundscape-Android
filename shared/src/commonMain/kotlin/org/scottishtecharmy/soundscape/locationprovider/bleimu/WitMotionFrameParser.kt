package org.scottishtecharmy.soundscape.locationprovider.bleimu

/**
 * Decoded sample from a WT9011DCL active-IMU frame (header 0x55 0x61).
 * Angles are in degrees, in the sensor's own (relative) frame — yaw drifts
 * from boot, so geographic heading needs a calibration step.
 */
data class WitMotionSample(
    val rollDegrees: Double,
    val pitchDegrees: Double,
    val yawDegrees: Double,
    val timestampMillis: Long,
)

/**
 * Stateful byte-stream parser for the WitMotion BLE 5.0 protocol.
 * BLE notifications arrive as arbitrary byte chunks — frames may split across
 * chunks, and a stale byte mid-stream must not desync the parser. Sync is
 * recovered by walking forward until a 0x55 0x61 header is found.
 */
class WitMotionFrameParser {

    private val buffer = ArrayDeque<Byte>()

    /**
     * Feed bytes from a notification. Returns any frames decoded as a result.
     * [timestampMillis] is stamped onto every sample produced by this call.
     */
    fun consume(bytes: ByteArray, timestampMillis: Long): List<WitMotionSample> {
        if (bytes.isEmpty()) return emptyList()
        for (b in bytes) buffer.addLast(b)
        val out = mutableListOf<WitMotionSample>()
        while (true) {
            val sample = tryDecodeOne(timestampMillis) ?: break
            out.add(sample)
        }
        return out
    }

    private fun tryDecodeOne(timestampMillis: Long): WitMotionSample? {
        // Resync: discard bytes until the buffer starts with the active-IMU header.
        while (buffer.size >= 2) {
            if (buffer[0] == HEADER_0 && buffer[1] == HEADER_1_ACTIVE) break
            buffer.removeFirst()
        }
        if (buffer.size < FRAME_LEN) return null

        val frame = ByteArray(FRAME_LEN) { buffer[it] }
        repeat(FRAME_LEN) { buffer.removeFirst() }

        val roll = int16Le(frame, 14) * ANGLE_SCALE
        val pitch = int16Le(frame, 16) * ANGLE_SCALE
        val yaw = int16Le(frame, 18) * ANGLE_SCALE
        return WitMotionSample(roll, pitch, yaw, timestampMillis)
    }

    private fun int16Le(b: ByteArray, offset: Int): Double {
        val lo = b[offset].toInt() and 0xFF
        val hi = b[offset + 1].toInt()
        return ((hi shl 8) or lo).toShort().toDouble()
    }

    fun reset() {
        buffer.clear()
    }

    companion object {
        const val FRAME_LEN = 20
        const val HEADER_0: Byte = 0x55
        const val HEADER_1_ACTIVE: Byte = 0x61

        // Euler angles: int16 / 32768 * 180 -> degrees
        private const val ANGLE_SCALE = 180.0 / 32768.0
    }
}
