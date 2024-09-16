package org.scottishtecharmy.soundscape.screens.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.maps.MapView
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.viewmodels.AheadOfMeViewModel
import org.scottishtecharmy.soundscape.viewmodels.DrawerViewModel
import org.scottishtecharmy.soundscape.viewmodels.HomeViewModel
import org.scottishtecharmy.soundscape.viewmodels.MyLocationViewModel
import org.scottishtecharmy.soundscape.viewmodels.WhatsAroundMeViewModel


@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun HomePreview() {
    Home({}, false)
}

@Composable
fun Home(onNavigate: (String) -> Unit, useView : Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent(onNavigate, drawerState, coroutineScope, useView) },
        gesturesEnabled = false,
    ) {
        Scaffold(
            topBar = {
                HomeTopAppBar(
                    drawerState,
                    coroutineScope
                )
            },
            bottomBar = {
                HomeBottomAppBar(useView)
            },
            floatingActionButton = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            HomeContent(
                onNavigate,
                innerPadding,
                searchBar = {
                    MainSearchBar(
                        searchText = "Hello Dave",
                        isSearching = false,
                        itemList = emptyList(),
                        onSearchTextChange = {  },
                        onToggleSearch = {  },
                        onItemClick = {  }
                    )
                },
                useView,
            )
        }

    }
}

@Composable
fun DrawerContent(
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
    useView : Boolean
) {
    val context = LocalContext.current
    var drawerViewModel : DrawerViewModel? = null
    if(useView)
        drawerViewModel = hiltViewModel<DrawerViewModel>()

    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    if (drawerState.isClosed){
                        drawerState.open()
                    }else{
                        drawerState.close()
                    }
                }
            },
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 4.dp),
                contentDescription = stringResource(R.string.ui_menu_close),
                tint = Color.White
            )
        }
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_devices),
            icon = Icons.Rounded.Headset
        )
        DrawerMenuItem(
            onClick = { onNavigate(MainScreens.Settings.route) },
            // Weirdly, original iOS Soundscape doesn't seem to have translation strings for "Settings"
            label = stringResource(R.string.general_alert_settings),
            icon = Icons.Rounded.Settings
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_help_and_tutorials),
            Icons.AutoMirrored.Rounded.HelpOutline
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_send_feedback),
            icon = Icons.Rounded.MailOutline
        )
        DrawerMenuItem(
            onClick = { drawerViewModel?.rateSoundscape(context as MainActivity) },
            label = stringResource(R.string.menu_rate),
            icon = Icons.Rounded.Star
        )
        DrawerMenuItem(
            onClick = { drawerViewModel?.shareLocation(context) },
            label = stringResource(R.string.share_title),
            icon = Icons.Rounded.IosShare
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = Color.White,
        ),
        title = { Text(stringResource(R.string.app_name)) },
        navigationIcon = {
            IconButton(
                onClick = {
                    coroutineScope.launch { drawerState.open() }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.ui_menu),
                    tint = Color.White
                )
            }
        },
        actions = {
            var serviceRunning by remember { mutableStateOf(true) }
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
        }
    )
}

@Composable
fun HomeBottomAppBar(
    useView : Boolean = true
) {
    val context = LocalContext.current
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }

    var myLocationViewModel : MyLocationViewModel? = null
    var whatsAroundMeViewModel : WhatsAroundMeViewModel? = null
    var aheadOfMeViewModel: AheadOfMeViewModel? = null
    if(useView){
        myLocationViewModel = hiltViewModel<MyLocationViewModel>()
        whatsAroundMeViewModel = hiltViewModel<WhatsAroundMeViewModel>()
        aheadOfMeViewModel = hiltViewModel<AheadOfMeViewModel>()
    }




    BottomAppBar(
        modifier = Modifier
            .height(150.dp)
            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    text = stringResource(R.string.callouts_panel_title).uppercase(),
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {

                Button(
                    onClick = {
                        myLocationViewModel?.myLocation()
                    },
                    shape = RectangleShape
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.my_location_24px),
                            contentDescription = stringResource(R.string.user_activity_my_location_title),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_my_location),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { whatsAroundMeViewModel?.whatsAroundMe() },
                    shape = RectangleShape
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.around_me_24px),
                            contentDescription = stringResource(R.string.user_activity_around_me_title),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_around_me),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { aheadOfMeViewModel?.aheadOfMe() },
                    shape = RectangleShape
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.ahead_of_me_24px),
                            contentDescription = stringResource(R.string.user_activity_ahead_of_me_title),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_ahead_of_me),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { notAvailableToast() },
                    shape = RectangleShape
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.nearby_markers_24px),
                            contentDescription = stringResource(R.string.user_activity_nearby_markers_title),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_nearby_markers),
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun MapContainerLibre(viewModel: HomeViewModel) {
    AndroidView(
        factory = { context ->
            MapLibre.getInstance(context, BuildConfig.TILE_PROVIDER_API_KEY, WellKnownTileServer.MapTiler)
            val mapView = MapView(context)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                viewModel.setMap(map)
            }
            mapView
        }
    )
}

@Composable
fun HomeContent(
    onNavigate: (String) -> Unit,
    innerPadding: PaddingValues,
    searchBar: @Composable () -> Unit,
    useView : Boolean
) {
    var viewModel : HomeViewModel? = null

    if(useView)
        viewModel = hiltViewModel<HomeViewModel>()

    Column(
        modifier = Modifier
            .padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        searchBar()

        Column(
            verticalArrangement = Arrangement.spacedBy((1).dp),
        ) {
            // Places Nearby
            NavigationButton(
                onClick = {
                    // This isn't the final code, just an example of how the LocationDetails could work
                    // The LocationDetails screen takes some JSON as an argument which tells the
                    // screen which location to provide details of. The JSON is appended to the route.
                    val ld = LocationDescription(
                        "Barrowland Ballroom",55.8552688,-4.2366753
                    )
                    onNavigate(generateLocationDetailsRoute(ld))
                },
                text = stringResource(R.string.search_nearby_screen_title)
            )
            // Markers and routes
            NavigationButton(
                onClick = { onNavigate("${MainScreens.MarkersAndRoutes.route}/markers")
                            Log.d("Navigation", "NavController: ${MainScreens.MarkersAndRoutes.route}/markers") },
                text = stringResource(R.string.search_view_markers)
            )
            // Current location
            NavigationButton(
                onClick = {
                    if(viewModel != null) {
                        // The LocationDetails screen takes some JSON as an argument which tells the
                        // screen which location to provide details of. The JSON is appended to the route.
                        val ld = LocationDescription(
                            "Current location",
                            viewModel.latitude,
                            viewModel.longitude
                        )
                        onNavigate(generateLocationDetailsRoute(ld))
                    }
                },
                text = stringResource(R.string.search_use_current_location)
            )
            if(viewModel != null)
                MapContainerLibre(viewModel = viewModel)
        }
    }
}