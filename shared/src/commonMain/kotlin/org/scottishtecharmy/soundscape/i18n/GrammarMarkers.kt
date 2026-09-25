package org.scottishtecharmy.soundscape.i18n

/**
 * Resolves the "either form" markers that translators write when a word's form depends on the
 * text a placeholder is replaced with, e.g. Hungarian «a(z) %1$s», Korean «%1$s을(를)»,
 * Turkish «%1$s'{DA}», Finnish «{Tiellä %1$s}», Estonian «{Tänaval %1$s}», or the Romance
 * «le long {fr:de %1$s}».
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
    val finnic = resolveEstonianRoadCase(resolveFinnishRoadCase(resolveRomanceArticles(hungarian)))
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
// French, Spanish, Italian, Portuguese: articles and contractions around a map name.
// ---------------------------------------------------------------------------------------------

// A template can't know the map name it will get, so it can't pick the article or contract it
// with the preposition before it (French «du», Spanish «del», Italian «alla», Portuguese «no»).
// It wraps the preposition and the name instead, tagged with the language because «de» and
// «entre» exist in several of them: «le long {fr:de %1$s}», «{es:por %1$s}», «{it:a %1$s}»,
// «{pt:na %1$s}». Only a placeholder that holds a place or street name is wrapped, never a
// distance or a count.
private val romanceMarker = Regex("\\{(fr|es|it|pt):(\\S+) ([^{}]+)\\}")

internal fun resolveRomanceArticles(text: String): String {
    if ('{' !in text) return text
    return romanceMarker.replace(text) { match ->
        val (language, preposition, name) = match.destructured
        when (language) {
            "fr" -> frenchPhrase(preposition, name)
            "es" -> spanishPhrase(preposition, name)
            "it" -> italianPhrase(preposition, name)
            else -> portuguesePhrase(preposition, name)
        }
    }
}

private enum class Gender { M, F }

/** A street or place word that opens a name, with the article it takes. */
private class NounType(val gender: Gender, val elides: Boolean = false, val lowercase: Boolean = false)

private fun MutableMap<String, NounType>.words(type: NounType, vararg words: String) =
    words.forEach { put(it, type) }

private fun capitaliseLike(model: String, text: String) =
    if (model[0].isUpperCase()) text.replaceFirstChar { it.uppercaseChar() } else text

// --- French ------------------------------------------------------------------------------------

// Road words are written lowercase after the article («la rue de Rivoli»); place words keep the
// name's capital («le Centre Pompidou»). Measured against the Paris extract (2026-09), road words
// open 96% of its 73,000 street names. Quebec's rang, côte, montée and croissant are included.
private val frenchTypes: Map<String, NounType> = buildMap {
    words(NounType(Gender.F, lowercase = true), "rue", "route", "place", "sente", "villa", "ruelle",
        "résidence", "voie", "cour", "promenade", "cité", "passerelle", "venelle", "montée", "rampe",
        "traverse", "piste", "berge", "digue", "galerie", "côte", "terrasse", "boucle")
    words(NounType(Gender.F, elides = true, lowercase = true), "allée", "avenue", "impasse",
        "esplanade", "autoroute")
    words(NounType(Gender.M, lowercase = true), "chemin", "square", "sentier", "passage", "boulevard",
        "rond-point", "clos", "quai", "mail", "pont", "hameau", "cours", "carrefour", "tunnel", "parvis",
        "faubourg", "rang", "croissant", "lotissement", "domaine")
    words(NounType(Gender.F), "pharmacie", "maison", "mairie", "crèche", "boucherie", "gare", "salle",
        "boulangerie", "porte", "bibliothèque", "piscine", "station", "banque", "poste", "clinique")
    words(NounType(Gender.F, elides = true), "école", "église")
    words(NounType(Gender.M), "centre", "collège", "gymnase", "lycée", "château", "stade", "café",
        "cimetière", "marché", "musée", "théâtre", "cinéma", "restaurant", "supermarché", "magasin",
        "parc", "jardin", "parking")
    words(NounType(Gender.M, elides = true), "hôtel", "espace", "institut", "atelier", "hôpital")
}

private const val FRENCH_VOWELS = "aeiouyàâéèêëîïôûüœAEIOUYÀÂÉÈÊËÎÏÔÛÜŒ"

