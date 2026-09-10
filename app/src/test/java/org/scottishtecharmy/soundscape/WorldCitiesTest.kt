package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.process

/**
 * Offline search, geocoding and callouts on the map data of cities outside the UK where
 * Soundscape has users: Tehran, San Salvador, Paris and Buenos Aires. Between them they have
 * Persian script and digits, French and Spanish accents, house numbers written after the street,
 * numbered street names and both hemispheres, none of which the UK extracts do.
 *
 * Addresses are written the way the country they're in writes them, whatever the phone is set
 * to, so the tests which check one run with the phone set to more than one country.
 */
class WorldCitiesTest {

    private class City(val location: LngLatAlt) {
        // Loading the grids is the slow part and nothing here changes them, so each city is
        // loaded once and shared between the tests.
        val gridState by lazy { getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE) }
        private val geocoder by lazy {
            val settlementState = getGridStateForLocation(location, 12, 3)
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
                .getNearestCollection(location, 500.0, 50, gridState.ruler)
                .map { it as MvtFeature }
                .firstOrNull { it.featureValue == "subway" && it.properties?.get("entrance") == null }
                ?.getText(null)?.text
    }

    companion object {
        private val tehran = City(tehranTestLocation)
        private val sanSalvador = City(sanSalvadorTestLocation)
        private val paris = City(parisTestLocation)
        private val buenosAires = City(buenosAiresTestLocation)
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

    // ---------------------------------------------------------------------------------- Transit

    @Test
    fun subwayStationCallouts() {
        // Tehran Metro, Paris Métro and Buenos Aires Subte stations are all railway=subway
        assertEquals("میدان انقلاب اسلامی Subway Station", tehran.nearestSubwayStation())
        assertEquals("Châtelet Subway Station", paris.nearestSubwayStation())
        assertEquals("Carlos Pellegrini Subway Station", buenosAires.nearestSubwayStation())
    }
}
