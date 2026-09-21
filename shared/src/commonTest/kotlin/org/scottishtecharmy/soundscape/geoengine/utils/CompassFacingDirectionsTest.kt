package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Minimal [LocalizedStrings] test double that renders a key/args pair into a stable,
 * human-readable string so tests can assert exactly which key (and placeholder args) the
 * production code selected, without needing a real string-resource bundle.
 */
private class FakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String {
        return if (args.isEmpty()) key.name else "${key.name}(${args.joinToString(",")})"
    }

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key(${args.joinToString(", ")})"

    override fun resolveFeatureClass(key: String): String? = null
}

class CompassFacingDirectionsTest {

    private val fake = FakeLocalizedStrings()

    // --- getCompassLabelFacingDirection: null localized (English fallback) ---

    @Test
    fun facingDirectionFallbackWhenStationary() {
        assertEquals("Facing north", getCompassLabelFacingDirection(null, 0, inMotion = false, inVehicle = false))
        assertEquals("Facing northeast", getCompassLabelFacingDirection(null, 45, inMotion = false, inVehicle = false))
        assertEquals("Facing east", getCompassLabelFacingDirection(null, 90, inMotion = false, inVehicle = false))
        assertEquals("Facing southeast", getCompassLabelFacingDirection(null, 135, inMotion = false, inVehicle = false))
        assertEquals("Facing south", getCompassLabelFacingDirection(null, 180, inMotion = false, inVehicle = false))
        assertEquals("Facing southwest", getCompassLabelFacingDirection(null, 225, inMotion = false, inVehicle = false))
        assertEquals("Facing west", getCompassLabelFacingDirection(null, 270, inMotion = false, inVehicle = false))
        assertEquals("Facing northwest", getCompassLabelFacingDirection(null, 315, inMotion = false, inVehicle = false))
    }

    @Test
    fun facingDirectionFallbackWhenWalking() {
        // inMotion = true, inVehicle = false -> "Heading" wording
        assertEquals("Heading north", getCompassLabelFacingDirection(null, 0, inMotion = true, inVehicle = false))
        assertEquals("Heading east", getCompassLabelFacingDirection(null, 90, inMotion = true, inVehicle = false))
        assertEquals("Heading south", getCompassLabelFacingDirection(null, 180, inMotion = true, inVehicle = false))
        assertEquals("Heading west", getCompassLabelFacingDirection(null, 270, inMotion = true, inVehicle = false))
    }

    @Test
    fun facingDirectionFallbackWhenInVehicle() {
        // inMotion = true, inVehicle = true -> "Traveling" wording
        assertEquals("Traveling north", getCompassLabelFacingDirection(null, 0, inMotion = true, inVehicle = true))
        assertEquals("Traveling east", getCompassLabelFacingDirection(null, 90, inMotion = true, inVehicle = true))
        assertEquals("Traveling south", getCompassLabelFacingDirection(null, 180, inMotion = true, inVehicle = true))
        assertEquals("Traveling west", getCompassLabelFacingDirection(null, 270, inMotion = true, inVehicle = true))
    }

    @Test
    fun facingDirectionIgnoresInVehicleWhenNotInMotion() {
        // inVehicle should have no effect while stationary - stationary wording always wins.
        assertEquals("Facing north", getCompassLabelFacingDirection(null, 0, inMotion = false, inVehicle = true))
    }

    @Test
    fun facingDirectionNorthWraparoundBoundaries() {
        assertEquals("Facing north", getCompassLabelFacingDirection(null, 338, inMotion = false, inVehicle = false))
        assertEquals("Facing north", getCompassLabelFacingDirection(null, 360, inMotion = false, inVehicle = false))
        assertEquals("Facing north", getCompassLabelFacingDirection(null, 22, inMotion = false, inVehicle = false))
        assertEquals("Facing northwest", getCompassLabelFacingDirection(null, 337, inMotion = false, inVehicle = false))
        assertEquals("Facing northeast", getCompassLabelFacingDirection(null, 23, inMotion = false, inVehicle = false))
    }

