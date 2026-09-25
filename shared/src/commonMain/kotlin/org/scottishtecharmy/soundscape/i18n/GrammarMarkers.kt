package org.scottishtecharmy.soundscape.i18n

/**
 * Resolves the "either form" markers that translators write when a word's form depends on the
 * text a placeholder is replaced with, e.g. Hungarian «a(z) %1$s» or Korean «%1$s을(를)».
 *
 * A translation can't pick the right form because it doesn't know the street or place name that
 * will be filled in, so it writes both. On screen that's merely awkward, but a screen reader
 * reads the brackets out, so this runs on the formatted text, where the name is known, and keeps
 * only the form that fits.
 *
 * The markers only occur in the languages that use them, so this needs no language check and
 * leaves every other language untouched.
 */
fun resolveGrammarMarkers(text: String): String {
    if ('(' !in text) return text
    return resolveKoreanParticles(resolveHungarianArticles(text))
}

// ---------------------------------------------------------------------------------------------
// Hungarian: «a(z)» becomes «az» before a vowel sound and «a» otherwise.
// ---------------------------------------------------------------------------------------------

private val hungarianArticle = Regex("([Aa])\\(z\\)")
private const val HUNGARIAN_VOWELS = "aáeéiíoóöőuúüű"

// Letters whose Hungarian names start with a vowel sound (ef, el, em, en, er, es, iksz,
// ipszilon), which is how an abbreviation or road number such as «M7» is read.
private const val HUNGARIAN_VOWEL_LETTER_NAMES = "AÁEÉIÍOÓÖŐUÚÜŰFLMNRSXY"

// Punctuation that can sit between the article and the word it belongs to.
private const val OPENING_PUNCTUATION = "\"'„“”‚‘’«»([{"

internal fun resolveHungarianArticles(text: String): String =
    hungarianArticle.replace(text) { match ->
        val start = match.range.first
        if (start > 0 && text[start - 1].isLetter()) return@replace match.value
        val article = match.groupValues[1]
        if (takesAz(text, match.range.last + 1)) article + "z" else article
    }

private fun takesAz(text: String, from: Int): Boolean {
    var i = from
    while (i < text.length && (text[i].isWhitespace() || text[i] in OPENING_PUNCTUATION)) i++
    if (i >= text.length) return false
    val first = text[i]
    return when {
        first.isDigit() -> hungarianNumberStartsWithVowel(text.drop(i).takeWhile { it.isDigit() })
        !first.isLetter() -> false
        // An abbreviation, a lone letter or a road number is read letter by letter.
        first.isUpperCase() && text.getOrNull(i + 1)?.isLowerCase() != true ->
            first in HUNGARIAN_VOWEL_LETTER_NAMES
        else -> first.lowercaseChar() in HUNGARIAN_VOWELS
    }
}

/**
 * Whether a number's spoken form starts with a vowel: egy (1), ezer (1000), egymillió, and
 * everything starting öt (5, 50-59, 500-599, 5000...). Ten to nineteen (tíz, tizen-) and the
 * hundreds (száz) don't, so only the leading group of three digits matters.
 */
private fun hungarianNumberStartsWithVowel(digits: String): Boolean {
    if (digits.isEmpty() || digits[0] == '0') return false
    val leadingGroup = digits.take((digits.length - 1) % 3 + 1)
    return leadingGroup == "1" || leadingGroup[0] == '5'
}

// ---------------------------------------------------------------------------------------------
// Korean: particles whose form depends on whether the preceding syllable ends in a consonant.
// ---------------------------------------------------------------------------------------------

/** One marker form and the particle it becomes after a consonant or a vowel. */
private class KoreanParticle(
    val afterConsonant: String,
    val afterVowel: String,
    val afterRieul: String = afterConsonant,
    val fallback: String,
)

