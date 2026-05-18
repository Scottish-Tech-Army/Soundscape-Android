package org.scottishtecharmy.soundscape.locationprovider.bleimu

/** 3-vector helper, used for the accel (g) and gyro (deg/s) channels. */
data class Vec3(val x: Double, val y: Double, val z: Double)

/**
 * Decoded sample from a WT9011DCL active-IMU frame (header 0x55 0x61).
 *
 * - [accelG] — accelerometer in g (gravity + any linear acceleration). When
 *   the sensor is roughly still this points along world-up in the body frame.
 * - [gyroDps] — gyroscope in deg/s about each body axis.
 * - [rollDegrees] / [pitchDegrees] / [yawDegrees] — the sensor's fused Euler
 *   angles. Yaw is **about the body Z axis**, so it's only a useful heading
 *   proxy when the sensor is mounted with its Z aligned with gravity.
 *   For mount-independent heading, integrate `gyro · up_in_body` instead.
 */
data class WitMotionSample(
    val accelG: Vec3,
    val gyroDps: Vec3,
    val rollDegrees: Double,
    val pitchDegrees: Double,
    val yawDegrees: Double,
    val timestampMillis: Long,
)

/**
 * Decoded register-read response (header 0x55 0x71).
 * [address] is the register that was queried (e.g. 0x64 for battery voltage).
 * [rawValue] is the first int16 LE following the header — interpretation
 * depends on which register was read.
 */
data class WitMotionRegisterResponse(
    val address: Int,
    val rawValue: Int,
    val timestampMillis: Long,
)

/**
 * Stateful byte-stream parser for the WitMotion BLE 5.0 protocol.
 * BLE notifications arrive as arbitrary byte chunks — frames may split across
 * chunks, and a stale byte mid-stream must not desync the parser. Sync is
 * recovered by walking forward until either a 0x55 0x61 (active IMU) or
 * 0x55 0x71 (register response) header is found.
 *
 * IMU frames are returned from [consume]. Register responses are pushed to
 * [onRegisterResponse] inline — they're sporadic, so a callback keeps the
 * common case (per-sample collection) free of allocations and noise.
 */
class WitMotionFrameParser(
    private val onRegisterResponse: (WitMotionRegisterResponse) -> Unit = {},
) {

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
            // Resync to either valid frame header.
            while (buffer.size >= 2) {
                val b1 = buffer[1]
                if (buffer[0] == HEADER_0 && (b1 == HEADER_1_ACTIVE || b1 == HEADER_1_REGISTER)) break
                buffer.removeFirst()
            }
            if (buffer.size < FRAME_LEN) break

            val isImu = buffer[1] == HEADER_1_ACTIVE
            val frame = ByteArray(FRAME_LEN) { buffer[it] }
            repeat(FRAME_LEN) { buffer.removeFirst() }

            if (isImu) {
                out.add(decodeImuSample(frame, timestampMillis))
            } else {
                onRegisterResponse(decodeRegisterResponse(frame, timestampMillis))
            }
        }
        return out
    }

    private fun decodeImuSample(frame: ByteArray, timestampMillis: Long): WitMotionSample {
        val accel = Vec3(
            int16Le(frame, 2) * ACCEL_SCALE,
            int16Le(frame, 4) * ACCEL_SCALE,
            int16Le(frame, 6) * ACCEL_SCALE,
        )
        val gyro = Vec3(
            int16Le(frame, 8) * GYRO_SCALE,
            int16Le(frame, 10) * GYRO_SCALE,
            int16Le(frame, 12) * GYRO_SCALE,
        )
        val roll = int16Le(frame, 14) * ANGLE_SCALE
        val pitch = int16Le(frame, 16) * ANGLE_SCALE
        val yaw = int16Le(frame, 18) * ANGLE_SCALE
        return WitMotionSample(accel, gyro, roll, pitch, yaw, timestampMillis)
    }

    private fun decodeRegisterResponse(
        frame: ByteArray,
        timestampMillis: Long,
    ): WitMotionRegisterResponse = WitMotionRegisterResponse(
        address = frame[2].toInt() and 0xFF,
        rawValue = int16Le(frame, 4).toInt(),
        timestampMillis = timestampMillis,
    )

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
        const val HEADER_1_REGISTER: Byte = 0x71

        // Euler angles: int16 / 32768 * 180 -> degrees
        private const val ANGLE_SCALE = 180.0 / 32768.0
        // Accelerometer: int16 / 32768 * 16 -> g
        private const val ACCEL_SCALE = 16.0 / 32768.0
        // Gyroscope: int16 / 32768 * 2000 -> deg/s
        private const val GYRO_SCALE = 2000.0 / 32768.0
    }
}
