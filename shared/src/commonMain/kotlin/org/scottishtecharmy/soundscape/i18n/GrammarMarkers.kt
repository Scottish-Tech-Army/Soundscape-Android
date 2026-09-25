package org.scottishtecharmy.soundscape.i18n

/**
 * Resolves the "either form" markers that translators write when a word's form depends on the
 * text a placeholder is replaced with, e.g. Hungarian «a(z) %1$s», Korean «%1$s을(를)»,
 * Turkish «%1$s'{DA}», Finnish «{Tiellä %1$s}», Estonian «{Tänaval %1$s}» or French
 * «le long {de %1$s}».
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
    if ('(' !in text && '{' !in text) return text
    val hungarian = resolveHungarianArticles(resolveHungarianTerminative(resolveHungarianRoadCase(text)))
    val finnic = resolveEstonianRoadCase(resolveFinnishRoadCase(resolveFrenchArticles(hungarian)))
    return resolveTurkishSuffixes(resolveKoreanParticles(finnic))
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

// Hungarian street names carry their street type («Andrássy út», «Váci utca», «Deák tér»), so a
// template can't add its own «úton» ("on the road") without saying it twice: «az Andrássy út úton».
// «%1$s{úton}» puts the name's own street word into the case the sentence needs instead
// («az Andrássy úton», «a Váci utcán», «a Deák téren»). A name without a known street word (a
// route number such as «M7», a foreign name) keeps the plain « úton», which is what the templates
// said before, so nothing reads worse than it did.
private val hungarianRoadCase = Regex("\\{úton\\}")

// Street words as they end a name, with the form "on/in it" takes. Matched against the end of the
// name's last word, longest first, so compounds work too («Nagykörút», «Kőhíd», «Pincesor») and
// «alagút» (a tunnel: in it, not on it) wins over «út». Measured against the Budapest extract
// (2026-09): these cover 95% of the 16,094 distinct street names, and most of the rest are Slovak
// names across the border. Unnamed ways get the class name («Ösvény», «Gyalogút», «Autópálya»,
// «Főútvonal», «Lakóterület»), which is covered too.
private val hungarianStreetWords = mapOf(
    "utca" to "utcán", "út" to "úton", "útja" to "útján", "körút" to "körúton",
    "sugárút" to "sugárúton", "alagút" to "alagútban", "tér" to "téren", "tere" to "terén",
    "köz" to "közön", "sor" to "soron", "fasor" to "fasoron", "sétány" to "sétányon",
    "dűlő" to "dűlőn", "lépcső" to "lépcsőn", "lejtő" to "lejtőn", "híd" to "hídon",
    "hídja" to "hídján", "rakpart" to "rakparton", "part" to "parton", "park" to "parkban",
    "lakópark" to "lakóparkban", "liget" to "ligetben", "kert" to "kertben", "udvar" to "udvarban",
    "telep" to "telepen", "negyed" to "negyedben", "terület" to "területen", "hely" to "helyen",
    "ösvény" to "ösvényen", "ösvénye" to "ösvényén", "járó" to "járón", "körönd" to "köröndön",
    "pálya" to "pályán", "vonal" to "vonalon", "korzó" to "korzón", "sziget" to "szigeten",
    "gát" to "gáton", "domb" to "dombon", "hegy" to "hegyen", "szél" to "szélen",
    "terasz" to "teraszon", "csapás" to "csapáson", "tanya" to "tanyán", "major" to "majorban",
    "völgy" to "völgyben", "rév" to "réven", "összekötő" to "összekötőn",
)
private val hungarianStreetWordsLongestFirst = hungarianStreetWords.keys.sortedByDescending { it.length }

internal fun resolveHungarianRoadCase(text: String): String {
    if ("{úton}" !in text) return text
    val out = StringBuilder()
    var last = 0
    for (match in hungarianRoadCase.findAll(text)) {
        val before = text.substring(last, match.range.first)
        val word = before.takeLastWhile { it.isLetter() }
        val lower = word.lowercase()
        val key = hungarianStreetWordsLongestFirst.firstOrNull { lower.endsWith(it) }
        if (key == null) {
            out.append(before).append(" úton")
        } else {
            val stem = before.dropLast(key.length)
            var form = hungarianStreetWords.getValue(key)
            // A whole-word match keeps the name's capital: an unnamed «Ösvény» becomes «Ösvényen».
            if (key.length == word.length && word[0].isUpperCase()) {
                form = form.replaceFirstChar { it.uppercaseChar() }
            }
            out.append(stem).append(form)
        }
        last = match.range.last + 1
    }
    return out.append(text.substring(last)).toString()
}

// «%3$s{-ig}» is the terminative ("as far as X"), which Hungarian writes onto the name itself:
// a final a/e lengthens («Váci utcáig», «Hősök teréig», «Astoriáig»), any other letter just
// takes «ig» («Deák Ferenc térig»), and only a number or abbreviation keeps the hyphen («M7-ig»).
private val hungarianTerminative = Regex("\\{-ig\\}")

internal fun resolveHungarianTerminative(text: String): String {
    if ("{-ig}" !in text) return text
    val out = StringBuilder()
    var last = 0
    for (match in hungarianTerminative.findAll(text)) {
        val before = text.substring(last, match.range.first)
        val word = before.takeLastWhile { it.isLetterOrDigit() }
        val abbreviation = word.length >= 2 && word.all { it.isUpperCase() || it.isDigit() }
        when {
            word.isEmpty() || word.last().isDigit() || abbreviation -> out.append(before).append("-ig")
            word.last() == 'a' -> out.append(before.dropLast(1)).append("áig")
            word.last() == 'e' -> out.append(before.dropLast(1)).append("éig")
            else -> out.append(before).append("ig")
        }
        last = match.range.last + 1
    }
    return out.append(text.substring(last)).toString()
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

// ---------------------------------------------------------------------------------------------
// Turkish: case suffixes follow the vowel harmony and final sound of the word they attach to.
// ---------------------------------------------------------------------------------------------

// Translators write the suffix in the archiphoneme notation Turkish grammars use, where the
// capitals are the letters that change: '{DA} (locative), '{DAn} (ablative), '{A} (dative),
// '{I} (accusative) and '{In} (genitive).
private val turkishSuffix = Regex("(['’])\\{(DAn|DA|A|In|I)\\}")

private const val TURKISH_VOWELS = "aeıioöuü"
private const val TURKISH_BACK_VOWELS = "aıou"
private const val TURKISH_VOICELESS = "fstkçşhp"

/** Lowercases with Turkish dotted and dotless i, which the default lowercase() gets wrong. */
private fun turkishLowercase(text: String): String =
    text.replace('I', 'ı').replace('İ', 'i').lowercase()

