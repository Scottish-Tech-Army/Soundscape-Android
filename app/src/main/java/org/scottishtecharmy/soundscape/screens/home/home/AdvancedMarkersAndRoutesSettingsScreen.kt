package org.scottishtecharmy.soundscape.screens.home.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.viewmodels.AdvancedMarkersAndRoutesSettingsViewModel

@Composable
fun AdvancedMarkersAndRoutesSettingsScreenVM(
    navController: NavHostController,
    modifier: Modifier
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<AdvancedMarkersAndRoutesSettingsViewModel>()
    val successString = stringResource(R.string.markers_and_routes_import_success)
    val failureString = stringResource(R.string.markers_and_routes_import_failure)
    val clearAllSuccessString = stringResource(R.string.menu_advanced_markers_and_routes_clear_all_success)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // 3. When a file is selected, pass the URI back to the ViewModel
                viewModel.importMarkersAndRoutes(context, uri, successString, failureString)
            }
        }
    )
    val userFeedback = viewModel.userFeedback.collectAsStateWithLifecycle("")
    println("userFeedback ${userFeedback.value}")

    LaunchedEffect(Unit) {
        viewModel.importEvent.collect {
            // When the event is received, launch the file picker
            filePickerLauncher.launch("application/zip")
        }
    }
    val chooserText = stringResource(R.string.advanced_markers_and_routes_export)
    AdvancedMarkersAndRoutesSettingsScreen(
        navController,
        { viewModel.exportMarkersAndRoutes(context, chooserText) },
        { viewModel.triggerImport() },
        { viewModel.deleteAllMarkersAndRoutes(context, clearAllSuccessString) },
        userFeedback.value,
        viewModel::userFeedbackShown,
        modifier
    )
}

@Composable
fun AdvancedMarkersAndRoutesSettingsScreen(
    navController: NavHostController,
    exportMarkersAndRoutes: () -> Unit = {},
    importMarkersAndRoutes: () -> Unit = {},
    clearMarkersAndRoutes: () -> Unit = {},
    userFeedback: String,
    onUserFeedbackShown: () -> Unit,
    modifier: Modifier) {

    val snackBarHostState = remember { SnackbarHostState() }
    val showConfirmationDialog = remember { mutableStateOf(false) }

    LaunchedEffect(userFeedback) {
        if (userFeedback.isNotEmpty()) {
            snackBarHostState.showSnackbar(
                message = userFeedback,
                duration = SnackbarDuration.Short
            )
            onUserFeedbackShown()
        }
    }

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.advanced_markers_and_routes_clear_all_alert_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearMarkersAndRoutes()
                        showConfirmationDialog.value = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ui_continue),
                        modifier = Modifier
                            .talkbackHint(stringResource(R.string.settings_reset_button_hint))
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog.value = false }
                ) {
                    Text(stringResource(R.string.general_alert_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        topBar = {
            FlexibleAppBar(
                title = stringResource(R.string.menu_advanced_markers_and_routes),
                leftSide = {
                    IconWithTextButton(
                        text = stringResource(R.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft")
                    ) {
                        navController.navigateUp()
                    }
                }
            )
        },

        content = { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                Text(
                    text = stringResource(R.string.advanced_markers_and_routes_description),
                    modifier = Modifier
                        .mediumPadding(),
                )
                CustomButton(
                    onClick = {
                        showConfirmationDialog.value = true
                    },
                    text = stringResource(R.string.advanced_markers_and_routes_clear_all_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding(),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                CustomButton(
                    onClick = {
                        exportMarkersAndRoutes()
                    },
                    text = stringResource(R.string.advanced_markers_and_routes_export_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding(),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                CustomButton(
                    onClick = {
                        importMarkersAndRoutes()
                    },
                    text = stringResource(R.string.advanced_markers_and_routes_import_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding(),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AdvancedMarkersAndRoutesSettingsPreview() {
    AdvancedMarkersAndRoutesSettingsScreen(
        navController = rememberNavController(),
        modifier = Modifier,
        userFeedback = "",
        onUserFeedbackShown = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AdvancedMarkersAndRoutesSettingsAlertPreview() {
    AdvancedMarkersAndRoutesSettingsScreen(
        navController = rememberNavController(),
        modifier = Modifier,
        userFeedback = "What's going on?",
        onUserFeedbackShown = {},
    )
}
