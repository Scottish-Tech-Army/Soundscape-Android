package org.scottishtecharmy.soundscape.screens.home.home

import kotlin.test.Test
import kotlin.test.assertEquals

class JoinRoadNamesTest {

    @Test
    fun noRoads_isEmpty() {
        assertEquals("", joinRoadNames(emptyList(), null))
    }

    @Test
    fun oneRoad_isJustTheName() {
        assertEquals("Moor Road", joinRoadNames(listOf("Moor Road"), " and Moor Road"))
    }

    @Test
    fun englishString_keepsASingleSpace() {
        assertEquals(
            "Main Street, High Street and Moor Road",
            joinRoadNames(listOf("Main Street", "High Street", "Moor Road"), " and Moor Road"),
        )
    }

    @Test
    fun translationThatLostItsSpace_getsOneBack() {
        // Indonesian stored "dan %1$s", which used to give "Jalan Bdan Jalan C".
        assertEquals(
            "Jalan A, Jalan B dan Jalan C",
            joinRoadNames(listOf("Jalan A", "Jalan B", "Jalan C"), "dan Jalan C"),
        )
    }
}
