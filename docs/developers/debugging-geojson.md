---
title: Debugging GeoJSON
layout: page
parent: Information for developers
has_toc: false
---

Here are some ideas on how best to debug GeoJSON. This was inspired by Adam's use of GeoJSON.io, and my laziness with copy and paste!

## Replay a recorded GPX and see the callouts on a map
When a user sends in a recording (the `travel.gpx` that the app shares), one command replays it and shows the result:
```
python3 -I scripts/replay-gpx.py ~/Downloads/travel.gpx
```
The script:
1. Reads the track and picks the offline extract(s) that cover it from the extract provider's manifest (`extractProviderUrl` in `local.properties`, or the `EXTRACT_PROVIDER_URL` environment variable). It downloads them once into `~/.cache/soundscape-replay/extracts`. Delete old builds from there by hand when space matters.
2. Runs the `MvtTileTest.replayExternalGpx` unit test. This is the same production-equivalent replay (`testMovingGrid`) that the GPX unit tests use, run here against those extracts.
3. Writes `app/build/test-output/replay/<name>.geojson`, `<name>.txt` (the callouts) and `<name>.html`, then opens the HTML. The page is self-contained: the GeoJSON is embedded, so it works from disk with no size limit. It shows the track, the raw and map-matched fixes, and every callout's field of view. Click a callout in the list to zoom to it. Tick **Inspect** and click the map to list the pmtiles features at that point, with their tags and ids. This includes features the style doesn't draw; hover over an entry to highlight its geometry.

The base map is drawn from the same pmtiles extract as the replay, in the OpenMapTiles project's own "OSM OpenMapTiles" style (`scripts/replay_basemap.py`). The page embeds the tiles around the track, the style and its icons, the glyphs its labels need, and MapLibre itself. It fetches nothing when opened, and a typical walk makes a page of around 10 MB. The style (OpenMapTiles v3.16), the Noto Sans glyphs and MapLibre are pinned and downloaded once into `~/.cache/soundscape-replay/basemap`.

Nothing is uploaded: the recording and the output stay on your machine.

Useful options are `--start N --end M` to replay only part of the track, and `--extract file.pmtiles ...` to use local extracts instead of the manifest. For several overlapping recordings from one walk, merge them first with the `merge-gpx` skill.

## Generate GeoJSON and view online
The workflow is as follows:

1. Generate a geojson output file from a unit test (for example see `testVectorToGeoJsonMilngavie` which generates `milngavie.geojson` in `app/build/test-output`). The filename must have the suffix `.geojson`.
2. Upload the file as a github gist. This is how I do it, first of the following 3 steps are done once, and then the 4th one can be repeated each time the GeoJSON is updated.
   1. Install the github client program `gh`
   2. Upload the gist e.g. `gh gist create app/build/test-output/milngavie.geojson`
   3. Get the ID for the new gist with `gh gist list` (referred to as `XXX_GIST_ID_XXX` in the following examples)
   4. When the file changes, upload a new version of it e.g. `gh gist edit XXX_GIST_ID_XXX app/build/test-output/milngavie.geojson`
3. Open the gist either
   1. In github (click on the user avatar and select _Your Gists_) which overlays the GeoJSON on Bing Maps, or
   2. In geojson.io by opening the URL `https://geojson.io/#id=gist:USERNAME/XXX_GIST_ID_XXX

The only limitation of this method is that the gist must smaller than 1MByte. Otherwise, it's a very quick way of viewing GeoJSON without having to copy or paste any text.

## Generate protomaps tiles to use in the above workflow
Getting hold of tiles from our server is straightforward e.g.
```
wget https://server_address/protomaps/15/17509/11948.pbf -O 17509x11948.mvt
```

However, an additional bit of workflow is required when making changes to the map where we want to test it prior to it being uploaded to the server. For this we have to build `planetiler` (see the instructions in the [mapping doc](mapping.md)). Then we need to use that to build a small map to test with. I build the whole of Scotland, though building Monaco is the default and very slightly quicker option e.g.
```
java -Xmx30g -jar target/planetiler-openmaptiles-3.15.1-SNAPSHOT-with-deps.jar --force --download --area=monaco --fetch-wikidata --output=monaco.pmtiles --nodemap-type=array --storage=mmap --maxzoom=15 --render_maxzoom=15 --simplify-tolerance-at-max-zoom=-1
```
It will take slightly longer to run the first time as it has to download the various data sources. However, it still takes around 100 seconds to run on my fairly fast linux PC. Once the mapping file has been generated there are various ways to use it:

1. View it via a server e.g.run [tileserver-gl-light](https://www.npmjs.com/package/tileserver-gl-light): `tileserver-gl-light --file monaco.pmtiles`. This allows viewing of the map both as a graphical UI map or as tile data where points can be queried.
2. Extract MVT tiles from the file. I use [go-pmtiles](https://github.com/protomaps/go-pmtiles) which does the trick:
   ```
   git checkout git@github.com:protomaps/go-pmtiles.git
   cd go-pmtiles
   go build main.go
   go install main.go
   main tile monaco.pmtiles 15 17509 11948 > 17509x11948.mvt
   ```
   For larger protomaps files (e.g. Europe, the output tiles are gzipped, and so they need to be unzipped as they come out e.g. `main tile monaco.pmtiles 15 17509 11948 | gunzip > 17509x11948.mvt`). The resulting mvt files can then be used from within unit tests etc.
