package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.geoengine.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.getPolygonOfBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTilesForRegion
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.tileToBoundingBox
import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle

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
        val tileXY = getXYTile(LngLatAlt(51.43860066718254, -2.69439697265625), 16 )
        val tileBoundingBox = tileToBoundingBox(tileXY.first, tileXY.second, 16)
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
        val tileXY = getXYTile(LngLatAlt(51.43860066718254, -2.69439697265625), 16 )
        val tileBoundingBox = tileToBoundingBox(tileXY.first, tileXY.second, 16)
        val tileBoundingBoxCorners = getBoundingBoxCorners(tileBoundingBox)
        val tileBoundingBoxCenter = getCenterOfBoundingBox(tileBoundingBoxCorners)

        val surroundingTiles = getTilesForRegion(
            tileBoundingBoxCenter.latitude, tileBoundingBoxCenter.longitude, 200.0, 16 )

        val newFeatureCollection = FeatureCollection()
        // Create a bounding box/Polygon for each tile in the grid
        for(tile in surroundingTiles){
            val surroundingTileBoundingBox = tileToBoundingBox(tile.tileX, tile.tileY, 16)
            val polygonBoundingBox = getPolygonOfBoundingBox(surroundingTileBoundingBox)
            val boundingBoxFeature = Feature().also {
                val ars3: HashMap<String, Any?> = HashMap()
                ars3 += Pair("Tile X", tile.tileX)
                ars3 += Pair("Tile Y", tile.tileY)
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
    fun roadsFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val gridState = getGridStateForLocation(LngLatAlt(51.43860066718254, -2.69439697265625), 1)
        val testRoadsCollection = gridState.getFeatureCollection(TreeId.ROADS)
        val roads = moshi.adapter(FeatureCollection::class.java).toJson(testRoadsCollection)
        // copy and paste into GeoJSON.io
        println(roads)
    }

    @Test
    fun intersectionsFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val gridState = getGridStateForLocation(LngLatAlt(51.43860066718254, -2.69439697265625), 1)
        // get the Intersections Feature Collection.
        val testIntersectionsCollection = gridState.getFeatureCollection(TreeId.INTERSECTIONS)

        val intersections = moshi.adapter(FeatureCollection::class.java).toJson(testIntersectionsCollection)
        // copy and paste into GeoJSON.io
        println(intersections)
    }

    @Test
    fun poiFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // convert coordinates to tile
        val gridState = getGridStateForLocation(LngLatAlt(51.43860066718254, -2.69439697265625), 1)
        // get the POI Feature Collection.
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        val poi = moshi.adapter(FeatureCollection::class.java).toJson(testPoiCollection)
        // copy and paste into GeoJSON.io
        println(poi)
    }

    @Test
    fun pathsFeatureCollection(){
        // The tile that I've been using above doesn't have any paths mapped in it
        // so I'm swapping to a different tile.
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val gridState = getGridStateForLocation(centralManchesterTestLocation, 1)

        val testPathCollection = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)

        val paths = moshi.adapter(FeatureCollection::class.java).toJson(testPathCollection)
        // copy and paste into GeoJSON.io
        println(paths)
    }

    @Test
    fun entrancesFeatureCollection(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val gridState = getGridStateForLocation(centralManchesterTestLocation, 1)
        val testEntrancesCollection = gridState.getFeatureCollection(TreeId.ENTRANCES)

        val entrances = moshi.adapter(FeatureCollection::class.java).toJson(testEntrancesCollection)
        // copy and paste into GeoJSON.io
        println(entrances)
    }

    @Test
    fun poiSuperCategory(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val gridState = getGridStateForLocation(centralManchesterTestLocation, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select super category
        //"information", "object", "place", "landmark", "mobility", "safety"
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("mobility", testPoiCollection)
        val superCategory = moshi.adapter(FeatureCollection::class.java).toJson(testSuperCategoryPoiCollection)
        // copy and paste into GeoJSON.io
        println(superCategory)
    }

    @Test
    fun intersectionsFieldOfView(){
        // Above I've just been filtering the entire tile into broad categories: roads, intersections, etc.
        // Now going to filter the tile by:
        // (1) where the device is located
        // (2) the direction the device is pointing,
        // (3) the distance that the "field of view" extends out.
        // Initially a right angle triangle but Soundscape is a bit more sophisticated which I'll get to
        // (4) whatever we are interested in -> roads, intersections, etc.

        // Fake device location and pretend the device is pointing East.
        // -2.6577997643930757, 51.43041390383118
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, 1)
        val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the intersection (this intersection is a transition from
        // Weston Road to Long Ashton Road)
        val triangle = getFovTriangle(userGeometry)
        val fovIntersectionsFeatureCollection = intersectionTree.getAllWithinTriangle(triangle)

        // *************************************************************

        val polygonTriangleFOV = createPolygonFromTriangle(triangle)

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovIntersectionsFeatureCollection.addFeature(featureFOVTriangle)

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val fovIntersections = moshi.adapter(FeatureCollection::class.java).toJson(fovIntersectionsFeatureCollection)
        // copy and paste into GeoJSON.io
        println(fovIntersections)
    }

    @Test
    fun roadsFieldOfView(){
        // Above I've just been filtering the entire tile into broad categories: roads, intersections, etc.
        // Now going to filter the tile by:
        // (1) where the device is located
        // (2) the direction the device is pointing,
        // (3) the distance that the "field of view" extends out.
        // Initially a right angle triangle but Soundscape is a bit more sophisticated which I'll get to
        // (4) whatever we are interested in -> roads, intersections, etc.

        // Fake device location and pretend the device is pointing East.
        // -2.6577997643930757, 51.43041390383118
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, 1)
        val roadsTree = gridState.getFeatureTree(TreeId.ROADS)

        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the roads in the FoV roads.
        // In this case Weston Road and Long Ashton Road
        val triangle = getFovTriangle(userGeometry)
        val fovRoadsFeatureCollection = roadsTree.getAllWithinTriangle(triangle)

        // *************************************************************
        val polygonTriangleFOV = createPolygonFromTriangle(triangle)

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val fovRoads = moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        // copy and paste into GeoJSON.io
        println(fovRoads)

    }

    @Test
    fun poiFieldOfView(){
        // Above I've just been filtering the entire tile into broad categories: roads, intersections, etc.
        // Now going to filter the tile by:
        // (1) where the device is located
        // (2) the direction the device is pointing,
        // (3) the distance that the "field of view" extends out.
        // Initially a right angle triangle but Soundscape is a bit more sophisticated which I'll get to
        // (4) whatever we are interested in -> roads, intersections, etc.

        // Fake device location and pretend the device is pointing East.
        // -2.6577997643930757, 51.43041390383118
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, 1)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)

        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the poi in the FoV.
        // In this case a couple of buildings
        val triangle = getFovTriangle(userGeometry)
        val fovPoiFeatureCollection = poiTree.getAllWithinTriangle(triangle)

        // *************************************************************

        val polygonTriangleFOV = createPolygonFromTriangle(triangle)

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovPoiFeatureCollection.addFeature(featureFOVTriangle)

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val fovPoi = moshi.adapter(FeatureCollection::class.java).toJson(fovPoiFeatureCollection)
        // copy and paste into GeoJSON.io
        println(fovPoi)

    }

    // Trying to understand how relative headings "ahead", "ahead left", etc. work
    // Displaying Soundscape COMBINED Direction types with this
    @Test
    fun relativeDirectionsCombined(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            0.0,
            50.0
        )
        val combinedDirectionPolygons  = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.COMBINED)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(combinedDirectionPolygons)

        println(relativeDirectionTrianglesString)

    }

    //Displaying Soundscape INDIVIDUAL Direction types
    @Test
    fun relativeDirectionsIndividual(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        val individualRelativeDirections = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.INDIVIDUAL)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(individualRelativeDirections)

        println(relativeDirectionTrianglesString)
    }

    //Displaying Soundscape AHEAD_BEHIND Direction types
    @Test
    fun relativeDirectionsAheadBehind(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        // Issue here is because of the bias towards "ahead" and "behind" you end up with a wide but shallow field of view
        // Probably better to do some trig to provide the distance to keep the depth of the field of view constant?
        val aheadBehindRelativeDirections  = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.AHEAD_BEHIND)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(aheadBehindRelativeDirections)

        println(relativeDirectionTrianglesString)

    }

    //Displaying Soundscape LEFT_RIGHT Direction types
    @Test
    fun relativeDirectionsLeftRight(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        val leftRightRelativeDirections  = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.LEFT_RIGHT)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(leftRightRelativeDirections)

        println(relativeDirectionTrianglesString)
    }

    @Test
    fun relativeDirectionsAll(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        // A wrapper around the individual functions
        val relativeDirections = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.COMBINED)
        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(relativeDirections)

        println(relativeDirectionTrianglesString)

    }


}