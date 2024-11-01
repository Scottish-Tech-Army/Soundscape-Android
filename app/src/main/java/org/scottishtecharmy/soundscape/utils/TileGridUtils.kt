package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter

class TileGrid(newTiles : MutableList<Tile>, newCentralBoundingBox : BoundingBox) {

    val tiles : MutableList<Tile> = newTiles
    var centralBoundingBox : BoundingBox = newCentralBoundingBox

    /**
     * Return GeoJSON that describes the tile grid and the central bounding box so that it can be
     * rendered over our map for debug.
     */
    fun generateGeoJson() : String {
        val featureCollection = FeatureCollection()

        if(true) {
            val feature = Feature()
            // Add central bounding box
            val coordinates = arrayListOf<LngLatAlt>()
            coordinates.add(LngLatAlt(centralBoundingBox.westLongitude, centralBoundingBox.northLatitude))
            coordinates.add(LngLatAlt(centralBoundingBox.eastLongitude, centralBoundingBox.northLatitude))
            coordinates.add(LngLatAlt(centralBoundingBox.eastLongitude, centralBoundingBox.southLatitude))
            coordinates.add(LngLatAlt(centralBoundingBox.westLongitude, centralBoundingBox.southLatitude))
            coordinates.add(LngLatAlt(centralBoundingBox.westLongitude, centralBoundingBox.northLatitude))
            feature.geometry = Polygon(coordinates)
            feature.properties = hashMapOf()
            featureCollection.addFeature(feature)
        }

        for(tile in tiles) {
            val feature = Feature()
            val box = tileToBoundingBox(tile.tileX, tile.tileY, tile.zoom)
            val coordinates = arrayListOf<LngLatAlt>()
            coordinates.add(LngLatAlt(box.westLongitude, box.northLatitude))
            coordinates.add(LngLatAlt(box.eastLongitude, box.northLatitude))
            coordinates.add(LngLatAlt(box.eastLongitude, box.southLatitude))
            coordinates.add(LngLatAlt(box.westLongitude, box.southLatitude))
            coordinates.add(LngLatAlt(box.westLongitude, box.northLatitude))
            feature.geometry = Polygon(coordinates)
            feature.properties = hashMapOf()
            featureCollection.addFeature(feature)
        }

        val adapter = GeoJsonObjectMoshiAdapter()
        return adapter.toJson(featureCollection).toString()
    }

