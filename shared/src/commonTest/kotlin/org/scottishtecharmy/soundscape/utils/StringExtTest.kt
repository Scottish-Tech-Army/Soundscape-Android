package org.scottishtecharmy.soundscape.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringExtTest {

    // --- containsNumber ---

    @Test
    fun containsNumber_emptyString_returnsFalse() {
        assertFalse("".containsNumber())
    }

    @Test
    fun containsNumber_blankString_returnsFalse() {
        assertFalse("   ".containsNumber())
    }

    @Test
    fun containsNumber_noDigits_returnsFalse() {
        assertFalse("hello world".containsNumber())
    }

    @Test
    fun containsNumber_wordStartsWithDigit_returnsTrue() {
        assertTrue("5 apples".containsNumber())
    }

    @Test
    fun containsNumber_digitInMiddleOfWord_returnsFalse() {
        // Only words that *start* with a digit count.
        assertFalse("apple5 pie".containsNumber())
    }

    @Test
    fun containsNumber_digitStartingLastWord_returnsTrue() {
        assertTrue("apple pie 5th".containsNumber())
    }

    @Test
    fun containsNumber_multipleSpacesBetweenWords_stillDetectsDigit() {
        // split(" ") produces empty tokens between consecutive spaces; those are skipped.
        assertTrue("a  5".containsNumber())
    }

    @Test
    fun containsNumber_negativeNumberWord_returnsFalse() {
        // "-5" starts with '-', not a digit, so this is not detected.
        assertFalse("-5 apples".containsNumber())
    }

    @Test
    fun containsNumber_wholeStringIsNumber_returnsTrue() {
        assertTrue("42".containsNumber())
    }

    @Test
    fun containsNumber_leadingSpaceBeforeDigitWord_returnsTrue() {
        assertTrue(" 5th avenue".containsNumber())
    }

    // --- fuzzyCompare ---

    @Test
    fun fuzzyCompare_identicalStrings_scoresZero() {
        assertEquals(0.0, "test".fuzzyCompare("test", false))
    }

    @Test
    fun fuzzyCompare_bothEmpty_scoresZero() {
        assertEquals(0.0, "".fuzzyCompare("", false))
    }

    @Test
    fun fuzzyCompare_emptyNeedle_scoresMax() {
        assertEquals(1.0, "".fuzzyCompare("abc", false))
    }

    @Test
    fun fuzzyCompare_emptyHaystack_scoresMax() {
        assertEquals(1.0, "abc".fuzzyCompare("", false))
    }

    @Test
    fun fuzzyCompare_isCaseSensitive() {
        // Every character differs -> full substitution distance.
        assertEquals(1.0, "abc".fuzzyCompare("ABC", false))
    }

    @Test
    fun fuzzyCompare_transposedAdjacentChars_countsAsSingleEdit() {
        // Damerau-Levenshtein: transposition of adjacent chars costs 1, not 2.
        assertEquals(0.5, "ab".fuzzyCompare("ba", false))
    }

    @Test
    fun fuzzyCompare_oneSubstitution_scoresRatio() {
        // "cat" vs "cot": 1 substitution over length 3.
        val score = "cat".fuzzyCompare("cot", false)
        assertEquals(1.0 / 3.0, score)
    }

    @Test
    fun fuzzyCompare_needleCanBeShorter_prefixMatch_scoresNearZero() {
        // Matches the documented "Tesco" / "Tesco Express" example.
        val score = "Tesco".fuzzyCompare("Tesco Express", true)
        assertEquals(0.01, score, 1e-9)
    }

    @Test
    fun fuzzyCompare_needleCanBeShorter_docExample_christine() {
        val score = "Christine".fuzzyCompare("Christine's on the Green", true)
        assertEquals(0.01 + 0.0 / 9.0, score, 1e-9)
    }

    @Test
    fun fuzzyCompare_needleCanBeShorter_sameLengthStrings_noExtraCost() {
        // sameSizeCost is only applied when the haystack was originally longer than the needle.
        assertEquals(0.0, "Westerton".fuzzyCompare("Westerton", true))
    }

    @Test
    fun fuzzyCompare_needleCanBeShorter_prioritizesExactLengthMatch() {
        // An exact-length match should score strictly better than a same-prefix
        // but longer haystack, matching the documented "Westerton" example.
        val exact = "Westerton".fuzzyCompare("Westerton", true)
        val longer = "Westerton".fuzzyCompare("Westerton Vets", true)
        assertTrue(exact < longer)
    }

    @Test
    fun fuzzyCompare_needleCanBeShorter_falseDoesNotTruncateHaystack() {
        // With needleCanBeShorter = false, the full haystack length is used even
        // though it starts with the needle, so the score is much worse.
        val truncated = "Tesco".fuzzyCompare("Tesco Express", true)
        val untruncated = "Tesco".fuzzyCompare("Tesco Express", false)
        assertTrue(untruncated > truncated)
    }

    @Test
    fun fuzzyCompare_needleLongerThanHaystack_needleCanBeShorterHasNoEffect() {
        // needleCanBeShorter only kicks in when the haystack is longer than the needle.
        val withFlag = "Christine's on the Green".fuzzyCompare("Christine", true)
        val withoutFlag = "Christine's on the Green".fuzzyCompare("Christine", false)
        assertEquals(withoutFlag, withFlag)
    }

    @Test
    fun fuzzyCompare_completelyDifferentStrings_scoresMax() {
        assertEquals(1.0, "abc".fuzzyCompare("xyz", false))
    }
}
