package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesListSort
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun MarkersScreenVM(
    homeNavController: NavController,
    userLocation: LngLatAlt?,
    viewModel: MarkersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState.userLocation = userLocation
    MarkersScreen(
        homeNavController,
        uiState,
        clearErrorMessage = { viewModel.clearErrorMessage() },
        onToggleSortOrder = { viewModel.toggleSortOrder() },
        onToggleSortByName = { viewModel.toggleSortByName() },
        userLocation = userLocation
    )
}

@Composable
fun MarkersScreen(
    homeNavController: NavController,
    uiState: MarkersUiState,
    clearErrorMessage: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit,
    userLocation: LngLatAlt?
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current

        // Display error message if it exists
        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                    if (uiState.markers.isEmpty()) {
                        Box(modifier = Modifier.padding(top = 40.dp)) {
                            Icon(
                                painter =
                                    painterResource(
                                        id = R.drawable.ic_markers,
                                    ),
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                        Box(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                stringResource(R.string.markers_no_markers_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                stringResource(R.string.markers_no_markers_hint_1),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(modifier = Modifier.padding(top = 10.dp, bottom = 40.dp)) {
                            Text(
                                stringResource(R.string.markers_no_markers_hint_2),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MarkersAndRoutesListSort(
                                isSortByName = uiState.isSortByName,
                                isAscending = uiState.isSortAscending,
                                onToggleSortOrder = onToggleSortOrder,
                                onToggleSortByName = onToggleSortByName
                            )
                        }
                        // Display the list of routes
                        MarkersList(
                            uiState = uiState,
                            navController = homeNavController,
                            userLocation = userLocation
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPopulatedPreview() {
    SoundscapeTheme {
        MarkersScreen(
            homeNavController = rememberNavController(),
            uiState =
                MarkersUiState(
                    markers =
                        listOf(
                            LocationDescription(
                                "Waypoint 1",
                                location = LngLatAlt(),
                                "Street Blabla, Blabla City",
                            ),
                        ),
                ),
            clearErrorMessage = {},
            onToggleSortOrder = {},
            onToggleSortByName = {},
            userLocation = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPreview() {
    SoundscapeTheme {
        MarkersScreen(
            homeNavController = rememberNavController(),
            uiState = MarkersUiState(),
            clearErrorMessage = {},
            onToggleSortOrder = {},
            onToggleSortByName = {},
            userLocation = null
        )
    }
}
