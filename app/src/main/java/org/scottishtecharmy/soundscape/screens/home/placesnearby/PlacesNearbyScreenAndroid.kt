package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute

@Composable
fun PlacesNearbyScreenVM(
    homeNavController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PlacesNearbyViewModel =  koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlacesNearbyScreen(
        uiState,
        onSelectItem = { desc ->
            homeNavController.navigate(generateLocationDetailsRoute(desc))
        },
        onClickFolder = { folder, title ->
            viewModel.onClickFolder(folder, title)
        },
        onClickBack = {
            if(uiState.level == 0)
                homeNavController.navigateUp()
            else
                viewModel.onClickBack()
        },
        onStartBeacon = { desc ->
            viewModel.startBeacon(desc.location, desc.name)
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PlacesNearbyPreview() {
    PlacesNearbyScreen(
        uiState =
            PlacesNearbyUiState(
                nearbyPlaces = FeatureCollection()
            ),
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPreview() {
    PlacesNearbyScreen(
        uiState = PlacesNearbyUiState(),
        onSelectItem = {},
    )
}