private fun frenchPhrase(preposition: String, name: String): String {
    val first = name.substringBefore(' ')
    val rest = name.substring(first.length)
    val lower = preposition.lowercase()

    // «de» + le = du, les = des; «à» + le = au, les = aux. Anything else keeps the article whole.
    fun withArticle(article: String): String = when {
        lower == "de" && article == "le " -> capitaliseLike(preposition, "du ")
        lower == "de" && article == "les " -> capitaliseLike(preposition, "des ")
        lower == "à" && article == "le " -> capitaliseLike(preposition, "au ")
        lower == "à" && article == "les " -> capitaliseLike(preposition, "aux ")
        else -> "$preposition $article"
    }

    frenchTypes[first.lowercase()]?.let { type ->
        val article = when {
            type.elides -> "l’"
            type.gender == Gender.M -> "le "
            else -> "la "
        }
        return withArticle(article) + (if (type.lowercase) first.lowercase() else first) + rest
    }
    // A name that brings its own «Le» / «Les» contracts it («du Bon Marché», «aux Halles»); with
    // any other preposition the article stays as the name writes it («vers Le Havre»).
    if ((first == "Le" || first == "Les") && rest.isNotEmpty() && (lower == "de" || lower == "à")) {
        return withArticle(first.lowercase() + " ") + rest.trimStart()
    }
    if (lower == "de" && name.isNotEmpty() && name[0] in FRENCH_VOWELS) {
        return capitaliseLike(preposition, "d’") + name
    }
    return "$preposition $name"
}

// --- Spanish -----------------------------------------------------------------------------------

// Spanish contracts only «el»: de + el = del, a + el = al. Road words go lowercase after the
// article («por la avenida de Mayo»); place words keep their capital («cerca del Hospital
// Italiano»). Checked against the Buenos Aires and San Salvador test tiles (2026-09); many
// Argentine streets are a person's name alone («Juan B. Justo»), which takes no article and is
// left as it is.
private val spanishTypes: Map<String, NounType> = buildMap {
    words(NounType(Gender.F, lowercase = true), "calle", "avenida", "diagonal", "senda", "carretera",
        "colonia", "prolongación", "entrada", "plaza", "ronda", "travesía", "glorieta", "vía", "rambla",
        "cuesta", "carrera", "transversal", "autopista", "autovía", "costanera", "plazoleta", "alameda",
        "rotonda", "cerrada", "privada", "calzada", "ruta", "circunvalación", "bajada", "subida",
        "vereda", "peatonal", "av.", "avda.")
    words(NounType(Gender.M, lowercase = true), "pasaje", "camino", "acceso", "puente", "boulevard",
        "bulevar", "callejón", "redondel", "paseo", "jirón", "sendero", "malecón", "andador",
        "periférico", "viaducto", "pasadizo", "pje.")
    words(NounType(Gender.F), "escuela", "farmacia", "iglesia", "comisaría", "estación", "casa",
        "clínica", "alcaldía", "tienda", "cancha", "universidad", "biblioteca", "catedral", "capilla",
        "terminal", "ferretería", "parada")
    words(NounType(Gender.M), "centro", "instituto", "club", "colegio", "supermercado", "hospital",
        "hotel", "parque", "jardín", "cementerio", "restaurante", "mercado", "museo", "teatro",
        "estadio", "banco", "aeropuerto", "gimnasio", "parqueo")
}

private val spanishFeminineOrdinal = Regex("^\\d+[aª]$")
private val spanishMasculineOrdinal = Regex("^\\d+[oº]$")

private fun spanishPhrase(preposition: String, name: String): String {
    val first = name.substringBefore(' ')
    val rest = name.substring(first.length)
    val lower = preposition.lowercase()

    fun withArticle(article: String): String = when {
        lower == "de" && article == "el " -> capitaliseLike(preposition, "del ")
        lower == "a" && article == "el " -> capitaliseLike(preposition, "al ")
        else -> "$preposition $article"
    }

    val type = spanishTypes[first.lowercase()]
    if (type != null) {
        val article = if (type.gender == Gender.M) "el " else "la "
        // An abbreviation («Av.», «Pje.») keeps the name's own spelling.
        val word = if (type.lowercase && !first.endsWith('.')) first.lowercase() else first
        return withArticle(article) + word + rest
    }
    // An ordinal such as «2a Calle Poniente» takes the article of the word it numbers.
    if (spanishFeminineOrdinal.matches(first)) return withArticle("la ") + name
    if (spanishMasculineOrdinal.matches(first)) return withArticle("el ") + name
    // A name's own «El» contracts («del Corte Inglés», «al Cairo»); La, Los and Las never do.
    if (first == "El" && rest.isNotEmpty() && (lower == "de" || lower == "a")) {
        return withArticle("el ") + rest.trimStart()
    }
    return "$preposition $name"
}

// --- Italian -----------------------------------------------------------------------------------

