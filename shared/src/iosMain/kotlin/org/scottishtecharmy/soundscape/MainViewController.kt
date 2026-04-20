package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun MainViewController() = ComposeUIViewController {
    val service = remember { IosSoundscapeService.getInstance() }

    App(
        locationFlow = service.getLocationFlow(),
        directionFlow = service.getOrientationFlow(),
        onStartBeacon = { lat, lng, name ->
            service.startBeacon(LngLatAlt(lng, lat), name)
        },
        onStopBeacon = {
            service.destroyBeacon()
        },
        onSpeak = { text ->
            service.speakCallout(text)
        },
    )
}
