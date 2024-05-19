package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Feature
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.utils.circleToPolygon
import com.kersnazzle.soundscapealpha.utils.cleanTileGeoJSON
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxCorners
import com.kersnazzle.soundscapealpha.utils.getCenterOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getEntrancesFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPathsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPointsOfInterestFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPolygonOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getTilesForRegion
import com.kersnazzle.soundscapealpha.utils.getXYTile
import com.kersnazzle.soundscapealpha.utils.tileToBoundingBox
import com.squareup.moshi.Moshi
import org.junit.Test

// Functions to output GeoJSON strings that can be put into the very useful Geojson.io
// for a visual check. The GeoJSON parser that they use is also handy to make sure output
// is correct. However it seems to use markers for any Point object which can make the screen a bit busy
// https://geojson.io/#map=2/0/20
class VisuallyCheckOutput {

    @Test
    fun youAreHereTest(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile - I'm cheating here as the coordinates are already
        // in the center of the tile.
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        val tileBoundingBox = tileToBoundingBox(tileXY.first, tileXY.second, 16.0)
        val tileBoundingBoxCorners = getBoundingBoxCorners(tileBoundingBox)
        val tilePolygon = getPolygonOfBoundingBox(tileBoundingBox)
        val tileBoundingBoxCenter = getCenterOfBoundingBox(tileBoundingBoxCorners)
        // Feature is the Polygon to display tile boundary
        val featurePolygon = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Polygon", "to display the tile bounding box")
            it.properties = ars3
        }
        featurePolygon.geometry = tilePolygon
        // Create a point to show center of tile
        val locationPoint = Point()
        locationPoint.coordinates = tileBoundingBoxCenter
        val featureHere = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Hello!", "World")
            it.properties = ars3
        }
        featureHere.geometry = locationPoint
        // create a new feature collection
        val newFeatureCollection = FeatureCollection()
        // add our two Features to the Feature Collection
        newFeatureCollection.addFeature(featurePolygon)
        newFeatureCollection.addFeature(featureHere)
        // convert FeatureCollection to string
        val youAreHere = moshi.adapter(FeatureCollection::class.java).toJson(newFeatureCollection)
        // copy and paste into GeoJSON.io
        println(youAreHere)
    }

    @Test
    fun grid3x3Test(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        val tileBoundingBox = tileToBoundingBox(tileXY.first, tileXY.second, 16.0)
        val tileBoundingBoxCorners = getBoundingBoxCorners(tileBoundingBox)
        val tileBoundingBoxCenter = getCenterOfBoundingBox(tileBoundingBoxCorners)

        val surroundingTiles = getTilesForRegion(
            tileBoundingBoxCenter.latitude, tileBoundingBoxCenter.longitude, 200.0, 16 )

        val newFeatureCollection = FeatureCollection()
        // Create a bounding box/Polygon for each tile in the grid
        for(tile in surroundingTiles){
            val surroundingTileBoundingBox = tileToBoundingBox(tile.tileX, tile.tileY, 16.0)
            val polygonBoundingBox = getPolygonOfBoundingBox(surroundingTileBoundingBox)
            val boundingBoxFeature = Feature().also {
                val ars3: HashMap<String, Any?> = HashMap()
                ars3 += Pair("Tile X", tile.tileX)
                ars3 += Pair("Tile Y", tile.tileY)
                ars3 += Pair("quadKey", tile.quadkey)
                it.properties = ars3
                it.type = "Feature"
            }
            boundingBoxFeature.geometry = polygonBoundingBox
            newFeatureCollection.addFeature(boundingBoxFeature)
        }
        // Display the circle we are using for the grid radius
        val circlePolygon = circleToPolygon(
            30,
            tileBoundingBoxCenter.latitude,
            tileBoundingBoxCenter.longitude,
            200.0
        )
        val circlePolygonFeature = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Shape", "circle")
            ars3 += Pair("Radius", 200)
            it.properties = ars3
            it.type = "Feature"
        }
        circlePolygonFeature.geometry = circlePolygon
        newFeatureCollection.addFeature(circlePolygonFeature)

        val grid3x3String = moshi.adapter(FeatureCollection::class.java).toJson(newFeatureCollection)
        // copy and paste into GeoJSON.io
        println(grid3x3String)
    }

    @Test
    fun entireTileFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        // Get the data for the entire tile
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            tileXY.first,
            tileXY.second,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(featureCollectionTest)
        )
        // copy and paste into GeoJSON.io
        println(cleanTileFeatureCollection)
    }

    @Test
    fun roadsFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        // Get the data for the entire tile
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            tileXY.first,
            tileXY.second,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(entireFeatureCollectionTest)
        )
        // get the roads Feature Collection.
        // Crossings are counted as roads by original Soundscape
        val testRoadsCollection = getRoadsFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
            )
        val roads = moshi.adapter(FeatureCollection::class.java).toJson(testRoadsCollection)
        // copy and paste into GeoJSON.io
        println(roads)
    }

    @Test
    fun intersectionsFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        // Get the data for the entire tile
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            tileXY.first,
            tileXY.second,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(entireFeatureCollectionTest)
        )
        // get the Intersections Feature Collection.
        val testIntersectionsCollection = getIntersectionsFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
        val intersections = moshi.adapter(FeatureCollection::class.java).toJson(testIntersectionsCollection)
        // copy and paste into GeoJSON.io
        println(intersections)
    }

    @Test
    fun poiFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val tileXY = getXYTile(51.43860066718254, -2.69439697265625, 16 )
        // Get the data for the entire tile
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            tileXY.first,
            tileXY.second,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(entireFeatureCollectionTest)
        )
        // get the POI Feature Collection.
        val testPoiCollection = getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
        val poi = moshi.adapter(FeatureCollection::class.java).toJson(testPoiCollection)
        // copy and paste into GeoJSON.io
        println(poi)
    }

    @Test
    fun pathsFeatureCollection(){
        // The tile that I've been using above doesn't have any paths mapped in it
        // so I'm swapping to a different tile.
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // Get the data for the entire tile
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            32295,
            21787,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(entireFeatureCollectionTest)
        )
        // get the paths Feature Collection.
        val testPathCollection = getPathsFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
        val paths = moshi.adapter(FeatureCollection::class.java).toJson(testPathCollection)
        // copy and paste into GeoJSON.io
        println(paths)
    }

    @Test
    fun entrancesFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // Get the data for the entire tile
        val entireFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        // clean it
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            32295,
            21787,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(entireFeatureCollectionTest)
        )
        // get the entrances Feature Collection.
        // Entrances are weird as it is a single Feature made up of MultiPoints and osm_ids
        // I'm assuming osm_ids correlate to the polygon which has the entrance but haven't checked this yet
        val testEntrancesCollection = getEntrancesFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
        val entrances = moshi.adapter(FeatureCollection::class.java).toJson(testEntrancesCollection)
        // copy and paste into GeoJSON.io
        println(entrances)
    }
}