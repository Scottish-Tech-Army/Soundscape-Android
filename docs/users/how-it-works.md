---
title: How Soundscape works
layout: page
parent: Using Soundscape
has_toc: false
---
# How Soundscape works
The aim of this page is to give a general understanding of how the Soundscape app works under the hood. You don't need to read this to use the app, but there are a few reasons that it's been written:

1. To help any interested newcomers to the app to understand where its limitations are
1. To give users an idea of what else could be possible with new features
1. To give developers an overview of the apps function

There are two pieces of technology which make the app possible, GPS and OpenStreetMap data. The GPS gives us a good idea of where the phone is, and where it's been. OpenStreetMap data can then be used to find what's nearby and we can use it describe that to the user.

## Audio beacons
In most ways these are the simplest things to implement from a technology standpoint. Assuming we have the phone location, and a direction for the phone, we can then alter the audio for the beacon so that it sounds like it's coming from that direction. We use a gaming engine called FMOD to do the audio positioning and not only does it uses different volumes in each ear to suggest the direction, it also filters the beacon audio so that it sounds slightly muffled when it's behind us rather than in front of us. The only other thing we do is to change the beacon audio so that a different sound is played depending on the angle between the user direction and the beacon location. The angles vary depending on the beacon selected (Tactile, Flare, Ping etc.) and some have a larger number of sounds than others. And that's audio beacons at the simplest level.

The only additional complexity is the assumption on the phone location and the direction that the user is pointing in. Let's look at these in turn.

### Location
The location returned by GPS can have quite a large error, and this depends on how much of the sky is visible to the phones GPS, and how many trees and tall buildings are reflecting the GPS signal on the way to the phone.

The approach we've taken to filtering the location is to use what's known as map matching. This assumes that the user is most likely to be travelling along a mapped path or road - we use the term 'Way' to cover all roads, tracks and paths. Map matching looks at where the user has been and using the direction of movement along with local mapping data it picks the most likely location on a Way. This approach not only accounts for errors from the GPS, but also errors in the map data. Not all Ways are mapped accurately and so they have errors too. In order to determine which is the most likely Way that the user is on the algorithm considers: 
* How near a Way is to the GPS location and previous GPS locations
* The direction of travel - are they moving in the same direction as the Way
* Whether it's possible to get from the last map matched location to the new location via the network of Ways. This is required to rule out switching between Ways which aren't actually connected e.g. one passes over another by a bridge, or under it by a tunnel.
The map matching may decide that there are no nearby Ways, or it's not confident which one the user is on, and in this case it simply waits for the next GPS location and tries again until it is confident.

### Direction
There are several directions that we track in the software:

1. The direction in which the phone is pointing. We use this when the phone is unlocked and the app is in use, but also when the phone is locked so long as it's held flat with the screen pointing at the sky. It's useful to bear this in mind when putting your phone in your bag. If it's put flat in the bottom of a bag that's held upright, the random direction your bag is pointing would be used by the app.
1. The direction in which the phone is travelling.
1. The direction from headphones with head tracking. We don't currently use this, though the iOS app did support it. We do have the technology in place to add it in the future.

When the phone is locked and in a bag, then the app will use the direction of travel. However, if the user isn't moving then no direction is available. When this happens the audio beacons go quieter to indicate that it's not possible to know the current direction of the beacon - the user could be turning round without changing location.

For some uses of direction in the app, the direction is 'snapped' to the direction of the map matched Way, so if the user is walking roughly in the direction of the Way then the actual direction of the Way is assumed to be correct and is used in those calculations.

### Conclusion
Although on the face of it the Audio beacons are straightforward, the use of map matching to try and remove location and direction errors introduces a fair amount of complexity.

## Map data
The map data used by the app almost all originates from the OpenStreetMap project. We run a server which contains a map of the whole world at multiple zoom levels. Each zoom level is split up into tiles. Zoom level 0 contains 1 tile, level 1 contains 4 tiles, level 2 contains 16 tiles and so on up to level 14 which contains around 268 million tiles to cover the planet. Each tile contains multiple layers and each layer has points, lines and polygons which can be drawn to make a graphical map. That graphical map is what is shown to the user in the app GUI. Each point, line and polygon has metadata which describes what it is. This is mostly straight from the OpenStreetMap data so a line might be a `footway` that's a `sidewalk` or a `road` that's a `minor`