    companion object {
        /**
         * The zoom level and grid size are constant. When using soundscape-backend these will be
         * 16 and 3, but if we switch to using protobuf tiles they will be 15 and 2.
         */
        const val SOUNDSCAPE_TILE_BACKEND = true
        val ZOOM_LEVEL = if(SOUNDSCAPE_TILE_BACKEND) 16 else 15
        var GRID_SIZE = if(SOUNDSCAPE_TILE_BACKEND) 3 else 2

        /**
         * Given a location it calculates the set of tiles (VectorTiles) that cover a
         * 3 x 3 grid around the specified location.
         * @param currentLatitude
         * The current location of the device.
         * @param currentLongitude
         * The current location of the device.
         * @return  A TileGrid describing the new grid
         */
        private fun get3x3TileGrid(
            currentLatitude: Double = 0.0,
            currentLongitude: Double = 0.0
        ) : TileGrid {

            // Get tile that contains current location
            val tileXY = getXYTile(currentLatitude, currentLongitude, ZOOM_LEVEL)

            // Center of grid is at the center of this tile
            val centerX = (tileXY.first * 256) + 128
            val centerY = (tileXY.second * 256) + 128
            val southWest = pixelXYToLatLon((centerX - 192).toDouble(), (centerY + 192).toDouble(), ZOOM_LEVEL)
            val northEast = pixelXYToLatLon((centerX + 192).toDouble(), (centerY - 192).toDouble(), ZOOM_LEVEL)
            val centralBoundingBox = BoundingBox(southWest.second,
                                                 southWest.first,
                                                 northEast.second,
                                                 northEast.first)

            // And build 3x3 grid around it ensuring that we wrap at the edges
            val maxCoordinate = mapSize(ZOOM_LEVEL) / 256
            val xValues = IntArray(3)
            xValues[0] = (tileXY.first - 1).mod(maxCoordinate)
            xValues[1] = tileXY.first
            xValues[2] = (tileXY.first + 1).mod(maxCoordinate)
            val yValues = IntArray(3)
            yValues[0] = (tileXY.second - 1).mod(maxCoordinate)
            yValues[1] = tileXY.second
            yValues[2] = (tileXY.second + 1).mod(maxCoordinate)

            val tiles: MutableList<Tile> = mutableListOf()
            for (y in yValues) {
                for (x in xValues) {
                    val surroundingTile = Tile("", x, y, ZOOM_LEVEL)
                    surroundingTile.quadkey = getQuadKey(x, y, ZOOM_LEVEL)
                    tiles.add(surroundingTile)
                }
            }
            return TileGrid(tiles, centralBoundingBox)
        }

        /**
         * Given a location it calculates the set of tiles (VectorTiles) that cover a
         * 2 x 2 grid around the specified location.
         * @param currentLatitude
         * The current location of the device.
         * @param currentLongitude
         * The current location of the device.
         * @return  A TileGrid describing the new grid
         */
        private fun get2x2TileGrid(
            currentLatitude: Double = 0.0,
            currentLongitude: Double = 0.0
        ): TileGrid {

            // Get tile that contains current location
            val tileXY = getXYTile(currentLatitude, currentLongitude, ZOOM_LEVEL)
            // Scale up the tile xy
            val scaledTile = Pair(tileXY.first * 2, tileXY.second * 2)

            // And the quadrant within that tile
            val tileQuadrant = getXYTile(currentLatitude, currentLongitude, ZOOM_LEVEL + 1)

            // The center of the grid is the corner of the tile that is shared with the quadrant that the
            // location is within.
            val maxCoordinate = mapSize(ZOOM_LEVEL) / 256
            val xValues = IntArray(2)
            val yValues = IntArray(2)
            if (tileQuadrant.first == scaledTile.first) {
                if (tileQuadrant.second == scaledTile.second) {
                    // Top left quadrant as the coordinates match
                    xValues[0] = (tileXY.first - 1).mod(maxCoordinate)
                    xValues[1] = tileXY.first
                    yValues[0] = (tileXY.second - 1).mod(maxCoordinate)
                    yValues[1] = tileXY.second
                } else {
                    // Bottom left quadrant as the x coordinate matches
                    xValues[0] = (tileXY.first - 1).mod(maxCoordinate)
                    xValues[1] = tileXY.first
                    yValues[0] = tileXY.second
                    yValues[1] = (tileXY.second + 1).mod(maxCoordinate)
                }
            } else {
                if (tileQuadrant.second == scaledTile.second) {
                    // Top right quadrant as only the y coordinates match
                    xValues[0] = tileXY.first
                    xValues[1] = (tileXY.first + 1).mod(maxCoordinate)
                    yValues[0] = (tileXY.second - 1).mod(maxCoordinate)
                    yValues[1] = tileXY.second
                } else {
                    // Bottom right quadrant as neither coordinate matches
                    xValues[0] = tileXY.first
                    xValues[1] = (tileXY.first + 1).mod(maxCoordinate)
                    yValues[0] = tileXY.second
                    yValues[1] = (tileXY.second + 1).mod(maxCoordinate)
                }
            }

            // Center of grid is the top left corner of the bottom right tile
            val centerX = (xValues[1] * 256)
            val centerY = (yValues[1] * 256)
            val southWest = pixelXYToLatLon((centerX - 160).toDouble(), (centerY + 160).toDouble(), ZOOM_LEVEL)
            val northEast = pixelXYToLatLon((centerX + 160).toDouble(), (centerY - 160).toDouble(), ZOOM_LEVEL)
            val centralBoundingBox = BoundingBox(southWest.second,
                southWest.first,
                northEast.second,
                northEast.first)

            val tiles: MutableList<Tile> = mutableListOf()
            for (y in yValues) {
                for (x in xValues) {
                    val surroundingTile = Tile("", x, y, ZOOM_LEVEL)
                    surroundingTile.quadkey = getQuadKey(x, y, ZOOM_LEVEL)
                    tiles.add(surroundingTile)
                }
            }
            return TileGrid(tiles, centralBoundingBox)
        }

        /**
         * Given a location and a grid size it creates either a 2x2 or a 3x3 grid of tiles
         * (VectorTiles) to cover that location.
         * @param currentLatitude
         * The current location of the device.
         * @param currentLongitude
         * The current location of the device.
         * @return  A TileGrid object which contains a MutableList of VectorTiles representing the
         * grid along with a BoundingBox around the center of the grid. When the location leaves
         * that BoundingBox a new grid is required.
         */
        fun getTileGrid(
            currentLatitude: Double = 0.0,
            currentLongitude: Double = 0.0
        ): TileGrid {
            when(GRID_SIZE) {
                2 -> return get2x2TileGrid(currentLatitude, currentLongitude)
                3 -> return get3x3TileGrid(currentLatitude, currentLongitude)
            }
            assert(false)
            return TileGrid(mutableListOf(), BoundingBox())
        }
    }
}