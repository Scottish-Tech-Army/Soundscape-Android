package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.screens.home.DrawerContent
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun HomePreview() {
    Home(
        latitude = null,
        longitude = null,
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
        streetPreviewEnabled = false,
        tileGridGeoJson = "",
        searchText = "Lille",
        isSearching = true,
        onSearchTextChange = {},
        onToggleSearch = {},
        searchItems = emptyList(),
    )
}

@Composable
fun Home(
    latitude: Double?,
    longitude: Double?,
    beaconLocation: LatLng?,
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
    streetPreviewEnabled: Boolean,
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
                    streetPreviewEnabled,
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
                latitude = latitude,
                longitude = longitude,
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
                                generateLocationDetailsRoute(
                                    LocationDescription(
                                        addressName = item.addressName,
                                        streetNumberAndName = item.streetNumberAndName,
                                        postcodeAndLocality = item.postcodeAndLocality,
                                        country = item.country,
                                        distance = item.distance,
                                        latitude = item.latitude,
                                        longitude = item.longitude,
                                    ),
                                ),
                            )
                        },
                    )
                },
                onMapLongClick = onMapLongClick,
                onMarkerClick = onMarkerClick,
                tileGridGeoJson = tileGridGeoJson,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    streetPreviewEnabled: Boolean,
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
            var serviceRunning by remember { mutableStateOf(true) }
            IconButton(
                enabled = streetPreviewEnabled,
                onClick = {
                    if(streetPreviewEnabled) {
                        (context as MainActivity).soundscapeServiceConnection.streetPreviewGo()
                    }
                },
            ) {
                if (streetPreviewEnabled) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "StreetPreview play"
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayCircleOutline,
                        tint = MaterialTheme.colorScheme.secondary,
                        contentDescription = "StreetPreview play disabled"
                    )
                }
            }
            IconToggleButton(
                checked = streetPreviewEnabled,
                enabled = true,
                onCheckedChange = { state ->
                    if (!state) {
                        (context as MainActivity).soundscapeServiceConnection.setStreetPreviewMode(
                            false,
                        )
                    }
                },
            ) {
                if (streetPreviewEnabled) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "StreetPreview play"
                    )
                    Icon(
                        Icons.Rounded.Preview,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.street_preview_enabled),
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayCircleOutline,
                        tint = MaterialTheme.colorScheme.secondary,
                        contentDescription = "StreetPreview play disabled"
                    )
                    Icon(
                        painterResource(R.drawable.preview_off),
                        tint = MaterialTheme.colorScheme.secondary,
                        contentDescription = stringResource(R.string.street_preview_disabled),
                    )
                }
            }
            IconToggleButton(
                checked = serviceRunning,
                enabled = true,
                onCheckedChange = { state ->
                    serviceRunning = state
                    (context as MainActivity).toggleServiceState(state)
                },
            ) {
                if (serviceRunning) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = "Service running")
                } else {
                    Icon(Icons.Rounded.LocationOff, contentDescription = "Service stopped")
                }
            }
        },
    )
}
