package org.scottishtecharmy.soundscape.platform

import org.scottishtecharmy.soundscape.screens.onboarding.language.androidAppContext

actual fun readResourceText(path: String): String {
    val classLoader =
        (Thread.currentThread().contextClassLoader ?: ResourceReader::class.java.classLoader)
            ?: error("No class loader available to read resource $path")
    classLoader.getResourceAsStream(path)?.let { stream ->
        return stream.bufferedReader().readText()
    }
    // TODO 2026-10-02 HughG: Remove this when we remove regression files, before merge.
    // Help markdown files live in app/src/main/assets/ — Android assets, not classpath
    // resources. Fall back to AssetManager via the app context installed at startup.
    androidAppContext?.assets?.open(path)?.use { stream ->
        return stream.bufferedReader().readText()
    }
    error("Resource not found: $path")
}

private object ResourceReader
