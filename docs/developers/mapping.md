---
title: Mapping data
layout: page
parent: Information for developers
has_toc: false
---

# Mapping in Soundscape
Mapping data is at the heart of Soundscape and there's a lot to think about!

## How did it work on Soundscape iOS?
The on screen map UI on iOS is provided by Apple maps. It's proprietary, but more importantly free for native apps. The mapping data used for the audio descriptions comes from a [custom server]( https://github.com/Scottish-Tech-Army/soundscape-backend). This code was written by Microsoft and turns [OpenStreetMap](https://www.openstreetmap.org/) (OSM) data into GeoJSON tiles at a single zoom level for the whole world. The server is constantly updating from OSM and so should be up to date with all edits made to OSM data. This means that OSM edits can be made to improve the mapping around a Soundscape users location and those will be reflected very quickly on the Soundscape app. The GeoJSON tiles are downloaded by Soundscape and used to describe the nearby points of interest (POI) and roads.
Obviously there are two sources of information here, so the UI and the audio might not always match.


## How does it work on Soundscape Android?
Android doesn't have a source of free maps for UI, so we immediately need a new solution for the UI. We chose [MapLibre Native Android](https://maplibre.org/) as the library to render the map UI. It's very widely used, supports a vast array of tile types and styling and is still very actively developed. In addition to the regular map layer, it's also possible to add GeoJSON layers which is very useful for debugging. The code can create a layer showing the current tile boundaries, or the current shapes used to determine what's to the left/right/straight ahead, or even to show every road, intersection and POI that we're using for the tiles.

Because we're now paying for the server costs involved in the map UI we need to take this into consideration when picking the source of the tiles of data. The best technology available that we found is [protomaps](https://protomaps.com/). This provides the most cost effective approach to serving mapping tiles. The [cost calculator](https://docs.protomaps.com/deploy/cost) they provide shows that 10 million tiles a month could be served for around £10.
The tiles are vector tiles rather than image tiles (e.g. PNG) which means that they can be dynamically styled by MapLibre when it renders them on the phone. This opens up a number of new UI possibilities, but also means that we can switch from using the soundscape-backend server GeoJSON to generating GeoJSON from the protomaps vector tiles. This gives us a number of benefits:

1. A single source of mapping data. The graphical and audio UI are both being served from the same data source. If the OSM data is updated then both the graphics and the audio will update at the same time.
2. A single server to maintain. `soundscape-backend` is very expensive to run and by using the protomaps server instead we no longer require it for Android Soundscape.
3. Greater control of our map data. We can easily alter what's in our map data and tailor it for our application.

## The path of the mapping data
This describes the journey of the data in the vector tiles.

