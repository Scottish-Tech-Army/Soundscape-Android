package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.analyticsEnabled
import org.scottishtecharmy.soundscape.platform.appVersionMinorTrimmed
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreference
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.search_bar_hint
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SharedHomeScreen(
    state: HomeState,
    onNavigate: (String) -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    preferencesProvider: PreferencesProvider?,
    onMapLongClick: ((LngLatAlt) -> Boolean)?,
    bottomButtonFunctions: BottomButtonFunctions,
    routeFunctions: RouteFunctions,
    streetPreviewFunctions: StreetPreviewFunctions,
    searchFunctions: SearchFunctions,
    getCurrentLocationDescription: () -> LocationDescription,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    shareRecording: () -> Unit,
    exitApp: () -> Unit = {},
    toggleTutorial: () -> Unit,
    tutorialRunning: Boolean,
    recordingEnabled: Boolean,
    voiceCommandListening: Boolean,
    permissionsRequired: Boolean,
    goToAppSettings: () -> Unit,
    onSleep: () -> Unit,
    onSetApplicationLocale: (String?) -> Unit,
    getLanguageMismatch: () -> org.scottishtecharmy.soundscape.screens.onboarding.language.Language?,
    modifier: Modifier = Modifier,
) {
    val showMap by rememberBooleanPreference(
        preferencesProvider,
        PreferenceKeys.SHOW_MAP,
        PreferenceDefaults.SHOW_MAP,
    )
    var drawerOpen by remember { mutableStateOf(false) }
    val fullscreenMap = remember { mutableStateOf(false) }
    val keyboardOpen = keyboardAsState()
    val routePlaying = (state.currentRouteData.routeData != null)

    val newReleaseDialog = remember {
        mutableStateOf(
            (preferencesProvider?.getString(
                PreferenceKeys.LAST_NEW_RELEASE,
                PreferenceDefaults.LAST_NEW_RELEASE
            )
                    != appVersionMinorTrimmed()) &&
                    analyticsEnabled,
        )
    }
    val phoneLanguage = remember { getLanguageMismatch() }
    val languageMismatchDialog = remember {
        mutableStateOf(
            (phoneLanguage != null &&
                    (preferencesProvider?.getBoolean(
                        PreferenceKeys.LANGUAGE_SUPPORTED_PROMPTED,
                        PreferenceDefaults.LANGUAGE_SUPPORTED_PROMPTED,
                    ) == false)) &&
                    analyticsEnabled,
        )
    }

    val currentLocation by rememberUpdatedState(state.location)
    val offlineMaps = remember(onNavigate) {
        {
            // Carry the current location through the offline maps refresh implicitly via state.
            currentLocation?.let { _ -> }
            onNavigate(SharedRoutes.OFFLINE_MAPS)
        }
    }

    Scaffold(
        modifier = if (drawerOpen) modifier.clearAndSetSemantics { } else modifier,
        // If the keyboard is open, then we don't show the top or the bottom bars. This makes more
        // room for the search. This is important when the font size is very large, but it's
        // also good for allowing the user to view more search results.
        topBar = {
            if (!keyboardOpen.value) {
                SharedHomeTopAppBar(
                    onMenuClick = { drawerOpen = true },
                    streetPreviewState = state.streetPreviewState.enabled != StreetPreviewEnabled.OFF,
                    streetPreviewFunctions = streetPreviewFunctions,
                    onSleep = onSleep,
                )
            }
        },
        bottomBar = {
            if (!fullscreenMap.value && !keyboardOpen.value) {
                SharedHomeBottomAppBar(bottomButtonFunctions)
            }
        },
        floatingActionButton = {
            if ((!keyboardOpen.value) && showMap && (fullscreenMap.value || !routePlaying)) {
                FullScreenMapFab(
                    fullscreenMap,
                    Modifier.testTag("homeFullScreenMap"),
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (languageMismatchDialog.value && phoneLanguage != null) {
            SharedLanguageMismatchDialog(
                innerPadding = innerPadding,
                preferencesProvider = preferencesProvider,
                showDialog = languageMismatchDialog,
                phoneLanguage = phoneLanguage,
                onSetApplicationLocale = onSetApplicationLocale,
            )
        } else if (newReleaseDialog.value) {
            SharedNewReleaseDialog(
                innerPadding = innerPadding,
                preferencesProvider = preferencesProvider,
                newReleaseDialog = newReleaseDialog,
            )
        }

        if (fullscreenMap.value && showMap) {
            state.location?.let { location ->
                PlatformMapContainer(
                    beaconLocation = state.beaconState?.location,
                    routeData = state.currentRouteData.routeData,
                    currentBeaconWaypointIndex = state.currentRouteData.currentWaypoint,
                    mapCenter = location,
                    allowScrolling = false,
                    userLocation = location,
                    userSymbolRotation = state.heading,
                    modifier = modifier.fillMaxSize(),
                )
            }
        } else {
            SharedHomeContent(
                location = state.location,
                beaconState = state.beaconState,
                routePlayerState = state.currentRouteData,
                heading = state.heading,
                modifier = modifier.padding(innerPadding),
                onNavigate = onNavigate,
                onSelectLocation = onSelectLocation,
                getCurrentLocationDescription = getCurrentLocationDescription,
                searchBar = {
                    MainSearchBar(
                        results = state.searchItems.orEmpty(),
                        onTriggerSearch = searchFunctions.onTriggerSearch,
                        onItemClick = { item -> onSelectLocation(item) },
                        hint = stringResource(Res.string.search_bar_hint),
                        userLocation = state.location,
                        isSearching = state.searchInProgress,
                    )
                },
                onMapLongClick = onMapLongClick,
                streetPreviewState = state.streetPreviewState,
                routeFunctions = routeFunctions,
                streetPreviewFunctions = streetPreviewFunctions,
                goToAppSettings = goToAppSettings,
                fullscreenMap = fullscreenMap,
                permissionsRequired = permissionsRequired,
                showMap = showMap,
                voiceCommandListening = voiceCommandListening,
            )
        }
    }

    AnimatedVisibility(
        visible = drawerOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        SharedDrawerContent(
            onClose = { drawerOpen = false },
            onNavigate = onNavigate,
            rateSoundscape = rateSoundscape,
            contactSupport = contactSupport,
            shareRecording = shareRecording,
            offlineMaps = offlineMaps,
            toggleTutorial = toggleTutorial,
            tutorialRunning = tutorialRunning,
            recordingEnabled = recordingEnabled,
            newReleaseDialog = newReleaseDialog,
            exitApp = exitApp,
        )
    }

    // Deprecated in CMP 1.11 in favor of androidx.navigationevent's NavigationEventHandler,
    // but that reads LocalNavigationEventDispatcherOwner, which CMP only wires up via the
    // internal compat local this BackHandler uses - migrating would break back handling on
    // iOS. Revisit once CMP exposes the public local (JetBrains/compose-multiplatform).
    @Suppress("DEPRECATION")
    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }
}
