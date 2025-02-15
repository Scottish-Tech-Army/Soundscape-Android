package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.home.previewLocationListShort
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun AddAndEditRouteScreenVM(
    routeObjectId: ObjectId?,
    routeName: String,
    navController: NavController,
    modifier: Modifier,
    userLocation: LngLatAlt?,
    viewModel: AddAndEditRouteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddAndEditRouteScreen(
        routeObjectId,
        routeName,
        navController,
        modifier,
        uiState,
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
    routeName: String,
    navController: NavController,
    modifier: Modifier,
    uiState: AddAndEditRouteUiState,
    userLocation: LngLatAlt?,
    onClearErrorMessage: () -> Unit,
    onResetDoneAction: () -> Unit,
    onNameChange: (newText: String) -> Unit,
    onDescriptionChange: (newText: String) -> Unit,
    onDeleteRoute: (objectId: ObjectId) -> Unit,
    onDoneClicked: () -> Unit,
) {
    val context = LocalContext.current
    val addWaypointDialog = remember { mutableStateOf(false) }
    val editExistingRoute = remember { mutableStateOf(routeName != newRouteName) }

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

    if(addWaypointDialog.value) {
        AddWaypointsDialog(
            uiState,
            onDone = {
                addWaypointDialog.value = false
            },
            onCancel = { addWaypointDialog.value = false },
            modifier = modifier,
            userLocation = userLocation
        )
    }
    else {
        Scaffold(
            modifier = modifier,
            topBar = {
                CustomAppBar(
                    title = stringResource(
                        if(editExistingRoute.value) (R.string.route_detail_action_edit)
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
                    if(editExistingRoute.value) {
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
                        onClick = { addWaypointDialog.value = true },
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
                        onClick = onDoneClicked,
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
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
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
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.description,
                            onValueChange = onDescriptionChange
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            thickness = 1.dp
                        )
                        // Display the list of markers in the route
                        if(uiState.routeMembers.isEmpty()) {
                            Text(
                                stringResource(R.string.route_detail_action_start_route_disabled_hint),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 15.dp),
                            )
                        } else {
                            ReorderableLocationList(
                                locations = uiState.routeMembers,
                                userLocation = userLocation
                            )
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
            routeName = newRouteName,
            navController = rememberNavController(),
            modifier = Modifier,
            uiState = AddAndEditRouteUiState(),
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
            routeName = "Route to preview",
            navController = rememberNavController(),
            modifier = Modifier,
            uiState = AddAndEditRouteUiState(
                routeMembers = previewLocationListShort.toMutableList()
            ),
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