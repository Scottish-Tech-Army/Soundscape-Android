package org.scottishtecharmy.soundscape.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The proximity beacon plays alongside the directional one and tells the user how close
 * the destination is. Its asset choice is purely a function of distance, so it can be
 * checked without any audio hardware.
 */
class ProximityBeaconTest {

    private fun selectAt(distance: Double, proximityNear: Double = 15.0) =
        PROXIMITY_BEACON_TYPE.selector(
            BeaconGeometry(
                userHeading = 0.0,
                poiBearing = 0.0,
                distance = distance,
                proximityNear = proximityNear
            )
        )

    private fun assetAt(distance: Double, proximityNear: Double = 15.0) =
        selectAt(distance, proximityNear)?.let {
            PROXIMITY_BEACON_TYPE.assets[it.assetIndex]
        }

    @Test
    fun closeAssetInsideProximityNear() {
        assertEquals("Proximity_Close", assetAt(0.0))
        assertEquals("Proximity_Close", assetAt(14.9))
    }

    @Test
    fun farAssetOutToTwiceProximityNear() {
        assertEquals("Proximity_Far", assetAt(15.0))
        assertEquals("Proximity_Far", assetAt(29.9))
    }

    @Test
    fun silentBeyondTwiceProximityNear() {
        assertNull(selectAt(30.0))
        assertNull(selectAt(1000.0))
    }

    @Test
    fun thresholdsScaleWithProximityNear() {
        assertEquals("Proximity_Close", assetAt(19.0, proximityNear = 20.0))
        assertEquals("Proximity_Far", assetAt(35.0, proximityNear = 20.0))
        assertNull(selectAt(41.0, proximityNear = 20.0))
    }

    @Test
    fun directionalBeaconsIgnoreDistance() {
        val type = BEACON_TYPES.getValue("Current")
        val near = type.selector(
            BeaconGeometry(
                userHeading = 0.0,
                poiBearing = 0.0,
                distance = 1.0,
                proximityNear = 15.0
            )
        )
        val far = type.selector(
            BeaconGeometry(
                userHeading = 0.0,
                poiBearing = 0.0,
                distance = 5000.0,
                proximityNear = 15.0
            )
        )
        assertEquals(near, far)
    }
}
