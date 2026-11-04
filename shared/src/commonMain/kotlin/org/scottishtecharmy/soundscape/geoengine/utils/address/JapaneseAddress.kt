package org.scottishtecharmy.soundscape.geoengine.utils.address

import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.normalizeForSearch

/**
 * Most Japanese addresses don't number a building along a street. A ward (区) is divided into
 * districts (町), most districts into numbered chōme (丁目), those into numbered blocks (番), and a
 * building is numbered within its block (号). Osaka Station is 北区梅田三丁目1-1: building 1 of block 1
 * in the third chōme of Umeda, in Kita ward - with the block and building numbers joined by a hyphen,
 * as they're written on signs and in maps.
 *
 * OpenStreetMap has these as addr:suburb (the ward), addr:quarter (the district), addr:neighbourhood
 * (the chōme), addr:block_number and addr:housenumber. The district and chōme are mapped in more
 * than one way, and all of them are common:
 *
 * - addr:neighbourhood=梅田三丁目, the district and chōme together, with no addr:quarter
 * - addr:quarter=豊崎 with addr:neighbourhood=3, 3丁 or 3丁目
 * - addr:neighbourhood=豊崎3
 * - addr:quarter or addr:neighbourhood on its own, for a district with no chōme
 */
object JapaneseAddress {

    /** The district and its chōme as they're written together - 梅田三丁目, 豊崎3丁目 - or null if neither is known. */
    fun chome(quarter: String?, neighbourhood: String?): String? {
        val district = quarter?.trim()?.takeIf { it.isNotEmpty() }
        // Some have the whole address, commas and all, in addr:neighbourhood
        val chome = neighbourhood?.trim()?.takeIf { it.isNotEmpty() && (',' !in it) }
        return when {
            district == null -> chome
            chome == null -> district
            chome.startsWith(district) -> chome
            chome.all { it.isDigit() } -> "$district${chome}丁目"
            else -> district + chome
        }
    }

