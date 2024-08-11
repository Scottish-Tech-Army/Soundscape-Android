package org.scottishtecharmy.soundscape.screens.onboarding

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.navigation.Screens
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
import org.scottishtecharmy.soundscape.ui.theme.Primary

data class AudioBeacon(
    val name: String
)

fun getAllAudioBeacons(): List<AudioBeacon> {
    return listOf(
        AudioBeacon(
            name = "Classic"
        ),
        AudioBeacon(
            name = "Drop"
        ),
        AudioBeacon(
            name = "Flare"
        ),
        AudioBeacon(
            name = "Mallet",
        ),
        AudioBeacon(
            name = "Mallet Slow",
        ),
        AudioBeacon(
            name = "Mallet Very Slow",
        ),
        AudioBeacon(
            name = "New",
        ),
        AudioBeacon(
            name = "Ping",
        ),
        AudioBeacon(
            name = "Route",
        ),
        AudioBeacon(
            name = "Shimmer",
        ),
        AudioBeacon(
            name = "Signal",
        ),
        AudioBeacon(
            name = "Signal Slow",
        ),
        AudioBeacon(
            name = "Signal Very Slow",
        ),
        AudioBeacon(
            name = "Tactile"
        )
    )
}
@Composable
fun AudioBeacons(navController: NavHostController) {

    val beacons = getAllAudioBeacons()
    var selected by remember { mutableStateOf(false) }

    val currentName = if (selected) "Do some stuff here in the datastore manager" else null


    IntroductionTheme {
        MaterialTheme(typography = IntroTypography) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .padding(top = 60.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // TODO translations
                Text(
                    text = "Choose an Audio Beacon",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "When navigating with Soundscape, you will hear the audio beacon in the direction of your destination. By following its sound, you will always know which way to go.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "Listen to the available audio beacons and select your preference.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "This can be changed later in app settings.",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))

                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .fillMaxWidth()
                        .heightIn(100.dp, 300.dp)
                        .background(Color.White)
                ) {
                    items(beacons) { beacon ->
                        AudioBeaconItem(
                            beacon.name,
                            beacon.name == currentName
                        ) {
                            selected = true
                            // change the audio beacon
                            Log.d(
                                "AudioBeacon",
                                "Audio beacon category changed to ${beacon.name}"
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 60.dp)
                        .requiredHeight(40.dp)
                ) {
                    if (selected) {
                        OnboardButton(
                            text = stringResource(R.string.ui_continue),
                            onClick = {
                                //navController.navigate(Screens.IntroductionTerms.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioBeaconItem(text: String, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            // Accessibility recommendation for the size of a clickable thing
            .padding(horizontal = 10.dp, vertical = 17.dp)
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        if (isSelected) {
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Primary)
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp)
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 2.dp),
        thickness = 0.8.dp,
        color = Primary
    )
}


@Preview
@Composable
fun IntroductionAudioBeaconPreview() {
    AudioBeacons(navController = rememberNavController())
}