    @Test
    fun facingDirectionHandlesNegativeDegrees() {
        // -10 normalizes to 350 -> north octant
        assertEquals("Facing north", getCompassLabelFacingDirection(null, -10, inMotion = false, inVehicle = false))
    }

    // --- getCompassLabelFacingDirection: localized lookups select the right key ---

    @Test
    fun facingDirectionLocalizedKeysWhenStationary() {
        assertEquals(
            StringKey.DirectionsFacingN.name,
            getCompassLabelFacingDirection(fake, 0, inMotion = false, inVehicle = false),
        )
        assertEquals(
            StringKey.DirectionsFacingNE.name,
            getCompassLabelFacingDirection(fake, 45, inMotion = false, inVehicle = false),
        )
        assertEquals(
            StringKey.DirectionsFacingSW.name,
            getCompassLabelFacingDirection(fake, 225, inMotion = false, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionLocalizedKeysWhenWalking() {
        assertEquals(
            StringKey.DirectionsHeadingE.name,
            getCompassLabelFacingDirection(fake, 90, inMotion = true, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionLocalizedKeysWhenInVehicle() {
        assertEquals(
            StringKey.DirectionsTravelingS.name,
            getCompassLabelFacingDirection(fake, 180, inMotion = true, inVehicle = true),
        )
    }

    // --- getCompassLabelFacingDirectionAlong: null localized (English fallback) ---

    @Test
    fun facingDirectionAlongFallbackWhenStationary() {
        assertEquals(
            "Facing north along Main Street",
            getCompassLabelFacingDirectionAlong(null, 0, "Main Street", inMotion = false, inVehicle = false),
        )
        assertEquals(
            "Facing southeast along Main Street",
            getCompassLabelFacingDirectionAlong(null, 135, "Main Street", inMotion = false, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionAlongFallbackWhenWalking() {
        assertEquals(
            "Heading west along the path",
            getCompassLabelFacingDirectionAlong(null, 270, "the path", inMotion = true, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionAlongFallbackWhenInVehicle() {
        assertEquals(
            "Traveling northwest along the highway",
            getCompassLabelFacingDirectionAlong(null, 315, "the highway", inMotion = true, inVehicle = true),
        )
    }

    @Test
    fun facingDirectionAlongWraparoundBoundaries() {
        assertEquals(
            "Facing north along X",
            getCompassLabelFacingDirectionAlong(null, 360, "X", inMotion = false, inVehicle = false),
        )
        assertEquals(
            "Facing north along X",
            getCompassLabelFacingDirectionAlong(null, 0, "X", inMotion = false, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionAlongHandlesNegativeDegrees() {
        // -45 normalizes to 315 -> northwest octant
        assertEquals(
            "Facing northwest along X",
            getCompassLabelFacingDirectionAlong(null, -45, "X", inMotion = false, inVehicle = false),
        )
    }

    // --- getCompassLabelFacingDirectionAlong: localized lookups pass through the key and placeholder ---

    @Test
    fun facingDirectionAlongLocalizedKeyAndPlaceholderWhenStationary() {
        assertEquals(
            "${StringKey.DirectionsAlongFacingN.name}(Main Street)",
            getCompassLabelFacingDirectionAlong(fake, 0, "Main Street", inMotion = false, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionAlongLocalizedKeyAndPlaceholderWhenWalking() {
        assertEquals(
            "${StringKey.DirectionsAlongHeadingE.name}(the path)",
            getCompassLabelFacingDirectionAlong(fake, 90, "the path", inMotion = true, inVehicle = false),
        )
    }

    @Test
    fun facingDirectionAlongLocalizedKeyAndPlaceholderWhenInVehicle() {
        assertEquals(
            "${StringKey.DirectionsAlongTravelingSW.name}(the highway)",
            getCompassLabelFacingDirectionAlong(fake, 225, "the highway", inMotion = true, inVehicle = true),
        )
    }
}
