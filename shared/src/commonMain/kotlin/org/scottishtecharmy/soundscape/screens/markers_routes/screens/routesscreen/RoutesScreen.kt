package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.Scaffold
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.resources.markers_routes_action_new
import org.scottishtecharmy.soundscape.resources.routes_title
import org.scottishtecharmy.soundscape.resources.route_detail_action_create
import org.scottishtecharmy.soundscape.resources.route_detail_action_create_hint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.ic_routes
import org.scottishtecharmy.soundscape.resources.routes_no_routes_hint_1
import org.scottishtecharmy.soundscape.resources.routes_no_routes_hint_2
import org.scottishtecharmy.soundscape.resources.routes_no_routes_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesListSort
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesList
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun RoutesScreen(
    uiState: MarkersAndRoutesUiState,
    userLocation: LngLatAlt?,
    clearErrorMessage: () -> Unit,
    onCycleSort: () -> Unit,
    onSelectItem: (LocationDescription) -> Unit,
    onShowError: (String) -> Unit = {},
    onStartPlayback: (Long) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onAddRoute: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.testTag("routesScreen"),
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.routes_title),
                onNavigateUp = onNavigateUp,
                rightButtonTitle = stringResource(Res.string.markers_routes_action_new),
                rightButtonDescription = stringResource(Res.string.route_detail_action_create),
                rightButtonHint = stringResource(Res.string.route_detail_action_create_hint),
                onRightButton = onAddRoute,
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            RoutesList(
                uiState = uiState,
                userLocation = userLocation,
                clearErrorMessage = clearErrorMessage,
                onCycleSort = onCycleSort,
                onSelectItem = onSelectItem,
                onShowError = onShowError,
                onStartPlayback = onStartPlayback,
            )
        }
    }
}

@Composable
private fun RoutesList(
    uiState: MarkersAndRoutesUiState,
    userLocation: LngLatAlt?,
    clearErrorMessage: () -> Unit,
    onCycleSort: () -> Unit,
    onSelectItem: (LocationDescription) -> Unit,
    onShowError: (String) -> Unit,
    onStartPlayback: (Long) -> Unit,
) {
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
                onShowError(message)
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
                if (uiState.entries.isEmpty()) {
                    // Display UI when no routes are available
                    Box(modifier = Modifier.padding(top = spacing.large)) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_routes),
                            contentDescription = null,
                            modifier = Modifier.size(spacing.targetSize * 2),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(modifier = Modifier.mediumPadding()) {
                        Text(
                            stringResource(Res.string.routes_no_routes_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(modifier = Modifier.mediumPadding()) {
                        Text(
                            stringResource(Res.string.routes_no_routes_hint_1),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(modifier = Modifier.mediumPadding()) {
                        Text(
                            stringResource(Res.string.routes_no_routes_hint_2),
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
                        onCycleSort = onCycleSort
                    )

                    // Display the list of routes
                    MarkersAndRoutesList(
                        uiState = uiState,
                        userLocation = userLocation,
                        modifier = Modifier.weight(1f),
                        onSelect = onSelectItem,
                        onStartPlayback = { desc ->
                            onStartPlayback(desc.databaseId)
                        }
                    )
                }
            }
        }
    }
}
