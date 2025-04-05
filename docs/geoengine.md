---
tags: [Mermaid]
mermaid: true
---
## GeoEngine introduction
The GeoEngine module takes care of parsing map tile data and making that data available to the rest of the app for generating audio callouts and some UI. The classes at the bottom levl are based around GeoJSON objects as per https://en.wikipedia.org/wiki/GeoJSON. Originally, the app was parsing GeoJSON from the soundscape-backend server and this was the natural result. However, now that we've switched to parsing Mapbox Vector Tiles the use of GeoJSON classes is somewhat legacy, though it does ease debugging as it's easy to output GeoJSON and render it on top of other maps e.g. using [geojson.io](https://geojson.io) . Here's a simplified view of the basic classes involved:

<div class="mermaid">
classDiagram
    class Feature{
        // A GeoJSON Feature
        geometry: Point/LineString/Polygon
        properties: HashMap
        foreign: HashMap
    }
    class FeatureCollection{
        // A container for Features from GeoJSON
    }
    class Way {
        // Each way represents a road/path segment between two intersections
        val intersection: Intersection[2] // Start and End of this Way
        fun followWays(fromIntersection: Intersection?)
        fun getName(direction: Boolean): String
        fun doesIntersect(other: Way): Boolean
        fun direction(fromIntersection: Intersection, deviceHeading: Double) : Direction
        fun heading(fromIntersection: Intersection): Double
    }
    class Intersection {
        // An Intersection is the point at which multiple Ways connect
        val members: List of Way
        val location: LngLatAlt

    }
    Way <|-- Feature
    Intersection <|-- Feature
    FeatureCollection *-- Feature

    class FeatureTree{
        // An r-tree for searching Features based on location
        fun getNearestFeature(location: LngLatAlt, distance: Double): Feature?
        fun getNearestCollection(location: LngLatAlt, distance: Double, maxCount: Int, initialCollection: FeatureCollection?): FeatureCollection
        fun getNearestCollectionWithinTriangle(triangle: Triangle, maxCount: Int): FeatureCollection
        fun getContainingPolygons(location: LngLatAlt) : FeatureCollection


    }
    FeatureTree *-- Feature
</div>

Features are points, lines or polygons along with some metadata describing what they are. We construct these based on the Mapbox Vector Tile contents and then divide them up into separate searchable FeatureTrees depending on the type of feature they are.

## Parsing the MVT (MapBox Vector Tiles)
Each MVT tile is processed layer by layer creating Features for all of the points that we are interested in. The main layers of interest are the POI which contains points and polygons for all of the various points of interest, and the transportation layers which contains roads and paths. We don't currently parse other layers (e.g. groundcover, water, housenumbers), though we may add some of those in future if we want to use that data e.g. rivers and streams.
After the initial parsing, some post-processing is done on the roads with the aim of:

* Finding Intersections so that we can call them out.
* Creating a Way for each segment of road/path between Intersections. Many of these will contain the same metadata because the original road/path in the tile stretched across multiple intersections.

## The TileGrid
At any time the app is working on a 2x2 grid of tiles. After parsing each tile independently, Ways which cross the tile boundaries need joining together, as do Polygons. The result is a set of FeatureCollections which cover the whole grid. The post processing on the grid is then:

* Categorising POIs into the same super categories as were present in iOS e.g. landmark, safety, mobility etc.
* The FeatureCollection for each category is then used to create a FeatureTree. That enables fast searching to find the nearest Feature, be that roads, intersections or POIs.
* Confect names for un-named ways (see below)

## Use the FeatureTrees
Searching the FeatureTrees is reasonably efficient, though multiple searches should be avoided due to CPU performance costs. They are very useful when generating the audio callouts. For nearby POI, `getNearestCollection` can be called to find POI within a certain distance from the user location. The alternative `getNearestCollectionWithinTriangle` can be used to search within a triangle rather than within a circle. If we do this with a `Triangle` created with one corner at the user location we can do a 'field-of-view' search. This also works for searching roads and paths.

## Traverse the Ways and Intersections
Once code has an Intersection or a Way it can follow Ways across the map tiles. Each Way has a reference to Intersections at either end and each Intersection contains a list of all of the Ways which comprise it. This traversal is very efficient and makes these features straightforward:

* Street Preview. It's possible to implement the Way traversal used by Street Preview using just FeatureTrees, but it's far more efficient to use Ways and Intersections.
* Name confection. This is how we add context to un-named paths and service roads. For each intersection that contains at least one named way (e.g. `"Roselea Drive"` rather than just `"path"`) we add a tag to each un-named way that joins that intersection. The tag will either be `"destination:forward"` or `"destination:backward"` depending on which way the Way runs (in to or out of the intersection). These can be used when describing paths later when approaching intersections e.g. `"Path to Roselea Drive"` when travelling one way, or `"Path to Mosswell Road"` when travelling in the opposite direction. We also add tags indicating dead-ends which is a useful way of filtering callouts e.g. don't call out un-named dead-ends - there are a lot of dead-end service roads in my local area, and calling them adds little context.
 
## Map Matching
When describing intersections, it's very important to describe them from the correct road coming into the intersection. The `MapMatchFilter` is the class which aims to track that road. Map matching is the term used to describe matching raw GPS locations on to map data. This is relatively straightforward in car satnavs, but doing it for pedestrians is a bit harder. Pedestrians can walk on paths, in any direction, cross roads and indeed cross open space not even using a path. There are lots of algorithms based on hidden Markov models that are designed to do this, but many of them are aimed at matching already completed GPS tracks rather than predicting live the next location. In fact it's possible that most applications don't actually need pedestrian map matching. The paper on which our algorithm is based is

[An Improved Map-Matching Technique Based on the Fréchet Distance Approach for Pedestrian Navigation Services" by Yoonsik Bang, Jiyoung Kim and Kiyun Yu.](https://pmc.ncbi.nlm.nih.gov/articles/PMC5087552/)

However, this paper doesn't describe how to create the paths on which to run the algorithm. I think our approach can be improved, but it does work well enough for a start.

## APIs
Most of the GeoEngine APIs use two pieces of data:

* `GridState` which contains all of the `FeatureTrees` for the current `TileGrid`
* `UserGeometry` which contains all of the information about the current user location, headings and map matched location. The heading calculations aim to match iOS which uses the phone heading, GPS travel heading as well as head tracking heading (not yet implemented on Android). It's possible to have no heading at all if the user isn't moving and the phone is locked and the phone is not held flat.

