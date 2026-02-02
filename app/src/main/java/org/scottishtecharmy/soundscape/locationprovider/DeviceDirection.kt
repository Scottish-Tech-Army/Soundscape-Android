package org.scottishtecharmy.soundscape.locationprovider

/**
 * Custom DeviceOrientation class to replace Google Play Services DeviceOrientation.
 * This allows the app to run on devices without Google Play Services.
 *
 * @property attitude Quaternion representing device orientation [x, y, z, w]
 * @property headingDegrees Heading in degrees (0-360, where 0 is north)
 * @property headingAccuracyDegrees Estimated heading accuracy in degrees
 * @property elapsedRealtimeNanos Timestamp in nanoseconds
 */
data class DeviceDirection(
    val attitude: FloatArray,
    val headingDegrees: Float,
    val headingAccuracyDegrees: Float,
    val elapsedRealtimeNanos: Long
) {
    /**
     * Builder class to match the Google Play Services DeviceOrientation.Builder pattern
     */
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
        if (javaClass != other?.javaClass) return false

        other as DeviceDirection

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
