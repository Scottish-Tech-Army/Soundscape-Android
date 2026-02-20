package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.gson.GsonBuilder
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationList
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class SimpleMarkerData(
    var addressName: String = "",
    var location: LngLatAlt = LngLatAlt()
)
private data class SimpleRouteData(
    var name: String = "",
    var description: String = "",
    var waypoints: MutableList<SimpleMarkerData> = mutableListOf()
)

fun generateRouteDetailsRoute(routeData: RouteWithMarkers): String {

    // Generate JSON for the RouteData and append it to the route
    val simpleRouteData = SimpleRouteData()
    simpleRouteData.name = routeData.route.name
    simpleRouteData.description = routeData.route.description
    for (waypoint in routeData.markers) {
        simpleRouteData.waypoints.add(
            SimpleMarkerData(
                waypoint.name,
                LngLatAlt(waypoint.longitude, waypoint.latitude)
            )
        )
    }

    val json = GsonBuilder().create().toJson(simpleRouteData)
    val urlEncodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
    return "${HomeRoutes.AddAndEditRoute.route}?command=import&data=$urlEncodedJson"
}

fun parseSimpleRouteData(jsonData: String): RouteWithMarkers {

    // Parse JSON
    val gson = GsonBuilder().create()
    val simpleRouteData = gson.fromJson(jsonData, SimpleRouteData::class.java)

    val markers = mutableListOf<MarkerEntity>()
    for (waypoint in simpleRouteData.waypoints) {
        markers.add(
            MarkerEntity(
                name =waypoint.addressName,
                longitude = waypoint.location.longitude,
                latitude = waypoint.location.latitude,
            )
        )
    }
    val routeData = RouteWithMarkers(
        RouteEntity(
            name = simpleRouteData.name,
            description = simpleRouteData.description,
        ),
        markers
    )
    return routeData
}

@Composable
fun AddAndEditRouteScreenVM(
    routeObjectId: Long?,
    navController: NavController,
    modifier: Modifier,
    userLocation: LngLatAlt?,
    editRoute: Boolean,
    getCurrentLocationDescription: () -> LocationDescription,
    viewModel: AddAndEditRouteViewModel = hiltViewModel(),
    heading: Float
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val placesNearbyUiState by viewModel.logic.uiState.collectAsStateWithLifecycle()
    AddAndEditRouteScreen(
        routeObjectId = routeObjectId,
        navController = navController,
        modifier = modifier,
        uiState = uiState,
        placesNearbyUiState = placesNearbyUiState,
        editRoute = editRoute,
        onClearErrorMessage = { viewModel.clearErrorMessage() },
        onResetDoneAction = { viewModel.resetDoneActionState() },
        onNameChange = { viewModel.onNameChange(it) },
        onDescriptionChange = { viewModel.onDescriptionChange(it) },
        onDeleteRoute = { viewModel.deleteRoute(it) },
        onEditComplete = { viewModel.editComplete(it) },
        onClickFolder = { folder, title ->
            viewModel.onClickFolder(folder, title)
        },
        onClickBack = { viewModel.onClickBack() },
        userLocation = userLocation,
        heading = heading,
        onSelectLocation = { location -> viewModel.onSelectLocation(location) },
        onToggleMember = { location -> viewModel.toggleMember(location) },
        createAndAddMarker = { location, successMessage, failureMessage, duplicateMessage ->
            viewModel.createAndAddMarker(
                location,
                successMessage,
                failureMessage,
                duplicateMessage)
        },
        getCurrentLocationDescription = getCurrentLocationDescription
    )
}


