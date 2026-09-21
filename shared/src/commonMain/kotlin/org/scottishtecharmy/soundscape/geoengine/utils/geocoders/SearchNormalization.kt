package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

expect fun normalizeUnicode(input: String): String

// normalizeForSearch runs over every string in the tiles around the user whenever the search needs
// them, so it keeps what it does per character cheap: ASCII - most of the characters there are, even
// in names in other scripts - takes a short path, and the lookups are `when`s, which don't box the
// Char the way looking it up in a Set or Map would.

private fun isApostrophe(ch: Char) = when (ch) {
    '\'', '‘', '’', '‛', 'ʻ', 'ʼ', 'ʹ', 'ꞌ', '＇' -> true
    else -> false
}

// Letters which are spelt more than one way and which NFKD leaves alone, folded to the way they're
// most often typed:
// - Arabic yeh, alef maksura and kaf (ي ى ك) are what an Arabic keyboard types where Persian has
//   farsi yeh and keheh (ی ک), and Iranian map data has a mixture of both. Teh marbuta (ة) and
//   Urdu's heh goal (ہ) are often typed as heh (ه), and alef wasla (ٱ) as alef.
// - The ligatures are routinely typed as two letters, and German's ß as ss.
// - To NFKD, the Latin letters with a stroke or a hook - ø, ł, đ, Turkish's dotless ı, Hausa's
//   ɓ ɗ ƙ ƴ - and Icelandic's eth and thorn aren't an accent on a letter, but they're typed as the
//   letter they look like on a keyboard without them.
// - Greek's final sigma (ς) is the σ that an upper case Σ lowercases to.
private fun foldedLetter(ch: Char): String? = when (ch) {
    'ي', 'ى' -> "ی"
    'ك' -> "ک"
    'ة', 'ہ' -> "ه"
    'ٱ' -> "ا"
    'œ', 'Œ' -> "oe"
    'æ', 'Æ' -> "ae"
    'ß', 'ẞ' -> "ss"
    'ø', 'Ø' -> "o"
    'ł', 'Ł' -> "l"
    'đ', 'Đ', 'ð', 'Ð' -> "d"
    'þ', 'Þ' -> "th"
    'ı' -> "i"
    'ɓ', 'Ɓ' -> "b"
    'ɗ', 'Ɗ' -> "d"
    'ƙ', 'Ƙ' -> "k"
    'ƴ', 'Ƴ' -> "y"
    'ς' -> "σ"
    else -> null
}

// Invisible characters which change how the letters around them are drawn, but not what's written:
// the Arabic tatweel, which draws out the join between two letters, and the zero width joiner,
// which picks out a way of joining letters in Hindi, Marathi and Bengali, as in कार्‍यालय
private const val TATWEEL = 'ـ'
private const val ZERO_WIDTH_JOINER = '‍'
private const val ZERO_WIDTH_NON_JOINER = '‌'

private fun isArabicScript(ch: Char) =
    (ch.code in 0x0600..0x06FF) || (ch.code in 0x0750..0x077F) ||
        (ch.code in 0xFB50..0xFDFF) || (ch.code in 0xFE70..0xFEFF)

/** The code point at [index] in [string], joining a surrogate pair into the character it encodes. */
internal fun codePointAt(string: String, index: Int): Int {
    val high = string[index]
    if (high.isHighSurrogate() && (index + 1 < string.length)) {
        val low = string[index + 1]
        if (low.isLowSurrogate()) return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
    }
    return high.code
}

/** The code point just before [index] in [string], joining a surrogate pair into the character it encodes. */
internal fun codePointBefore(string: String, index: Int): Int {
    val low = string[index - 1]
    if (low.isLowSurrogate() && (index >= 2)) {
        val high = string[index - 2]
        if (high.isHighSurrogate()) return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
    }
    return low.code
}

// Musical symbols, emoji and the other pictographs, playing cards and mahjong tiles, and the tags
// that follow some emoji - the characters beyond U+FFFF that are no more part of a word than the
// symbols before it are
private fun isSymbolBeyondBmp(codePoint: Int) =
    (codePoint in 0x1D000..0x1D24F) || (codePoint in 0x1F000..0x1FAFF) || (codePoint in 0xE0000..0xE007F)

private fun isCombiningMark(ch: Char) = when (ch.category) {
    CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK, CharCategory.ENCLOSING_MARK -> true
    else -> false
}

