package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesTabsVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreenVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreenVM
import org.scottishtecharmy.soundscape.ui.theme.spacing
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
                FlexibleAppBar(
                    title = stringResource(R.string.search_view_markers),
                    leftSide = {
                        IconWithTextButton(
                            text = stringResource(R.string.ui_back_button_title),
                            color = MaterialTheme.colorScheme.onSurface
                        ) {
                            navController.popBackStack()
                        }
                    },
                    rightSide = {
                        if (state.routesTabSelected) {
                            IconWithTextButton(
                                text = "",
                                icon = Icons.Default.Add,
                                iconModifier = Modifier.size(spacing.targetSize),
                                color = MaterialTheme.colorScheme.onSurface,
                                contentDescription = stringResource(R.string.route_detail_action_create),
                                talkbackHint = stringResource(R.string.route_detail_action_create_hint)
                            ) {
                                navController.navigate("${HomeRoutes.AddAndEditRoute.route}?command=new")
                            }
                        }
                    }
                )
                MarkersAndRoutesTabsVM(viewModel)
            }},
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if(state.routesTabSelected) {
                RoutesScreenVM(navController, userLocation = state.location)
            } else {
                MarkersScreenVM(navController, userLocation = state.location)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesPreview() {
    MarkersAndRoutesScreen(
        navController = rememberNavController(),
        viewModel = viewModel(),
        modifier = Modifier
    )
}
