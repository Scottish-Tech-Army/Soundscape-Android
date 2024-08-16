package org.scottishtecharmy.soundscape.screens.onboarding

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
import org.scottishtecharmy.soundscape.ui.theme.Primary
import javax.inject.Inject

@HiltViewModel
class AudioBeaconsViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    //the list of live data
    var beaconTypes = mutableListOf<String>()
    private var beacon : Long = 0

    //initialize the viewmodel
    init {
        viewModelScope.launch {
            val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
            for (type in audioEngineBeaconTypes) {
                beaconTypes.add(type)
            }
        }
    }

    fun setAudioBeaconType(type: String) {
        audioEngine.setBeaconType(type)
        if(beacon != 0L)
            audioEngine.destroyBeacon(beacon)
        beacon = audioEngine.createBeacon(0.0, 0.0)
    }

    fun silenceBeacon() {
        if(beacon != 0L)
            audioEngine.destroyBeacon(beacon)
    }
}


@Composable
fun AudioBeacons(onNavigate: (String) -> Unit, mockData : MockHearingPreviewData?) {

    var viewModel : AudioBeaconsViewModel? = null
    if(mockData == null)
        viewModel = hiltViewModel<AudioBeaconsViewModel>()

    var beacons : List<String> = emptyList()
    if(viewModel == null) {
        // Preview operation
        if(mockData != null)
            beacons = mockData.names
    }
    else {
        // Regular operation
        beacons = viewModel.beaconTypes
    }

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
                            beacon,
                            beacon == currentName
                        ) {
                            selected = true
                            // change the audio beacon
                            viewModel?.setAudioBeaconType(beacon)
                            Log.d(
                                "AudioBeacon",
                                "Audio beacon category changed to $beacon"
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
                                viewModel?.silenceBeacon()
                                onNavigate(Screens.Terms.route)
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

// Data used by preview
data object MockHearingPreviewData {
    val names = listOf(
        "Classic",
        "New",
        "Tactile",
        "Flare",
        "Shimmer",
        "Ping",
        "Drop",
        "Signal",
        "Signal Slow",
        "Signal Very Slow",
        "Mallet",
        "Mallet Slow",
        "Mallet Very Slow"
    )
}

@Preview
@Composable
fun IntroductionAudioBeaconPreview() {
    AudioBeacons(onNavigate = {}, MockHearingPreviewData)
}