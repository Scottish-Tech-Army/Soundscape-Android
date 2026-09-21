package org.scottishtecharmy.soundscape.platform

actual fun readResourceText(path: String): String {
    val classLoader =
        (Thread.currentThread().contextClassLoader ?: ResourceReader::class.java.classLoader)
            ?: error("No class loader available to read resource $path")
    val stream = classLoader.getResourceAsStream(path)
        ?: error("Resource not found: $path")
    return stream.bufferedReader().readText()
}

private object ResourceReader
