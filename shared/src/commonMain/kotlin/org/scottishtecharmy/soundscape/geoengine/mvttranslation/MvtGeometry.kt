package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun parseGeometry(
    cropToTile: Boolean,
    geometry: List<Int>
): List<ArrayList<Pair<Int, Int>>> {

    var x = 0
    var y = 0
    val results = mutableListOf(arrayListOf<Pair<Int,Int>>())
    var id : Int
    var count = 0
    var deltaX = 0
    var deltaY: Int
    var firstOfPair = true
    var lineCount = 0
    for (commandOrParameterInteger in geometry) {
        if (count == 0) {
            id = commandOrParameterInteger.and(0x7)
            count = commandOrParameterInteger.shr(3)
            when (id) {
                1 -> {
                    deltaX = 0
                    firstOfPair = true
                    ++lineCount
                    if((lineCount > 1) && results.last().isNotEmpty())
                        results.add(arrayListOf())
                }

                2 -> {
                    deltaX = 0
                }

                7 -> {
                    results.last().add(results.last().first())
                    count = 0
                }

                else -> {
                    println("Unknown command id $id")
                }
            }
        } else {
            val value =
                ((commandOrParameterInteger.shr(1)).xor(-(commandOrParameterInteger.and(1))))

            if (firstOfPair) {
                deltaX = value
                firstOfPair = false
            } else {
                deltaY = value
                firstOfPair = true

                x += deltaX
                y += deltaY

                var add = true
                if(cropToTile) {
                    if(pointIsOffTile(x, y))
                        add = false
                }
                if(add) {
                    results.last().add(Pair(x,y))
                }
                --count
            }
        }
    }

    return results
}

fun convertGeometry(tileX : Int, tileY : Int, tileZoom : Int, geometry: ArrayList<Pair<Int, Int>>) : ArrayList<LngLatAlt> {
    val results = arrayListOf<LngLatAlt>()
    for(point in geometry) {
        results.add(
            getLatLonTileWithOffset(tileX,
            tileY,
            tileZoom,
            sampleToFractionOfTile(point.first),
            sampleToFractionOfTile(point.second))
        )
    }
    return results
}

fun areCoordinatesClockwise(
    coordinates: ArrayList<Pair<Int, Int>>
): Boolean {
    var area = 0.0
    for(i in 0 until coordinates.size - 1) {
        area += (coordinates[i + 1].first - coordinates[i].first) * (coordinates[i + 1].second + coordinates[i].second)

    }
    return area < 0
}
