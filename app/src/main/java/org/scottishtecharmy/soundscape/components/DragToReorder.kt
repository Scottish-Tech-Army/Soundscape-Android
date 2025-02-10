package org.scottishtecharmy.soundscape.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

// This code is based on the excellent drag/drop example here:
//
//   https://github.com/PSPDFKit-labs/Drag-to-Reorder-in-Compose/tree/main
//
@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun <T> Modifier.dragToReorder(
    item: T,
    itemList: List<T>,
    itemHeights: IntArray,
    updateSlideState: (item: T, slideState: SlideState) -> Unit,
    onStartDrag: (currIndex: Int) -> Unit = {},
    onStopDrag: (currIndex: Int, destIndex: Int) -> Unit // Call invoked when drag is finished
): Modifier = composed {

    // Keep track of the of the vertical drag offset smoothly
    val offsetY = remember { Animatable(0f) }

    val itemIndex = itemList.indexOf(item)
    // Threshold for when an item should be considered as moved to a new position in the list
    // Needs to be at least a half of the height of the item but this can be modified as needed
    var numberOfSlidItems = 0
    var previousNumberOfItems: Int
    var listOffset = 0

    val onDragStart = {
        // Interrupt any ongoing animation of other items.
        CoroutineScope(Job()).launch {
            offsetY.stop()
        }
        onStartDrag(itemIndex)
    }
    val onDragging = { change: androidx.compose.ui.input.pointer.PointerInputChange ->

        val verticalDragOffset = offsetY.value + change.positionChange().y
        CoroutineScope(Job()).launch {
            offsetY.snapTo(verticalDragOffset)
            val offsetSign = offsetY.value.sign.toInt()

            previousNumberOfItems = numberOfSlidItems
            numberOfSlidItems = calculateNumberOfSlidItems(
                offsetY.value,
                itemIndex,
                itemHeights
            )

            if (previousNumberOfItems > numberOfSlidItems) {
                //println("Update ${itemIndex + previousNumberOfItems * offsetSign} to NONE ($previousNumberOfItems vs $numberOfSlidItems, ${offsetY.value * offsetSign})")
                updateSlideState(
                    itemList[itemIndex + previousNumberOfItems * offsetSign],
                    SlideState.NONE
                )
            } else if ((numberOfSlidItems != 0) && (previousNumberOfItems != numberOfSlidItems)) {
                try {
                    //println("Update ${itemIndex + numberOfSlidItems * offsetSign} to ${if (offsetSign == 1) "UP" else "DOWN"} ($previousNumberOfItems vs $numberOfSlidItems, ${offsetY.value * offsetSign})")
                    updateSlideState(
                        itemList[itemIndex + numberOfSlidItems * offsetSign],
                        if (offsetSign == 1) SlideState.UP else SlideState.DOWN
                    )
                } catch (e: IndexOutOfBoundsException) {
                    println("Exception: $e")
                    numberOfSlidItems = previousNumberOfItems
                }
            }
            listOffset = numberOfSlidItems * offsetSign
        }
        // Consume the gesture event, not passed to external
        change.consume()
    }
    val onDragEnd = {
        CoroutineScope(Job()).launch {
            if(listOffset == 0) {
                // If we haven't moved the item, then we want to snap back to our starting position.
                // The reordering caused by onStopDrag will update item locations when the positions
                // do change.
                offsetY.snapTo(0.0F)
            }
            onStopDrag(itemIndex, itemIndex + listOffset)
        }
    }
    pointerInput(Unit) {
        coroutineScope {
            detectDragGestures(
                onDragStart = { onDragStart() },
                onDrag = { change, _ -> onDragging(change) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() }
            )
        }
    }.offset {
        IntOffset(0, offsetY.value.roundToInt())
    }
}

enum class SlideState { NONE, UP, DOWN }

fun calculateNumberOfSlidItems(
    offsetY: Float,
    itemIndex: Int,
    itemHeights: IntArray,
): Int {

    var offset = abs(offsetY)
    val down = offsetY.sign.toInt()
    var index = itemIndex
    var count = 0

    // Calculate how many items we've moved by. By making the cutoff point two-thirds of the item
    // height this provides us with some hysteresis in the calculation.
    while (offset > (2 * itemHeights[index]) / 3) {
        offset -= itemHeights[index]
        ++count
        index += down
        if ((index < 0) || (index == itemHeights.size)) break
    }

    return count
}