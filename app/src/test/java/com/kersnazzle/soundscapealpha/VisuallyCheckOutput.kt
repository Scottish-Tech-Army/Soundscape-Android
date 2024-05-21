package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Feature
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.utils.Quadrant
import com.kersnazzle.soundscapealpha.utils.circleToPolygon
import com.kersnazzle.soundscapealpha.utils.cleanTileGeoJSON
import com.kersnazzle.soundscapealpha.utils.createTriangleFOV
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxCorners
import com.kersnazzle.soundscapealpha.utils.getCenterOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getDestinationCoordinate
import com.kersnazzle.soundscapealpha.utils.getEntrancesFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovIntersectionFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovPoiFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovRoadsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPathsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPoiFeatureCollectionBySuperCategory
import com.kersnazzle.soundscapealpha.utils.getPointsOfInterestFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPolygonOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getQuadrants
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
        val testPathCollection = getPathsFeatureCollectionFromTileFeatureCollection(
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

    @Test
    fun poiSuperCategory(){
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
        // get the poi Feature Collection.
        val testPoiCollection = getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
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
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the intersection (this intersection is a transition from
        // Weston Road to Long Ashton Road)
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )
        // *************************************************************

        // Direction the device is pointing
        val quadrants = getQuadrants(deviceHeading)
        // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
        var quadrantIndex = 0
        for (quadrant in quadrants) {
            val containsHeading = quadrant.contains(deviceHeading)
            if (containsHeading) {
                break
            } else {
                quadrantIndex++
            }
        }
        // Get the coordinate for the "Left" of the FOV
        val destinationCoordinateLeft = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].left,
            fovDistance
        )

        //Get the coordinate for the "Right" of the FOV
        val destinationCoordinateRight = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].right,
            fovDistance
        )

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            destinationCoordinateLeft,
            currentLocation,
            destinationCoordinateRight
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovIntersectionsFeatureCollection.addFeature(featureFOVTriangle)

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
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the roads in the FoV roads.
        // In this case Weston Road and Long Ashton Road
        val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testRoadsCollectionFromTileFeatureCollection
        )
        // *************************************************************

        // Direction the device is pointing
        val quadrants = getQuadrants(deviceHeading)
        // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
        var quadrantIndex = 0
        for (quadrant in quadrants) {
            val containsHeading = quadrant.contains(deviceHeading)
            if (containsHeading) {
                break
            } else {
                quadrantIndex++
            }
        }
        // Get the coordinate for the "Left" of the FOV
        val destinationCoordinateLeft = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].left,
            fovDistance
        )

        //Get the coordinate for the "Right" of the FOV
        val destinationCoordinateRight = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].right,
            fovDistance
        )

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            destinationCoordinateLeft,
            currentLocation,
            destinationCoordinateRight
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)

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
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the poi from the tile
        val testPoiCollectionFromTileFeatureCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // ********* This is the only line that is useful. The rest of it is
        // ********* outputting the triangle that represents the FoV
        //
        // Create a FOV triangle to pick up the poi in the FoV.
        // In this case a couple of buildings
        val fovPoiFeatureCollection = getFovPoiFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testPoiCollectionFromTileFeatureCollection
        )
        // *************************************************************

        // Direction the device is pointing
        val quadrants = getQuadrants(deviceHeading)
        // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
        var quadrantIndex = 0
        for (quadrant in quadrants) {
            val containsHeading = quadrant.contains(deviceHeading)
            if (containsHeading) {
                break
            } else {
                quadrantIndex++
            }
        }
        // Get the coordinate for the "Left" of the FOV
        val destinationCoordinateLeft = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].left,
            fovDistance
        )

        //Get the coordinate for the "Right" of the FOV
        val destinationCoordinateRight = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].right,
            fovDistance
        )

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            destinationCoordinateLeft,
            currentLocation,
            destinationCoordinateRight
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovPoiFeatureCollection.addFeature(featureFOVTriangle)

        val fovPoi = moshi.adapter(FeatureCollection::class.java).toJson(fovPoiFeatureCollection)
        // copy and paste into GeoJSON.io
        println(fovPoi)

    }

    // Trying to understand how relative headings "ahead", "ahead left", etc. work
    // Displaying Soundscape COMBINED Direction types with this
    @Test
    fun relativeDirectionsPolygonsTest(){

        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 42.0

        val tileXY = getXYTile(location.latitude, location.longitude)
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        val cleanTileFeatureCollection = cleanTileGeoJSON(
            tileXY.first,
            tileXY.second,
            16.0,
            moshi.adapter(FeatureCollection::class.java).toJson(featureCollectionTest)
        )

        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                moshi.adapter(FeatureCollection::class.java)
                    .fromJson(cleanTileFeatureCollection)!!
            )
        val testRoadsCollection = getRoadsFeatureCollectionFromTileFeatureCollection(
            moshi.adapter(FeatureCollection::class.java)
                .fromJson(cleanTileFeatureCollection)!!
        )
        val newFeatureCollection = FeatureCollection()

        // smushing the roads and intersections into one Feature Collection
        for (feature in testRoadsCollection){
            newFeatureCollection.addFeature(feature)
        }
        for (feature in testIntersectionsCollectionFromTileFeatureCollection) {
            newFeatureCollection.addFeature(feature)
        }

        // Take the original 45 degree "ahead"/quadrant triangle and cutting it down
        // to a 30 degree "ahead" triangle
        val triangle1DirectionsQuad = Quadrant(deviceHeading)
        val triangle1Left = (triangle1DirectionsQuad.left + 30.0) % 360.0
        val triangle1Right = (triangle1DirectionsQuad.right - 30.0) % 360.0

        // creating triangle to visualise what is going on
        val ahead1 = getDestinationCoordinate(
            location,
            triangle1Left,
            50.0
        )
        val ahead2 = getDestinationCoordinate(
            location,
            triangle1Right,
            50.0
        )
        val aheadTriangle = createTriangleFOV(
            ahead1,
            location,
            ahead2
        )

        val featureAheadTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Direction", "Ahead 30 degrees")
            it.properties = ars3
        }
        featureAheadTriangle.geometry = aheadTriangle
        newFeatureCollection.addFeature(featureAheadTriangle)

        // Take the original 45 degree "ahead"/quadrant triangle and making
        // it a 60 degree "ahead left" triangle
        val triangle2DirectionsQuad = Quadrant(deviceHeading)
        val triangle2Left = (triangle2DirectionsQuad.left - 30.0) % 360.0
        val triangle2Right = (triangle2DirectionsQuad.right - 60.0) % 360.0

        val aheadLeft1 = getDestinationCoordinate(
            location,
            triangle2Left,
            50.0
        )
        val aheadLeft2 = getDestinationCoordinate(
            location,
            triangle2Right,
            50.0
        )

        val aheadLeftTriangle = createTriangleFOV(
            aheadLeft1,
            location,
            aheadLeft2
        )

        val featureAheadLeftTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Ahead Left 60 degrees")
            it.properties = ars
        }
        featureAheadLeftTriangle.geometry = aheadLeftTriangle

        newFeatureCollection.addFeature(featureAheadLeftTriangle)
        // Take the original 45 degree "ahead"/quadrant triangle and making
        // it a 60 degree "ahead right" triangle
        val triangle3DirectionsQuad = Quadrant(deviceHeading)
        val triangle3Left = (triangle3DirectionsQuad.left + 60.0) % 360.0
        val triangle3Right = (triangle3DirectionsQuad.right + 30.0) % 360.0

        val aheadRight1 = getDestinationCoordinate(
            location,
            triangle3Left,
            50.0
        )
        val aheadRight2 = getDestinationCoordinate(
            location,
            triangle3Right,
            50.0
        )

        val aheadRightTriangle = createTriangleFOV(
            aheadRight1,
            location,
            aheadRight2
        )

        val featureAheadRightTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Ahead Right 60 degrees")
            it.properties = ars
        }
        featureAheadRightTriangle.geometry = aheadRightTriangle

        newFeatureCollection.addFeature(featureAheadRightTriangle)

        // Take the original 45 degree "ahead"/quadrant triangle and making
        // it a 30 degree "right" triangle
        val triangle4DirectionsQuad = Quadrant(deviceHeading)
        val triangle4Left = (triangle4DirectionsQuad.left + 120.0) % 360.0
        val triangle4Right = (triangle4DirectionsQuad.right + 60.0) % 360.0

        val right1 = getDestinationCoordinate(
            location,
            triangle4Left,
            50.0
        )
        val right2 = getDestinationCoordinate(
            location,
            triangle4Right,
            50.0
        )

        val rightTriangle = createTriangleFOV(
            right1,
            location,
            right2
        )

        val featureRightTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Right 30 degrees")
            it.properties = ars
        }
        featureRightTriangle.geometry = rightTriangle

        newFeatureCollection.addFeature(featureRightTriangle)

        // Take the  original 45 degree "ahead"/quadrant triangle and making
        // it a 60 degree "behind right" triangle
        val triangle5DirectionsQuad = Quadrant(deviceHeading)
        val triangle5Left = (triangle5DirectionsQuad.left + 150.0) % 360.0
        val triangle5Right = (triangle5DirectionsQuad.right + 120.0) % 360.0

        val behindRight1 = getDestinationCoordinate(
            location,
            triangle5Left,
            50.0
        )
        val behindRight2 = getDestinationCoordinate(
            location,
            triangle5Right,
            50.0
        )

        val behindRightTriangle = createTriangleFOV(
            behindRight1,
            location,
            behindRight2
        )

        val featureBehindRightTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Behind Right 60 degrees")
            it.properties = ars
        }
        featureBehindRightTriangle.geometry = behindRightTriangle

        newFeatureCollection.addFeature(featureBehindRightTriangle)

        // Take the  original 45 degree "ahead"/quadrant triangle and making
        // it a 30 degree "behind" triangle
        val triangle6DirectionsQuad = Quadrant(deviceHeading)
        val triangle6Left = (triangle6DirectionsQuad.left + 210.0) % 360.0
        val triangle6Right = (triangle6DirectionsQuad.right + 150.0) % 360.0

        val behind1 = getDestinationCoordinate(
            location,
            triangle6Left,
            50.0
        )
        val behind2 = getDestinationCoordinate(
            location,
            triangle6Right,
            50.0
        )

        val behindTriangle = createTriangleFOV(
            behind1,
            location,
            behind2
        )

        val featureBehindTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Behind 30 degrees")
            it.properties = ars
        }
        featureBehindTriangle.geometry = behindTriangle

        newFeatureCollection.addFeature(featureBehindTriangle)

        // Take the  original 45 degree "ahead"/quadrant triangle and making
        // it a 30 degree "behind left" triangle
        val triangle7DirectionsQuad = Quadrant(deviceHeading)
        val triangle7Left = (triangle7DirectionsQuad.left + 240.0) % 360.0
        val triangle7Right = (triangle7DirectionsQuad.right + 210.0) % 360.0

        val behindLeft1 = getDestinationCoordinate(
            location,
            triangle7Left,
            50.0
        )
        val behindLeft2 = getDestinationCoordinate(
            location,
            triangle7Right,
            50.0
        )

        val behindLeftTriangle = createTriangleFOV(
            behindLeft1,
            location,
            behindLeft2
        )

        val featureBehindLeftTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Behind Left 60 degrees ")
            it.properties = ars
        }
        featureBehindLeftTriangle.geometry = behindLeftTriangle

        newFeatureCollection.addFeature(featureBehindLeftTriangle)

        // Take the original 45 degree "ahead"/quadrant triangle and making
        // it a 30 degree "left" triangle
        val triangle8DirectionsQuad = Quadrant(deviceHeading)
        val triangle8Left = (triangle8DirectionsQuad.left + 300.0) % 360.0
        val triangle8Right = (triangle8DirectionsQuad.right + 240.0) % 360.0

        val left1 = getDestinationCoordinate(
            location,
            triangle8Left,
            50.0
        )
        val left2 = getDestinationCoordinate(
            location,
            triangle8Right,
            50.0
        )

        val leftTriangle = createTriangleFOV(
            left1,
            location,
            left2
        )

        val featureLeftTriangle = Feature().also {
            val ars: HashMap<String, Any?> = HashMap()
            ars += Pair("Direction", "Left 30 degrees")
            it.properties = ars
        }
        featureLeftTriangle.geometry = leftTriangle

        newFeatureCollection.addFeature(featureLeftTriangle)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java).toJson(newFeatureCollection)
        println(relativeDirectionTrianglesString)

    }
}