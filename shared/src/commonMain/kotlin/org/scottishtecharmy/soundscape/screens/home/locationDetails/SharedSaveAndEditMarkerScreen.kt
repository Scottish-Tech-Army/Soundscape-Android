package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreference
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.annotation_description_hint
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.general_alert_done
import org.scottishtecharmy.soundscape.resources.location_detail_adjust_marker_location
import org.scottishtecharmy.soundscape.resources.location_detail_adjust_marker_location_instructions
import org.scottishtecharmy.soundscape.resources.location_detail_full_screen_for_edit_hint
import org.scottishtecharmy.soundscape.resources.marker_name_description_hint
import org.scottishtecharmy.soundscape.resources.markers_action_delete
import org.scottishtecharmy.soundscape.resources.markers_action_delete_alert_message
import org.scottishtecharmy.soundscape.resources.markers_annotation
import org.scottishtecharmy.soundscape.resources.markers_edit_screen_title_edit
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_name
import org.scottishtecharmy.soundscape.resources.settings_reset_dialog_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.resources.user_activity_save_marker_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * Shared screen for creating or editing a marker.
 * Shows name/annotation fields, a map preview, and save/delete controls.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SharedSaveAndEditMarkerScreen(
    locationDescription: LocationDescription,
    userLocation: LngLatAlt?,
    heading: Float = 0f,
    preferencesProvider: PreferencesProvider? = null,
    onCancel: () -> Unit,
    onSave: (LocationDescription) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
) {
    val showMap by rememberBooleanPreference(
        preferencesProvider,
        PreferenceKeys.SHOW_MAP,
        PreferenceDefaults.SHOW_MAP,
    )
    var name by rememberSaveable { mutableStateOf(locationDescription.name) }
    var annotation by rememberSaveable { mutableStateOf(locationDescription.description ?: "") }
    // The marker location can be moved in adjust mode, where a full screen map keeps the marker
    // pinned to its centre so that panning the map moves the marker.
    var latitude by rememberSaveable { mutableStateOf(locationDescription.location.latitude) }
    var longitude by rememberSaveable { mutableStateOf(locationDescription.location.longitude) }
    val markerLocation = LngLatAlt(longitude, latitude)
    val isEditing = locationDescription.databaseId != 0L
    var adjustingLocation by rememberSaveable { mutableStateOf(false) }
    // Where the marker was when adjust mode was entered, so that Cancel can put it back.
    var adjustStartLatitude by rememberSaveable { mutableStateOf(latitude) }
    var adjustStartLongitude by rememberSaveable { mutableStateOf(longitude) }
    val cancelAdjusting = {
        latitude = adjustStartLatitude
        longitude = adjustStartLongitude
        adjustingLocation = false
    }
    var mapInteracting by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(Res.string.markers_action_delete_alert_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(locationDescription.databaseId)
                    },
                    modifier = Modifier.testTag("saveMarkerDeleteConfirm"),
                ) {
                    Text(stringResource(Res.string.ui_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("saveMarkerDeleteCancel"),
                ) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }

    // System back while adjusting behaves like Cancel rather than leaving the whole screen.
    if (adjustingLocation) {
        @Suppress("DEPRECATION")
        BackHandler(enabled = true) { cancelAdjusting() }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            if (adjustingLocation) {
                TextOnlyAppBar(
                    title = stringResource(Res.string.location_detail_adjust_marker_location),
                    navigationButtonTitle = stringResource(Res.string.general_alert_cancel),
                    onNavigateUp = cancelAdjusting,
                    rightButtonTitle = stringResource(Res.string.general_alert_done),
                    onRightButton = { adjustingLocation = false },
                )
            } else {
                TextOnlyAppBar(
                    title = if (isEditing) stringResource(Res.string.markers_edit_screen_title_edit)
                    else stringResource(Res.string.user_activity_save_marker_title),
                    navigationButtonTitle = stringResource(Res.string.general_alert_cancel),
                    onNavigateUp = onCancel,
                    rightButtonTitle = stringResource(Res.string.general_alert_done),
                    onRightButton = {
                        val updated = LocationDescription(
                            name = name.ifBlank { locationDescription.name },
                            description = annotation.ifBlank { null },
                            location = markerLocation,
                            databaseId = locationDescription.databaseId,
                        )
                        onSave(updated)
                    },
                )
            }
        },
        bottomBar = {
            if (isEditing && onDelete != null && !adjustingLocation) {
                Column(modifier = Modifier.smallPadding()) {
                    CustomButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .mediumPadding()
                            .testTag("saveMarkerDeleteButton"),
                        buttonColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(spacing.small),
                        text = stringResource(Res.string.markers_action_delete),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        floatingActionButton = {
            if (showMap && !adjustingLocation) {
                FloatingActionButton(
                    onClick = {
                        adjustStartLatitude = latitude
                        adjustStartLongitude = longitude
                        adjustingLocation = true
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("saveMarkerAdjustLocationButton"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditLocationAlt,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(Res.string.location_detail_full_screen_for_edit_hint),
                    )
                }
            }
        },
    ) { padding ->
        if (adjustingLocation && showMap) {
            // Only the top padding is applied: the bottom padding still includes the Delete
            // button's bar on the first frame, and the map's surface keeps that first, shorter
            // size, leaving a black band at the bottom.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Text(
                    text = stringResource(Res.string.location_detail_adjust_marker_location_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .smallPadding()
                        .testTag("saveMarkerAdjustLocationInstructions"),
                )
                // Fix the centre for this visit to adjust mode, otherwise every edit would
                // re-centre the camera and fight the user's pan.
                val editStartLocation = remember { markerLocation }
                PlatformMapContainer(
                    beaconLocation = markerLocation,
                    mapCenter = editStartLocation,
                    allowScrolling = true,
                    userLocation = userLocation,
                    userSymbolRotation = heading,
                    routeData = null,
                    modifier = Modifier.fillMaxSize(),
                    onBeaconLocationEdited = { edited ->
                        latitude = edited.latitude
                        longitude = edited.longitude
                    },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .smallPadding()
                    .verticalScroll(contentScrollState, enabled = !mapInteracting)
            ) {
                CustomTextField(
                    fieldName = stringResource(Res.string.markers_sort_button_sort_by_name),
                    fieldHint = stringResource(Res.string.marker_name_description_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markerName"),
                    value = name,
                    onValueChange = { name = it },
                    testTagPreFix = "name",
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                CustomTextField(
                    fieldName = stringResource(Res.string.markers_annotation),
                    fieldHint = stringResource(Res.string.annotation_description_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markerAnnotation"),
                    value = annotation,
                    onValueChange = { annotation = it },
                    testTagPreFix = "notes",
                )
                Spacer(modifier = Modifier.height(spacing.medium))

                // Map showing the marker location
                if (showMap) {
                    PlatformMapContainer(
                        mapCenter = markerLocation,
                        allowScrolling = false,
                        userLocation = userLocation,
                        userSymbolRotation = heading,
                        beaconLocation = markerLocation,
                        routeData = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f),
                        onInteractionChanged = { mapInteracting = it },
                    )
                }
            }
        }
    }
}
