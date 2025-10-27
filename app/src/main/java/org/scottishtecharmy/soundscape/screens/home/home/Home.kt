package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapLibreMap.OnMapLongClickListener
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.DrawerContent
import org.scottishtecharmy.soundscape.screens.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.viewmodels.home.HomeState

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun Home(
    state: HomeState,
    onNavigate: (String) -> Unit,
    preferences: SharedPreferences?,
    onMapLongClick: OnMapLongClickListener,
    bottomButtonFunctions: BottomButtonFunctions,
    getCurrentLocationDescription: () -> LocationDescription,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    routeFunctions: RouteFunctions,
    streetPreviewFunctions : StreetPreviewFunctions,
    modifier: Modifier = Modifier,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    goToAppSettings: (Context) -> Unit,
    permissionsRequired: Boolean
) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val fullscreenMap = remember { mutableStateOf(false) }
    val keyboardOpen = keyboardAsState()
    val routePlaying = (state.currentRouteData.routeData != null)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                onNavigate = onNavigate,
                drawerState = drawerState,
                rateSoundscape = rateSoundscape,
                contactSupport = contactSupport,
                shareRecording = { (context as MainActivity).shareRecording() },
                preferences = preferences
            )
        },
        modifier = modifier,
    ) {
        Scaffold(
            // If the keyboard is open, then we don't show the top or the bottom bars. This makes more
            // room for the search. This is important when the font size is very large, but it's
            // also good for allowing the user to view more search results.
            topBar = {
                if(!keyboardOpen.value) {
                    HomeTopAppBar(
                        drawerState,
                        coroutineScope,
                        onNavigate,
                        state.streetPreviewState.enabled != StreetPreviewEnabled.OFF
                    )
                }
            },
            bottomBar = {
                if(!fullscreenMap.value && !keyboardOpen.value)
                    HomeBottomAppBar(bottomButtonFunctions)
            },
            floatingActionButton = {
                if((!keyboardOpen.value) && showMap && (fullscreenMap.value || !routePlaying))
                    FullScreenMapFab(
                        fullscreenMap,
                        Modifier.testTag("homeFullScreenMap")
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            if (fullscreenMap.value) {
                state.location?.let { location ->
                    MapContainerLibre(
                        beaconLocation = state.beaconState?.location,
                        routeData = state.currentRouteData.routeData,
                        mapCenter = location,
                        allowScrolling = false,
                        userLocation = location,
                        userSymbolRotation = state.heading,
                        onMapLongClick = onMapLongClick,
                        modifier = modifier.fillMaxSize(),
                        showMap = showMap
                    )
                }
            } else {
                HomeContent(
                    location = state.location,
                    beaconState = state.beaconState,
                    routePlayerState = state.currentRouteData,
                    heading = state.heading,
                    modifier = modifier.padding(innerPadding),
                    onNavigate = onNavigate,
                    getCurrentLocationDescription = getCurrentLocationDescription,
                    searchBar = {
                        MainSearchBar(
                            searchText = searchText,
                            isSearching = state.isSearching,
                            itemList = state.searchItems.orEmpty(),
                            onSearchTextChange = onSearchTextChange,
                            onToggleSearch = onToggleSearch,
                            onItemClick = { item ->
                                onNavigate(
                                    generateLocationDetailsRoute(item),
                                )
                            },
                            userLocation = state.location
                        )
                    },
                    onMapLongClick = onMapLongClick,
                    streetPreviewState = state.streetPreviewState,
                    routeFunctions = routeFunctions,
                    streetPreviewFunctions = streetPreviewFunctions,
                    goToAppSettings = goToAppSettings,
                    fullscreenMap = fullscreenMap,
                    permissionsRequired = permissionsRequired,
                    showMap = showMap
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    onNavigate: (String) -> Unit,
    streetPreviewState: Boolean
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = if(streetPreviewState) (stringResource(R.string.preview_title)) else (stringResource(R.string.app_name)),
                modifier = Modifier.semantics { this.invisibleToUser() },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    coroutineScope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .talkbackHint(stringResource(R.string.ui_menu_hint))
                    .testTag("topBarMenu")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.ui_menu),
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        actions = {
            IconButton(
                enabled = true,
                onClick = {
                    (context as MainActivity).setServiceState(
                        newServiceState = false,
                        sleeping = true
                    )
                    onNavigate(HomeRoutes.Sleep.route)
                },
                modifier = Modifier
                    .talkbackHint(stringResource(R.string.sleep_sleep_acc_hint))
                    .testTag("topBarSleep")
            ) {
                Icon(Icons.Rounded.Snooze,
                    contentDescription = stringResource(R.string.sleep_sleep),
                    tint = MaterialTheme.colorScheme.onSurface,)
            }
        },
    )
}


@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
@Preview(showBackground = true)
@Composable
fun HomePreview() {
    SoundscapeTheme {
        Home(
            state = HomeState(),
            onNavigate = {},
            preferences = null,
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = {
                LocationDescription(
                    "Current Location",
                    LngLatAlt()
                )
            },
            rateSoundscape = {},
            contactSupport = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
            goToAppSettings = {},
            permissionsRequired = false
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true, fontScale = 3.13f)
@Preview(showBackground = true)
@Composable
fun HomeSearchPreview() {
    SoundscapeTheme {
        Home(
            state = HomeState(),
            onNavigate = {},
            preferences = null,
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = {
                LocationDescription(
                    "Current Location",
                    LngLatAlt()
                )
            },
            rateSoundscape = {},
            contactSupport = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
            goToAppSettings = {},
            permissionsRequired = false
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
@Preview(showBackground = true)
@Composable
fun HomeRoutePreview() {
    val routePlayerState = RoutePlayerState(
        routeData = RouteWithMarkers(
            RouteEntity(
                name = "NameOfRoute 1",
                description = "DescriptionOfRoute 1"
            ),
            emptyList()
        ),
        currentWaypoint = 0
    )
    SoundscapeTheme {
        Home(
            state = HomeState(
                heading = 90f,
                location = LngLatAlt(10.0, 10.0),
                currentRouteData = routePlayerState
            ),
            onNavigate = {},
            preferences = null,
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = {
                LocationDescription(
                    "Current Location",
                    LngLatAlt()
                )
            },
            rateSoundscape = {},
            contactSupport = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
            goToAppSettings = {},
            permissionsRequired = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StreetPreview() {
    SoundscapeTheme {
        Home(
            state = HomeState(
                streetPreviewState = StreetPreviewState(
                    StreetPreviewEnabled.ON,
                    listOf(
                        StreetPreviewChoice(45.0, "Main Street", Way())
                    )
                ),
                heading = 90f,
                location = LngLatAlt(10.0, 10.0)
            ),
            onNavigate = {},
            preferences = null,
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = {
                LocationDescription(
                    "Current Location",
                    LngLatAlt()
                )
            },
            rateSoundscape = {},
            contactSupport = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
            goToAppSettings = {},
            permissionsRequired = false
        )
    }
}

val previewLocationList = listOf(
    LocationDescription(
        name = "Barrowland Ballroom",
        description = "Somewhere in Glasgow",
        location = LngLatAlt(-4.2366753, 55.8552688)
    ),
    LocationDescription(
        name = "King Tut's Wah Wah Hut",
        description = "Somewhere else in Glasgow",
        location = LngLatAlt(-4.2649646, 55.8626180)
    ),
    LocationDescription(
        name = "St. Lukes and the Winged Ox",
        description = "Where else?",
        location = LngLatAlt( -4.2347580, 55.8546320)
    )
)