@Composable
fun AddAndEditRouteScreen(
    routeObjectId: Long?,
    navController: NavController,
    modifier: Modifier,
    uiState: AddAndEditRouteUiState,
    placesNearbyUiState: PlacesNearbyUiState,
    editRoute: Boolean,
    userLocation: LngLatAlt?,
    onClearErrorMessage: () -> Unit,
    onResetDoneAction: () -> Unit,
    onNameChange: (newText: String) -> Unit,
    onDescriptionChange: (newText: String) -> Unit,
    onDeleteRoute: (objectId: Long) -> Unit,
    onEditComplete: (List<LocationDescription>) -> Unit,
    onClickFolder: (String, String) -> Unit,
    onClickBack: () -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    onToggleMember: (LocationDescription) -> Unit,
    createAndAddMarker: (LocationDescription, String, String, String) -> Unit,
    getCurrentLocationDescription: () -> LocationDescription,
    heading: Float,
) {
    val context = LocalContext.current
    var addWaypointDialog by remember { mutableStateOf(false) }
    var routeMembers by remember(uiState.routeMembers) {
        val members = uiState.routeMembers.toList()
        for((index, marker) in members.withIndex()) {
            marker.orderId = index.toLong()
        }
        mutableStateOf(members)
    }

    val lazyListState = rememberLazyListState()
    val location by remember(userLocation) {mutableStateOf(userLocation)}
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Update the list when an item is dragging
        routeMembers = routeMembers.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }


    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onClearErrorMessage()
        }
    }

    // Observe navigation and trigger it if necessary
    LaunchedEffect(uiState.doneActionCompleted) {
        if (uiState.doneActionCompleted) {
            val actionType = uiState.actionType
            when (actionType) {
                ActionType.UPDATE -> {
                    navController.popBackStack()
                }
                ActionType.DELETE -> {
                    // The route has been deleted, so navigate directly to the routes tab
                    navController.navigate(HomeRoutes.MarkersAndRoutes.route + "?tab=routes",)
                }
                else -> {
                    assert(false)
                }
            }
            onResetDoneAction()
            val message = when (actionType) {
                ActionType.UPDATE -> context.getString(R.string.route_update_success_title)
                ActionType.DELETE -> context.getString(R.string.routes_action_deleted)
                else -> ""
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if(addWaypointDialog) {
        AddWaypointsDialog(
            uiState = uiState,
            placesNearbyUiState = placesNearbyUiState,
            onAddWaypointComplete = {
                // Create the final list of markers within the route

                // Determine which ids to keep (from uiState, not toggled out)
                val keepIds = uiState.routeMembers
                    .filter { marker -> !uiState.toggledMembers.any { it.databaseId == marker.databaseId } }
                    .map { it.databaseId }
                    .toSet()

                // Filter routeMembers (preserving user's reordering) to keep only those ids in order
                val routeMemberIds = routeMembers.map { it.databaseId }.toSet()
                val members = routeMembers
                    .filter { it.databaseId in keepIds }
                    .toMutableList()

                // Add entries that are in keepIds but weren't in routeMembers (added while dialog open)
                val missingFromReorder = uiState.routeMembers
                    .filter { it.databaseId in keepIds && it.databaseId !in routeMemberIds }
                members.addAll(missingFromReorder)

                // Add toggled members that weren't already in the route
                for(marker in uiState.toggledMembers) {
                    if(!uiState.routeMembers.any { it.databaseId == marker.databaseId }) {
                        members.add(marker)
                    }
                }
                // Reset orderId values
                for((index, marker) in members.withIndex()) {
                    marker.orderId = index.toLong()
                }

                routeMembers = members
                addWaypointDialog = false
            },
            onClickFolder = onClickFolder,
            onClickBack = {
                if(placesNearbyUiState.level == 0) {
                    addWaypointDialog = false
                }
                else
                    onClickBack()
            },
            onSelectLocation = onSelectLocation,
            onToggleMember = onToggleMember,
            createAndAddMarker = createAndAddMarker,
            modifier = modifier,
            userLocation = location,
            heading = heading,
            getCurrentLocationDescription = getCurrentLocationDescription
        )
    }
    else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TextOnlyAppBar(
                    title = stringResource(
                        if(editRoute) (R.string.route_detail_action_edit)
                        else  (R.string.route_detail_action_create)
                    ),
                    onNavigateUp = { navController.popBackStack() },
                    navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                    onRightButton = {
                        // Update the route
                        onEditComplete(routeMembers)
                    },
                    rightButtonTitle = stringResource(R.string.general_alert_done)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                ) {
                    if(editRoute) {
                        CustomButton(
                            onClick = {
                                onDeleteRoute(routeObjectId!!)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding(),
                            buttonColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(spacing.small),
                            text = stringResource(R.string.route_detail_edit_delete),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    CustomButton(
                        Modifier
                            .fillMaxWidth()
                            .smallPadding(),
                        onClick = {
                            addWaypointDialog = true
                        },
                        shape = RoundedCornerShape(spacing.small),
                        text = stringResource(R.string.route_detail_edit_waypoints_button),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .extraSmallPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .extraSmallPadding()
                    ) {
                        CustomTextField(
                            fieldName = stringResource(R.string.markers_sort_button_sort_by_name),
                            fieldHint = stringResource(R.string.route_name_description_hint),
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.name,
                            onValueChange = onNameChange
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        CustomTextField(
                            fieldName = stringResource(R.string.route_detail_edit_description),
                            fieldHint = stringResource(R.string.route_description_description_hint),
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.description,
                            onValueChange = onDescriptionChange
                        )

                        HorizontalDivider(
                            thickness = spacing.tiny,
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding(),
                        )
                        // Display the list of markers in the route
                        if(routeMembers.isEmpty()) {
                            Text(
                                stringResource(R.string.route_detail_action_start_route_disabled_hint),
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
                                itemsIndexed(routeMembers, key = { _,item -> item.orderId.toString() }) { index, item ->
                                    ReorderableItem(reorderableLazyListState, item.orderId.toString()) { _ ->
                                        Row(modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            LocationItem(
                                                item = item,
                                                modifier = Modifier.weight(1f),
                                                decoration = LocationItemDecoration(
                                                    index = index,
                                                    indexDescription = stringResource(R.string.waypoint_title),
                                                    reorderable = true,
                                                    moveDown = { i ->
                                                        if (i < routeMembers.size - 1) {
                                                            routeMembers = routeMembers.toMutableList().apply {
                                                                add(i + 1, removeAt(i))
                                                            }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                    moveUp = { i ->
                                                        if (i > 0) {
                                                            routeMembers = routeMembers.toMutableList().apply {
                                                                add(i - 1, removeAt(i))
                                                            }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }

                                                ),
                                                userLocation = location
                                            )

                                            IconButton(
                                                modifier = Modifier
                                                    .draggableHandle()
                                                    .width(spacing.targetSize)
                                                    .align(Alignment.CenterVertically),
                                                onClick = {}
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
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NewRouteScreenPreview() {
    AddAndEditRouteScreen(
        routeObjectId = 0L,
        navController = rememberNavController(),
        modifier = Modifier,
        uiState = AddAndEditRouteUiState(),
        placesNearbyUiState = PlacesNearbyUiState(),
        editRoute = false,
        userLocation = LngLatAlt(),
        heading = 45.0F,
        onClearErrorMessage = {},
        onResetDoneAction = {},
        onNameChange = {},
        onDescriptionChange = {},
        onDeleteRoute = {},
        onEditComplete = {_ -> },
        onClickFolder = {_,_ ->},
        onClickBack = {},
        onSelectLocation = {_ ->},
        onToggleMember = {_ ->},
        createAndAddMarker = {_,_,_,_ ->},
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}

@Preview(showBackground = true)
@Composable
fun EditRouteScreenPreview() {
    AddAndEditRouteScreen(
        routeObjectId = 0L,
        navController = rememberNavController(),
        modifier = Modifier,
        uiState = AddAndEditRouteUiState(
            routeMembers = previewLocationList
        ),
        placesNearbyUiState = PlacesNearbyUiState(),
        editRoute = true,
        userLocation = LngLatAlt(),
        heading = 45.0F,
        onClearErrorMessage = {},
        onResetDoneAction = {},
        onNameChange = {},
        onDescriptionChange = {},
        onDeleteRoute = {},
        onEditComplete = {_ -> },
        onClickFolder = {_,_ ->},
        onClickBack = {},
        onSelectLocation = {_ ->},
        onToggleMember = {_ ->},
        createAndAddMarker = {_,_,_,_ ->},
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}