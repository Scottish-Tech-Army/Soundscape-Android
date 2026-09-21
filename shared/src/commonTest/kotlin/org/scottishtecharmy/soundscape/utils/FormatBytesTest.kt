package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Records every key/args pair passed to [get] and returns a deterministic, inspectable string
 * so assertions can pin down exactly which StringKey and formatted argument formatBytes used,
 * without depending on real translated copy.
 */
private class FakeLocalizedStrings : LocalizedStrings {
    val calls = mutableListOf<Pair<StringKey, List<Any?>>>()

    override fun get(key: StringKey, vararg args: Any?): String {
        calls.add(key to args.toList())
        return when (key) {
            StringKey.NumberDecimalSeparator -> ","
            StringKey.NumberDecimalSeparatorA11y -> " comma "
            else -> "${key.name}(${args.joinToString(",")})"
        }
    }

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key(${args.joinToString(", ")})"

    override fun resolveFeatureClass(key: String): String? = null
}

class FormatBytesTest {

    // --- No LocalizedStrings supplied (fallback English strings) ---

    @Test
    fun zeroBytes_fallback_short() {
        assertEquals("0 B", formatBytes(0L, null))
    }

    @Test
    fun negativeBytes_fallback_short() {
        // Any negative value is < 1000, so it takes the byte-count branch verbatim.
        assertEquals("-1 B", formatBytes(-1L, null))
    }

    @Test
    fun negativeBytes_largeMagnitude_stillTreatedAsBytes() {
        assertEquals("-5000 B", formatBytes(-5000L, null))
    }

    @Test
    fun bytesJustBelowKbBoundary_fallback_short() {
        assertEquals("999 B", formatBytes(999L, null))
    }

    @Test
    fun exactlyOneKb_fallback_short() {
        assertEquals("1.0 kB", formatBytes(1000L, null))
    }

    @Test
    fun oneAndAHalfKb_fallback_short() {
        assertEquals("1.5 kB", formatBytes(1500L, null))
    }

    @Test
    fun kbValueAtOrAbove100_roundsToInteger() {
        assertEquals("100 kB", formatBytes(100_000L, null))
    }

    @Test
    fun kbValueJustBelow100_keepsOneDecimal() {
        assertEquals("99.9 kB", formatBytes(99_900L, null))
    }

    @Test
    fun exactlyOneMb_fallback_short() {
        assertEquals("1.0 MB", formatBytes(1_000_000L, null))
    }

    @Test
    fun oneAndAHalfMb_fallback_short() {
        assertEquals("1.5 MB", formatBytes(1_500_000L, null))
    }

    @Test
    fun exactlyOneGb_fallback_short() {
        assertEquals("1.0 GB", formatBytes(1_000_000_000L, null))
    }

    @Test
    fun exactlyOneTb_fallback_short() {
        assertEquals("1.0 TB", formatBytes(1_000_000_000_000L, null))
    }

    @Test
    fun beyondTb_clampsAtTbUnit() {
        // There is no unit past TB, so the value keeps growing in the TB slot instead of
        // rolling over to a fifth unit.
        assertEquals("5000 TB", formatBytes(5_000_000_000_000_000L, null))
    }

    @Test
    fun roundingAtUnitBoundary_rollsOverToNextUnit() {
        // 999_500 bytes is 999.5 kB, which rounds to "1000" for display - the unit-selection
        // loop compares the raw (pre-rounding) value, so on its own it would decide the value
        // is still < 1000 kB and never advance to MB. A second check re-examines the rounded
        // display value and advances the unit when rounding alone would have reached 1000,
        // producing "1.0 MB" instead of "1000 kB". Same at every unit boundary.
        assertEquals("1.0 MB", formatBytes(999_500L, null))
        assertEquals("1.0 GB", formatBytes(999_500_000L, null))
    }

    // --- forAccessibility = true, no LocalizedStrings (fallback spelled-out strings) ---

    @Test
    fun zeroBytes_fallback_accessibility() {
        assertEquals("0 bytes", formatBytes(0L, null, forAccessibility = true))
    }

    @Test
    fun oneKb_fallback_accessibility_spellsOutUnitAndSpacesDigits() {
        // formatDecimal is asked to space out fractional digits for accessibility; with only
        // one fractional digit that's a no-op ("0" stays "0"), but the separator itself
        // becomes " point " and the unit is spelled out.
        assertEquals("1 point 0 kilobytes", formatBytes(1000L, null, forAccessibility = true))
    }

    @Test
    fun oneMb_fallback_accessibility() {
        assertEquals(
            "1 point 5 megabytes",
            formatBytes(1_500_000L, null, forAccessibility = true),
        )
    }

    @Test
    fun oneGb_fallback_accessibility() {
        assertEquals(
            "1 point 0 gigabytes",
            formatBytes(1_000_000_000L, null, forAccessibility = true),
        )
    }

    @Test
    fun oneTb_fallback_accessibility() {
        assertEquals(
            "1 point 0 terabytes",
            formatBytes(1_000_000_000_000L, null, forAccessibility = true),
        )
    }

    // --- LocalizedStrings supplied ---

    @Test
    fun bytesBelowKb_usesLocalizedByteKey() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(500L, fake)
        assertEquals("BytesFormatB(500)", result)
        assertEquals(listOf(StringKey.BytesFormatB to listOf<Any?>("500")), fake.calls)
    }

    @Test
    fun bytesBelowKb_accessibility_usesLocalizedA11yByteKey() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(500L, fake, forAccessibility = true)
        assertEquals("BytesFormatBA11y(500)", result)
    }

    @Test
    fun kbValue_usesLocalizedDecimalSeparator() {
        val fake = FakeLocalizedStrings()
        // FakeLocalizedStrings uses a comma decimal separator, like many European locales.
        val result = formatBytes(1500L, fake)
        assertEquals("BytesFormatKb(1,5)", result)
    }

    @Test
    fun kbValue_accessibility_usesLocalizedA11ySeparatorAndUnit() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(1500L, fake, forAccessibility = true)
        assertEquals("BytesFormatKbA11y(1 comma 5)", result)
    }

    @Test
    fun mbValue_usesLocalizedMbKey() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(2_500_000L, fake)
        assertEquals("BytesFormatMb(2,5)", result)
    }

    @Test
    fun gbValue_usesLocalizedGbKey() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(3_000_000_000L, fake)
        assertEquals("BytesFormatGb(3,0)", result)
    }

    @Test
    fun tbValue_usesLocalizedTbKey() {
        val fake = FakeLocalizedStrings()
        val result = formatBytes(4_000_000_000_000L, fake)
        assertEquals("BytesFormatTb(4,0)", result)
    }

    @Test
    fun negativeBytes_withLocalizedStrings_usesByteKeyVerbatim() {
        val fake = FakeLocalizedStrings()
        assertEquals("BytesFormatB(-50)", formatBytes(-50L, fake))
    }
}
