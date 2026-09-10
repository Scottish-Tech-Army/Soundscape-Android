package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.nameKeysForLanguage
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.process

/**
 * Offline search, geocoding and callouts on the map data of cities outside the UK: Tehran, San
 * Salvador, Paris, Buenos Aires and Osaka. Between them they have Persian script and digits,
 * Japanese with no spaces between its words, French and Spanish accents, house numbers written
 * after the street, addresses written from the largest place to the smallest, numbered street
 * names and both hemispheres, none of which the UK extracts do.
 *
 * Addresses are written the way the country they're in writes them, whatever the phone is set
 * to, so the tests which check one run with the phone set to more than one country.
 */
class WorldCitiesTest {

    private class City(val location: LngLatAlt, private val nameKeys: List<String> = emptyList()) {
        // Loading the grids is the slow part and nothing here changes them, so each city is
        // loaded once and shared between the tests.
        val gridState by lazy {
            getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE, nameKeys = nameKeys)
        }
        private val geocoder by lazy {
            val settlementState = getGridStateForLocation(location, 12, 3, nameKeys = nameKeys)
            OfflineGeocoder(
                gridState,
                settlementState,
                TileSearch(offlineExtractPath, gridState, settlementState),
                processor = { it.process() }
            )
        }

        fun search(text: String): List<LocationDescription> = runBlocking {
            geocoder.getAddressFromLocationName(text, location, null)!!
        }

        fun reverseGeocode(at: LngLatAlt): LocationDescription = runBlocking {
            geocoder.getAddressFromLngLat(UserGeometry(at), null, false)!!
        }

