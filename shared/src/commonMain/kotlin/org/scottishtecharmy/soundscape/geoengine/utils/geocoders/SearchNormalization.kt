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

fun normalizeForSearch(input: String): String {
    val nfkd = normalizeUnicode(input)

    val sb = StringBuilder(nfkd.length)
    var lastWasSpace = false

    for (ch in nfkd) {
        if (ch.category == CharCategory.NON_SPACING_MARK) {
            // Only strip Latin/Greek/Cyrillic combining diacritics, which is what NFKD produces
            // for accented letters (e.g. é -> e + U+0301). Combining marks from other scripts
            // (Devanagari matras/virama, Arabic harakat, Hebrew niqqud, etc.) are semantically
            // essential, not decorative, so keep them verbatim.
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
