package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationList
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesListSort
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesList
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.ui.theme.spacing

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
    uiState: MarkersAndRoutesUiState,
    clearErrorMessage: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit,
    userLocation: LngLatAlt?
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        val noMarkersText = stringResource(R.string.markers_no_markers_hint_1)
        val noMarkerHtmlText = remember(uiState.entries.isEmpty()) {
                val parser: Parser = Parser.builder().build()
                val document: Node? =
                    parser.parse(noMarkersText)
                val renderer = HtmlRenderer.builder().build()
                renderer.render(document)
        }
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
                    if (uiState.entries.isEmpty()) {
                        Box(modifier = Modifier.padding(top = spacing.large)) {
                            Icon(
                                painter =
                                painterResource(
                                    id = R.drawable.ic_markers,
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                                contentDescription = null,
                                modifier = Modifier.size(spacing.extraLarge),
                            )
                        }
                        Box(modifier = Modifier.padding(top = spacing.small)) {
                            Text(
                                stringResource(R.string.markers_no_markers_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(modifier = Modifier.padding(top = spacing.small)) {
                            Text(
                                text = AnnotatedString.fromHtml(
                                    htmlString = noMarkerHtmlText,
                                    linkStyles = TextLinkStyles(
                                        style = SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                        )
                                    )
                                ),
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
                                stringResource(R.string.markers_no_markers_hint_2),
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

                        // Display the list of routes
                        MarkersAndRoutesList(
                            uiState = uiState,
                            userLocation = userLocation,
                            modifier = Modifier.weight(1f),
                            onSelect = { desc ->
                                homeNavController.navigate(generateLocationDetailsRoute(desc))
                            }
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
    MarkersScreen(
        homeNavController = rememberNavController(),
        uiState =
            MarkersAndRoutesUiState(
                entries = previewLocationList
            ),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        userLocation = null
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPreview() {
    MarkersScreen(
        homeNavController = rememberNavController(),
        uiState = MarkersAndRoutesUiState(),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        userLocation = null
    )
}
