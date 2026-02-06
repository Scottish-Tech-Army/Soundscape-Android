package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.IntersectionType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.Side
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getCentralPointForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getSideOfLine
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import java.util.SortedMap
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.math.round
import kotlin.math.sign

class StreetDescription(val name: String, val gridState: GridState) {

    // Street numbers
    // There are two types of street numbers, those which include a street name, and those which are
    // just numbers. The former are reliable, but the latter could be on the corner between streets
    // and so require some validation.
    //

    val ways: MutableList<Pair<Way, Boolean>> = mutableListOf()
    var sortedDescriptivePoints: SortedMap<Double, MvtFeature> = sortedMapOf()
    var leftSortedNumbers: SortedMap<Double, MvtFeature> = sortedMapOf()
    var leftMode: HouseNumberMode = HouseNumberMode.MIXED
    var rightSortedNumbers: SortedMap<Double, MvtFeature> = sortedMapOf()
    var rightMode: HouseNumberMode = HouseNumberMode.MIXED

    fun whichSide(way: Way,
                  direction: Boolean,
                  pdh: PointAndDistanceAndHeading,
                  location: LngLatAlt) : Side {
        val line = way.geometry as LineString
        var start = line.coordinates[pdh.index]
        var end = line.coordinates[pdh.index + 1]
        if (direction) {
            // Swap direction based on Way direction
            val tmp = start
            start = end
            end = tmp
        }

        return getSideOfLine(start, end, location)
    }

    fun sideToBool(side: Side) : Boolean? {
        return when (side) {
            Side.LEFT -> false
            Side.RIGHT -> true
            else -> null
        }
    }
    fun otherSide(side: Side) : Side? {
        return when (side) {
            Side.LEFT -> Side.RIGHT
            Side.RIGHT -> Side.LEFT
            else -> null
        }
    }

    fun parseHouseNumber(houseNumber: String) : Int? {
        val numericPart = houseNumber.takeWhile { it.isDigit() }

        // Check if we actually found any digits before trying to parse.
        if (numericPart.isNotEmpty()) {
            return numericPart.toInt()
        }
        return null
    }

    fun parseHouseNumberRange(houseNumber: String) : Pair<Int,Int>? {
        var highest = 0
        var lowest = Int.MAX_VALUE
        var remaining = houseNumber
        while(true) {
            remaining = remaining.dropWhile { !it.isDigit() }
            if (remaining.isEmpty()) break

            val numericPart = remaining.takeWhile { it.isDigit() }
            if (numericPart.isNotEmpty()) {
                val number = numericPart.toInt()
                if (number < lowest) lowest = number
                if (number > highest) highest = number

                remaining = remaining.drop(numericPart.length)
            }
        }
        return if(lowest == Int.MAX_VALUE)
            null
        else
            Pair(lowest, highest)
    }

    fun distanceAlongLine(nearestWay: Way, pdh: PointAndDistanceAndHeading) : Double {
        var totalDistance = 0.0
        for (way in ways) {
            if (way.first == nearestWay) {
                var lineDistance = 0.0
                val line = way.first.geometry as LineString
                for (i in 0 until pdh.index) {
                    lineDistance += gridState.ruler.distance(
                        line.coordinates[i],
                        line.coordinates[i + 1]
                    )
                }
                lineDistance += (pdh.positionAlongLine - pdh.index) * gridState.ruler.distance(
                    line.coordinates[pdh.index],
                    line.coordinates[pdh.index + 1]
                )
                totalDistance += if(way.second) {
                    lineDistance
                } else {
                    (way.first.length - lineDistance)
                }
                break
            }
            totalDistance += way.first.length
        }
        return totalDistance
    }
    fun nearestWayOnStreet(location: LngLatAlt?) : Pair<Way, Boolean>? {
        if(location == null)
            return null

        var nearestWay : Pair<Way, Boolean>? = null
        var nearestPdh = PointAndDistanceAndHeading()

        for(way in ways) {
            val pdh = getDistanceToFeature(location, way.first, gridState.ruler)
            if(pdh.distance < nearestPdh.distance) {
                nearestWay = way
                nearestPdh = pdh
            }
        }
        return nearestWay
    }
    fun distanceAlongStreet(startPoint: LngLatAlt?, distance: Double, ruler: Ruler) : LngLatAlt? {
        if(startPoint == null) return null
        val nearestWayToStart = nearestWayOnStreet(startPoint) ?: return null

        var searching = true
        var distanceLeft = distance
        for(way in ways) {
            if(searching) {
                if(way.first == nearestWayToStart.first) {
                    searching = false
                    if(way.first.length > distanceLeft) {
                        return ruler.along(way.first.geometry as LineString, distanceLeft)
                    }
                }
                else {
                    distanceLeft -= way.first.length
                    continue
                }
            }
            if(distance > way.first.length) {
                distanceLeft -= way.first.length
                continue
            }
            return ruler.along(way.first.geometry as LineString, distanceLeft)
        }
        return null
    }

