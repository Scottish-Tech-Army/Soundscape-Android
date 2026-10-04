package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * A screen's map, which is shown either in its place in the screen's content or filling the screen,
 * with [fullscreen] saying which.
 *
 * It's one map either way, and it has to stay where it is in the composition: the screen calls
 * [Content] from the same place whether or not it's full screen, and goes full screen by leaving
 * out the content around the map and giving it a modifier that fills the space. The map is then
 * only resized. Composing it somewhere else instead - whether as a second map or by moving this one
 * - detaches its view on Android, which throws away the map's surface and everything it has on the
 * GPU, and it is seconds on a slow phone before the map is drawn again.
 *
 * The screen should also only call [Content] once it knows the size the map is to be. A map that
 * is created at one size and resized a frame later sets its surface's crop twice in quick
 * succession, and on some Android phones (a Samsung A54 on Android 16) the first can be applied
 * last: the map then shows only as much of itself as its first size, with black for the rest.
 */
@Stable
class FullScreenableMap internal constructor(
    val fullscreen: MutableState<Boolean>,
    private val content: State<MapContent>,
) {
    /**
     * The map. Panning is off unless it's full screen, so that a drag scrolls the screen's content
     * instead - see MapContainerLibre's onInteractionChanged for the pinch that does reach the map.
     */
    @Composable
    fun Content(modifier: Modifier, onInteractionChanged: (Boolean) -> Unit = {}) {
        val c = content.value
        if (c.mapCenter != null) {
            PlatformMapContainer(
                mapCenter = c.mapCenter,
                allowScrolling = fullscreen.value,
                userLocation = c.userLocation,
                userSymbolRotation = c.userSymbolRotation,
                beaconLocation = c.beaconLocation,
                routeData = c.routeData,
                currentBeaconWaypointIndex = c.currentBeaconWaypointIndex,
                modifier = modifier,
                onInteractionChanged = onInteractionChanged,
            )
        }
    }
}

/**
 * Remembers the screen's [FullScreenableMap]. The parameters are those of [PlatformMapContainer];
 * nothing is shown while [mapCenter] is null.
 */
@Composable
fun rememberFullScreenableMap(
    mapCenter: LngLatAlt?,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    currentBeaconWaypointIndex: Int = 0,
): FullScreenableMap {
    val content: State<MapContent> = rememberUpdatedState(
        MapContent(
            mapCenter,
            userLocation,
            userSymbolRotation,
            beaconLocation,
            routeData,
            currentBeaconWaypointIndex,
        )
    )
    return remember { FullScreenableMap(fullscreen = mutableStateOf(false), content = content) }
}

internal data class MapContent(
    val mapCenter: LngLatAlt?,
    val userLocation: LngLatAlt?,
    val userSymbolRotation: Float,
    val beaconLocation: LngLatAlt?,
    val routeData: RouteWithMarkers?,
    val currentBeaconWaypointIndex: Int,
)
