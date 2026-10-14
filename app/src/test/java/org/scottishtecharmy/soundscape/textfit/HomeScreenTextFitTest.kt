package org.scottishtecharmy.soundscape.textfit

import android.app.Application
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.platform.appVersionMinorTrimmed
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import org.scottishtecharmy.soundscape.screens.home.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import java.io.File
import java.util.Locale

/**
 * Finds translated text on the Home screen that doesn't fit, without any reference images: it
 * asks the laid-out text whether it overflowed, and whether its container clipped it.
 *
 * Renders in every app language at the size of the screenshot tests' small_phone, at the default
 * font size and at the largest Android offers, since low-vision users use it. Fails if any text
 * doesn't fit, listing it; each run's findings are also written to build/reports/text-fit/.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // Real text measurement, not Robolectric's stub
// A plain Application rather than the app's, which starts services and Koin that the screen
// doesn't need. SDK 35 because Espresso, under Compose's test rule, can't yet drive SDK 37's
// InputManager.
@Config(qualifiers = "w360dp-h640dp-xhdpi", sdk = [35], application = Application::class)
class HomeScreenTextFitTest(private val qualifier: String, private val fontScale: Float) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenTextFits() {
        RuntimeEnvironment.setQualifiers("+$qualifier")
        RuntimeEnvironment.setFontScale(fontScale)
        // Strings fetched outside composition (distances, directions) use the default locale.
        Locale.setDefault(Locale.forLanguageTag(qualifier.replace("-r", "-")))

        composeRule.setContent {
            SoundscapeTheme {
                SharedHomeScreen(
                    state = HomeState(location = LngLatAlt(-4.2518, 55.8642)),
                    onNavigate = {},
                    onSelectLocation = {},
                    preferencesProvider = TestPreferencesProvider,
                    bottomButtonFunctions = BottomButtonFunctions(),
                    routeFunctions = RouteFunctions(),
                    streetPreviewFunctions = StreetPreviewFunctions(),
                    searchFunctions = SearchFunctions(),
                    getCurrentLocationDescription = {
                        LocationDescription(
                            name = "Current location",
                            location = LngLatAlt(-4.2518, 55.8642),
                            locationType = LocationType.Street,
                        )
                    },
                    rateSoundscape = {},
                    contactSupport = {},
                    shareRecording = {},
                    toggleTutorial = {},
                    tutorialRunning = false,
                    recordingEnabled = false,
                    permissionsRequired = false,
                    goToAppSettings = {},
                    onSleep = {},
                )
            }
        }
        composeRule.waitForIdle()

        val problems = findTextProblems(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        report("Home", problems)
    }

    private fun report(screen: String, problems: List<String>) {
        val text = buildString {
            appendLine("$screen [$qualifier, font x$fontScale]: ${problems.size} problem(s)")
            problems.forEach { appendLine("  $it") }
        }
        print(text)
        File("build/reports/text-fit").apply { mkdirs() }
            .resolve("$screen-$qualifier-x$fontScale.txt").writeText(text)
        org.junit.Assert.assertTrue(text, problems.isEmpty())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} x{1}")
        fun parameters() = locales.flatMap { locale -> fontScales.map { arrayOf<Any>(locale, it) } }

        // The default, and the largest that Android's settings offer.
        private val fontScales = listOf(1f, 2f)

        private val locales = listOf(
            "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "en-rGB", "es", "et", "fa",
            "fi", "fr", "fr-rCA", "ha", "hi", "hr", "hu", "id", "is", "it", "ja", "ko", "mr",
            "nb", "nl", "pa", "pl", "pt", "pt-rBR", "ro", "ru", "sk", "sl", "sr", "sv", "sw",
            "ta", "te", "th", "tr", "uk", "ur", "vi", "zh",
        )
    }
}

/**
 * Every text node under [root] that either overflowed its own layout (ellipsized, or cut at
 * maxLines or a fixed size) or was partly clipped by an ancestor. Clipping inside something that
 * scrolls is ignored: that's text scrolled out of view, not text that doesn't fit.
 */
internal fun findTextProblems(root: SemanticsNode): List<String> {
    val problems = mutableListOf<String>()

    fun visit(node: SemanticsNode, insideScroller: Boolean) {
        val scrolls = insideScroller || node.config.getOrNull(SemanticsActions.ScrollBy) != null
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
        if (!text.isNullOrBlank()) {
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val layout = layouts.firstOrNull()
            if (layout != null) {
                textLost(layout)?.let { problems += "$it: \"$text\"" }
            }
            val full = Rect(node.positionInRoot, node.size.toSize())
            val shown = node.boundsInRoot
            if (!scrolls && (shown.width < full.width - 1f || shown.height < full.height - 1f)) {
                problems += "CLIPPED (shows ${shown.width.toInt()}x${shown.height.toInt()} " +
                    "of ${full.width.toInt()}x${full.height.toInt()} px): \"$text\""
            }
        }
        node.children.forEach { visit(it, scrolls) }
    }
    visit(root, insideScroller = false)
    return problems
}