    enum class HouseNumberMode {
        EVEN,
        ODD,
        MIXED
    }
    fun assignHouseNumberModes(odd: Array<Int>, even: Array<Int>) {
        if((odd[0] + odd[1] + even[0] + even[1]) >= 2) {
            // We have at least 2 house numbers
            if (
                (odd[0] == 0) &&
                (even[0] >= 0) &&
                (odd[1] >= 0) &&
                (even[1] == 0)
            ) {
                leftMode = HouseNumberMode.EVEN
                rightMode = HouseNumberMode.ODD
                println("Odd on right side, even on left")
                return
            } else if (
                (odd[1] == 0) &&
                (even[1] >= 0) &&
                (odd[0] >= 0) &&
                (even[0] == 0)

            ) {
                leftMode = HouseNumberMode.ODD
                rightMode = HouseNumberMode.EVEN
                println("Odd on left side, even on right")
                return
            }
        }
        println("Mixed house numbering")
        leftMode = HouseNumberMode.MIXED
        rightMode = HouseNumberMode.MIXED
    }

    private fun addHouse(house: MvtFeature,
                         nearestWay: Pair<Way,Boolean>?,
                         points: MutableMap<Double, MvtFeature>,
                         streetConfidence: Boolean) {
        if(nearestWay != null) {
            val location = getCentralPointForFeature(house) ?: return
            val pdh = getDistanceToFeature(location, nearestWay.first, gridState.ruler)
            val totalDistance = distanceAlongLine(nearestWay.first, pdh)
            val side = whichSide(
                nearestWay.first,
                nearestWay.second,
                pdh,
                location
            )
            house.side = sideToBool(side)
            house.streetConfidence = streetConfidence

            points[totalDistance] = house
        }
    }

    fun checkSortedNumberConsistency(sortedNumbers: SortedMap<Double, MvtFeature>) : SortedMap<Double, MvtFeature> {

        var firstConfidentHouse = Double.MIN_VALUE
        var lastConfidentHouse = Double.MIN_VALUE
        for (house in sortedNumbers) {
            if(house.value.streetConfidence) {
                if(firstConfidentHouse == Double.MIN_VALUE)
                    firstConfidentHouse = house.key
                lastConfidentHouse = house.key
            }
        }
        if(lastConfidentHouse == Double.MIN_VALUE) {
            // There are no houses on this side that we are confident about the number of
            return sortedMapOf()
        }

        var lastDelta = 0
        var lastHouse : MvtFeature? = null
        var lastNumbers : Pair<Int, Int>? = null
        val removalSet = mutableSetOf<MvtFeature>()
        for (house in sortedNumbers) {

            if((house.key < firstConfidentHouse) || (house.key > lastConfidentHouse)) {
                // We only allow houses that are in between ones that we are confident belong to
                // this street.
                removalSet.add(house.value)
                continue
            }
            val numbers = parseHouseNumberRange(house.value.housenumber ?: "")
            if((lastNumbers != null) && (numbers != null)) {
                // Check for overlap of range
                if((numbers.first <= lastNumbers.second) && (numbers.second >= lastNumbers.first)) {
                    // The range overlaps
                } else {
                    val newDelta = numbers.first - lastNumbers.first
                    if ((lastDelta != 0) && (newDelta != 0) && (newDelta.sign != lastDelta.sign)) {
                        // Numbers have changed direction
                        if (!house.value.streetConfidence) {
                            // The confidence in this street number isn't high as was found via a search
                            // Remove it
                            removalSet.add(house.value)
                        }
                        if (lastHouse != null) {
                            if (!lastHouse.streetConfidence) {
                                // The confidence in this street number isn't high as was found via a search
                                removalSet.add(lastHouse)
                            }
                        }
                        continue
                    }
                    if (newDelta != 0)
                        lastDelta = newDelta
                }
            }
            lastHouse = house.value
            lastNumbers = numbers
        }
        if(removalSet.isEmpty())
            return sortedNumbers

        // We need to remove some houses, so create a new map
        val newMap = mutableMapOf<Double, MvtFeature>()
        for(house in sortedNumbers) {
            if (!removalSet.contains(house.value)) {
                newMap[house.key] = house.value
            }
        }
        return newMap.toSortedMap()
    }

