package org.scottishtecharmy.soundscape.geoengine.mvttranslation

/**
 * The tile keys holding a feature's name in [language], most preferred first. Empty when there's
 * no language, which leaves every feature with just its local name.
 *
 * OSM tags translations as name:<language>, and mostly ignores the region - there's no name:en-GB -
 * so the region only matters where OSM splits a language by script. Chinese is written in
 * Simplified characters on the mainland and Traditional ones in Taiwan and Hong Kong, and plain
 * name:zh is whichever the mapper happened to use. Norwegian Bokmål is tagged both as name:nb and,
 * more often, as the macrolanguage name:no.
 *
 * A language written in the Latin alphabet falls back to name:latin, the romanised name, before
 * the local one: most places have no translation, and "Daimaru Umedaten" is more use to someone
 * reading English than 大丸梅田店. Where the local name is already in Latin letters, name:latin is
 * just a copy of it.
 */
fun nameKeysForLanguage(language: String?, region: String?): List<String> {
    val keys = when (language) {
        null, "" -> return emptyList()
        "zh" -> if (region in traditionalChineseRegions) {
            listOf("name:zh-Hant", "name:zh")
        } else {
            listOf("name:zh-Hans", "name:zh")
        }
        "nb" -> listOf("name:nb", "name:no")
        else -> listOf("name:$language")
    }
    return if (language in latinScriptLanguages) keys + "name:latin" else keys
}

private val traditionalChineseRegions = setOf("TW", "HK", "MO")

// The app's languages which are written in the Latin alphabet - see supportedLanguages. Serbian is
// offered in Cyrillic, so isn't one of them.
private val latinScriptLanguages = setOf(
    "ca", "cs", "da", "de", "en", "es", "et", "fi", "fr", "ha", "hr", "hu", "id", "is", "it", "nb",
    "nl", "pl", "pt", "ro", "sk", "sl", "sv", "sw", "tr", "vi"
)

/**
 * Picks the app language's name for a feature out of its name:xx tags, as a layer's tags are read
 * one at a time. Made once per layer, since that's what the key indices belong to, and reset for
 * each feature.
 */
internal class NameTranslationPicker(layerKeys: List<String>, nameKeys: List<String>) {
    // Where each of the layer's keys comes in nameKeys, or -1 for a key which isn't one of them.
    private val ranks = IntArray(layerKeys.size) { nameKeys.indexOf(layerKeys[it]) }
    private var rank = Int.MAX_VALUE

    var translatedName: String? = null
        private set

    fun reset() {
        rank = Int.MAX_VALUE
        translatedName = null
    }

    fun offer(keyIndex: Int, value: String?) {
        val keyRank = ranks[keyIndex]
        if ((value != null) && (keyRank >= 0) && (keyRank < rank)) {
            rank = keyRank
            translatedName = value
        }
    }

    /** The translation, or null where there isn't one or it's no different from [name]. */
    fun translationOf(name: String?): String? = translatedName?.takeIf { it != name }
}
