package org.scottishtecharmy.soundscape

import android.os.Environment
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
                createAndAddMarker = { _, _, _ -> },
                getCurrentLocationDescription = { LocationDescription("Current Location", location) },
            )
        }
    }

    @Test
    fun getHelp() {

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val helpDir = File(
            targetContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "help"
        )
        if (!helpDir.exists()) {
            helpDir.mkdirs()
        }

        for (page in helpPages) {

            if(page.titleId == R.string.menu_help_and_tutorials)
                continue
            val pageTitle = targetContext.getString(page.titleId)

            val markdownOutput = StringBuilder()
            markdownOutput.append("---\n")
            markdownOutput.append("title: $pageTitle\n")
            markdownOutput.append("layout: page\n")
            markdownOutput.append("parent: Using Soundscape\n")
            markdownOutput.append("has_toc: false\n")
            markdownOutput.append("---\n\n")

            markdownOutput.append("# ")
            markdownOutput.append(pageTitle)
            markdownOutput.append("\n")
            for(section in page.sections) {
                when (section.type) {
                    SectionType.Faq -> {
                        markdownOutput.append("\n")
                        markdownOutput.append("### ")
                        markdownOutput.append(targetContext.getString(section.textId))
                        markdownOutput.append("\n")
                        markdownOutput.append(targetContext.getString(section.faqAnswer))
                    }
                    SectionType.Title -> {
                        markdownOutput.append("\n")
                        markdownOutput.append("## ")
                        markdownOutput.append(targetContext.getString(section.textId))
                    }
                    else -> {
                        markdownOutput.append("\n")
                        markdownOutput.append(targetContext.getString(section.textId))
                    }
                }
                markdownOutput.append("\n")
            }
            markdownOutput.append("\n")

            val file = File(helpDir, "help-${pageTitle.toSafeFilename()}.md")
            val outputFile = FileOutputStream(file)
            outputFile.write(markdownOutput.toString().toByteArray())
            outputFile.close()
        }
    }
}