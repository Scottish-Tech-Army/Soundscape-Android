package org.scottishtecharmy.soundscape.screens.home.home

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_clear_all_alert_message
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_clear_all_button
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_description
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_export
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_export_button
import org.scottishtecharmy.soundscape.resources.advanced_markers_and_routes_import_button
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.markers_and_routes_import_failure
import org.scottishtecharmy.soundscape.resources.markers_and_routes_import_success
import org.scottishtecharmy.soundscape.resources.menu_advanced_markers_and_routes
import org.scottishtecharmy.soundscape.resources.menu_advanced_markers_and_routes_clear_all_success
import org.scottishtecharmy.soundscape.resources.settings_reset_dialog_title
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedAdvancedMarkersAndRoutesSettingsScreen(
    holder: AdvancedMarkersAndRoutesSettingsViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val successString = stringResource(Res.string.markers_and_routes_import_success)
    val failureString = stringResource(Res.string.markers_and_routes_import_failure)
    val clearAllSuccessString =
        stringResource(Res.string.menu_advanced_markers_and_routes_clear_all_success)
    val chooserText = stringResource(Res.string.advanced_markers_and_routes_export)

    SharedAdvancedMarkersAndRoutesSettingsScreen(
        userFeedback = holder.userFeedback.collectAsState("").value,
        onUserFeedbackShown = holder::userFeedbackShown,
        onExport = { holder.exportMarkersAndRoutes(chooserText) },
        onImport = { holder.importMarkersAndRoutes(successString, failureString) },
        onClearAll = { holder.deleteAllMarkersAndRoutes(clearAllSuccessString) },
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}

@Composable
fun SharedAdvancedMarkersAndRoutesSettingsScreen(
    userFeedback: String,
    onUserFeedbackShown: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val showConfirmationDialog = remember { mutableStateOf(false) }

    LaunchedEffect(userFeedback) {
        if (userFeedback.isNotEmpty()) {
            snackBarHostState.showSnackbar(
                message = userFeedback,
                duration = SnackbarDuration.Short,
            )
            onUserFeedbackShown()
        }
    }

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text(stringResource(Res.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(Res.string.advanced_markers_and_routes_clear_all_alert_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showConfirmationDialog.value = false
                    },
                    modifier = Modifier.testTag("advancedMarkersClearAllConfirm"),
                ) {
                    Text(stringResource(Res.string.ui_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog.value = false },
                    modifier = Modifier.testTag("advancedMarkersClearAllCancel"),
                ) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            FlexibleAppBar(
                title = stringResource(Res.string.menu_advanced_markers_and_routes),
                leftSide = {
                    IconWithTextButton(
                        text = stringResource(Res.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft"),
                    ) { onNavigateUp() }
                },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                Text(
                    text = stringResource(Res.string.advanced_markers_and_routes_description),
                    modifier = Modifier.mediumPadding(),
                )
                CustomButton(
                    onClick = { showConfirmationDialog.value = true },
                    text = stringResource(Res.string.advanced_markers_and_routes_clear_all_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding()
                        .testTag("advancedMarkersClearAllButton"),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                CustomButton(
                    onClick = onExport,
                    text = stringResource(Res.string.advanced_markers_and_routes_export_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding()
                        .testTag("advancedMarkersExportButton"),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                CustomButton(
                    onClick = onImport,
                    text = stringResource(Res.string.advanced_markers_and_routes_import_button),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding()
                        .testTag("advancedMarkersImportButton"),
                    shape = RoundedCornerShape(spacing.extraSmall),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}