The data is turned into the graphical map via a 'style' which has rules on how to draw the different points, lines and polygons in each layer e.g. how to draw a path, how to draw a forest, how to draw a bus stop. The rules can vary by zoom level, which is why as you zoom in, more and more points and lines become visible which aren't visible when zoomed out e.g. bus stops and paths.

By altering the style we can change how the GUI map looks, which is where the 'accessible map' which we're trying out comes from. It aims to have greater contrast and bolder lines and text. The style is built in to the app, so we don't have to change the map on the server to change how it looks.

But how do we use the map data for audio?

### Using the map data for audio
We currently use a relatively small amount of the mapping data to generate the audio user interface. Almost all of the audio UI uses just the tiles at the maximum zoom level. The app stitches together a 2 by 2 grid of tiles around where the user is and then it looks at just a few of the layers:

* `transportation` - for all the types of Ways including roads, paths, railways and tram ways.
* `poi` - points of interest e.g. shops, sports centres, benches, post boxes, bus stops etc.
* `building` - this is for `poi` which are mapped as more than just a point e.g large supermarkets or town halls.

It joins up lines and polygons across the tile boundaries and turns all of the Ways into connected Way segments and intersections. This is important because it allows us to search along a Way to find out where we can get to.

All of the parsed data is put also put into an easy to search format so that the app can easily find which features of the map are nearby. At this point the data is classified into categories. The current categories are:

* Roads
* Roads and paths (all Ways)
* Intersections - the points at which Ways intersect
* Entrances - these are points on a building that have been marked as an entrance.
* Crossings - road crossings
* POIs - all points of interest
* Transit stops - bus stops, railways stations, tram stops and so on.
* Subcategories of POIS:
  * Information
  * Object
  * Place
  * Landmark
  * Mobility
  * Safety
* Settlements and subcategories (see next section)
  * Cities
  * Towns
  * Villages
  * Hamlets

 With this in place, for any location the app can then easily find

 * "All transit stops within 50m" or
 * "The nearest intersection ahead of me" or
 * "The nearest hamlet, village, town or city"
  
With this in place, creating the audio callouts is just a question of querying the data based on the current location and direction. As the user moves across a tile grid, it updates it so that it centres around the current location.

### More data
One of the problems with our very local map data grid is that it means we can only 'see' at most about 1km in any direction. That's okay for when we're describing what's in front of us, but sometimes we'd like to give more context. The main example of this is when the app is used and the user isn't walking.

When the app detects that the user is travelling at more than 5 meters per second, it switches how it describes the world. Instead of calling out every intersection and POI it calls out less often and only nearby roads. The problem with this is that knowing a road name isn't very useful if you don't know what town it's in.

To try and address this, we also now parse the map data at a lower zoom level and extract data from the `place` layer. This contains the names of town, cities, neighbourhoods, villages and so on. One issue with mapping things is that there's not always an obvious boundary between these places. OpenStreetMap sometimes has city boundaries in it's database, but even when that's the case by the time it makes it to our tiled map that information is often lost. What we do have is the location that the place names are drawn on the map. These are categorised and then the app will find the nearest hamlet, village, town or city to the user and report that.

For many cities the actual city name will never be called out, because most cities are divided into smaller divisions like neighbourhoods, but those do give extra context are very useful. Just remember that because the app reports that you are nearby a street in a particular neighbourhood that just means that the label for that neighbourhood is the nearest point and it might be incorrect or even across a river.

### More context
The more context that can be added in descriptions the better, so long as it's kept concise and predictable. One of the issues we saw with describing intersections is that often there were 'un-named' Ways involved. These are Ways which have no name. In the mapping data these might be just a track, a path or a service road, but without more context it's not very useful in the text descriptions. Luckily, we can do better, so what the app does is that whenever it is about to announce an un-named Way it sees if it can figure out some more context for it.

