package org.scottishtecharmy.soundscape.bluetooth

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reports battery level (0–100) for the most recently active Bluetooth audio
 * device, using the system broadcast Android fires when it learns a connected
 * headset's battery (via HFP `AT+IPHONEACCEV`, AVRCP `GetCapabilities`, or
 * vendor profiles).
 *
 * The broadcast action and extra strings are hidden `BluetoothDevice` constants
 * but have been stable for years — every music player on Android relies on
 * them. They're protected broadcasts so only the system can send them.
 *
 * Limitations:
 *  - Only fires for paired audio devices, not arbitrary BLE peripherals.
 *  - AirPods on Android typically don't report battery (Apple's protocol isn't
 *    parsed by the AOSP stack).
 *  - Requires the `BLUETOOTH_CONNECT` runtime permission on API 31+.
 */
class AudioHeadsetBatteryMonitor(private val context: Context) {

    private val _batteryPercentFlow = MutableStateFlow<Int?>(null)
    /** 0–100, or null until the first audio device reports. */
    val batteryPercentFlow: StateFlow<Int?> = _batteryPercentFlow

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action != ACTION_BATTERY_LEVEL_CHANGED) return
            val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
            if (level !in 0..100) return
            val device = IntentCompat.getParcelableExtra(
                intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java,
            ) ?: return
            // bluetoothClass requires BLUETOOTH_CONNECT on API 31+. The broadcast
            // itself won't reach us without that permission, but lint can't
            // prove that, so check explicitly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) return
            // Ignore non-audio devices (smartwatches, fitness trackers etc.).
            val major = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
            if (major != BluetoothClass.Device.Major.AUDIO_VIDEO) return
            _batteryPercentFlow.value = level
        }
    }

    private var started = false

    fun start() {
        if (started) return
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_BATTERY_LEVEL_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        started = true
    }

    fun stop() {
        if (!started) return
        runCatching { context.unregisterReceiver(receiver) }
        started = false
        _batteryPercentFlow.value = null
    }

    companion object {
        // BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED — @hide but stable.
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        // BluetoothDevice.EXTRA_BATTERY_LEVEL — @hide but stable.
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
