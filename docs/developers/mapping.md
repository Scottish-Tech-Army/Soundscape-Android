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
The tile server is serving up a single [PMTiles](https://github.com/protomaps/PMTiles) file. Much of the data in that comes from OpenStreetMap, though there's other data pulled in too. We're using [planetiler](https://github.com/onthegomap/planetiler) to generate the `.pmtiles` file. The contents of the `.pmtiles` file is highly configurable and we have customized it (details further down). It contains vector tiles at a range of zoom levels, but the contents of the vector tiles can be configured to have whatever data we want in them. We use the greatest zoom level as the source of our GeoJSON for the audio callouts.

That maximum zoom level is now **14**, having originally been 15. Dropping a level quarters the number of tiles in the planet-wide file, which makes it both cheaper to serve and very much smaller to download as an offline extract, and at 60cm resolution (see below) it's still far finer than anything our callout distances care about. The app's own `MAX_ZOOM_LEVEL` in `Configuration.kt` has to agree with whatever the maps were built at. Because the customizations are written to apply at whichever zoom level is the maximum, rather than being hard-coded to 15, changing it is a matter of changing the build arguments.

### Vector tiles
We use [MapBox vector tiles (mvt)](https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/). They are [protobuf](https://github.com/protocolbuffers/protobuf) encoded and provide a tile of mapping data similar to GeoJSON but much compressed.
Decoding from protobuf is handled automatically in Android, all that's required is a `.proto` file which describes the format. In our case it's [vector_tile.proto](https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto). However, we still need to decode the vector tile data from its compressed format.

There are two ways in which the vector tiles compresses the data:

1. The tile contains a list of strings which make up keys and values. Strings are then referenced by their id when used in each `feature`. This means that each string is only stored once per tile.
2. Latitude and longitude for each node within a tile are reduced down to an x,y integer position relative to the top left corner of the tile. The x,y values are stored as an accumulating list of cursor moves so that they are mostly deltas which are even more compressible. The tile is square and has an `extents` value which declares the length of its side. The origin of the tile in latitude and longitude is known, and so the x,y position can be easily converted into latitude and longitude. The default value for `extents` is `4096` and this does mean that the resolution of the locations within the tile varies with zoom level. At zoom level 0 a single tile represents the whole world, at zoom1 level 1 there are 4 tiles and so on down through the zooms. The size of a tile in metres is therefore the circumference of the earth divided by 2<sup>zoom-level</sup> = (2 x $\pi$ x 6378137) / (2<sup>zoom-level</sup>). At our zoom of 14 that's a tile 2446m square, and with `extents` at the default of 4096 that gives a resolution of 60cm, which is well within what we need for our mapping calculations. (At zoom 15, which we used previously, it was a 1223m tile at 30cm resolution.)

### PMTile
The file that ends up on the server is a single `.pmtiles` file which consists of all of the vector tiles smashed together into a single file with a metadata index. The server uses that metadata to find each tile so that when a request like:
```
https://api.protomaps.com/tiles/v3/$tileZoom/$tileX/$tileY.mvt
```

is received it can return the piece of the `.pmtiles` file corresponding to that tile. Nothing other than the server knows that the vector tile was stored in protomaps format, the client just receives a regular MapBox vector tile.

### What's in a tile?
If you looked at the [MapBox vector tiles specification](https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/) you'd notice that there's not much in there other than layers, lines, points and properties. What actually goes into the tile, and how that data is then used within the app can all be configured. The list of layers on a tile might be:

`building`
`housenumber`
`landcover`
`landuse`
`place`
`poi`
`transportation`
`transportation_name`
`water`
`water_name`
`waterway`

(That's the set actually present in our tiles around Glasgow; layers like `aeroway` or `mountain_peak` only appear where there's something to put in them.) Of these, the GeoEngine parses `transportation`, `transportation_name`, `poi`, `building`, `housenumber`, `water` and `waterway`, plus `place` at the lower zoom used for settlements. The rest, including `water_name`, are there for the graphical map — water names now come from the `name` we add to the `water` polygons themselves, which is what makes a bay or firth crossing detectable. `landcover` and `landuse` now carry names as well, but nothing parses them yet: they're in the tiles ahead of the app work, the way the `water` names were.

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

The build script now does the "is there a new planet file?" part of this itself — see `build-planet-map.sh` below — so a scheduled rebuild only costs anything on the weeks the planet file has actually changed.

## What's required to build the protomaps file?
There are 3 repositories that are currently use by planetiler. These have been forked and are currently all in davecraig's GitHub account.

### 1. The [openmaptiles repo](https://github.com/davecraig/openmaptiles)
The layer mappings here are YAML, read directly from github by `planetiler-openmaptiles` to generate planetiler code that will match the openmaptiles configuration. Everything here is about making OSM tags *reach* the Java layer code; what is then done with them is in the next repo. The fork:

* **Loosens the POI mappings to `__any__`** for `amenity`, `historic`, `leisure`, `office`, `shop`, `sport` and `tourism`. Upstream has explicit allow-lists — roughly 86 shop values, 61 office values, 57 amenity values and so on — and anything not on them is dropped. We first extended the shop list by hand to about 180 values taken from the OSM wiki, then gave up on enumeration and replaced the lists with the wildcard, which is why arbitrary POI types now appear. Values outside the upstream class mapping simply fall through to a generic class.
* **Adds `entrance`** as a POI mapping and as a column, so building entrances become POIs.
* **Adds a `junction` column** to `highway_linestring`, which is what carries `junction=roundabout`.
* **Adds a `footway` column** to `highway_linestring`, which distinguishes `sidewalk` from `crossing` from a plain path.
* **Adds `highway=crossing` to `highway_point`** along with a `crossing` column. Upstream's `highway_point` table holds only motorway junction nodes, so without this pedestrian crossings never reach the tiles at all.
* **Adds a `name` column to `building_polygon`**, so a building can be named without a separate POI node.

Note that these changes only add the columns to the imposm mapping. The layer schema YAML is untouched, so the attribute names that actually come out of the tiles are decided by the Java in the next repo.

A mapping change here is also the *expensive* way to add something, because it means pushing to this repo and then regenerating `Tables.java` in the next one from GitHub. Most of what follows doesn't need it: a tag that's already on a feature reaching a layer can be read straight off the `SourceFeature`, and a tag in no mapping at all can be picked up from the raw OSM feature in `processAllOsm`. The `building_polygon` column above is from before that was understood; nothing since has needed one.

### 2. The [planetiler-openmaptiles repo](https://github.com/davecraig/planetiler-openmaptiles)
This is the main place where maps are customized. Two rules apply throughout:

* Almost every addition below is emitted **only at the maximum zoom level**, via `config.maxzoom()` rather than a hard-coded number. Lower zoom levels are untouched, so the graphical map is unaffected and the extra data costs nothing at the zooms where most tiles get served. Writing it against `config.maxzoom()` rather than against 15 is what made the move to zoom 14 a build-argument change.
* Attribute names are usually the literal OSM tag key, with the NAPTAN ones as the exception.

#### `transportation` layer

| Attribute | From | Why |
| --- | --- | --- |
| `name` | `name` | Upstream only puts names in `transportation_name`. Having it here means the GeoJSON translation doesn't have to correlate two layers. |
| `ref` | `ref` | Same reason. Refs were only in `transportation_name`, so individual segments didn't carry them and a motorway couldn't be named "M8" on the segment being driven. |
| `tunnel_name` | `tunnel:name` | So entering a tunnel can be announced by name. |
| `junction` | `junction` | Carries `roundabout`; describing roundabouts goes back to Soundscape iOS. |
| `footway` | `footway` | Gives us `sidewalk`, which is how pavements are identified. |

Also in this layer:

* **OSM ids are preserved at max zoom.** Planetiler merges adjacent line strings to make them cheaper to draw, which destroys the one-way-one-id relationship. A temporary `__osm_id_merge` tag is added before the merge and removed afterwards, so ways with different ids are never merged together. We need correct ids and metadata per way to identify intersections.
* **Pedestrian crossings** become points with `class=crossing`, carrying `crossing`, `button_operated`, `crossing:island`, `crossing:markings`, `kerb`, `tactile_paving` and `traffic_signals:sound`.
* **Rail stop nodes** become points carrying `name` and `ref`, from three taggings:

  | OSM | `class` | `subclass` |
  | --- | --- | --- |
  | `railway=stop` | `rail` | `stop` |
  | `railway=tram_stop` | `transit` | `tram_stop` |
  | `public_transport=stop_position` + `train=yes` | `rail` | `stop` |
  | `public_transport=stop_position` + `tram=yes` | `transit` | `tram_stop` |

  The `stop_position` forms matter because that's the newer tagging and some stations only have it — Bellgrove on the North Clyde Line is one. A node carrying both old and new tagging is emitted once, and a bus-only `stop_position` is ignored.

  These matter because a `railway=station` POI is a place *beside* the tracks, and where several lines run close together a train passes stations it doesn't call at. A stop node is on the line itself, so the line knows its own stops. They go in `transportation` rather than `poi` precisely so that they don't duplicate the station POI in lists of what's nearby. Because `railway=stop` is in no OpenMapTiles mapping at all, this is handled directly from the raw OSM feature rather than through the generated tables.

#### `water` layer
* **Bay polygons are emitted** rather than dropped, at max zoom only.
* **`name` on water polygons**, at max zoom. Upstream keeps water names in the separate `water_name` layer.
* **Bay and strait *lines*** are emitted with `class=bay`/`class=strait` and a `name`. A lot of water is mapped as a named line rather than a polygon — the Menai Strait and most of the Norwegian fjords are done this way — and without this there's nothing to name when crossing one.

#### `poi` layer
* **Polygon POIs are emitted twice**, as the polygon and then as its centroid point. MapLibre wants the point for labelling; we want the polygon so that distance can be measured to the nearest edge rather than to the middle. The polygon comes first so the parser finds it first.
* **Entrances** get `class=entrance`, `subclass` from the `entrance` tag, and a `railway` attribute for station entrances (`subway_entrance`, `train_station_entrance`).
* **NAPTAN attributes** for UK bus stops, which is how stops get the name that's actually on the timetable and the sign: `naptanCode`, `naptanAtcoCode`, `naptanBearing`, `naptanCommonName`, `naptanStreet`, `naptanLandmark`, `naptanIndicator` and `naptanLocalityName`, from the corresponding `naptan:*` tags. `Bearing` gives the direction a stop serves.
* **Address attributes**: `street`, `housenumber`, `block_number`, `neighbourhood`, `quarter` and `suburb`, from the matching `addr:*` tags.

#### `housenumber` layer
The same address attributes — `street`, `block_number`, `neighbourhood`, `quarter`, `suburb` — plus `housename` from `addr:housename`.

The block-level tags are there for Japan, where most addresses number a building within its block rather than along a street, so `addr:street` is usually absent and a housenumber on its own says very little.

Upstream's de-duplication has also been **removed**. It kept only one feature per street/block/housenumber group, preferring the unnamed one, which is reasonable for drawing labels and loses real addresses for our purposes. Identical housenumbers within a tile still collapse into a multipoint, which is only a size optimisation.

#### `building` layer
`name` and the raw `building` value (e.g. `warehouse`, `house`, `retail`) at max zoom.

#### `landcover` layer
**`name` at max zoom.** Upstream emits only `class` and `subclass` here, so a wood, forest or moor arrived anonymous — `Garadhban` was a `class=wood` polygon and `Drumclog Moor` a `subclass=heath` one, with nothing to call either of them. In open country the landcover name is often the only name there is. This also names `class=farmland`, which is how a named field or farm holding arrives.

Max zoom isn't just the house rule here, it's what keeps the change free. `postProcess` merges wood and forest polygons at z7–13 to make them cheaper to draw, and merging groups on the exact set of attributes a feature carries. Below max zoom the `name` resolves to null and is dropped, so those tiles are byte-for-byte what they were and the merging is untouched. If the build's maxzoom were ever dropped to 13 that would stop being true and differently named woods would stop merging into one another.

#### `landuse` layer
* **`name` at max zoom**, for the same reason. Incidentally names quarries, industrial estates, military areas and the `place=suburb`/`quarter`/`neighbourhood` polygons. One knock-on: this layer merges `class=residential` at z13+, so at max zoom two differently named estates no longer merge into each other — which is the point, since which estate you're in is the useful part. Unnamed residential still collapses into a single multipolygon.
* **`landuse=farmyard` becomes `class=farmyard`**, at max zoom. `farmyard` is in no OpenMapTiles mapping at all, so like `railway=stop` it's read from the raw OSM feature. It's emitted named or not, as every other class in this layer is. Note it had to go here rather than in `landcover`: this layer takes its `class` straight from the OSM tag, whereas `landcover` resolves `class` through a generated `subclass`→`class` table that has no `farmyard` in it, and an unmapped subclass there is silently dropped.

#### `place` layer
**`place=farm` becomes `class=farm`**, at max zoom, from the raw OSM feature — upstream's `city_point` mapping stops at `isolated_dwelling`. Nodes become points; the minority mapped as areas become a point on the surface. A name is required, which `city_point` gets from an imposm filter and this path has to check for itself.

Max zoom is deliberate rather than convenient. It does mean farms land in tiles the app doesn't read the `place` layer from — `place` is parsed only *below* the maximum zoom, from the z12 settlement grid — but a farm is not a settlement and has no business being picked up by the settlement lookahead. `isolated_dwelling` already sits at the same zoom for the same reason. Consuming farms later means either reading `place` at max zoom or deliberately lowering this one minzoom.

#### How a farm reaches the tiles
Worth setting out, because the answer is spread across four taggings and no single one of them is the farm:

| OSM | Layer | As |
| --- | --- | --- |
| `building=farm` (the farmhouse) | `building` | polygon with `name` and `building=farm` |
| `place=farm` (the farm as a locality) | `place` | point with `class=farm` and `name` |
| `landuse=farmyard` (buildings and yard) | `landuse` | polygon with `class=farmyard` |
| `landuse=farmland`, `landuse=farm` (the fields) | `landcover` | polygon with `class=farmland` and `name` |

A given farm may be tagged with any combination of these, or just one, which is why no single layer answers "where are the farms". Two neighbours west of Drymen make the point: `Hoish Farm` is a named `building=farm` and so was always in the tiles, while `Douchlage`, a few hundred metres away, is a `place=farm` node — so before this change it was absent entirely.

### 3. The [planetiler repo](https://github.com/davecraig/planetiler)
Ideally we don't have to make changes here, however I did have a fix here prior to it being accepted upstream. Because the change has now landed we don't currently need this repo. But it's useful to see the history:

* The change disabled simplification at the maximum zoom level. Simplification removes nodes from lines and polygons that would have no effect on a drawn graphical map. The easiest example is a straight line consisting of 3 nodes. The middle node adds nothing to how the line is drawn and so can be simplified away. Obviously the simplification becomes very important the more you zoom out from the map as more and more nodes can be simplified away. It's important for our translation to GeoJSON that we preserve all intersection nodes, and the easiest way to do this is to disable simplification at the maximum zoom level. The intersection nodes are all preserved and we can build up our own list of intersections. This is now the `--simplify-tolerance-at-max-zoom=-1` build argument, and it's paired with `--min_feature_size_at_max_zoom=0` so that small features aren't dropped either.

Note that all of the changes we have made are to support our MVT to GeoJSON translation which we only perform at the maximum zoom level. As a result, only that zoom level of tile should be affected, all others remain unchanged.

### Building a map
`planetiler` is built using Maven and then we just have to run it. The build and upload scripts all live in the `soundscape-maps/` directory of the planetiler-openmaptiles fork:

```
# Clone the repo
git clone git@github.com:davecraig/planetiler-openmaptiles.git
cd planetiler-openmaptiles
# Ensure that the generated files are up to date with the `openmaptiles` repo
scripts/regenerate-openmaptiles.sh
# Build the code
scripts/build.sh
cd soundscape-maps
# Build a single region for testing, e.g. ./build-map.sh scotland
./build-map.sh <area>
# ...or build the whole planet. This is what takes all the time.
./build-planet-map.sh -t <temp-dir> -o <output-dir>
# Once complete we have to upload the file to the cloud
./map-to-serve/s3_push.sh <file>
```

`build-planet-map.sh` compares the MD5 of the planet file we already have against the latest in OSM's `osm-planet-eu-central-1` S3 bucket, and only downloads and rebuilds if there's a newer one (`-f` forces a build regardless). That's what makes a scheduled rebuild practical — it can run on a timer and do nothing most of the time. `s3_push.sh` splits the file into 100 parts for a multipart upload, since it's far too big to push in one go, and `s3_continue.sh` has the incantations for resuming an upload that timed out.

The planet build passes `--languages=` (empty) to suppress the `name:xx` translation attributes, which would otherwise add a great deal of bulk for data the app doesn't use.

The same directory holds the two Python scripts that slice the planet file into the per-region offline extracts, and the `world_countries_and_city_groups.geojson` that defines the regions. Those are described in [Offline maps]({% link developers/offline-maps.md %}).

### Limitations
`planetiler` is limited to a maximum zoom level of 15, and we currently build at 14. Tiles at zoom 14 are 2446 metres square (at the equator) and with `extents` set to 4096 have a resolution of 60cm. MapLibre can still zoom in further, and although the rendering is done at the higher zoom the data used will be from the zoom level 14 tile. Soundscape iOS GeoJSON tiles were at zoom level 16 so some care has to be taken with style and configuration to ensure that we have the same data available.

`planetiler` cannot do incremental tile generation, it always starts from scratch. The `planet.osm.pbf` can be updated incrementally, but the main time sink is the tile generation. However, this could be argued to be a benefit as any issues creating tiles in one build would hopefully be gone by the next build rather than slowly accruing problems.
