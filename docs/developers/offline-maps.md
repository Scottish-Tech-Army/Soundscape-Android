---
title: Offline maps
layout: page
parent: Information for developers
has_toc: false
---
# Offline maps
A new feature for Android Soundscape is support for using offline maps. These maps are extracts of the planet wide .pmtiles map that can be downloaded to users' phones so that the app can run without accessing the Internet. This document aims to describe the processes for creating and using those extracts.

## Generating extracts
The `pmtiles` tool <https://github.com/protomaps/PMTiles> is very efficient at creating extracts either from a bounding box, or from a GeoJSON region. We've now got a couple of Python scripts to automate the process.

### step1-generate-geojson-for-extracts.py
This script generates a GeoJSON file with a `Feature` for each extract that we want to generate. At it's simplest it generates a `Feature` for every country in the world using data from GeoNames <https://download.geonames.org/export/dump/>. However, it can also be configured to output a `Feature` for each of the states or provinces for specific countries, and even to generate a coalesced `Feature` for every city in the world. The country and state `Feature` (known in geonames as admin1) are expanded so that there is a large overlap between adjacent `Feature` and also simplified so that the polygons contain fewer corners. That's important as we want to keep the size of the GeoJSON down because it forms the basis of what ends up being parsed when choosing extracts from within the app.
The countries that are split up are currently:
* USA
* Canada
* China
* Japan
* India
* Russia
* Australia
* Germany
* Poland

They were chosen because the country files were the largest and were easy to split up. There's some more finessing required here - possibly to group some of the Japan prefectures into larger zones, to split the UK into countries and to decide what to do about France which has very small departments.

The GeoJSON file is currently around 1MB uncompressed which is very manageable and can easily be viewed on <https://geojson.io> to check that it looks as expected.

Many countries end up as `MultiPolygon` - the UK is currently two `Polygon`, one for most of the UK and the other for Rockall, a tiny island in the Atlantic. This requires some care when dealing with it and we need to improve the `FeatureTree` support so that we can efficiently search to find which extracts apply to a given location.

### step2-generate-extracts-from-geojson.py
Once we're happy with GeoJSON we pass it in to this script which creates the extracts themselves. Each extract is a standalone `.pmtiles` file that can be downloaded and used directly by the app. It also outputs a GeoJSON file which is the input GeoJSON file with the addition of a property for each `Feature` which contains the size of the extract. This is so the app can allow for informed selection of different extracts. With only country extracts being generated, most users will only have a single extract that includes their location, but if we add smaller city extracts then the number to choose from will increase.

## Serving up the extracts
The extracts are all going to be made available from R2 on Cloudflare. This is by far the most cost effective way to serve up large files. The cost is for storage only (~1.5 cents per GB) with no charge for the bandwidth used downloading the files. There is a very small charge for each download, but there is a large number of free downloads (millions!) and so we should never hit that limit. 

The manifest will be hosted there too and the cost should be less than around $5 per month - all the cost of the storage of all the extracts.

## App behaviour
During onboarding, the app will offer the option of downloading offline maps. Extracts that cover the user's current location will be suggested and the user can either download them or choose to run in online mode. There will also be a list based download user interface which will categorize the extracts by continent and country. These can be shown on top of our map, but the list approach is generally much more accessible. We need to consider how we are going to deal with translations for this, though initially country and state names may just be in English.

It will be possible to download the extracts to either internal or external (microSD card) storage.

The `maplibre` code can work directly from a `.pmtiles` file, but it can only handle a single file. We'll either have to write some code to make multiple files appear as a single file, or have an initial limit of one extract per user.

Our own audio code which parses `.pmtiles` can use multiple sources, though there may be a small performance hit.

## Map updates
Whenever we rebuild the planet wide map we should regenerate all of the extracts. Users don't have to download new extracts, though in future we may add more data to the maps for specific features and in that case new offline maps would be required for those features to work.


## Minimum viable release
I think we could ship this to Beta testers once we have:
* Extracts for each country or states/provinces available with associated metadata
* Support for a single downloaded extract within the app

This covers the use cases for a lot of users where they just need a single region. The extracts may be too large for some users, and that's where we need to look at how to improve our extracts via groups of cities etc.

We do need to take care that we are happy with the metadata format prior to shipping initial support. We want to try and avoid dealing with multiple versions of the metadata. Once we ship parsing a type of metadata in the app then we'll have to ensure that format is supported 'forever' on the server. However, hopefully any changes will just be the addition of new properties to the GeoJSON `Feature` and so there'll be some amount of backwards compatibility.