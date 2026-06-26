package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Direction provider that uses Android's native SensorManager instead of
 * Google Play Services' FusedOrientationProviderClient.
 *
 * This uses the rotation vector sensor (or falls back to accelerometer + magnetometer)
 * to compute device orientation and heading.
 */
class AndroidDirectionProvider(context: Context) : DirectionProvider() {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private var lastUpdateTime = 0L
    private var lastUpdateValue = 0.0

    // For rotation vector sensor
    private val rotationMatrix = FloatArray(9)

    // For fallback accelerometer + magnetometer
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    processRotationVector(event.values, event.timestamp)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                    hasAccelerometerReading = true
                    if (hasMagnetometerReading) {
                        processAccelerometerMagnetometer(event.timestamp)
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                    hasMagnetometerReading = true
                    if (hasAccelerometerReading) {
                        processAccelerometerMagnetometer(event.timestamp)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Could track accuracy for headingAccuracyDegrees if needed
        }
    }

    private fun processRotationVector(rotationVector: FloatArray, timestampNanos: Long) {
        // Get rotation matrix from rotation vector
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        // Calculate heading as the direction the back of the phone points (matching Google Play Services).
        // The back of the phone is the -Z axis in device coordinates.
        // Transform to world coordinates using rotation matrix and compute heading from North.
        val headingDegrees = computeHeadingFromRotationMatrix(rotationMatrix)

        // Convert rotation vector to quaternion for attitude
        // SensorManager returns [w, x, y, z] format, but we need [x, y, z, w] to match
        // Google Play Services DeviceOrientation.attitude format
        val quaternion = FloatArray(4)
        SensorManager.getQuaternionFromVector(quaternion, rotationVector)
        val reorderedQuaternion = floatArrayOf(quaternion[1], quaternion[2], quaternion[3], quaternion[0])

        emitOrientation(headingDegrees, reorderedQuaternion, timestampNanos)
    }

    private fun processAccelerometerMagnetometer(timestampNanos: Long) {
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)

        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            inclinationMatrix,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            // Calculate heading as the direction the back of the phone points (matching Google Play Services)
            val headingDegrees = computeHeadingFromRotationMatrix(rotationMatrix)

            // Create quaternion from the rotation matrix (already in [x, y, z, w] format)
            val quaternion = rotationMatrixToQuaternion(rotationMatrix)

