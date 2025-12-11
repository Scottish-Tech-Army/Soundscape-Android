package org.scottishtecharmy.soundscape

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.scottishtecharmy.soundscape.utils.LogcatHelper
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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

        var filename: String?
        runBlocking {
            filename = LogcatHelper.saveLogcatToFile(appContext)
        }
        assertNotNull(filename)
        val file = File(filename!!)
        assertEquals(true, file.length() > 0)

        val unzip = ZipInputStream(file.inputStream())
        var zipEntry: ZipEntry? = unzip.nextEntry
        var count = 0
        while (zipEntry != null) {
            ++count

            // Read the zip file entry
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesInEntry = 0L

            // Read from the stream in chunks until it's exhausted (-1)
            while (unzip.read(buffer).also { bytesRead = it } != -1) {
                totalBytesInEntry += bytesRead
            }
            assertTrue(
                "The content of the zip entry '${zipEntry.name}' should not be empty.",
                totalBytesInEntry > 0
            )
            zipEntry = unzip.nextEntry
        }
        assertEquals(1, count)
    }
}