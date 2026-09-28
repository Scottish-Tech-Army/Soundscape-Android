package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.search_synonyms_atm
import org.scottishtecharmy.soundscape.resources.search_synonyms_bakery
import org.scottishtecharmy.soundscape.resources.search_synonyms_bank
import org.scottishtecharmy.soundscape.resources.search_synonyms_bench
import org.scottishtecharmy.soundscape.resources.search_synonyms_bicycle_shop
import org.scottishtecharmy.soundscape.resources.search_synonyms_book_shop
import org.scottishtecharmy.soundscape.resources.search_synonyms_bus_stop
import org.scottishtecharmy.soundscape.resources.search_synonyms_cafe
import org.scottishtecharmy.soundscape.resources.search_synonyms_cinema
import org.scottishtecharmy.soundscape.resources.search_synonyms_convenience
import org.scottishtecharmy.soundscape.resources.search_synonyms_dentist
import org.scottishtecharmy.soundscape.resources.search_synonyms_doctors
import org.scottishtecharmy.soundscape.resources.search_synonyms_drinking_water
import org.scottishtecharmy.soundscape.resources.search_synonyms_fast_food
import org.scottishtecharmy.soundscape.resources.search_synonyms_fuel
import org.scottishtecharmy.soundscape.resources.search_synonyms_gym
import org.scottishtecharmy.soundscape.resources.search_synonyms_hairdresser
import org.scottishtecharmy.soundscape.resources.search_synonyms_hospital
import org.scottishtecharmy.soundscape.resources.search_synonyms_hotel
import org.scottishtecharmy.soundscape.resources.search_synonyms_library
import org.scottishtecharmy.soundscape.resources.search_synonyms_museum
import org.scottishtecharmy.soundscape.resources.search_synonyms_park
import org.scottishtecharmy.soundscape.resources.search_synonyms_parking
import org.scottishtecharmy.soundscape.resources.search_synonyms_pharmacy
import org.scottishtecharmy.soundscape.resources.search_synonyms_phone_shop
import org.scottishtecharmy.soundscape.resources.search_synonyms_place_of_worship
import org.scottishtecharmy.soundscape.resources.search_synonyms_playground
import org.scottishtecharmy.soundscape.resources.search_synonyms_police
import org.scottishtecharmy.soundscape.resources.search_synonyms_post_box
import org.scottishtecharmy.soundscape.resources.search_synonyms_post_office
import org.scottishtecharmy.soundscape.resources.search_synonyms_pub
import org.scottishtecharmy.soundscape.resources.search_synonyms_restaurant
import org.scottishtecharmy.soundscape.resources.search_synonyms_school
import org.scottishtecharmy.soundscape.resources.search_synonyms_supermarket
import org.scottishtecharmy.soundscape.resources.search_synonyms_swimming_pool
import org.scottishtecharmy.soundscape.resources.search_synonyms_taxi
import org.scottishtecharmy.soundscape.resources.search_synonyms_toilets
import org.scottishtecharmy.soundscape.resources.search_synonyms_train_station
import org.scottishtecharmy.soundscape.resources.search_synonyms_veterinary
import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.getSystemLocale
import org.scottishtecharmy.soundscape.utils.fuzzyCompare

/**
 * An OSM tag in Photon's osm_tag form. A null [key] matches [value] under any key, which is what
 * lets a [ResourceMapper] value - "pharmacy", with no idea whether it came from amenity= or shop= -
 * be searched for as it is.
 */
data class OsmTag(val key: String?, val value: String) {
    fun toPhoton() = "${key ?: ""}:$value"
}

/** A type of place that can be searched for - every OSM tag which means that type of place. */
data class SearchCategory(val id: String, val tags: List<OsmTag>) {
    /** The OSM values, which is all the offline tiles' class and subclass have to match against */
    val values: Set<String> get() = tags.map { it.value }.toSet()
}

/**
 * A search which names a [category] of place, and possibly also a name for it to have - the
 * "boots" of "boots pharmacy" - as the [remainder].
 */
data class CategoryMatch(val category: SearchCategory, val remainder: String?)

/**
 * Finds the [SearchCategory] a search is for - "pharmacy", "chemist", "pharmacie" - from the
 * phrases which name each category in the current language.
 */
class SearchCategoryMatcher(phrases: List<Pair<String, SearchCategory>>) {

    private class Phrase(val text: String, val words: List<String>, val category: SearchCategory)

    private val phrases = phrases
        .map { (phrase, category) ->
            val text = normalizeForSearch(phrase)
            Phrase(text, text.split(" ").filter { it.isNotEmpty() }, category)
        }
        .filter { it.text.length >= minimumLength(it.text) }

