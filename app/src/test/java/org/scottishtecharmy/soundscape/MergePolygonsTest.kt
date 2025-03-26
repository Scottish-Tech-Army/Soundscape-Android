package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.isDuplicateByOsmId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream

class MergePolygonsTest {

    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()

    @Test
    fun mergePolygons() {
        /*val location = LngLatAlt(-2.640563550340726, 51.540046658498945)
        val grid = getTileGrid(location.latitude, location.longitude, 2)
        for (tile in grid.tiles) {
            println("wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/${tile.tileX}/${tile.tileY}.pbf -O ${tile.tileX}x${tile.tileY}.mvt")
        }
        val getGeoJsonForLocationFC = getGeoJsonForLocation(location)

        // convert FeatureCollection to string
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val tileGridString = moshi.adapter(FeatureCollection::class.java).toJson(getGeoJsonForLocationFC)
        // copy and paste into GeoJSON.io
        println(tileGridString)*/

        val polygon1String = "{\"geometry\":{\"coordinates\":[[[-2.641388475894928,51.54281206119232],[-2.641863226890564,51.54281206119232],[-2.641788125038147,51.542920490515364],[-2.6419490575790405,51.542963862172236],[-2.6418471336364746,51.54311232637706],[-2.6416057348251343,51.543047269088504],[-2.641710340976715,51.54289880467142],[-2.641388475894928,51.54281206119232]]],\"type\":\"Polygon\"},\"feature_type\":\"building\",\"feature_value\":\"warehouse\",\"osm_ids\":[6.250644792E9],\"properties\":{\"name\":\"Amazon\",\"building\":\"warehouse\",\"osm_ids\":6.250644792E9},\"type\":\"Feature\"}"
        val polygon2String = "{\"geometry\":{\"coordinates\":[[[-2.6413187384605408,51.54015462794914],[-2.643338441848755,51.54069847186351],[-2.6431426405906677,51.540978732447456],[-2.643257975578308,51.54100876026479],[-2.6431775093078613,51.54112553492164],[-2.6430460810661316,51.54109050255605],[-2.6425498723983765,51.541801153839955],[-2.6426008343696594,51.54181449929781],[-2.6425471901893616,51.54189290378369],[-2.642509639263153,51.54188289470791],[-2.641788125038147,51.542920490515364],[-2.6419490575790405,51.542963862172236],[-2.6419061422348022,51.54302558330498],[-2.6416218280792236,51.54302558330498],[-2.641710340976715,51.54289880467142],[-2.6407554745674133,51.5426402418898],[-2.6407313346862793,51.54267527306238],[-2.640613317489624,51.54264357819311],[-2.640637457370758,51.54260521069025],[-2.639513611793518,51.54230327399525],[-2.639618217945099,51.542153138981284],[-2.639486789703369,51.54211810740676],[-2.6406213641166687,51.54048827529291],[-2.641012966632843,51.540593373699565],[-2.6413187384605408,51.54015462794914]]],\"type\":\"Polygon\"},\"feature_type\":\"building\",\"feature_value\":\"warehouse\",\"osm_ids\":[6.250644792E9],\"properties\":{\"name\":\"Amazon\",\"building\":\"warehouse\",\"osm_ids\":6.250644792E9},\"type\":\"Feature\"}"
        val feature1 = moshi.adapter(GeoJsonObject::class.java).fromJson(polygon1String)
        val feature2 = moshi.adapter(GeoJsonObject::class.java).fromJson(polygon2String)

        // The outer ring should be counter clockwise to be valid GeoJson so reverse it
        val isRingClockwise = isPolygonClockwise(feature1 as Feature)
        if (isRingClockwise) {
            (feature1.geometry as Polygon).coordinates[0].reverse()
        }
        val isRingClockwise2 = isPolygonClockwise(feature2 as Feature)
        if (isRingClockwise2) {
            (feature2.geometry as Polygon).coordinates[0].reverse()
        }
        /*val feature1String = moshi.adapter(Feature::class.java).toJson(feature1)
        val feature2String = moshi.adapter(Feature::class.java).toJson(feature2)
        println(feature1String)
        println(feature2String)*/
        val geometryFactory = GeometryFactory()
        val feature1Coordinates = (feature1.geometry as? Polygon)?.coordinates?.firstOrNull()
            ?.map {
                position -> Coordinate(position.longitude, position.latitude)
            }?.toTypedArray()
        val feature2Coordinates = (feature2.geometry as? Polygon)?.coordinates?.firstOrNull()
            ?.map {
                position -> Coordinate(position.longitude, position.latitude)
            }?.toTypedArray()

        val polygon1GeometryJTS = feature1Coordinates?.let { geometryFactory.createPolygon(it)}
        val polygon2GeometryJTS = feature2Coordinates?.let { geometryFactory.createPolygon(it)}
        // merge/union the polygons
        val mergedGeometryJTS = polygon1GeometryJTS?.union(polygon2GeometryJTS)
        // create a new Polygon with a single outer ring using the coordinates from the JTS merged geometry
        val mergedPolygon = Feature().also { feature ->
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("merged polygon", "true")
            feature.properties = ars3
            feature.type = "Feature"
            feature.geometry = Polygon().also { polygon ->
                //Convert JTS to GeoJSON coordinates
                val geoJsonCoordinates = mergedGeometryJTS?.coordinates?.map { coordinate ->
                    LngLatAlt(coordinate.x, coordinate.y )
                }?.let {
                    arrayListOf(arrayListOf(*it.toTypedArray()))
                }
                polygon.coordinates = geoJsonCoordinates ?: arrayListOf()
            }
        }
        val mergedPolygonString = moshi.adapter(Feature::class.java).toJson(mergedPolygon)
        println(mergedPolygonString)

    }

