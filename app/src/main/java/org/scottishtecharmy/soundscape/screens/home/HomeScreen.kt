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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.scottishtecharmy.soundscape.AppCallbacks
import org.scottishtecharmy.soundscape.AppFlows
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.navigation.NavigationStateHolder
import org.scottishtecharmy.soundscape.navigation.SharedNavHost
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.OfflineMapsScreenVM
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenVM
import org.scottishtecharmy.soundscape.screens.home.home.AdvancedMarkersAndRoutesSettingsScreenVM
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreenVM
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteScreenVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.parseSimpleRouteData
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageViewModel
import org.scottishtecharmy.soundscape.utils.AnalyticsProvider
import org.scottishtecharmy.soundscape.utils.getLanguageMismatch
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class Navigator {
    var destination = MutableStateFlow(SharedRoutes.HOME)

    fun navigate(newDestination: String) {
        Log.d("NavigationRoot", "Navigate to $newDestination")
        this.destination.value = newDestination
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    preferences: SharedPreferences,
    viewModel: HomeViewModel = koinViewModel(),
    audioTour: AudioTour,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    permissionsRequired: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as MainActivity
    val serviceConnection: SoundscapeServiceConnection = koinInject()

    val audioTourRunningFlow = remember(audioTour) { MutableStateFlow(audioTour.isRunning()) }
    LaunchedEffect(audioTour) {
        audioTour.currentInstruction.collect {
            audioTourRunningFlow.value = audioTour.isRunning()
        }
    }

    val recordingEnabledFlow = remember(preferences) {
        val initial = preferences.getBoolean(MainActivity.RECORD_TRAVEL_KEY, MainActivity.RECORD_TRAVEL_DEFAULT)
        MutableStateFlow(initial).also { flow ->
            preferences.registerOnSharedPreferenceChangeListener { sp, key ->
                if (key == MainActivity.RECORD_TRAVEL_KEY) {
                    flow.value = sp.getBoolean(MainActivity.RECORD_TRAVEL_KEY, MainActivity.RECORD_TRAVEL_DEFAULT)
                }
            }
        }
    }

    val permissionsRequiredFlow = remember(permissionsRequired) { MutableStateFlow(permissionsRequired) }
    val voiceCommandListeningFlow: StateFlow<Boolean> = remember(viewModel) {
        viewModel.state.map { it.voiceCommandListening }
            .stateIn(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate),
                started = SharingStarted.Eagerly,
                initialValue = false,
            )
    }

    val navStateHolder = remember { NavigationStateHolder() }

    val onMapLongClickListener: (LngLatAlt) -> Boolean = remember(viewModel, navStateHolder) {
        { lngLatAlt: LngLatAlt ->
            val ld = viewModel.getLocationDescription(lngLatAlt) ?: LocationDescription("", lngLatAlt)
            navStateHolder.setSelectedLocation(ld)
            navController.navigate(SharedRoutes.LOCATION_DETAILS)
            AnalyticsProvider.getInstance().logEvent("longPressOnMap", null)
            true
        }
    }

    val flows = remember(audioTour, audioTourRunningFlow, recordingEnabledFlow, permissionsRequiredFlow, voiceCommandListeningFlow) {
        AppFlows(
            homeState = viewModel.state,
            audioTourRunning = audioTourRunningFlow.asStateFlow(),
            audioTourInstruction = audioTour.currentInstruction,
            recordingEnabled = recordingEnabledFlow.asStateFlow(),
            permissionsRequired = permissionsRequiredFlow.asStateFlow(),
            voiceCommandListening = voiceCommandListeningFlow,
        )
    }

    val callbacks = remember(viewModel, audioTour, activity, rateSoundscape, contactSupport, serviceConnection) {
        AppCallbacks(
            onStartBeacon = { lat, lng, _ ->
                serviceConnection.soundscapeService?.createBeacon(LngLatAlt(lng, lat), headingOnly = false)
            },
            onStopBeacon = { serviceConnection.soundscapeService?.destroyBeacon() },
            onMyLocation = { viewModel.myLocation(); audioTour.onButtonPressed(TourButton.MY_LOCATION) },
            onWhatsAroundMe = { viewModel.whatsAroundMe(); audioTour.onButtonPressed(TourButton.AROUND_ME) },
            onAheadOfMe = { viewModel.aheadOfMe(); audioTour.onButtonPressed(TourButton.AHEAD_OF_ME) },
            onNearbyMarkers = { viewModel.nearbyMarkers(); audioTour.onButtonPressed(TourButton.NEARBY_MARKERS) },
            onRouteSkipPrevious = { viewModel.routeSkipPrevious() },
            onRouteSkipNext = { viewModel.routeSkipNext() },
            onRouteMute = { viewModel.routeMute() },
            onRouteStop = { viewModel.routeStop() },
            onSearch = { viewModel.onTriggerSearch(it) },
            onStreetPreviewGo = { viewModel.streetPreviewGo() },
            onStreetPreviewExit = { viewModel.streetPreviewExit() },
            onSleep = {
                activity.setServiceState(newServiceState = false, sleeping = true)
                navController.navigate(HomeRoutes.Sleep.route)
            },
            onShareRecording = { activity.shareRecording() },
            onRateApp = rateSoundscape,
            onContactSupport = contactSupport,
            onToggleAudioTour = { audioTour.toggleState() },
            onMapLongClick = onMapLongClickListener,
            onGoToAppSettings = { viewModel.goToAppSettings(context) },
            onGetCurrentLocationDescription = {
                val location = viewModel.state.value.location
                if (location != null) {
                    viewModel.getLocationDescription(location) ?: LocationDescription("", location)
                } else {
                    LocationDescription("", LngLatAlt())
                }
            },
            onSetApplicationLocale = { tag ->
                if (tag != null) {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(tag),
                    )
                }
            },
            onGetLanguageMismatch = { getLanguageMismatch(context) },
            getOpenSourceLicensesJson = {
                context.assets.open("open_source_licenses.json")
                    .bufferedReader()
                    .use { it.readText() }
            },
        )
    }

    SharedNavHost(
            navController = navController,
            navStateHolder = navStateHolder,
            flows = flows,
            callbacks = callbacks,
            startDestination = SharedRoutes.HOME,
            audioTour = audioTour,
            preferencesProvider = remember(preferences) { AndroidSharedPreferencesAdapter(preferences) },
            settingsContent = { navCtrl ->
                val settingsViewModel: SettingsViewModel = koinViewModel()
                val uiState by settingsViewModel.state.collectAsStateWithLifecycle()
                val languageViewModel: LanguageViewModel = koinViewModel()
                val languageUiState by languageViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    settingsViewModel.restartAppEvent.collect { activity.recreate() }
                }

                Settings(
                    navController = navCtrl,
                    uiState = uiState,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true },
                    supportedLanguages = languageUiState.supportedLanguages,
                    onLanguageSelected = { selectedLanguage ->
                        languageViewModel.updateLanguage(selectedLanguage)
                        settingsViewModel.updateLanguage(activity)
                    },
                    selectedLanguageIndex = languageUiState.selectedLanguageIndex,
                    storages = uiState.storages,
                    onStorageSelected = { path -> settingsViewModel.selectStorage(path) },
                    selectedStorageIndex = uiState.selectedStorageIndex,
                    resetSettings = { settingsViewModel.resetToDefaults() },
                )
            },
            platformNavBuilder = {
                composable(HomeRoutes.Language.route) {
                    LanguageScreen(
                        onNavigate = { navController.navigateUp() },
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                    )
                }

                composable(HomeRoutes.LocationDetails.route + "/{json}") { navBackStackEntry ->
                    val urlEncodedJson = navBackStackEntry.arguments?.getString("json")
                    val locationDescription = remember(urlEncodedJson) {
                        val gson = GsonBuilder().create()
                        val json = URLDecoder.decode(urlEncodedJson, StandardCharsets.UTF_8.toString())
                        gson.fromJson(json, LocationDescription::class.java)
                    }
                    LocationDetailsScreen(
                        locationDescription = locationDescription,
                        location = state.location,
                        navController = navController,
                        heading = state.heading,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                    )
                }

                composable(
                    HomeRoutes.MarkersAndRoutes.route + "?tab={tab}",
                    arguments = listOf(navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = ""
                    }),
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
                            .semantics { testTagsAsResourceId = true },
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
                        userLocation = state.location,
                        heading = state.heading,
                        routePlayerState = state.currentRouteData,
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
                        },
                    ),
                ) { backStackEntry ->
                    val command = backStackEntry.arguments?.getString("command") ?: ""
                    val data = backStackEntry.arguments?.getString("data") ?: ""
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
                    val addAndEditRouteViewModel: AddAndEditRouteViewModel = koinViewModel()
                    LaunchedEffect(data) {
                        addAndEditRouteViewModel.loadMarkers()
                        if (routeData != null) {
                            addAndEditRouteViewModel.initializeRouteFromData(routeData)
                        } else if (command == "edit") {
                            addAndEditRouteViewModel.initializeRouteFromDatabase(data.toLong())
                        }
                    }
                    AddAndEditRouteScreenVM(
                        navController = navController,
                        viewModel = addAndEditRouteViewModel,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                        userLocation = state.location,
                        heading = state.heading,
                        editRoute = (command == "edit"),
                        getCurrentLocationDescription = {
                            val location = state.location
                            if (location != null) {
                                viewModel.getLocationDescription(location) ?: LocationDescription("", location)
                            } else {
                                LocationDescription("", LngLatAlt())
                            }
                        },
                    )
                }

                composable(HomeRoutes.Sleep.route) {
                    SleepScreenVM(
                        navController = navController,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                    )
                }
                composable(HomeRoutes.PlacesNearby.route) {
                    PlacesNearbyScreenVM(
                        homeNavController = navController,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                    )
                }
                composable(HomeRoutes.AdvancedMarkersAndRoutesSettings.route) {
                    AdvancedMarkersAndRoutesSettingsScreenVM(
                        navController = navController,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                    )
                }
                composable(HomeRoutes.OfflineMaps.route + "/{json}") { navBackStackEntry ->
                    val urlEncodedJson = navBackStackEntry.arguments?.getString("json")
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
                            .semantics { testTagsAsResourceId = true },
                    )
                }
            },
        )
}

private class AndroidSharedPreferencesAdapter(
    private val prefs: SharedPreferences,
) : PreferencesProvider {
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}