* **Is it a sidewalk?**
A lot of areas of OpenStreetMap now have sidewalks mapped separately to roads. These are usually tagged as `sidewalk` but they don't normally say what the road is that they are the sidewalk to.

    When the app comes across an un-named sidewalk, it searches for a road that it things is running next to it and uses that to name the sidewalk. This turns out to be very important for our callouts. Instead of announcing every sidewalk intersection, as we move along a sidewalk the callouts are made as if we were moving along the associated road. Instead of *"Travelling West along path"* we have *"Travelling West along Moor Road"*. The user is on the mapped sidewalk, but the description makes more sense.

* **Does it end at a named Way?**
Very often there are pedestrian paths that join two roads together. By looking at both ends of the path we can easily add that context so that in one direction it might be *"Path to Moor Road"* and approaching from the other end it might be *"Path to Roselea Drive"*. This is only done where the path doesn't split, if it splits in to two un-named paths then we don't try and add this context.

* **Does it end near a Marker?**
If an un-named Way starts or ends near a Marker, that is used to describe it e.g. *"Path to large tree junction"*. The user can add Markers wherever they want, and by adding Markers along path networks they can add context to a whole route.

* **Does it enter or exit a POI?**
If an un-named Way starts outside a POI and ends inside it (or vice versa) then we can add that context e.g. *"Track to Lennox Park"*. 

* **Does it end near an Entrance?**
If an un-named Way starts or ends nearer an Entrance then we can add that context e.g. *"Service road to Best Buy"*.

* **Does it end near an Landmark or Place?**
If an un-named Way starts or ends nearer a Landmark then we can also add that context e.g. *"Service road to St. Giles Cathedral"*.

* **Is it a dead-end**
The app marks as a dead end any un-named Ways which don't lead anywhere.

* **Does it Way pass any steps?**
If the un-named Way passes over a bridge, through a tunnel or up/down steps then this is noted and added to the context. This is separate to the destination tagging so context such as *"Path over bridge to Lennox Park"* is possible.

These contexts are added in order and so it's possible to have *"Path to Park Lane via steps"* in one direction and *"Path to Lennox Park via steps* in the other direction. The named street gets priority leaving the park, but the park is used when entering it.

#### Future context
There are various additional contexts we hope to add in the future including:
* Context for Ways following linear water features e.g. *"Path next to River Dee"*
* Context for Ways following the edge of bodies of water *"Path next to Milngavie reservoir "*
* Context for Ways following railways e.g. *"Path next to railway"*. This could even include the name of the railway line
* Add extra content to bridges and tunnels, what are they over or under e.g. *"Path via bridge over railway to Moor Road"*

## Audio callouts
Now that we have the map data in a format that we can easily use, generating callouts really is fairly straightforward.

### Callouts when walking
When walking the audio callouts that can happen are (in order of priority):

1. Describe how far away the current destination is
1. Describe an upcoming intersection
1. Describe the 5 nearest points of interest

All of the callouts are rate limited so that they don't repeat too often. If the user stops moving then the callouts will stop, and even when moving a callout won't repeat on every new GPS location. The frequency as per iOS app is:

* Every 60 seconds for the current destination
* Every 30 seconds for an upcoming intersection
* Every 60 seconds for a point of interest

Callouts can be filtered via the settings menu, and there's certainly scope to widen this behaviour.

### Callouts when travelling faster
When travelling at more than 5 metres per second, the callout to the current destination still takes place, but the intersection and points of interest callouts are replaced by a callout describing roughly where the user is. This gives a nearby transit stop, a point of interest that contains us e.g. inside a large park, or a nearby road and settlement. These use the data described earlier, and there's obvious room for allowing customisation of this in future.

## Markers and Routes
For the most part markers and routes are just a user interface feature that relies neither on GPS or on really on map data. Markers are named locations that the user wants to store, and routes are an ordered list of those markers. The user interface to create both is taken directly from the iOS version.

### Route playback
Route playback is where routes come to life. When a route is played back an audio beacon is created at the first marker in the route. Once the user gets close to that marker, the route automatically moves the audio beacon to the next marker in the route. If there are no more markers, then the route playback finishes.

## Conclusion
Hopefully that's given some insight into how the app functions. The app is always developing based on feedback from users so please get in touch if there's anything that you think could be added.