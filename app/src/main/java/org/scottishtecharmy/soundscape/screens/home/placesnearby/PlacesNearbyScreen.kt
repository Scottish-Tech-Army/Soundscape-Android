package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Box
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
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun PlacesNearbyScreenVM(
    homeNavController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PlacesNearbyViewModel =  hiltViewModel(),
) {
    val uiState by viewModel.logic.uiState.collectAsStateWithLifecycle()
    PlacesNearbyScreen(
        homeNavController,
        uiState,
        onClickFolder = { folder, title ->
            viewModel.onClickFolder(folder, title)
        },
        onClickBack = {
            if(uiState.level == 0)
                homeNavController.navigateUp()
            else
                viewModel.onClickBack()
        },
        modifier = modifier
    )
}

@Composable
fun PlacesNearbyScreen(
    homeNavController: NavController,
    uiState: PlacesNearbyUiState,
    modifier: Modifier = Modifier,
    onClickFolder: (String, String) -> Unit = {_,_ -> true},
    onClickBack: () -> Unit = {},
) {

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CustomAppBar(
                    title =
                        if(uiState.level == 0) stringResource(R.string.search_nearby_screen_title)
                        else uiState.title,
                    navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                    onNavigateUp = {
                        onClickBack()
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.extraSmallPadding()) {
            // Display the list of routes
            PlacesNearbyList(
                uiState = uiState,
                navController = homeNavController,
                onClickFolder = onClickFolder,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlacesNearbyPreview() {
    PlacesNearbyScreen(
        homeNavController = rememberNavController(),
        uiState =
            PlacesNearbyUiState(
                nearbyPlaces = FeatureCollection()
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
