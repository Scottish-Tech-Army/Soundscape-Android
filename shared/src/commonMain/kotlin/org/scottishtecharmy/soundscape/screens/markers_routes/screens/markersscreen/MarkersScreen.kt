package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

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
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesList
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun MarkersScreen(
    uiState: MarkersAndRoutesUiState,
    clearErrorMessage: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit,
    userLocation: LngLatAlt?,
    onSelectItem: (LocationDescription) -> Unit,
    onShowError: (String) -> Unit = {},
    onStartBeacon: (LngLatAlt, String) -> Unit = { _, _ -> }
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val noMarkersText = stringResource(Res.string.markers_no_markers_hint_1)

        // Display error message if it exists
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let { message ->
                onShowError(message)
                clearErrorMessage()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Display loading state
            if (uiState.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (uiState.entries.isEmpty()) {
                        Box(modifier = Modifier.padding(top = spacing.large)) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_markers),
                                tint = MaterialTheme.colorScheme.onSurface,
                                contentDescription = null,
                                modifier = Modifier.size(spacing.extraLarge),
                            )
                        }
                        Box(modifier = Modifier.padding(top = spacing.small)) {
                            Text(
                                stringResource(Res.string.markers_no_markers_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(modifier = Modifier.padding(top = spacing.small)) {
                            Text(
                                text = noMarkersText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            modifier = Modifier.padding(
                                top = spacing.small,
                                bottom = spacing.large
                            )
                        ) {
                            Text(
                                stringResource(Res.string.markers_no_markers_hint_2),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        MarkersAndRoutesListSort(
                            isSortByName = uiState.isSortByName,
                            isAscending = uiState.isSortAscending,
                            onToggleSortOrder = onToggleSortOrder,
                            onToggleSortByName = onToggleSortByName
                        )

                        // Display the list of markers
                        MarkersAndRoutesList(
                            uiState = uiState,
                            userLocation = userLocation,
                            modifier = Modifier.weight(1f),
                            onSelect = onSelectItem,
                            onStartBeacon = { desc ->
                                onStartBeacon(desc.location, desc.name)
                            }
                        )
                    }
                }
            }
        }
    }
}