internal fun resolveTurkishSuffixes(text: String): String =
    turkishSuffix.replace(text) { match ->
        val apostrophe = match.groupValues[1]
        val stem = turkishStem(text, match.range.first)
        apostrophe + turkishSuffixFor(match.groupValues[2], stem)
    }

/**
 * What the suffix attaches to, as Turkish would say it: [spoken] is the word whose sounds decide
 * the suffix, and [possessive] is set for names like «Atatürk Caddesi», whose final possessive
 * ending takes an extra n («Caddesi'nde», not «Caddesi'de»).
 */
private class TurkishStem(val spoken: String, val possessive: Boolean)

private fun turkishStem(text: String, end: Int): TurkishStem? {
    var i = end
    while (i > 0 && text[i - 1] in CLOSING_PUNCTUATION) i--
    val before = text.substring(0, i)
    // A street-type abbreviation is read in full: «Bağdat Cd.» is «Bağdat Caddesi».
    if (before.endsWith('.')) {
        val abbreviation = before.dropLast(1).takeLastWhile { it.isLetter() }
        val full = turkishStreetAbbreviations[turkishLowercase(abbreviation)]
        if (full != null) return TurkishStem(full, possessive = true)
    }
    val digits = before.takeLastWhile { it.isDigit() }
    if (digits.isNotEmpty()) return TurkishStem(turkishNumberLastWord(digits), possessive = false)
    val word = before.takeLastWhile { it.isLetter() }
    if (word.isEmpty()) return null
    val lower = turkishLowercase(word)
    // An abbreviation is read letter by letter: TRT'ye (te), ABD'de (de).
    if (word.length == 1 || (word.length <= 4 && word.all { it.isUpperCase() })) {
        return TurkishStem(turkishLetterName(lower.last()), possessive = false)
    }
    return TurkishStem(lower, possessive = isTurkishPossessive(lower))
}