    @Test
    fun mergeAllPolygonsInGrid(){
        val location = LngLatAlt(-2.640563550340726, 51.540046658498945)

        val getGeoJsonForLocationFeatureCollection = getGeoJsonForLocation(location)
        val processOsmIds = mutableSetOf<Any>()
        val notDuplicateFeaturesFeatureCollection = FeatureCollection()
        val duplicateFeaturesFeatureCollection = FeatureCollection()

        for (feature in getGeoJsonForLocationFeatureCollection.features) {
            if (!isDuplicateByOsmId(processOsmIds, feature)) {
                notDuplicateFeaturesFeatureCollection.features.add(feature)
            } else {
                duplicateFeaturesFeatureCollection.features.add(feature)
            }
        }

        val mergedPolygonsFeatureCollection = FeatureCollection()
        val duplicateLineStringsAndPoints = FeatureCollection()
        val originalPolygonsUsedInMerge = mutableSetOf<Any>() // Track original polygons

        for (duplicate in duplicateFeaturesFeatureCollection) {
            // Find the original Feature
            val originalFeature = notDuplicateFeaturesFeatureCollection.features.find {
                it.foreign?.get("osm_ids") == duplicate.foreign?.get("osm_ids")
            }

            // Merge duplicate polygons
            if (originalFeature != null && originalFeature.geometry.type == "Polygon" && duplicate.geometry.type == "Polygon") {
                mergedPolygonsFeatureCollection.features.add(mergePolygons(originalFeature, duplicate))
                // Add to the set
                originalFeature.foreign?.get("osm_ids")?.let { originalPolygonsUsedInMerge.add(it) }
                // Add to the set
                duplicate.foreign?.get("osm_ids")?.let { originalPolygonsUsedInMerge.add(it) }
            } else {
                // TODO Merge the linestrings so we get a contiguous road/path
                if (duplicate.geometry.type == "LineString" || duplicate.geometry.type == "Point"){
                    duplicateLineStringsAndPoints.features.add(duplicate)
                }
            }
        }

        val finalFeatureCollection = FeatureCollection()

        // Add merged Polygons
        finalFeatureCollection.features.addAll(mergedPolygonsFeatureCollection.features)


        // Add original Features but excluding the Polygons that were merged
        for (feature in notDuplicateFeaturesFeatureCollection.features) {
            if (!isDuplicateByOsmId(originalPolygonsUsedInMerge, feature)) { // Check object identity
                finalFeatureCollection.features.add(feature)
            }
        }

        // Add the duplicate linestrings and points back in... need to sort out/merge the linestrings at later date
        finalFeatureCollection.features.addAll(duplicateLineStringsAndPoints)

        // The original GeoJson from the MVT tile has some features that aren't valid GeoJSON and
        // GeoJSON.io is having a huff: "Polygons and MultiPolygons should follow the right hand rule"
        // so fix that.
        for (feature in finalFeatureCollection.features) {
            if (feature.geometry.type == "Polygon") {
                if (isPolygonClockwise(feature)){
                    (feature.geometry as Polygon).coordinates[0].reverse()
                }
            }
        }

        //TODO: figure out why the MVT tile has a linestring with only one coordinate? GeoJSON has a huff about it
        val thisIsTheFinalFeatureCollectionHonest = FeatureCollection()
        for (feature in finalFeatureCollection) {
            if (feature.geometry.type == "LineString" && (feature.geometry as LineString).coordinates.size < 2 ){
                println("Bug: This is a linestring with only one coordinate")
            } else {
                thisIsTheFinalFeatureCollectionHonest.features.add(feature)
            }
        }
        val mergedGridPolygonsFeatureCollection = moshi.adapter(FeatureCollection::class.java).toJson(thisIsTheFinalFeatureCollectionHonest)
        println(mergedGridPolygonsFeatureCollection)
    }

