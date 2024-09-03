# Mapping in Soundscape
Mapping data is at the heart of Soundscape and there's a lot to think about!

## How did it work on Soundscape iOS?
The map in the UI on iOS is provided by Apple maps. It's proprietary, but more importantly free for native apps. The mapping data used for the audio descriptions comes from a [custom server]( https://github.com/Scottish-Tech-Army/soundscape-backend). This code was written by Microsoft and turns [OpenStreetMap](https://www.openstreetmap.org/) (OSM) data into GeoJSON tiles at a single zoom level for the whole world. The server is constantly updating from OSM and so should be up to date with all edits made to OSM data. This means that OSM edits can be made to improve the mapping around a Soundscape users location and those will be reflected very quickly on the Soundscape app. The GeoJSON tiles are downloaded by Soundscape and used to describe the nearby points of interest (POI) and roads.
Obviously there are two sources of information here, so the UI and the audio might not always match.


## How does it work on Soundscape Android?
Android doesn't have a source of free maps for UI, so we immediately need a new solution for the UI. We chose [MapLibre Native Android](https://maplibre.org/) as the library to render the map UI. It's very widely used, supports a vast array of tile types and styling and is still very actively developed.
Because we're now paying for the server costs involved in the map UI we need to take this into consideration when picking the source of the tiles of data. The best technology available seems to be [protomaps](https://protomaps.com/). This provides the most cost effective approach to serving mapping tiles. The [cost calculator](https://docs.protomaps.com/deploy/cost) they provide shows that 10 million tiles a month could be served for around £10.
The tiles are vector tiles (like compressed JSON) rather than image tiles (e.g. PNG) which means that they can be dynamically styled by MapLibre when it renders them on the phone. This opens up a number of new UI possibilities, but also means that we may be able to switch from using the soundscape-backend server GeoJSON to using the same vector tiles for audio as we do for UI.

## How many tiles?
This is the big question for server costing - how many tiles will users request from the server every month? The tiles are cached on the phone, and for the audio tiles only tiles immediately surrounding the user are downloaded and at a single zoom level. For the UI, zooming out and panning around the world means that far more tiles could be consumed. However, the majority of users will likely be using the app within a fairly fixed geography and the map UI is of limited use to many visually impaired. Although we know how many tiles we serve from soundscape-backend, we don't know how many Apple Maps currently serve up to the Soundscape iOS app.

## The path of the mapping data
This describes the journey of the data in the vector tiles.

