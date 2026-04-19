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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesListSort
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesList
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun RoutesScreen(
    uiState: MarkersAndRoutesUiState,
    userLocation: LngLatAlt?,
    clearErrorMessage: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit,
    onSelectItem: (LocationDescription) -> Unit,
    onShowError: (String) -> Unit = {},
    onStartPlayback: (Long) -> Unit = {}
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
                        onToggleSortOrder = onToggleSortOrder,
                        onToggleSortByName = onToggleSortByName
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
