package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesTabsVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreenVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreenVM
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel

@Composable
fun MarkersAndRoutesScreen(
    navController: NavController,
    viewModel : HomeViewModel,
    modifier: Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                MarkersAndRoutesAppBar(
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                )
                MarkersAndRoutesTabsVM(viewModel)
            }},
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if(state.routesTabSelected) {
                RoutesScreenVM(navController)
            } else {
                MarkersScreenVM(navController)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesPreview() {
    SoundscapeTheme {
        MarkersAndRoutesScreen(
            navController = rememberNavController(),
            viewModel = viewModel(),
            modifier = Modifier
        )
    }
}
