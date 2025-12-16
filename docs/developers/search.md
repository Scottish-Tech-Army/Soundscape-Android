---
title: Offline search
layout: page
parent: Information for developers
has_toc: false
---
# Offline search
Offline search is a new feature where we provide users with the ability to perform a search using only the contents of the current tile grid or possibly any offline map extracts.

## Searching within the tile grid
At all times the app has a 2x2 grid of decoded tiles that surround the user's current location. Searching within that is relatively straightforward as the tiles are fully decoded.

## Searching further afield in offline map
If we have offline maps then we have fast access to many more nearby tiles. The tiles are all in MapBox vector tile format and each layer in the tile has a list of keys (Strings) and lists of values of different types e.g. String, Int, Bool etc. Checking whether a tile includes the search String or not is a relatively cheap operation. There are 3 layers that might be of interest in each tile:
* transportation - road names
* poi - points of interest e.g. shops
* building - named buildings

The tile has to be read from the `.pmtiles` file and decoded from its protomaps format, but it wouldn't have to be fully parsed as the lists of values in each layer can be easily obtained.
If a tile has a match for a search String, then it could be decoded further to find which Features reference the String. This could be performed as a complete tile decode, or we could write a fast decode where the aim is only to decode Features which reference the search String.

## What do we search on?
Our online search allows free text searching of full addresses and uses fuzzy string matching. This is far from a straightforward thing to implement, largely due to differences in how address formats vary across languages and how even in a single language there are many possible formats. It's also far from perfect as house numbers could be matched with values in postcodes as it is simply treating the strings as a sequence of tokens.
The gold standard approach for handling free text addresses is to use `libpostal` which uses NLP and requires a 2.2GB in memory data model - not something that we can do offline on a phone. This complexity is the reason why Search on car SatNavs doesn't generally allow free text searches. Instead there are separate fields to fill in for street number, street name, city etc. This makes the user do the complex work of breaking down the address which can then be used to search in a more structured format.

> In theory our Photon online search server supports structured searching - see https://github.com/komoot/photon/blob/master/docs/structured.md. This means that we could add a structured search UI which would work with both Photon and local search. However, our photon instance doesn't appear to support the structured endpoint so some further investigation will be required as to how to enable it.

If we can create a UI which separates out the search then we can bypass address format complexity and potentially provide a simpler UI.

## Multi-level search
One idea for a local search UI is that the user has to enter a string that is one of the following:

1. A street name (search the transportation layer for the string)
2. The name of a point of interest e.g. 'Tesco' (search the poi layer for the string)
3. A type of point of interest e.g. 'pharmacy' (search the poi layer for OSM tag - reverse mapping of translation required)

It's straightforward to search on all of these and if there are multiple results then the user can be presented with the options, the type (Street or POI) and how far away they are.

If it's a POI then the user selecting it can go straight to the Location Details screen. If it's a Street then we have to provide a user interface that allows them to choose from various locations on the street. I'm imagining some kind of linear map (see https://osm.mathmos.net/linear/edinburgh/south-bridge/ for an example) where the map appears as a thumbwheel type UI. User can scroll the road up and down with the "selected" location appearing in the middle of the screen. The locations that could be selected would include:

* Junctions with other roads
* POI including shops, landmarks etc.
* Transit stops
* Street numbers
* We could also include buildings/houses which don't have a street number or perhaps groups of buildings along with a range of interpolated street numbers.

Major issues to be resolved include:
1. Figuring out how to deal with long roads. The user doesn't want to have to do lots of scrolling.
2. Handling intermittent house numbering.
3. Handle smoothly roads which are not linear e.g. Arden Close https://www.openstreetmap.org/way/353502084#map=19/51.525254/-2.543878&layers=N

It may be that we have a second search box on this page which allows a street number to be specified directly.

This street UI might also be a better UI for Street Preview than we current have. It would allow users to explore a street in either direction and at junctions give the option of moving on to a different street.