    private fun mergePolygons(
        polygon1: Feature,
        polygon2: Feature
    ): Feature {

        val geometryFactory = GeometryFactory()
        val feature1Coordinates = (polygon1.geometry as? Polygon)?.coordinates?.firstOrNull()
            ?.map {
                    position -> Coordinate(position.longitude, position.latitude)
            }?.toTypedArray()
        val feature2Coordinates = (polygon2.geometry as? Polygon)?.coordinates?.firstOrNull()
            ?.map {
                    position -> Coordinate(position.longitude, position.latitude)
            }?.toTypedArray()

        val polygon1GeometryJTS = feature1Coordinates?.let { geometryFactory.createPolygon(it)}
        val polygon2GeometryJTS = feature2Coordinates?.let { geometryFactory.createPolygon(it)}
        // merge/union the polygons
        val mergedGeometryJTS = polygon1GeometryJTS?.union(polygon2GeometryJTS)
        // create a new Polygon with a single outer ring using the coordinates from the JTS merged geometry
        val mergedPolygon = Feature().also { feature ->
            feature.properties = polygon1.properties
            feature.foreign = polygon1.foreign
            feature.type = "Feature"
            feature.geometry = Polygon().also { polygon ->
                //Convert JTS to GeoJSON coordinates
                val geoJsonCoordinates = mergedGeometryJTS?.coordinates?.map { coordinate ->
                    LngLatAlt(coordinate.x, coordinate.y )
                }?.let {
                    arrayListOf(arrayListOf(*it.toTypedArray()))
                }
                polygon.coordinates = geoJsonCoordinates ?: arrayListOf()
            }
        }
        return mergedPolygon
    }

    private fun isPolygonClockwise(
        feature: Feature
    ): Boolean {
        // get outer ring coordinates (don't care about inner rings at the moment)
        val coordinates = (feature.geometry as Polygon).coordinates[0]
        var area = 0.0
        val n = coordinates.size
        for(i in 0 until n) {
            val j = (i + 1) % n
            area += (coordinates[j].longitude - coordinates[i].longitude) * (coordinates[j].latitude + coordinates[i].latitude)

        }
        return area > 0
    }

