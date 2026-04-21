package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
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
    onCancel: () -> Unit,
    onSave: (LocationDescription) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(locationDescription.name) }
    var annotation by rememberSaveable { mutableStateOf(locationDescription.description ?: "") }
    val isEditing = locationDescription.databaseId != 0L

    Scaffold(
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
                            .mediumPadding(),
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .smallPadding()
                .verticalScroll(rememberScrollState())
        ) {
            CustomTextField(
                fieldName = stringResource(Res.string.markers_sort_button_sort_by_name),
                fieldHint = stringResource(Res.string.marker_name_description_hint),
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            CustomTextField(
                fieldName = stringResource(Res.string.markers_annotation),
                fieldHint = stringResource(Res.string.annotation_description_hint),
                modifier = Modifier.fillMaxWidth(),
                value = annotation,
                onValueChange = { annotation = it },
            )
            Spacer(modifier = Modifier.height(spacing.medium))

            // Map showing the marker location
            PlatformMapContainer(
                mapCenter = locationDescription.location,
                allowScrolling = true,
                userLocation = userLocation,
                userSymbolRotation = heading,
                beaconLocation = locationDescription.location,
                routeData = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f),
            )
        }
    }
}
