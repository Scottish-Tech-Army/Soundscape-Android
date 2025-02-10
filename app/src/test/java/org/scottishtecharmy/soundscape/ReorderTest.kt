package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.components.calculateNumberOfSlidItems
import org.scottishtecharmy.soundscape.geoengine.GridState.Companion.createFromGeoJson
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import kotlin.math.abs
import kotlin.math.sign

class ReorderTest {

    @Test
    fun testReorder() {

        val itemHeights = listOf(100,200,50,100,200)
        var lastCount = 0
        val targets = listOf(-600, 600, 0)
        var offset = 0
        for(target in targets.dropLast(1) ) {
            val nextTarget = targets[(targets.indexOf(target) + 1)]
            val delta = (target- nextTarget) / abs(nextTarget - target)
            while(offset != target) {
                val numberOfSlidItems = calculateNumberOfSlidItems(
                    offset.toFloat(),
                    2,
                    itemHeights,
                    0
                )
                if (lastCount != numberOfSlidItems) {
                    println("Offset $offset -> $numberOfSlidItems")
                    lastCount = numberOfSlidItems
                }
                offset += delta
            }
        }
   }
}