### Data source
The tile server is serving up a single [PMTiles](https://github.com/protomaps/PMTiles) file. Much of the data in that comes from OpenStreetMap, though there's other data pulled in too. We're using [planetiler](https://github.com/onthegomap/planetiler) to generate the `.pmtiles` file. The contents of the `.pmtiles` file is highly configurable and we have customized it (details further down). It contains vector tiles at a range of zoom levels, but the contents of the vector tiles can be configured to have whatever data we want in them. We use the greatest zoom level (15) as the source of our GeoJSON for the audio callouts.

### Vector tiles
We use [MapBox vector tiles (mvt)](https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/). They are [protobuf](https://github.com/protocolbuffers/protobuf) encoded and provide a tile of mapping data similar to GeoJSON but much compressed.
Decoding from protobuf is handled automatically in Android, all that's required is a `.proto` file which describes the format. In our case it's [vector_tile.proto](https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto). However, we still need to decode the vector tile data from its compressed format.

There are two ways in which the vector tiles compresses the data:

1. The tile contains a list of strings which make up keys and values. Strings are then referenced by their id when used in each `feature`. This means that each string is only stored once per tile.
2. Latitude and longitude for each node within a tile are reduced down to an x,y integer position relative to the top left corner of the tile. The x,y values are stored as an accumulating list of cursor moves so that they are mostly deltas which are even more compressible. The tile is square and has an `extents` value which declares the length of its side. The origin of the tile in latitude and longitude is known, and so the x,y position can be easily converted into latitude and longitude. The default value for `extents` is `4096` and this does mean that the resolution of the locations within the tile varies with zoom level. At zoom level 0 a single tile represents the whole world, at zoom1 level 1 there are 4 tiles and so on down through the zooms. The size of a tile in metres is therefore the circumference of the earth divided by 2<sup>zoom-level</sup> = (2 x $\pi$ x 6378137) / (2<sup>zoom-level</sup>). At a zoom of 15 and with `extents` at the default of 4096, that gives a resolution of 30cm so that's well within what we need for our mapping calculations.

### PMTile
The file that ends up on the server is a single `.pmtiles` file which consists of all of the vector tiles smashed together into a single file with a metadata index. The server uses that metadata to find each tile so that when a request like:
```
https://api.protomaps.com/tiles/v3/$tileZoom/$tileX/$tileY.mvt
```

is received it can return the piece of the `.pmtiles` file corresponding to that tile. Nothing other than the server knows that the vector tile was stored in protomaps format, the client just receives a regular MapBox vector tile.

### What's in a tile?
If you looked at the [MapBox vector tiles specification](https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/) you'd notice that there's not much in there other than layers, lines, points and properties. What actually goes into the tile, and how that data is then used within the app can all be configured. The list of layers on a tile might be:

`earth`
`landuse`
`water`
`pois`
`transportation`
`transportation_name`

The default `planetiler` build uses [this schema](https://github.com/openmaptiles/planetiler-openmaptiles https://openmaptiles.org/schema/) and the layers are described [here](https://github.com/openmaptiles/openmaptiles). When displaying the map in the app, a `style.json` file is used to describe how to display the contents of each of the layers. This includes where to get the tiles, the fonts and the icons from. The style has to be matched to the tiles as the naming convention for layers isn't fully standardized. If a layer isn't used in the style, then it should be removed from the vector tile generation. Designing the style is a huge amount of work and hopefully we can continue to use the same profile with minimum tweaking.

### Tile UI rendering
The MapLibre library is doing all of the work for the map rendering to the UI. A URL to a `style.json` is provided and that's all it requires. It caches tiles and deals with all the tile decoding and rendering.

## Performance
We need more concrete data here - these are very initial thoughts.

The app performance has two aspects:

1. UI rendering - do vector tiles work well on low cost/older Android devices. This is wholly down to MapLibre and initial tests look good.
2. GeoJSON translation. This is called regularly, but it's only every 10s of seconds rather than multiple times per second. It seems fast enough, though we should look at this more closely.

The actual serving of the tile data is performant enough for us, though presumably there are all sorts of possible improvements that can be done on the server side. Our initial installation is using AWS which although slightly more expensive than Cloudflare apparently has a lower latency.

The main performance challenge is the generation of the tile data. The [protomaps cost calculator](https://docs.protomaps.com/deploy/cost) assumes that the `.pmtiles` file is generated offline and then uploaded to the cloud. That's what I'm currently doing, and a map of Europe takes around 1hr20 and is 53GB in size. The upload is purely down to bandwidth and for me it takes ~8 hours. By moving the map generation to AWS we should essentially remove the upload time, though obviously we'll be paying for a relatively large instance to generate the maps.

There's lots of information on the planetiler github on how to optimize planet sized tile generation: 
https://github.com/onthegomap/planetiler/blob/main/PLANET.md. More memory is the main accelerator, and in their [example](https://github.com/onthegomap/planetiler/blob/main/PLANET.md#example) they were using [a cloud server](https://www.digitalocean.com/pricing/droplets) with 128GB of RAM and it took 3hrs21min to do a world map including the downloading of the OSM data. According to the cloud company they use that would have cost ~$5.

If we could run a similar cloud build, then a weekly rebuild of the planet would be relatively cheap at only ~£20 per month. OSM publish the `planet-latest.osm.pbf` once per week so it could be timed to build from that. It's available in S3 and so should be very quick to download to our builder instance.

## What's required to build the protomaps file?
There are 3 repositories that are currently use by planetiler. These have been forked and are currently all in davecraig's GitHub account.
1. The [openmaptiles repo](https://github.com/davecraig/openmaptiles). The layer mappings here are read directly from github by `planetiler-openmaptiles` to generate planetiler code that will match the openmaptiles configuration. The fork contains changes to increase the number of types of shop that appear as POI, and add junction (for roundabouts) and road crossings support.
2. The [plantiler-openmaptiles repo](https://github.com/davecraig/planetiler-openmaptiles). This is the main place where maps can be customize and we have several changes that we've had to make:
   - Changes to the `Transportation.Java` code so that only roads/paths with the same OSM id are merged into multi-strings. This is done to can ensure that the OSM id in the resulting data is correct. Without it, un-named roads get merged together to make them more efficient to draw. However, we process these roads and need them to have correct metadata so that we can correctly identify intersections.
   - Add junction tags at to tiles at maximum zoom level (15). This ensures that any `roundabout` tags propagate through to our GeoJSON which makes it easier to describe them.
   - Add NAPTAN metadata to bus stops. Simply passing through more metadata from OSM rather than discarding it.
   - Add `crossing` to `Transportation.Java`. This includes metadata for dropped kerbs, tactile paving etc. 
   - Add POI with Polygon in addition to POI at Point. Our graphical map uses the Point POI, but for audio it's more useful to know the Polygon as the distance can be calculated to the nearest point rather than to the central point.
3. The [planetiler repo](https://github.com/davecraig/planetiler). Ideally we don't have to make changes here, however I did have a fix here prior to it being accepted upstream. Because the change has now landed we don't currently need this repo. But it's useful to see the history:
   - The change disabled simplification at zoom level 15. Simplification removes nodes from lines and polygons that would have no effect on a drawn graphical map. The easiest example is a straight line consisting of 3 nodes. The middle node adds nothing to how the line is drawn and so can be simplified away. Obviously the simplification becomes very important the more you zoom out from the map as more and more nodes can be simplified away. It's important for our translation to GeoJSON that we preserve all intersection nodes, and the easiest way to do this is to disable simplification at the maximum zoom level. The intersection nodes are all preserved and we can build up our own list of intersections.

Note that all of the changes we have made are to support our MVT to GeoJSON translation which we only perform at zoom level 15. As a result, only that zoom level of tile should be affected, all other remain unchanged.

There are several steps to build a map, but it's quite straightforward. `planetiler` is built using Maven and then we just have to run it:

```
# Clone the repo
git clone git@github.com:davecraig/planetiler-openmaptiles.git
cd planetiler-openmaptiles
# Ensure that the generated files are up to date with the `openmaptiler` repo
scripts/regenerate-openmaptiles.sh
# Build the code
scripts/build.sh
# We now have something that we can run to generate a map. This is what takes all the time.
java -Xmx30g -jar target/planetiler-openmaptiles-3.15.1-SNAPSHOT-with-deps.jar --force --download --area=planet --fetch-wikidata --output=planet.pmtiles --nodemap-type=array --storage=mmap --maxzoom=15 --render_maxzoom=15 --simplify-tolerance-at-max-zoom=-1
# Once complete we have to upload the file to the cloud
```


### Limitations
`planetiler` is limited to a maximum zoom level of 15. Tiles at that zoom level are 1222 metres x 1222 metres (at the equator) and with `extents` set to 4096 have a resolution of 30cm. MapLibre can still zoom in further, and although the rendering is done at the higher zoom the data used will be from the zoom level 15 tile. Soundscape iOS GeoJSON tiles were at zoom level 16 so some care will have to be taken with style and configuration to ensure that we have the same data available.

`planetiler` cannot do incremental tile generation, it always starts from scratch. The `planet.osm.pbf` can be updated incrementally, but the main time sink is the tile generation. However, this could be argued to be a benefit as any issues creating tiles in one build would hopefully be gone by the next build rather than slowly accruing problems.
