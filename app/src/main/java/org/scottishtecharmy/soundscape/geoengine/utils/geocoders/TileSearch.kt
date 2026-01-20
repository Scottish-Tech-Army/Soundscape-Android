package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import ch.poole.geo.pmtiles.Reader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertGeometryAndClipLineToTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.parseGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.pointIsOffTile
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geoengine.utils.getCentroidOfPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import vector_tile.VectorTile
import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlin.collections.isNotEmpty
import kotlin.text.iterator

class TileSearch(val offlineExtractPath: String,
                 val gridState: GridState,
                 val settlementGrid: GridState) {

    val stringCache = mutableMapOf<Long, List<String>>()

    fun findNearestNamedWay(location: LngLatAlt, name: String?) : Way? {
        val nearestWays =
            gridState.getFeatureTree(TreeId.ROADS).getNearestCollection(
                location,
                100.0,
                10,
                gridState.ruler,

                )
        for (way in nearestWays) {
            val wayName = (way as MvtFeature?)?.name
            if(name != null) {
                if (wayName == name) {
                    return way as Way?
                }
            } else {
                if (wayName != null) {
                    return way as Way?
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun search(
        location: LngLatAlt,
        searchString: String,
        localizedContext: Context?
    ) : List<LocationDescription> {
        val tileLocation = getXYTile(location, MAX_ZOOM_LEVEL)
        val extracts = findExtractPaths(offlineExtractPath).toMutableList()
        var reader : Reader? = null
        for(extract in extracts) {
            reader = Reader(File(extract))
            if(reader.getTile(MAX_ZOOM_LEVEL, tileLocation.first, tileLocation.second) != null)
                break
        }

        // We now have a PM tile reader
        var x = tileLocation.first
        var y = tileLocation.second

        var dx = 1 // Change in x per step
        var dy = 0 // Change in y per step

        var steps = 1 // Number of steps to take in the current direction
        var turnCount = 0
        var stepsTaken = 0

        // Set a limit to how far out you want to spiral
        val maxSearchRadius = 10
        val maxTurns = maxSearchRadius * 2

        // Can we decode this into a street number and a street?
        var housenumber = ""
        val needleBuilder = StringBuilder()
        val words = searchString.split(" ")
        for(word in words) {
            if(word.isEmpty()) continue
            if(word.first().isDigit()) {
                // If any word starts with a number we're going to assume is a house number...big if.
                housenumber = word
            } else {
                // All other parts we use as the needle
                needleBuilder.append(word)
                needleBuilder.append(" ")
            }
        }
        val normalizedNeedle = normalizeForSearch(needleBuilder.toString())

        data class TileSearchResult(
            var score: Double,
            var string: String,
            val tileX: Int,
            val tileY: Int,
        )
        data class DetailedSearchResult(
            var score: Double,
            var string: String,
            var location: LngLatAlt,
            var properties: HashMap<String, Any?> = hashMapOf(),
            val layer: String
        )
        val searchResults = mutableListOf<TileSearchResult>()
        val searchResultLimit = 8

        while (turnCount < maxTurns) {
            val tileIndex = x.toLong() + (y.toLong().shl(32))
            var cache = stringCache[tileIndex]
            if(cache == null) {
                // Load the tile and add all of its String to a cache
                cache = mutableListOf()
                val tileData = reader?.getTile(MAX_ZOOM_LEVEL, x, y)
                if (tileData != null) {
                    val tile = decompressTile(reader.tileCompression, tileData)
                    if(tile != null) {
                        for(layer in tile.layersList) {
                            if((layer.name == "transportation") || (layer.name == "poi")){
                                for (value in layer.valuesList) {
                                    if (value.hasStringValue()) {
                                        cache.add(normalizeForSearch(value.stringValue))
                                    }
                                }
                            }
                        }
                        stringCache[tileIndex] = cache
                    }
                }
            }
            for(string in cache) {
                val score = normalizedNeedle.fuzzyCompare(string, true)
                if(score < 0.25) {
                    // If we already have better search results, discard this one
                    val countOfBetter = searchResults.count { it.score < score }
                    if(countOfBetter < searchResultLimit) {
                        println("Found $searchString as $string (score $score) in tile ($x, $y)")
                        searchResults += TileSearchResult(score, string, x, y)
                        searchResults.sortBy { it.score }
                        if(searchResults.size > searchResultLimit)
                            searchResults.removeAt(searchResults.lastIndex)
                    }
                }
            }
            // --- 2. Move to the next position in the spiral ---
            x += dx
            y += dy
            stepsTaken++

            // --- 3. Check if it's time to turn ---
            if (stepsTaken == steps) {
                stepsTaken = 0
                turnCount++

                // Rotate direction: (1,0) -> (0,1) -> (-1,0) -> (0,-1)
                val temp = dx
                dx = -dy
                dy = temp

                // After every two turns, increase the number of steps
                if (turnCount % 2 == 0) {
                    steps++
                }
            }
        }
        // We have some rough results, but we need to get precise locations for each and remove any
        // duplicates due to tile boundary overlap and roads crossing tiles
        val ruler = CheapRuler(location.latitude)
        val detailedResults = mutableListOf<DetailedSearchResult>()
        for(result in searchResults) {
            val tileData = reader?.getTile(MAX_ZOOM_LEVEL, result.tileX, result.tileY)
            if (tileData != null) {
                val tile = decompressTile(reader.tileCompression, tileData)
                if(tile != null) {
                    var stringValue = ""
                    for(layer in tile.layersList) {
                        // Was the string found in transportation or POI? TODO: Or both?
                        if((layer.name == "transportation") || (layer.name == "poi")){
                            var nameTag = -1
                            for ((index, value) in layer.keysList.withIndex()) {
                                if (value == "name") {
                                    nameTag = index
                                    break
                                }
                            }

                            var stringKey = -1
                            for ((index, value) in layer.valuesList.withIndex()) {
                                if (value.hasStringValue()) {
                                    if(normalizeForSearch(value.stringValue) == result.string) {
                                        stringKey = index
                                        stringValue = value.stringValue
                                        break
                                    }
                                }
                            }
                            if(stringKey != -1) {
                                // We need to look for the feature
                                for (feature in layer.featuresList) {
                                    var firstInPair = true
                                    var skip = false
                                    var found = false
                                    for (tag in feature.tagsList) {
                                        if (firstInPair) {
                                            skip = (tag != nameTag)
                                        } else {
                                            if(!skip) {
                                                val raw = layer.getValues(tag)
                                                if (raw.hasStringValue() && (tag == stringKey)) {
                                                    found = true
                                                    break
                                                }
                                            }
                                        }
                                        firstInPair = !firstInPair
                                    }
                                    if (found) {
                                        // Parse all of the properties
                                        var firstInPair = true
                                        var key = ""
                                        var value: Any? = null
                                        val properties = hashMapOf<String, Any?>()
                                        for (tag in feature.tagsList) {
                                            if (firstInPair)
                                                key = layer.getKeys(tag)
                                            else {
                                                val raw = layer.getValues(tag)
                                                if (raw.hasBoolValue())
                                                    value = layer.getValues(tag).boolValue
                                                else if (raw.hasIntValue())
                                                    value = layer.getValues(tag).intValue
                                                else if (raw.hasSintValue())
                                                    value = layer.getValues(tag).sintValue
                                                else if (raw.hasFloatValue())
                                                    value = layer.getValues(tag).doubleValue
                                                else if (raw.hasDoubleValue())
                                                    value = layer.getValues(tag).floatValue
                                                else if (raw.hasStringValue())
                                                    value = layer.getValues(tag).stringValue
                                                else if (raw.hasUintValue())
                                                    value = layer.getValues(tag).uintValue
                                            }

                                            if (!firstInPair) {
                                                properties[key] = value
                                                firstInPair = true
                                            } else
                                                firstInPair = false
                                        }

                                        if (feature.type == VectorTile.Tile.GeomType.POINT) {
                                            val points = parseGeometry(true, feature.geometryList)
                                            for (point in points) {
                                                if (point.isNotEmpty()) {
                                                    val coordinates = convertGeometry(
                                                        result.tileX,
                                                        result.tileY,
                                                        MAX_ZOOM_LEVEL,
                                                        point)
                                                    for(coordinate in coordinates) {
                                                        detailedResults.add(
                                                            DetailedSearchResult(
                                                                result.score,
                                                                stringValue,
                                                                coordinate,
                                                                properties,
                                                                layer.name
                                                            )
                                                        )
                                                        break
                                                    }
                                                }
                                            }
                                            break
                                        } else if (feature.type == VectorTile.Tile.GeomType.LINESTRING) {
                                            val lines = parseGeometry(
                                                false,
                                                feature.geometryList
                                            )
                                            for (line in lines) {
                                                val interpolatedNodes : MutableList<LngLatAlt> = mutableListOf()
                                                val clippedLines = convertGeometryAndClipLineToTile(
                                                    result.tileX,
                                                    result.tileY,
                                                    MAX_ZOOM_LEVEL,
                                                    line,
                                                    interpolatedNodes
                                                )
                                                var resultValid = false
                                                for (clippedLine in clippedLines) {
                                                    resultValid = true
                                                    val centreDistance = ruler.lineLength(clippedLine)/2
                                                    val lineCentre = ruler.along(clippedLine, centreDistance)
                                                    detailedResults.add(
                                                        DetailedSearchResult(
                                                            result.score,
                                                            stringValue,
                                                            lineCentre,
                                                            properties,
                                                            layer.name
                                                        )
                                                    )
                                                    break
                                                }
                                                if(resultValid) break
                                            }
                                            break
                                        }
                                        else if(feature.type == VectorTile.Tile.GeomType.POLYGON) {
                                            val polygons = parseGeometry(
                                                false,
                                                feature.geometryList
                                            )

                                            // If all of the polygon points are outside the tile, then we can immediately
                                            // discard it
                                            var allOutside = true
                                            for (polygon in polygons) {
                                                for(point in polygon) {
                                                    if(!pointIsOffTile(point.first, point.second)) {
                                                        allOutside = false
                                                        break
                                                    }
                                                }
                                                if(!allOutside)
                                                    break
                                            }
                                            if(allOutside) continue

                                            for (polygon in polygons) {
                                                val polygonGeo = Polygon(
                                                    convertGeometry(
                                                        result.tileX,
                                                        result.tileY,
                                                        MAX_ZOOM_LEVEL,
                                                        polygon
                                                    )
                                                )
                                                val centroid = getCentroidOfPolygon(polygonGeo)
                                                detailedResults.add(
                                                    DetailedSearchResult(
                                                        result.score,
                                                        stringValue,
                                                        centroid ?: polygonGeo.coordinates[0][0],
                                                        properties,
                                                        layer.name
                                                    )
                                                )
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    result.string = stringValue
                }
            }
        }

        // Sort the results so far and deduplicate them
        val whittledResults = detailedResults
            .sortedWith { a, b ->
                if(a.score == b.score) {
                    val aDistance = ruler.distance(a.location, location)
                    val bDistance = ruler.distance(b.location, location)
                    aDistance.compareTo(bDistance)
                }
                else
                    a.score.compareTo(b.score)
            }
            .fold(mutableListOf<DetailedSearchResult>()) { accumulator, result ->
                if(accumulator.size < 5) {
                    // Check if we already have this exact name at approximately the same location
                    val isDuplicate = accumulator.any {
                        it.string == result.string && ruler.distance(
                            it.location,
                            result.location
                        ) < 100.0
                    }
                    if (!isDuplicate) {
                        accumulator.add(result)
                    }
                }
                accumulator
            }

        return whittledResults.map { result ->

            val mvt = MvtFeature()
            mvt.name = result.properties.get("name") as? String?
            mvt.properties = result.properties
            mvt.geometry = Point(result.location)

            // We've got results, see if we can improve the description from our GridState
            runBlocking {
                withContext(gridState.treeContext) {
                    if(gridState.isLocationWithinGrid(result.location)) {
                        val nearestWay = findNearestNamedWay(
                            result.location,
                            mvt.properties?.get("street") as String?
                        )
                        if(nearestWay != null) {
                            if (mvt.properties?.get("street") == null) {
                                mvt.properties?.set("street", nearestWay.name)
                            }
                            if (result.layer == "transportation") {
                                val sd = StreetDescription(result.string, gridState)
                                sd.createDescription(nearestWay, localizedContext)
                                val numberResult = sd.getLocationFromStreetNumber(housenumber)
                                if(numberResult != null) {
                                    mvt.properties?.set("housenumber", numberResult.second)
                                    result.location = numberResult.first
                                    mvt.geometry = Point(result.location)
                                    // We want the housenumber to appear in the LocationDescription,
                                    // so unset the name on the feature
                                    mvt.name = null
                                }
                            }
                        }
                    } else {
                        // We could go a step further here and decode the grids around each result,
                        // but that's a lot more work for the phone as it would have to do it for
                        // each result that's outside the current grid. It could likely be easily
                        // run in parallel, and we could perhaps make the results flow back and
                        // update dynamically which would mean that the time taken was less
                        // important.
                        if (result.layer == "transportation") {
                            if(mvt.name != null) {
                                mvt.properties?.set("street", mvt.name)
                            }
                        }
                    }
                    if(settlementGrid.isLocationWithinGrid(result.location)) {

                        // Get the nearest settlements. Nominatim uses the following proximities,
                        // so we do the same:
                        //
                        // cities, municipalities, islands | 15 km
                        // towns, boroughs                 |  4 km
                        // villages, suburbs               |  2 km
                        // hamlets, farms, neighbourhoods  |  1 km
                        //
                        var nearestDistrict: MvtFeature?
                        nearestDistrict = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
                            .getNearestFeature(location, settlementGrid.ruler, 1000.0) as MvtFeature?
                        if(nearestDistrict?.name == null) {
                            nearestDistrict = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE)
                                    .getNearestFeature(result.location, settlementGrid.ruler, 2000.0) as MvtFeature?
                            if(nearestDistrict?.name == null) {
                                nearestDistrict =
                                    settlementGrid.getFeatureTree(TreeId.SETTLEMENT_TOWN)
                                        .getNearestFeature(
                                            result.location,
                                            settlementGrid.ruler,
                                            4000.0
                                        ) as MvtFeature?
                                if (nearestDistrict?.name == null) {
                                    nearestDistrict =
                                        settlementGrid.getFeatureTree(TreeId.SETTLEMENT_CITY)
                                            .getNearestFeature(
                                                result.location,
                                                settlementGrid.ruler,
                                                15000.0
                                            ) as MvtFeature?
                                }
                            }
                        }
                        if (nearestDistrict?.name != null) {
                            mvt.properties?.set("city", nearestDistrict.name)
                        }
                    }
                }
            }

//        // We could decode just the housenumbers layer in the tile, but that only works for
//        // direct matches and not interpolation. We could try and interpolate from known values,
//        // but that's more special code which seems like a bad idea.
//        for(result in searchResults) {
//            val tileData = reader?.getTile(MAX_ZOOM_LEVEL, result.tileX, result.tileY)
//            if (tileData != null) {
//                val tile = decompressTile(reader.tileCompression, tileData)
//                if(tile != null) {
//                    for(layer in tile.layersList) {
//                        // Was the string found in transportation or POI? TODO: Or both?
//                        if(layer.name == "housenumber"){
//                            // We need to look for the feature. First search by street.
//                            for (feature in layer.featuresList) {
//                                var firstInPair = true
//                                var key = ""
//                                var street = ""
//                                var housenumber = ""
//                                for (tag in feature.tagsList) {
//                                    if (firstInPair) {
//                                        key = layer.getKeys(tag)
//                                    } else {
//                                        val raw = layer.getValues(tag)
//                                        if (raw.hasStringValue()) {
//                                            if(key == "street") {
//                                                street = raw.stringValue.toString()
//                                            } else if(key == "housenumber") {
//                                                housenumber = raw.stringValue.toString()
//                                            }
//                                        }
//                                    }
//                                    firstInPair = !firstInPair
//                                }
//                                if((normalizeForSearch(street) == result.string) && housenumber == "12") {
//                                    if (feature.type == VectorTile.Tile.GeomType.POINT) {
//                                        val points = parseGeometry(true, feature.geometryList)
//                                        for (point in points) {
//                                            if (point.isNotEmpty()) {
//                                                val coordinates = convertGeometry(
//                                                    result.tileX,
//                                                    result.tileY,
//                                                    MAX_ZOOM_LEVEL,
//                                                    point
//                                                )
//                                                for (coordinate in coordinates) {
//                                                    detailedResults.add(
//                                                        DetailedSearchResult(
//                                                            result.score,
//                                                            "$housenumber $street",
//                                                            coordinate
//                                                        )
//                                                    )
//                                                    break
//                                                }
//                                            }
//                                        }
//                                        break
//                                    }
//                                }
//                            }
//                        }
//                    }
////                    result.string = stringValue
//                }
//            }
//        }

            val ld = mvt.toLocationDescription(LocationSource.OfflineGeocoder)
            ld ?: LocationDescription(
                name = result.string,
                location = result.location
            )
        }
    }
}

private val apostrophes = setOf('\'', '’', '‘', '‛', 'ʻ', 'ʼ', 'ʹ', 'ꞌ', '＇')
fun normalizeForSearch(input: String): String {
    // Unicode normalize (decompose accents etc.)
    val normalizedString = Normalizer.normalize(input, Normalizer.Form.NFKD)

    val sb = StringBuilder(normalizedString.length)
    var lastWasSpace = false

    for (ch in normalizedString) {
        // Remove combining marks (diacritics)
        val type = Character.getType(ch)
        if (type == Character.NON_SPACING_MARK.toInt()) continue

        // Make apostrophes disappear completely (missing/extra apostrophes become irrelevant)
        if (ch in apostrophes) continue

        // Turn most punctuation into spaces (keeps token boundaries stable)
        val isLetterOrDigit = Character.isLetterOrDigit(ch)
        val outCh = when {
            isLetterOrDigit -> ch.lowercaseChar()
            Character.isWhitespace(ch) -> ' '
            else -> ' ' // punctuation -> space
        }

        if (outCh == ' ') {
            if (!lastWasSpace) {
                sb.append(' ')
                lastWasSpace = true
            }
        } else {
            sb.append(outCh)
            lastWasSpace = false
        }
    }

    return sb.toString().trim().lowercase(Locale.ROOT)
}