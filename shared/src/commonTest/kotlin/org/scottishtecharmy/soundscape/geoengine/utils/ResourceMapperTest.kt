package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.osm_crossing
import org.scottishtecharmy.soundscape.resources.osm_gas_station
import org.scottishtecharmy.soundscape.resources.osm_railway
import org.scottishtecharmy.soundscape.resources.osm_residential_street
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ResourceMapper]. The lookup table itself (`resourceMap`) and the miss-tracking set
 * (`unfoundKeys`) both live in the companion object, i.e. they are effectively global/static for
 * the whole test process. There is no reset/clear API exposed, so these tests:
 *  - never assert exact size/equality on [ResourceMapper.getUnfoundKeys], only `contains`/
 *    `!contains` for keys unique to this file, so they stay correct regardless of what other
 *    tests (or test ordering) have already looked up.
 *  - use nonsense keys that are extremely unlikely to collide with real OSM tag keys used
 *    elsewhere in the map or in other tests.
 */
class ResourceMapperTest {

    @Test
    fun nullKeyReturnsNull() {
        assertNull(ResourceMapper.getStringResource(null))
        assertFalse(ResourceMapper.hasResource(null))
    }

    @Test
    fun emptyKeyReturnsNullAndDoesNotThrow() {
        assertNull(ResourceMapper.getStringResource(""))
        assertFalse(ResourceMapper.hasResource(""))
    }

    @Test
    fun knownKeysResolveToExpectedResource() {
        assertEquals(Res.string.osm_crossing, ResourceMapper.getStringResource("crossing"))
        assertTrue(ResourceMapper.hasResource("crossing"))

        assertEquals(
            Res.string.osm_residential_street,
            ResourceMapper.getStringResource("residential_street"),
        )
        assertTrue(ResourceMapper.hasResource("residential_street"))

        assertEquals(Res.string.osm_railway, ResourceMapper.getStringResource("railway"))
        assertTrue(ResourceMapper.hasResource("railway"))
    }

    @Test
    fun railAndRailwayAreConsistentSynonyms() {
        // The lookup table has separate put("rail", ...) and put("railway", ...) calls - they
        // should both resolve to the same underlying resource.
        val rail = ResourceMapper.getStringResource("rail")
        val railway = ResourceMapper.getStringResource("railway")
        assertEquals(railway, rail)
        assertEquals(Res.string.osm_railway, rail)
    }

    @Test
    fun unknownKeyReturnsNullAndDoesNotThrow() {
        val unknownKey = "__resource_mapper_test_unknown_key__"
        assertNull(ResourceMapper.getStringResource(unknownKey))
        assertFalse(ResourceMapper.hasResource(unknownKey))
    }

    @Test
    fun unfoundKeysRecordsMissesAsASideEffect() {
        val unknownKey = "__resource_mapper_test_unfound_tracking_key__"

        // Not present before we look it up (assuming no earlier test used this exact key).
        assertFalse(ResourceMapper.getUnfoundKeys().contains(unknownKey))

        assertNull(ResourceMapper.getStringResource(unknownKey))

        // getStringResource records the miss as a side effect.
        assertTrue(ResourceMapper.getUnfoundKeys().contains(unknownKey))
    }

    @Test
    fun unfoundKeysDoesNotRecordHits() {
        val knownKey = "crossing"

        assertNull(ResourceMapper.getStringResource(knownKey.uppercase() + "_never_added_marker"))
        assertTrue(ResourceMapper.hasResource(knownKey))

        // A key that successfully resolves should never show up in the unfound set.
        assertFalse(ResourceMapper.getUnfoundKeys().contains(knownKey))
    }

    @Test
    fun hasResourceAlsoRecordsMissesViaGetStringResource() {
        // hasResource() is implemented in terms of getStringResource(), so calling it with an
        // unknown key should have the same unfoundKeys side effect.
        val unknownKey = "__resource_mapper_test_has_resource_unfound_key__"

        assertFalse(ResourceMapper.hasResource(unknownKey))
        assertTrue(ResourceMapper.getUnfoundKeys().contains(unknownKey))
    }

    @Test
    fun specialCasedValuesReturnNullWithoutBeingTrackedAsUnfound() {
        // "unclassified", "yes" and "no" are explicitly special-cased to null in
        // getStringResource, before the unfoundKeys tracking code runs.
        for (key in listOf("unclassified", "yes", "no")) {
            assertNull(ResourceMapper.getStringResource(key))
            assertFalse(
                ResourceMapper.getUnfoundKeys().contains(key),
                "Expected special-cased key '$key' to NOT be tracked as unfound",
            )
        }
    }

    @Test
    fun fuelResolvesToGasStationNotTheGenericDuplicateEntry() {
        // "fuel" used to appear twice in the lookup table; the later put("fuel", osm_fuel) call
        // silently overwrote the earlier, intended put("fuel", osm_gas_station) one, since the
        // table is just sequential HashMap.put() calls. The duplicate was removed so this
        // resolves to the specific "Gas Station" text, not the generic "Fuel Station" one.
        assertEquals(Res.string.osm_gas_station, ResourceMapper.getStringResource("fuel"))
    }

    @Test
    fun lookupIsCaseSensitive() {
        // getStringResource does not normalize case before doing the map lookup, so an
        // upper-cased variant of a known key is treated as a completely different (unknown) key.
        assertTrue(ResourceMapper.hasResource("crossing"))
        assertFalse(ResourceMapper.hasResource("CROSSING"))
        assertNull(ResourceMapper.getStringResource("CROSSING"))
    }
}