/**
 * The last word of a number as spoken: 3 üç, 40 kırk, 100 yüz, 2000 iki bin. Only that word
 * matters to the suffix («40'ta», «2000'de»).
 */
private fun turkishNumberLastWord(digits: String): String {
    val number = digits.trimStart('0')
    if (number.isEmpty()) return "sıfır"
    val units = listOf("bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    val tens = listOf("on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan")
    val last = number.last() - '0'
    if (last != 0) return units[last - 1]
    if (number.length >= 2 && number[number.length - 2] != '0') {
        return tens[number[number.length - 2] - '1']
    }
    if (number.length >= 3 && number[number.length - 3] != '0') return "yüz"
    // Ends in 000: the last non-zero group of three names the power.
    val trailingZeros = number.length - number.trimEnd('0').length
    return when (trailingZeros / 3) {
        1 -> "bin"
        2 -> "milyon"
        3 -> "milyar"
        else -> "trilyon"
    }
}

/** Letter names: be, ce, de… end in e; vowels are their own name; X is iks. */
private fun turkishLetterName(letter: Char): String = when (letter) {
    in TURKISH_VOWELS -> letter.toString()
    'x' -> "iks"
    'q' -> "kü"
    else -> "${letter}e"
}

// Generic nouns that end a place name in their possessive form («Bağdat Caddesi», «Moda
// Parkı»). Matched as word endings, so compounds such as «Havalimanı» and «Otoyolu» count too.
private val turkishPossessiveEndings = listOf(
    "parkı", "yolu", "sokağı", "bulvarı", "meydanı", "durağı", "limanı", "alanı", "istasyonu",
    "okulu", "camii", "merkezi", "tüneli", "gölü", "garı", "ormanı", "sarayı", "oteli", "plajı",
    "kavşağı", "geçidi", "mezarlığı", "anıtı", "terminali", "otogarı", "stadı", "stadyumu",
    "kampüsü", "pazarı", "evleri", "konutları", "tesisleri", "köyü", "hanı", "hamamı",
)

private val turkishStreetAbbreviations = mapOf(
    "cd" to "caddesi", "cad" to "caddesi", "sk" to "sokağı", "sok" to "sokağı",
    "blv" to "bulvarı", "bul" to "bulvarı", "mah" to "mahallesi", "mh" to "mahallesi",
)

private fun isTurkishPossessive(word: String): Boolean {
    if (turkishPossessiveEndings.any { word.endsWith(it) }) return true
    // -sı/-si/-su/-sü after a vowel is the possessive of a vowel-final noun: Caddesi, Mahallesi,
    // Müzesi, Köprüsü, Çarşısı. Personal names rarely end this way.
    return word.length >= 4 &&
        word[word.length - 1] in "ıiuü" &&
        word[word.length - 2] == 's' &&
        word[word.length - 3] in TURKISH_VOWELS
}

private fun turkishSuffixFor(archiphoneme: String, stem: TurkishStem?): String {
    // Nothing to go on (a symbol, or no text at all): the plain front-vowel, consonant-final form.
    val spoken = stem?.spoken ?: "e"
    val lastVowel = spoken.lastOrNull { it in TURKISH_VOWELS } ?: 'e'
    val back = lastVowel in TURKISH_BACK_VOWELS
    val a = if (back) "a" else "e"
    val i = when (lastVowel) {
        'a', 'ı' -> "ı"
        'e', 'i' -> "i"
        'o', 'u' -> "u"
        else -> "ü"
    }
    val endsInVowel = stem != null && spoken.last() in TURKISH_VOWELS
    val voiceless = spoken.last() in TURKISH_VOICELESS
    val possessive = stem?.possessive == true
    val d = if (voiceless) "t" else "d"
    return when (archiphoneme) {
        "DA" -> if (possessive) "nd$a" else "$d$a"
        "DAn" -> if (possessive) "nd${a}n" else "$d${a}n"
        "A" -> when {
            possessive -> "n$a"
            endsInVowel -> "y$a"
            else -> a
        }
        "I" -> when {
            possessive -> "n$i"
            endsInVowel -> "y$i"
            else -> i
        }
        else -> if (possessive || endsInVowel) "n${i}n" else "${i}n" // In
    }
}

// ---------------------------------------------------------------------------------------------
// Finnish: "on the road X" is the road name itself in the adessive: «Mannerheimintiellä».
// ---------------------------------------------------------------------------------------------

// Finnish can't inflect a map name it doesn't know, so templates said «Tiellä %1$s» ("on the road
// Mannerheimintie"), which names the road type twice. «{Tiellä %1$s}» wraps the name instead: when
// its last word ends in a known street word, that ending takes the adessive («Mannerheimintiellä»,
// «Aleksanterinkadulla»); otherwise the label stays («Tiellä Almas väg»), as before. The label is
// matched case-insensitively and only «tiellä» / «kadulla» count, so no other brace is touched.
private val finnishRoadCase = Regex("\\{([Tt]iellä|[Kk]adulla) ([^{}]+)\\}")

// Street-word endings and their adessive, consonant gradation included (katu → kadulla,
// mäki → mäellä). Measured against the Helsinki extract (2026-09): these cover about 94% of the
// Finnish street names; most of the rest are Swedish names, which keep the label.
private val finnishStreetEndings = mapOf(
    "tie" to "tiellä", "katu" to "kadulla", "kuja" to "kujalla", "polku" to "polulla",
    "rinne" to "rinteellä", "raitti" to "raitilla", "mäki" to "mäellä", "kaari" to "kaarella",
    "ranta" to "rannalla", "silta" to "sillalla", "portti" to "portilla", "piha" to "pihalla",
    "aukio" to "aukiolla", "kallio" to "kalliolla", "kulma" to "kulmalla", "tori" to "torilla",
    "puistikko" to "puistikolla", "väylä" to "väylällä", "rata" to "radalla",
    "kaarre" to "kaarteella", "kierto" to "kierrolla", "penger" to "penkereellä",
    "laituri" to "laiturilla", "bulevardi" to "bulevardilla", "esplanadi" to "esplanadilla",
    "promenadi" to "promenadilla", "reitti" to "reitillä", "lenkki" to "lenkillä",
    "kenttä" to "kentällä", "puisto" to "puistossa", "niemi" to "niemellä", "linja" to "linjalla",
    "tanhua" to "tanhualla", "vainio" to "vainiolla", "portaat" to "portailla",
    "tunneli" to "tunnelissa", "niitty" to "niityllä", "käytävä" to "käytävällä",
    "taival" to "taipaleella", "mutka" to "mutkalla", "törmä" to "törmällä", "laita" to "laidalla",
    "reuna" to "reunalla", "pelto" to "pellolla", "suora" to "suoralla", "harju" to "harjulla",
    "haara" to "haaralla", "varsi" to "varrella", "koukku" to "koukulla",
    "kierros" to "kierroksella", "ympyrä" to "ympyrällä",
)
private val finnishStreetEndingsLongestFirst = finnishStreetEndings.keys.sortedByDescending { it.length }

// A leading adjective or ordinal declines with the street word («Vanhalla Vihdintiellä», «Toisella
// linjalla»). Anything else before it is a genitive or a person's name, which stays as it is
// («Ali-Seppälän tiellä», «Toivo Kuulan polulla»).
private val finnishLeadingWords = mapOf(
    "vanha" to "vanhalla", "iso" to "isolla", "uusi" to "uudella", "pieni" to "pienellä",
    "itäinen" to "itäisellä", "läntinen" to "läntisellä", "pohjoinen" to "pohjoisella",
    "eteläinen" to "eteläisellä", "toinen" to "toisella", "kolmas" to "kolmannella",
    "neljäs" to "neljännellä", "viides" to "viidennellä", "kuudes" to "kuudennella",
    "seitsemäs" to "seitsemännellä", "kahdeksas" to "kahdeksannella",
    "yhdeksäs" to "yhdeksännellä", "kymmenes" to "kymmenennellä",
)

internal fun resolveFinnishRoadCase(text: String): String {
    if ('{' !in text) return text
    return finnishRoadCase.replace(text) { match ->
        val label = match.groupValues[1]
        val name = match.groupValues[2]
        finnishAdessive(name) ?: "$label $name"
    }
}

private fun finnishAdessive(name: String): String? {
    val words = name.split(' ').toMutableList()
    val last = words.last()
    val lower = last.lowercase()
    val ending = finnishStreetEndingsLongestFirst.firstOrNull { lower.endsWith(it) } ?: return null
    words[words.lastIndex] = last.dropLast(ending.length) + finnishStreetEndings.getValue(ending)
    if (words.size > 1) {
        finnishLeadingWords[words[0].lowercase()]?.let { form ->
            words[0] = if (words[0][0].isUpperCase()) form.replaceFirstChar { it.uppercaseChar() } else form
        }
    }
    return words.joinToString(" ")
}

// ---------------------------------------------------------------------------------------------
// Estonian: the same construction as Finnish, «{Teel %1$s}» / «{Tänaval %1$s}».
// ---------------------------------------------------------------------------------------------

private val estonianRoadCase = Regex("\\{([Tt]eel|[Tt]änaval) ([^{}]+)\\}")

// Street words and their adessive («Pärnu maantee» → «Pärnu maanteel», «Kalda põik» → «Kalda
// põigul»), matched against the end of the last word, longest first. Measured against the Tallinn
// extract (2026-09): together with the single-word rule below these cover 93% of its 5,765 street
// names; the rest (village road names, route labels) keep the label.
private val estonianStreetWords = mapOf(
    "tee" to "teel", "maantee" to "maanteel", "puiestee" to "puiesteel", "tänav" to "tänaval",
    "põik" to "põigul", "sild" to "sillal", "rada" to "rajal", "tunnel" to "tunnelis",
    "ring" to "ringil", "allee" to "alleel", "plats" to "platsil", "väljak" to "väljakul",
    "käik" to "käigul", "promenaad" to "promenaadil", "trepp" to "trepil", "park" to "pargis",
    "turg" to "turul", "rand" to "rannal", "kallas" to "kaldal",
)
private val estonianStreetWordsLongestFirst = estonianStreetWords.keys.sortedByDescending { it.length }

// Abbreviations used in names («Pärnu mnt», «Kadrioru pst»), read in full.
private val estonianStreetAbbreviations = mapOf("mnt" to "maanteel", "pst" to "puiesteel", "tn" to "tänaval")

internal fun resolveEstonianRoadCase(text: String): String {
    if ('{' !in text) return text
    return estonianRoadCase.replace(text) { match ->
        val label = match.groupValues[1]
        val name = match.groupValues[2]
        estonianAdessive(name) ?: "$label $name"
    }
}

private fun estonianAdessive(name: String): String? {
    val words = name.split(' ')
    val last = words.last()
    val lower = last.lowercase().trimEnd('.')
    val head = words.dropLast(1).joinToString(" ")
    fun join(tail: String) = if (head.isEmpty()) tail else "$head $tail"

    estonianStreetAbbreviations[lower]?.let { return join(it) }
    val key = estonianStreetWordsLongestFirst.firstOrNull { lower.endsWith(it) }
    if (key != null) {
        var form = last.dropLast(key.length) + estonianStreetWords.getValue(key)
        // A whole-word match keeps the name's capital: an unnamed «Rada» becomes «Rajal».
        if (key.length == last.length && last[0].isUpperCase()) {
            form = form.replaceFirstChar { it.uppercaseChar() }
        }
        return join(form)
    }
    // Estonian map data drops «tänav» from street names: «Metsa» is Metsa tänav, so it is
    // «Metsa tänaval». Only for a single capitalised word, which is how those names appear.
    if (words.size == 1 && last[0].isUpperCase() && last.all { it.isLetter() || it == '-' }) {
        return "$last tänaval"
    }
    return null
}

// ---------------------------------------------------------------------------------------------
// French: articles and contractions around a map name, «le long {de %1$s}».
// ---------------------------------------------------------------------------------------------

// French puts an article before a street or place type and contracts it with «de» / «à»: «le
// long de la rue de Rivoli», «au boulevard Haussmann», «près du Lycée Marie Curie», «aux Halles».
// A template can't know the name, so it wraps the preposition and the name, «{de %1$s}», and this
// adds or contracts the article. Only the prepositions below are recognised, so no other brace is
// touched, and a template with a distance or a count in that slot simply isn't wrapped.
private val frenchArticle = Regex("\\{(de|De|à|À|sur|Sur|vers|Vers|entre|et|après|avant|depuis) ([^{}]+)\\}")

private enum class FrenchGender { M, F }

private class FrenchType(val gender: FrenchGender, val elides: Boolean, val lowercase: Boolean)

// Road words are written lowercase after the article («la rue de Rivoli»); place words keep the
// name's capital («le Centre Pompidou»). Measured against the Paris extract (2026-09), road words
// open 96% of its 73,000 street names. Quebec's rang, côte, montée and croissant are included.
private val frenchTypes: Map<String, FrenchType> = buildMap {
    fun road(g: FrenchGender, elides: Boolean, vararg words: String) =
        words.forEach { put(it, FrenchType(g, elides, lowercase = true)) }
    fun place(g: FrenchGender, elides: Boolean, vararg words: String) =
        words.forEach { put(it, FrenchType(g, elides, lowercase = false)) }
    road(FrenchGender.F, false, "rue", "route", "place", "sente", "villa", "ruelle", "résidence", "voie",
        "cour", "promenade", "cité", "passerelle", "venelle", "montée", "rampe", "traverse", "piste",
        "berge", "digue", "galerie", "côte", "terrasse", "boucle")
    road(FrenchGender.F, true, "allée", "avenue", "impasse", "esplanade", "autoroute")
    road(FrenchGender.M, false, "chemin", "square", "sentier", "passage", "boulevard", "rond-point",
        "clos", "quai", "mail", "pont", "hameau", "cours", "carrefour", "tunnel", "parvis", "faubourg",
        "rang", "croissant", "lotissement", "domaine")
    place(FrenchGender.F, false, "pharmacie", "maison", "mairie", "crèche", "boucherie", "gare", "salle",
        "boulangerie", "porte", "bibliothèque", "piscine", "station", "banque", "poste", "clinique")
    place(FrenchGender.F, true, "école", "église")
    place(FrenchGender.M, false, "centre", "collège", "gymnase", "lycée", "château", "stade", "café",
        "cimetière", "marché", "musée", "théâtre", "cinéma", "restaurant", "supermarché", "magasin",
        "parc", "jardin", "parking")
    place(FrenchGender.M, true, "hôtel", "espace", "institut", "atelier", "hôpital")
}

private const val FRENCH_VOWELS = "aeiouyàâéèêëîïôûüœAEIOUYÀÂÉÈÊËÎÏÔÛÜŒ"

internal fun resolveFrenchArticles(text: String): String {
    if ('{' !in text) return text
    return frenchArticle.replace(text) { match -> frenchPhrase(match.groupValues[1], match.groupValues[2]) }
}

private fun frenchPhrase(preposition: String, name: String): String {
    val first = name.substringBefore(' ')
    val rest = name.substring(first.length)
    val lower = preposition.lowercase()
    val capital = preposition[0].isUpperCase()
    fun cap(s: String) = if (capital) s.replaceFirstChar { it.uppercaseChar() } else s

    // «de» + le = du, les = des; «à» + le = au, les = aux. Anything else keeps the article whole.
    fun withArticle(article: String): String = when {
        lower == "de" && article == "le " -> cap("du ")
        lower == "de" && article == "les " -> cap("des ")
        lower == "à" && article == "le " -> cap("au ")
        lower == "à" && article == "les " -> cap("aux ")
        else -> "$preposition $article"
    }

    frenchTypes[first.lowercase()]?.let { type ->
        val article = when {
            type.elides -> "l’"
            type.gender == FrenchGender.M -> "le "
            else -> "la "
        }
        val word = if (type.lowercase) first.lowercase() else first
        return withArticle(article) + word + rest
    }
    // A name that brings its own «Le» / «Les» contracts it («du Bon Marché», «aux Halles»); with
    // any other preposition the article stays as the name writes it («vers Le Havre»).
    if ((first == "Le" || first == "Les") && rest.isNotEmpty() && (lower == "de" || lower == "à")) {
        return withArticle(first.lowercase() + " ") + rest.trimStart()
    }
    if (lower == "de" && name.isNotEmpty() && name[0] in FRENCH_VOWELS) {
        return cap("d’") + name
    }
    return "$preposition $name"
}
