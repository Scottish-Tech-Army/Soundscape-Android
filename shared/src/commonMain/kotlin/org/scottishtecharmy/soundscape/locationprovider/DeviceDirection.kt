package org.scottishtecharmy.soundscape.locationprovider

import kotlin.math.acos

data class DeviceDirection(
    val attitude: FloatArray,
    val headingDegrees: Float,
    val headingAccuracyDegrees: Float,
    val elapsedRealtimeNanos: Long
) {
    class Builder(
        private val attitude: FloatArray,
        private val headingDegrees: Float,
        private val headingAccuracyDegrees: Float,
        private val elapsedRealtimeNanos: Long
    ) {
        fun build(): DeviceDirection {
            return DeviceDirection(
                attitude = attitude.copyOf(),
                headingDegrees = headingDegrees,
                headingAccuracyDegrees = headingAccuracyDegrees,
                elapsedRealtimeNanos = elapsedRealtimeNanos
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DeviceDirection) return false

        if (!attitude.contentEquals(other.attitude)) return false
        if (headingDegrees != other.headingDegrees) return false
        if (headingAccuracyDegrees != other.headingAccuracyDegrees) return false
        if (elapsedRealtimeNanos != other.elapsedRealtimeNanos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attitude.contentHashCode()
        result = 31 * result + headingDegrees.hashCode()
        result = 31 * result + headingAccuracyDegrees.hashCode()
        result = 31 * result + elapsedRealtimeNanos.hashCode()
        return result
    }
}

private fun Float.toDegrees(): Float {
    return (this * 180 / kotlin.math.PI).toFloat()
}

fun phoneHeldFlat(deviceOrientation: DeviceDirection?) : Boolean {
    if(deviceOrientation == null) return false

    val attitude = deviceOrientation.attitude

    val gravityZ = 1 - 2 * (attitude[0] * attitude[0] + attitude[1] * attitude[1])
    val angleToZ = acos(gravityZ).toDegrees()

    return (angleToZ < 20)
}
