package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationListShort
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
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
    AddAndEditRouteScreen(
        routeObjectId,
        navController,
        modifier,
        uiState,
        editRoute,
        onClearErrorMessage = { viewModel.clearErrorMessage() },
        onResetDoneAction = { viewModel.resetDoneActionState() },
        onNameChange = { viewModel.onNameChange(it) },
        onDescriptionChange = { viewModel.onDescriptionChange(it) },
        onDeleteRoute = { viewModel.deleteRoute(it) },
        onDoneClicked = { viewModel.onDoneClicked() },
        userLocation = userLocation
    )
}


@Composable
fun AddAndEditRouteScreen(
    routeObjectId: ObjectId?,
    navController: NavController,
    modifier: Modifier,
    uiState: AddAndEditRouteUiState,
    editRoute: Boolean,
    userLocation: LngLatAlt?,
    onClearErrorMessage: () -> Unit,
    onResetDoneAction: () -> Unit,
    onNameChange: (newText: String) -> Unit,
    onDescriptionChange: (newText: String) -> Unit,
    onDeleteRoute: (objectId: ObjectId) -> Unit,
    onDoneClicked: () -> Unit,
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
            uiState,
            onDone = {
                addWaypointDialog = false
            },
            onCancel = { addWaypointDialog = false },
            modifier = modifier,
            userLocation = location
        )
    }
    else {
        Scaffold(
            modifier = modifier,
            topBar = {
                CustomAppBar(
                    title = stringResource(
                        if(editRoute) (R.string.route_detail_action_edit)
                        else  (R.string.route_detail_action_create)
                    ),
                    navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                    onNavigateUp = { navController.popBackStack() },
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
                                .padding(16.dp),
                            buttonColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(10.dp),
                            text = stringResource(R.string.route_detail_edit_delete),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    CustomButton(
                        Modifier
                            .fillMaxWidth(),
                        onClick = { addWaypointDialog = true },
                        buttonColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(10.dp),
                        text = stringResource(R.string.route_detail_edit_waypoints_button),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    CustomButton(
                        Modifier
                            .fillMaxWidth(),
                        onClick = {
                            uiState.routeMembers = routeMembers
                            onDoneClicked()
                        },
                        buttonColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(10.dp),
                        text = stringResource(R.string.general_alert_done),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(4.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
                            text = stringResource(R.string.markers_sort_button_sort_by_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.surfaceBright
                        )
                        CustomTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.name,
                            onValueChange = onNameChange
                        )
                        Text(
                            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
                            text = stringResource(R.string.route_detail_edit_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.surfaceBright
                        )
                        CustomTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp),
                            value = uiState.description,
                            onValueChange = onDescriptionChange
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            thickness = 2.dp
                        )
                        // Display the list of markers in the route
                        if(routeMembers.isEmpty()) {
                            Text(
                                stringResource(R.string.route_detail_action_start_route_disabled_hint),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 15.dp),
                            )
                        } else {
                            LazyColumn(state = lazyListState) {
                                itemsIndexed(routeMembers, key = { _,item -> item.markerObjectId!!.toString() }) { index, item ->
                                    ReorderableItem(reorderableLazyListState, item.markerObjectId.toString()) { _ ->
                                        Row(modifier = Modifier
                                            .padding(4.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            LocationItem(
                                                item = item,
                                                modifier = Modifier.weight(1f),
                                                decoration = LocationItemDecoration(
                                                    index = index
                                                ),
                                                userLocation = location
                                            )

                                            IconButton(
                                                modifier = Modifier
                                                    .draggableHandle()
                                                    .width(30.dp)
                                                    .align(Alignment.CenterVertically),
                                                onClick = {}
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DragIndicator,
                                                    contentDescription = "",
                                                    tint = Color.White,
                                                )
                                            }
                                        }
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
    SoundscapeTheme {
        AddAndEditRouteScreen(
            routeObjectId = ObjectId(),
            navController = rememberNavController(),
            modifier = Modifier,
            uiState = AddAndEditRouteUiState(),
            editRoute = false,
            onClearErrorMessage = {},
            onResetDoneAction = {},
            onNameChange = {},
            onDescriptionChange = {},
            onDeleteRoute = {},
            onDoneClicked = {},
            userLocation = LngLatAlt()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditRouteScreenPreview() {
    SoundscapeTheme {
        AddAndEditRouteScreen(
            routeObjectId = ObjectId(),
            navController = rememberNavController(),
            modifier = Modifier,
            uiState = AddAndEditRouteUiState(),
            editRoute = true,
            onClearErrorMessage = {},
            onResetDoneAction = {},
            onNameChange = {},
            onDescriptionChange = {},
            onDeleteRoute = {},
            onDoneClicked = {},
            userLocation = LngLatAlt()
        )
    }
}