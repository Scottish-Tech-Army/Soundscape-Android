package org.scottishtecharmy.soundscape.screens

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.screens.navigation.Screens


@Composable
fun Home(
    navController: NavHostController
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val window = (LocalView.current.context as Activity).window
    val statusBarColor = MaterialTheme.colorScheme.background

    SideEffect {
        window.statusBarColor = statusBarColor.toArgb()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent(drawerState, navController, coroutineScope) },
        gesturesEnabled = false,
    ) {
        Scaffold(
            topBar = {
                HomeTopAppBar(
                    drawerState,
                    coroutineScope,
                    navController
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
                navController,
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
    navController: NavHostController,
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
                contentDescription = "Close Side Menu",
                tint = Color.White
            )
        }
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = "Head Tracking Headphones",
            icon = Icons.Rounded.Headset
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label ="Settings",
            icon = Icons.Rounded.Settings
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = "Help & Tutorials",
            Icons.AutoMirrored.Rounded.HelpOutline
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = "Send Feedback",
            icon = Icons.Rounded.MailOutline
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = "Rate",
            icon = Icons.Rounded.Star
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = "Share",
            icon = Icons.Rounded.IosShare
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    navController: NavHostController
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
                    contentDescription = "Open Side Menu",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    notAvailableToast()
                    //navController.navigate(Screens.Sleeping.route)
                },
            ) {
                Icon(
                    Icons.Rounded.PhonelinkErase,
                    contentDescription = "Enable Battery Saver Mode",
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
                    text = "HEAR MY SURROUNDINGS",
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
                    Column() {
                        Icon(
                            painter = painterResource(R.drawable.my_location_24px),
                            contentDescription = "My Location",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = "My\nLocation",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { notAvailableToast() },
                    shape = RectangleShape
                ) {
                    Column() {
                        Icon(
                            painter = painterResource(R.drawable.around_me_24px),
                            contentDescription = "Around Me",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = "Around\nMe",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { notAvailableToast() },
                    shape = RectangleShape
                ) {
                    Column() {
                        Icon(
                            painter = painterResource(R.drawable.ahead_of_me_24px),
                            contentDescription = "Ahead of me",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = "Ahead\nof Me",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { notAvailableToast() },
                    shape = RectangleShape
                ) {
                    Column() {
                        Icon(
                            painter = painterResource(R.drawable.nearby_markers_24px),
                            contentDescription = "Nearby Markers",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Text(
                            softWrap = true,
                            text = "Nearby\nMarkers",
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
    navController: NavHostController,
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

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    SoundscapeTheme {
        Home(navController = rememberNavController())
    }
}
