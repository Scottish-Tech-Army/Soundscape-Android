package org.scottishtecharmy.soundscape

import java.io.File

/**
 * Files that unit tests write for visual checking (GeoJSON, callout text) go under
 * build/test-output rather than the module root, so they stay out of the project view.
 */
fun testOutputFile(name: String): File =
    File("build/test-output", name).apply { parentFile.mkdirs() }
