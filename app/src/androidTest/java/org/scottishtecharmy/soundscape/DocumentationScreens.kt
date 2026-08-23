package org.scottishtecharmy.soundscape

import android.os.Environment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.menu_help
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SectionType
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.home.helpPages
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import java.io.File
import java.io.FileOutputStream

fun String.toSafeFilename(replacement: String = "-"): String {
    val illegalCharsRegex = """[/:\\?*"<>|#%&{}^`~ ]""".toRegex()
    var safeName = this.replace(illegalCharsRegex, replacement)
    safeName = safeName.trim('.', ' ')
    safeName = safeName.replace(Regex("$replacement{2,}"), replacement)
    if (safeName.isEmpty()) {
        assert(false)
    }
    return safeName.lowercase()
}

// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class DocumentationScreens {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val location = LngLatAlt(
        -4.3178027,
        55.9410791
    )

    private val routeToShops = RouteWithMarkers(
        RouteEntity(
            name = "Route to shops",
            description = "",
            routeId = 1L
        ),
        listOf(
            MarkerEntity(
                name = "Craigton Road",
                longitude = -4.3239319,
                latitude = 55.9446396,
                markerId = 1L
            ),
            MarkerEntity(
                name = "Clober Road",
                longitude = -4.3210534,
                latitude = 55.9417227,
                markerId = 2L
            ),
            MarkerEntity(
                name = "Douglas Street",
                longitude = -4.3194968,
                latitude = 55.9406974,
                markerId = 3L
            ),
            MarkerEntity(
                name = "Underpass to shops",
                longitude = -4.3175668,
                latitude = 55.9399973,
                markerId = 4L
            )
        )
    )

    private fun runScreenTest(
        screenshotFileName: String,
        testCode: @Composable () -> Unit
    ) {

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val themeStateFlow = MutableStateFlow(ThemeState())
        composeTestRule.setContent {
            SoundscapeTheme(themeStateFlow) {
                testCode()
            }
        }

        // Delay to allow the maps to load
        Thread.sleep(5000)

        // Capture screenshot of the root composable
        ScreenshotUtils.captureAndSaveScreenshot(
            context = targetContext, // Use target context
            filename = screenshotFileName
        )
    }

    @Test
    fun homeScreen() {

        runScreenTest("homeScreen") {
            SharedHomeScreen(
                state = HomeState(
                    location = location,
                    heading = 45.0F,
                ),
                onNavigate = {},
                onSelectLocation = {},
                preferencesProvider = null,
                onMapLongClick = { false },
                bottomButtonFunctions = BottomButtonFunctions(),
                getCurrentLocationDescription = {
                    LocationDescription(
                        name = "Milngavie",
                        location = location,
                    )
                },
                searchFunctions = SearchFunctions(),
                rateSoundscape = { },
                contactSupport = { },
                shareRecording = { },
                toggleTutorial = {},
                tutorialRunning = false,
                recordingEnabled = false,
                voiceCommandListening = false,
                routeFunctions = RouteFunctions(),
                streetPreviewFunctions = StreetPreviewFunctions(),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                goToAppSettings = { },
                permissionsRequired = false,
                onSleep = {},
            )
        }
    }

    @Test
    fun homeScreenWithRoute() {
        val routePlayerState = RoutePlayerState(
            routeData = routeToShops,
            currentWaypoint = 0
        )

        runScreenTest("homeScreenWithRoute") {
            SharedHomeScreen(
                state = HomeState(
                    location = location,
                    heading = 45.0F,
                    currentRouteData = routePlayerState,
                ),
                onNavigate = {},
                onSelectLocation = {},
                preferencesProvider = null,
                onMapLongClick = { false },
                bottomButtonFunctions = BottomButtonFunctions(),
                getCurrentLocationDescription = {
                    LocationDescription(
                        name = "Milngavie",
                        location = location,
                    )
                },
                searchFunctions = SearchFunctions(),
                rateSoundscape = { },
                contactSupport = {},
                shareRecording = {},
                toggleTutorial = {},
                tutorialRunning = false,
                recordingEnabled = false,
                voiceCommandListening = false,
                routeFunctions = RouteFunctions(),
                streetPreviewFunctions = StreetPreviewFunctions(),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                goToAppSettings = { },
                permissionsRequired = false,
                onSleep = {},
            )
        }
    }

    @Test
    fun routeDetailsScreen() {
        val waypoints = routeToShops.markers.map { marker ->
            LocationDescription(
                name = marker.name,
                location = LngLatAlt(marker.longitude, marker.latitude),
            )
        }

        runScreenTest("routeDetails") {
            SharedRouteDetailsScreen(
                routeName = routeToShops.route.name,
                routeDescription = routeToShops.route.description,
                waypoints = waypoints,
                isRoutePlaying = false,
                userLocation = location,
                heading = 45.0F,
                onNavigateUp = { },
                onStartRoute = { },
                onStartRouteInReverse = { },
                onStopRoute = { },
                onEditRoute = { },
                onShareRoute = { },
            )
        }
    }

    // Maps each Android resource qualifier to its web / BCP-47 (jekyll-polyglot)
    // language code. The default language "en" is emitted with no filename suffix.
    // Source of truth for the supported set: app/build.gradle.kts resourceConfigurations.
    // Keep this in sync with the `languages` list in docs/_config.yml.
    private val localeMap: Map<String, String> = mapOf(
        "en" to "en",          // default -> no suffix, no `lang:` front matter
        "ar" to "ar",
        "arz" to "arz",
        "bg" to "bg",
        "bn" to "bn",
        "ca" to "ca",
        "zh-rCN" to "zh-CN",
        "cs" to "cs",
        "da" to "da",
        "de" to "de",
        "el" to "el",
        "en-rGB" to "en-GB",
        "es" to "es",
        "et" to "et",
        "fa" to "fa",
        "fi" to "fi",
        "fr" to "fr",
        "fr-rCA" to "fr-CA",
        "ha" to "ha",
        "hi" to "hi",
        "hr" to "hr",
        "hu" to "hu",
        "in" to "id",
        "is" to "is",
        "it" to "it",
        "ja" to "ja",
        "ko" to "ko",
        "mr" to "mr",
        "nb" to "nb",
        "nl" to "nl",
        "pl" to "pl",
        "pt" to "pt",
        "pt-rBR" to "pt-BR",
        "ro" to "ro",
        "ru" to "ru",
        "sk" to "sk",
        "sl" to "sl",
        "sr" to "sr",
        "sv" to "sv",
        "sw" to "sw",
        "ta" to "ta",
        "te" to "te",
        "th" to "th",
        "tr" to "tr",
        "uk" to "uk",
        "ur" to "ur",
        "vi" to "vi",
    )

    // Localized "Using Soundscape" — the nav parent/section title. Keyed by web language
    // code. just-the-docs matches a child page's `parent:` to the parent page's `title:`
    // within each language, so the value here must exactly match the title of the
    // corresponding docs/users/user.<lang>.md. English (and en-GB) keep the English label.
    private val parentLabels: Map<String, String> = mapOf(
        "en" to "Using Soundscape",
        "en-GB" to "Using Soundscape",
        "ar" to "استخدام Soundscape",
        "arz" to "استخدام ساوندسكيب",
        "bg" to "Използване на Soundscape",
        "bn" to "সাউন্ডস্কেপ ব্যবহার",
        "ca" to "Utilitzar Soundscape",
        "zh-CN" to "使用 Soundscape",
        "cs" to "Používání Soundscape",
        "da" to "Brug af Soundscape",
        "de" to "Soundscape verwenden",
        "el" to "Χρήση του Soundscape",
        "es" to "Usar Soundscape",
        "et" to "Soundscape'i kasutamine",
        "fa" to "استفاده از ساند‌اسکیپ",
        "fi" to "Soundscapen käyttö",
        "fr" to "Utiliser Soundscape",
        "fr-CA" to "Utiliser Soundscape",
        "ha" to "Amfani da Soundscape",
        "hi" to "Soundscape का उपयोग",
        "hr" to "Korištenje Soundscape",
        "hu" to "A Soundscape használata",
        "id" to "Menggunakan Soundscape",
        "is" to "Að nota Soundscape",
        "it" to "Usare Soundscape",
        "ja" to "Soundscape を使う",
        "ko" to "Soundscape 사용하기",
        "mr" to "Soundscape वापरणे",
        "nb" to "Bruke Soundscape",
        "nl" to "Soundscape gebruiken",
        "pl" to "Korzystanie z Soundscape",
        "pt" to "Utilizar o Soundscape",
        "pt-BR" to "Usando o Soundscape",
        "ro" to "Utilizarea Soundscape",
        "ru" to "Использование Soundscape",
        "sk" to "Používanie Soundscape",
        "sl" to "Uporaba Soundscape",
        "sr" to "Коришћење Soundscape",
        "sv" to "Använda Soundscape",
        "sw" to "Kutumia Soundscape",
        "ta" to "Soundscape பயன்படுத்துதல்",
        "te" to "Soundscape వాడకం",
        "th" to "การใช้ Soundscape",
        "tr" to "Soundscape Kullanımı",
        "uk" to "Використання Soundscape",
        "ur" to "Soundscape کا استعمال",
        "vi" to "Sử dụng Soundscape",
    )

    /**
     * Builds the markdown for a single help page rendered under [environment]'s locale.
     * [parentLabel] is the localized "Using Soundscape" nav parent for [webLang].
     */
    private suspend fun buildPageMarkdown(
        environment: org.jetbrains.compose.resources.ResourceEnvironment,
        page: org.scottishtecharmy.soundscape.screens.home.home.Sections,
        webLang: String,
        slug: String,
        parentLabel: String
    ): String {
        suspend fun localized(resource: org.jetbrains.compose.resources.StringResource): String =
            org.jetbrains.compose.resources.getString(environment, resource)

        val pageTitle = localized(page.titleId)

        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("title: $pageTitle\n")
        sb.append("layout: page\n")
        // Parent must match the title of docs/users/user.<lang>.md in the same language so
        // just-the-docs nests this page under the localized "Using Soundscape" section.
        sb.append("parent: \"$parentLabel\"\n")
        sb.append("has_toc: false\n")
        if (webLang != "en") {
            sb.append("lang: $webLang\n")
            // Pin the permalink to the English page's URL so jekyll-polyglot matches this
            // file (by page_id, which it derives from the URL) to its English sibling and
            // serves it at /<lang>/users/help-<slug>.html instead of a separate URL.
            sb.append("permalink: /users/help-$slug.html\n")
        }
        sb.append("---\n\n")

        sb.append("# ")
        sb.append(pageTitle)
        sb.append("\n")
        for (section in page.sections) {
            when (section.type) {
                SectionType.Faq -> {
                    sb.append("\n")
                    sb.append("### ")
                    sb.append(localized(section.textId))
                    sb.append("\n")
                    section.faqAnswer?.let { answer ->
                        sb.append(localized(answer))
                    }
                }

                SectionType.Title -> {
                    sb.append("\n")
                    sb.append("## ")
                    sb.append(localized(section.textId))
                }

                else -> {
                    sb.append("\n")
                    sb.append(localized(section.textId))
                }
            }
            sb.append("\n")
        }
        sb.append("\n")

        return sb.toString()
    }

    @Test
    fun getHelp() {

        val base = InstrumentationRegistry.getInstrumentation().targetContext

        val helpDir = File(
            base.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "help"
        )
        if (!helpDir.exists()) {
            helpDir.mkdirs()
        }

        // The slug always comes from the English title so all languages of one page share a
        // base name (help-routes.md / help-routes.de.md) and the same permalink, which is how
        // polyglot pairs them. "en" has no strings.xml qualifier folder, so it resolves via the
        // default (unqualified) resource folder, which is English.
        val originalLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        runBlocking {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags("en")
            )
            val englishEnvironment = org.jetbrains.compose.resources.getSystemResourceEnvironment()

            for ((_, webLang) in localeMap) {
                if (webLang != "en") {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(webLang)
                    )
                }
                val environment = org.jetbrains.compose.resources.getSystemResourceEnvironment()
                // Fall back to the English label if a language is somehow missing from the map.
                val parentLabel = parentLabels[webLang] ?: "Using Soundscape"

                for (page in helpPages) {
                    if (page.titleId == Res.string.menu_help)
                        continue

                    // Slug always comes from the English title so all languages of one page
                    // share a base name (help-routes.md / help-routes.de.md) and the same
                    // permalink, which is how polyglot pairs them.
                    val slug = getString(englishEnvironment, page.titleId).toSafeFilename()

                    // Emit a file for every language even when a page is untranslated (its
                    // strings fall back to English). The localized `parent:` keeps it nested
                    // under the language's "Using Soundscape" section instead of orphaning a
                    // fallback page whose English parent wouldn't match.
                    val markdown = buildPageMarkdown(environment, page, webLang, slug, parentLabel)

                    val suffix = if (webLang == "en") "" else ".$webLang"

                    val file = File(helpDir, "help-$slug$suffix.md")
                    val outputFile = FileOutputStream(file)
                    outputFile.write(markdown.toByteArray())
                    outputFile.close()
                }
            }

            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(originalLocales)
        }
    }
}