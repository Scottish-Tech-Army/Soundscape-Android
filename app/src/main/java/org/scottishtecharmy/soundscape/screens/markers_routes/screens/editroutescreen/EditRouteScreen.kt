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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField

@Composable
fun EditRouteScreen(
    routeName: String,
    routeDescription: String,
    navController: NavController,
    viewModel: EditRouteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showWaypointDialog by remember { mutableStateOf(false) }

    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    // Observe navigation and trigger it if necessary
    LaunchedEffect(uiState.doneActionCompleted) {
        if (uiState.doneActionCompleted) {
            val actionType = uiState.actionType
            navController.navigate("${HomeRoutes.MarkersAndRoutes.route}/routes") {
                popUpTo(HomeRoutes.MarkersAndRoutes.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            viewModel.resetDoneActionState()
            val message = when (actionType) {
                ActionType.UPDATE -> context.getString(R.string.route_edited_successfully)
                ActionType.DELETE -> context.getString(R.string.route_deleted_successfully)
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
                showDoneButton = uiState.showDoneButton,
                onDoneClicked = {
                    viewModel.updateRoute()
                }
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
                        stringResource(R.string.route_edit_no_waypoints_hint_1),
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
                        onValueChange = { newText -> viewModel.onNameChange(newText) },
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
                        onValueChange = { newText -> viewModel.onDescriptionChange(newText) },
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
                        onClick = { viewModel.deleteRoute(routeName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        buttonColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(10.dp),
                        text = stringResource(R.string.delete_route),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )

                }
            }

        }
    )
//TODO create @Preview that works with viewmodel
}