    /**
     * The category which [query] names, as a whole or with a name before or after it. The whole
     * query is tried first, and then the longest run of words from its end and then its start, so
     * that "boots pharmacy" and "pharmacy boots" are both a pharmacy called "boots".
     */
    fun match(query: String): CategoryMatch? {
        val words = normalizeForSearch(query).split(" ").filter { it.isNotEmpty() }
        for (length in words.size downTo 1) {
            val starts = if (length == words.size) listOf(0) else listOf(words.size - length, 0)
            for (start in starts) {
                val span = words.subList(start, start + length).joinToString(" ")
                val category = categoryFor(span) ?: continue
                val remainder = (words.subList(0, start) + words.subList(start + length, words.size))
                    .joinToString(" ")
                return CategoryMatch(category, remainder.ifEmpty { null })
            }
        }
        return null
    }

    /**
     * The category whose phrase is closest to [span], if any is close enough. Where several
     * categories share the best phrase - French names amenity=pharmacy and shop=chemist both
     * "Pharmacie" - they're all searched for.
     *
     * A phrase matches as a whole or, failing that, by whole words from its start or its end:
     * "butcher" is the "Butcher Shop", and "bicicletas" the "Tienda de bicicletas". Only whole words
     * count, or "bar" would find the "Barber Shop". A span which matches part of too many phrases -
     * "shop" - is too vague to be a category at all.
     */
    private fun categoryFor(span: String): SearchCategory? {
        if (span.length < minimumLength(span)) return null

        val whole = bestCategories(phrases.asSequence().map { span.fuzzyCompare(it.text, false) to it.category })
        if (whole.isNotEmpty()) return combine(whole)

        val spanWordCount = span.count { it == ' ' } + 1
        val partial = bestCategories(
            phrases.asSequence()
                .filter { it.words.size > spanWordCount }
                .flatMap { phrase ->
                    sequenceOf(
                        phrase.words.take(spanWordCount),
                        phrase.words.takeLast(spanWordCount)
                    ).map { words -> span.fuzzyCompare(words.joinToString(" "), false) to phrase.category }
                }
        )
        if (partial.size > MAX_PARTIAL_MATCH_CATEGORIES) return null
        return combine(partial)
    }

    /** The categories which share the best of [scores], if it's close enough to be a match */
    private fun bestCategories(scores: Sequence<Pair<Double, SearchCategory>>): List<SearchCategory> {
        var bestScore = MATCH_THRESHOLD
        val best = mutableListOf<SearchCategory>()
        for ((score, category) in scores) {
            if (score < bestScore) {
                bestScore = score
                best.clear()
                best.add(category)
            } else if ((score == bestScore) && (category !in best)) {
                best.add(category)
            }
        }
        return best
    }

    private fun combine(categories: List<SearchCategory>): SearchCategory? = when (categories.size) {
        0 -> null
        1 -> categories[0]
        else -> SearchCategory(
            categories.joinToString("+") { it.id },
            categories.flatMap { it.tags }.distinct()
        )
    }

    /**
     * The shortest phrase which can name a category. Two letters is too few to mean anything in
     * most scripts, but in Chinese, Japanese and Korean it's how long most of the words are: 药店,
     * 薬局, 약국.
     */
    private fun minimumLength(text: String) =
        if (text.isNotEmpty() && isUnspacedScript(codePointAt(text, 0))) 2 else 3

    companion object {
        private const val MATCH_THRESHOLD = 0.2

        // The most categories a match on part of a phrase can be for before it's too vague to use
        private const val MAX_PARTIAL_MATCH_CATEGORIES = 3
    }
}

/**
 * Builds the [SearchCategoryMatcher] for the app's current language from two sources: the
 * translated name of every OSM value in [ResourceMapper], and the translated synonym lists for the
 * most commonly searched for types of place.
 */
object SearchCategories {

    /**
     * Values which aren't a type of place anyone would search for, or which there are far too many
     * of for a search to be any use - roads, settlements and the like.
     */
    private val excludedValues = setOf(
        "highway", "intersection", "roundabout", "highway_ramp", "merging_lane", "entrance",
        "crossing", "unmanaged_crossing", "construction", "walking_path", "pedestrian_street",
        "bicycle_path", "residential_street", "service_road", "rail", "railway", "transit",
        "service", "road", "primary", "secondary", "tertiary", "minor", "motorway", "trunk", "path",
        "raceway", "busway", "bus_guideway", "ferry", "motorway_construction", "trunk_construction",
        "primary_construction", "secondary_construction", "tertiary_construction",
        "minor_construction", "path_construction", "service_construction", "track_construction",
        "raceway_construction", "city", "town", "village", "hamlet", "quarter", "locality",
        "neighbourhood", "yes", "no", "unclassified", "building", "sport",
    )

