@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.Side
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Encodes (key, args) into a stable readable string instead of real localized text - same pattern
 * as WayNamingFakeLocalizedStrings in WayNamingTest.kt (renamed here to avoid a top-level name
 * clash in the same module).
 */
private class StreetDescriptionFakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        if (args.isEmpty()) key.name else "${key.name}(${args.joinToString(",")})"
    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)
    override fun resolveFeatureClass(key: String): String? = null
}

private fun newGridState(): GridState = GridState().apply { validateContext = false }

private fun newStreetDescription(name: String = "Test Street"): StreetDescription =
    StreetDescription(name, newGridState())

private fun house(number: String, location: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        housenumber = number
        geometry = Point(location)
    }

class StreetDescriptionTest {

    // ============================================================================================
    // sideToBool / otherSide
    // ============================================================================================

    @Test
    fun sideToBool_mapsLeftRightAndInline() {
        val sd = newStreetDescription()
        assertEquals(false, sd.sideToBool(Side.LEFT))
        assertEquals(true, sd.sideToBool(Side.RIGHT))
        assertNull(sd.sideToBool(Side.INLINE))
    }

    @Test
    fun otherSide_flipsLeftAndRightButInlineHasNoOpposite() {
        val sd = newStreetDescription()
        assertEquals(Side.RIGHT, sd.otherSide(Side.LEFT))
        assertEquals(Side.LEFT, sd.otherSide(Side.RIGHT))
        assertNull(sd.otherSide(Side.INLINE))
    }

    // ============================================================================================
    // parseHouseNumber
    // ============================================================================================

    @Test
    fun parseHouseNumber_plainNumber_parsesFully() {
        val sd = newStreetDescription()
        assertEquals(123, sd.parseHouseNumber("123"))
    }

    @Test
    fun parseHouseNumber_numberWithLetterSuffix_parsesLeadingDigitsOnly() {
        val sd = newStreetDescription()
        assertEquals(12, sd.parseHouseNumber("12A"))
    }