        /** The callout for the nearest subway station, ignoring its entrances. */
        fun nearestSubwayStation(): String? =
            gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
                // Enough stops to get past the dozens of entrances and bus stops around a big
                // station - Osaka's are all nearer than the station on the Midōsuji Line below it
                .getNearestCollection(location, 500.0, 200, gridState.ruler)
                .map { it as MvtFeature }
                .firstOrNull { it.featureValue == "subway" && it.properties?.get("entrance") == null }
                ?.getText(null)?.text
    }

    companion object {
        private val tehran = City(tehranTestLocation)
        private val sanSalvador = City(sanSalvadorTestLocation)
        private val paris = City(parisTestLocation)
        private val buenosAires = City(buenosAiresTestLocation)
        private val osaka = City(osakaTestLocation)
        // The same, with the app set to English
        private val osakaInEnglish = City(osakaTestLocation, nameKeysForLanguage("en", "GB"))
    }

    // ---------------------------------------------------------------------------------- Tehran

    @Test
    fun tehranSearchInPersian() {
        assertEquals("میدان انقلاب", tehran.search("میدان انقلاب").first().name)
    }

    @Test
    fun tehranSearchByEnglishName() {
        // The metro station's name:en - the result is named in whichever language matched
        assertEquals(
            "Meydan-e Enghelab-e Eslami",
            tehran.search("Meydan-e Enghelab-e Eslami").first().name
        )
    }

    @Test
    fun tehranSearchTypedOnArabicKeyboard() {
        // An Arabic keyboard types kaf and yeh (ك ي) where Persian has keheh and farsi yeh (ک ی)
        assertEquals("کتاب نوین", tehran.search("كتاب نوين").first().name)
    }

    @Test
    fun tehranSearchWithOrWithoutZeroWidthNonJoiner() {
        // "بن‌بست" (dead end) has a zero-width non-joiner between its two parts, which people type
        // as the non-joiner, as a space, or not at all.
        for (typed in listOf("بن‌بست افشار", "بن بست افشار", "بنبست افشار")) {
            assertEquals(typed, "بن‌بست افشار", tehran.search(typed).first().name)
        }
    }

    @Test
    fun tehranSearchNumberedAlley() {
        // The alleys off Namjoo are numbered, and the numbers are part of their names
        for (typed in listOf("نامجو ۱۲", "نامجو 12")) {
            assertEquals(typed, "نامجو ۱۲", tehran.search(typed).first().name)
        }
    }

    @Test
    fun tehranSearchHouseNumberInPersianOrWesternDigits() {
        for (typed in listOf("رشتچی ۱۴", "رشتچی 14")) {
            assertEquals(typed, "رشتچی ۱۴", tehran.search(typed).first().name)
        }
    }

    @Test
    fun tehranReverseGeocodePersianHouseNumber() {
        // Iranian addresses put the house number on the line after the road
        for (phone in listOf("en-GB", "fa-IR")) {
            withDefaultLocale(phone) {
                assertEquals(
                    phone,
                    "رشتچی ۱۴",
                    tehran.reverseGeocode(LngLatAlt(51.390515863895416, 35.69980174504374)).name
                )
            }
        }
    }

    // ------------------------------------------------------------------------------ San Salvador

    @Test
    fun sanSalvadorSearchWithoutTilde() {
        assertEquals("Avenida España", sanSalvador.search("Avenida Espana").first().name)
    }

    @Test
    fun sanSalvadorSearchHouseNumberAfterStreet() = withDefaultLocale("en-GB") {
        // Spanish addresses put the number after the street, and accents are often left out
        assertEquals(
            "Calle Presbítero Vicente Aguilar 225",
            sanSalvador.search("Calle Presbitero Vicente Aguilar 225").first().name
        )
    }

    @Test
    fun sanSalvadorReverseGeocodeHouseNumber() {
        for (phone in listOf("en-GB", "es-SV")) {
            withDefaultLocale(phone) {
                val address = sanSalvador.reverseGeocode(LngLatAlt(-89.18934255838394, 13.697920728666729))
                assertEquals(phone, "Calle Presbítero Vicente Aguilar 315", address.name)
                // The SV template's "{{postcode}} - {{city}}" line mustn't leave a stray "-"
                assertEquals(phone, "Calle Presbítero Vicente Aguilar 315", address.description)
            }
        }
    }

    @Test
    fun sanSalvadorSearchNumberedStreet() {
        // Most of the streets in the centre are numbered
        assertEquals("4a Avenida Sur", sanSalvador.search("4a Avenida Sur").first().name)
    }

    @Test
    fun sanSalvadorSearchListsEachPlaceOnce() {
        // The cathedral's name is tagged in Spanish, Catalan, Portuguese and Indonesian, and all
        // of them match. (Its crypt is a separate place, and is found too.)
        val cathedralNames = setOf(
            "Catedral Metropolitana del Divino Salvador del Mundo",
            "Catedral de San Salvador",
            "Catedral Metropolitana de San Salvador",
            "Katedral San Salvador",
        )
        val results = sanSalvador.search("Catedral")
        val cathedral = results.filter { it.name in cathedralNames }
        assertEquals(results.joinToString { it.name }, 1, cathedral.size)
        assertEquals("Catedral Metropolitana del Divino Salvador del Mundo", cathedral.first().name)
    }

    // ------------------------------------------------------------------------------------ Paris

    @Test
    fun parisSearchWithoutAccents() {
        assertEquals("Châtelet", paris.search("Chatelet").first().name)
        assertEquals("Théâtre du Châtelet", paris.search("Theatre du Chatelet").first().name)
        assertEquals("Hôtel Victoria Châtelet", paris.search("Hotel Victoria Chatelet").first().name)
    }

    @Test
    fun parisSearchHouseNumberBeforeOrAfterStreet() {
        for (typed in listOf("45 Rue de Rivoli", "Rue de Rivoli 45")) {
            assertEquals(typed, "45 Rue de Rivoli", paris.search(typed).first().name)
        }
    }

    @Test
    fun parisReverseGeocodeHouseNumber() {
        assertEquals(
            "2 Rue Jean Lantier",
            paris.reverseGeocode(LngLatAlt(2.34695702791214, 48.85846994039124)).name
        )
    }

    @Test
    fun parisSearchLastWordOfStreetName() {
        val results = paris.search("Rivoli")
        assertTrue(results.joinToString { it.name }, results.any { it.name == "Rue de Rivoli" })
    }

    // ----------------------------------------------------------------------------- Buenos Aires

    @Test
    fun buenosAiresSearchEndOfStreetName() {
        assertEquals("Avenida Corrientes", buenosAires.search("Corrientes").first().name)
    }

    @Test
    fun buenosAiresAddress() {
        for (phone in listOf("en-GB", "es-AR")) {
            withDefaultLocale(phone) {
                assertEquals(
                    phone,
                    "Avenida Corrientes 1155, San Nicolás",
                    buenosAires.search("Teatro Broadway").first().description
                )
                assertEquals(
                    phone,
                    "Avenida Corrientes 1124",
                    buenosAires.reverseGeocode(LngLatAlt(-58.382594883441925, -34.6039276783525)).name
                )
            }
        }
    }

    @Test
    fun buenosAiresSearchStreetNamedAfterDate() {
        assertEquals(
            "Avenida 9 de Julio",
            buenosAires.search("Avenida 9 de Julio").firstOrNull()?.name
        )
    }

    @Test
    fun buenosAiresSearchStationAndStreetOfTheSameName() {
        // The station is on the street of the same name, 30m away
        val results = buenosAires.search("Carlos Pellegrini")
        assertTrue(results.joinToString { it.name }, results.any { it.name == "Carlos Pellegrini" })
    }

    @Test
    fun buenosAiresSearchObelisco() {
        // "Terminal de Combis Obelisco" mustn't hide the Obelisco itself
        val results = buenosAires.search("Obelisco")
        assertTrue(results.joinToString { it.name }, results.any { it.name == "Obelisco" })
    }

    // ------------------------------------------------------------------------------------ Osaka

    @Test
    fun osakaSearchInJapanese() {
        assertEquals("大阪駅", osaka.search("大阪駅").first().name)
    }

    @Test
    fun osakaSearchByEnglishName() {
        // The station's name:en - the result is named in whichever language matched
        assertEquals("Osaka Station", osaka.search("Osaka Station").first().name)
    }

    @Test
    fun osakaSearchTypedFullOrHalfWidth() {
        // Japanese keyboards can type Latin letters full width, and katakana half width
        assertEquals("MUJI", osaka.search("ＭＵＪＩ").first().name)
        assertEquals("ルクア大阪", osaka.search("ﾙｸｱ").first().name)
    }

    @Test
    fun osakaSearchWithIdeographicSpace() {
        // The space bar on a Japanese keyboard types the full-width U+3000
        assertEquals("ホテル イビス 大阪 梅田", osaka.search("ホテル　イビス").first().name)
    }

    @Test
    fun osakaSearchWithOrWithoutTheSpacesInAName() {
        // Japanese is typed without spaces between its words, whether or not the name in the map
        // has them - and a name without them can be looked for with them
        assertEquals("ホテル イビス 大阪 梅田", osaka.search("ホテルイビス").first().name)
        val results = osaka.search("大阪 梅田")
        assertTrue(results.joinToString { it.name }, results.any { it.name == "大阪梅田" })
    }

    @Test
    fun osakaAddressIsWrittenLargestFirst() {
        // Japanese addresses go from the largest place to the smallest, whatever language the phone
        // is set to - the names in them are in Japanese either way
        for (phone in listOf("en-GB", "ja-JP")) {
            withDefaultLocale(phone) {
                assertEquals(
                    phone,
                    "北区, 創造のみち, 20",
                    osaka.search("グランフロント大阪郵便局").first().description
                )
            }
        }
    }

    @Test
    fun osakaHouseNumberWithoutAStreetIsKept() {
        // Most Japanese addresses number the building within its block rather than along a street,
        // so their house numbers have no addr:street - 88 of the 101 in the tile around Osaka
        // station. The Festival Tower's is 18.
        val tileX = 14358
        val tileY = 6506
        val reader = PmTilesReader("$offlineExtractPath/osaka-prefecture-jp.pmtiles".toPath())
        val tile = decompressTile(reader.tileCompression, reader.getTile(MAX_ZOOM_LEVEL, tileX, tileY)!!)!!
        reader.close()

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, streetNumberMap, true, MAX_ZOOM_LEVEL)

        val festivalTower = streetNumberMap.values.flatMap { it.features }.map { it as MvtFeature }
            .firstOrNull { (it.osmId == 941884472L) && (it.superCategory == SuperCategoryId.HOUSENUMBER) }
        assertEquals("18", festivalTower?.housenumber)
    }

    @Test
    fun osakaSearchWordInsideName() {
        // The Hanshin and Hankyu stations are both 大阪梅田, and it's 梅田 people look for
        val results = osaka.search("梅田")
        assertTrue(results.joinToString { it.name }, results.any { it.name == "大阪梅田" })
    }

    // ---------------------------------------------------------------------------------- Transit

    @Test
    fun subwayStationCallouts() {
        // Tehran Metro, Paris Métro, Buenos Aires Subte and Osaka Metro stations are all
        // railway=subway
        assertEquals("میدان انقلاب اسلامی Subway Station", tehran.nearestSubwayStation())
        assertEquals("Châtelet Subway Station", paris.nearestSubwayStation())
        assertEquals("Carlos Pellegrini Subway Station", buenosAires.nearestSubwayStation())
        assertEquals("梅田 Subway Station", osaka.nearestSubwayStation())
    }

    @Test
    fun osakaCalloutsAreInTheAppLanguage() {
        // The station's name:en, where the Japanese app above gets 梅田
        assertEquals("Umeda Subway Station", osakaInEnglish.nearestSubwayStation())
    }
}
