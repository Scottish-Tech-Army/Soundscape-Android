package org.scottishtecharmy.soundscape.geoengine

/**
 *  This file contains the various configuration options for the GeoEngine.
 */

/**
 * The zoom level and grid size were constant when using soundscape-backend at 16 and 3
 * respectively. With protobuf tiles the tile grid for walking around will be 15 and 2, but we can
 * also have a lower zoom level to allow us to give better context when we're travelling by faster
 * means of transport e.g. inter city train. The lower zoom levels mean that we can search for
 * the nearest village or town instead of just the nearest street or road.
 */
const val MAX_ZOOM_LEVEL = 14
const val MIN_MAX_ZOOM_LEVEL = 14
const val GRID_SIZE = 2

/**
 * The default tile server is the one out in the cloud where the tile JSON is at:
 *    https://server/protomaps.json
 *
 * and the tiles are at
 *    https://server/protomaps/{z}/{x}/{y}.mvt
 */
const val PROTOMAPS_SERVER_PATH = "protomaps"
const val PROTOMAPS_SUFFIX = "mvt"

const val MANIFEST_NAME = "manifest.geojson.gz"

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

