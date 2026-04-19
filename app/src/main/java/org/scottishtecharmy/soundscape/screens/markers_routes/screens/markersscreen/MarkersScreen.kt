package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationList
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState

@Composable
fun MarkersScreenVM(
    homeNavController: NavController,
    userLocation: LngLatAlt?,
    viewModel: MarkersViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    uiState.userLocation = userLocation
    MarkersScreen(
        uiState,
        clearErrorMessage = { viewModel.clearErrorMessage() },
        onToggleSortOrder = { viewModel.toggleSortOrder() },
        onToggleSortByName = { viewModel.toggleSortByName() },
        userLocation = userLocation,
        onSelectItem = { desc ->
            homeNavController.navigate(generateLocationDetailsRoute(desc))
        },
        onShowError = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        },
        onStartBeacon = { location, name -> viewModel.startBeacon(location, name) }
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPopulatedPreview() {
    MarkersScreen(
        uiState =
            MarkersAndRoutesUiState(
                entries = previewLocationList
            ),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        userLocation = null,
        onSelectItem = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPreview() {
    MarkersScreen(
        uiState = MarkersAndRoutesUiState(),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        userLocation = null,
        onSelectItem = {}
    )
}
