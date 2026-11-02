---
title: Roadmap
layout: default
nav_exclude: true
search_exclude: true
permalink: /roadmap/
---

# Soundscape roadmap
This is our current roadmap. It starts off with some fairly firm goals, but after that it's largely dependent on user feedback and what we can glean from analytics. This isn't the whole list of ideas that we've discussed, but it's ones that I think would have the most user impact.

## Current state of play
We released Android v2.0.95 to production on October 28th 2026. That is the first production release to be built using our Kotlin multi-platform source code which can also be built for iOS. Each release is now built for iOS as well as Android. The iOS release is much less tested and so is likely a few months away from full release. We have a small closed Beta on iOS, but that's largely been sighted STA members checking translations.

## iOS extended beta
We want to extend the beta testing to include users of the original iOS app and any new blind or visually impaired users. The expectation that this will result in quite a lot of bug fixing and adding in small features that were missed in the original Android app.

## iOS and Android release - early Q1 2027
We'll continue running an Android beta release with any changes that come out of iOS and Android feedback. When everyone is happy we'll release a new version that targets both iOS and Android. This is a major milestone for the project! Once successfully released on AppStore the old server can be monitored for use and then when idle turned off. 

## What's next?
### Improving callouts
Callout improvements come in many different forms:

#### Improvements for walking navigation
An example might be to improve T junction descriptions. Instead of calling out the same road going left and right, extra context could be added like Buchanan Street goes left towards Main Street/down the hill/towards the station.
Roundabout and complex junction simplification also falls into this category. These are poorly described by the current code.

#### Improvements for travel navigation
Roads are only known about within the 2x2 tile grid - so very local to the user's current position. This means that in travel mode we can't describe where a road at a junction is heading e.g. to a town 10 miles away. We should try parsing some Ways at lower zoom levels so that we can give more context in our descriptions. Tile simplification needs to be considered, but I think we can work around it if we're only following a road to see where it goes.

#### Improvements for exploration
In travel mode describing nearby mountains or lakes, or woodland that's being passed through. Less about the exact road being travelled on and more about what surrounds the road.
We should also consider improvements for descriptions when hiking e.g. path towards forest (or named forest), or path to top of mountain.
I'm also interested in whether we can automatically use relations stored in OSM data similar to https://hiking.waymarkedtrails.org/#route?id=2866087&type=relation. Automatically creating turn-by-turn directions or routes for Soundscape from waymarked routes.

### Localisation and translation improvements
Hopefully feedback on translations from native speakers will start coming in and we can integrate it. The callout behaviour in different countries is also important where differences in OpenStreetMap mapping styles, address formatting and search all will need improvements. We just need real user feedback.

### Route creation
Route creation is currently hard and we have various alternative designs that might work. This is the most obvious app feature to be developed. Various options exist including turning a journey just completed into a route.

### Hardware integrations
Meta have finally updated their glasses SDK to include head tracking sensor info, though apps that include it can't publish via Play Store yet. A user has requested the release of Bose Frames and WitMotion head tracking support on Android. We'll likely do that under Debug settings initially. There's documentation required for the setup of the WitMotion module.

## User feedback and community
Soundscape is just one app out of many that folks use, and there's not enough hours in the day for people to provide all of the feedback that we as developers might want. However, we do want the app to serve its users and address their needs. I don't think it's possible to have an active online forum just for Soundscape so perhaps continuing to perform outreach to as many groups as possible is how we approach this. We have considered soliciting feedback from within the app - perhaps just having a speech announcement on startup every so often (a few months) suggesting the user contact support would be the least invasive.