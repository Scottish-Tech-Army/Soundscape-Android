package org.scottishtecharmy.soundscape.locationprovider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

/**
 * Step detector and step counter, both optional hardware: a phone may have either, both or
 * neither. Both need ACTIVITY_RECOGNITION, which the onboarding permissions screen
 * (NavigatingScreen) requires on any phone that has one - see [hasStepSensor]. Until it's granted
 * [snapshot] returns null and tries again on the next fix.
 */
class AndroidStepProvider(private val context: Context) : StepProvider {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val detector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val counter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val state = AtomicReference<StepSnapshot?>(null)
    private var started = false

    // TYPE_STEP_COUNTER counts from boot, so the first value seen is the baseline
    private var counterBaseline: Long? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val millis = eventWallClockMillis(event.timestamp)
            when (event.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> state.updateAndGet { old ->
                    val current = old ?: StepSnapshot()
                    current.copy(
                        detectedSteps = (current.detectedSteps ?: 0L) + 1,
                        lastStepMillis = millis,
                    )
                }
                Sensor.TYPE_STEP_COUNTER -> {
                    val total = event.values[0].toLong()
                    // Registering delivers the sensor's last value straight away, stamped with
                    // whenever it was produced - hours ago, if the phone hasn't walked since. It
                    // is only the baseline, not a time anything was counted.
                    val baseline = counterBaseline
                    if (baseline == null) {
                        counterBaseline = total
                        state.updateAndGet { old -> (old ?: StepSnapshot()).copy(counterSteps = 0) }
                        return
                    }
                    state.updateAndGet { old ->
                        (old ?: StepSnapshot()).copy(
                            counterSteps = total - baseline,
                            counterMillis = millis,
                        )
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    @Synchronized
    override fun snapshot(): StepSnapshot? {
        if (!started) start()
        return state.get()
    }

    private fun start() {
        if (detector == null && counter == null) return
        if (context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        // No batching: the point is to see how quickly steps show up
        detector?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST, 0) }
        counter?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST, 0) }
        started = true
    }

    @Synchronized
    override fun stop() {
        if (started) sensorManager.unregisterListener(listener)
        started = false
        counterBaseline = null
        state.set(null)
    }

    companion object {
        fun hasStepSensor(context: Context): Boolean {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return (manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) ||
                (manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null)
        }
    }

    // SensorEvent.timestamp is on the elapsedRealtimeNanos clock
    private fun eventWallClockMillis(eventNanos: Long): Long =
        System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - eventNanos) / 1_000_000L
}
