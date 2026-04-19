package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationList
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState

@Composable
fun RoutesScreenVM(
    homeNavController: NavController,
    userLocation: LngLatAlt?,
    viewModel: RoutesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    uiState.userLocation = userLocation

    RoutesScreen(
        uiState,
        userLocation,
        clearErrorMessage = { viewModel.clearErrorMessage()},
        onToggleSortOrder = { viewModel.toggleSortOrder() },
        onToggleSortByName = { viewModel.toggleSortByName() },
        onSelectItem = { desc ->
            homeNavController.navigate("${HomeRoutes.RouteDetails.route}/${desc.databaseId}")
        },
        onShowError = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        },
        onStartPlayback = { routeId -> viewModel.startRoute(routeId) }
    )
}


@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(
        uiState = MarkersAndRoutesUiState(),
        userLocation = null,
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        onSelectItem = {}
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPopulatedPreview() {
    RoutesScreen(
        uiState = MarkersAndRoutesUiState(
            entries = previewLocationList
        ),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        onSelectItem = {},
        userLocation = null
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenLoadingPreview() {
    RoutesScreen(
        uiState = MarkersAndRoutesUiState(isLoading = true),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        onSelectItem = {},
        userLocation = null
    )
}
