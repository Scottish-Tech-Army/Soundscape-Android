package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFilteringTest {

    private fun mvtFeature(osmId: Long, featureValue: String? = null): MvtFeature {
        return MvtFeature().apply {
            this.osmId = osmId
            this.featureValue = featureValue
        }
    }

    // --- featureIsInFilterGroup ---

    @Test
    fun featureIsInFilterGroupMatchesTransitTags() {
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "bus_stop"), "transit"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, "train_station"), "transit"))
        assertFalse(featureIsInFilterGroup(mvtFeature(3, "cafe"), "transit"))
    }

    @Test
    fun featureIsInFilterGroupMatchesFoodAndDrinkTags() {
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "cafe"), "food_and_drink"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, "pub"), "food_and_drink"))
        assertFalse(featureIsInFilterGroup(mvtFeature(3, "bank"), "food_and_drink"))
    }

    @Test
    fun featureIsInFilterGroupMatchesParksTags() {
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "park"), "parks"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, "nature_reserve"), "parks"))
        assertFalse(featureIsInFilterGroup(mvtFeature(3, "supermarket"), "parks"))
    }

    @Test
    fun featureIsInFilterGroupMatchesGroceriesTags() {
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "supermarket"), "groceries"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, "convenience"), "groceries"))
        assertFalse(featureIsInFilterGroup(mvtFeature(3, "park"), "groceries"))
    }

    @Test
    fun featureIsInFilterGroupMatchesBanksTags() {
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "bank"), "banks"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, "atm"), "banks"))
        assertFalse(featureIsInFilterGroup(mvtFeature(3, "cafe"), "banks"))
    }

    @Test
    fun featureIsInFilterGroupWithUnknownFilterAlwaysReturnsTrue() {
        // The `else` branch in featureIsInFilterGroup produces an empty tag list, and the
        // function treats an empty tag list as "no filtering applied" -> true.
        assertTrue(featureIsInFilterGroup(mvtFeature(1, "cafe"), "not_a_real_filter"))
        assertTrue(featureIsInFilterGroup(mvtFeature(2, null), ""))
    }

    @Test
    fun featureIsInFilterGroupWithNullFeatureValueDoesNotMatchNamedFilter() {
        assertFalse(featureIsInFilterGroup(mvtFeature(1, null), "banks"))
    }

    // --- isDuplicateByOsmId (already indirectly covered elsewhere - light direct coverage) ---

    @Test
    fun isDuplicateByOsmIdTracksSeenIds() {
        val seen = mutableSetOf<Any>()
        val featureA = mvtFeature(42)
        val featureB = mvtFeature(42)
        val featureC = mvtFeature(43)

        assertFalse(isDuplicateByOsmId(seen, featureA))
        assertTrue(isDuplicateByOsmId(seen, featureB))
        assertFalse(isDuplicateByOsmId(seen, featureC))
        assertEquals(setOf<Any>(42L, 43L), seen)
    }

    // --- deduplicateFeatureCollection ---

    @Test
    fun deduplicateFeatureCollectionKeepsFirstOccurrenceOfEachOsmId() {
        val input = FeatureCollection()
        val featureA1 = mvtFeature(1)
        val featureA2 = mvtFeature(1)
        val featureB = mvtFeature(2)
        input.features.add(featureA1)
        input.features.add(featureA2)
        input.features.add(featureB)

        val output = FeatureCollection()
        val existingSet = mutableSetOf<Any>()
        deduplicateFeatureCollection(output, input, existingSet)

        assertEquals(2, output.features.size)
        assertTrue(output.features.contains(featureA1))
        assertFalse(output.features.contains(featureA2))
        assertTrue(output.features.contains(featureB))
        assertEquals(setOf<Any>(1L, 2L), existingSet)
    }

    @Test
    fun deduplicateFeatureCollectionSkipsFeaturesAlreadyInExistingSet() {
        val input = FeatureCollection()
        val feature = mvtFeature(5)
        input.features.add(feature)

        val output = FeatureCollection()
        val existingSet = mutableSetOf<Any>(5L)
        deduplicateFeatureCollection(output, input, existingSet)

        assertTrue(output.features.isEmpty())
    }

    @Test
    fun deduplicateFeatureCollectionHandlesNullInputCollection() {
        val output = FeatureCollection()
        val existingSet = mutableSetOf<Any>()
        deduplicateFeatureCollection(output, null, existingSet)

        assertTrue(output.features.isEmpty())
        assertTrue(existingSet.isEmpty())
    }

    @Test
    fun deduplicateFeatureCollectionAppendsToExistingOutputContents() {
        val input = FeatureCollection()
        val feature = mvtFeature(9)
        input.features.add(feature)

        val output = FeatureCollection()
        val preExisting = mvtFeature(100)
        output.features.add(preExisting)

        deduplicateFeatureCollection(output, input, mutableSetOf())

        assertEquals(2, output.features.size)
        assertTrue(output.features.contains(preExisting))
        assertTrue(output.features.contains(feature))
    }

    // --- removeDuplicateOsmIds ---

    @Test
    fun removeDuplicateOsmIdsRemovesDuplicatesWithinASingleCollection() {
        val input = FeatureCollection()
        val featureA1 = mvtFeature(1)
        val featureA2 = mvtFeature(1)
        val featureB1 = mvtFeature(2)
        val featureB2 = mvtFeature(2)
        val featureC = mvtFeature(3)
        input.features.add(featureA1)
        input.features.add(featureB1)
        input.features.add(featureA2)
        input.features.add(featureC)
        input.features.add(featureB2)

        val result = removeDuplicateOsmIds(input)

        assertEquals(3, result.features.size)
        assertEquals(listOf<Feature>(featureA1, featureB1, featureC), result.features)
    }

    @Test
    fun removeDuplicateOsmIdsOnEmptyCollectionReturnsEmptyCollection() {
        val result = removeDuplicateOsmIds(FeatureCollection())
        assertTrue(result.features.isEmpty())
    }

    @Test
    fun removeDuplicateOsmIdsWithNoDuplicatesReturnsAllFeatures() {
        val input = FeatureCollection()
        val featureA = mvtFeature(1)
        val featureB = mvtFeature(2)
        input.features.add(featureA)
        input.features.add(featureB)

        val result = removeDuplicateOsmIds(input)

        assertEquals(2, result.features.size)
        assertEquals(listOf<Feature>(featureA, featureB), result.features)
    }

    // --- getPoiFeatureCollectionBySuperCategory (already indirectly covered - light direct coverage) ---

    @Test
    fun getPoiFeatureCollectionBySuperCategoryFiltersByCategory() {
        val input = FeatureCollection()
        val landmark = MvtFeature().apply { osmId = 1; superCategory = SuperCategoryId.LANDMARK }
        val place = MvtFeature().apply { osmId = 2; superCategory = SuperCategoryId.PLACE }
        input.features.add(landmark)
        input.features.add(place)

        val result = getPoiFeatureCollectionBySuperCategory(SuperCategoryId.LANDMARK, input)

        assertEquals(1, result.features.size)
        assertTrue(result.features.contains(landmark))
    }
}
