package org.scottishtecharmy.soundscape.utils

import okio.FileSystem
import okio.Path.Companion.toPath

fun findExtractPaths(path: String): List<String> {
    val dir = path.toPath()
    return try {
        FileSystem.SYSTEM.list(dir)
            .filter { it.name.endsWith(".pmtiles") }
            .map { it.toString() }
    } catch (_: Exception) {
        emptyList()
    }
}
