package org.scottishtecharmy.soundscape

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.utils.LogcatHelper
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LoggingTest {
    @Test
    fun saveLogTest() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val marker = "LoggingTest marker ${System.nanoTime()}"
        AppLog.tag("LoggingTest").d { marker }

        var filename: String?
        runBlocking {
            filename = LogcatHelper.saveLogcatToFile(appContext)
        }
        assertNotNull(filename)
        val file = File(filename!!)
        assertTrue("Log file should exist", file.exists())
        assertTrue("Log file should not be empty", file.length() > 0)

        val content = file.readText()
        assertTrue(
            "Log file should contain the marker line we just logged",
            content.contains(marker)
        )
    }
}