/**
 * Why some of [layout]'s text can't be seen, or null if all of it can.
 *
 * Not [TextLayoutResult.hasVisualOverflow]: centred or wrapped text is laid out at the full width
 * on offer and then shrunk to its content, which that counts as overflowing even though every
 * glyph is drawn. Instead this checks for each way text is actually lost.
 */
private fun textLost(layout: TextLayoutResult): String? {
    val lines = 0 until layout.lineCount
    val widest = lines.maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
    return when {
        lines.any { layout.isLineEllipsized(it) } -> "ELLIPSIZED"
        layout.multiParagraph.didExceedMaxLines -> "TOO MANY LINES (max ${layout.layoutInput.maxLines})"
        widest > layout.size.width + 1f ->
            "CUT OFF AT THE SIDE (needs ${widest.toInt()} px, has ${layout.size.width})"
        layout.didOverflowHeight ->
            "CUT OFF BELOW (needs ${layout.multiParagraph.height.toInt()} px, has ${layout.size.height})"
        else -> null
    }
}

private fun androidx.compose.ui.unit.IntSize.toSize() =
    androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())

private object TestPreferencesProvider : PreferencesProvider {
    override fun getBoolean(key: String, default: Boolean): Boolean =
        // The map is native MapLibre, which Robolectric can't load, and has no text anyway.
        if (key == PreferenceKeys.SHOW_MAP) false else default

    // Mark this release's dialog as seen, or it covers the screen being checked.
    override fun getString(key: String, default: String): String =
        if (key == PreferenceKeys.LAST_NEW_RELEASE) appVersionMinorTrimmed() else default

    override fun getFloat(key: String, default: Float): Float = default
    override fun putBoolean(key: String, value: Boolean) {}
    override fun putString(key: String, value: String) {}
    override fun clearAll() {}
    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

/** Checks [findTextProblems] spots each kind of cut-off text, so a clean report means something. */
@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi", sdk = [35], application = Application::class)
class TextProblemsControlTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun spotsEachKindOfCutOffText() {
        val long = "A sentence far too long to fit in the space it has been given"
        // In the app's theme, as the screens are: Robolectric measures the bare default text
        // style at about half size.
        composeRule.setContent {
          SoundscapeTheme {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    "fits", modifier = androidx.compose.ui.Modifier.width(200.dp)
                )
                androidx.compose.material3.Text(
                    "ellipsized $long", maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = androidx.compose.ui.Modifier.width(80.dp),
                )
                androidx.compose.material3.Text(
                    "maxlines $long", maxLines = 2,
                    modifier = androidx.compose.ui.Modifier.width(80.dp),
                )
                androidx.compose.material3.Text(
                    "fixed height $long",
                    modifier = androidx.compose.ui.Modifier.width(80.dp).height(20.dp),
                )
                androidx.compose.material3.Text(
                    "no wrap $long", softWrap = false,
                    modifier = androidx.compose.ui.Modifier.width(80.dp),
                )
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.size(80.dp, 20.dp).clipToBounds()
                ) {
                    androidx.compose.material3.Text(
                        "clipped by parent $long",
                        modifier = androidx.compose.ui.Modifier.wrapContentSize(unbounded = true),
                    )
                }
            }
          }
        }
        composeRule.waitForIdle()
        val problems = findTextProblems(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        problems.forEach(::println)

        fun found(kind: String, prefix: String) = problems.any { it.startsWith(kind) && "\"$prefix" in it }
        org.junit.Assert.assertTrue("ellipsis", found("ELLIPSIZED", "ellipsized"))
        org.junit.Assert.assertTrue("maxLines", found("TOO MANY LINES", "maxlines"))
        org.junit.Assert.assertTrue("fixed height", found("CUT OFF BELOW", "fixed height"))
        // softWrap = false still wraps under Robolectric, so this is reported as cut off below
        // rather than at the side. On a device it would be one clipped line. Either way it's found.
        org.junit.Assert.assertTrue("no wrap", problems.any { "\"no wrap" in it })
        org.junit.Assert.assertTrue("parent clip", found("CLIPPED", "clipped by parent"))
        org.junit.Assert.assertFalse("false positive", problems.any { "\"fits\"" in it })
    }
}
