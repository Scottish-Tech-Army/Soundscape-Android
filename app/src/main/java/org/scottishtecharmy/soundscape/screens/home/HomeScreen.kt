package org.scottishtecharmy.soundscape.screens.home

import android.content.SharedPreferences
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import org.maplibre.android.maps.MapLibreMap.OnMapLongClickListener
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.AudioTourInstructionDialog
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.home.OfflineMapsScreenVM
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenVM
import org.scottishtecharmy.soundscape.screens.home.home.AdvancedMarkersAndRoutesSettingsScreenVM
import org.scottishtecharmy.soundscape.screens.home.home.OpenSourceLicensesVM
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreenVM
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteScreenVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.parseSimpleRouteData
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageViewModel
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.viewmodels.home.HomeState
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class Navigator {
    var destination = MutableStateFlow(HomeRoutes.Home.route)

    fun navigate(newDestination: String) {
        Log.d("NavigationRoot", "Navigate to $newDestination")
        this.destination.value = newDestination
    }
}

// To reduce the number of viewmodel functions passed around, use these data classes instead. They
// still provide insulation from the viewmodel so that they can be used in Preview.
data class BottomButtonFunctions(
    val viewModel: HomeViewModel?,
    private val audioTour: AudioTour? = null
) {
    val myLocation = {
        viewModel?.myLocation()
        audioTour?.onButtonPressed(org.scottishtecharmy.soundscape.audio.TourButton.MY_LOCATION)
    }
    val aheadOfMe = {
        viewModel?.aheadOfMe()
        audioTour?.onButtonPressed(org.scottishtecharmy.soundscape.audio.TourButton.AHEAD_OF_ME)
    }
    val aroundMe = {
        viewModel?.whatsAroundMe()
        audioTour?.onButtonPressed(org.scottishtecharmy.soundscape.audio.TourButton.AROUND_ME)
    }
    val nearbyMarkers = {
        viewModel?.nearbyMarkers()
        audioTour?.onButtonPressed(org.scottishtecharmy.soundscape.audio.TourButton.NEARBY_MARKERS)
    }
}

data class RouteFunctions(val viewModel: HomeViewModel?) {
    val skipPrevious = { viewModel?.routeSkipPrevious() }
    val skipNext = { viewModel?.routeSkipNext() }
    val mute = { viewModel?.routeMute() }
    val stop =  { viewModel?.routeStop() }
}

data class SearchFunctions(val viewModel: HomeViewModel?) {
    val onTriggerSearch: (String) -> Unit = { viewModel?.onTriggerSearch(it) }
}

data class StreetPreviewFunctions(val viewModel: HomeViewModel?) {
    val go = { viewModel?.streetPreviewGo() }
    val exit = { viewModel?.streetPreviewExit() }
}

