package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the street line LocationDescription.process() builds for POIs with no address of their
 * own, from the way and settlement GridState.attachNearestWays() associated at tile load time.
 */
class LocationDescriptionStreetTest {

    private val location = LngLatAlt(-2.657, 51.430)

    private fun postBox(configure: MvtFeature.() -> Unit = {}): MvtFeature =
        MvtFeature().apply {
            geometry = Point(location)
            featureClass = "post"
            configure()
        }

    @Test
    fun poiWithNoAddressTakesTheStreetAndSettlementFromItsNearestWay() {
        val feature = postBox {
            nearestWay = Way().apply { name = "London Road" }
            nearestSettlement = "Bridgeton"
        }

        val description = feature.toLocationDescription(LocationSource.OfflineGeocoder)

        assertEquals("London Road, Bridgeton", description.street)
        assertNull(description.description)
    }

    @Test
    fun refIsUsedWhenTheNearestWayHasNoName() {
        val feature = postBox {
            nearestWay = Way().apply { ref = "A81" }
            nearestSettlement = "Milngavie"
        }

        val description = feature.toLocationDescription(LocationSource.OfflineGeocoder)

        assertEquals("A81, Milngavie", description.street)
    }

    @Test
    fun bothHalvesAreNeeded() {
        // A way with no settlement, or a settlement with no way, is not shown at all
        val wayOnly = postBox { nearestWay = Way().apply { name = "London Road" } }
        val settlementOnly = postBox { nearestSettlement = "Bridgeton" }

        assertNull(wayOnly.toLocationDescription(LocationSource.OfflineGeocoder).street)
        assertNull(settlementOnly.toLocationDescription(LocationSource.OfflineGeocoder).street)
    }

    @Test
    fun poiWithNothingAssociatedHasNoStreet() {
        assertNull(postBox().toLocationDescription(LocationSource.OfflineGeocoder).street)
    }

    /**
     * A POI with an OSM address gets a formatted one instead of the confected street line. OSM
     * very often stops at addr:street with no addr:city, so the settlement recorded at tile load
     * fills that in - this is the "Kersland Drive Car Park" case, which would otherwise present
     * as a bare street with no town.
     */
    @Test
    fun poiWithItsOwnStreetGetsTheSettlementFoldedIntoItsAddress() {
        val carPark = MvtFeature().apply {
            geometry = Point(location)
            properties = hashMapOf()
            name = "Kersland Drive Car Park"
            street = "Kersland Drive"
            nearestSettlement = "Milngavie"
        }

        val description = carPark.toLocationDescription(LocationSource.OfflineGeocoder)

        assertEquals("Kersland Drive Car Park", description.name)
        assertEquals("Kersland Drive, Milngavie", description.description)
        assertNull(description.street)
    }

    @Test
    fun anOsmCityIsNotOverriddenByTheSettlement() {
        val shop = MvtFeature().apply {
            geometry = Point(location)
            properties = hashMapOf<String, Any?>("city" to "Bearsden")
            housenumber = "17"
            street = "Kersland Drive"
            nearestSettlement = "Milngavie"
        }

        val description = shop.toLocationDescription(LocationSource.OfflineGeocoder)

        assertEquals("17 Kersland Drive, Bearsden", description.description)
    }
}
