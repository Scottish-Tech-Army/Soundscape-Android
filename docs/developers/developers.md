---
title: Information for developers
layout: page
nav_order: 10
has_toc: true
---

# Developer information
The app is developed using [Android Studio](https://developer.android.com/studio) with the majority of the app code written in Kotlin.

## Get started
The quickest way to get started building the app is to run Android Studio and select `File/New/Project from version control` from the menu. Select `Git` as the version control and paste in the HTTPS code link for this project from above. Click on Clone and the project will download, open and initialize ready to build.

The URL and API Key for the tile provider server (mapping tiles) are GitHub secrets, and developers have to define them locally. An example `local.properties` from the root directory should look something like this:

```
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
sdk.dir=/home/userName/Android/Sdk

# Tile provider URL and API key
tileProviderUrl= https://protomaps_server_base_url
tileProviderApiKey=XXXXXXXXXXXXXXXXXXXX
```

If you would like access to the main Soundscape tile provider for development, get in touch.

## Code documentation

* [App architecture]({% link developers/architecture.md %}) is an introduction to the app structure and its UI.
* [GeoEngine]({% link developers/geoengine.md %}) describes the code which parses mapping data and provides an API to the parts of the app that need it.
* [Audio API]({% link developers/audio-API.md %}) describes the interface with the audio engine for beacon and text to speech play out.
* [Dokka generated docs]({% link dokka/index.html %}) contains docs auto-generated from comments in the code.

## Mapping data
See [here]({% link developers/mapping.md %}) for information about how we configure and use mapping data. There's also [this document](debugging-geojson.md) which describes a possible workflow for GeoJSON debugging.

## Continual Integration on GitHub
See the document [here]({% link developers/actions.md %}) to learn about the GitHub actions used and how they are configured.