    private fun descriptiveIntersection(intersection: Intersection, localizedContext: Context?) : Boolean {
        // Return true if this intersection is useful for describing the street. If it's just an
        // un-named path with no confection then we return false.
        var count = 0
        for(member in intersection.members) {
            if(member.name == name) {
                // Skip this street
                continue
            }
            if(member.properties?.get("pavement") != null) {
                // Skip over pavements
                continue
            }
            val direction = member.intersections[WayEnd.START.id] == intersection
            val segmentName = member.getName(
                direction,
                gridState,
                localizedContext,
                nonGenericOnly = true,
                noGenericDeadEnds = true
            )
            if(segmentName.isEmpty()) continue

            val segmentNameReverse = member.getName(
                !direction,
                gridState,
                localizedContext,
                nonGenericOnly = true,
                noGenericDeadEnds = true
            )
            // If the description is the same in both directions, then we don't want to include it
            // as it's probably a loop joining on to our road
            if(segmentName.isNotEmpty() && ((segmentName == member.name) || (segmentName != segmentNameReverse))) ++count
        }

        return count > 0
    }

    /**
     * createDescription creates the street description
     */
    fun createDescription(matchedWay: Way, localizedContext: Context?) {
        val descriptivePoints: MutableMap<Double, MvtFeature> = mutableMapOf()
        val houseNumberPoints: MutableMap<Double, MvtFeature> = mutableMapOf()

        // We've got part of our street, so follow it in each direction adding to our list
        var intersection = matchedWay.intersections[WayEnd.START.id]
        var currentWay = matchedWay
        for(index in -1..0) {
            while (intersection != null) {
                intersection = currentWay.getOtherIntersection(intersection)
                val direction = (intersection == currentWay.intersections[WayEnd.END.id])
                if(index == 0) {
                    if (ways.isEmpty() || (ways[0].first != currentWay)) {
                        val newPair = Pair(currentWay, !direction)
                        if(ways.contains(newPair)) {
                            // We've looped around to a Way that we already have
                            break
                        }
                        ways.add(index, newPair)
                    }
                }
                else {
                    val newPair = Pair(currentWay, direction)
                    if(ways.contains(newPair)) {
                        // We've looped around to a Way that we already have
                        break
                    }
                    ways.add(Pair(currentWay, direction))
                }

                if (intersection != null) {
                    var found = false
                    var newWay = currentWay
                    // TODO: We need to deal with named roads splintering into dual carriageways e.g.
                    //  St Vincent Street https://www.openstreetmap.org/way/262604454. In fact there
                    //  all sorts of other challenges including non-linear roads e.g. Prestonfield
                    //  https://www.openstreetmap.org/way/1053351053 or Marchfield
                    //  https://www.openstreetmap.org/way/138354016. The main problem here is that
                    //  the housenumber map has ALL of the house numbers with that street and so it
                    //  confuses the odd/even numbering analysis.
                    for (member in intersection.members) {
                        if ((currentWay != member) &&
                            ((member.name == name) || (member.wayType == WayType.JOINER))) {
                            // We've got a Way of the same name extending away. See if it's continuing
                            // on in the same direction
                            newWay = member
                            found = true
                            break
                        }
                    }
                    if (found) {
                        currentWay = newWay
                    }
                    else {
                        // We reached an intersection which has no Way of the same name, so we're done
                        intersection = null
                    }
                }
            }
            intersection = matchedWay.intersections[WayEnd.END.id]
            currentWay = matchedWay
        }

        // We've now got an ordered list of Ways for our named street. Add all of the intersections
        // to our linear map
        var totalDistance = 0.0
        for(way in ways) {
            val beginIntersection =
                if (way.second)
                    way.first.intersections[WayEnd.START.id]
                else
                    way.first.intersections[WayEnd.END.id]

            if (beginIntersection != null) {
                if(beginIntersection.intersectionType != IntersectionType.TILE_EDGE) {
                    if(descriptiveIntersection(beginIntersection, localizedContext))
                        descriptivePoints[totalDistance] = beginIntersection
                }
            }
            totalDistance += way.first.length
            if (way == ways.last()) {
                val lastIntersection =
                    if (way.second)
                        way.first.intersections[WayEnd.END.id]
                    else
                        way.first.intersections[WayEnd.START.id]

                if (lastIntersection != null) {
                    if(descriptiveIntersection(lastIntersection, localizedContext))
                        descriptivePoints[totalDistance + way.first.length] = lastIntersection
                }
            }
        }

        // Add all of the house numbers with known street to our linear map
        val houseNumberTree = gridState.gridStreetNumberTreeMap[name]
        if(houseNumberTree != null) {
            val houseCollection = houseNumberTree.getAllCollection()
            for(house in houseCollection) {
                val nearestWay = nearestWayOnStreet(getCentralPointForFeature(house as MvtFeature))
                addHouse(house, nearestWay, houseNumberPoints, true)
            }
        }

        // Now search in the house numbers which don't have a known street
        val unknownStreetTree = gridState.gridStreetNumberTreeMap["null"]
        if(unknownStreetTree != null) {
            // Search each of our ways for street numbers with no street
            for(way in ways) {
                val results = unknownStreetTree.getNearbyLine(
                    way.first.geometry as LineString,
                    25.0,
                    gridState.ruler
                )
                for(house in results) {
                    // A searched for house should only be added if it's the nearest Way that it was
                    // found in.
                    val nearestWay = nearestWayOnStreet(getCentralPointForFeature(house as MvtFeature))
                    if(way.first == nearestWay?.first) {
                        addHouse(house, way, houseNumberPoints, false)
                    }
                }
            }
        }

        // Look for POI near the road
        val poiTree = gridState.getFeatureTree(TreeId.LANDMARK_POIS)
        // Search each of our ways for street numbers with no street
        for(way in ways) {
            val results = poiTree.getNearbyLine(
                way.first.geometry as LineString,
                25.0,
                gridState.ruler
            )
            for(poi in results) {
                val nearestWay = nearestWayOnStreet(getCentralPointForFeature(poi as MvtFeature))
                if(way.first == nearestWay?.first) {
                    addHouse(poi, way, descriptivePoints, false)
                }
            }
        }

        sortedDescriptivePoints = descriptivePoints.toSortedMap()

        // Analyse the house numbers on each side of the road
        val odd = arrayOf(0,0)
        val even = arrayOf(0,0)
        val sides = arrayOf(true,false)
        for(side in 0..1) {
            val numberPoints: MutableMap<Double, MvtFeature> = mutableMapOf()
            for (point in houseNumberPoints) {
                if(point.value.side != sides[side])
                    continue

                // We have a house number on the side of the street that we're interested in
                if(point.value.housenumber != null) {
                    val houseNumber = parseHouseNumber(point.value.housenumber!!)
                    if(houseNumber != null) {
                        numberPoints[point.key] = point.value
                        if(houseNumber % 2 == 0)
                            even[side]++
                        else
                            odd[side]++
                    }
                }
            }
            if(sides[side]) {
                leftSortedNumbers = numberPoints.toSortedMap()
            } else {
                rightSortedNumbers = numberPoints.toSortedMap()
            }
        }

        leftSortedNumbers = checkSortedNumberConsistency(leftSortedNumbers)
        rightSortedNumbers = checkSortedNumberConsistency(rightSortedNumbers)

        assignHouseNumberModes(odd, even)
    }

