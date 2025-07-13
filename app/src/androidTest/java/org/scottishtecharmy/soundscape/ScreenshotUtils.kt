package org.scottishtecharmy.soundscape

import android.content.Context
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.test.uiautomator.UiDevice

object ScreenshotUtils {

    // Screenshots are stored in:
    // /storage/emulated/0/Android/data/org.scottishtecharmy.soundscape/files/Pictures/screenshots/$filename.png
    fun captureAndSaveScreenshot(
        context: Context,
        filename: String
    ): String {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val screenshotDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "screenshots"
        )
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs()
        }

        val file = File(screenshotDir, "$filename.png")
        try {
            device.takeScreenshot(file)
            println("Screenshot saved to: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            System.err.println("Error saving screenshot: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw to fail the test if saving fails
        }
    }
}
