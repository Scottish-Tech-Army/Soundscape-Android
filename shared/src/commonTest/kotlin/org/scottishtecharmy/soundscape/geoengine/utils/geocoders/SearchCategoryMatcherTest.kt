package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchCategoryMatcherTest {

    private val pharmacy = SearchCategory("pharmacy", listOf(OsmTag(null, "pharmacy"), OsmTag("shop", "chemist")))
    private val bar = SearchCategory("bar", listOf(OsmTag(null, "bar")))
    private val barber = SearchCategory("barber", listOf(OsmTag("shop", "hairdresser")))
    private val chemistShop = SearchCategory("chemist", listOf(OsmTag(null, "chemist")))
    private val butcher = SearchCategory("butcher", listOf(OsmTag(null, "butcher")))
    private val bicycle = SearchCategory("bicycle", listOf(OsmTag(null, "bicycle")))
    private val books = SearchCategory("books", listOf(OsmTag(null, "books")))
    private val shoes = SearchCategory("shoes", listOf(OsmTag(null, "shoes")))

    private val matcher = SearchCategoryMatcher(
        listOf(
            "pharmacy" to pharmacy,
            "chemist" to pharmacy,
            "drugstore" to pharmacy,
            "Bar" to bar,
            "barber" to barber,
            "Pharmacie" to pharmacy,
            "Pharmacie" to chemistShop,
            "Butcher Shop" to butcher,
            "Barber Shop" to barber,
            "Book Shop" to books,
            "Shoe Shop" to shoes,
            "Tienda de bicicletas" to bicycle,
        )
    )

    @Test
    fun synonymsMatchTheirCategory() {
        assertEquals(CategoryMatch(pharmacy, null), matcher.match("pharmacy"))
        assertEquals(CategoryMatch(pharmacy, null), matcher.match("chemist"))
        assertEquals(CategoryMatch(pharmacy, null), matcher.match("Drugstore"))
    }

    @Test
    fun smallTyposStillMatch() {
        assertEquals(CategoryMatch(pharmacy, null), matcher.match("pharmasy"))
    }

    @Test
    fun aNameBeforeOrAfterTheCategoryIsTheRemainder() {
        assertEquals(CategoryMatch(pharmacy, "boots"), matcher.match("Boots pharmacy"))
        assertEquals(CategoryMatch(pharmacy, "boots"), matcher.match("pharmacy Boots"))
    }

    @Test
    fun theWholePhraseHasToMatch() {
        assertEquals(CategoryMatch(bar, null), matcher.match("bar"))
        assertEquals(CategoryMatch(barber, null), matcher.match("barber"))
        assertNull(matcher.match("barb"))
    }

    @Test
    fun categoriesSharingAPhraseAreSearchedTogether() {
        val match = matcher.match("pharmacie")!!
        assertEquals(setOf("pharmacy", "chemist"), match.category.values)
        assertNull(match.remainder)
    }

    @Test
    fun namesAreNotCategories() {
        assertNull(matcher.match("Tesco"))
        assertNull(matcher.match(""))
    }

    @Test
    fun theFirstOrLastWordsOfAPhraseMatch() {
        assertEquals(CategoryMatch(butcher, null), matcher.match("butcher"))
        assertEquals(CategoryMatch(butcher, "smith"), matcher.match("Smith butcher"))
        assertEquals(CategoryMatch(bicycle, null), matcher.match("bicicletas"))
        assertEquals(CategoryMatch(bicycle, null), matcher.match("tienda de bicicletas"))
    }

    @Test
    fun aWholePhraseMatchBeatsPartOfOne() {
        // "barber" is a phrase of its own as well as the start of "Barber Shop", and "bar" is only
        // ever whole words
        assertEquals(CategoryMatch(barber, null), matcher.match("barber"))
        assertEquals(CategoryMatch(bar, null), matcher.match("bar"))
    }

    @Test
    fun partOfTooManyPhrasesIsNotACategory() {
        assertNull(matcher.match("shop"))
    }

    @Test
    fun twoCharacterWordsMatchInChineseJapaneseAndKorean() {
        val cjk = SearchCategoryMatcher(listOf("药店" to pharmacy, "薬局" to pharmacy, "약국" to pharmacy, "ab" to bar))
        assertEquals(CategoryMatch(pharmacy, null), cjk.match("药店"))
        assertEquals(CategoryMatch(pharmacy, null), cjk.match("薬局"))
        assertEquals(CategoryMatch(pharmacy, null), cjk.match("약국"))
        assertNull(cjk.match("ab"))
    }
}