    @Test
    fun parseHouseNumber_noLeadingDigits_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.parseHouseNumber("A12"))
    }

    @Test
    fun parseHouseNumber_emptyString_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.parseHouseNumber(""))
    }

    @Test
    fun parseHouseNumber_zero_parsesAsZero() {
        val sd = newStreetDescription()
        assertEquals(0, sd.parseHouseNumber("0"))
    }

    @Test
    fun parseHouseNumber_leadingWhitespace_returnsNull() {
        // Documents current behaviour: parseHouseNumber does not trim/skip leading whitespace, so
        // a housenumber string with a leading space (unusual, but not impossible from dirty OSM
        // data) fails to parse at all rather than falling back to skipping the space.
        val sd = newStreetDescription()
        assertNull(sd.parseHouseNumber(" 123"))
    }

    @Test
    fun parseHouseNumber_dashedRange_parsesFirstNumberOnly() {
        val sd = newStreetDescription()
        assertEquals(12, sd.parseHouseNumber("12-14"))
    }

    // ============================================================================================
    // parseHouseNumberRange
    // ============================================================================================

    @Test
    fun parseHouseNumberRange_simpleAscendingRange() {
        val sd = newStreetDescription()
        assertEquals(Pair(12, 14), sd.parseHouseNumberRange("12-14"))
    }

    @Test
    fun parseHouseNumberRange_descendingRangeIsStillNormalisedLowToHigh() {
        val sd = newStreetDescription()
        assertEquals(Pair(12, 14), sd.parseHouseNumberRange("14-12"))
    }

    @Test
    fun parseHouseNumberRange_singleNumber_returnsSameNumberTwice() {
        val sd = newStreetDescription()
        assertEquals(Pair(12, 12), sd.parseHouseNumberRange("12"))
    }

    @Test
    fun parseHouseNumberRange_numberWithLetterSuffix_treatedAsSingleNumber() {
        val sd = newStreetDescription()
        assertEquals(Pair(12, 12), sd.parseHouseNumberRange("12A"))
    }

    @Test
    fun parseHouseNumberRange_noDigitsAtAll_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.parseHouseNumberRange("abc"))
    }

    @Test
    fun parseHouseNumberRange_emptyString_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.parseHouseNumberRange(""))
    }

    @Test
    fun parseHouseNumberRange_multipleNumbers_returnsOverallMinAndMax() {
        val sd = newStreetDescription()
        // Middle number (3) is lost, but min/max is exactly what callers (checkSortedNumberConsistency)
        // need for overlap detection.
        assertEquals(Pair(1, 5), sd.parseHouseNumberRange("1,3,5"))
    }

    // ============================================================================================
    // whichSide
    // ============================================================================================

    @Test
    fun whichSide_pointEastOfNorthHeadingLine_isRightWhenNotReversed() {
        val sd = newStreetDescription()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        val eastPoint = getDestinationCoordinate(south, 90.0, 5.0)
        val pdh = PointAndDistanceAndHeading(index = 0)

        assertEquals(Side.RIGHT, sd.whichSide(way, direction = false, pdh, eastPoint))
    }

    @Test
    fun whichSide_directionTrueSwapsStartAndEnd_flippingTheSide() {
        val sd = newStreetDescription()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        val eastPoint = getDestinationCoordinate(south, 90.0, 5.0)
        val pdh = PointAndDistanceAndHeading(index = 0)

        assertEquals(Side.LEFT, sd.whichSide(way, direction = true, pdh, eastPoint))
    }

    @Test
    fun whichSide_usesTheSegmentIdentifiedByPdhIndex() {
        val sd = newStreetDescription()
        val p0 = LngLatAlt(-2.657, 51.430)
        val p1 = getDestinationCoordinate(p0, 0.0, 50.0)
        val p2 = getDestinationCoordinate(p1, 90.0, 50.0) // second segment heads east
        val way = Way().apply { geometry = LineString(p0, p1, p2) }
        // A point just north of p1/p2's segment is on its left (heading east, north = left).
        val pointNorthOfSecondSegment = getDestinationCoordinate(p1, 0.0, 5.0)
        val pdh = PointAndDistanceAndHeading(index = 1)

        assertEquals(Side.LEFT, sd.whichSide(way, direction = false, pdh, pointNorthOfSecondSegment))
    }

    @Test
    fun whichSide_addHouseAndGetStreetNumberConventionsAgree() {
        // addHouse (called while recording a house's side during createDescription) invokes
        // whichSide(way, wayDirection, pdh, location) directly; getStreetNumber (called when
        // looking a point back up against the recorded numbers) must use the exact same
        // convention, or a house addHouse records as being on the left would be looked up by
        // getStreetNumber as being on the right, and vice versa.
        val sd = newStreetDescription()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        val eastPoint = getDestinationCoordinate(south, 90.0, 5.0)
        val pdh = PointAndDistanceAndHeading(index = 0)
        val wayDirection = true

        val sideRecordedByAddHouse = sd.whichSide(way, wayDirection, pdh, eastPoint)
        // getStreetNumber's own direction lookup for the same way (see its `for (member in ways)`
        // loop) yields the same wayDirection value stored in the ways list - passed to whichSide
        // as-is, not negated.
        val sideLookedUpByGetStreetNumber = sd.whichSide(way, wayDirection, pdh, eastPoint)

        assertEquals(Side.LEFT, sideRecordedByAddHouse)
        assertEquals(sideRecordedByAddHouse, sideLookedUpByGetStreetNumber)
    }

    // ============================================================================================
    // distanceAlongLine
    // ============================================================================================

    @Test
    fun distanceAlongLine_singleSegmentForwardWay() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val p0 = LngLatAlt(-2.657, 51.430)
        val p1 = getDestinationCoordinate(p0, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(p0, p1) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val pdh = PointAndDistanceAndHeading(index = 0, positionAlongLine = 0.3)
        val expected = 0.3 * ruler.distance(p0, p1)

        assertEquals(expected, sd.distanceAlongLine(way, pdh), 0.001)
    }

    @Test
    fun distanceAlongLine_reversedWaySubtractsFromLength() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val p0 = LngLatAlt(-2.657, 51.430)
        val p1 = getDestinationCoordinate(p0, 0.0, 40.0)
        val segmentLength = ruler.distance(p0, p1)
        val way = Way().apply {
            geometry = LineString(p0, p1)
            length = segmentLength
        }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, false))

        val pdh = PointAndDistanceAndHeading(index = 0, positionAlongLine = 0.25)
        val expected = segmentLength - (0.25 * segmentLength)

        assertEquals(expected, sd.distanceAlongLine(way, pdh), 0.001)
    }

    @Test
    fun distanceAlongLine_addsFullLengthOfPrecedingWays() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val wayA = Way().apply { length = 15.0 }
        val p0 = LngLatAlt(-2.657, 51.430)
        val p1 = getDestinationCoordinate(p0, 0.0, 60.0)
        val wayB = Way().apply { geometry = LineString(p0, p1) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(wayA, true))
        sd.ways.add(Pair(wayB, true))

        val pdh = PointAndDistanceAndHeading(index = 0, positionAlongLine = 0.3)
        val expected = 15.0 + (0.3 * ruler.distance(p0, p1))

        assertEquals(expected, sd.distanceAlongLine(wayB, pdh), 0.001)
    }

    @Test
    fun distanceAlongLine_multiSegmentWayAccumulatesFullPriorSegments() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val p0 = LngLatAlt(-2.657, 51.430)
        val p1 = getDestinationCoordinate(p0, 0.0, 50.0)
        val p2 = getDestinationCoordinate(p1, 90.0, 50.0)
        val way = Way().apply { geometry = LineString(p0, p1, p2) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val pdh = PointAndDistanceAndHeading(index = 1, positionAlongLine = 1.5)
        val expected = ruler.distance(p0, p1) + (0.5 * ruler.distance(p1, p2))

        assertEquals(expected, sd.distanceAlongLine(way, pdh), 0.001)
    }

    @Test
    fun distanceAlongLine_wayNotInList_sumsAllWayLengths() {
        val sd = newStreetDescription()
        sd.ways.add(Pair(Way().apply { length = 10.0 }, true))
        sd.ways.add(Pair(Way().apply { length = 20.0 }, true))
        val unrelatedWay = Way().apply { length = 5.0 }

        assertEquals(30.0, sd.distanceAlongLine(unrelatedWay, PointAndDistanceAndHeading()), 0.001)
    }

    // ============================================================================================
    // nearestWayOnStreet
    // ============================================================================================

    @Test
    fun nearestWayOnStreet_nullLocation_returnsNull() {
        val sd = newStreetDescription()
        sd.ways.add(Pair(Way().apply { geometry = LineString(LngLatAlt(0.0, 0.0), LngLatAlt(0.0, 1.0)) }, true))
        assertNull(sd.nearestWayOnStreet(null))
    }

    @Test
    fun nearestWayOnStreet_noWays_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.nearestWayOnStreet(LngLatAlt(-2.657, 51.430)))
    }

    @Test
    fun nearestWayOnStreet_picksClosestOfSeveralWays() {
        val sd = newStreetDescription()
        val origin = LngLatAlt(-2.657, 51.430)
        val farAway = getDestinationCoordinate(origin, 90.0, 500.0)
        val nearWayStart = origin
        val nearWayEnd = getDestinationCoordinate(origin, 0.0, 30.0)
        val farWayStart = getDestinationCoordinate(farAway, 0.0, 0.0)
        val farWayEnd = getDestinationCoordinate(farAway, 0.0, 30.0)

        val nearWay = Way().apply { geometry = LineString(nearWayStart, nearWayEnd) }
        val farWay = Way().apply { geometry = LineString(farWayStart, farWayEnd) }
        sd.ways.add(Pair(farWay, true))
        sd.ways.add(Pair(nearWay, true))

        val result = sd.nearestWayOnStreet(getDestinationCoordinate(origin, 0.0, 5.0))
        assertNotNull(result)
        assertEquals(nearWay, result.first)
    }

    // ============================================================================================
    // distanceAlongStreet
    // ============================================================================================

    @Test
    fun distanceAlongStreet_nullStartPoint_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.distanceAlongStreet(null, 10.0, sd.gridState.ruler))
    }

    @Test
    fun distanceAlongStreet_noNearbyWay_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.distanceAlongStreet(LngLatAlt(-2.657, 51.430), 10.0, sd.gridState.ruler))
    }

    @Test
    fun distanceAlongStreet_withinFirstWay_returnsPointAlongIt() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(start, end); length = ruler.distance(start, end) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val result = sd.distanceAlongStreet(start, 10.0, ruler)
        val expected = ruler.along(way.geometry as LineString, 10.0)
        assertNotNull(result)
        assertEquals(expected.longitude, result.longitude, 0.0000001)
        assertEquals(expected.latitude, result.latitude, 0.0000001)
    }

    @Test
    fun distanceAlongStreet_spansIntoSecondWay_correctlyCarriesRemainder() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val start = LngLatAlt(-2.657, 51.430)
        val mid = getDestinationCoordinate(start, 0.0, 10.0)
        val end = getDestinationCoordinate(mid, 0.0, 50.0)
        val wayA = Way().apply { geometry = LineString(start, mid); length = ruler.distance(start, mid) }
        val wayB = Way().apply { geometry = LineString(mid, end); length = ruler.distance(mid, end) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(wayA, true))
        sd.ways.add(Pair(wayB, true))

        // Total distance 25m: 10m consumes the whole of wayA, remaining 15m into wayB.
        val result = sd.distanceAlongStreet(start, 25.0, ruler)
        val expected = ruler.along(wayB.geometry as LineString, 15.0)
        assertNotNull(result)
        assertEquals(expected.longitude, result.longitude, 0.0000001)
        assertEquals(expected.latitude, result.latitude, 0.0000001)
    }

    @Test
    fun distanceAlongStreet_manyShortSegments_interpolatesCorrectly() {
        // The loop compares the *remaining* distance (distanceLeft) against each subsequent
        // way's length to decide whether to keep consuming ways or stop and interpolate within
        // the current one. A street made up of several segments that are each individually
        // shorter than the requested total `distance` (a very ordinary situation on a real
        // street with many short blocks) must still resolve to the correct interpolated point.
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        var cursor = LngLatAlt(-2.657, 51.430)
        val segmentLength = 10.0
        repeat(4) {
            val next = getDestinationCoordinate(cursor, 0.0, segmentLength)
            val way = Way().apply {
                geometry = LineString(cursor, next)
                length = ruler.distance(cursor, next)
            }
            sd.ways.add(Pair(way, true))
            cursor = next
        }
        sd.gridState.ruler = ruler
        val start = (sd.ways.first().first.geometry as LineString).coordinates[0]

        // ~5m into the third way (10 + 10 = 20 consumed, 5 remaining of the requested 25m) -
        // computed via the same floating-point path distanceAlongStreet itself takes (way.length
        // is a geodesic distance, not exactly 10.0), rather than a literal 5.0, to avoid an
        // unrelated sub-millimetre precision mismatch against the 1e-7 degree tolerance below.
        val thirdWay = sd.ways[2].first
        val distanceLeftAtThirdWay = 25.0 - sd.ways[0].first.length - sd.ways[1].first.length
        val expected = ruler.along(thirdWay.geometry as LineString, distanceLeftAtThirdWay)
        val result = sd.distanceAlongStreet(start, 25.0, ruler)

        assertNotNull(result)
        assertEquals(expected.longitude, result.longitude, 0.0000001)
        assertEquals(expected.latitude, result.latitude, 0.0000001)
    }

    // ============================================================================================
    // assignHouseNumberModes
    // ============================================================================================

    @Test
    fun assignHouseNumberModes_evenOnLeftOddOnRight() {
        val sd = newStreetDescription()
        sd.assignHouseNumberModes(odd = arrayOf(0, 5), even = arrayOf(3, 0))
        assertEquals(StreetDescription.HouseNumberMode.EVEN, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.ODD, sd.rightMode)
    }

    @Test
    fun assignHouseNumberModes_oddOnLeftEvenOnRight() {
        val sd = newStreetDescription()
        sd.assignHouseNumberModes(odd = arrayOf(5, 0), even = arrayOf(0, 3))
        assertEquals(StreetDescription.HouseNumberMode.ODD, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.EVEN, sd.rightMode)
    }

    @Test
    fun assignHouseNumberModes_bothParitiesOnBothSides_isMixed() {
        val sd = newStreetDescription()
        sd.assignHouseNumberModes(odd = arrayOf(3, 2), even = arrayOf(1, 4))
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.rightMode)
    }

    @Test
    fun assignHouseNumberModes_fewerThanTwoHouseNumbers_staysMixed() {
        val sd = newStreetDescription()
        sd.assignHouseNumberModes(odd = arrayOf(1, 0), even = arrayOf(0, 0))
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.rightMode)
    }

    @Test
    fun assignHouseNumberModes_noHouseNumbersAtAll_staysMixed() {
        val sd = newStreetDescription()
        sd.assignHouseNumberModes(odd = arrayOf(0, 0), even = arrayOf(0, 0))
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.MIXED, sd.rightMode)
    }

    // ============================================================================================
    // checkSortedNumberConsistency
    // ============================================================================================

    @Test
    fun checkSortedNumberConsistency_noConfidentHouses_returnsEmptyMap() {
        val sd = newStreetDescription()
        val numbers = mapOf(
            0.0 to house("10", LngLatAlt(0.0, 0.0)).apply { streetConfidence = false },
            10.0 to house("12", LngLatAlt(0.0, 0.0)).apply { streetConfidence = false },
        )
        assertTrue(sd.checkSortedNumberConsistency(numbers).isEmpty())
    }

    @Test
    fun checkSortedNumberConsistency_consistentAscendingNumbers_returnsUnchanged() {
        val sd = newStreetDescription()
        val numbers = mapOf(
            0.0 to house("10", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            10.0 to house("12", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            20.0 to house("14", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
        )
        val result = sd.checkSortedNumberConsistency(numbers)
        assertEquals(listOf("10", "12", "14"), result.values.map { it.housenumber })
    }

    @Test
    fun checkSortedNumberConsistency_removesHousesOutsideConfidentRange() {
        val sd = newStreetDescription()
        val numbers = mapOf(
            -5.0 to house("99", LngLatAlt(0.0, 0.0)).apply { streetConfidence = false },
            0.0 to house("10", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            10.0 to house("12", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            20.0 to house("101", LngLatAlt(0.0, 0.0)).apply { streetConfidence = false },
        )
        val result = sd.checkSortedNumberConsistency(numbers)
        assertEquals(listOf("10", "12"), result.values.map { it.housenumber })
    }

    @Test
    fun checkSortedNumberConsistency_lowConfidenceReversalIsRemoved() {
        val sd = newStreetDescription()
        val numbers = linkedMapOf(
            0.0 to house("10", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            10.0 to house("20", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            // A spuriously-matched low-confidence number that momentarily reverses the trend.
            20.0 to house("5", LngLatAlt(0.0, 0.0)).apply { streetConfidence = false },
            30.0 to house("22", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
        )
        val result = sd.checkSortedNumberConsistency(numbers)
        assertEquals(listOf("10", "20", "22"), result.values.map { it.housenumber })
    }

    @Test
    fun checkSortedNumberConsistency_highConfidenceReversalIsKeptDespiteInconsistency() {
        // Documents that a reversal is only "corrected" by dropping the offending entry when that
        // entry lacks streetConfidence - a confident number is trusted and kept even though it
        // makes the overall sequence directionally inconsistent.
        val sd = newStreetDescription()
        val numbers = linkedMapOf(
            0.0 to house("10", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            10.0 to house("20", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            20.0 to house("5", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
            30.0 to house("22", LngLatAlt(0.0, 0.0)).apply { streetConfidence = true },
        )
        val result = sd.checkSortedNumberConsistency(numbers)
        assertEquals(listOf("10", "20", "5", "22"), result.values.map { it.housenumber })
    }

    // ============================================================================================
    // getInterpolateLocation
    // ============================================================================================

    @Test
    fun getInterpolateLocation_emptyMap_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.getInterpolateLocation(12, emptyMap()))
    }

    @Test
    fun getInterpolateLocation_exactMatch_returnsThatHousesLocation() {
        val sd = newStreetDescription()
        val pointA = LngLatAlt(-2.657, 51.430)
        val pointB = getDestinationCoordinate(pointA, 0.0, 20.0)
        val numbers = mapOf(0.0 to house("10", pointA), 20.0 to house("14", pointB))

        val result = sd.getInterpolateLocation(14, numbers)
        assertNotNull(result)
        assertEquals(pointB, result.first)
        assertEquals("14", result.second)
    }

    @Test
    fun getInterpolateLocation_needleOutsideRange_returnsNull() {
        val sd = newStreetDescription()
        val pointA = LngLatAlt(-2.657, 51.430)
        val pointB = getDestinationCoordinate(pointA, 0.0, 20.0)
        val numbers = mapOf(0.0 to house("10", pointA), 20.0 to house("14", pointB))

        assertNull(sd.getInterpolateLocation(5, numbers))
    }

    @Test
    fun getInterpolateLocation_interpolatesBetweenTwoKnownNumbers() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(start, end); length = ruler.distance(start, end) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val pointA = start
        val pointB = getDestinationCoordinate(start, 0.0, 20.0)
        val numbers = mapOf(0.0 to house("10", pointA), 20.0 to house("14", pointB))

        // needle=12 is halfway between 10 and 14, so expected distance is halfway between the two
        // keys (0.0 and 20.0), i.e. 10m along the way from pointA.
        val result = sd.getInterpolateLocation(12, numbers)
        assertNotNull(result)
        assertEquals("12", result.second)
        val expectedLocation = ruler.along(way.geometry as LineString, 10.0)
        assertEquals(expectedLocation.longitude, result.first.longitude, 0.00001)
        assertEquals(expectedLocation.latitude, result.first.latitude, 0.00001)
    }

    // ============================================================================================
    // getLocationFromStreetNumber
    // ============================================================================================

    @Test
    fun getLocationFromStreetNumber_invalidHouseNumberString_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.getLocationFromStreetNumber("not-a-number"))
    }

    @Test
    fun getLocationFromStreetNumber_onlyLeftHasAMatch_returnsLeft() {
        val sd = newStreetDescription()
        val point = LngLatAlt(-2.657, 51.430)
        sd.leftSortedNumbers = mapOf(0.0 to house("14", point))
        val result = sd.getLocationFromStreetNumber("14")
        assertNotNull(result)
        assertEquals(point, result.first)
    }

    @Test
    fun getLocationFromStreetNumber_onlyRightHasAMatch_returnsRight() {
        val sd = newStreetDescription()
        val point = LngLatAlt(-2.657, 51.430)
        sd.rightSortedNumbers = mapOf(0.0 to house("14", point))
        val result = sd.getLocationFromStreetNumber("14")
        assertNotNull(result)
        assertEquals(point, result.first)
    }

    @Test
    fun getLocationFromStreetNumber_neitherSideHasAMatch_returnsNull() {
        val sd = newStreetDescription()
        sd.leftSortedNumbers = mapOf(0.0 to house("99", LngLatAlt(0.0, 0.0)))
        sd.rightSortedNumbers = mapOf(0.0 to house("101", LngLatAlt(0.0, 0.0)))
        assertNull(sd.getLocationFromStreetNumber("14"))
    }

    @Test
    fun getLocationFromStreetNumber_evenNumber_prefersLeftWhenLeftModeIsEven() {
        val sd = newStreetDescription()
        val leftPoint = LngLatAlt(1.0, 1.0)
        val rightPoint = LngLatAlt(2.0, 2.0)
        sd.leftSortedNumbers = mapOf(0.0 to house("14", leftPoint))
        sd.rightSortedNumbers = mapOf(0.0 to house("14", rightPoint))
        sd.leftMode = StreetDescription.HouseNumberMode.EVEN
        sd.rightMode = StreetDescription.HouseNumberMode.ODD

        val result = sd.getLocationFromStreetNumber("14")
        assertNotNull(result)
        assertEquals(leftPoint, result.first)
    }

    @Test
    fun getLocationFromStreetNumber_evenNumber_prefersRightWhenLeftModeIsOdd() {
        val sd = newStreetDescription()
        val leftPoint = LngLatAlt(1.0, 1.0)
        val rightPoint = LngLatAlt(2.0, 2.0)
        sd.leftSortedNumbers = mapOf(0.0 to house("14", leftPoint))
        sd.rightSortedNumbers = mapOf(0.0 to house("14", rightPoint))
        sd.leftMode = StreetDescription.HouseNumberMode.ODD
        sd.rightMode = StreetDescription.HouseNumberMode.EVEN

        val result = sd.getLocationFromStreetNumber("14")
        assertNotNull(result)
        assertEquals(rightPoint, result.first)
    }

    @Test
    fun getLocationFromStreetNumber_oddNumber_prefersLeftWhenLeftModeIsOdd() {
        val sd = newStreetDescription()
        val leftPoint = LngLatAlt(1.0, 1.0)
        val rightPoint = LngLatAlt(2.0, 2.0)
        sd.leftSortedNumbers = mapOf(0.0 to house("15", leftPoint))
        sd.rightSortedNumbers = mapOf(0.0 to house("15", rightPoint))
        sd.leftMode = StreetDescription.HouseNumberMode.ODD
        sd.rightMode = StreetDescription.HouseNumberMode.EVEN

        val result = sd.getLocationFromStreetNumber("15")
        assertNotNull(result)
        assertEquals(leftPoint, result.first)
    }

    @Test
    fun getLocationFromStreetNumber_oddNumber_prefersRightWhenLeftModeIsEven() {
        val sd = newStreetDescription()
        val leftPoint = LngLatAlt(1.0, 1.0)
        val rightPoint = LngLatAlt(2.0, 2.0)
        sd.leftSortedNumbers = mapOf(0.0 to house("15", leftPoint))
        sd.rightSortedNumbers = mapOf(0.0 to house("15", rightPoint))
        sd.leftMode = StreetDescription.HouseNumberMode.EVEN
        sd.rightMode = StreetDescription.HouseNumberMode.ODD

        val result = sd.getLocationFromStreetNumber("15")
        assertNotNull(result)
        assertEquals(rightPoint, result.first)
    }

    // ============================================================================================
    // getStreetNumber
    // ============================================================================================

    @Test
    fun getStreetNumber_wayNotInWaysList_returnsEmptyResult() {
        val sd = newStreetDescription()
        val way = Way().apply {
            geometry = LineString(LngLatAlt(-2.657, 51.430), LngLatAlt(-2.657, 51.431))
        }
        val result = sd.getStreetNumber(way, LngLatAlt(-2.657, 51.4305))
        assertEquals(Pair("", false), result)
    }

    @Test
    fun getStreetNumber_exactNearbyHouseOnLocationSide_returnsItWithoutCrossingIndicated() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north); length = ruler.distance(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        // A point five metres east of the 40m mark: with wayDirection=true, whichSide puts this
        // on the LEFT (matching addHouse's convention, which getStreetNumber must also use).
        val queryPoint = getDestinationCoordinate(getDestinationCoordinate(south, 0.0, 40.0), 90.0, 5.0)
        val nearbyPoint = getDestinationCoordinate(south, 0.0, 41.0)
        sd.leftSortedNumbers = mapOf(41.0 to house("26", nearbyPoint))

        val result = sd.getStreetNumber(way, queryPoint)
        assertEquals("26", result.first)
        assertFalse(result.second)
    }

    @Test
    fun getStreetNumber_fallsBackToOtherSideWhenPreferredSideIsEmpty() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north); length = ruler.distance(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val queryPoint = getDestinationCoordinate(getDestinationCoordinate(south, 0.0, 40.0), 90.0, 5.0)
        val nearbyPoint = getDestinationCoordinate(south, 0.0, 41.0)
        // Nothing on the left (the location's own side, per whichSide) - only the right has a
        // number, so getStreetNumber must fall back and report that it's on the other side.
        sd.rightSortedNumbers = mapOf(41.0 to house("15", nearbyPoint))

        val result = sd.getStreetNumber(way, queryPoint)
        assertEquals("15", result.first)
        assertTrue(result.second)
    }

    @Test
    fun getStreetNumber_interpolatesBetweenFloorAndCeiling() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north); length = ruler.distance(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val queryPoint = getDestinationCoordinate(getDestinationCoordinate(south, 0.0, 40.0), 90.0, 5.0)
        val floorPoint = getDestinationCoordinate(south, 0.0, 20.0)
        val ceilingPoint = getDestinationCoordinate(south, 0.0, 60.0)
        sd.rightSortedNumbers = mapOf(
            20.0 to house("10", floorPoint),
            60.0 to house("18", ceilingPoint),
        )

        val result = sd.getStreetNumber(way, queryPoint)
        // floorDistance=20, ceilingDistance=20 -> adjustment=0.5 -> (18-10)*0.5=4, rounded to the
        // nearest even step -> 10 + 4 = 14.
        assertEquals("14", result.first)
    }

    // ============================================================================================
    // getIntersectionText
    // ============================================================================================

    @Test
    fun getIntersectionText_nullIntersection_returnsNull() {
        val sd = newStreetDescription()
        assertNull(sd.getIntersectionText(null, Way(), null))
    }

    @Test
    fun getIntersectionText_wayIsNull_skipsCrossStreetSearchAndUsesFallback() {
        val sd = newStreetDescription()
        val crossStreet = Way().apply { name = "Oak Avenue" }
        val intersection = Intersection().apply {
            name = "Test Street/Oak Avenue"
            members = mutableListOf(crossStreet)
        }
        assertEquals("Near intersection of Test Street/Oak Avenue", sd.getIntersectionText(intersection, null, null))
    }

    @Test
    fun getIntersectionText_findsDifferentlyNamedCrossStreet() {
        val sd = newStreetDescription()
        val ownStreet = Way().apply { name = "Test Street" }
        val crossStreet = Way().apply { name = "Oak Avenue" }
        val intersection = Intersection().apply {
            name = "Test Street/Oak Avenue"
            members = mutableListOf(ownStreet, crossStreet)
        }
        assertEquals("Oak Avenue", sd.getIntersectionText(intersection, ownStreet, null))
    }

    @Test
    fun getIntersectionText_crossStreetHasNoUsableName_fallsBackToIntersectionName() {
        val sd = newStreetDescription()
        val ownStreet = Way().apply { name = "Test Street" }
        // No name/ref/featureClass at all, so getName(nonGenericOnly = true) returns "". It still
        // needs a real LineString geometry though - the unnamed-way path in getName() calls
        // confectNamesForRoad(), which (via addPoiDestinations()) unconditionally casts
        // way.geometry to LineString.
        val unnamedCrossing = Way().apply {
            geometry = LineString(LngLatAlt(-2.0, 51.0), LngLatAlt(-2.0, 51.001))
        }
        val intersection = Intersection().apply {
            name = "Test Street junction"
            members = mutableListOf(ownStreet, unnamedCrossing)
        }
        assertEquals("Near intersection of Test Street junction", sd.getIntersectionText(intersection, ownStreet, null))
    }

    @Test
    fun getIntersectionText_usesLocalizedStringForFallback() {
        val sd = newStreetDescription()
        val intersection = Intersection().apply { name = "Test Street junction" }
        val strings = StreetDescriptionFakeLocalizedStrings()
        assertEquals(
            "StreetDescriptionIntersection(Test Street junction)",
            sd.getIntersectionText(intersection, null, strings)
        )
    }

    // ============================================================================================
    // describeLocation
    // ============================================================================================

    @Test
    fun describeLocation_nullNearestWay_returnsDefaultDescription() {
        val sd = newStreetDescription()
        val result = sd.describeLocation(LngLatAlt(0.0, 0.0), 0.0, null, null)
        assertNull(result.name)
        assertEquals(StreetDescription.StreetPosition(), result.ahead)
        assertEquals(StreetDescription.StreetPosition(), result.behind)
    }

    @Test
    fun describeLocation_noDescriptivePoints_returnsDefaultPositions() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val result = sd.describeLocation(getDestinationCoordinate(south, 0.0, 50.0), 0.0, way, null)
        assertEquals(StreetDescription.StreetPosition(), result.ahead)
        assertEquals(StreetDescription.StreetPosition(), result.behind)
    }

    @Test
    fun describeLocation_travellingForward_reportsAheadAndBehindInTravelDirection() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val crossStreetIntersection = Intersection().apply { name = "Cross Street" }
        val cafe = MvtFeature().apply { name = "Cafe" }
        sd.sortedDescriptivePoints = mapOf(30.0 to crossStreetIntersection, 70.0 to cafe)

        // The way runs due north (heading 0). A heading of 0.0 matches, so the user is travelling
        // forward (towards increasing distance).
        val queryLocation = getDestinationCoordinate(south, 0.0, 50.0)
        val result = sd.describeLocation(queryLocation, 0.0, way, null)

        assertEquals("Cafe", result.ahead.name)
        assertEquals(20.0, result.ahead.distance, 0.5)
        assertEquals("Near intersection of Cross Street", result.behind.name)
        assertEquals(20.0, result.behind.distance, 0.5)
    }

    @Test
    fun describeLocation_travellingBackward_swapsAheadAndBehind() {
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))

        val crossStreetIntersection = Intersection().apply { name = "Cross Street" }
        val cafe = MvtFeature().apply { name = "Cafe" }
        sd.sortedDescriptivePoints = mapOf(30.0 to crossStreetIntersection, 70.0 to cafe)

        // Heading 180 is opposite the way's own heading (0), so this is travelling backward.
        val queryLocation = getDestinationCoordinate(south, 0.0, 50.0)
        val result = sd.describeLocation(queryLocation, 180.0, way, null)

        assertEquals("Cafe", result.behind.name)
        assertEquals("Near intersection of Cross Street", result.ahead.name)
    }

    @Test
    fun describeLocation_noHeadingProvided_defaultsToTreatingUserAsTravellingBackward() {
        // Documents the current default: when heading is null, `direction` stays at its initial
        // value of false, which the ahead/behind assignment treats the same as "travelling
        // backward" - i.e. the raw "ahead in distance" point is reported as `behind` the user.
        val sd = newStreetDescription()
        val ruler = LngLatAlt(-2.657, 51.430).createCheapRuler()
        val south = LngLatAlt(-2.657, 51.430)
        val north = getDestinationCoordinate(south, 0.0, 100.0)
        val way = Way().apply { geometry = LineString(south, north) }
        sd.gridState.ruler = ruler
        sd.ways.add(Pair(way, true))
        val cafe = MvtFeature().apply { name = "Cafe" }
        sd.sortedDescriptivePoints = mapOf(70.0 to cafe)

        val queryLocation = getDestinationCoordinate(south, 0.0, 50.0)
        val result = sd.describeLocation(queryLocation, null, way, null)

        assertEquals("Cafe", result.behind.name)
        assertEquals(StreetDescription.StreetPosition(), result.ahead)
    }

    // ============================================================================================
    // createDescription (integration - built by hand rather than via the MVT tile pipeline)
    // ============================================================================================

    /**
     * A two-segment "Test Street" running due north for 100m total, with:
     *  - a dead-end stub at the very start (no other street to discover there)
     *  - a mid-point junction where the street continues under the same name
     *  - a T-junction at the far end where a differently-named "Cross Street" joins
     */
    private class LinearStreetFixture(
        val gridState: GridState,
        val waySouth: Way,
        val wayNorth: Way,
        val intersectionEnd: Intersection,
        val pointA: LngLatAlt,
        val pointB: LngLatAlt,
        val pointC: LngLatAlt,
    )

    private fun buildLinearStreetFixture(): LinearStreetFixture {
        val pointA = LngLatAlt(-2.657, 51.430)
        val pointB = getDestinationCoordinate(pointA, 0.0, 40.0)
        val pointC = getDestinationCoordinate(pointA, 0.0, 100.0)
        val ruler = pointA.createCheapRuler()

        val waySouth = Way().apply {
            name = "Test Street"
            geometry = LineString(pointA, pointB)
            length = ruler.distance(pointA, pointB)
        }
        val wayNorth = Way().apply {
            name = "Test Street"
            geometry = LineString(pointB, pointC)
            length = ruler.distance(pointB, pointC)
        }
        val intersectionStart = Intersection().apply {
            location = pointA
            members = mutableListOf(waySouth)
        }
        val intersectionMid = Intersection().apply {
            location = pointB
            members = mutableListOf(waySouth, wayNorth)
        }
        val crossStreet = Way().apply {
            name = "Cross Street"
            geometry = LineString(
                getDestinationCoordinate(pointC, 270.0, 20.0),
                getDestinationCoordinate(pointC, 90.0, 20.0),
            )
        }
        val intersectionEnd = Intersection().apply {
            location = pointC
            members = mutableListOf(wayNorth, crossStreet)
        }

        waySouth.intersections[WayEnd.START.id] = intersectionStart
        waySouth.intersections[WayEnd.END.id] = intersectionMid
        wayNorth.intersections[WayEnd.START.id] = intersectionMid
        wayNorth.intersections[WayEnd.END.id] = intersectionEnd

        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = ruler
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(waySouth); addFeature(wayNorth); addFeature(crossStreet) }
        )

        return LinearStreetFixture(gridState, waySouth, wayNorth, intersectionEnd, pointA, pointB, pointC)
    }

    @Test
    fun createDescription_buildsOrderedWaysListAcrossASameNamedJunction() {
        val fixture = buildLinearStreetFixture()
        val sd = StreetDescription("Test Street", fixture.gridState)

        sd.createDescription(fixture.waySouth, null)

        assertEquals(listOf(fixture.waySouth, fixture.wayNorth), sd.ways.map { it.first })
        assertTrue(sd.ways.all { it.second })
    }

    @Test
    fun createDescription_onlyTheFarCrossStreetJunctionIsDescriptive() {
        val fixture = buildLinearStreetFixture()
        val sd = StreetDescription("Test Street", fixture.gridState)

        sd.createDescription(fixture.waySouth, null)

        // The start (dead end) and mid-point (same street name on both sides) junctions carry no
        // useful description; only the far T-junction with "Cross Street" does.
        assertEquals(1, sd.sortedDescriptivePoints.size)
        assertEquals(fixture.intersectionEnd, sd.sortedDescriptivePoints.values.first())
    }

    @Test
    fun createDescription_farJunctionDistanceIsTheStreetsActualTotalLength() {
        // The trailing descriptive intersection at the end of the whole street must be recorded
        // at the street's real total length (waySouth.length + wayNorth.length, ~100m here) -
        // totalDistance already has the last way's length added to it by the time this point is
        // recorded, so it must not be added again.
        val fixture = buildLinearStreetFixture()
        val sd = StreetDescription("Test Street", fixture.gridState)

        sd.createDescription(fixture.waySouth, null)

        val recordedDistance = sd.sortedDescriptivePoints.keys.first()
        val correctDistance = fixture.waySouth.length + fixture.wayNorth.length

        assertEquals(correctDistance, recordedDistance, 0.01)
    }

    @Test
    fun createDescription_assignsHouseNumbersToSidesAndDetectsOddEvenMode() {
        val fixture = buildLinearStreetFixture()
        val sd = StreetDescription("Test Street", fixture.gridState)

        // Even numbers to the east, odd numbers to the west of the (south-to-north) street. The
        // east/west pairs are deliberately placed at *different* distances along the street
        // (rather than directly opposite each other) - on a due-north line, a cheap-ruler
        // projection depends only on latitude, so an east/west pair at the same along-street
        // position would compute the exact same distanceAlongLine() value and collide as the same
        // key in the (side-unaware) houseNumberPoints map, silently dropping one of the two.
        val house12 = house("12", getDestinationCoordinate(getDestinationCoordinate(fixture.pointA, 0.0, 15.0), 90.0, 5.0))
        val house14 = house("14", getDestinationCoordinate(getDestinationCoordinate(fixture.pointA, 0.0, 35.0), 90.0, 5.0))
        val house11 = house("11", getDestinationCoordinate(getDestinationCoordinate(fixture.pointA, 0.0, 20.0), 270.0, 5.0))
        val house13 = house("13", getDestinationCoordinate(getDestinationCoordinate(fixture.pointA, 0.0, 40.0), 270.0, 5.0))
        fixture.gridState.gridStreetNumberTreeMap["Test Street"] = FeatureTree(
            FeatureCollection().apply {
                addFeature(house12); addFeature(house14); addFeature(house11); addFeature(house13)
            }
        )

        sd.createDescription(fixture.waySouth, null)

        // Due to the whichSide/addHouse orientation convention (see the dedicated whichSide test
        // documenting its "reversed" behaviour), the west-side (odd) houses land in
        // leftSortedNumbers and the east-side (even) houses land in rightSortedNumbers for this
        // south-to-north street.
        assertEquals(listOf("11", "13"), sd.leftSortedNumbers.values.map { it.housenumber })
        assertEquals(listOf("12", "14"), sd.rightSortedNumbers.values.map { it.housenumber })
        assertEquals(StreetDescription.HouseNumberMode.ODD, sd.leftMode)
        assertEquals(StreetDescription.HouseNumberMode.EVEN, sd.rightMode)
    }

    // ============================================================================================
    // describeStreet (smoke test only - the function's only observable behaviour is println output)
    // ============================================================================================

    @Test
    fun describeStreet_doesNotThrowWithPopulatedOrEmptyDescriptivePoints() {
        val sd = newStreetDescription()
        sd.describeStreet()

        val poi = MvtFeature().apply { name = "Cafe"; side = true }
        val poi2 = MvtFeature().apply { name = "Bakery"; side = false }
        val poi3 = MvtFeature().apply { name = "Statue" } // side == null
        sd.sortedDescriptivePoints = mapOf(0.0 to poi, 10.0 to poi2, 20.0 to poi3)
        sd.describeStreet()
    }
}
