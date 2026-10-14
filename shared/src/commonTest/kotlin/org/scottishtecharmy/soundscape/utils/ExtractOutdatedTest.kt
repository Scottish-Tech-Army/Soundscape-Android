package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtractOutdatedTest {

    private fun entry(schema: Any?): Feature = Feature().apply {
        properties = hashMapOf<String, Any?>("filename" to "glasgow-gb.pmtiles").also {
            if (schema != null) it["schema"] = schema
        }
    }

    @Test
    fun noManifestEntry_isNotOutdated() {
        assertFalse(isExtractOutdated(localSchema = 0, manifestEntry = null, wantedSchema = 2))
    }

    @Test
    fun manifestEntryWithoutSchema_isNotOutdated() {
        assertFalse(isExtractOutdated(localSchema = 0, manifestEntry = entry(null), wantedSchema = 2))
    }

    @Test
    fun sameSchemaAsServer_isNotOutdated() {
        // A routine rebuild with fresh OSM data keeps the schema, so it isn't worth prompting for
        assertFalse(isExtractOutdated(localSchema = 2, manifestEntry = entry(2), wantedSchema = 2))
    }

    @Test
    fun serverSchemaTheAppDoesNotUseYet_isNotOutdated() {
        assertFalse(isExtractOutdated(localSchema = 2, manifestEntry = entry(3), wantedSchema = 2))
    }

    @Test
    fun appWantsASchemaTheServerDoesNotHaveYet_isNotOutdated() {
        assertFalse(isExtractOutdated(localSchema = 1, manifestEntry = entry(1), wantedSchema = 2))
    }

    @Test
    fun olderThanBothServerAndApp_isOutdated() {
        assertTrue(isExtractOutdated(localSchema = 1, manifestEntry = entry(2), wantedSchema = 2))
    }

    @Test
    fun partialUpgradeTowardsTheWantedSchema_isOutdated() {
        // The server isn't at the wanted schema yet, but it is still newer than the phone's copy
        assertTrue(isExtractOutdated(localSchema = 0, manifestEntry = entry(1), wantedSchema = 2))
    }

    @Test
    fun schemaAsDoubleOrString_isUnderstood() {
        // JSON numbers can come back from the parser as Double, and a hand-edited manifest might
        // quote them
        assertTrue(isExtractOutdated(localSchema = 0, manifestEntry = entry(1.0), wantedSchema = 1))
        assertTrue(isExtractOutdated(localSchema = 0, manifestEntry = entry("1"), wantedSchema = 1))
    }

    @Test
    fun extractThatIsNotAPmtilesFile_hasSchemaZero() {
        // Missing files and unreadable ones must not throw
        assertEquals(0, readExtractSchema("/nonexistent/glasgow-gb.pmtiles"))
    }
}
