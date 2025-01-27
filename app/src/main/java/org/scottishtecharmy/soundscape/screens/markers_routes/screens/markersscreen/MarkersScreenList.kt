package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes

@Composable
fun MarkersList(
    uiState: MarkersUiState,
    navController: NavController,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp)
    ) {
        items(uiState.markers) { marker ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = marker.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
//                    Text(
//                        text = marker.description,
//                        style = MaterialTheme.typography.bodyLarge,
//                        fontWeight = FontWeight.Bold,
//                    )
                }
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clickable {
                            val ld =
                                LocationDescription(
                                    addressName = marker.name,
                                    latitude = marker.location!!.latitude,
                                    longitude = marker.location!!.longitude
                                )
                            // This effectively replaces the current screen with the new one
                            navController.navigate(generateLocationDetailsRoute(ld)) {
                                popUpTo(ScreensForMarkersAndRoutes.Routes.route) {
                                    inclusive = false  // Ensures Route screen is not popped from the stack
                                }
                                launchSingleTop = true  // Prevents multiple instances of Home
                            }
                        },
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = ""
                )
            }
        }
    }
}