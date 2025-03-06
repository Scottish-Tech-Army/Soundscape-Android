package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.geoengine.formatDistance
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

data class EnabledFunction(
    var enabled: Boolean = false,
    var functionString: (String) -> Unit = {},
    var functionBoolean: (Boolean) -> Unit = {},
    var value: Boolean = false,
    var hintWhenOn: String = "",
    var hintWhenOff: String = ""
)
data class LocationItemDecoration(
    val location: Boolean = false,
    val index: Int = -1,
    val editRoute: EnabledFunction = EnabledFunction(),
    val details: EnabledFunction = EnabledFunction()
)

@Composable
fun LocationItem(
    item: LocationDescription,
    userLocation: LngLatAlt?,
    modifier: Modifier = Modifier,
    decoration: LocationItemDecoration = LocationItemDecoration(),
) {
    val context = LocalContext.current
    var distanceString = ""
    if(userLocation != null) {
        distanceString = formatDistance(
            userLocation.distance(item.location),
            context
        )
    }
    Row(
        modifier = modifier.fillMaxWidth()
                           .background(MaterialTheme.colorScheme.primaryContainer)
                           .smallPadding()
                           .fillMaxWidth()
                           .clickable{
                               if (decoration.details.enabled) {
                                   decoration.details.functionString(item.name!!)
                               } else if (decoration.editRoute.enabled) {
                                   decoration.editRoute.functionBoolean(!decoration.editRoute.value)
                               }
                           }
                           .clearAndSetSemantics() {
                               if (decoration.editRoute.enabled) {
                                   // Provide a clearer description of the current state and what
                                   // happens when the user double taps.
                                   contentDescription = if (decoration.editRoute.value)
                                       "Selected. ${item.name!!}"
                                   else
                                       "Not selected. ${item.name!!}"
                                   onClick(
                                       label =
                                           if (decoration.editRoute.value) decoration.editRoute.hintWhenOn
                                           else decoration.editRoute.hintWhenOff,
                                       action = { false }
                                   )
                               } else {
                                   contentDescription = item.name!!
                               }
                           },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(decoration.location) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(spacing.icon)
            )
        } else if (decoration.index != -1) {
            Text(
                text = (decoration.index + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(spacing.targetSize).align(Alignment.CenterVertically)
            )
        }
        Column(
            modifier = Modifier.padding(start = spacing.small).weight(1F),
        ) {
            item.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if(distanceString.isNotEmpty()) {
                Text(
                    text = distanceString,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if(item.fullAddress?.isNotEmpty() == true) {
                item.fullAddress?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        if(decoration.editRoute.enabled) {
            Switch(
                checked = decoration.editRoute.value,
                onCheckedChange = null,                     // Handled by the row
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    uncheckedThumbColor = Color.Red,
                ),
                modifier = Modifier.extraSmallPadding()
            )
        } else if(decoration.details.enabled) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(spacing.icon)
            )
        }
    }
}

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
                fullAddress = "18 Street \n59000 Lille\nFrance",
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
