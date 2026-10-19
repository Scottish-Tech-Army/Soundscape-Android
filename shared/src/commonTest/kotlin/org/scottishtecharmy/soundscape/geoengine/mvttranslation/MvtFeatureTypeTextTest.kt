package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Resolves a feature class to the name of its string resource, e.g. "osm_sports_field" */
private class ResourceNameLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String = key.name
    override fun getOrNull(key: StringKey, vararg args: Any?): String? = key.name
    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String = key.name
    override fun resolveFeatureClass(key: String): String? =
        ResourceMapper.getStringResource(key)?.key
}

/**
 * Unnamed pitches are called out by their sport, artworks by their artwork_type and information
 * POIs by their information tag, rather than as the "Sports Field", "Gallery" and "Information" of
 * their OpenMapTiles class.
 */
class MvtFeatureTypeTextTest {
    private val localized = ResourceNameLocalizedStrings()

    private fun text(
        featureClass: String,
        subclass: String?,
        properties: Map<String, Any?>? = null,
    ) = MvtFeature().apply {
        this.featureClass = featureClass
        featureSubClass = subclass
        this.properties = properties?.let { HashMap(it) }
    }.getText(localized).text

    @Test
    fun pitchIsCalledOutBySport() {
        assertEquals("Osm_pitch_skateboard", text("pitch", "skateboard"))
        assertEquals("Osm_pitch_soccer", text("pitch", "soccer"))
        assertEquals("Osm_pitch_bmx", text("pitch", "bmx"))
    }

    @Test
    fun rugbyAndHockeyVariantsShareAString() {
        assertEquals("Osm_pitch_rugby", text("pitch", "rugby_union"))
        assertEquals("Osm_pitch_rugby", text("pitch", "rugby_league"))
        assertEquals("Osm_pitch_field_hockey", text("pitch", "hockey"))
    }

    @Test
    fun pitchWithSeveralSportsUsesTheFirst() {
        assertEquals("Osm_pitch_soccer", text("pitch", "soccer;basketball"))
    }

    @Test
    fun pitchFallsBackToSportNounThenSportsField() {
        // wrestling has a sport name but no pitch name
        assertEquals("Osm_wrestling", text("pitch", "wrestling"))
        assertEquals("Osm_sports_field", text("pitch", "not_a_real_sport"))
        assertEquals("Osm_sports_field", text("pitch", null))
    }

    @Test
    fun artworkIsCalledOutByType() {
        assertEquals(
            "Osm_artwork_mural",
            text("art_gallery", "artwork", mapOf("artwork_type" to "mural"))
        )
        assertEquals(
            "Osm_artwork_statue",
            text("art_gallery", "artwork", mapOf("artwork_type" to "statue;sculpture"))
        )
    }

    @Test
    fun artworkWithoutKnownTypeIsArtwork() {
        assertEquals("Osm_artwork", text("art_gallery", "artwork"))
        assertEquals(
            "Osm_artwork",
            text("art_gallery", "artwork", mapOf("artwork_type" to "bust_of_someone"))
        )
    }

    @Test
    fun otherArtGallerySubclassesKeepTheirOwnName() {
        assertEquals("Osm_gallery", text("art_gallery", "gallery"))
        assertEquals("Osm_arts_centre", text("art_gallery", "arts_centre"))
    }

    @Test
    fun informationIsCalledOutByType() {
        assertEquals("Osm_information_guidepost", text("information", "guidepost"))
        assertEquals("Osm_information_board", text("information", "board"))
        assertEquals("Osm_information_tactile_map", text("information", "tactile_map"))
        assertEquals("Osm_information_tactile_model", text("information", "tactile_model"))
        assertEquals("Osm_information_map", text("information", "hikingmap"))
        assertEquals("Osm_information_office", text("information", "visitor_centre"))
    }

    @Test
    fun informationWithoutKnownTypeIsInformation() {
        assertEquals("Osm_information", text("information", null))
        // a subclass without its own name isn't used raw: "stele" has no string at all
        assertEquals("Osm_information", text("information", "stele"))
    }

    @Test
    fun otherClassesStillUseTheClassName() {
        // place_of_worship's subclass is the religion, not a name for the place
        assertEquals("Osm_place_of_worship", text("place_of_worship", "christian"))
    }

    @Test
    fun qualifiedKeysAreNotSearchCategories() {
        assertNull(ResourceMapper.entries()["pitch_skateboard"])
        assertNull(ResourceMapper.entries()["artwork_mural"])
        assertTrue(ResourceMapper.hasResource("pitch_skateboard"))
        assertEquals("pitch_soccer", ResourceMapper.qualifiedKey("pitch", "soccer"))
        assertNull(ResourceMapper.qualifiedKey("pitch", "wrestling"))
        assertNull(ResourceMapper.qualifiedKey("pitch", null))
        assertFalse(ResourceMapper.getUnfoundKeys().contains("pitch_wrestling"))
    }
}
