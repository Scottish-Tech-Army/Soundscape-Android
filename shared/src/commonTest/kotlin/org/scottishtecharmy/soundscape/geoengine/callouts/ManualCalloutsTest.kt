@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.Earcons
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Minimal [LocalizedStrings] stub that encodes exactly which [StringKey] (and arguments) was
 * resolved, so tests can assert on the precise callout that was built without pulling in the real
 * Compose string-resource bundle. Mirrors the equivalent fake used by the MVT-tile-backed tests in
 * app/src/test (see IntersectionsTestMvt.kt's FakeLocalizedStrings).
 */
private class FakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        "$key(${args.joinToString(", ")})"

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun resolveFeatureClass(key: String): String? = null
}

private class FakeGeocoder(private val result: LocationDescription?) : SoundscapeGeocoder() {
    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? = result
}

private fun landmark(name: String, location: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        this.name = name
        geometry = Point(location)
    }

private fun marker(name: String, location: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        this.name = name
        superCategory = SuperCategoryId.MARKER
        geometry = Point(location)
    }

class ManualCalloutsTest {

    private val strings = FakeLocalizedStrings()

    // ---- buildMyLocationCallout ----------------------------------------------------------

    @Test
    fun myLocation_invalidLocation_returnsLocationError() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))
        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = false,
            geocoder = SoundscapeGeocoder(),
            localizedStrings = strings,
            gridState = GridState(),
        )
        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.GeneralErrorFindLocationError), callout.positionedStrings[0].text)
    }

    @Test
    fun myLocation_geocoderResult_returnsFacingDirectionThenAddressName() {
        val location = LngLatAlt(-2.657, 51.430)
        val userGeometry = UserGeometry(location = location, phoneHeading = 90.0)
        val geocoded = LocationDescription(name = "1 Main Street", location = location)

        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            geocoder = FakeGeocoder(geocoded),
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(2, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.DirectionsFacingE), callout.positionedStrings[0].text)
        assertEquals("1 Main Street", callout.positionedStrings[1].text)
    }

    @Test
    fun myLocation_noGeocoder_orientationAndRoadName_returnsFacingDirectionAlongRoad() {
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 90.0,
            mapMatchedWay = Way().apply { name = "Test Road" },
        )

        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            geocoder = FakeGeocoder(null),
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(
            strings.get(StringKey.DirectionsAlongFacingE, "Test Road"),
            callout.positionedStrings[0].text
        )
    }

    @Test
    fun myLocation_noGeocoder_orientationOnly_returnsFacingDirection() {
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 90.0,
        )

        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            geocoder = FakeGeocoder(null),
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.DirectionsFacingE), callout.positionedStrings[0].text)
    }

    @Test
    fun myLocation_noGeocoder_roadNameOnly_returnsStationaryOnWay() {
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            mapMatchedWay = Way().apply { name = "Test Road" },
        )

        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            geocoder = FakeGeocoder(null),
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(
            strings.get(StringKey.StationaryOnWay, "Test Road"),
            callout.positionedStrings[0].text
        )
    }

    @Test
    fun myLocation_noGeocoder_neitherOrientationNorRoad_returnsLocationError() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))

        val callout = buildMyLocationCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            geocoder = FakeGeocoder(null),
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.GeneralErrorFindLocationError), callout.positionedStrings[0].text)
    }

    // ---- buildWhatsAroundMeCallout ---------------------------------------------------------

    @Test
    fun whatsAroundMe_invalidLocation_returnsLocationError() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))
        val callout = buildWhatsAroundMeCallout(
            userGeometry = userGeometry,
            hasValidLocation = false,
            localizedStrings = strings,
            gridState = GridState(),
        )
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.GeneralErrorFindLocationError), callout.positionedStrings[0].text)
    }

    @Test
    fun whatsAroundMe_fourDirections_returnsOneLandmarkPerDirectionInSouthWestNorthEastOrder() {
        val origin = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(origin, 0.0, 100.0)
        val east = getDestinationCoordinate(origin, 90.0, 100.0)
        val south = getDestinationCoordinate(origin, 180.0, 100.0)
        val west = getDestinationCoordinate(origin, 270.0, 100.0)

        val collection = FeatureCollection().apply {
            addFeature(landmark("North Point", north))
            addFeature(landmark("East Point", east))
            addFeature(landmark("South Point", south))
            addFeature(landmark("West Point", west))
        }

        val gridState = GridState()
        gridState.featureTrees[TreeId.PLACES_AND_LANDMARKS.id] = FeatureTree(collection)

        val userGeometry = UserGeometry(location = origin, phoneHeading = 0.0)

        val callout = buildWhatsAroundMeCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            localizedStrings = strings,
            gridState = gridState,
        )

        assertEquals(4, callout.positionedStrings.size)
        // Individual direction segments (behind/left/ahead/right) are indexed south, west, north,
        // east when heading is due north - see getIndividualDirectionSegments.
        assertTrue(callout.positionedStrings[0].text.startsWith("South Point. "))
        assertTrue(callout.positionedStrings[1].text.startsWith("West Point. "))
        assertTrue(callout.positionedStrings[2].text.startsWith("North Point. "))
        assertTrue(callout.positionedStrings[3].text.startsWith("East Point. "))
        for (positionedString in callout.positionedStrings) {
            assertEquals(Earcons.SENSE_POI, positionedString.earcon)
            assertEquals(AudioType.LOCALIZED, positionedString.type)
        }
    }

    // ---- buildAheadOfMeCallout --------------------------------------------------------------

    @Test
    fun aheadOfMe_invalidLocation_returnsLocationError() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))
        val callout = buildAheadOfMeCallout(
            userGeometry = userGeometry,
            hasValidLocation = false,
            localizedStrings = strings,
            gridState = GridState(),
        )
        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.GeneralErrorFindLocationError), callout.positionedStrings[0].text)
    }

    @Test
    fun aheadOfMe_featureAheadIncluded_featureBehindExcluded() {
        val origin = LngLatAlt(-2.657, 51.430)
        val ahead = getDestinationCoordinate(origin, 0.0, 200.0)
        val behind = getDestinationCoordinate(origin, 180.0, 200.0)

        val collection = FeatureCollection().apply {
            addFeature(landmark("Ahead Landmark", ahead))
            addFeature(landmark("Behind Landmark", behind))
        }

        val gridState = GridState()
        gridState.featureTrees[TreeId.PLACES_AND_LANDMARKS.id] = FeatureTree(collection)

        val userGeometry = UserGeometry(location = origin, phoneHeading = 0.0)

        val callout = buildAheadOfMeCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            localizedStrings = strings,
            gridState = gridState,
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertTrue(callout.positionedStrings[0].text.startsWith("Ahead Landmark. "))
        assertEquals(Earcons.SENSE_POI, callout.positionedStrings[0].earcon)
        assertEquals(AudioType.LOCALIZED, callout.positionedStrings[0].type)
    }

    @Test
    fun aheadOfMe_noFeaturesInRange_returnsNothingToCallOutNow() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430), phoneHeading = 0.0)

        val callout = buildAheadOfMeCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            localizedStrings = strings,
            gridState = GridState(),
        )

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(
            strings.get(StringKey.CalloutsNothingToCallOutNow),
            callout.positionedStrings[0].text
        )
    }

    // ---- buildNearbyMarkersCallout -----------------------------------------------------------

    @Test
    fun nearbyMarkers_invalidLocation_returnsLocationError() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))
        val callout = buildNearbyMarkersCallout(
            userGeometry = userGeometry,
            hasValidLocation = false,
            localizedStrings = strings,
            gridState = GridState(),
        )
        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.GeneralErrorFindLocationError), callout.positionedStrings[0].text)
    }

    @Test
    fun nearbyMarkers_noMarkerTree_returnsNoNearbyMarkers() {
        val userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430))
        val gridState = GridState()
        assertNull(gridState.markerTree)

        val callout = buildNearbyMarkersCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            localizedStrings = strings,
            gridState = gridState,
        )

        assertEquals(1, callout.positionedStrings.size)
        assertEquals(strings.get(StringKey.CalloutsNoNearbyMarkers), callout.positionedStrings[0].text)
    }

    @Test
    fun nearbyMarkers_markersPresent_returnsMarkerCallouts() {
        val origin = LngLatAlt(-2.657, 51.430)
        val near = getDestinationCoordinate(origin, 0.0, 50.0)
        val far = getDestinationCoordinate(origin, 90.0, 150.0)

        val collection = FeatureCollection().apply {
            addFeature(marker("Near Marker", near))
            addFeature(marker("Far Marker", far))
        }

        val gridState = GridState()
        gridState.markerTree = FeatureTree(collection)

        val userGeometry = UserGeometry(location = origin)

        val callout = buildNearbyMarkersCallout(
            userGeometry = userGeometry,
            hasValidLocation = true,
            localizedStrings = strings,
            gridState = gridState,
        )

        assertEquals(2, callout.positionedStrings.size)
        // Nearest marker first.
        assertTrue(
            callout.positionedStrings[0].text.startsWith(
                strings.get(StringKey.MarkersMarkerWithName, "Near Marker")
            )
        )
        assertTrue(
            callout.positionedStrings[1].text.startsWith(
                strings.get(StringKey.MarkersMarkerWithName, "Far Marker")
            )
        )
        for (positionedString in callout.positionedStrings) {
            assertEquals(Earcons.SENSE_POI, positionedString.earcon)
            assertEquals(AudioType.LOCALIZED, positionedString.type)
        }
    }
}
