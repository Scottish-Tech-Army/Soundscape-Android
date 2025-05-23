# Developer information
The app is developed using [Android Studio](https://developer.android.com/studio) with the majority of the app code written in Kotlin. 

## Progress
We now have an app which has many of the features available which are available in the iOS app, though it is still relatively early days in the project. 

## Get started
The quickest way to get started building the app is to run Android Studio and select `File/New/Project from version control` from the menu. Select `Git` as the version control and paste in the HTTPS code link for this project from above. Click on Clone and the project will download, open and initialize ready to build.

## Code documentation
* [App architecture](architecture.md) is an introduction to the app structure and its UI.
* [GeoEngine](geoengine.md) describes the code which parses mapping data and provides an API to the parts of the app that need it.
* [Audio API](audio-API.md) describes the interface with the audio engine for beacon and text to speech play out.
* [Dokka generated docs](dokka/index.html) contains docs auto-generated from comments in the code.

## Mapping data
See [here](mapping.md) for information about how we configure and use mapping data. There's also [this document](debugging-geojson.md) which describes a possible workflow for GeoJSON debugging.

## Release notes
[Here](release-notes.md) are the latest release notes.

## Testing resources
[A document](tricky-locations.md) containing locations which cause particular issues in Soundscape. 

## Continual Integration on GitHub
See the document [here](actions.md) to learn about the GitHub actions used and how they are configured.