### Data source
The tile server will be serving up a single [PMTiles](https://github.com/protomaps/PMTiles) file. Much of the data in that comes from OpenStreetMap, though there's other data pulled in too. We're currently using [planetiler](https://github.com/onthegomap/planetiler) to generate the `.pmtiles` file. The contents of the `.pmtiles` file is highly configurable. It contains vector tiles at a range of zoom levels, but the contents of the vector tiles can be configured to have whatever data we want in them.

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
`natural`
`physical_line`
`physical_point`
`pois`
`roads`
`water`

`planetiler` uses [this schema](https://github.com/openmaptiles/planetiler-openmaptiles https://openmaptiles.org/schema/) and the layers are described [here](https://github.com/openmaptiles/openmaptiles). When displaying the map in the app, a `style.json` file is used to describe how to display the contents of each of the layers. This includes where to get the tiles, the fonts and the icons from. The style has to be matched to the tiles as the naming convention for layers isn't fully standardized. If a layer isn't used in the style, then it should be removed from the vector tile generation. Designing the style is a huge amount of work and hopefully we can continue to use the same profile with minimum tweaking.

### Tile UI rendering
The MapLibre library is doing all of the work for the map rendering to the UI. A URL to a `style.json` is provided along with an API key for the tile provider and that's all it requires. It caches tiles and deals with all the tile decoding and rendering.

### Vector tiles for audio
We're currently looking at whether we can use the same tiles for the audio as for the UI. The answer appears to be yes, but the work is ongoing. The changes required in the app are:

1. Get the vector tile instead of the GeoJSON tile.
2. Decompress the vector tile into the same format as we parse GeoJSON. At this point we can likely discard a lot of the layers e.g. perhaps just parse the POIs and Roads.

Ensuring that we have the same data available as with `soundscape-backend` may take a little more work, but there is a great benefit to having a single source of mapping data.

## Performance
We need a lot more concrete data here - these are very initial thoughts.

The app performance has two aspects:

1. UI rendering - do vector tiles work well on low cost/older Android devices. This is wholly down to MapLibre and initial tests look good.
2. GeoJSON translation. This will be called regularly, but it's only every 10s of seconds rather than multiple times per second. My guess is that it's likely no less efficient than parsing from JSON.

The actual serving of the tile data is likely to be performant enough for us, though presumably there are all sorts of possible improvements that can be done on the Cloudflare side. The main challenge is really the generation of the tile data. The [protomaps cost calculator](https://docs.protomaps.com/deploy/cost) assumes that the `.pmtiles` file is generated offline and then uploaded to Cloudflare. The upload is free, but it is a large file. Here's what I've seen so far:

| Action | File size | Time in hours |
| ----------- | ----------- |----------- |
| Download of `planet.osm.pbf` | ~80GB | ~5 |
| Build the `.pmtiles` file with `planetiler` on linux box (AMD ThreadRipper with 64GB RAM and plenty of SSD) | 76GB | ~5 |
| Upload to Cloudflare | 76GB | >8 |

For reference the `planetiler` command used was:
```
java -Xmx40g -jar planetiler.jar --osm-path=planet-latest.osm.pbf --area=planet --bounds=planet --download --fetch-wikidata --output=output.pmtiles --nodemap-type=array --storage=mmap
```
And the build summary was:

```
Max tile sizes
                      z0    z1    z2    z3    z4    z5    z6    z7    z8    z9   z10   z11   z12   z13   z14   all
           boundary 5.4k   37k   43k   25k   20k   36k   29k   27k   22k   33k   36k   30k   31k   21k   17k   43k
          landcover 1.5k   990  8.1k  4.6k  3.2k   31k   17k  271k  334k  235k  153k  175k  166k  111k  333k  334k
              place  56k  150k  1.2M  1.1M  733k  360k  188k  123k   88k  118k  111k   95k   68k  123k  227k  1.2M
              water 8.7k  4.3k   11k  9.9k   17k   13k   91k  114k  132k  119k  133k   94k  167k  102k   91k  167k
         water_name  15k   32k   50k   28k   28k   16k  9.3k  9.5k   13k  9.9k  7.4k    6k  3.9k  9.6k   29k   50k
           waterway    0     0     0   549  3.7k  1.6k   18k   13k  9.8k   28k   20k   16k   59k   75k   88k   88k
            landuse    0     0     0     0  2.6k  3.6k   54k   77k  116k  113k  102k  112k   56k  115k   48k  116k
               park    0     0     0     0   56k  204k  130k   87k   99k   83k   90k   55k   47k   19k   50k  204k
     transportation    0     0     0     0   59k   59k   59k  123k   88k  134k   91k   68k  313k  190k  129k  313k
transportation_name    0     0     0     0     0     0   38k   22k   19k   14k   37k   23k   36k   28k  179k  179k
      mountain_peak    0     0     0     0     0     0     0   18k   20k   17k   15k   14k   11k  337k  234k  337k
    aerodrome_label    0     0     0     0     0     0     0     0  8.8k  5.5k    8k  4.5k  4.1k  3.5k  3.3k  8.8k
            aeroway    0     0     0     0     0     0     0     0     0     0   16k   27k   36k   31k   17k   36k
                poi    0     0     0     0     0     0     0     0     0     0     0  2.1k   69k   32k    1M    1M
           building    0     0     0     0     0     0     0     0     0     0     0     0     0  141k  709k  709k
        housenumber    0     0     0     0     0     0     0     0     0     0     0     0     0     0  315k  315k
          full tile  87k  224k  1.3M  1.2M  773k  532k  410k  475k  412k  431k  305k  216k  398k  341k  1.3M  1.3M
            gzipped  51k  127k  637k  583k  385k  281k  248k  327k  292k  303k  220k  154k  210k  227k  779k  779k

    Max tile: 1.3M (gzipped: 779k)
    Avg tile: 143k (gzipped: 86k) using weighted average based on OSM traffic
    # tiles: 271,427,243
    # features: 3,479,181,963
Finished in 1h18m15s cpu:34h7m23s gc:6m10s avg:26.2
   read    2x(13% 10m17s wait:48m41s done:12s)
   merge   1x(14% 10m55s wait:1h2m14s done:16s)
   encode 30x(75% 58m54s block:3s wait:8m26s done:16s)
   write   1x(7% 5m36s wait:1h9m36s done:16s)
 Finished in 4h59m29s cpu:76h10m16s gc:11m13s avg:15.3
 FINISHED!

 	overall          4h59m29s cpu:76h10m16s gc:11m13s avg:15.3
 	wikidata         6m34s cpu:3h3m54s gc:14s avg:28
 	lake_centerlines 3s cpu:21s avg:6.9
 	water_polygons   1m27s cpu:36m29s gc:7s avg:25.3
 	natural_earth    37s cpu:38s avg:1
 	osm_pass1        20m23s cpu:2h28m47s gc:7s avg:7.3
 	osm_pass2        2h16m15s cpu:33h58m31s gc:3m31s avg:15
 	ne_lakes         0.4s cpu:0.6s avg:1.4
 	boundaries       11s cpu:16s avg:1.5
 	agg_stop         0.5s cpu:0.9s avg:2
 	sort             55m29s cpu:1h53m39s gc:1m4s avg:2
 	archive          1h18m15s cpu:34h7m23s gc:6m10s avg:26.2
 ----------------------------------------
 	archive	76GB
```

This was using the default styling and configuration.

There's lots of information on the planetiler github on how to optimize planet sized tile generation: 
https://github.com/onthegomap/planetiler/blob/main/PLANET.md. More memory is the main accelerator, and in their [example](https://github.com/onthegomap/planetiler/blob/main/PLANET.md#example) they were using [a cloud server](https://www.digitalocean.com/pricing/droplets) with 128GB of RAM and it took 3hrs21min to do everything including the downloading of the OSM data. According to the cloud company they use that would have cost ~$5. The speed of connection in the cloud to both get the OSM data and push it back to Cloudflare obviously makes this much faster than running on my local machine.

If we could run a similar cloud build, then a weekly rebuild of the planet would be relatively cheap at only ~£20 per month. OSM publish the `planet-latest.osm.pbf` once per week so it could be timed to build from that.

### Limitations
`planetiler` is limited to a maximum zoom level of 15. Tiles at that zoom level are 1222 metres x 1222 metres and with `extents` set to 4096 have a resolution of 30cm. MapLibre can still zoom in further, and although the rendering is done at the higher zoom the data used will be from the zoom level 15 tile. Soundscape iOS GeoJSON tiles were at zoom level 16 so some care will have to be taken with style and configuration to ensure that we have the same data available.

`planetiler` cannot do incremental tile generation, it always starts from scratch. The `planet.osm.pbf` can be updated incrementally, but the main time sink is the tile generation. However, this could be argued to be a benefit as any issues creating tiles in one build would hopefully be gone by the next build rather than slowly accruing problems.