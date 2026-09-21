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
    private val numberedChome = Regex("([0-9]+|[〇一二三四五六七八九十]+)丁目?")
    private val numberSeparator = Regex("([0-9])\\s*(?:番地|番|号|の|ー)")
    private val spaces = Regex("\\s+")
    private val spaceBeforeNumber = Regex("([^0-9 ]) ([0-9])")
    private val searchedAddress = Regex("[^0-9 ].*[0-9] [0-9]+")
    private val searchedBlock = Regex("[^0-9 ].*[0-9]( [0-9]+)*")
}