    /** The block and building numbers as they're written together - 1-1 - or null if neither is known. */
    fun number(blockNumber: String?, houseNumber: String?): String? {
        val block = blockNumber?.trim()?.takeIf { it.isNotEmpty() }
        val house = houseNumber?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            block == null -> house
            // A building "number" that isn't one - "フジタビル1BF", the floor of a building - isn't
            // part of the address
            (house == null) || !house.first().isDigit() -> block
            else -> "$block-$house"
        }
    }

    /** The address within its ward - 梅田三丁目1-1 - or null if its district isn't known. */
    fun address(quarter: String?, neighbourhood: String?, blockNumber: String?, houseNumber: String?): String? {
        val chome = chome(quarter, neighbourhood) ?: return null
        return chome + (number(blockNumber, houseNumber) ?: "")
    }

    /**
     * [text] in the form addresses are compared in for search, in which however the numbers were
     * written - 梅田3-1-1, 梅田三丁目1番1号, 梅田３丁目１−１ - they're digits after the district:
     * "梅田3 1 1". Null if [text] isn't written like a Japanese address, as a name in Japanese
     * followed by a block number at least.
     *
     * Two numbers are wanted, so that a name which happens to end in one - "ローソン梅田3" - isn't
     * taken for an address. A district with no chōme has only one, and its block is searched for as
     * "大深町4番": 番 says the number is a block, which a name never does.
     */
    fun searchKey(text: String): String? {
        // Every search goes through here, so a search which can't be for a Japanese address - it has
        // nothing written in Japanese in it - is turned away before anything is normalized
        if (text.none { it.code >= 0x3000 }) return null
        val key = canonical(text)
        if (key.isEmpty() || (key[0].code < 0x3000)) return null
        val wanted = if ('番' in text) searchedBlock else searchedAddress
        return if (wanted.matches(key)) key else null
    }

    /**
     * The [searchKey]s the address of a building is found by - its own, and its block's - or null
     * if it isn't numbered within a block of a known district.
     */
    fun searchKeys(quarter: String?, neighbourhood: String?, blockNumber: String?, houseNumber: String?): Pair<String, String>? {
        val chome = chome(quarter, neighbourhood) ?: return null
        val block = blockNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return Pair(canonical("$chome ${number(block, houseNumber)}"), canonical("$chome $block"))
    }

    /** The parts of a block address, as they'd have been tagged had they been tagged that way. */
    data class Parts(
        val suburb: String?,
        val quarter: String?,
        val neighbourhood: String?,
        val blockNumber: String,
        val houseNumber: String,
    )

    /**
     * The block address in an [street] and [houseNumber] that are tagged as if it were a street
     * address, or null if they aren't. Plenty of Japanese addresses are tagged that way, with the
     * district in addr:street and the rest of the address in addr:housenumber:
     *
     * - addr:street=神戸市中央区北長狭通 with addr:housenumber=1-31-35 or １丁目３０−１９ - the chōme,
     *   block and building, behind the city and ward
     * - addr:street=南船場３丁目 with addr:housenumber=11-24 - the chōme with the district, and the
     *   block and building
     *
     * No road is named that way, so they're no use to anything that follows the street. A
     * housenumber with one number, or two with no chōme - 平岡町新在家 2214-57, a plot of land - can't
     * be told from a street address, and isn't one of these. Nor is a road's name - 国道176号線,
     * 堀川通 (Horikawa-dori) - or a phone number, which turns up as a housenumber too.
     */
    fun fromStreet(street: String?, houseNumber: String?): Parts? {
        // Every house in the map with a street comes through here, so most are turned away on
        // their first characters
        if ((street == null) || (houseNumber == null)) return null
        if ((street.firstOrNull()?.code ?: 0) < 0x3000) return null
        if (houseNumber.firstOrNull()?.let { it.isDigit() } != true) return null

        val number = asciiNumbers(houseNumber).trim()
        var district = asciiNumbers(street).trim()
        if (district.any { it.code < 0x3000 && !it.isDigit() } || district.any { it in HANGUL }) return null
        if ((district.last() in ROAD_ENDINGS) || district.endsWith("通り")) return null
        if (district.any { it in BRACKETS }) return null

        // The city and ward in front of the district: the ward is the suburb
        var suburb: String? = null
        val prefecture = PREFECTURE.find(district)
        if (prefecture != null) district = district.substring(prefecture.range.last + 1)
        val ward = CITY_AND_WARD.find(district)
        if (ward != null) {
            suburb = ward.groupValues[2]
            district = district.substring(ward.range.last + 1)
        } else if (prefecture != null) {
            CITY.find(district)?.let { district = district.substring(it.range.last + 1) }
        }
        if (district.isEmpty()) return null

        val chome = STREET_CHOME.find(district)
        if (chome != null) {
            // 南船場3丁目 11-24: the block and building are all that's left in the housenumber
            if (district.substring(0, chome.range.first).let { it.isEmpty() || it.any(Char::isDigit) }) return null
            val parts = BLOCK_AND_BUILDING.matchEntire(number) ?: return null
            return Parts(suburb, null, district, parts.groupValues[1], parts.groupValues[2])
        }
        // 北長狭通 1-31-35: the chōme too
        if (district.any(Char::isDigit)) return null
        val parts = CHOME_BLOCK_AND_BUILDING.matchEntire(number) ?: return null
        return Parts(suburb, district, parts.groupValues[1], parts.groupValues[2], parts.groupValues[3])
    }

    /** [text] with its full-width digits and the dashes between numbers written in ASCII. */
    private fun asciiNumbers(text: String): String {
        val result = StringBuilder(text.length)
        for (ch in text) {
            result.append(
                when (ch) {
                    in '０'..'９' -> '0' + (ch - '０')
                    in NUMBER_DASHES -> '-'
                    else -> ch
                }
            )
        }
        return result.toString()
    }

    /** Whether a search for [searchKey] is for the address with [key] - the same, or with its ward or city in front. */
    fun isMatch(searchKey: String, key: String): Boolean {
        if (!searchKey.endsWith(key)) return false
        val before = searchKey.length - key.length - 1
        return (before < 0) || !searchKey[before].isDigit()
    }

    private fun canonical(text: String): String {
        var key = normalizeForSearch(text)
        key = numberedChome.replace(key) { match ->
            val number = match.groupValues[1]
            "${number.toIntOrNull() ?: kanjiNumber(number)} "
        }
        key = numberSeparator.replace(key, "$1 ")
        key = spaces.replace(key, " ").trim()
        return spaceBeforeNumber.replace(key, "$1$2")
    }

    /** The value of a number written in kanji, e.g. 二十三 for 23. */
    private fun kanjiNumber(kanji: String): Int {
        var total = 0
        var digits = 0
        for (ch in kanji) {
            if (ch == '十') {
                total += (if (digits == 0) 1 else digits) * 10
                digits = 0
            } else {
                digits = digits * 10 + KANJI_DIGITS.indexOf(ch)
            }
        }
        return total + digits
    }

    private const val KANJI_DIGITS = "〇一二三四五六七八九"
    // ー is a long vowel mark in a name, and only a dash between digits, so it's only replaced in
    // the housenumber's numbers - see BLOCK_AND_BUILDING
    private const val NUMBER_DASHES = "−－‐‑–—―"
    private val HANGUL = '\uAC00'..'\uD7AF'
    // The name of a road (国道176号線, 千日前通商店街) rather than a district - 路, 段, 巷 and 弄 are
    // Chinese ones, for the Taiwanese streets a block address must never be taken from
    private const val ROAD_ENDINGS = "線号路街段巷弄"
    // A district's name has 通 on its own - 北長狭通 - and it's a road that's 通り: 周防町通り 1-15-15
    // is in 東心斎橋, not in a district of that name. Nor is a district's name followed by another
    // in brackets, as roads' are - 南堀江立花通（オレンジストリート）
    private const val BRACKETS = "（）「」【】"
    private val PREFECTURE = Regex("^(?:東京都|北海道|京都府|大阪府|.{2,3}県)")
    private val CITY_AND_WARD = Regex("^(.+?[市郡])?(.+?[区區])")
    private val CITY = Regex("^.+?市|^.+?郡.+?[町村]")
    private val STREET_CHOME = Regex("(?:[0-9]+|[〇一二三四五六七八九十]+)丁目$")
    private val BLOCK_AND_BUILDING = Regex("([1-9][0-9]*)\\s*(?:番地?|[-ー])\\s*([1-9][0-9]*)\\s*号?")
    // A chōme is numbered from one, and there are rarely more than a dozen of them - which also
    // turns away a phone number (075-212-9026)
    private val CHOME_BLOCK_AND_BUILDING =
        Regex("([1-9][0-9]?)\\s*(?:丁目|[-ー])\\s*([1-9][0-9]*)\\s*(?:番地?|[-ー])\\s*([1-9][0-9]*)\\s*号?")
    private val numberedChome = Regex("([0-9]+|[〇一二三四五六七八九十]+)丁目?")
    private val numberSeparator = Regex("([0-9])\\s*(?:番地|番|号|の|ー)")
    private val spaces = Regex("\\s+")
    private val spaceBeforeNumber = Regex("([^0-9 ]) ([0-9])")
    private val searchedAddress = Regex("[^0-9 ].*[0-9] [0-9]+")
    private val searchedBlock = Regex("[^0-9 ].*[0-9]( [0-9]+)*")
}