// Italian leaves the article out before via, piazza and corso («su via Roma»), so nothing is
// added there. What it can't do in a template is fuse a preposition with a name's own article:
// «a La Scala» is «alla Scala», «di Il Vittoriano» is «del Vittoriano».
private val italianArticulated = mapOf(
    "a" to listOf("al", "allo", "alla", "all’", "ai", "agli", "alle"),
    "di" to listOf("del", "dello", "della", "dell’", "dei", "degli", "delle"),
    "da" to listOf("dal", "dallo", "dalla", "dall’", "dai", "dagli", "dalle"),
    "in" to listOf("nel", "nello", "nella", "nell’", "nei", "negli", "nelle"),
    "su" to listOf("sul", "sullo", "sulla", "sull’", "sui", "sugli", "sulle"),
)
private val italianArticles = listOf("Il", "Lo", "La", "L’", "I", "Gli", "Le")

private fun italianPhrase(preposition: String, name: String): String {
    val forms = italianArticulated[preposition.lowercase()] ?: return "$preposition $name"
    val normalised = name.replace("L'", "L’")
    if (normalised.startsWith("L’") && normalised.length > 2) {
        return capitaliseLike(preposition, forms[3]) + normalised.substring(2)
    }
    val first = name.substringBefore(' ')
    val rest = name.substring(first.length).trimStart()
    val index = italianArticles.indexOf(first)
    if (index >= 0 && rest.isNotEmpty()) return capitaliseLike(preposition, forms[index]) + " " + rest
    return "$preposition $name"
}

// --- Portuguese --------------------------------------------------------------------------------

// Portuguese uses the article before most street and place names and fuses it with em, de, a and
// por («na Rua Augusta», «no Largo do Carmo», «da Avenida da Liberdade», «ao Parque»). The
// templates had written a fixed feminine article («na %1$s»), wrong for every masculine type. The
// marker keeps whatever preposition the template wrote; this picks the article from the name's
// first word, and a name it doesn't recognise keeps the template's own wording.
private val portugueseTypes: Map<String, NounType> = buildMap {
    words(NounType(Gender.F), "rua", "avenida", "praça", "travessa", "estrada", "alameda", "calçada",
        "rodovia", "ponte", "praceta", "via", "ladeira", "rotunda", "quinta", "viela", "marginal",
        "autoestrada", "escadaria", "servidão", "vila", "estação", "escola", "igreja", "universidade",
        "farmácia", "biblioteca", "capela", "clínica", "padaria", "loja", "faculdade",
        "r.", "av.", "tv.", "trav.", "pç.", "pça.", "estr.", "al.")
    words(NounType(Gender.M), "largo", "beco", "viaduto", "túnel", "parque", "jardim", "caminho",
        "bairro", "pátio", "terreiro", "cais", "passeio", "mercado", "hospital", "museu", "centro",
        "shopping", "teatro", "colégio", "supermercado", "estádio", "cemitério", "aeroporto",
        "terminal", "posto", "banco", "hotel", "restaurante", "café", "mosteiro", "palácio",
        "castelo", "convento", "instituto")
}

// The preposition a template's wording stands for, with its article fused or not.
private val portugueseBase = mapOf(
    "em" to "em", "na" to "em", "no" to "em", "nas" to "em", "nos" to "em",
    "de" to "de", "da" to "de", "do" to "de", "das" to "de", "dos" to "de",
    "a" to "a", "à" to "a", "ao" to "a", "às" to "a", "aos" to "a",
    "por" to "por", "pela" to "por", "pelo" to "por",
)
private val portugueseFused = mapOf(
    "em" to listOf("no", "na", "nos", "nas"),
    "de" to listOf("do", "da", "dos", "das"),
    "a" to listOf("ao", "à", "aos", "às"),
    "por" to listOf("pelo", "pela", "pelos", "pelas"),
)

private fun portuguesePhrase(preposition: String, name: String): String {
    val first = name.substringBefore(' ')
    val rest = name.substring(first.length)
    val base = portugueseBase[preposition.lowercase()] ?: preposition.lowercase()

    // Index into the fused forms: masculine / feminine, singular / plural.
    fun phrase(index: Int, body: String): String {
        val fused = portugueseFused[base]?.get(index)
            ?: "$base ${listOf("o", "a", "os", "as")[index]}"
        return capitaliseLike(preposition, fused) + " " + body
    }

    portugueseTypes[first.lowercase()]?.let { type ->
        return phrase(if (type.gender == Gender.M) 0 else 1, name)
    }
    // A name's own article («O Mosteiro», «A Brasileira») fuses the same way.
    val ownArticle = listOf("O", "A", "Os", "As").indexOf(first)
    if (ownArticle >= 0 && rest.isNotEmpty()) return phrase(ownArticle, rest.trimStart())
    return "$preposition $name"
}