            emitOrientation(headingDegrees, quaternion, timestampNanos)
        }
    }

    /**
     * Compute heading to match Google Play Services DeviceOrientation.headingDegrees behavior.
     *
     * When the phone is held upright (screen facing user), heading is the direction
     * the back of the phone points (-Z axis).
     *
     * When the phone is held flat (screen facing up), heading is the direction
     * the top of the phone points (Y axis).
     */
    private fun computeHeadingFromRotationMatrix(rotationMatrix: FloatArray): Float {
        // The rotation matrix R transforms device coordinates to world coordinates.
        // World coordinates: X = East, Y = North, Z = Up
        //
        // R[2][2] (rotationMatrix[8]) indicates how vertical the phone's Z axis is:
        // - When phone is flat (screen up): R[2][2] ≈ 1 (device Z points to world Z/up)
        // - When phone is upright: R[2][2] ≈ 0 (device Z is horizontal)
        val deviceZVertical = rotationMatrix[8]

        val worldX: Float
        val worldY: Float

        if (abs(deviceZVertical) > 0.7f) {
            // Phone is relatively flat - use Y axis (top of phone) direction
            // Device Y axis in world coordinates:
            //   world_x = R[0][1] = rotationMatrix[1]
            //   world_y = R[1][1] = rotationMatrix[4]
            worldX = rotationMatrix[1]
            worldY = rotationMatrix[4]
        } else {
            // Phone is upright - use -Z axis (back of phone) direction
            // Device -Z axis in world coordinates:
            //   world_x = -R[0][2] = -rotationMatrix[2]
            //   world_y = -R[1][2] = -rotationMatrix[5]
            worldX = -rotationMatrix[2]
            worldY = -rotationMatrix[5]
        }

        // Heading is angle from North (Y) clockwise to East (X)
        var headingDegrees = Math.toDegrees(atan2(worldX.toDouble(), worldY.toDouble())).toFloat()
        if (headingDegrees < 0) {
            headingDegrees += 360f
        }
        return headingDegrees
    }

    private fun rotationMatrixToQuaternion(rotationMatrix: FloatArray): FloatArray {
        // Convert 3x3 rotation matrix to quaternion
        // Using Shepperd's method for numerical stability
        val m00 = rotationMatrix[0]
        val m01 = rotationMatrix[1]
        val m02 = rotationMatrix[2]
        val m10 = rotationMatrix[3]
        val m11 = rotationMatrix[4]
        val m12 = rotationMatrix[5]
        val m20 = rotationMatrix[6]
        val m21 = rotationMatrix[7]
        val m22 = rotationMatrix[8]

        val trace = m00 + m11 + m22

        val quaternion = FloatArray(4)

        if (trace > 0) {
            val s = 0.5f / kotlin.math.sqrt(trace + 1.0f)
            quaternion[3] = 0.25f / s
            quaternion[0] = (m21 - m12) * s
            quaternion[1] = (m02 - m20) * s
            quaternion[2] = (m10 - m01) * s
        } else if (m00 > m11 && m00 > m22) {
            val s = 2.0f * kotlin.math.sqrt(1.0f + m00 - m11 - m22)
            quaternion[3] = (m21 - m12) / s
            quaternion[0] = 0.25f * s
            quaternion[1] = (m01 + m10) / s
            quaternion[2] = (m02 + m20) / s
        } else if (m11 > m22) {
            val s = 2.0f * kotlin.math.sqrt(1.0f + m11 - m00 - m22)
            quaternion[3] = (m02 - m20) / s
            quaternion[0] = (m01 + m10) / s
            quaternion[1] = 0.25f * s
            quaternion[2] = (m12 + m21) / s
        } else {
            val s = 2.0f * kotlin.math.sqrt(1.0f + m22 - m00 - m11)
            quaternion[3] = (m10 - m01) / s
            quaternion[0] = (m02 + m20) / s
            quaternion[1] = (m12 + m21) / s
            quaternion[2] = 0.25f * s
        }

        return quaternion
    }

    private fun emitOrientation(headingDegrees: Float, quaternion: FloatArray, timestampNanos: Long) {
        val newHeading = headingDegrees.toDouble()

        // If the heading has changed by more than 1 degree and hasn't changed in the last
        // 20ms, update all interested parties.
        val delta = newHeading - lastUpdateValue
        if (abs(delta) > 1.0) {
            val now = System.currentTimeMillis()
            if ((now - lastUpdateTime) > 20) {
                // Create and emit the DeviceOrientation object
                val orientation = DeviceDirection(
                    attitude = quaternion,
                    headingDegrees = headingDegrees,
                    headingAccuracyDegrees = 0f, // Native sensors don't provide accuracy estimate
                    elapsedRealtimeNanos = timestampNanos
                )
                mutableOrientationFlow.value = orientation

                lastUpdateTime = now
                lastUpdateValue = newHeading
            }
        }
    }

    override fun destroy() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    override fun start(audio: NativeAudioEngine, locProvider: LocationProvider) {
        super.start(audio, locProvider)

        // Prefer rotation vector sensor (fuses accelerometer, magnetometer, and gyroscope)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor != null) {
            // Use rotation vector sensor (best option)
            sensorManager.registerListener(
                sensorEventListener,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_UI // ~60Hz, similar to 50Hz FusedOrientationProvider
            )
        } else {
            // Fallback to accelerometer + magnetometer
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            accelerometerSensor?.let {
                sensorManager.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            magnetometerSensor?.let {
                sensorManager.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }
    }
}
