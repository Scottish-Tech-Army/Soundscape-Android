package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renders as `Key[quantity](args)` so the assertions below can pin down all three things that
 * matter without depending on real translated copy: which unit was chosen, which plural form the
 * quantity would select, and the number the user actually hears. The quantity and the number are
 * deliberately separate - "1.4 km" is quantity 2.
 */
private class FakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String = when (key) {
        StringKey.NumberDecimalSeparator -> "."
        StringKey.NumberDecimalSeparatorA11y -> " point "
        else -> "${key.name}(${args.joinToString(",")})"
    }

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key[$quantity](${args.joinToString(",")})"

    override fun resolveFeatureClass(key: String): String? = null
}

/**
 * Distances are read out aloud, so the aim throughout is the fewest syllables that still tell the
 * user what they need - see the rounding table on [metric].
 */
class FormatDistanceTest {

    private val localized = FakeLocalizedStrings()

    private fun format(distance: Double, speed: Double = 0.0) =
        formatDistanceAndDirection(distance, null, localized, speed = speed)

    private fun formatForA11y(distance: Double, speed: Double = 0.0) =
        formatDistanceAndDirection(
            distance, null, localized, forAccessibility = true, speed = speed
        )

    @AfterTest
    fun restoreUnits() {
        metric = true
    }

    @Test
    fun smallDistancesKeepFiveUnitPrecision() {
        assertEquals("DistanceMeters[20](20)", format(21.0))
        assertEquals("DistanceMeters[45](45)", format(44.0))
        assertEquals("DistanceMeters[95](95)", format(96.0))
    }

    /**
     * From 100 up there's no unit digit left to read out - "110 metres", never "114 metres".
     */
    @Test
    fun distancesOverOneHundredRoundToTheNearestTen() {
        assertEquals("DistanceMeters[110](110)", format(114.0))
        assertEquals("DistanceMeters[120](120)", format(117.0))
        assertEquals("DistanceMeters[950](950)", format(953.0))
    }

    @Test
    fun bigUnitsUnderTenGetOneDecimalPlace() {
        assertEquals("DistanceKm[2](1.4)", format(1400.0))
        assertEquals("DistanceKm[2](3.5)", format(3456.0))
        assertEquals("DistanceKm[2](9.9)", format(9876.0))
    }

    /**
     * There's nothing for a decimal place to say about "1.0 km" that "1 km" doesn't, so an exact
     * number of big units drops it even below the whole-unit threshold.
     */
    @Test
    fun wholeBigUnitsDropTheDecimalPlace() {
        assertEquals("DistanceKm[1](1)", format(1000.0))
        assertEquals("DistanceKm[2](2)", format(2010.0))
        assertEquals("DistanceKm[9](9)", format(8970.0))
    }

    /**
     * A tenth of a kilometre is noise at 10km and beyond, and "twelve point three" is three words
     * where "twelve" would do.
     */
    @Test
    fun bigUnitsOverTenRoundToWholeUnits() {
        assertEquals("DistanceKm[12](12)", format(12345.0))
        assertEquals("DistanceKm[43](43)", format(43210.0))
    }

    /**
     * 9.96km would otherwise be formatted to one decimal place as "10.0", which is exactly the
     * "point zero" the whole-unit rounding exists to avoid.
     */
    @Test
    fun bigUnitsRoundingUpToTenDropTheDecimalPlace() {
        assertEquals("DistanceKm[10](10)", format(9960.0))
    }

    @Test
    fun aboveThirtyMilesPerHourEverythingIsInBigUnits() {
        val fast = UserGeometry.BIG_UNIT_SPEED_THRESHOLD_MPS + 1.0
        assertEquals("DistanceKm[2](0.2)", format(155.0, speed = fast))
        assertEquals("DistanceKm[2](0.8)", format(800.0, speed = fast))
    }

