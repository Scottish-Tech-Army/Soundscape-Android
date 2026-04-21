package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedAddAndEditRouteScreen(
    isEditing: Boolean = false,
    routeName: String = "",
    routeDescription: String = "",
    waypoints: List<LocationDescription> = emptyList(),
    availableMarkers: List<LocationDescription> = emptyList(),
    onNavigateUp: () -> Unit,
    onSave: (name: String, description: String, waypoints: List<LocationDescription>) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(routeName) }
    var description by remember { mutableStateOf(routeDescription) }
    var currentWaypoints by remember { mutableStateOf(waypoints) }
    var showAddWaypoints by remember { mutableStateOf(false) }

    if (showAddWaypoints) {
        AddWaypointSelectionScreen(
            currentWaypoints = currentWaypoints,
            availableMarkers = availableMarkers,
            onBack = { showAddWaypoints = false },
            onSelectWaypoint = { desc ->
                currentWaypoints = currentWaypoints + desc
                showAddWaypoints = false
            },
        )
    } else {
        Scaffold(
            topBar = {
                CustomAppBar(
                    title = if (isEditing) stringResource(Res.string.route_detail_action_edit)
                            else stringResource(Res.string.route_detail_action_create),
                    onNavigateUp = onNavigateUp,
                    rightButtonTitle = stringResource(Res.string.general_alert_done),
                    onRightButton = {
                        if (name.isNotBlank()) {
                            onSave(name, description, currentWaypoints)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = spacing.medium),
            ) {
                // Route name
                CustomTextField(
                    fieldName = stringResource(Res.string.markers_sort_button_sort_by_name),
                    fieldHint = stringResource(Res.string.route_name_description_hint),
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(spacing.small))

                // Route description
                CustomTextField(
                    fieldName = stringResource(Res.string.route_detail_edit_description),
                    fieldHint = stringResource(Res.string.route_detail_edit_description),
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Waypoints header with add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.route_detail_edit_waypoints_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showAddWaypoints = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.route_detail_edit_waypoints_button))
                    }
                }

                // Waypoints list
                if (currentWaypoints.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.route_detail_action_start_route_disabled_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = spacing.small),
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(currentWaypoints) { index, waypoint ->
                            LocationItem(
                                item = waypoint,
                                decoration = LocationItemDecoration(
                                    index = index,
                                    editRoute = EnabledFunction(
                                        enabled = true,
                                        value = true,
                                        functionBoolean = {
                                            currentWaypoints = currentWaypoints.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                        hintWhenOn = stringResource(Res.string.route_detail_edit_delete),
                                        hintWhenOff = "",
                                    ),
                                ),
                                userLocation = null,
                            )
                        }
                    }
                }

                // Delete route button (only when editing)
                if (isEditing && onDelete != null) {
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(spacing.small),
                    ) {
                        Text(stringResource(Res.string.route_detail_edit_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWaypointSelectionScreen(
    currentWaypoints: List<LocationDescription>,
    availableMarkers: List<LocationDescription>,
    onBack: () -> Unit,
    onSelectWaypoint: (LocationDescription) -> Unit,
) {
    val currentIds = currentWaypoints.map { it.databaseId }.toSet()
    val available = availableMarkers.filter { it.databaseId !in currentIds }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.route_detail_edit_waypoints_button),
                onNavigateUp = onBack,
            )
        },
    ) { padding ->
        if (available.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.markers_no_markers_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = stringResource(Res.string.markers_no_markers_hint_2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                itemsIndexed(available) { index, marker ->
                    if (index == 0) {
                        HorizontalDivider(thickness = spacing.tiny)
                    }
                    LocationItem(
                        item = marker,
                        decoration = LocationItemDecoration(
                            location = true,
                            details = EnabledFunction(
                                enabled = true,
                                functionLocation = onSelectWaypoint,
                            ),
                        ),
                        userLocation = null,
                    )
                }
            }
        }
    }
}
