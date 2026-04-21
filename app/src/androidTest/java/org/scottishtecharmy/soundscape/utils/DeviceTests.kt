package org.scottishtecharmy.soundscape.utils

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
    @Test
    fun deviceTest_AndroidDevice_SdkVersion() {
        val buildSdkVersion = Build.VERSION.SDK_INT

        val device = AndroidDevice(Build.VERSION.SDK_INT.toString())
        assert(device.platform() == Platform.Android)
        assert(device.osSdkVersionNumber().toInt() == buildSdkVersion)
    }
}