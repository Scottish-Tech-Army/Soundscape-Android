package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroute

import android.widget.Toast
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
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextFieldWithLabel
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun AddRouteScreen(
    navController: NavController,
    viewModel: AddRouteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Observe navigation and trigger it if necessary
    LaunchedEffect(uiState.navigateToMarkersAndRoutes) {
        if (uiState.navigateToMarkersAndRoutes) {
            navController.navigate("${HomeRoutes.MarkersAndRoutes.route}/routes") {
                popUpTo(HomeRoutes.MarkersAndRoutes.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            viewModel.resetNavigationState()
            Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(R.string.route_detail_action_create),
                navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                onNavigateUp = { navController.popBackStack()}
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.route_no_waypoints_hint_1),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                    )
                TextFieldWithLabel(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 5.dp),
                    value = uiState.name,
                    label =  stringResource(R.string.markers_sort_button_sort_by_name),
                    onValueChange = { newText -> viewModel.onNameChange(newText) },
                )
                TextFieldWithLabel(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 5.dp),
                    value = uiState.description,
                    label = stringResource(R.string.route_detail_edit_description),
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
                        viewModel.onAddWaypointsClicked()
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
                // TODO fix the formatting/alignment for the addition of the original iOS text below
            }
        }
    )
}

@Preview
@Composable
fun AddRoutePreview() {
    SoundscapeTheme {
        AddRouteScreen(navController = rememberNavController())
    }
}

