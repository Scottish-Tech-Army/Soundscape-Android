package org.scottishtecharmy.soundscape.locationprovider.bose

import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * BLE client for the Bose Frames AR sensor service.
 *
 * Unlike the WitMotion sensor (which is name-filtered), the Frames advertise
 * the AR service UUID, so we filter on that. After connect we write a
 * configuration enabling just the fused rotation sensor at 25 Hz (40 ms
 * period) — that's the only sensor we care about for head heading.
 */
@OptIn(ExperimentalUuidApi::class)
class BoseFramesClient {

    private val serviceUuid = Uuid.parse(SERVICE_UUID)
    private val dataCharacteristic = characteristicOf(serviceUuid, Uuid.parse(DATA_CHAR_UUID))
    private val configCharacteristic = characteristicOf(serviceUuid, Uuid.parse(CONFIG_CHAR_UUID))

    /**
     * Scan, connect, configure, then stream notification bytes via [onBytes].
     * Suspends for the lifetime of the connection — cancel the calling
     * coroutine to disconnect.
     */
    suspend fun runSession(
        onConnected: () -> Unit,
        onBytes: suspend (ByteArray) -> Unit,
    ) {
        val advertisement = Scanner {
            filters {
                match { services = listOf(serviceUuid) }
            }
        }.advertisements.first()

        val peripheral = Peripheral(advertisement)
        peripheral.connect()
        try {
            val existing = peripheral.read(configCharacteristic)
            val patched = patchEnableRotation(existing, rotationPeriodMs = ROTATION_PERIOD_MS)
            peripheral.write(configCharacteristic, patched, WriteType.WithResponse)
            onConnected()
            peripheral.observe(dataCharacteristic).collect { bytes ->
                onBytes(bytes)
            }
        } finally {
            peripheral.disconnect()
        }
    }

    /**
     * Take the configuration the device just reported and turn on the fused
     * rotation entry (sensorId = 2) at [rotationPeriodMs], leaving every other
     * entry untouched. Preserving the original length is important — some
     * firmware versions reject writes of a different size to what they sent.
     */
    private fun patchEnableRotation(existing: ByteArray, rotationPeriodMs: Int): ByteArray {
        val out = existing.copyOf()
        var pos = 0
        while (pos + 2 < out.size) {
            val sensorId = out[pos].toInt() and 0xFF
            if (sensorId == ROTATION_SENSOR_ID) {
                out[pos + 1] = ((rotationPeriodMs shr 8) and 0xFF).toByte()
                out[pos + 2] = (rotationPeriodMs and 0xFF).toByte()
            }
            pos += 3
        }
        return out
    }

    companion object {
        // Bose AR sensor service (SIG-base UUID, custom 16-bit ID 0xFDD2).
        const val SERVICE_UUID = "0000fdd2-0000-1000-8000-00805f9b34fb"
        const val DATA_CHAR_UUID = "56a72ab8-4988-4cc8-a752-fbd1d54a953d"
        const val CONFIG_CHAR_UUID = "5af38af6-000e-404b-9b46-07f77580890b"

        // 25 Hz — high enough for responsive head tracking, gentle on the frames' battery.
        private const val ROTATION_PERIOD_MS = 40
        // Bose sensor index for the fused rotation quaternion.
        private const val ROTATION_SENSOR_ID = 2
    }
}
