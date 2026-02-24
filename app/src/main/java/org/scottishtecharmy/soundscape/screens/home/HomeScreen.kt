package org.scottishtecharmy.soundscape.screens.home

import android.content.SharedPreferences
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
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
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.MarkdownHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.HelpTopic


import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenVM
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
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel
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
data class BottomButtonFunctions(val viewModel: HomeViewModel?) {
    val myLocation = { viewModel?.myLocation() }
    val aheadOfMe = { viewModel?.aheadOfMe() }
    val aroundMe = { viewModel?.whatsAroundMe() }
    val nearbyMarkers = { viewModel?.nearbyMarkers() }
}

data class RouteFunctions(val viewModel: HomeViewModel?) {
    val skipPrevious = { viewModel?.routeSkipPrevious() }
    val skipNext = { viewModel?.routeSkipNext() }
    val mute = { viewModel?.routeMute() }
    val stop =  { viewModel?.routeStop() }
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
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    permissionsRequired: Boolean
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val searchText = viewModel.searchText.collectAsStateWithLifecycle()
    val routeFunctions = remember(viewModel) { RouteFunctions(viewModel) }
    val streetPreviewFunctions = remember(viewModel) { StreetPreviewFunctions(viewModel) }
    val bottomButtonFunctions = remember(viewModel) { BottomButtonFunctions(viewModel) }
    val onMapLongClickListener = remember(viewModel) {
        OnMapLongClickListener { latLong ->
            val location = LngLatAlt(latLong.longitude, latLong.latitude)
            val ld = viewModel.getLocationDescription(location) ?: LocationDescription("", location)
            navController.navigate(generateLocationDetailsRoute(ld))
            true
        }
    }


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
                searchText = searchText.value,
                onToggleSearch = viewModel::onToggleSearch,
                onSearchTextChange = viewModel::onSearchTextChange,
                rateSoundscape = rateSoundscape,
                contactSupport = contactSupport,
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
            Settings(
                onNavigateUp = { navController.navigateUp() },
                uiState = uiState.value,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                supportedLanguages = languageUiState.value.supportedLanguages,
                onLanguageSelected = { selectedLanguage ->
                    languageViewModel.updateLanguage(selectedLanguage)
                    settingsViewModel.updateLanguage(localActivity)
                 },
                selectedLanguageIndex = languageUiState.value.selectedLanguageIndex
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

            // Parse the LocationDescription out of the json provided by the caller
            val gson = GsonBuilder().create()
            val urlEncodedJson = navBackStackEntry.arguments?.getString("json")
            val json = URLDecoder.decode(urlEncodedJson, StandardCharsets.UTF_8.toString())
            val locationDescription = gson.fromJson(json, LocationDescription::class.java)

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
        composable(HomeRoutes.MarkersAndRoutes.route) {
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

            var routeData : RouteWithMarkers? = null
            when(command) {
                "import" -> {
                    try {
                        val json = URLDecoder.decode(data, StandardCharsets.UTF_8.toString())
                        routeData = parseSimpleRouteData(json)
                    } catch(e: Exception) {
                        Log.e("RouteDetailsScreen", "Error parsing route data: $e")
                    }
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
            val topicParam = backStackEntry.arguments?.getString("topic") ?: ""
            val helpTopic = HelpTopic.fromRouteParam(topicParam)
            val useMarkdownHelp = preferences.getBoolean(
                MainActivity.MARKDOWN_HELP_KEY,
                MainActivity.MARKDOWN_HELP_DEFAULT
            )
            val showMarkdown = (helpTopic is HelpTopic.Home && useMarkdownHelp)
                    || helpTopic is HelpTopic.MarkdownPage
                    || helpTopic is HelpTopic.MarkdownFaq

            if (showMarkdown) {
                MarkdownHelpScreen(
                    topic = topicParam,
                    navController = navController,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true }
                )
            } else {
                HelpScreen(
                    topic = topicParam,
                    navController = navController,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true }
                )
            }
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
    }
}