// Each marker falls back to the form written outside the brackets when the preceding text gives
// no clue (a symbol, Hanja, or no text at all), which is the form the translator wrote first.
private val koreanParticles = mapOf(
    "을(를)" to KoreanParticle("을", "를", fallback = "을"),
    "를(을)" to KoreanParticle("을", "를", fallback = "를"),
    "이(가)" to KoreanParticle("이", "가", fallback = "이"),
    "가(이)" to KoreanParticle("이", "가", fallback = "가"),
    "은(는)" to KoreanParticle("은", "는", fallback = "은"),
    "는(은)" to KoreanParticle("은", "는", fallback = "는"),
    "과(와)" to KoreanParticle("과", "와", fallback = "과"),
    "와(과)" to KoreanParticle("과", "와", fallback = "와"),
    // 로 follows a vowel and also ㄹ: 서울로, not 서울으로.
    "(으)로" to KoreanParticle("으로", "로", afterRieul = "로", fallback = "로"),
    // The copula, as in (이)라는 / (이)나 / (이)며: 이 after a consonant, dropped after a vowel.
    "(이)" to KoreanParticle("이", "", fallback = ""),
)

private val koreanMarker = Regex(koreanParticles.keys.joinToString("|") { Regex.escape(it) })

private enum class FinalSound { Vowel, Consonant, Rieul }

internal fun resolveKoreanParticles(text: String): String =
    koreanMarker.replace(text) { match ->
        val particle = koreanParticles.getValue(match.value)
        when (finalSoundBefore(text, match.range.first)) {
            FinalSound.Vowel -> particle.afterVowel
            FinalSound.Consonant -> particle.afterConsonant
            FinalSound.Rieul -> particle.afterRieul
            null -> particle.fallback
        }
    }

private const val CLOSING_PUNCTUATION = "\"'”’»)]}」』〉》"

private fun finalSoundBefore(text: String, end: Int): FinalSound? {
    var i = end - 1
    while (i >= 0 && text[i] in CLOSING_PUNCTUATION) i--
    if (i < 0) return null
    val last = text[i]
    return when {
        last in '가'..'힣' -> when ((last - '가') % 28) {
            0 -> FinalSound.Vowel
            8 -> FinalSound.Rieul
            else -> FinalSound.Consonant
        }
        last.isDigit() -> koreanNumberFinalSound(text.substring(0, i + 1).takeLastWhile { it.isDigit() })
        last in 'A'..'Z' || last in 'a'..'z' ->
            latinFinalSound(text.substring(0, i + 1).takeLastWhile { it in 'A'..'Z' || it in 'a'..'z' })
        else -> null
    }
}

/**
 * Numbers are read in Sino-Korean: the last non-zero digit decides (일 칠 팔 end in ㄹ; 영 삼 육
 * in another consonant; 이 사 오 구 in a vowel), unless the number ends in zeros, in which case it
 * ends on 십, 백, 천 or 만, all consonants.
 */
private fun koreanNumberFinalSound(digits: String): FinalSound {
    if (digits.length > 1 && digits.last() == '0') return FinalSound.Consonant
    return when (digits.last()) {
        '1', '7', '8' -> FinalSound.Rieul
        '0', '3', '6' -> FinalSound.Consonant
        else -> FinalSound.Vowel
    }
}

/**
 * Latin text is read either as letter names (an abbreviation such as «KTX», or a single letter)
 * or as a loanword. Letter names ending in a consonant are 엘 and 알 (ㄹ), 엠 and 엔. Loanwords are
 * approximated by their spelling: -l is ㄹ (홀), -m, -n and -ng are consonants (메인, 링), and
 * almost everything else gains a final vowel in Korean (파크, 스트리트, 센터).
 */
private fun latinFinalSound(word: String): FinalSound {
    val last = word.last()
    if (word.length == 1 || word.all { it.isUpperCase() }) {
        return when (last.uppercaseChar()) {
            'L', 'R' -> FinalSound.Rieul
            'M', 'N' -> FinalSound.Consonant
            else -> FinalSound.Vowel
        }
    }
    return when (last.lowercaseChar()) {
        'l' -> FinalSound.Rieul
        'm', 'n' -> FinalSound.Consonant
        'g' -> if (word.lowercase().endsWith("ng")) FinalSound.Consonant else FinalSound.Vowel
        else -> FinalSound.Vowel
    }
}
