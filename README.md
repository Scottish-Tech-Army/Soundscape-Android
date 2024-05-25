# Android Development

The original Soundscape is an iOS app that provides 3D audio navigation for the blind or visually impaired. It has the added benefit that the map data is stored on a user device so it is resilient in the event of a network outage. Can we do the same on Android?

Below is a rough brain dump of areas to look at. Going to use Kotlin for the language and Jetpack Compose for the UI.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled.png)

Above is a picture of a tile boundary with an artificial location in the center of the tile.

We need to convert the GPS coordinates into the X/Y format and make our request to the backend Tile service. 


**Deserializing our Tile data**

We've got our tile which is a string in the **GeoJSON** ([https://geojson.org/](https://geojson.org/)) format.
At a high level our string consists of:

A **FeatureCollection** which contains

A set of **Features** and

A **Feature** can be composed of:

**Point**, **LineString**, **Polygon**, **MultiPoint**, **MultiLineString**, **MultiPolygon** and **GeometryCollection**

In addition there are **Foreign Members** which original Soundscape makes use of

To do anything useful we need to deserialize the GeoJSON into Kotlin objects. I've used this GeoJSON parser from here:
([https://github.com/scruffyfox/Android-GeoGson](https://github.com/scruffyfox/Android-GeoGson))

Here’s a picture of a GeoJSON tile FeatureCollection overlayed on a map (there is a lot more information in the underlying GeoJSON data such as speed limits on roads, addresses for houses, etc. that the map won’t display) The markers are intersections and some crossings:

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%201.png)

Here’s another picture of the same tile with the GeoJSON parsed so only the **Roads FeatureCollection** for that tile is displayed (we need to be able to do this for navigation) The markers are crossings:

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%202.png)

Here’s another picture with the GeoJSON parsed so only the **Points Of Interest** Feature Collection is displayed. Original Soundscape breaks this down further into super categories:

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%203.png)

Original Soundscape doesn’t use one tile it has a 3 x 3 grid with the active tile in the middle. Here’s a picture of the tiles and a search polygon/circle: 

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%204.png)

Once we've got our tile we need some way of storing the tile(s).
Original Soundscape appears to use Swift Realm to do this and there is an Android version:
[https://www.mongodb.com/docs/realm/introduction/](https://www.mongodb.com/docs/realm/introduction/)


**Doing something useful with the Tile data**

If we've got our Tile data in Kotlin objects then we can use standard GIS techniques to query the contents of the GeoJSON FeatureCollection.
For example we might want to put bounding boxes around all the Points Of Interest so we can calculate how far way they are. Here’s a picture of bounding boxes around all the buildings (which looks a bit like a Mondrian painting if you squint):

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%205.png)


**Intersections**
 
Here’s a picture of the simplest intersection where one road transitions into another road (Weston Road into Long Ashton Road) The triangle is the “field of view” where a device is on Weston Road and pointing towards Long Ashton Road so we can detect when the **Intersection** is in the “field of view”/triangle. The middle marker is the **Intersection**/transition between the two roads. As the triangle represents a FOV at a point in time an example callout might be “You are walking along Weston Road”

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%206.png)

The image below shows the FOV for the user as they’ve walked further along in time. You can see that the FOV now contains two Intersection markers and we have to calculate the distance to them and return the nearest intersection to the user. The Intersection data tells us the road “osm_ids” that make up the intersection but unfortunately nothing else.

At this point the triangle 90 degree field of view is useless as it can only represent 4 quadrants (ahead, left, right, behind) and we need a more granular approach which is what original Soundscape does
We now have to figure out if the transition is “ahead”, “ahead to the left”, “ahead to the right”, “left”, “right”, “behind left”, “behind”, “behind right” These are all relative directions not compass directions.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%207.png)

As mentioned above Soundscape uses relative directions and our simple FOV triangle morphs into three (actually eight but I’m only displaying three in this image) FOV triangles which are clockwise on the image below “ahead left”, “ahead” and “ahead right” assuming the user still has their device pointing East.
So our callout could be: 

“Walking on Weston Road. 

Intersection with Long Ashton Road 6 meters ahead. 

Long Ashton Road ahead to the left”:

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%208.png)

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%209.png)

Above is an image of one of the types of Field of View (”combined”) that Soundscape uses. The red triangle is the relative direction “ahead” but the real device heading is East or 90.0 degrees. 

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2010.png)

Above is an image of one of the types of Field of View (”Individual”) that Soundscape uses. The red triangle is the relative direction “ahead” but the real device heading is East or 90.0 degrees. You can see that it is a simpler “ahead”, “right”, “behind” and “left” with 90 degree triangles.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2011.png)

Above is an image generated of one of the types of Field of View (”ahead_behind”) that Soundscape uses. The red triangle is the relative direction “ahead” but the real device heading is East or 90.0 degrees. You can see that it is a simpler “ahead”, “right”, “behind” and “left” however it has a bias towards “ahead” and “behind”

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2012.png)

Above is an image of one of the types of Field of View (”right_left”) that Soundscape uses. The red triangle is the relative direction “ahead” but the real device heading is East or 90.0 degrees. You can see that it is a simpler “ahead”, “right”, “behind” and “left” however it has a bias towards "left” and “right”

**Types of Simple Intersections**

Below are a list of simple intersection images that we want to be able to detect (we can currently detect all of them)

Simple **right turn** if you are standing where the marker is from one road (Belgrave Place) into another road (Codrington Place)

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2013.png)

Simple **left turn** if you are standing where the marker is from one road (Codrington Place) into another road (Belgrave Place)

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2014.png)

Simple **side road right** if you are standing at the marker on Long Ashton Road and St Martins is on your right.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2015.png)

Simple **side road lef**t if you are standing at the marker on Long Ashton Road and St Martins is on your left.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2016.png)

Simple **T junction type 1** if you are standing at the marker on St Martins and Left and Right is Long Ashton Road

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2017.png)

Simple **T junction type 2** if you are standing at the marker on Goodeve Road and Left is Seawalls Road and Right is Knoll Hill

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2018.png)

Simple **crossroads type 1** if you are standing at the marker on Grange Road then left is Manilla Road, Ahead is Grange Road and Right is Manilla Road.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2019.png)

Simple **crossroads type 2** if you are standing at the marker on Lansdown Road then left is Manilla Road, Ahead is Lansdown Road and Right is Vyvyan Road.

![Untitled](Android%20Development%20e8357d834ca14e1bb6ecd0a70a501c2d/Untitled%2020.png)

Simple crossroads type 3 … TODO


**3D Audio and headphones**
Core feature of Soundscape is the 3D audio part.

**Routing**
Open source project here:
[https://github.com/graphhopper/graphhopper](https://github.com/graphhopper/graphhopper)
Google offer it but it costs:
[https://mapsplatform.google.com/solutions/offer-efficient-routes/](https://mapsplatform.google.com/solutions/offer-efficient-routes/)

**How do we want to display a map on the screen?**
Soundscape iOS uses Apple Maps but you can't hook into it from Android.
Make our own? Here’s a nice Docker image which will create an Open Street Map tile server for us - web/database/map tile renderer:
[https://github.com/Overv/openstreetmap-tile-server](https://github.com/Overv/openstreetmap-tile-server)