    fun getInterpolateLocation(needle: Int, sortedNumbers: SortedMap<Double, MvtFeature>) : Pair<LngLatAlt, String>? {
        var lastKey = 0.0
        var lastParsed : Int = Int.MAX_VALUE
        var lastPoint : LngLatAlt? = null
        for (number in sortedNumbers) {
            val parsedHaystack = parseHouseNumber(number.value.housenumber ?: "")
            if(parsedHaystack == null)
                continue

            if (parsedHaystack == needle) {
                // We've found an exact match
                return Pair(
                    (number.value.geometry as Point).coordinates,
                    number.value.housenumber ?: ""
                )
            }
            if(lastParsed != Int.MAX_VALUE) {
                val range = minOf(lastParsed, parsedHaystack)..maxOf(lastParsed, parsedHaystack)
                if(needle in range) {
                    // We're going to interpolate between two house numbers
                    val ratio = (needle - lastParsed).toDouble() / (parsedHaystack - lastParsed).toDouble()
                    val distance = lastKey + (ratio * (number.key - lastKey))
                    val location = distanceAlongStreet(lastPoint, distance, gridState.ruler)
                    if(location != null)
                        return Pair(location, needle.toString())

                    return null
                }
            }

            lastParsed = parsedHaystack
            lastKey = number.key
            lastPoint = (number.value.geometry as Point).coordinates
        }
        return  null
    }

