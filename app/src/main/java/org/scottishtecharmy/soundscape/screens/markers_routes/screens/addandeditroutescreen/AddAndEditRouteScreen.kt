package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
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
    var waypoints: MutableList<SimpleMarkerData> = emptyList<SimpleMarkerData>().toMutableList()
)

fun generateRouteDetailsRoute(routeData: RouteData): String {

    // Generate JSON for the RouteData and append it to the route
    val simpleRouteData = SimpleRouteData()
    simpleRouteData.name = routeData.name
    simpleRouteData.description = routeData.description
    for (waypoint in routeData.waypoints) {
        simpleRouteData.waypoints.add(
            SimpleMarkerData(
                waypoint.addressName,
                waypoint.location!!.location()
            )
        )
    }

    val json = GsonBuilder().create().toJson(simpleRouteData)
    val urlEncodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
    return "${HomeRoutes.AddAndEditRoute.route}?command=import&data=$urlEncodedJson"
}

fun parseSimpleRouteData(jsonData: String): RouteData {

    // Parse JSON
    val gson = GsonBuilder().create()
    val simpleRouteData = gson.fromJson(jsonData, SimpleRouteData::class.java)

    val routeData = RouteData(
        name = simpleRouteData.name,
        description = simpleRouteData.description,
    )
    for (waypoint in simpleRouteData.waypoints) {
        routeData.waypoints.add(
            MarkerData(
                waypoint.addressName,
                Location(waypoint.location)
            )
        )
    }
    return routeData
}

@Composable
fun AddAndEditRouteScreenVM(
    routeObjectId: ObjectId?,
    navController: NavController,
    modifier: Modifier,
    userLocation: LngLatAlt?,
    editRoute: Boolean,
    viewModel: AddAndEditRouteViewModel = hiltViewModel()
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
        onEditComplete = { viewModel.editComplete() },
        onClickFolder = { folder, title ->
            viewModel.onClickFolder(folder, title)
        },
        onClickBack = { viewModel.onClickBack() },
        userLocation = userLocation,
        onSelectLocation = { location -> viewModel.onSelectLocation(location) },
        createAndAddMarker = { location -> viewModel.createAndAddMarker(location) }
    )
}


@Composable
fun AddAndEditRouteScreen(
    routeObjectId: ObjectId?,
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
    onDeleteRoute: (objectId: ObjectId) -> Unit,
    onEditComplete: () -> Unit,
    onClickFolder: (String, String) -> Unit,
    onClickBack: () -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    createAndAddMarker: (LocationDescription) -> Unit
) {
    val context = LocalContext.current
    var addWaypointDialog by remember { mutableStateOf(false) }
    var routeMembers by remember(uiState.routeMembers) {
        mutableStateOf(uiState.routeMembers.toList())
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
                    navController.popBackStack(HomeRoutes.MarkersAndRoutes.route, false)
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
                addWaypointDialog = false
            },
            onClickFolder = onClickFolder,
            onClickBack = {
                if(placesNearbyUiState.level == 0)
                    addWaypointDialog = false
                else
                    onClickBack()
            },
            onSelectLocation = onSelectLocation,
            createAndAddMarker = createAndAddMarker,
            modifier = modifier,
            userLocation = location
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
                        uiState.routeMembers = routeMembers
                        onEditComplete()
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
                        onClick = { addWaypointDialog = true },
                        buttonColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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
                        Text(
                            modifier = Modifier.padding(top = spacing.small, bottom = spacing.extraSmall),
                            text = stringResource(R.string.markers_sort_button_sort_by_name),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        CustomTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .extraSmallPadding(),
                            value = uiState.name,
                            onValueChange = onNameChange
                        )
                        Text(
                            modifier = Modifier.padding(top = spacing.medium, bottom = spacing.extraSmall),
                            text = stringResource(R.string.route_detail_edit_description),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        CustomTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .extraSmallPadding(),
                            value = uiState.description,
                            onValueChange = onDescriptionChange
                        )

                        HorizontalDivider(
                            thickness = spacing.tiny,
                            modifier = Modifier
                                .fillMaxWidth()
                                .smallPadding(),
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
                                itemsIndexed(routeMembers, key = { _,item -> item.databaseId!!.toString() }) { index, item ->
                                    ReorderableItem(reorderableLazyListState, item.databaseId.toString()) { _ ->
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
        routeObjectId = ObjectId(),
        navController = rememberNavController(),
        modifier = Modifier,
        uiState = AddAndEditRouteUiState(),
        placesNearbyUiState = PlacesNearbyUiState(),
        editRoute = false,
        onClearErrorMessage = {},
        onResetDoneAction = {},
        onNameChange = {},
        onDescriptionChange = {},
        onDeleteRoute = {},
        onEditComplete = {},
        onClickFolder = {_,_ ->},
        onClickBack = {},
        onSelectLocation = {_ ->},
        createAndAddMarker = {_ ->},
        userLocation = LngLatAlt()
    )
}

@Preview(showBackground = true)
@Composable
fun EditRouteScreenPreview() {
    AddAndEditRouteScreen(
        routeObjectId = ObjectId(),
        navController = rememberNavController(),
        modifier = Modifier,
        uiState = AddAndEditRouteUiState(
            routeMembers = previewLocationList
        ),
        placesNearbyUiState = PlacesNearbyUiState(),
        editRoute = true,
        onClearErrorMessage = {},
        onResetDoneAction = {},
        onNameChange = {},
        onDescriptionChange = {},
        onDeleteRoute = {},
        onEditComplete = {},
        onClickFolder = {_,_ ->},
        onClickBack = {},
        onSelectLocation = {_ ->},
        createAndAddMarker = {_ ->},
        userLocation = LngLatAlt()
    )
}