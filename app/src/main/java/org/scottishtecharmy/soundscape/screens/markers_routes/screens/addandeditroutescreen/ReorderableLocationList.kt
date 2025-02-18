package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.components.SlideState
import org.scottishtecharmy.soundscape.components.dragToReorder
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun ReorderableLocationList(
    locations: MutableList<LocationDescription>,
    userLocation: LngLatAlt?
)
{
    val listState = rememberLazyListState()
    val isPlaced = remember { mutableStateOf(false) }
    val currentIndex = remember { mutableIntStateOf(0) }
    val destinationIndex = remember { mutableIntStateOf(0) }
    val slideStates = remember {
        mutableStateMapOf<LocationDescription, SlideState>()
            .apply {
                locations.associateWith { SlideState.NONE }.also { putAll(it) }
            }
    }
    val itemHeights = remember(locations.size) { IntArray(size = locations.size) }

    LaunchedEffect(isPlaced.value) {
        if (isPlaced.value) {
            launch {
                if (currentIndex.intValue != destinationIndex.intValue) {

                    // Move the element
                    val element = locations[currentIndex.intValue]
                    locations.removeAt(currentIndex.intValue)
                    locations.add(destinationIndex.intValue, element)

                    // And update the slide state to indicate no longer dragging
                    slideStates.apply {
                        locations.associateWith { SlideState.NONE }.also { putAll(it) }
                    }
                }
                isPlaced.value = false

                for((index, location) in locations.withIndex()) {
                    println("$index: $location")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = listState
    ) {
        items(locations.size) { idx ->
            val locationDescription = locations.getOrNull(idx) ?: return@items
            val slideState = slideStates[locationDescription] ?: SlideState.NONE
            val verticalTranslation = when (slideState) {
                    // The space to fill is the size of the currently dragged item
                    SlideState.UP -> -itemHeights[currentIndex.intValue]
                    SlideState.DOWN -> itemHeights[currentIndex.intValue]
                    else -> 0
            }

            key(locationDescription) {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .offset { IntOffset(0, verticalTranslation) }
                        .onGloballyPositioned {
                            // This is called to update our array of list item heights. That's then
                            // used when calculating the dragging behaviour of the list as each
                            // item can be a different height.
                            itemHeights[idx] = it.size.height
                        }
                        .dragToReorder(
                            item = locationDescription,
                            itemList = locations,
                            itemHeights = itemHeights,
                            updateSlideState = { param: LocationDescription, state: SlideState ->
                                slideStates[param] = state
                            },
                            onStartDrag = {
                                index -> currentIndex.intValue = index
                            },
                            onStopDrag = { currIndex: Int, destIndex: Int ->
                                isPlaced.value = true
                                currentIndex.intValue = currIndex
                                destinationIndex.intValue = destIndex
                            }
                        )
                ) {
                    LocationItem(
                        item = locationDescription,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary),
                        decoration = LocationItemDecoration(
                            index = idx
                        ),
                        userLocation = userLocation
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewReorderableLocationList() {

    SoundscapeTheme {
        ReorderableLocationList(
            locations = mutableListOf(
                LocationDescription(name ="Marker1", location = LngLatAlt(), fullAddress = "111 A street"),
                LocationDescription(name ="Marker2", location = LngLatAlt(), fullAddress = "111 B street"),
                LocationDescription(name ="Marker3", location = LngLatAlt(), fullAddress = "111 C street"),
                LocationDescription(name ="Marker4", location = LngLatAlt(), fullAddress = "111 D street"),
                LocationDescription(name ="Marker5", location = LngLatAlt(), fullAddress = "111 E street"),
            ),
            userLocation = LngLatAlt(1.0, 0.0)
        )
    }
}