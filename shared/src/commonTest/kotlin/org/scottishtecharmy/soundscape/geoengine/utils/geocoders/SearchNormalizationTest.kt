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
    fun preservesSpacingVowelSigns() {
        // Vowel signs written beside their consonant are spacing combining marks (category Mc).
        // They aren't letters, but they're part of the word, not a gap in it.
        // Devanagari "दिल्ली" (Delhi): ि U+093F and ी U+0940 are spacing, ् U+094D isn't
        assertEquals("दिल्ली", normalizeForSearch("दिल्ली"))
        // Burmese "ကား" (car): ာ U+102C and း U+1038
        assertEquals("ကား", normalizeForSearch("ကား"))
        // Khmer "កា": ា U+17B6
        assertEquals("កា", normalizeForSearch("កា"))
        // Tamil "கா": ா U+0BBE
        assertEquals("கா", normalizeForSearch("கா"))
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

    @Test
    fun foldsFrenchAndSpanishAccents() {
        // Paris, San Salvador and Buenos Aires street names are routinely typed without accents.
        assertEquals("chatelet", normalizeForSearch("Châtelet"))
        assertEquals("rue saint honore", normalizeForSearch("Rue Saint-Honoré"))
        assertEquals("francois", normalizeForSearch("François"))
        assertEquals("avenida espana", normalizeForSearch("Avenida España"))
        assertEquals("pena", normalizeForSearch("Peña"))
    }

    @Test
    fun foldsOeLigature() {
        // NFKD doesn't decompose the œ ligature, but "Sacré-Cœur" is typed "coeur"
        assertEquals("sacre coeur", normalizeForSearch("Sacré-Cœur"))
    }

    @Test
    fun zeroWidthNonJoinerSeparatesPersianWords() {
        // Persian writes the parts of some words with a zero-width non-joiner (U+200C) between
        // them rather than a space, e.g. "بن‌بست" (dead end). It isn't a letter, so it's treated
        // like any other separator - which makes the data match a search typed with a space.
        assertEquals("بن بست", normalizeForSearch("بن‌بست"))
        assertEquals(normalizeForSearch("بن بست"), normalizeForSearch("بن‌بست"))
    }

    @Test
    fun foldsPersianAndArabicIndicDigits() {
        // Persian (U+06F0..U+06F9) and Arabic-Indic (U+0660..U+0669) digits, which are used for
        // house numbers and numbered alleys in Iranian map data, but often typed as ASCII
        assertEquals("پلاک 12", normalizeForSearch("پلاک ۱۲"))
        assertEquals("12", normalizeForSearch("١٢"))
    }

    @Test
    fun foldsFullWidthAndHalfWidthJapaneseInput() {
        // Japanese keyboards can type Latin letters and digits full width, and katakana half width,
        // where the map data has them the other way round
        assertEquals("muji 12", normalizeForSearch("ＭＵＪＩ　１２"))
        assertEquals(normalizeForSearch("ルクア"), normalizeForSearch("ﾙｸｱ"))
        assertEquals(normalizeForSearch("グランフロント"), normalizeForSearch("ｸﾞﾗﾝﾌﾛﾝﾄ"))
    }

    @Test
    fun ideographicSpaceSeparatesWords() {
        // The space bar on a Japanese keyboard types U+3000
        assertEquals(normalizeForSearch("ホテル イビス"), normalizeForSearch("ホテル　イビス"))
    }

    @Test
    fun kanaMatchWhicheverWayTheirVoicingMarkIsWritten() {
        // "グ" can be the one character (U+30B0) or "ク" followed by a combining voicing mark
        // (U+3099), and the two have to match
        assertEquals(normalizeForSearch("グランフロント"), normalizeForSearch("グランフロント"))
    }

    @Test
    fun foldsArabicYehAndKafToPersian() {
        // An Arabic keyboard types yeh, alef maksura and kaf (U+064A, U+0649, U+0643) where
        // Persian has farsi yeh and keheh (U+06CC, U+06A9), and Iranian map data has both.
        // "خیابان" (street)
        assertEquals(normalizeForSearch("خیابان"), normalizeForSearch("خيابان"))
        // "کوچه" (alley)
        assertEquals(normalizeForSearch("کوچه"), normalizeForSearch("كوچه"))
        // "حقوقی" (legal)
        assertEquals(normalizeForSearch("حقوقی"), normalizeForSearch("حقوقى"))
    }
}
