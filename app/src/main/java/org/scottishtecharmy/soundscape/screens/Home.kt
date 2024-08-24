package org.scottishtecharmy.soundscape.screens

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
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhonelinkErase
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton


@Preview(showBackground = true)
@Composable
fun Home() {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent(drawerState, coroutineScope) },
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
                HomeBottomAppBar()
            },
            floatingActionButton = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            HomeContent(
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
                }
            )
        }

    }
}

@Composable
fun DrawerContent(
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
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
            onClick = { notAvailableToast() },
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
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_rate),
            icon = Icons.Rounded.Star
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
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
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }
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
            IconButton(
                onClick = {
                    notAvailableToast()
                    //onNavigate(Screens.Sleeping.route)
                },
            ) {
                Icon(
                    Icons.Rounded.PhonelinkErase,
                    contentDescription = "Enable Battery Saver Mode",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    (context as MainActivity).stopServiceAndExit()
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = "Exit application",
                    tint = Color.White
                )
            }
        }
    )
}

@Composable
fun HomeBottomAppBar(

) {
    val context = LocalContext.current
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
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
                    onClick = { notAvailableToast() },
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
                    onClick = { notAvailableToast() },
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
                    onClick = { notAvailableToast() },
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
fun HomeContent(
    innerPadding: PaddingValues,
    searchBar: @Composable () -> Unit
) {
    val context = LocalContext.current
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }
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
                onClick = { notAvailableToast() },
                text = "Places Nearby"
            )
            // Markers and routes
            NavigationButton(
                onClick = { notAvailableToast() },
                text = "Markers & Routes"
            )
            // Current location
            NavigationButton(
                onClick = { notAvailableToast() },
                text = "Current Location"
            )
        }
    }
}
