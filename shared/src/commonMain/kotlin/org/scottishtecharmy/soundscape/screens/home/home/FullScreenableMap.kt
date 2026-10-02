package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * A screen's map, which can be shown either in its place in the screen's content ([Inline]) or
 * filling the screen ([FullScreen]), with [fullscreen] saying which. A screen calls exactly one of
 * the two on each composition.
 *
 * It's one map either way. The map is movable content, so going full screen moves the existing map
 * into the full screen slot and resizes it - keeping its camera, style and tiles - rather than
 * throwing it away and building a second one from scratch, which on a slow phone meant several
 * seconds of blank map every time the button was pressed.
 */
@Stable
class FullScreenableMap internal constructor(
    val fullscreen: MutableState<Boolean>,
    private val map: @Composable (allowScrolling: Boolean, onInteractionChanged: (Boolean) -> Unit, modifier: Modifier) -> Unit,
) {
    /**
     * The map in its place in the screen's content. Panning is off so that a drag scrolls the
     * content instead - see MapContainerLibre's onInteractionChanged for the pinch that does reach
     * the map.
     */
    @Composable
    fun Inline(modifier: Modifier, onInteractionChanged: (Boolean) -> Unit = {}) =
        map(false, onInteractionChanged, modifier)

    /** The map filling the screen, where there's nothing to scroll and so it can be panned. */
    @Composable
    fun FullScreen(modifier: Modifier = Modifier.fillMaxSize()) = map(true, {}, modifier)
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
    // The movable content is created once, so it reads the latest of these rather than capturing
    // the first.
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
    return remember {
        FullScreenableMap(
            fullscreen = mutableStateOf(false),
            map = movableContentOf { allowScrolling: Boolean, onInteractionChanged: (Boolean) -> Unit, modifier: Modifier ->
                val c = content.value
                if (c.mapCenter != null) {
                    PlatformMapContainer(
                        mapCenter = c.mapCenter,
                        allowScrolling = allowScrolling,
                        userLocation = c.userLocation,
                        userSymbolRotation = c.userSymbolRotation,
                        beaconLocation = c.beaconLocation,
                        routeData = c.routeData,
                        currentBeaconWaypointIndex = c.currentBeaconWaypointIndex,
                        modifier = modifier,
                        onInteractionChanged = onInteractionChanged,
                    )
                }
            },
        )
    }
}

private data class MapContent(
    val mapCenter: LngLatAlt?,
    val userLocation: LngLatAlt?,
    val userSymbolRotation: Float,
    val beaconLocation: LngLatAlt?,
    val routeData: RouteWithMarkers?,
    val currentBeaconWaypointIndex: Int,
)
