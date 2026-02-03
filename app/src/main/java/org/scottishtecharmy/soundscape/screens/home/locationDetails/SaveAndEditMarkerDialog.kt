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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SaveAndEditMarkerDialog(
    locationDescription: LocationDescription,
    location: LngLatAlt?,
    heading: Float,
    saveMarker: (
        description: LocationDescription,
        successMessage: String,
        failureMessage: String) -> Unit,
    deleteMarker: (objectId: Long) -> Unit,
    modifier: Modifier = Modifier,
    dialogState: MutableState<Boolean>
) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)

    var name by rememberSaveable { mutableStateOf(locationDescription.name) }
    var annotation by rememberSaveable { mutableStateOf(locationDescription.description ?: "") }
    val objectId = locationDescription.databaseId
    val fullscreenMap = remember { mutableStateOf(false) }
    val newLocation = remember { locationDescription.location.clone() }

    val successMessage = stringResource(R.string.markers_marker_created)
    val failureMessage = stringResource(R.string.general_error_add_marker_error)
    Scaffold(
        modifier = modifier,
        topBar = {
            TextOnlyAppBar(
                title = if(locationDescription.databaseId != 0L) stringResource(R.string.markers_edit_screen_title_edit)
                        else  stringResource(R.string.user_activity_save_marker_title),
                navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                onNavigateUp = { dialogState.value = false },
                rightButtonTitle = stringResource(R.string.general_alert_done),
                onRightButton = {
                    locationDescription.name = name
                    locationDescription.description = annotation
                    locationDescription.location = newLocation
                    saveMarker(locationDescription, successMessage, failureMessage)
                    dialogState.value = false
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.smallPadding()
            ) {
                if(objectId != 0L) {
                    CustomButton(
                        onClick = {
                            deleteMarker(objectId)
                            dialogState.value = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .mediumPadding(),
                        buttonColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(spacing.small),
                        text = stringResource(R.string.markers_action_delete),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        floatingActionButton = {
            if(showMap) FullScreenMapFab(
                fullscreenMap = fullscreenMap,
                openMapHint = R.string.location_detail_full_screen_for_edit_hint,
                closeMapHint = R.string.location_detail_exit_full_screen_for_edit_hint
            )
        },
        content = { padding ->
            if(fullscreenMap.value) {
                MapContainerLibre(
                    beaconLocation = newLocation,
                    mapCenter = newLocation,
                    editBeaconLocation = true,
                    allowScrolling = true,
                    userLocation = location ?: LngLatAlt(),
                    userSymbolRotation = heading,
                    routeData = null,
                    onMapLongClick = { false },
                    modifier = Modifier
                        .fillMaxSize(),
                    showMap = showMap
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .smallPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    CustomTextField(
                        fieldName = stringResource(R.string.markers_sort_button_sort_by_name),
                        fieldHint = stringResource(R.string.marker_name_description_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("markerName"),
                        value = name,
                        onValueChange = {
                            name = it
                        }
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    CustomTextField(
                        fieldName = stringResource(R.string.markers_annotation),
                        fieldHint = stringResource(R.string.annotation_description_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("markerAnnotation"),
                        value = annotation,
                        onValueChange = {
                            annotation = it
                        }
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))

                    MapContainerLibre(
                        beaconLocation = newLocation,
                        mapCenter = newLocation,
                        editBeaconLocation = false,
                        allowScrolling = true,
                        userLocation = location ?: LngLatAlt(),
                        userSymbolRotation = heading,
                        routeData = null,
                        onMapLongClick = { false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f)
                            .smallPadding(),
                        showMap = showMap
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddRouteScreenPreview() {
    SaveAndEditMarkerDialog(
        locationDescription = LocationDescription(
            name = "Pizza hut",
            location = LngLatAlt(),
            description = "139 boulevard gambetta"
        ),
        location = null,
        heading = 45.0F,
        saveMarker = {_,_,_ ->},
        deleteMarker = {},
        modifier = Modifier,
        dialogState = remember { mutableStateOf(false) }
    )
}
