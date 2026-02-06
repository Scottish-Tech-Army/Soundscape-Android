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
import java.nio.file.Paths
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
            .appendLine()
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

private val regexForLambdas = Regex("(\\$[0-9]+|ExternalSyntheticLambda[^@]+)+@")
private val regexForObjectIds = Regex("@[0-9a-f]+")

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
        it.toString()
            // Strip anonymous and synthetic lambda names, because are irrelevant diffs.
            .replace(regexForLambdas, "\\$<N>@")
            // Strip explicit object IDs, because they might appear as meaningless diffs.
            .replace(regexForObjectIds, "@<id>")
            // Ignore the fact that we're switching to a new help screen class name.
            .replace("MarkdownHelpScreen", "HelpScreen")
            // Ignore (for now) the fact that we refactored the regression tests.
            .replace("HelpScreenRegressionTestBase", "HelpScreenRegressionTest")
    }
    return "(bounds=${bounds.toShortString()}, modifiers=[$modifiers])"
}

private fun androidx.compose.ui.geometry.Rect.toShortString(): String =
    "(${left.toInt()},${top.toInt()} - ${right.toInt()},${bottom.toInt()})"

private sealed class AssertResult {
    object Passed : AssertResult()
    data class Failed(val message: String) : AssertResult()
}

/**
 * Hybrid baseline assertion:
 * - Reads committed baseline from assets/
 * - If missing, writes a temporary new file to context.filesDir
 * - If mismatched, prints a diff
 */
fun assertLayoutMatchesHybridBaseline(
    filename: String,
    snapshot: String,
    snapshotType: String
) {
    val context = InstrumentationRegistry.getInstrumentation().context
    val baselineSubpathString = "baselines/${filename}"
    val baselineText = loadBaselineFromAssets(context, baselineSubpathString)

    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val outputDir: Path = (System.getProperty("test.baselineOutputDir") as String?)
        ?.let { Paths.get(it) }
        ?: Paths.get(targetContext.filesDir.absolutePath)

    if (baselineText == null) {
        // If no baseline in assets, create a new one on the Android device (or host if Robolectric)
        // for review.
        val outputBaselineFile = generateBaselineFile(outputDir, baselineSubpathString, snapshot)
        fail(
            "No $snapshotType baseline found in 'assets/$baselineSubpathString'. " +
            "A new one will be written to '${outputBaselineFile}'; review then commit it.")
    }
    else {
        // We check the result of the diff, rather than comparing the baseline and snapshot
        // directly, because the line endings may differ, and the diff ignores those.
        val diff = generateDiff(baselineText, snapshot)
        if (diff.isNotEmpty()) {
            // If baseline exists but differs, print diff and fail.
            println("\n$snapshotType changed! Diff:\n")
            println(diff)
            val outputBaselineFile = generateBaselineFile(outputDir, baselineSubpathString, snapshot)
            println("New $snapshotType snapshot written to: ${outputBaselineFile}")
            fail(
                "$snapshotType does not match baseline. See diff above. " +
                "An updated version will be copied into ${outputBaselineFile}; " +
                "review the changes before committing.")
        }
        else {
            println("$snapshotType matches baseline.")
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

private fun generateBaselineFile(outputDir: Path, filename: String, snapshot: String): Path {
    val outputBaselineFile = outputDir.resolve(filename)
    ensureBaselineDirExistsAndIsReadable(outputDir, outputBaselineFile.parent)
    outputBaselineFile.writeText(snapshot)
    outputBaselineFile.toFile().setReadable(true, false)
    return outputBaselineFile
}

fun ensureBaselineDirExistsAndIsReadable(outputDir: Path, path: Path) {

    fun Path.makeReadableAndTraversable() {
        val dir = toFile()
        dir.setReadable(true, false)
        dir.setExecutable(true, false) // so adb "shell" user can traverse it
    }

    path.createDirectories().makeReadableAndTraversable()
    // Recursively ensure all parents are world-readable and -executable.
    generateSequence(path) { it.parent }
        .takeWhile { it.startsWith(outputDir) }
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
