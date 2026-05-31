package org.scottishtecharmy.soundscape

import android.content.Context
import android.content.res.Configuration
import android.os.Environment
import android.os.LocaleList
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.MainActivity.Companion.ACCESSIBLE_MAP_KEY
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.home.SectionType
import org.scottishtecharmy.soundscape.screens.home.home.helpPages
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsUiState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.utils.processMaps
import org.scottishtecharmy.soundscape.viewmodels.home.HomeState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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
        testCode: @Composable () -> Unit) {

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Unpack map assets
        processMaps(targetContext)

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
    fun homeScreen(){

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(targetContext)
        // Use accessible map
        sharedPreferences.edit().putBoolean(ACCESSIBLE_MAP_KEY, true).apply()

        runScreenTest("homeScreen") {
            Home(
                state = HomeState(
                    location = location,
                    heading = 45.0F,

                    ),
                onNavigate = {},
                preferences = sharedPreferences,
                onMapLongClick = { false},
                bottomButtonFunctions = BottomButtonFunctions(null),
                getCurrentLocationDescription = {
                    LocationDescription(
                        name = "Milngavie",
                        location = location
                    )
                },
                searchFunctions = SearchFunctions(null),
                rateSoundscape = { },
                contactSupport = { },
                toggleTutorial = {},
                tutorialRunning = false,
                routeFunctions = RouteFunctions(viewModel = null),
                streetPreviewFunctions = StreetPreviewFunctions(viewModel = null),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                goToAppSettings = { },
                permissionsRequired = false
            )
        }
    }

    @Test
    fun homeScreenWithRoute(){
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(targetContext)
        // Use accessible map
        sharedPreferences.edit().putBoolean(ACCESSIBLE_MAP_KEY, true).apply()

        val routePlayerState = RoutePlayerState(
            routeData = routeToShops,
            currentWaypoint = 0
        )

        runScreenTest("homeScreenWithRoute") {
            Home(
                state = HomeState(
                    location = location,
                    heading = 45.0F,
                    currentRouteData = routePlayerState
                ),
                onNavigate = {},
                preferences = sharedPreferences,
                onMapLongClick = { false},
                bottomButtonFunctions = BottomButtonFunctions(null),
                getCurrentLocationDescription = {
                    LocationDescription(
                        name = "Milngavie",
                        location = location
                    )
                },
                searchFunctions = SearchFunctions(null),
                rateSoundscape = { },
                contactSupport = {},
                toggleTutorial = {},
                tutorialRunning = false,
                routeFunctions = RouteFunctions(viewModel = null),
                streetPreviewFunctions = StreetPreviewFunctions(viewModel = null),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                goToAppSettings = { },
                permissionsRequired = false
            )
        }
    }

    @Test
    fun routeDetailsScreen(){
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        runScreenTest("routeDetails") {
            RouteDetailsScreen(
                navController = NavController(targetContext),
                routeId = 1,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                uiState = RouteDetailsUiState(
                    route = routeToShops
                ),
                routePlayerState = RoutePlayerState(
                    null,
                    0
                ),
                getRouteById = { },
                startRoute = { },
                startRouteInReverse = { },
                stopRoute = { },
                shareRoute = { },
                clearErrorMessage = { },
                userLocation = location,
                heading = 45.0F
            )
        }
    }
    @Test
    fun editScreen() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val route = routeToShops
        val members = mutableListOf<LocationDescription>()
        for ((index, marker) in route.markers.withIndex()) {
            members.add(
                LocationDescription(
                    name = marker.name,
                    location = LngLatAlt(marker.longitude, marker.latitude),
                    orderId = index.toLong(),
                )
            )
        }

        val uiState = AddAndEditRouteUiState(
            name = "To shops",
            description = "Route to shops",
            routeMembers = members,
        )

        runScreenTest("routeEdit") {
            AddAndEditRouteScreen(
                routeObjectId = 1,
                navController = NavController(targetContext),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                uiState = uiState,
                placesNearbyUiState = PlacesNearbyUiState(),
                editRoute = true,
                userLocation = location,
                heading = 45.0f,
                onClearErrorMessage = { },
                onResetDoneAction = { },
                onNameChange = { },
                onDescriptionChange = { },
                onDeleteRoute = { },
                onEditComplete = { },
                onClickFolder = { _, _ -> },
                onClickBack = { },
                onSelectLocation = { },
                createAndAddMarker = { _, _, _, _ -> },
                getCurrentLocationDescription = {
                    LocationDescription(
                        "Current Location",
                        location
                    )
                },
                onToggleMember = {},
            )
        }
    }

    // Maps each Android resource qualifier to its web / BCP-47 (jekyll-polyglot)
    // language code. The default language "en" is emitted with no filename suffix.
    // Source of truth for the supported set: app/build.gradle.kts resourceConfigurations.
    // Keep this in sync with the `languages` list in docs/_config.yml.
    private val localeMap: Map<String, String> = mapOf(
        "en" to "en",          // default -> no suffix, no `lang:` front matter
        "arz" to "arz",
        "zh-rCN" to "zh-CN",
        "da" to "da",
        "de" to "de",
        "el" to "el",
        "en-rGB" to "en-GB",
        "es" to "es",
        "fa" to "fa",
        "fi" to "fi",
        "fr" to "fr",
        "fr-rCA" to "fr-CA",
        "hi" to "hi",
        "is" to "is",
        "it" to "it",
        "ja" to "ja",
        "nb" to "nb",
        "nl" to "nl",
        "pl" to "pl",
        "pt" to "pt",
        "pt-rBR" to "pt-BR",
        "ro" to "ro",
        "ru" to "ru",
        "sv" to "sv",
        "tr" to "tr",
        "uk" to "uk",
    )

    // Localized "Using Soundscape" — the nav parent/section title. Keyed by web language
    // code. just-the-docs matches a child page's `parent:` to the parent page's `title:`
    // within each language, so the value here must exactly match the title of the
    // corresponding docs/users/user.<lang>.md. English (and en-GB) keep the English label.
    private val parentLabels: Map<String, String> = mapOf(
        "en" to "Using Soundscape",
        "en-GB" to "Using Soundscape",
        "arz" to "استخدام ساوندسكيب",
        "zh-CN" to "使用 Soundscape",
        "da" to "Brug af Soundscape",
        "de" to "Soundscape verwenden",
        "el" to "Χρήση του Soundscape",
        "es" to "Usar Soundscape",
        "fa" to "استفاده از ساند‌اسکیپ",
        "fi" to "Soundscapen käyttö",
        "fr" to "Utiliser Soundscape",
        "fr-CA" to "Utiliser Soundscape",
        "hi" to "Soundscape का उपयोग",
        "is" to "Að nota Soundscape",
        "it" to "Usare Soundscape",
        "ja" to "Soundscape を使う",
        "nb" to "Bruke Soundscape",
        "nl" to "Soundscape gebruiken",
        "pl" to "Korzystanie z Soundscape",
        "pt" to "Utilizar o Soundscape",
        "pt-BR" to "Usando o Soundscape",
        "ro" to "Utilizarea Soundscape",
        "ru" to "Использование Soundscape",
        "sv" to "Använda Soundscape",
        "tr" to "Soundscape Kullanımı",
        "uk" to "Використання Soundscape",
    )

    /** Returns a Context whose getString() resolves strings in [webLang]. */
    private fun localizedContext(base: Context, webLang: String): Context {
        val locale = Locale.forLanguageTag(webLang)   // "fr-CA", "pt-BR", "zh-CN" all resolve
        val config = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(config)
    }

    /**
     * Builds the markdown for a single help page rendered in [ctx]'s locale.
     * [parentLabel] is the localized "Using Soundscape" nav parent for [webLang].
     */
    private fun buildPageMarkdown(
        ctx: Context,
        page: org.scottishtecharmy.soundscape.screens.home.home.Sections,
        webLang: String,
        slug: String,
        parentLabel: String
    ): String {
        fun localized(resId: Int): String = ctx.getString(resId)

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
                    sb.append(localized(section.faqAnswer))
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

        for ((_, webLang) in localeMap) {
            val ctx = if (webLang == "en") base else localizedContext(base, webLang)
            // Fall back to the English label if a language is somehow missing from the map.
            val parentLabel = parentLabels[webLang] ?: "Using Soundscape"

            for (page in helpPages) {

                if (page.titleId == R.string.menu_help)
                    continue

                // Slug always comes from the English title so all languages of one page
                // share a base name (help-routes.md / help-routes.de.md) and the same
                // permalink, which is how polyglot pairs them.
                val slug = base.getString(page.titleId).toSafeFilename()

                // Emit a file for every language even when a page is untranslated (its
                // strings fall back to English). The localized `parent:` keeps it nested
                // under the language's "Using Soundscape" section instead of orphaning a
                // fallback page whose English parent wouldn't match.
                val markdown = buildPageMarkdown(ctx, page, webLang, slug, parentLabel)

                val suffix = if (webLang == "en") "" else ".$webLang"

                val file = File(helpDir, "help-$slug$suffix.md")
                val outputFile = FileOutputStream(file)
                outputFile.write(markdown.toByteArray())
                outputFile.close()
            }
        }
    }
}