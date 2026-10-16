package org.scottishtecharmy.soundscape.platform

import org.scottishtecharmy.soundscape.screens.onboarding.language.androidAppContext

private val CR_OR_CRLF = Regex("\r\n|\r")

actual fun readResourceText(path: String): String {
    val classLoader =
        (Thread.currentThread().contextClassLoader ?: ResourceReader::class.java.classLoader)
            ?: error("No class loader available to read resource $path")
    val raw = classLoader.getResourceAsStream(path)?.use { stream ->
        stream.bufferedReader().readText()
    } ?: androidAppContext?.assets?.open(path)?.use { stream ->
        stream.bufferedReader().readText()
    } ?: error("Resource not found: $path")
    return CR_OR_CRLF.replace(raw, "\n")
}

private object ResourceReader