    /**
     * The types of place with their own list of synonyms, and the OSM tags each one means. These
     * take several tags where OSM has more than one way of tagging the same kind of place.
     */
    private val synonyms: List<Triple<String, StringResource, List<OsmTag>>> = listOf(
        Triple("pharmacy", Res.string.search_synonyms_pharmacy, tags(":pharmacy", "shop:chemist", "healthcare:pharmacy")),
        Triple("cafe", Res.string.search_synonyms_cafe, tags(":cafe")),
        Triple("restaurant", Res.string.search_synonyms_restaurant, tags(":restaurant")),
        Triple("fast_food", Res.string.search_synonyms_fast_food, tags(":fast_food")),
        Triple("pub", Res.string.search_synonyms_pub, tags(":pub", ":bar")),
        Triple("toilets", Res.string.search_synonyms_toilets, tags(":toilets")),
        Triple("atm", Res.string.search_synonyms_atm, tags(":atm")),
        Triple("bank", Res.string.search_synonyms_bank, tags(":bank")),
        Triple("supermarket", Res.string.search_synonyms_supermarket, tags(":supermarket", ":greengrocer")),
        Triple("convenience", Res.string.search_synonyms_convenience, tags(":convenience")),
        Triple("bus_stop", Res.string.search_synonyms_bus_stop, tags(":bus_stop")),
        Triple("train_station", Res.string.search_synonyms_train_station, tags("railway:station", "railway:halt")),
        Triple("post_office", Res.string.search_synonyms_post_office, tags(":post_office")),
        Triple("post_box", Res.string.search_synonyms_post_box, tags(":post_box")),
        Triple("doctors", Res.string.search_synonyms_doctors, tags(":doctors", ":clinic", "healthcare:doctor")),
        Triple("dentist", Res.string.search_synonyms_dentist, tags(":dentist")),
        Triple("hospital", Res.string.search_synonyms_hospital, tags(":hospital")),
        Triple("library", Res.string.search_synonyms_library, tags(":library")),
        Triple("police", Res.string.search_synonyms_police, tags(":police")),
        Triple("fuel", Res.string.search_synonyms_fuel, tags(":fuel")),
        Triple("parking", Res.string.search_synonyms_parking, tags("amenity:parking")),
        Triple("hotel", Res.string.search_synonyms_hotel, tags(":hotel", ":guest_house", ":hostel")),
        Triple("park", Res.string.search_synonyms_park, tags("leisure:park")),
        Triple("playground", Res.string.search_synonyms_playground, tags(":playground")),
        Triple("place_of_worship", Res.string.search_synonyms_place_of_worship, tags(":place_of_worship")),
        Triple("school", Res.string.search_synonyms_school, tags("amenity:school")),
        Triple("cinema", Res.string.search_synonyms_cinema, tags(":cinema")),
        Triple("bakery", Res.string.search_synonyms_bakery, tags(":bakery")),
        Triple("hairdresser", Res.string.search_synonyms_hairdresser, tags(":hairdresser")),
        Triple("bench", Res.string.search_synonyms_bench, tags(":bench")),
        Triple("drinking_water", Res.string.search_synonyms_drinking_water, tags(":drinking_water")),
        Triple("taxi", Res.string.search_synonyms_taxi, tags(":taxi")),
        Triple("museum", Res.string.search_synonyms_museum, tags(":museum")),
        Triple("gym", Res.string.search_synonyms_gym, tags(":fitness_centre")),
        Triple("swimming_pool", Res.string.search_synonyms_swimming_pool, tags(":swimming_pool", "sport:swimming")),
        Triple("veterinary", Res.string.search_synonyms_veterinary, tags(":veterinary")),
        Triple("bicycle_shop", Res.string.search_synonyms_bicycle_shop, tags("shop:bicycle")),
        Triple("book_shop", Res.string.search_synonyms_book_shop, tags("shop:books")),
        Triple("phone_shop", Res.string.search_synonyms_phone_shop, tags("shop:mobile_phone", "shop:telecommunication")),
    )

    private fun tags(vararg photonTags: String) = photonTags.map {
        val key = it.substringBefore(':')
        OsmTag(key.ifEmpty { null }, it.substringAfter(':'))
    }

    private val mutex = Mutex()
    private var cachedLanguage: String? = null
    private var cachedMatcher: SearchCategoryMatcher? = null

    /** The matcher for the app's current language, built the first time it's asked for */
    suspend fun matcher(): SearchCategoryMatcher = mutex.withLock {
        val language = (getAppLocale() ?: getSystemLocale()).let { "${it.language}-${it.region}" }
        cachedMatcher?.takeIf { cachedLanguage == language }
            ?: SearchCategoryMatcher(buildPhrases()).also {
                cachedMatcher = it
                cachedLanguage = language
            }
    }

    private suspend fun buildPhrases(): List<Pair<String, SearchCategory>> {
        val phrases = mutableListOf<Pair<String, SearchCategory>>()

        // Several values can share a name - "lift" and "elevator" - so there's one category per
        // name, which searches for all of them
        ResourceMapper.entries()
            .filterKeys { it !in excludedValues }
            .entries
            .groupBy({ it.value }, { it.key })
            .forEach { (resource, values) ->
                val category = SearchCategory(resource.key, values.sorted().map { OsmTag(null, it) })
                phrases.add(getString(resource) to category)
            }

        for ((id, resource, tags) in synonyms) {
            val category = SearchCategory(id, tags)
            for (phrase in getString(resource).split(',', '،', '、', '，', ';')) {
                if (phrase.isNotBlank()) phrases.add(phrase.trim() to category)
            }
        }
        return phrases
    }
}
