package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

@Preview(name = "Light Mode")
@Composable
fun PreviewSearchItemButton() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val test =
            LocationDescription(
                name = "Bristol",
                description = "18 Street \n59000 Lille\nFrance",
                location = LngLatAlt(8.00, 9.55)
            )
        LocationItem(
            item = test,
            userLocation = LngLatAlt(9.00, 9.55),
            decoration = LocationItemDecoration(
                location = true,
                editRoute = EnabledFunction(true, {}, {}, true),
                details = EnabledFunction(false),
            ),
        )
    }
}

@Preview(name = "Compact")
@Composable
fun PreviewCompactSearchItemButton() {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val test =
            LocationDescription(
                name = "Bristol",
                location = LngLatAlt(8.00, 9.55)
            )
        LocationItem(
            item = test,
            decoration = LocationItemDecoration(
                location = true,
                editRoute = EnabledFunction(false),
                details = EnabledFunction(true),
                source = LocationSource.OfflineGeocoder
            ),
            userLocation = LngLatAlt(8.00, 10.55)
        )
        LocationItem(
            item = test,
            decoration = LocationItemDecoration(
                location = true,
                editRoute = EnabledFunction(false),
                details = EnabledFunction(true),
                source = LocationSource.AndroidGeocoder
            ),
            userLocation = LngLatAlt(8.00, 10.55)
        )
        LocationItem(
            item = test,
            decoration = LocationItemDecoration(
                location = true,
                editRoute = EnabledFunction(false),
                details = EnabledFunction(true),
                source = LocationSource.PhotonGeocoder
            ),
            userLocation = LngLatAlt(8.00, 10.55)
        )
        LocationItem(
            item = test,
            decoration = LocationItemDecoration(
                location = false,
                editRoute = EnabledFunction(false),
                details = EnabledFunction(false),
                index = 99,
            ),
            userLocation = LngLatAlt(8.00, 10.55)
        )
    }
}

@Preview(name = "Compact")
@Composable
fun PreviewOrderedItemButton() {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val test =
            LocationDescription(
                name = "Bristol",
                location = LngLatAlt(8.00, 9.55)
            )
        LocationItem(
            item = test,
            decoration = LocationItemDecoration(
                index = 2,
            ),
            userLocation = LngLatAlt(8.20, 9.55)
        )
    }
}
