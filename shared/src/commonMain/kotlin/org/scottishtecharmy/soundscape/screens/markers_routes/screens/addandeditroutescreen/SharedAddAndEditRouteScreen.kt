package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.general_alert_done
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_name
import org.scottishtecharmy.soundscape.resources.route_description_description_hint
import org.scottishtecharmy.soundscape.resources.route_detail_action_create
import org.scottishtecharmy.soundscape.resources.route_detail_action_edit
import org.scottishtecharmy.soundscape.resources.route_detail_action_start_route_disabled_hint
import org.scottishtecharmy.soundscape.resources.route_detail_edit_delete
import org.scottishtecharmy.soundscape.resources.route_detail_edit_delete_alert_message
import org.scottishtecharmy.soundscape.resources.route_detail_edit_description
import org.scottishtecharmy.soundscape.resources.route_detail_edit_waypoints_button
import org.scottishtecharmy.soundscape.resources.route_name_description_hint
import org.scottishtecharmy.soundscape.resources.route_update_success_title
import org.scottishtecharmy.soundscape.resources.routes_action_deleted
import org.scottishtecharmy.soundscape.resources.settings_reset_dialog_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.resources.waypoint_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SharedAddAndEditRouteScreen(
    holder: AddAndEditRouteViewModel,
    isEditing: Boolean,
    userLocation: LngLatAlt?,
    heading: Float,
    getCurrentLocationDescription: () -> LocationDescription,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    onDeleteComplete: () -> Unit,
    onShowError: (String) -> Unit = {},
    onShowSuccess: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by holder.uiState.collectAsState()
    val placesNearbyUiState by holder.logic.uiState.collectAsState()

    var addWaypointDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var routeMembers by remember(uiState.routeMembers) {
        val members = uiState.routeMembers.toList()
        for ((index, marker) in members.withIndex()) {
            marker.orderId = index.toLong()
        }
        mutableStateOf(members)
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        routeMembers = routeMembers.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            onShowError(message)
            holder.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.doneActionCompleted) {
        if (uiState.doneActionCompleted) {
            val actionType = uiState.actionType
            holder.resetDoneActionState()
            when (actionType) {
                ActionType.UPDATE -> {
                    onShowSuccess(getString(Res.string.route_update_success_title))
                    onSaveComplete()
                }

                ActionType.DELETE -> {
                    onShowSuccess(getString(Res.string.routes_action_deleted))
                    onDeleteComplete()
                }

                else -> Unit
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(Res.string.route_detail_edit_delete_alert_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        uiState.routeObjectId?.let { holder.deleteRoute(it) }
                    },
                    modifier = Modifier.testTag("addAndEditRouteDeleteConfirm"),
                ) {
                    Text(stringResource(Res.string.ui_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("addAndEditRouteDeleteCancel"),
                ) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }

    if (addWaypointDialog) {
        AddWaypointsDialog(
            uiState = uiState,
            placesNearbyUiState = placesNearbyUiState,
            modifier = modifier,
            onAddWaypointComplete = {
                val keepIds = uiState.routeMembers
                    .filter { marker -> !uiState.toggledMembers.any { it.databaseId == marker.databaseId } }
                    .map { it.databaseId }
                    .toSet()

                val routeMemberIds = routeMembers.map { it.databaseId }.toSet()
                val members = routeMembers
                    .filter { it.databaseId in keepIds }
                    .toMutableList()

                val missingFromReorder = uiState.routeMembers
                    .filter { it.databaseId in keepIds && it.databaseId !in routeMemberIds }
                members.addAll(missingFromReorder)

                for (marker in uiState.toggledMembers) {
                    if (!uiState.routeMembers.any { it.databaseId == marker.databaseId }) {
                        members.add(marker)
                    }
                }
                for ((index, marker) in members.withIndex()) {
                    marker.orderId = index.toLong()
                }

                routeMembers = members
                addWaypointDialog = false
            },
            onClickFolder = { filter, title -> holder.onClickFolder(filter, title) },
            onClickBack = {
                if (placesNearbyUiState.level == 0) {
                    addWaypointDialog = false
                } else {
                    holder.onClickBack()
                }
            },
            onSelectLocation = { holder.onSelectLocation(it) },
            onToggleMember = { holder.toggleMember(it) },
            createAndAddMarker = { desc, success, failure, duplicate ->
                holder.createAndAddMarker(desc, success, failure, duplicate)
            },
            userLocation = userLocation,
            heading = heading,
            getCurrentLocationDescription = getCurrentLocationDescription,
        )
    } else {
        Scaffold(
            modifier = modifier.imePadding(),
            topBar = {
                TextOnlyAppBar(
                    title = stringResource(
                        if (isEditing) Res.string.route_detail_action_edit
                        else Res.string.route_detail_action_create
                    ),
                    onNavigateUp = onNavigateUp,
                    navigationButtonTitle = stringResource(Res.string.general_alert_cancel),
                    onRightButton = { holder.editComplete(routeMembers) },
                    rightButtonTitle = stringResource(Res.string.general_alert_done),
                )
            },
            bottomBar = {
                Column {
                    if (isEditing) {
                        CustomButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding()
                                .testTag("addAndEditRouteDeleteButton"),
                            buttonColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(spacing.small),
                            text = stringResource(Res.string.route_detail_edit_delete),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    CustomButton(
                        Modifier
                            .fillMaxWidth()
                            .smallPadding()
                            .testTag("addWaypointsButton"),
                        onClick = { addWaypointDialog = true },
                        shape = RoundedCornerShape(spacing.small),
                        text = stringResource(Res.string.route_detail_edit_waypoints_button),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .extraSmallPadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .extraSmallPadding(),
                    ) {
                        CustomTextField(
                            fieldName = stringResource(Res.string.markers_sort_button_sort_by_name),
                            fieldHint = stringResource(Res.string.route_name_description_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("routeName"),
                            value = uiState.name,
                            onValueChange = { holder.onNameChange(it) },
                            testTagPreFix = "name",
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        CustomTextField(
                            fieldName = stringResource(Res.string.route_detail_edit_description),
                            fieldHint = stringResource(Res.string.route_description_description_hint),
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.description,
                            onValueChange = { holder.onDescriptionChange(it) },
                            testTagPreFix = "description",
                        )

                        HorizontalDivider(
                            thickness = spacing.tiny,
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding(),
                        )

                        if (routeMembers.isEmpty()) {
                            Text(
                                stringResource(Res.string.route_detail_action_start_route_disabled_hint),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = spacing.large),
                            )
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                            ) {
                                itemsIndexed(
                                    routeMembers,
                                    key = { _, item -> item.orderId.toString() }) { index, item ->
                                    ReorderableItem(
                                        reorderableLazyListState,
                                        item.orderId.toString()
                                    ) { _ ->
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            LocationItem(
                                                item = item,
                                                modifier = Modifier.weight(1f),
                                                decoration = LocationItemDecoration(
                                                    index = index,
                                                    indexDescription = stringResource(Res.string.waypoint_title),
                                                    reorderable = true,
                                                    moveDown = { i ->
                                                        if (i < routeMembers.size - 1) {
                                                            routeMembers =
                                                                routeMembers.toMutableList().apply {
                                                                    add(i + 1, removeAt(i))
                                                                }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                    moveUp = { i ->
                                                        if (i > 0) {
                                                            routeMembers =
                                                                routeMembers.toMutableList().apply {
                                                                    add(i - 1, removeAt(i))
                                                                }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                ),
                                                userLocation = userLocation,
                                            )

                                            IconButton(
                                                modifier = Modifier
                                                    .draggableHandle()
                                                    .width(spacing.targetSize)
                                                    .align(Alignment.CenterVertically)
                                                    .testTag("addAndEditRouteDragHandle"),
                                                onClick = {},
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DragIndicator,
                                                    contentDescription = "",
                                                    modifier = Modifier.size(spacing.targetSize),
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            thickness = spacing.tiny,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

