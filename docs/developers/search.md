---
title: Search
layout: page
parent: Information for developers
has_toc: false
---
# Search
There have been a number of changes in how Search works in release v0.2.0. Offline search is a new feature where we provide users with the ability to perform a search using only the contents of offline map extracts. We also now use the Android system geocoder as well as results from the Photon server to try and do a better job wrt street numbers.

## Searching in offline maps
When we have offline maps downloaded then we have fairly fast access to many more nearby tiles than just our 2x2 grid. The tiles are all in MapBox vector tile format and each layer in the tile has a list of keys (Strings) and lists of values of different types e.g. String, Int, Bool etc. Checking whether a tile includes the search String or not is a relatively cheap operation. 

There are 3 layers that might be of interest in each tile:
* transportation - road names
* poi - points of interest e.g. shops
* building - named buildings

The search spirals out from the centre tile and each tile is read from the `.pmtiles` file and decoded from its protomaps format. A fuzzy search compares the query string with the strings in each of the layers. If a tile has a match for a search String, then it is decoded further to find all Features that use that String.

The fuzzy search started off just using the Damerau-Levenshtein distance to decide whether the strings matched. However, there were a couple of changes from that:

1. If a word in the string starts with a number then it's assumed to be a housenumber. That's excluded from the initial string match and kept for later refinement. The result is that "2010 Main Street" will match with "Main Street".
2. An attempt is made to match the end of the query string with known settlements in the area from our Settlement Grid. These are the names of hamlets, villages, neighbourhoods, towns and cities. Stripping these off a search is important because the online search does require them and if we want to search both offline and online with the same query then we have no option.
3. If the query string is shorter than the string it's compared to the start and the end of the strings in the tiles. This is because we want to match a search for "Tesco" with "Tesco Express" and also "Post Office" with "Tarland Post Office".

The outcome of this stage of search is a number of Features with a string match. Those each have a longitude and latitude and can be deduplicated.

The next step is to try and do better if a housenumber was in the original query string. This stage currently only happens for results that are within the 2x2 grid, but we can extend this in the future by creating a temporary grid to use. For now, we generate a StreetDescription for the street that we found and use that to get a more accurate location for the housenumber in the query. The StreetDescription basically makes a line of the street and places all known housenumbers along it. Some of these are known from the data to be on that street, but others lack that data but are close enough that they likely are houses on the same street. The StreetDescription can be used for both forwards and reverse geocoding - if we know how far along the street we are, we can also turn that into a street number, possibly real or otherwise interpolated.

## Searching online
Online searching runs simultaneous searches with the Android system geocoder and our photon server. The Android geocoder currently seems to have much better street number data than OSM. From an OSM perspective this varies from street to street simply because it depends on whether or not someone has been out and done the mapping. However, the Android geocoder ONLY returns street numbers, and never returns business names. If a user searches on "2010 Main Street Anytown" then the Android geocoder might return an accurate street address that can just be used directly. But if a user searches on "Tesco Anytown" then it will still return the street address "2010 Main Street Anytown". Photon might also return the Tesco Extra at that location, but without the street address. Merging the two together might give us "Tesco Extra, 2010 Main Street, Anytown" which is by far the best result.

Another complication is that Photon searches are done with a bias towards our current location. We limit Android geocoder searches to about a country size around our current location. However, for local searching this works okay. International cities can be searched on (with Photon providing results), and country wide street addresses can be searched on with Android providing street addresses and Photon perhaps the business address.

## Displaying the results
We use a library to format the results of our searches based on the current locale. We also now give the type of the result. This can be city/village/neighbourhood, or a POI type e.g. restaurant, bakery. This is useful information to help users figure out which is the result they are interested in.


## Future work
A new UI which displays the StreetDescription as a scrollable linear map seems like it might be a good approach both for Street Preview and perhaps for searching streets.
