package org.scottishtecharmy.soundscape.locationprovider.bleimu

import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * BLE client for the WitMotion WT9011DCL IMU.
 * Auto-discovers any "WT*" device, connects, raises the IMU stream rate to
 * 100 Hz, and exposes the raw notify byte stream so a parser layer can decode
 * it. The sensor uses a non-standard 128-bit base UUID (`...9a34fb` rather
 * than the SIG `...9b34fb`), so the full UUID strings are mandatory.
 */
@OptIn(ExperimentalUuidApi::class)
class BleImuClient {

    private val serviceUuid = Uuid.parse(SERVICE_UUID)
    private val dataCharacteristic = characteristicOf(serviceUuid, Uuid.parse(NOTIFY_CHAR_UUID))
    private val commandCharacteristic = characteristicOf(serviceUuid, Uuid.parse(WRITE_CHAR_UUID))

    /**
     * Scan, connect to the first matching peripheral, configure it, and stream
     * notifications. Suspends for the lifetime of the connection; cancel the
     * calling coroutine to disconnect. Each emission from [onBytes] is one raw
     * BLE notification.
     */
    suspend fun runSession(
        onConnected: () -> Unit,
        onBytes: suspend (ByteArray) -> Unit,
    ) {
        val advertisement = Scanner {
            filters {
                match { name = Filter.Name.Prefix(DEVICE_NAME_PREFIX) }
            }
        }.advertisements.first()

        val peripheral = Peripheral(advertisement)
        peripheral.connect()
        try {
            configureForHeadTracking(peripheral)
            onConnected()
            coroutineScope {
                // Sibling coroutine periodically asks the sensor for its battery
                // voltage. The reply arrives on the notify characteristic and is
                // routed to the parser like any other frame.
                launch {
                    while (coroutineContext.isActive) {
                        peripheral.write(
                            commandCharacteristic,
                            readRegisterCommand(REG_BATTERY_VOLTAGE),
                            WriteType.WithoutResponse,
                        )
                        delay(BATTERY_POLL_INTERVAL_MILLIS)
                    }
                }
                peripheral.observe(dataCharacteristic).collect { bytes ->
                    onBytes(bytes)
                }
            }
        } finally {
            peripheral.disconnect()
        }
    }

    private fun readRegisterCommand(register: Int): ByteArray = byteArrayOf(
        0xFF.toByte(),
        0xAA.toByte(),
        0x27,
        register.toByte(),
        0x00,
    )

    private suspend fun configureForHeadTracking(peripheral: Peripheral) {
        // Raise the output rate to 100 Hz (default is 10 Hz). The sensor needs
        // a brief settle, then a SAVE command to persist the rate to flash so
        // the next connection starts fast.
        peripheral.write(commandCharacteristic, cmd(REG_OUTPUT_RATE, RATE_100_HZ), WriteType.WithoutResponse)
        delay(CONFIG_SETTLE_MILLIS)
        peripheral.write(commandCharacteristic, CMD_SAVE, WriteType.WithoutResponse)
    }

    private fun cmd(register: Int, value: Int): ByteArray = byteArrayOf(
        0xFF.toByte(),
        0xAA.toByte(),
        register.toByte(),
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
    )

    companion object {
        // Custom WitMotion base UUID — note 9a34fb, NOT the SIG 9b34fb.
        const val SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9a34fb"
        const val NOTIFY_CHAR_UUID = "0000ffe4-0000-1000-8000-00805f9a34fb"
        const val WRITE_CHAR_UUID = "0000ffe9-0000-1000-8000-00805f9a34fb"

        const val DEVICE_NAME_PREFIX = "WT"

        private const val REG_OUTPUT_RATE = 0x03
        private const val RATE_100_HZ = 0x09
        private val CMD_SAVE = byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x00, 0x00, 0x00)
        private const val CONFIG_SETTLE_MILLIS = 100L

        /** Register that holds battery voltage in 0.01 V units (e.g. 410 = 4.10 V). */
        const val REG_BATTERY_VOLTAGE = 0x64

        // Battery level barely changes minute-to-minute; polling every 30 s is
        // plenty to drive a UI indicator and is negligible RF load.
        private const val BATTERY_POLL_INTERVAL_MILLIS = 30_000L
    }
}
