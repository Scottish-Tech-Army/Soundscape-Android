package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

expect fun normalizeUnicode(input: String): String

private val apostrophes = setOf('\'', '\u2018', '\u2019', '\u201B', '\u02BB', '\u02BC', '\u02B9', '\uA78C', '\uFF07')

fun normalizeForSearch(input: String): String {
    val nfkd = normalizeUnicode(input)

    val sb = StringBuilder(nfkd.length)
    var lastWasSpace = false

    for (ch in nfkd) {
        if (ch.category == CharCategory.NON_SPACING_MARK) continue
        if (ch in apostrophes) continue

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