fun getCurrentLocationDescription(viewModel: HomeViewModel, state: HomeState): LocationDescription
{
    if(state.location != null) {
        val location = LngLatAlt(
            state.location!!.longitude,
            state.location!!.latitude
        )
        return viewModel.getLocationDescription(location) ?: LocationDescription("", location)
    } else {
        return LocationDescription("", LngLatAlt())
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    preferences: SharedPreferences,
    viewModel: HomeViewModel = hiltViewModel(),
    audioTour: AudioTour,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    permissionsRequired: Boolean
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val audioTourInstruction by audioTour.currentInstruction.collectAsStateWithLifecycle()
    val routeFunctions = remember(viewModel) { RouteFunctions(viewModel) }
    val searchFunctions = remember(viewModel) { SearchFunctions(viewModel) }
    val streetPreviewFunctions = remember(viewModel) { StreetPreviewFunctions(viewModel) }
    val bottomButtonFunctions = remember(viewModel, audioTour) { BottomButtonFunctions(viewModel, audioTour) }
    val onMapLongClickListener = remember(viewModel) {
        OnMapLongClickListener { latLong ->
            val location = LngLatAlt(latLong.longitude, latLong.latitude)
            val ld = viewModel.getLocationDescription(location) ?: LocationDescription("", location)
            navController.navigate(generateLocationDetailsRoute(ld))

            Analytics.getInstance().logEvent("longPressOnMap", null)
            true
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = HomeRoutes.Home.route,
        ) {
            // Main navigation
            composable(HomeRoutes.Home.route) {
                Home(
                    state = state.value,
                    onNavigate = { dest -> navController.navigate(dest) },
                    preferences = preferences,
                    onMapLongClick = onMapLongClickListener,
                    bottomButtonFunctions = bottomButtonFunctions,
                    getCurrentLocationDescription = { getCurrentLocationDescription(viewModel, state.value) },
                    searchFunctions = searchFunctions,
                    rateSoundscape = rateSoundscape,
                    contactSupport = contactSupport,
                    toggleTutorial = { audioTour.toggleState() },
                    tutorialRunning = audioTour.isRunning(),
                    routeFunctions = routeFunctions,
                    streetPreviewFunctions = streetPreviewFunctions,
                    goToAppSettings = viewModel::goToAppSettings,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true },
                    permissionsRequired = permissionsRequired
                )
            }

        // Settings screen
        composable(HomeRoutes.Settings.route) {
            // Always just pop back out of settings, don't add to the queue
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState = settingsViewModel.state.collectAsStateWithLifecycle()
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val languageUiState = languageViewModel.state.collectAsStateWithLifecycle()
            val localActivity = LocalActivity.current as MainActivity

            LaunchedEffect(Unit) {
                settingsViewModel.restartAppEvent.collect {
                    localActivity.recreate()
                }
            }

            Settings(
                navController = navController,
                uiState = uiState.value,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                supportedLanguages = languageUiState.value.supportedLanguages,
                onLanguageSelected = { selectedLanguage ->
                    languageViewModel.updateLanguage(selectedLanguage)
                    settingsViewModel.updateLanguage(localActivity)
                 },
                selectedLanguageIndex = languageUiState.value.selectedLanguageIndex,
                storages = uiState.value.storages,
                onStorageSelected = { path ->
                    settingsViewModel.selectStorage(path)
                },
                selectedStorageIndex = uiState.value.selectedStorageIndex,
                resetSettings = { settingsViewModel.resetToDefaults() }
            )
        }

        // Language choosing screen
        composable(HomeRoutes.Language.route) {
            // Always just pop back out of settings, don't add to the queue
            LanguageScreen(
                onNavigate = { navController.navigateUp() },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
            )
        }

        // Location details screen
        composable(HomeRoutes.LocationDetails.route + "/{json}") { navBackStackEntry ->
            val urlEncodedJson = navBackStackEntry.arguments?.getString("json")

            // Parse the LocationDescription only once, not on every recomposition
            val locationDescription = remember(urlEncodedJson) {
                val gson = GsonBuilder().create()
                val json = URLDecoder.decode(urlEncodedJson, StandardCharsets.UTF_8.toString())
                gson.fromJson(json, LocationDescription::class.java)
            }

            LocationDetailsScreen(
                locationDescription = locationDescription,
                location = state.value.location,
                navController = navController,
                heading = state.value.heading,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
            )
        }

        // MarkersAndRoutesScreen with tab selection
        composable(
            HomeRoutes.MarkersAndRoutes.route + "?tab={tab}",
            arguments = listOf(navArgument("tab") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: ""
            LaunchedEffect(tab) {
                if (tab == "markers") {
                    viewModel.setRoutesAndMarkersTab(false)
                } else if (tab == "routes") {
                    viewModel.setRoutesAndMarkersTab(true)
                }
            }
            MarkersAndRoutesScreen(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(HomeRoutes.RouteDetails.route + "/{routeId}") { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            RouteDetailsScreenVM(
                routeId = routeId.toLong(),
                navController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                userLocation = state.value.location,
                heading = state.value.heading,
                routePlayerState = state.value.currentRouteData
            )
        }

        composable(HomeRoutes.AddAndEditRoute.route + "?command={command}&data={data}",
            arguments = listOf(
                navArgument("command") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("data") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val command = backStackEntry.arguments?.getString("command") ?: ""
            val data = backStackEntry.arguments?.getString("data") ?: ""

            // Parse route data only once, not on every recomposition
            val routeData = remember(command, data) {
                when (command) {
                    "import" -> {
                        try {
                            val json = URLDecoder.decode(data, StandardCharsets.UTF_8.toString())
                            parseSimpleRouteData(json)
                        } catch (e: Exception) {
                            Log.e("RouteDetailsScreen", "Error parsing route data: $e")
                            null
                        }
                    }
                    else -> null
                }
            }

            val addAndEditRouteViewModel: AddAndEditRouteViewModel = hiltViewModel()

            // Call the ViewModel's function to initialize the route data
            LaunchedEffect(data) {
                addAndEditRouteViewModel.loadMarkers()
                if(routeData != null) {
                    addAndEditRouteViewModel.initializeRouteFromData(routeData)
                } else if(command == "edit") {
                    addAndEditRouteViewModel.initializeRouteFromDatabase(data.toLong())
                }
            }

            // Pass any route details to the EditRouteScreen composable
            val uiState by addAndEditRouteViewModel.uiState.collectAsStateWithLifecycle()
            AddAndEditRouteScreenVM(
                routeObjectId = uiState.routeObjectId,
                navController = navController,
                viewModel = addAndEditRouteViewModel,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                userLocation = state.value.location,
                heading = state.value.heading,
                editRoute = (command == "edit"),
                getCurrentLocationDescription = { getCurrentLocationDescription(viewModel, state.value) },
            )
        }

        composable(HomeRoutes.Help.route + "/{topic}") { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            HelpScreen(
                topic = topic,
                navController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(HomeRoutes.Sleep.route) {
            SleepScreenVM(
                navController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(HomeRoutes.PlacesNearby.route) {
            PlacesNearbyScreenVM(
                homeNavController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(HomeRoutes.AdvancedMarkersAndRoutesSettings.route) {
            AdvancedMarkersAndRoutesSettingsScreenVM(
                navController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(HomeRoutes.OpenSourceLicense.route) {
            OpenSourceLicensesVM(
                navController = navController,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(HomeRoutes.OfflineMaps.route + "/{json}") { navBackStackEntry ->
            val urlEncodedJson = navBackStackEntry.arguments?.getString("json")

            // Parse the LocationDescription only once, not on every recomposition
            val locationDescription = remember(urlEncodedJson) {
                val gson = GsonBuilder().create()
                val json = URLDecoder.decode(urlEncodedJson, StandardCharsets.UTF_8.toString())
                gson.fromJson(json, LocationDescription::class.java)
            }

            OfflineMapsScreenVM(
                navController = navController,
                locationDescription = locationDescription,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
    }

        // Show tutorial dialog on top of all screens
        audioTourInstruction?.let { instruction ->
            AudioTourInstructionDialog(
                instruction = instruction,
                onContinue = { audioTour.onInstructionAcknowledged() }
            )
        }
    }
}

