package org.scottishtecharmy.soundscape.locationprovider.bose

import kotlin.math.PI
import kotlin.math.atan2

/**
 * Decoded yaw sample from a Bose Frames sensor-data notification.
 *
 * Bose's protocol streams multiple sensor samples per notification; we only
 * use the rotation quaternion, so this parser extracts the most recent one.
 */
data class BoseYawSample(val yawDegrees: Double, val timestampMillis: Long)

/**
 * Parser for the Bose Frames AR `sensorData` characteristic (notify).
 *
 * Each notification is a concatenation of records:
 *   [sensorId : u8][timestamp : u16 BE][sample bytes][accuracy bytes?]
 * Sample / accuracy sizes are determined by sensor id:
 *
 *   id 0 (accelerometer)             — 6-byte vector,  1-byte accuracy
 *   id 1 (gyroscope)                 — 6-byte vector,  1-byte accuracy
 *   id 2 (rotation, fused)           — 8-byte quat,    2-byte accuracy
 *   id 3 (game rotation)             — 8-byte quat,    (no accuracy)
 *   id 4 (orientation)               — 6-byte vector,  1-byte accuracy
 *   id 5 (magnetometer)              — 6-byte vector,  1-byte accuracy
 *   id 6 (uncalibrated magnetometer) — 6-byte vector,  (no accuracy)
 *
 * All multi-byte fields are big-endian. Quaternion components are int16 / 2^14;
 * yaw is derived from the standard quaternion → Z-axis rotation formula.
 *
 * Pure logic — unit-testable in `commonTest`.
 */
object BoseFramesDataParser {

    fun parse(bytes: ByteArray, timestampMillis: Long): BoseYawSample? {
        if (bytes.isEmpty()) return null
        var pos = 0
        var lastYaw: Double? = null

        while (pos < bytes.size) {
            // Each record needs at least sensorId + timestamp + smallest payload.
            if (pos + RECORD_HEADER_LEN > bytes.size) break
            val sensorId = bytes[pos].toInt() and 0xFF
            pos += RECORD_HEADER_LEN

            val size = recordPayloadSize(sensorId) ?: break // unknown id — bail to avoid desync
            if (pos + size > bytes.size) break

            when (sensorId) {
                ROTATION_ID, GAME_ROTATION_ID -> {
                    lastYaw = quaternionYawDegrees(bytes, pos)
                }
            }
            pos += size
        }

        return lastYaw?.let { BoseYawSample(it, timestampMillis) }
    }

    private fun recordPayloadSize(sensorId: Int): Int? = when (sensorId) {
        ACCELEROMETER_ID, GYROSCOPE_ID, ORIENTATION_ID, MAGNETOMETER_ID ->
            VECTOR_LEN + VECTOR_ACCURACY_LEN
        UNCAL_MAG_ID -> VECTOR_LEN
        ROTATION_ID -> QUAT_LEN + QUAT_ACCURACY_LEN
        GAME_ROTATION_ID -> QUAT_LEN
        else -> null
    }

    private fun quaternionYawDegrees(bytes: ByteArray, offset: Int): Double {
        val x = readQuatComponent(bytes, offset)
        val y = readQuatComponent(bytes, offset + 2)
        val z = readQuatComponent(bytes, offset + 4)
        val w = readQuatComponent(bytes, offset + 6)
        // Standard quaternion → yaw (Z-axis rotation).
        val yawRadians = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))
        return yawRadians * RAD_TO_DEG
    }

    private fun readQuatComponent(bytes: ByteArray, offset: Int): Double =
        readInt16Be(bytes, offset).toDouble() / QUAT_SCALE

    private fun readInt16Be(bytes: ByteArray, offset: Int): Int {
        val hi = bytes[offset].toInt()
        val lo = bytes[offset + 1].toInt() and 0xFF
        return ((hi shl 8) or lo).toShort().toInt()
    }

    private const val RECORD_HEADER_LEN = 3 // sensorId(1) + timestamp(2)
    private const val VECTOR_LEN = 6
    private const val QUAT_LEN = 8
    private const val VECTOR_ACCURACY_LEN = 1
    private const val QUAT_ACCURACY_LEN = 2

    private const val ACCELEROMETER_ID = 0
    private const val GYROSCOPE_ID = 1
    private const val ROTATION_ID = 2
    private const val GAME_ROTATION_ID = 3
    private const val ORIENTATION_ID = 4
    private const val MAGNETOMETER_ID = 5
    private const val UNCAL_MAG_ID = 6

    // Quaternion components are int16 fixed-point with 14 fractional bits.
    private const val QUAT_SCALE = (1 shl 14).toDouble()
    private const val RAD_TO_DEG = 180.0 / PI
}

/**
 * Build a sensor-configuration write payload for the Bose Frames
 * `sensorConfiguration` characteristic.
 *
 * Each entry is `[sensorId : u8][samplePeriodMs : u16 BE]`, period 0 = disabled.
 * The frames accept the full 7-entry list. We enable only the fused
 * rotation sensor at [rotationPeriodMs] (default 40 ms = 25 Hz).
 */
fun boseEnableRotationOnlyConfig(rotationPeriodMs: Int = 40): ByteArray {
    val out = ByteArray(7 * 3)
    for (id in 0..6) {
        val base = id * 3
        out[base] = id.toByte()
        val period = if (id == 2) rotationPeriodMs else 0
        out[base + 1] = ((period shr 8) and 0xFF).toByte()
        out[base + 2] = (period and 0xFF).toByte()
    }
    return out
}
