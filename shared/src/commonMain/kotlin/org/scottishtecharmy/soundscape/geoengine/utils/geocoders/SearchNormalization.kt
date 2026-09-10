package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

expect fun normalizeUnicode(input: String): String

private val apostrophes =
    setOf('\'', '\u2018', '\u2019', '\u201B', '\u02BB', '\u02BC', '\u02B9', '\uA78C', '\uFF07')

// Letters which are spelt more than one way and which NFKD leaves alone. Arabic yeh, alef maksura
// and kaf (ي ى ك) are what an Arabic keyboard types where Persian has farsi yeh and keheh (ی ک),
// and Iranian map data has a mixture of both. The ligatures are routinely typed as two letters.
private val foldedLetters = mapOf(
    'ي' to "ی",
    'ى' to "ی",
    'ك' to "ک",
    'œ' to "oe", 'Œ' to "oe",
    'æ' to "ae", 'Æ' to "ae",
)

/** The code point at [index] in [string], joining a surrogate pair into the character it encodes. */
internal fun codePointAt(string: String, index: Int): Int {
    val high = string[index]
    if (high.isHighSurrogate() && (index + 1 < string.length)) {
        val low = string[index + 1]
        if (low.isLowSurrogate()) return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
    }
    return high.code
}

// Musical symbols, emoji and the other pictographs, playing cards and mahjong tiles, and the tags
// that follow some emoji - the characters beyond U+FFFF that are no more part of a word than the
// symbols before it are
private fun isSymbolBeyondBmp(codePoint: Int) =
    (codePoint in 0x1D000..0x1D24F) || (codePoint in 0x1F000..0x1FAFF) || (codePoint in 0xE0000..0xE007F)

private val combiningMarks = setOf(
    CharCategory.NON_SPACING_MARK,
    CharCategory.COMBINING_SPACING_MARK,
    CharCategory.ENCLOSING_MARK,
)

fun normalizeForSearch(input: String): String {
    val nfkd = normalizeUnicode(input)

    val sb = StringBuilder(nfkd.length)
    var lastWasSpace = false

    var skipNext = false
    for ((index, ch) in nfkd.withIndex()) {
        if (skipNext) {
            skipNext = false
            continue
        }

        // A character beyond U+FFFF - a rarer CJK ideograph like the 𩸽 of hokke, or a letter from
        // one of the scripts added to Unicode later - is a pair of surrogates in a String, and
        // neither half of the pair is a letter on its own
        if (ch.isHighSurrogate() && (index + 1 < nfkd.length) && nfkd[index + 1].isLowSurrogate()) {
            skipNext = true
            val codePoint = codePointAt(nfkd, index)
            when {
                codePoint in 0xE0100..0xE01EF -> {} // A variation selector, dropped like those below
                isSymbolBeyondBmp(codePoint) -> {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                }
                else -> {
                    sb.append(ch)
                    sb.append(nfkd[index + 1])
                    lastWasSpace = false
                }
            }
            continue
        }

        // A variation selector picks out one way of drawing the character before it, which the
        // same character drawn the other way has to match
        if (ch.code in 0xFE00..0xFE0F) continue

        if (ch.category in combiningMarks) {
            // Only strip Latin/Greek/Cyrillic combining diacritics, which is what NFKD produces
            // for accented letters (e.g. é -> e + U+0301). Combining marks from other scripts
            // (Devanagari matras/virama, Arabic harakat, Hebrew niqqud, etc.) are semantically
            // essential, not decorative, so keep them verbatim. That includes the spacing ones -
            // the vowel signs written beside their consonant, like Devanagari ा, Burmese ာ and
            // Khmer ា - which aren't letters, and so would otherwise be taken for a gap between
            // words.
            if (ch.code in 0x0300..0x036F) continue
            sb.append(ch)
            lastWasSpace = false
            continue
        }
        if (ch in apostrophes) continue

        // Digits from any script, e.g. Persian ۱۲, become ASCII so that a number matches however
        // it was typed or mapped
        val folded = if (ch.isDigit()) ('0' + ch.digitToInt()).toString() else foldedLetters[ch]
        if (folded != null) {
            sb.append(folded)
            lastWasSpace = false
            continue
        }

        val isLetterOrDigit = ch.isLetterOrDigit()
        val outCh = when {
            isLetterOrDigit -> ch.lowercaseChar()
            ch.isWhitespace() -> ' '
            else -> ' '
        }

        if (outCh == ' ') {
            if (!lastWasSpace) {
                sb.append(' ')
                lastWasSpace = true
            }
        } else {
            sb.append(outCh)
            lastWasSpace = false
        }
    }

    return sb.toString().trim().lowercase()
}
