---
title: Unit test example
layout: page
parent: Information for developers
has_toc: false
---

# Unit tests for developing geo ideas

Android Studio debugger is at its quickest when running the unitTest which run locally on the host. androidTest and the app itself run on a phone or an emulator and there's more going on and the debugger though generally very good can fail to connect or other threads can kill the debug thread if it's paused for too long etc. As a result, being able to run a unit test with all of the mapping data is a very useful way to start on any new geo feature or to debug an old one.

At this point, it's worth making sure that you are familiar with the [geoengine documentation]({% link developers/geoengine.md %}).

## The boilerplate required

The tests have grown organically and as always there are improvements that could be made. However, here's what's currently required.

Most unit tests create grids of geo data just like the app. These are 2x2 grids of decoded map tiles at the maximum zoom level surrounding the current location. The map data is taken from pmtiles found in `offlineExtractPath` which is currently set to `src/test/res/org/scottishtecharmy/soundscape`. The unit tests use 3 separate map extracts and a manifest which can be downloaded from Cloudflare:

```
wget https://pub-0a3501283b024ab3bbfbb6d1e217f5d0.r2.dev/street-metadata/bristol-gb.pmtiles  -O app/src/test/res/org/scottishtecharmy/soundscape/bristol-gb.pmtiles
wget https://pub-0a3501283b024ab3bbfbb6d1e217f5d0.r2.dev/street-metadata/glasgow-gb.pmtiles -O app/src/test/res/org/scottishtecharmy/soundscape/glasgow-gb.pmtiles
wget https://pub-0a3501283b024ab3bbfbb6d1e217f5d0.r2.dev/street-metadata/liverpool-gb.pmtiles -O app/src/test/res/org/scottishtecharmy/soundscape/liverpool-gb.pmtiles
wget https://pub-0a3501283b024ab3bbfbb6d1e217f5d0.r2.dev/manifest.geojson.gz -O app/src/test/res/org/scottishtecharmy/soundscape/manifest.geojson.gz
```

Note that these extracts are not updated as regularly as the main map extracts and are kept specifically for unit tests. When we do update them, there's normally some work to do to fix test result changes due to the updated map data.

Here's an example of code creating the grids:
```
val currentLocation = LngLatAlt(-4.3060165, 55.9475021)
val gridState = getGridStateForLocation(currentLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
val settlementState = getGridStateForLocation(currentLocation, 12, 3)
```

Note that as well as the 2x2 maximum zoom grid, it's also creating a 3x3 grid at zoom level 12. This grid only decodes settlement names i.e. hamlets, villages, neighbourhoods, towns and cities. It's used in the app to describe the location when the user is in a car or train (moving faster than 5m/s). We also use those settlement names during offline search.

With those lines of code we now have all of the geo data required to run most of our callouts and analysis. There are numerous unit tests to test the APIs that access the geo data e.g. `testNearestFeatures` or `intersectionsRightTurn`. There are also tests that replay GPX journeys and replay callouts (`testCallouts`) and ones that test offline reverse geocoding (`replayStreetNumbers`).

The crucial data other than the grid data is the `UserGeometry`. That class includes all of the data about what location the user is at, which direction they are travelling and what the most likely street is that they are on. For some unit tests the location is all that is required, for others the heading (direction) is important too and for some the most likely street (`mapMatchedWay` and `mapMatchedLocation`) are vital. The latter can be manually set within unit tests by searching for the nearest road, or in the same way as the app by running the map matching algorithm which is what `testCallouts` does.

## Using the geo data

The main API for accessing geo data is to use `FeatureTree` e.g.
```
val poi = gridState.getFeatureTree(TreeId.POIS)
val nearestPoi = poi.getNearestFeature(currentLocation, gridState.ruler)
```

This simply returns the nearest `Feature` within the POIS `FeatureTree`. There are a number of trees, all containing different types of `Feature`. `ROADS_AND_PATHS` and `INTERSECTIONS` are other ones which are widely used.

The road and path network code adds another layer of complexity. All `Feature` in the `INTERSECTIONS` tree are actually of type `Intersection`. These make up a graph of the roads and paths along with the `Way` which connect them. Each `Way` has an `Intersection` at each end, and each `Intersection` has a list of `Way` that intersect at it. We have a Djikstra implementation which can find the shortest route through this network, but mostly we traverse it so that we can better describe the `Way` (see name confection in the [geoengine documentation]({% link developers/geoengine.md %}).

Searching the tree is not very expensive (sub 1ms), but following `Way` is in general much more efficient.

## Test outputs

Many tests are real tests in that they fail if a function returns a value deemed incorrect. However, there are a lot of tests where the aim is to output data for the developer to view e.g. `testCallouts`. In those cases, the output is usually a mix of GeoJSON which can be easily visualized in https://geojson.io/ or text files. Looking at a GeoJSON map is often a good way to spot errors e.g. train tracks in the `ROAD` `FeatureTree` or missing `INTERSECTIONS` that can't necessarily be done programmatically