    @Test
    fun belowThirtyMilesPerHourShortDistancesStayInSmallUnits() {
        val slow = UserGeometry.BIG_UNIT_SPEED_THRESHOLD_MPS - 1.0
        assertEquals("DistanceMeters[160](160)", format(155.0, speed = slow))
    }

    /**
     * "0.0 kilometres" tells the user nothing, so anything too close to express in big units
     * stays in small ones however fast we're going.
     */
    @Test
    fun distancesTooShortForBigUnitsStayInSmallUnitsAtSpeed() {
        assertEquals("DistanceMeters[30](30)", format(30.0, speed = 30.0))
    }

    /**
     * Accessibility strings are read by a screen reader rather than the callout voice, so they
     * spell the decimal point out ("3 point 5") and use the separate DistanceKmA11y key -
     * but the rounding they're given is exactly the same.
     */
    @Test
    fun accessibilityBigUnitsSpellOutTheDecimalPoint() {
        assertEquals("DistanceKmA11y[2](3 point 5)", formatForA11y(3456.0))
        assertEquals("DistanceKmA11y[2](1 point 4)", formatForA11y(1400.0))
    }

    /**
     * Whole big units have no decimal point to spell out, so the a11y string is just the number -
     * "1 kilometres", never "1 point 0 kilometres".
     */
    @Test
    fun accessibilityWholeBigUnitsHaveNoDecimalPoint() {
        assertEquals("DistanceKmA11y[1](1)", formatForA11y(1000.0))
        assertEquals("DistanceKmA11y[12](12)", formatForA11y(12345.0))
        assertEquals("DistanceKmA11y[10](10)", formatForA11y(9960.0))
    }

    /**
     * Small units are whole numbers either way, so accessibility mode changes nothing about them.
     */
    @Test
    fun accessibilitySmallUnitsAreUnchanged() {
        assertEquals("DistanceMeters[110](110)", formatForA11y(114.0))
        assertEquals("DistanceMeters[45](45)", formatForA11y(44.0))
    }

    @Test
    fun accessibilityHonoursTheBigUnitSpeedThreshold() {
        val fast = UserGeometry.BIG_UNIT_SPEED_THRESHOLD_MPS + 1.0
        assertEquals("DistanceKmA11y[2](0 point 2)", formatForA11y(155.0, speed = fast))
        assertEquals("DistanceMeters[160](160)", formatForA11y(155.0))
    }

    /**
     * Imperial has no separate a11y key (there's no "km"/"kilometres" abbreviation problem to
     * solve for "miles"), but it still gets the spelt-out decimal point.
     */
    @Test
    fun accessibilityImperialSpellsOutTheDecimalPoint() {
        metric = false
        assertEquals("DistanceMiles[2](3 point 1)", formatForA11y(5000.0))
        assertEquals("DistanceMiles[12](12)", formatForA11y(20000.0))
        assertEquals("DistanceFeet[110](110)", formatForA11y(35.0))
    }

    /**
     * The only distance that can ever be singular is an exact 1 big unit - small units are
     * rounded to the nearest 5 or 10, so "1 metre" is unreachable, and a fraction is never
     * singular in any language.
     */
    @Test
    fun onlyExactlyOneBigUnitSelectsTheSingularForm() {
        assertEquals("DistanceKm[1](1)", format(1000.0))
        assertEquals("DistanceKm[2](1.1)", format(1100.0))
        assertEquals("DistanceKm[2](2)", format(2000.0))
        metric = false
        assertEquals("DistanceMiles[1](1)", format(1609.0))
    }

    @Test
    fun imperialDistancesRoundTheSameWay() {
        metric = false
        // 35m is 114.8 feet, so the nearest ten feet is 110.
        assertEquals("DistanceFeet[110](110)", format(35.0))
        // 20000m is 12.4 miles, past the point where the decimal place is dropped.
        assertEquals("DistanceMiles[12](12)", format(20000.0))
        // 5000m is 3.1 miles.
        assertEquals("DistanceMiles[2](3.1)", format(5000.0))
    }
}
