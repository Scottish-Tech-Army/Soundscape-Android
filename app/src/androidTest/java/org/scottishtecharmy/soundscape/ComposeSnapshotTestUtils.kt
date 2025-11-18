package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Dumps the full layout and semantics tree as a deterministic string.
 * Includes modifiers, bounds, and semantics properties.
 *
 * @author ChatGPT (via Hugh Greene)
 */
fun ComposeTestRule.dumpLayoutTree(): String {
    val sb = StringBuilder()

    fun dumpSemanticsNode(indent: String, info: SemanticsNode) {
        sb.append(indent)
            .append("- ")
            .append((info.config.getOrNull(SemanticsProperties.TestTag)?.let { "[$it] " }) ?: "[] ")
            .append(info.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: "''")
            .append("  ")
            .append(info.layoutInfo())
            .append("\n")
    }

    fun dump(info: SemanticsNode, indent: String = "") {
        dumpSemanticsNode(indent, info)

        info.children.forEach { child ->
            dump(child, indent + "  ")
        }
    }

    dump(onRoot(useUnmergedTree = true).fetchSemanticsNode())
    return sb.toString()
}

/**
 * Dumps the full layout and semantics tree as a deterministic string.
 * Includes modifiers, bounds, and semantics properties.
 *
 * @author ChatGPT (via Hugh Greene)
 */
private fun SemanticsNode.layoutInfo(): String {
    val bounds = this.boundsInRoot
    // Simplified modifier info for testing (truncate long chains)
    val modifiers = this.layoutInfo.getModifierInfo().joinToString(", ") {
        // Strip explicit object IDs, because they might appear as meaningless diffs.
        it.toString().replace(Regex("@[0-9a-f]+"), "@<id>")
    }
    return "(bounds=${bounds.toShortString()}, modifiers=[$modifiers])"
}

private fun androidx.compose.ui.geometry.Rect.toShortString(): String =
    "(${left.toInt()},${top.toInt()} - ${right.toInt()},${bottom.toInt()})"

/**
 * Hybrid baseline assertion:
 * - Reads committed baseline from assets/
 * - If missing, writes a temporary new file to context.filesDir
 * - If mismatched, prints a diff
 */
fun ComposeTestRule.assertLayoutMatchesHybridBaseline(filename: String) {
    val context = InstrumentationRegistry.getInstrumentation().context
    val baselineSubpathString = "baselines/$filename"
    val baselineText = loadBaselineFromAssets(context, baselineSubpathString)

    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val filesDir = targetContext.filesDir.toPath()

    val snapshot = dumpLayoutTree()

    if (baselineText == null) {
        // If no baseline in assets, create a new one on the Android device for review.
        val androidSideBaselineFile =
            generateAndroidSideBaselineFile(filesDir, baselineSubpathString, snapshot)
        fail("No baseline found in 'assets/$baselineSubpathString'. " +
                "A new one will be written to '${androidSideBaselineFile}' under " +
                "'src/androidTest/assets/baselines/'; review then commit it.")
    }
    else {
        // We check the result of the diff, rather than comparing the baseline and snapshot
        // directly, because the line endings may differ, and the diff ignores those.
        val diff = generateDiff(baselineText, snapshot)
        if (diff.isNotEmpty()) {
            // If baseline exists but differs, print diff and fail.
            println("\nLayout structure changed! Diff:\n")
            println(diff)
            val androidSideBaselineFile =
                generateAndroidSideBaselineFile(filesDir, baselineSubpathString, snapshot)
            println("New snapshot written to: ${androidSideBaselineFile}")
            fail("Layout structure does not match baseline. See diff above. " +
                    "An updated version will be copied into ${androidSideBaselineFile} under " +
                    "'src/androidTest/assets/baselines/'; review the changes before committing.")
        }
        else {
            println("Layout matches baseline.")
        }
    }
}

/**
 * Reads a baseline file from the Android test assets folder.
 * Returns null if not found.
 */
fun loadBaselineFromAssets(context: Context, filename: String): String? {
    val baselineInputStream = try {
        context.assets.open(filename)
    } catch (_: Exception) {
        null
    }
    return baselineInputStream?.run {
        bufferedReader().use { it.readText() }
    }
}

private fun generateAndroidSideBaselineFile(filesDir: Path, filename: String, snapshot: String): Path? {
    val androidSideBaselineFile = filesDir.resolve(filename)
    ensureAndroidSideBaselineDirExistsAndIsReadable(filesDir, androidSideBaselineFile.parent)
    androidSideBaselineFile.writeText(snapshot)
    androidSideBaselineFile.toFile().setReadable(true, false)
    return androidSideBaselineFile
}

fun ensureAndroidSideBaselineDirExistsAndIsReadable(filesDir: Path, path: Path) {

    fun Path.makeReadableAndTraversable() {
        val dir = toFile()
        dir.setReadable(true, false)
        dir.setExecutable(true, false) // so adb "shell" user can traverse it
    }

    path.createDirectories().makeReadableAndTraversable()
    // Recursively ensure all parents are world-readable and -executable.
    generateSequence(path) { it.parent }
        .takeWhile { it.startsWith(filesDir) }
        .forEach { it.makeReadableAndTraversable() }
}

/**
 * Simple line-by-line unified diff generator
 */
private fun generateDiff(expected: String, actual: String): String {
    val expectedLines = expected.lines()
    val actualLines = actual.lines()
    val diff = StringBuilder()
    val maxLines = maxOf(expectedLines.size, actualLines.size)

    for (i in 0 until maxLines) {
        val e = expectedLines.getOrNull(i) ?: ""
        val a = actualLines.getOrNull(i) ?: ""
        if (e != a) {
            diff.append(String.format("-%s\n+%s\n", e, a))
        }
    }
    return diff.toString()
}
