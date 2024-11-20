package org.scottishtecharmy.soundscape.geoengine

/**
 *  This file contains the various configuration options for the GeoEngine.
 */

/**
 * The zoom level and grid size are constant. When using soundscape-backend these will be
 * 16 and 3, but if we switch to using protobuf tiles they will be 15 and 2.
 */
const val SOUNDSCAPE_TILE_BACKEND = false
val ZOOM_LEVEL = if(SOUNDSCAPE_TILE_BACKEND) 16 else 15
var GRID_SIZE = if(SOUNDSCAPE_TILE_BACKEND) 3 else 2

/**
 * The default tile server is the one out in the cloud where the tile JSON is at:
 *    https://server/protomaps.json
 *
 * and the tiles are at
 *    https://server/protomaps/{z}/{x}/{y}.mvt
 */
const val PROTOMAPS_SERVER_BASE = "https://d1wzlzgah5gfol.cloudfront.net"
const val PROTOMAPS_SERVER_PATH = "protomaps"
const val PROTOMAPS_SUFFIX = "mvt"

/**
 * It's also useful to be able to use tiles served up locally when testing. When I
 * test locally I'm serving up the file like this:
 *
 *    tileserver-gl-light --file europe.pmtiles -b 192.168.86.39
 *
 * With this configuration the tile JSON descriptor appears at:
 *    http://192.168.86.39:8080/data/v3.json
 *
 * and the tiles within it are at:
 *    http://192.168.86.39:8080/data/v3/{z}/{x}/{y}.pbf
 *
 */
//const val PROTOMAPS_SERVER_BASE = "http://192.168.86.39:8080"
//const val PROTOMAPS_SERVER_PATH = "data/v3"
//const val PROTOMAPS_SUFFIX = "pbf"

