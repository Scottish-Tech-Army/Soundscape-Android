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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.scottishtecharmy.soundscape.resources.location_detail_exit_full_screen_for_edit_hint
import org.scottishtecharmy.soundscape.resources.location_detail_full_screen_for_edit_hint
import org.scottishtecharmy.soundscape.resources.marker_name_description_hint
import org.scottishtecharmy.soundscape.resources.markers_action_delete
import org.scottishtecharmy.soundscape.resources.markers_annotation
import org.scottishtecharmy.soundscape.resources.markers_edit_screen_title_edit
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_name
import org.scottishtecharmy.soundscape.resources.user_activity_save_marker_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
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
    val isEditing = locationDescription.databaseId != 0L
    val fullscreenMap = remember { mutableStateOf(false) }
    var mapInteracting by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
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
                        location = locationDescription.location,
                        databaseId = locationDescription.databaseId,
                    )
                    onSave(updated)
                },
            )
        },
        bottomBar = {
            if (isEditing && onDelete != null) {
                Column(modifier = Modifier.smallPadding()) {
                    CustomButton(
                        onClick = {
                            onDelete(locationDescription.databaseId)
                        },
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
            if (showMap) FullScreenMapFab(
                fullscreenMap = fullscreenMap,
                modifier = Modifier.testTag("saveMarkerFullScreenMapFab"),
                openMapHint = Res.string.location_detail_full_screen_for_edit_hint,
                closeMapHint = Res.string.location_detail_exit_full_screen_for_edit_hint,
            )
        },
    ) { padding ->
        if (fullscreenMap.value && showMap) {
            PlatformMapContainer(
                beaconLocation = locationDescription.location,
                mapCenter = locationDescription.location,
                allowScrolling = true,
                userLocation = userLocation,
                userSymbolRotation = heading,
                routeData = null,
                modifier = Modifier.fillMaxSize(),
            )
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
                        mapCenter = locationDescription.location,
                        allowScrolling = false,
                        userLocation = userLocation,
                        userSymbolRotation = heading,
                        beaconLocation = locationDescription.location,
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
