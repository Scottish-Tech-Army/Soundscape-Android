package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchNormalizationTest {

    @Test
    fun stripsLatinCombiningDiacritics() {
        assertEquals("cafe", normalizeForSearch("café"))
        assertEquals("resume", normalizeForSearch("résumé"))
        assertEquals("uber", normalizeForSearch("Über"))
    }

    @Test
    fun stripsApostropheVariants() {
        // Straight apostrophe (U+0027)
        assertEquals("obrien", normalizeForSearch("O'Brien"))
        // Right single quotation mark (U+2019) - the typical "curly" apostrophe
        assertEquals("obrien", normalizeForSearch("O’Brien"))
        // Left single quotation mark (U+2018)
        assertEquals("obrien", normalizeForSearch("O‘Brien"))
        // Single low-9 quotation mark (U+201B)
        assertEquals("obrien", normalizeForSearch("O‛Brien"))
        // Modifier letter turned comma / apostrophe (U+02BB, U+02BC, U+02B9)
        assertEquals("obrien", normalizeForSearch("OʻBrien"))
        assertEquals("obrien", normalizeForSearch("OʼBrien"))
        assertEquals("obrien", normalizeForSearch("OʹBrien"))
        // Latin small letter saltillo (U+A78C)
        assertEquals("obrien", normalizeForSearch("OꞌBrien"))
        // Fullwidth apostrophe (U+FF07)
        assertEquals("obrien", normalizeForSearch("O＇Brien"))
    }

    @Test
    fun lowercasesMixedCaseInput() {
        assertEquals("mixed case input", normalizeForSearch("MiXeD CaSe InPuT"))
    }

    @Test
    fun collapsesAndTrimsWhitespace() {
        assertEquals("hello world", normalizeForSearch("  hello   world  "))
        assertEquals("hello world", normalizeForSearch("\thello\nworld\r"))
        assertEquals("", normalizeForSearch("     "))
    }

    @Test
    fun preservesDevanagariCombiningMarks() {
        // U+0915 (DEVANAGARI LETTER KA) + U+094D (DEVANAGARI SIGN VIRAMA, category Mn).
        // The virama is essential to the word's meaning, unlike a Latin accent, and must
        // NOT be stripped even though it is a NON_SPACING_MARK like the Latin diacritics are.
        val input = "क्"
        assertEquals(input, normalizeForSearch(input))
    }

    @Test
    fun preservesHebrewCombiningMarks() {
        // U+05D0 (HEBREW LETTER ALEF) + U+05B8 (HEBREW POINT QAMATS, category Mn).
        val input = "אָ"
        assertEquals(input, normalizeForSearch(input))
    }

    @Test
    fun preservesDigits() {
        assertEquals("abc123", normalizeForSearch("abc123"))
        assertEquals("route 66", normalizeForSearch("Route 66"))
    }

    @Test
    fun emptyStringReturnsEmptyString() {
        assertEquals("", normalizeForSearch(""))
    }
}
