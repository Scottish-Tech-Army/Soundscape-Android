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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.DrawerContent
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.ui.theme.OnPrimary
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun Home(
    location: LngLatAlt?,
    beaconLocation: LngLatAlt?,
    heading: Float,
    onNavigate: (String) -> Unit,
    onMapLongClick: (LatLng) -> Boolean,
    onMarkerClick: (Marker) -> Boolean,
    getMyLocation: () -> Unit,
    getWhatsAroundMe: () -> Unit,
    getWhatsAheadOfMe: () -> Unit,
    getCurrentLocationDescription: () -> LocationDescription,
    shareLocation: () -> Unit,
    rateSoundscape: () -> Unit,
    streetPreviewState: StreetPreviewState,
    streetPreviewGo: () -> Unit,
    streetPreviewExit: () -> Unit,
    modifier: Modifier = Modifier,
    tileGridGeoJson: String,
    searchText: String,
    isSearching: Boolean,
    onSearchTextChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    searchItems: List<LocationDescription>,
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
                HomeBottomAppBar(
                    getMyLocation = getMyLocation,
                    getWhatsAroundMe = getWhatsAroundMe,
                    getWhatsAheadOfMe = getWhatsAheadOfMe,
                )
            },
            floatingActionButton = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            HomeContent(
                location = location,
                beaconLocation = beaconLocation,
                heading = heading,
                modifier = Modifier.padding(innerPadding),
                onNavigate = onNavigate,
                getCurrentLocationDescription = getCurrentLocationDescription,
                searchBar = {
                    MainSearchBar(
                        searchText = searchText,
                        isSearching = isSearching,
                        itemList = searchItems,
                        onSearchTextChange = onSearchTextChange,
                        onToggleSearch = onToggleSearch,
                        onItemClick = { item ->
                            onNavigate(
                                generateLocationDetailsRoute(item),
                            )
                        },
                    )
                },
                onMapLongClick = onMapLongClick,
                onMarkerClick = onMarkerClick,
                tileGridGeoJson = tileGridGeoJson,
                streetPreviewState = streetPreviewState,
                streetPreviewGo = streetPreviewGo,
                streetPreviewExit = streetPreviewExit
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
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        title = {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    coroutineScope.launch { drawerState.open() }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.ui_menu),
                    tint = Color.White,
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
            ) {
                Icon(Icons.Rounded.Snooze,
                    tint = OnPrimary,
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
            location = null,
            beaconLocation = null,
            heading = 0.0f,
            onNavigate = {},
            onMapLongClick = { false },
            onMarkerClick = { true },
            getMyLocation = {},
            getWhatsAroundMe = {},
            getWhatsAheadOfMe = {},
            getCurrentLocationDescription = { LocationDescription() },
            shareLocation = {},
            rateSoundscape = {},
            streetPreviewState = StreetPreviewState(StreetPreviewEnabled.OFF),
            streetPreviewExit = {},
            streetPreviewGo = {},
            tileGridGeoJson = "",
            searchText = "Lille",
            isSearching = false,
            onSearchTextChange = {},
            onToggleSearch = {},
            searchItems = emptyList(),
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
@Preview(showBackground = true)
@Composable
fun HomeSearchPreview() {
    SoundscapeTheme {
        Home(
            location = null,
            beaconLocation = null,
            heading = 0.0f,
            onNavigate = {},
            onMapLongClick = { false },
            onMarkerClick = { true },
            getMyLocation = {},
            getWhatsAroundMe = {},
            getWhatsAheadOfMe = {},
            getCurrentLocationDescription = { LocationDescription() },
            shareLocation = {},
            rateSoundscape = {},
            streetPreviewState = StreetPreviewState(StreetPreviewEnabled.OFF),
            streetPreviewExit = {},
            streetPreviewGo = {},
            tileGridGeoJson = "",
            searchText = "Lille",
            isSearching = true,
            onSearchTextChange = {},
            onToggleSearch = {},
            searchItems = listOf(
                LocationDescription(
                    addressName = "Barrowland Ballroom",
                    fullAddress = "Somewhere in Glasgow",
                    distance = "10 km",
                    location = LngLatAlt(-4.2366753, 55.8552688)
                ),
                LocationDescription(
                    addressName = "King Tut's Wah Wah Hut",
                    fullAddress = "Somewhere else in Glasgow",
                    distance = "999 metres",
                    location = LngLatAlt(-4.2649646, 55.8626180)
                ),
                LocationDescription(
                    addressName = "St. Lukes and the Winged Ox",
                    fullAddress = "Where else?",
                    distance = "12km",
                    location = LngLatAlt( -4.2347580, 55.8546320)
                )
            )
        )
    }
}
