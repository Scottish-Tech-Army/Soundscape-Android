package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.DrawerContent
import org.scottishtecharmy.soundscape.screens.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.viewmodels.home.HomeState

@Composable
fun Home(
    state: HomeState,
    onNavigate: (String) -> Unit,
    onMapLongClick: (LatLng) -> Boolean,
    bottomButtonFunctions: BottomButtonFunctions,
    getCurrentLocationDescription: () -> LocationDescription,
    shareLocation: () -> Unit,
    rateSoundscape: () -> Unit,
    routeFunctions: RouteFunctions,
    streetPreviewFunctions : StreetPreviewFunctions,
    modifier: Modifier = Modifier,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigate = onNavigate,
                drawerState = drawerState,
                shareLocation = shareLocation,
                rateSoundscape = rateSoundscape,
            )
        },
        gesturesEnabled = false,
        modifier = modifier,
    ) {
        Scaffold(
            topBar = {
                HomeTopAppBar(
                    drawerState,
                    coroutineScope,
                    onNavigate
                )
            },
            bottomBar = {
                HomeBottomAppBar(bottomButtonFunctions)
            },
            floatingActionButton = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            HomeContent(
                location = state.location,
                beaconLocation = state.beaconLocation,
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
                streetPreviewFunctions = streetPreviewFunctions
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val sleepHint = stringResource(R.string.sleep_sleep_acc_hint)
    val menuHint = stringResource(R.string.ui_menu_hint)

    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        title = {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.semantics { this.invisibleToUser() },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    coroutineScope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .semantics { onClick(label = menuHint, action = { false }) }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.ui_menu),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        actions = {
            IconButton(
                enabled = true,
                onClick = {
                    (context as MainActivity).setServiceState(false)
                    onNavigate(HomeRoutes.Sleep.route)
                },
                modifier = Modifier
                    .semantics { onClick(label = sleepHint, action = { false }) }
            ) {
                Icon(Icons.Rounded.Snooze,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = stringResource(R.string.sleep_sleep))
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
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = { LocationDescription("Current Location", LngLatAlt()) },
            shareLocation = {},
            rateSoundscape = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
@Preview(showBackground = true)
@Composable
fun HomeSearchPreview() {
    SoundscapeTheme {
        Home(
            state = HomeState(),
            onNavigate = {},
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = { LocationDescription("Current Location", LngLatAlt()) },
            shareLocation = {},
            rateSoundscape = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
@Preview(showBackground = true)
@Composable
fun HomeRoutePreview() {
    val routePlayerState = RoutePlayerState(
        routeData = RouteData(
            name = "Route 1",
            description = "Description 1"
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
            onMapLongClick = { false },
            bottomButtonFunctions = BottomButtonFunctions(null),
            getCurrentLocationDescription = { LocationDescription("Current Location", LngLatAlt()) },
            shareLocation = {},
            rateSoundscape = {},
            searchText = "Lille",
            onSearchTextChange = {},
            onToggleSearch = {},
            routeFunctions = RouteFunctions(null),
            streetPreviewFunctions = StreetPreviewFunctions(null),
        )
    }
}

val previewLocationList = listOf(
    LocationDescription(
        name = "Barrowland Ballroom",
        fullAddress = "Somewhere in Glasgow",
        location = LngLatAlt(-4.2366753, 55.8552688),
        markerObjectId = ObjectId()
    ),
    LocationDescription(
        name = "King Tut's Wah Wah Hut",
        fullAddress = "Somewhere else in Glasgow",
        location = LngLatAlt(-4.2649646, 55.8626180),
        markerObjectId = ObjectId()
    ),
    LocationDescription(
        name = "St. Lukes and the Winged Ox",
        fullAddress = "Where else?",
        location = LngLatAlt( -4.2347580, 55.8546320),
        markerObjectId = ObjectId()
    )
)
