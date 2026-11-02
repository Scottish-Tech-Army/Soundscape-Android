package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlin.test.Test
import kotlin.test.assertEquals

class WitMotionBatteryTest {

    @Test
    fun followsWitMotionsTable() {
        assertEquals(100, witMotionBatteryPercent(420))
        assertEquals(100, witMotionBatteryPercent(397))
        assertEquals(90, witMotionBatteryPercent(396))
        assertEquals(90, witMotionBatteryPercent(393))
        assertEquals(75, witMotionBatteryPercent(392))
        assertEquals(75, witMotionBatteryPercent(387))
        assertEquals(60, witMotionBatteryPercent(386))
        assertEquals(60, witMotionBatteryPercent(382))
        assertEquals(50, witMotionBatteryPercent(381))
        assertEquals(50, witMotionBatteryPercent(379))
        assertEquals(40, witMotionBatteryPercent(378))
        assertEquals(40, witMotionBatteryPercent(377))
        assertEquals(30, witMotionBatteryPercent(376))
        assertEquals(30, witMotionBatteryPercent(373))
        assertEquals(20, witMotionBatteryPercent(372))
        assertEquals(20, witMotionBatteryPercent(370))
        assertEquals(15, witMotionBatteryPercent(369))
        assertEquals(15, witMotionBatteryPercent(368))
        assertEquals(10, witMotionBatteryPercent(367))
        assertEquals(10, witMotionBatteryPercent(350))
        assertEquals(5, witMotionBatteryPercent(349))
        assertEquals(5, witMotionBatteryPercent(340))
        assertEquals(0, witMotionBatteryPercent(339))
        assertEquals(0, witMotionBatteryPercent(0))
    }

    @Test
    fun theSensorsReadingOf371Is20Percent() {
        assertEquals(20, witMotionBatteryPercent(371))
    }
}