fun normalizeForSearch(input: String): String {
    // NFKD leaves ASCII as it is, and seeing that a string is all ASCII costs less than asking the
    // platform to normalize it
    val nfkd = if (input.all { it.code < 0x80 }) input else normalizeUnicode(input)

    val sb = StringBuilder(nfkd.length)
    var lastWasSpace = false
    var hasLetterBeyondBmp = false

    fun appendSpace() {
        if (!lastWasSpace) {
            sb.append(' ')
            lastWasSpace = true
        }
    }

    // Catalan's l·l, which is also typed as l.l, is a double l
    fun isBetweenCatalanDoubleL(index: Int) =
        (sb.lastOrNull() == 'l') && (index + 1 < nfkd.length) && (nfkd[index + 1].lowercaseChar() == 'l')

    var skip = 0
    for ((index, ch) in nfkd.withIndex()) {
        if (skip > 0) {
            skip--
            continue
        }

        // ASCII needs nothing of what follows beyond lowercasing its letters, taking its apostrophe
        // out, and taking anything else for a gap between words
        if (ch.code < 0x80) {
            when {
                ch.isLetterOrDigit() -> {
                    sb.append(ch.lowercaseChar())
                    lastWasSpace = false
                }
                (ch == '\'') || ((ch == '.') && isBetweenCatalanDoubleL(index)) -> {}
                else -> appendSpace()
            }
            continue
        }

        // A character beyond U+FFFF - a rarer CJK ideograph like the 𩸽 of hokke, or a letter from
        // one of the scripts added to Unicode later - is a pair of surrogates in a String, and
        // neither half of the pair is a letter on its own
        if (ch.isHighSurrogate() && (index + 1 < nfkd.length) && nfkd[index + 1].isLowSurrogate()) {
            skip = 1
            val codePoint = codePointAt(nfkd, index)
            when {
                codePoint in 0xE0100..0xE01EF -> {} // A variation selector, dropped like those below
                isSymbolBeyondBmp(codePoint) -> appendSpace()
                else -> {
                    sb.append(ch)
                    sb.append(nfkd[index + 1])
                    lastWasSpace = false
                    hasLetterBeyondBmp = true
                }
            }
            continue
        }

        // NFKD takes a Hangul syllable apart into its letters. Put it back together, so that a
        // syllable is one character, as it is when it's typed - for comparing, and for telling where
        // a word in it could start.
        if ((ch.code in 0x1100..0x1112) && (index + 1 < nfkd.length) && (nfkd[index + 1].code in 0x1161..0x1175)) {
            val vowel = nfkd[index + 1].code - 0x1161
            val finalCode = if (index + 2 < nfkd.length) nfkd[index + 2].code else 0
            val final = if (finalCode in 0x11A8..0x11C2) finalCode - 0x11A7 else 0
            sb.append((0xAC00 + (((ch.code - 0x1100) * 21) + vowel) * 28 + final).toChar())
            lastWasSpace = false
            skip = if (final > 0) 2 else 1
            continue
        }

        // A variation selector picks out one way of drawing the character before it, which the
        // same character drawn the other way has to match - and like the tatweel and the joiner,
        // it isn't part of what's written
        if ((ch.code in 0xFE00..0xFE0F) || (ch == TATWEEL) || (ch == ZERO_WIDTH_JOINER)) continue

        // The zero width non-joiner separates the parts of a Persian word, as in "بن‌بست", where
        // it's typed as a space as often as not - so there it counts as one. Elsewhere, in Hindi,
        // Marathi and Bengali, it picks out a way of drawing a letter as the joiner does, and goes.
        if (ch == ZERO_WIDTH_NON_JOINER) {
            val before = sb.lastOrNull()
            val after = if (index + 1 < nfkd.length) nfkd[index + 1] else null
            if ((before != null) && (after != null) && isArabicScript(before) && isArabicScript(after))
                appendSpace()
            continue
        }

        if (isCombiningMark(ch)) {
            // Strip the Latin/Greek/Cyrillic combining diacritics, which is what NFKD produces for
            // accented letters (e.g. é -> e + U+0301). Arabic's vowel marks (U+064B..U+0652) are
            // hardly ever typed, and the hamza and madda which NFKD takes off an alef (أ إ آ,
            // U+0653..U+0655) are as often left off, so they go too. Combining marks from other
            // scripts (Devanagari matras and virama, Hebrew niqqud, etc.) are essential, not
            // decorative, so keep them verbatim. That includes the spacing ones - the vowel signs
            // written beside their consonant, like Devanagari ा, Burmese ာ and Khmer ា - which
            // aren't letters, and so would otherwise be taken for a gap between words.
            if ((ch.code in 0x0300..0x036F) || (ch.code in 0x064B..0x0655)) continue
            sb.append(ch)
            lastWasSpace = false
            continue
        }
        if (isApostrophe(ch) || ((ch == '·') && isBetweenCatalanDoubleL(index))) continue

        // Hiragana and katakana spell the same sounds, and a name written in one is looked for in
        // the other, so katakana become hiragana
        if ((ch.code in 0x30A1..0x30F6) || (ch.code in 0x30FD..0x30FE)) {
            sb.append((ch.code - 0x60).toChar())
            lastWasSpace = false
            continue
        }

        // Digits from any script, e.g. Persian ۱۲, become ASCII so that a number matches however
        // it was typed or mapped
        val folded = if (ch.isDigit()) ('0' + ch.digitToInt()).toString() else foldedLetter(ch)
        if (folded != null) {
            sb.append(folded)
            lastWasSpace = false
            continue
        }

        if (ch.isLetterOrDigit()) {
            sb.append(ch.lowercaseChar())
            lastWasSpace = false
        } else {
            appendSpace()
        }
    }

    // Every letter has been lowercased as it went in, apart from those beyond U+FFFF, which only the
    // whole string can be
    val normalized = sb.toString().trim()
    return if (hasLetterBeyondBmp) normalized.lowercase() else normalized
}
