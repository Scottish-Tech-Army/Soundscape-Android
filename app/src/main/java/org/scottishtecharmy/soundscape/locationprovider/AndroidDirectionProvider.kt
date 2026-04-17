package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2

class AndroidDirectionProvider(context: Context) : DirectionProvider() {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private var lastUpdateTime = 0L
    private var lastUpdateValue = 0.0

    private val rotationMatrix = FloatArray(9)

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

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun processRotationVector(rotationVector: FloatArray, timestampNanos: Long) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        val headingDegrees = computeHeadingFromRotationMatrix(rotationMatrix)

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
            val headingDegrees = computeHeadingFromRotationMatrix(rotationMatrix)
            val quaternion = rotationMatrixToQuaternion(rotationMatrix)
            emitOrientation(headingDegrees, quaternion, timestampNanos)
        }
    }

    private fun computeHeadingFromRotationMatrix(rotationMatrix: FloatArray): Float {
        val deviceZVertical = rotationMatrix[8]

        val worldX: Float
        val worldY: Float

        if (abs(deviceZVertical) > 0.7f) {
            worldX = rotationMatrix[1]
            worldY = rotationMatrix[4]
        } else {
            worldX = -rotationMatrix[2]
            worldY = -rotationMatrix[5]
        }

        var headingDegrees = Math.toDegrees(atan2(worldX.toDouble(), worldY.toDouble())).toFloat()
        if (headingDegrees < 0) {
            headingDegrees += 360f
        }
        return headingDegrees
    }

    private fun rotationMatrixToQuaternion(rotationMatrix: FloatArray): FloatArray {
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

        val delta = newHeading - lastUpdateValue
        if (abs(delta) > 1.0) {
            val now = System.currentTimeMillis()
            if ((now - lastUpdateTime) > 20) {
                val orientation = DeviceDirection(
                    attitude = quaternion,
                    headingDegrees = headingDegrees,
                    headingAccuracyDegrees = 0f,
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

    fun start() {
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                sensorEventListener,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
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
