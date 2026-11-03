package org.scottishtecharmy.soundscape.locationprovider

/**
 * Everything a [CalibratedHeadTrackingProvider] is given, as text, so that a recorded walk can be
 * run through it again with exactly the same result - see [HeadTrackerReplay]. One event per
 * line, each starting with a letter and the time in milliseconds the provider saw it at:
 *
 * - `S <t>` the tracker connected, which restarts calibration
 * - `E <t>` the connection ended
 * - `B <t> <hex>` one BLE notification, its bytes in hex (WitMotion, Bose Frames)
 * - `Y <t> <yaw>` one yaw sample in degrees, from a tracker that works out its own (AirPods)
 * - `P <t> <heading> <accuracy> <qx> <qy> <qz> <qw>` the phone's heading and attitude, or
 *   `P <t> -` when it has none
 * - `L <t> <bearing> <speed> <bearingAccuracy>` a filtered location's course, with `-` for a
 *   field it didn't carry, or `L <t> -` for no location. Recordings made before bearing accuracy
 *   was used have no fourth field.
 * - `G <t>` events were lost before this one, so a replay can't match from here on
 *
 * Numbers are written with Kotlin's shortest round-tripping form, so they read back bit for bit.
 *
 * Recording costs a string per input, 25 to 100 a second, so the log only records while
 * something is draining it: [drain] arms it, [stop] stops it, and if it isn't drained for
 * [IDLE_MILLIS] it clears itself and stops. Not thread safe: the provider holds its own lock
 * around every call, which is also what keeps the events in the order the provider acted on them.
 */
class HeadTrackerInputLog {
    private val events = StringBuilder()
    private var armed = false
    private var lastDrainMillis = 0L
    private var lost = false

    fun connected(t: Long) = add(t) { append("S ").append(t) }

    fun ended(t: Long) = add(t) { append("E ").append(t) }

    fun bytes(t: Long, bytes: ByteArray) = add(t) {
        append("B ").append(t).append(' ')
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append(HEX[v shr 4]).append(HEX[v and 0xF])
        }
    }

    fun yaw(t: Long, yawDegrees: Double) = add(t) { append("Y ").append(t).append(' ').append(yawDegrees) }

    fun direction(t: Long, dir: DeviceDirection?) = add(t) {
        append("P ").append(t)
        if (dir == null) {
            append(" -")
        } else {
            append(' ').append(dir.headingDegrees).append(' ').append(dir.headingAccuracyDegrees)
            for (q in dir.attitude) append(' ').append(q)
        }
    }

    fun location(t: Long, loc: SoundscapeLocation?) = add(t) {
        append("L ").append(t)
        if (loc == null) {
            append(" -")
        } else {
            append(' ').append(if (loc.hasBearing) loc.bearing.toString() else "-")
            append(' ').append(if (loc.hasSpeed) loc.speed.toString() else "-")
            append(' ').append(
                if (loc.hasBearingAccuracy) loc.bearingAccuracyDegrees.toString() else "-"
            )
        }
    }

    /** The events since the last drain, or null if there are none. Starts the log recording. */
    fun drain(nowMillis: Long): String? {
        armed = true
        lastDrainMillis = nowMillis
        if (events.isEmpty()) return null
        return events.toString().also { events.clear() }
    }

    /** Stops recording and discards anything not yet drained, until [drain] is next called. */
    fun stop() {
        armed = false
        events.clear()
        lost = false
    }

    private inline fun add(t: Long, line: StringBuilder.() -> Unit) {
        if (!armed) return
        if (t - lastDrainMillis > IDLE_MILLIS) {
            // Nobody is reading it any more. Whatever is drained next starts after a gap.
            armed = false
            events.clear()
            lost = true
            return
        }
        if (lost) {
            events.append("G ").append(t).append('\n')
            lost = false
        }
        events.line()
        events.append('\n')
    }

    companion object {
        /** How long the log keeps recording without being drained: GPS fixes come every second. */
        const val IDLE_MILLIS = 30_000L
        private const val HEX = "0123456789abcdef"

        /** Reads back a `P` event's fields. */
        fun parseDirection(fields: List<String>): DeviceDirection? {
            if (fields[2] == "-") return null
            return DeviceDirection(
                attitude = FloatArray(fields.size - 4) { fields[4 + it].toFloat() },
                headingDegrees = fields[2].toFloat(),
                headingAccuracyDegrees = fields[3].toFloat(),
                elapsedRealtimeNanos = 0L,
            )
        }

        /** Reads back an `L` event's fields. */
        fun parseLocation(fields: List<String>): SoundscapeLocation? {
            if (fields[2] == "-" && fields.size == 3) return null
            val bearing = fields[2].takeIf { it != "-" }?.toFloat()
            val speed = fields[3].takeIf { it != "-" }?.toFloat()
            val accuracy = fields.getOrNull(4)?.takeIf { it != "-" }?.toFloat()
            return SoundscapeLocation(
                bearing = bearing ?: 0f,
                speed = speed ?: 0f,
                bearingAccuracyDegrees = accuracy ?: 0f,
                hasBearing = bearing != null,
                hasSpeed = speed != null,
                hasBearingAccuracy = accuracy != null,
            )
        }

        /** Reads back a `B` event's bytes. */
        fun parseBytes(hex: String): ByteArray =
            ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
