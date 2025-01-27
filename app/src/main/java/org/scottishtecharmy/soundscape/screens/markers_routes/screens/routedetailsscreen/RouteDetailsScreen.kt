package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun RouteDetailsScreenVM(
    navController: NavController,
    routeName: String,
    viewModel: RouteDetailsViewModel = hiltViewModel(),
    modifier: Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RouteDetailsScreen(
        navController,
        routeName,
        modifier,
        uiState,
        getRouteByName = { viewModel.getRouteByName(routeName) },
        clearErrorMessage = { viewModel.clearErrorMessage() }
    )
}

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    routeName: String,
    modifier: Modifier,
    uiState: RouteDetailsUiState,
    getRouteByName: (routeName: String) -> Unit,
    clearErrorMessage: () -> Unit,
) {
    // Observe the UI state from the ViewModel
    val context = LocalContext.current

    // Fetch the route details when the screen is launched
    LaunchedEffect(routeName) {
        getRouteByName(routeName)
    }

    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = stringResource(R.string.behavior_experiences_route_nav_title),
                onNavigateUp = {
                    navController.navigate(HomeRoutes.MarkersAndRoutes.route) {
                        popUpTo(HomeRoutes.MarkersAndRoutes.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.selectedRoute != null -> {
                    val route = uiState.selectedRoute
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = route.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = route.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            // Display additional route details if necessary
                        }
                        Column {
                            IconWithTextButton(
                                icon = Icons.Default.PlayArrow,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = stringResource(R.string.route_detail_action_start_route),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { /*TODO*/ })
                            IconWithTextButton(
                                icon = Icons.Default.Edit,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = stringResource(R.string.route_detail_action_edit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { navController.navigate("${HomeRoutes.EditRoute.route}/${route.name}") })
                            IconWithTextButton(
                                icon = Icons.Default.Share,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = stringResource(R.string.share_title),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { /*TODO*/ })
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Route not found")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsPopulatedPreview() {
    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeName = "Route name",
            modifier = Modifier,
            uiState = RouteDetailsUiState(
                route = listOf(
                    RouteData("Route 1", "Description 1"),
                    RouteData("Route 2", "Description 2"),
                    RouteData("Route 3", "Description 3"),
                ),
                selectedRoute = RouteData("Route 2", "Description 2")
            ),
            getRouteByName = {},
            clearErrorMessage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsLoadingPreview() {
    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeName = "Route name",
            uiState = RouteDetailsUiState(isLoading = true),
            modifier = Modifier,
            getRouteByName = {},
            clearErrorMessage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsEmptyPreview() {
    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeName = "Route name",
            modifier = Modifier,
            uiState = RouteDetailsUiState(),
            getRouteByName = {},
            clearErrorMessage = {}
        )
    }
}
