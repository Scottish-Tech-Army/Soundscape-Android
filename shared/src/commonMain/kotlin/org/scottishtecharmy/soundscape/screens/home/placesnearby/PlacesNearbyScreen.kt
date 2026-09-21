package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.search_nearby_screen_title
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlacesNearbyScreen(
    uiState: PlacesNearbyUiState,
    onSelectItem: (LocationDescription) -> Unit,
    modifier: Modifier = Modifier,
    onClickFolder: (String, String) -> Unit = { _, _ -> },
    onClickBack: () -> Unit = {},
    onStartBeacon: (LocationDescription) -> Unit = {},
) {

    // System back (button, gesture, or predictive-back swipe) must drill up a folder level
    // the same way the AppBar back button does, rather than falling through to the
    // NavController's default pop, which would skip levels and land back on Home.
    //
    // Deprecated in CMP 1.11 in favor of androidx.navigationevent's NavigationEventHandler,
    // but that reads LocalNavigationEventDispatcherOwner, which CMP only wires up via the
    // internal compat local this BackHandler uses - migrating would break back handling on
    // iOS. Revisit once CMP exposes the public local (JetBrains/compose-multiplatform).
    @Suppress("DEPRECATION")
    BackHandler(enabled = true) {
        onClickBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CustomAppBar(
                    title =
                        if (uiState.level == 0) stringResource(Res.string.search_nearby_screen_title)
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
