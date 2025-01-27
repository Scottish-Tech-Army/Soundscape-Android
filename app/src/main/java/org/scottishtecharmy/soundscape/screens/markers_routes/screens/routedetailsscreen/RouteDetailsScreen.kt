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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    routeName: String,
    viewModel: RouteDetailsViewModel = hiltViewModel()
) {
    // Observe the UI state from the ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Fetch the route details when the screen is launched
    LaunchedEffect(routeName) {
        viewModel.getRouteByName(routeName)
    }

    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = "Route Details",
                onNavigateUp = {
                    navController.navigate(ScreensForMarkersAndRoutes.Routes.route) {
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
                    val route = uiState.selectedRoute!!
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
                                iconText = "Start Route",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { /*TODO*/ })
                            IconWithTextButton(
                                icon = Icons.Default.Edit,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = "Edit Route",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { navController.navigate("${HomeRoutes.EditRoute.route}/${route.name}") })
                            IconWithTextButton(
                                icon = Icons.Default.Share,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = "Share Route",
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
