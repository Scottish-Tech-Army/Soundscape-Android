package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState

@Preview(showBackground = true)
@Composable
fun AddWaypointsListPreview() {
    AddWaypointsList(
        uiState =
            AddAndEditRouteUiState(
                routeMembers =
                    mutableListOf(
                        LocationDescription(name = "Waypoint 1", location = LngLatAlt(), databaseId = 1L),
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 3", location = LngLatAlt(), databaseId = 3L),
                    ),
                markers =
                    mutableListOf(
                        LocationDescription(name = "Waypoint 1", location = LngLatAlt(), databaseId = 1L),
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 3", location = LngLatAlt(), databaseId = 3L),
                        LocationDescription(name = "Waypoint 4", location = LngLatAlt(), databaseId = 4L),
                        LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = 5L),
                        LocationDescription(name = "Waypoint 6", location = LngLatAlt(), databaseId = 6L),
                        LocationDescription(name = "Waypoint 7", location = LngLatAlt(), databaseId = 7L),
                        LocationDescription(name = "Waypoint 8", location = LngLatAlt(), databaseId = 8L),
                    ),
                toggledMembers =
                    listOf(
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = 5L),
                    )
            ),
        placesNearbyUiState = PlacesNearbyUiState(),
        onClickFolder = {_,_ -> },
        onSelectLocation = {_ -> },
        onToggleMember = {_ -> },
        userLocation = LngLatAlt(),
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}
