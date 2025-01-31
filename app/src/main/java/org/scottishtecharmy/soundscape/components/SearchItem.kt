package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.Foreground2
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
import org.scottishtecharmy.soundscape.ui.theme.PaleBlue
import org.scottishtecharmy.soundscape.utils.buildAddressFormat

@Composable
fun SearchItemButton(
    item: LocationDescription,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(0),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = Color.White,
            )
            Column(
                modifier = Modifier.padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                item.addressName?.let {
                    Text(
                        text = it,
                        fontWeight = FontWeight(700),
                        fontSize = 22.sp,
                        color = Color.White,
                    )
                }
                item.distance?.let {
                    Text(
                        text = it,
                        color = Foreground2,
                        fontWeight = FontWeight(450),
                    )
                }
                item.buildAddressFormat()?.let {
                    Text(
                        text = it,
                        fontWeight = FontWeight(400),
                        fontSize = 18.sp,
                        color = PaleBlue,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
fun PreviewSearchItemButton() {
    IntroductionTheme {
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
                    addressName = "Bristol",
                    streetNumberAndName = "18 Street",
                    postcodeAndLocality = "59000 Lille",
                    distance = "17 Km",
                    country = "France",
                    location = LngLatAlt(8.00, 9.55)
                )
            SearchItemButton(test, onClick = {}, Modifier.width(200.dp))
        }
    }
}
