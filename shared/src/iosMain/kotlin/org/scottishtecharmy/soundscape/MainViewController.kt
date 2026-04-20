package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun MainViewController() = ComposeUIViewController {
    val service = remember { IosSoundscapeService.getInstance() }

    App(
        flows = AppFlows(
            locationFlow = service.getLocationFlow(),
            directionFlow = service.getOrientationFlow(),
            placesNearbyUiState = service.placesNearbyUiState,
        ),
        callbacks = AppCallbacks(
            onStartBeacon = { lat, lng, name ->
                service.startBeacon(LngLatAlt(lng, lat), name)
            },
            onStopBeacon = {
                service.destroyBeacon()
            },
            onSpeak = { text ->
                service.speakCallout(text)
            },
            onPlacesNearbyClickFolder = { filter, title ->
                service.placesNearbyClickFolder(filter, title)
            },
            onPlacesNearbyClickBack = {
                service.placesNearbyClickBack()
            },
        ),
    )
}