    private fun getGeoJsonForLocation(
        location: LngLatAlt,
        soundscapeBackend: Boolean = false
    ): FeatureCollection {

        var gridSize = 2
        if (soundscapeBackend) {
            gridSize = 3
        }
        // Get a grid around the location
        val grid = getTileGrid(location, gridSize)
        for (tile in grid.tiles) {
            println("Need tile ${tile.tileX}x${tile.tileY}")
        }

        // This is implemented for the soundscape-backend yet, so assert to make that clear
        assert(!soundscapeBackend)

        // Read in the files
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val joiner = InterpolatedPointsJoiner()
        val featureCollection = FeatureCollection()
        for (tile in grid.tiles) {
            val geojson = vectorTileToGeoJsonFromFile(
                tile.tileX,
                tile.tileY,
                "${tile.tileX}x${tile.tileY}.mvt",
                intersectionMap
            )
            for (feature in geojson) {
                val addFeature = joiner.addInterpolatedPoints(feature)
                if (addFeature) {
                    featureCollection.addFeature(feature)
                }
            }
        }
        // Add lines to connect all of the interpolated points
        joiner.addJoiningLines(featureCollection)

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("${grid.tiles[0].tileX}-${grid.tiles[1].tileX}x${grid.tiles[0].tileY}-${grid.tiles[3].tileY}.geojson")
        outputFile.write(adapter.toJson(featureCollection).toByteArray())
        outputFile.close()

        return featureCollection
    }

    private fun vectorTileToGeoJsonFromFile(
        tileX: Int,
        tileY: Int,
        filename: String,
        intersectionMap:  HashMap<LngLatAlt, Intersection>,
        cropPoints: Boolean = true
    ): FeatureCollection {

        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + filename)
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)

        val featureCollection = vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, cropPoints, 15)

//            // We want to check that all of the coordinates generated are within the buffered
//            // bounds of the tile. The tile edges are 4/256 further out, so we adjust for that.
//            val nwPoint = getLatLonTileWithOffset(tileX, tileY, 15, -4 / 256.0, -4 / 256.0)
//            val sePoint = getLatLonTileWithOffset(tileX + 1, tileY + 1, 15, 4 / 256.0, 4 / 256.0)
//            for (feature in featureCollection) {
//                var box = BoundingBox()
//                when (feature.geometry.type) {
//                    "Point" -> box = getBoundingBoxOfPoint(feature.geometry as Point)
//                    "MultiPoint" -> box = getBoundingBoxOfMultiPoint(feature.geometry as MultiPoint)
//                    "LineString" -> box = getBoundingBoxOfLineString(feature.geometry as LineString)
//                    "MultiLineString" -> box =
//                        getBoundingBoxOfMultiLineString(feature.geometry as MultiLineString)
//
//                    "Polygon" -> box = getBoundingBoxOfPolygon(feature.geometry as Polygon)
//                    "MultiPolygon" -> box =
//                        getBoundingBoxOfMultiPolygon(feature.geometry as MultiPolygon)
//
//                    else -> assert(false)
//                }
//                // Check that the feature bounding box is within the tileBoundingBox. This has been
//                // broken by the addition of POI polygons which go beyond tile boundaries.
//                assert(box.westLongitude >= nwPoint.longitude) { "${box.westLongitude} vs. ${nwPoint.longitude}" }
//                assert(box.eastLongitude <= sePoint.longitude) { "${box.eastLongitude} vs. ${sePoint.longitude}" }
//                assert(box.southLatitude >= sePoint.latitude) { "${box.southLatitude} vs. ${sePoint.latitude}" }
//                assert(box.northLatitude <= nwPoint.latitude) { "${box.northLatitude} vs. ${nwPoint.latitude}" }
//            }
        return featureCollection
    }
}