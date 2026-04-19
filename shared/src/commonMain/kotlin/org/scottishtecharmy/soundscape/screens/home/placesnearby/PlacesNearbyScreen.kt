package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun PlacesNearbyScreen(
    uiState: PlacesNearbyUiState,
    onSelectItem: (LocationDescription) -> Unit,
    modifier: Modifier = Modifier,
    onClickFolder: (String, String) -> Unit = {_,_ ->},
    onClickBack: () -> Unit = {},
    onStartBeacon: (LocationDescription) -> Unit = {},
) {

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CustomAppBar(
                    title =
                        if(uiState.level == 0) stringResource(Res.string.search_nearby_screen_title)
                        else uiState.title,
                    navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
                    onNavigateUp = {
                        onClickBack()
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.extraSmallPadding()) {
            // Display the list of places
            PlacesNearbyList(
                uiState = uiState,
                onSelectItem = onSelectItem,
                onClickFolder = onClickFolder,
                onStartBeacon = onStartBeacon,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}
