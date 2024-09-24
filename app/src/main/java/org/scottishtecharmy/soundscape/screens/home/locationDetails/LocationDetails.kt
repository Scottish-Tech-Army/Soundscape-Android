package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.gson.GsonBuilder
import org.scottishtecharmy.soundscape.mapui.RamaniMapUi
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel

data class LocationDescription(val title : String,
                               val latitude : Double,
                               val longitude : Double)

fun generateLocationDetailsRoute(locationDescription: LocationDescription) : String {
    // Generate JSON for the LocationDescription and append it to the rout
    val gson = GsonBuilder().create()
    val json = gson.toJson(locationDescription)

    return HomeRoutes.LocationDetails.route + "/" + json
}

@Composable
fun LocationDetails(navController : NavController,
                    locationDescription : LocationDescription,
                    useView : Boolean = true) {

    var viewModel : LocationDetailsViewModel? = null
    if(useView)
        viewModel = hiltViewModel<LocationDetailsViewModel>()

    Column(
        modifier = Modifier
            .fillMaxHeight(),
    ) {
        CustomAppBar("Location Details", "Location details screen", navController)
        Text(
            text = locationDescription.title,
            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surfaceBright
        )
        Text(
            text = "A DESCRIPTION BASED ON THE TILE DATA!",
            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surfaceBright
        )
        RamaniMapUi(locationDescription)
        Button(
            onClick = {
                viewModel?.createBeacon(locationDescription.latitude, locationDescription.longitude)
            }
        ) {
            Text(
                text = "Create an audio beacon",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
   }
}

@Preview(showBackground = true)
@Composable
fun LocationDetailsPreview() {
    SoundscapeTheme {
        LocationDetails(
            navController = rememberNavController(),
            LocationDescription("", 0.0, 0.0),
            false
        )
    }
}