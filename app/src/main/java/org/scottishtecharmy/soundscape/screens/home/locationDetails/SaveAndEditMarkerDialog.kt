package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomTextField
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SaveAndEditMarkerDialog(
    locationDescription: LocationDescription,
    location: LngLatAlt?,
    heading: Float,
    saveMarker: (description: LocationDescription) -> Unit,
    deleteMarker: (objectId: ObjectId) -> Unit,
    modifier: Modifier = Modifier,
    dialogState: MutableState<Boolean>
) {
    var name by rememberSaveable { mutableStateOf(locationDescription.name ?: "") }
    var annotation by rememberSaveable { mutableStateOf(locationDescription.fullAddress ?: "") }
    val objectId = locationDescription.markerObjectId

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = if(locationDescription.markerObjectId != null) stringResource(R.string.markers_edit_screen_title_edit)
                        else  stringResource(R.string.user_activity_save_marker_title),
                navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                onNavigateUp = { dialogState.value = false },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
            ) {
                if(objectId != null) {
                    CustomButton(
                        onClick = {
                            deleteMarker(objectId)
                            dialogState.value = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .mediumPadding(),
                        buttonColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(spacing.small),
                        text = stringResource(R.string.markers_action_delete),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                CustomButton(
                    Modifier
                        .fillMaxWidth(),
                    onClick = {
                        locationDescription.name = name
                        locationDescription.fullAddress = annotation
                        saveMarker(locationDescription)
                        dialogState.value = false
                    },
                    buttonColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(spacing.small),
                    text = stringResource(R.string.general_alert_done),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .smallPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = spacing.small, bottom = spacing.small),
                    text = stringResource(R.string.markers_sort_button_sort_by_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surfaceBright
                )
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = {
                        println("onValueChange $it")
                        name = it
                        //locationDescription.addressName = it
                    }
                )
                Text(
                    modifier = Modifier.padding(top = spacing.small, bottom = spacing.small),
                    text = stringResource(R.string.markers_annotation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surfaceBright
                )
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = annotation,
                    onValueChange = {
                        println("onValueChange $it")
                        annotation = it
                        //locationDescription.fullAddress = it
                    }
                )
                MapContainerLibre(
                    beaconLocation = locationDescription.location,
                    mapCenter = locationDescription.location,
                    allowScrolling = true,
                    mapViewRotation = 0.0F,
                    userLocation = location ?: LngLatAlt(),
                    userSymbolRotation = heading,
                    routeData = null,
                    onMapLongClick = { false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddRouteScreenPreview() {
    SoundscapeTheme {
        SaveAndEditMarkerDialog(
            locationDescription = LocationDescription(
                name = "Pizza hut",
                location = LngLatAlt(),
                fullAddress = "139 boulevard gambetta",
            ),
            location = null,
            heading = 45.0F,
            saveMarker = {},
            deleteMarker = {},
            modifier = Modifier,
            dialogState = remember { mutableStateOf(false) }
        )
    }
}
