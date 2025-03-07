package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesListSort
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.ui.theme.tinyPadding

@Composable
fun RoutesScreenVM(
    homeNavController: NavController,
    viewModel: RoutesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RoutesScreen(
        homeNavController,
        uiState,
        clearErrorMessage = { viewModel.clearErrorMessage()},
        onToggleSortOrder = { viewModel.toggleSortOrder() },
        onToggleSortByName = { viewModel.toggleSortByName() }
    )
}


@Composable
fun RoutesScreen(
    homeNavController: NavController,
    uiState: RoutesUiState,
    clearErrorMessage: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Display error message if it exists
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                clearErrorMessage()
            }
        }

        // Display loading state
        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.routes.isEmpty()) {
                    // Display UI when no routes are available
                        Box(modifier = Modifier.padding(top = spacing.large)) {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_routes
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(spacing.targetSize * 2),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(modifier = Modifier.mediumPadding()) {
                            Text(
                                stringResource(R.string.routes_no_routes_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(modifier = Modifier.mediumPadding()) {
                            Text(
                                stringResource(R.string.routes_no_routes_hint_1),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(modifier = Modifier.mediumPadding()) {
                            Text(
                                stringResource(R.string.routes_no_routes_hint_2),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                    }
                } else {
                    MarkersAndRoutesListSort(
                        isSortByName = uiState.isSortByName,
                        isAscending = uiState.isSortAscending,
                        onToggleSortOrder = onToggleSortOrder,
                        onToggleSortByName = onToggleSortByName
                    )

                    // Display the list of routes
                    RouteList(
                        uiState = uiState,
                        navController = homeNavController
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(
        homeNavController = rememberNavController(),
        uiState = RoutesUiState(),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPopulatedPreview() {
    RoutesScreen(
        homeNavController = rememberNavController(),
        uiState = RoutesUiState(
            routes = listOf(
                RouteData("Route 1", "Description A"),
                RouteData("Route 2", "Description B"),
                RouteData("Route 3", "Description C"),
                RouteData("Route 4", "Description D"),
                RouteData("Route 5", "Description E"),
                RouteData("Route 6", "Description F"),
                RouteData("Route 7", "Description G"),
                RouteData("Route 8", "Description H"),
                RouteData("Route 9", "Description I"),
                RouteData("Route 10", "Description J"),
                RouteData("Route 11", "Description K"),
                RouteData("Route 12", "Description L"),
                RouteData("Route 13", "Description M"),
            )
        ),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenLoadingPreview() {
    RoutesScreen(
        homeNavController = rememberNavController(),
        uiState = RoutesUiState(isLoading = true),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {}
    )
}
