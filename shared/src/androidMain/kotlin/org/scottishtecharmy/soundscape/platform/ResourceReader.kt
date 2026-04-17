package org.scottishtecharmy.soundscape.platform

actual fun readResourceText(path: String): String {
    val classLoader = Thread.currentThread().contextClassLoader ?: ResourceReader::class.java.classLoader
    return classLoader!!.getResourceAsStream(path)!!.bufferedReader().readText()
}

private object ResourceReader
