package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.Quadrant
import org.scottishtecharmy.soundscape.utils.RelativeDirections
import org.scottishtecharmy.soundscape.utils.circleToPolygon
import org.scottishtecharmy.soundscape.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.utils.getAheadBehindDirectionPolygons
import org.scottishtecharmy.soundscape.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.utils.getCombinedDirectionPolygons
import org.scottishtecharmy.soundscape.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.utils.getEntrancesFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovPoiFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.utils.getIndividualDirectionPolygons
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getLeftRightDirectionPolygons
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getPathsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.utils.getPointsOfInterestFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getPolygonOfBoundingBox
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getReferenceCoordinate
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getTilesForRegion
import org.scottishtecharmy.soundscape.utils.getXYTile
import org.scottishtecharmy.soundscape.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.utils.tileToBoundingBox
import com.squareup.moshi.Moshi
import org.junit.Assert
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
    fun grid3x3WithPoisTest(){
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
        val gridFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONData3x3gridnoduplicates.tileGrid)
        for (feature in gridFeatureCollectionTest!!.features){
            newFeatureCollection.addFeature(feature)
        }
        val grid3x3PoiString = moshi.adapter(FeatureCollection::class.java).toJson(newFeatureCollection)
        // copy and paste into GeoJSON.io
        println(grid3x3PoiString)
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
    fun relativeDirectionsCombined(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 0.0
        val distance = 50.0

        val combinedDirectionPolygons  = getCombinedDirectionPolygons(location, deviceHeading, distance)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(combinedDirectionPolygons)

        println(relativeDirectionTrianglesString)

    }

    //Displaying Soundscape INDIVIDUAL Direction types
    @Test
    fun relativeDirectionsIndividual(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 90.0
        val distance = 50.0

        val individualRelativeDirections = getIndividualDirectionPolygons(location, deviceHeading, distance)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(individualRelativeDirections)

        println(relativeDirectionTrianglesString)
    }

    //Displaying Soundscape AHEAD_BEHIND Direction types
    @Test
    fun relativeDirectionsAheadBehind(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 90.0
        val distance = 50.0

        // Issue here is because of the bias towards "ahead" and "behind" you end up with a wide but shallow field of view
        // Probably better to do some trig to provide the distance to keep the depth of the field of view constant?
        val aheadBehindRelativeDirections = getAheadBehindDirectionPolygons(location, deviceHeading, distance)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(aheadBehindRelativeDirections)

        println(relativeDirectionTrianglesString)

    }

    //Displaying Soundscape LEFT_RIGHT Direction types
    @Test
    fun relativeDirectionsLeftRight(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 90.0
        val distance = 50.0

        val leftRightRelativeDirections = getLeftRightDirectionPolygons(location, deviceHeading, distance)

        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(leftRightRelativeDirections)

        println(relativeDirectionTrianglesString)
    }

    @Test
    fun relativeDirectionsAll(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val location = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 90.0
        val distance = 50.0

        // A wrapper around the individual functions
        val relativeDirections = getRelativeDirectionsPolygons(
            location, deviceHeading, distance, RelativeDirections.COMBINED
        )
        val relativeDirectionTrianglesString = moshi.adapter(FeatureCollection::class.java)
            .toJson(relativeDirections)

        println(relativeDirectionTrianglesString)

    }


}