    fun getLocationFromStreetNumber(houseNumber: String) : Pair<LngLatAlt, String>? {
        val parsedNeedle = parseHouseNumber(houseNumber) ?: return null
        val left = getInterpolateLocation(parsedNeedle, leftSortedNumbers)
        val right = getInterpolateLocation(parsedNeedle, rightSortedNumbers)
        return if(left == null) right
        else if(right == null) left
        else if(parsedNeedle.mod(2) == 0) {
            if(leftMode == HouseNumberMode.EVEN)
                left
            else
                right
        } else {
            if(leftMode == HouseNumberMode.ODD)
                left
            else
                right
        }
    }

    /**
     * Given a point and a Way this function returns the best guess house number for it. The Boolean
     * is true if the house number is on the other side of the street.
     */
    fun getStreetNumber(way: Way, location: LngLatAlt) : Pair<String, Boolean> {

        // Find the way in our list and see which direction it's going
        var direction: Boolean? = null
        for(member in ways) {
            if(way == member.first) {
                direction = member.second
                break
            }
        }
        if(direction == null) return Pair("", false)

        // Get the distance along our lines of points
        val pdh = getDistanceToFeature(location, way, gridState.ruler)
        val distance = distanceAlongLine(way, pdh)

        // Find which side of the road the point is on
        val locationSide = whichSide(way, !direction, pdh, location)

        // Try that side first, but it could be that there are no street numbers on this side,
        // so we also have to fallback to trying the other side too.
        for(side in listOf(locationSide, otherSide(locationSide))) {
            val sortedNumbers = when (side) {
                Side.LEFT -> leftSortedNumbers
                Side.RIGHT -> rightSortedNumbers
                else -> continue
            }
            val ceiling = sortedNumbers.keys.firstOrNull { it >= distance }
            val ceilingValue = if(ceiling != null) sortedNumbers[ceiling] else null
            var ceilingDistance = Double.MAX_VALUE
            val floor = sortedNumbers.keys.lastOrNull { it <= distance }
            val floorValue = if(floor != null) sortedNumbers[floor] else null
            var floorDistance = Double.MAX_VALUE

            var houseNumber = ""
            if (ceiling != null) {
                ceilingDistance = ceiling - distance
                if (ceilingDistance < 10.0)
                    houseNumber = ceilingValue?.housenumber ?: ""
            }
            if (floor != null) {
                floorDistance = distance - floor
                if ((floorDistance < 10.0) &&
                    ((floorDistance < ceilingDistance) || houseNumber.isEmpty()))
                    houseNumber = floorValue?.housenumber ?: ""
            }

            // Check to see if we have an exact match
            if (houseNumber.isNotEmpty())
                return Pair(houseNumber, side != locationSide)

            if((ceilingDistance != Double.MAX_VALUE) && ((floorDistance != Double.MAX_VALUE))) {
                val floorNumber = parseHouseNumber(floorValue?.housenumber ?: "")!!
                val ceilingNumber = parseHouseNumber(ceilingValue?.housenumber ?: "")!!
                val adjustment = floorDistance / (ceilingDistance + floorDistance)
                val interpolatedDouble = ((ceilingNumber - floorNumber) * adjustment)
                val interpolatedInt = round(interpolatedDouble / 2.0).toInt() * 2
                return Pair((interpolatedInt + floorNumber).toString(), side != locationSide)
            }
            if (houseNumber.isNotEmpty())
                return Pair(houseNumber, side != locationSide)
        }
        return Pair("", false)
    }

