package org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun EditRouteScreenVM(
    routeName: String,
    routeDescription: String,
    navController: NavController,
    viewModel: EditRouteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EditRouteScreen(
        routeName,
        routeDescription,
        navController,
        uiState,
        onClearErrorMessage = { viewModel.clearErrorMessage() },
        onResetDoneAction = { viewModel.resetDoneActionState() },
        onNameChange = { viewModel.onNameChange(it) },
        onDescriptionChange = { viewModel.onDescriptionChange(it) },
        onDeleteRoute = { viewModel.deleteRoute(it) }
    )
}


@Composable
fun EditRouteScreen(
    routeName: String,
    routeDescription: String,
    navController: NavController,
    uiState: EditRouteUiState,
    onClearErrorMessage: () -> Unit,
    onResetDoneAction: () -> Unit,
    onNameChange: (newText: String) -> Unit,
    onDescriptionChange: (newText: String) -> Unit,
    onDeleteRoute: (routeName: String) -> Unit,
) {
    val context = LocalContext.current

    var showWaypointDialog by remember { mutableStateOf(false) }

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
            navController.navigate(ScreensForMarkersAndRoutes.Routes.route) {
                popUpTo(ScreensForMarkersAndRoutes.Routes.route) {
                    inclusive = true
                }
                launchSingleTop = true
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

    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(R.string.route_detail_action_edit),
                navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                onNavigateUp = { navController.popBackStack() },
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.route_detail_action_start_route_disabled_hint),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp),
                    )
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
                    CustomButton(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 20.dp, bottom = 10.dp),
                        onClick = {
                            showWaypointDialog = true
                        },
                        buttonColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(10.dp),
                        text = stringResource(R.string.route_detail_edit_waypoints_button),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        thickness = 1.dp,
                    )
                    CustomButton(
                        onClick = { onDeleteRoute(routeName) },
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
            }

        }
    )
}

@Preview(showBackground = true)
@Composable
fun EditRouteScreenPreview() {
    SoundscapeTheme {
        EditRouteScreen(
            routeName = "Route to preview",
            routeDescription = "Description of route",
            navController = rememberNavController(),
            uiState = EditRouteUiState(),
            onClearErrorMessage = {},
            onResetDoneAction = {},
            onNameChange = {},
            onDescriptionChange = {},
            onDeleteRoute = {}
        )
    }
}