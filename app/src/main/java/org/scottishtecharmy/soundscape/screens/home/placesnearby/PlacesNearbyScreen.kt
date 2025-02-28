package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationList
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar

@Composable
fun PlacesNearbyScreenVM(
    homeNavController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PlacesNearbyViewModel =  hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlacesNearbyScreen(
        homeNavController,
        uiState,
        modifier = modifier
    )
}

@Composable
fun PlacesNearbyScreen(
    homeNavController: NavController,
    uiState: PlacesNearbyUiState,
    modifier: Modifier = Modifier,
) {

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CustomAppBar(
                    title = stringResource(R.string.search_nearby_screen_title),
                    navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                    onNavigateUp = {
                        homeNavController.navigateUp()
                    },
                )
            }
        },
    ) { innerPadding ->
        // Display the list of routes
        PlacesNearbyList(
            uiState = uiState,
            navController = homeNavController,
            modifier = modifier.padding(innerPadding)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PlacesNearbyPreview() {
    PlacesNearbyScreen(
        homeNavController = rememberNavController(),
        uiState =
            PlacesNearbyUiState(
                locations = previewLocationList
            ),
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPreview() {
    PlacesNearbyScreen(
        homeNavController = rememberNavController(),
        uiState = PlacesNearbyUiState(),
    )
}