    data class StreetPosition(
        val name: String = "",
        val distance: Double = Double.MAX_VALUE
    )

    data class StreetLocationDescription(
        var name: String? = null,
        var behind: StreetPosition = StreetPosition(),
        var ahead: StreetPosition = StreetPosition()
    )

    fun getIntersectionText(intersection: Intersection?, way: Way?, localizedContext: Context?) : String? {
        if(intersection != null) {
            if(way != null) {
                // Describe the intersection from the perspective of Way
                for(crossStreet in intersection.members) {
                    if (crossStreet.name != name) {
                        val crossStreetName = crossStreet.getName(
                            crossStreet.intersections[WayEnd.START.id] == intersection,
                            gridState,
                            localizedContext,
                            nonGenericOnly = true
                        )
                        if(crossStreetName.isNotEmpty())
                            return crossStreetName
                    }
                }
            }

            val formatString = (localizedContext?.getString(R.string.street_description_intersection) ?:
            "Near intersection of %s")
            return formatString.format(intersection.name)
        }
        return null
    }

    fun describeLocation(location: LngLatAlt, heading: Double?, nearestWay: Way?, localizedContext: Context?) : StreetLocationDescription {
        if(nearestWay == null) return StreetLocationDescription()

        // Get the distance along our lines of points
        val pdh = getDistanceToFeature(location, nearestWay, gridState.ruler)
        val distance = distanceAlongLine(nearestWay, pdh)

        var direction = false
        val result = StreetLocationDescription()
        if (heading != null) {
            val headingDifference = calculateHeadingOffset(heading, pdh.heading)
            direction = (headingDifference < 90.0)
        }

        val ahead = sortedDescriptivePoints.keys.firstOrNull { it >= distance }
        val behind = sortedDescriptivePoints.keys.lastOrNull { it <= distance }

        var tmpAhead = StreetPosition()
        if (ahead != null) {
            val aheadValue = sortedDescriptivePoints[ahead]
            if(aheadValue != null) {
                val text = getIntersectionText(
                    aheadValue as? Intersection?,
                    nearestWay,
                    localizedContext
                ) ?: getTextForFeature(localizedContext, aheadValue).text

                tmpAhead = StreetPosition(text, ahead - distance)
            }
        }
        var tmpBehind = StreetPosition()
        if (behind != null) {
            val behindValue = sortedDescriptivePoints[behind]
            if(behindValue != null) {
                val text = getIntersectionText(
                    behindValue as? Intersection?,
                    nearestWay,
                    localizedContext
                ) ?: getTextForFeature(localizedContext, behindValue).text
                tmpBehind = StreetPosition(
                    text,
                    distance - behind
                )
            }
        }

        // The StreetLocationDescription is relative to the direction that the user is travelling
        if(direction) {
            result.ahead = tmpAhead
            result.behind = tmpBehind
        }
        else {
            result.behind = tmpAhead
            result.ahead = tmpBehind
        }
        return result
    }

    fun describeStreet() {
        println("Describe $name")
        for(point in sortedDescriptivePoints) {
            val text = getTextForFeature(null,point.value)
            when(point.value.side) {
                null -> println("\t\t\t\t\t${point.key.toInt()}m (${text.text})")
                true -> println("\t\t\t\t\t${point.key.toInt()}m ${text.text}")
                false -> println("${text.text}\t${point.key.toInt()}m")
            }
        }
    }
}