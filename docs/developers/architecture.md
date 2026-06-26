---
title: App architecture
layout: page
parent: Information for developers
has_toc: false
---

# Soundscape Android architecture

This document describes the basic architecture of the app and the various UI screens. The main purpose of the app is to provide audio cues to aid navigation and improve awareness of surroundings for the visually impaired. The screen UI is important for setting up the location of audio markers and creating routes, but the audio UI is at least as important. Because the audio has to continue running when the phone is locked, the audio UI is all driven from a foreground service which carries on running when other apps are in use and when the phone is locked.

## Soundscape foreground service
The foreground service (`SoundscapeService`) needs to know the current location and the direction that the phone is pointing. It uses those in conjunction with Mapbox Vector Tile data — fetched from our protomaps tile server when online, or read from a downloaded `.pmtiles` extract when offline — to generate strings that describe the current location (known as callouts). Tile parsing and the searchable `FeatureTree`s are described in the [GeoEngine document]({% link developers/geoengine.md %}). Audio beacons and speech are played out via the `AudioEngine` described [here]({% link developers/audio-API.md %}).

The location and direction are provided by the `LocationProvider` and `DirectionProvider` classes. During normal operation these use the Android `FusedLocationProvider`, and `FusedOrientationProvider` APIs. However, in Street preview mode the `LocationProvider` can use a fixed location, effectively teleporting the user so that they can hear the callouts for somewhere they are planning on traveling to.

A `KalmanFilter` class is used to filter the locations from `FusedLocationProvider` to reduce jumps in location. Kalman filters perform a weighted average on the current location and the location from the OS to give a new location. The weighting is based on the accuracy value provided by the OS so that low accuracy locations move the position more slowly than high accuracy ones.

## Offline maps
The app can also be driven entirely from downloaded map extracts so that users can run with no network. Extracts are `.pmtiles` files covering a country, state or other region, generated from the planet-wide map and served from Cloudflare R2. See [Offline maps]({% link developers/offline-maps.md %}) for the extract pipeline and [Cloudflare setup]({% link developers/cloudflare.md %}) for the hosting side. Within the app, extracts are downloaded and managed from the Offline Maps screen (reached from the menu, or during onboarding); the same screen offers a map-based picker that lets the user choose extracts by tapping the region they cover.

## Audio control while the screen is locked
Because Soundscape is primarily an audio app it is essential that the user can drive it without unlocking the phone. Three control surfaces are supported in addition to the touch UI:

1. Media-key shortcuts (play/pause/next/previous on a headset or watch), modelled on the iOS app's bindings.
2. Voice commands — the user tap-and-holds a media key to enter a listening state, speaks a command, and the recognised text is dispatched to the same handlers as the touch UI.
3. An audio menu — repeated media-key presses walk a tree of spoken menu options that mirrors the GUI.

See [Voice and audio menu control]({% link developers/voice-and-audio-control.md %}) for the command catalogue and menu layout. The implementation lives in `services/mediacontrol/` (`MediaControlTarget`, `VoiceCommandManager`, `AudioMenu`, `SoundscapeMediaSessionCallback`).

## Onboarding Activity
The onboarding screens have been given their own activity. Onboarding screens guide the users through some initial choices of language,  permissions and audio beacon settings. Onboarding screens are only shown the first time through the app, or if the user selects *App Setup* from within the *Help & Tutorials* section of the menu.

## Main Activity
This is where the app normally spends its time. The `Home` screen looks like this:

<img src="HomeScreen.png" height="400"/>

The map is zoomed around the current location and rotated based on the direction that the phone is pointing in. Here's what can be accessed from the Home screen:

```mermaid
flowchart LR
    Home(<b>Home</b>><br>Main screen with map of current location and various large buttons) --> Menu(<b>Menu</b><br>Opens drawer menu on Home screen)
    Menu --> Settings(<b>Settings</b><br>The various configurable options for the app)
    Menu --> HelpAndTutorials(<b>Help & Tutorials</b><br>A large menu of help and tutorials covering use of the app)
    Menu --> OfflineMaps(<b>Offline Maps</b><br>Download and manage offline map extracts)
    OfflineMaps --> OfflineMapExtractDetails(<b>Map Extract Details</b><br>Map-based picker showing which extract covers a tapped location)
    Menu --> SendFeedback
    Menu --> Rate(<b>Rate</b><br>Rate the app in the app store)
    Menu --> Share
    Home --> Search(<b>Search</b><br>A text box allowing searching of Markers database and Internet search)
    Home --> SleepUntilWoke(<b>Sleep until woke</b><br>Sleeps the audio and location services until the wake button is pressed)
    SleepUntilWoke --> SleepUntilLeave(<b>Sleep until leave</b><br> Sleeps the audio until the phone leaves it's location)
    Search --> SearchResults(<b>Search Results</b><br>A list of results from the search.)
    SearchResults --> SearchDetails(<b>Search Details</b><br>LocationDetails describing each search result.)
    Home --> PlacesNearby(<b>Places Nearby</b><br>A search results page listing places nearby)
    Home --> MarkersAndRoutesScreen(<b>Markers and Routes</b><br>A screen with two tabs, one for Markers and the other for Routes)
    Home --> CurrentLocation(<b>Current Location</b><br>Shows LocationDetails of current location)
    Home --> MyLocation(<b>My Location</b><br>Triggers audio callout of current location)
    Home --> AroundMe(<b>Around Me</b><br>Triggers audio callout of nearby POI and intersections)
    Home --> AheadOfMe(<b>Ahead of Me</b><br>Triggers audio callout of what's ahead)
    Home --> NearMe(<b>Near Me</b><br>Triggers audio callout of what's Nearby)
    MarkersAndRoutesScreen --> MarkersScreen(<b>Markers</b><br>Markers are named locations stored in a local database along with a description)
    MarkersAndRoutesScreen --> RoutesScreen(<b>Routes</b><br>Routes are an ordered list of Markers that can be played back as audio beacons)
    MarkersScreen --> LocationDetails(<b>Location Details</b>)
    RoutesScreen --> NewRoute(<b>New Route</b><br>Create a new route)
    RoutesScreen --> RouteDetails(<b>Route Details</b>)
    RouteDetails --> StartRoute(<b>Start Route</b><br>Start playback of the route. This shows a map of the route with Markers shown by numbers and also a list of them with details below the map)
    RouteDetails --> EditRoute(<b>Edit Route</b><br>Allows editing of the Markers in the route)
    RouteDetails --> RouteShare(<b>Route Share</b><br>Shares a GPX file with other apps)
    NewRoute --> AddWaypoints(<b>Add Waypoints</b><br>Allows the adding or Markers to a Route)
    LocationDetails --> StartAudioBeacon(<b>Start Audio Beacon</b><br>Sets the current audio beacon at this location)
    LocationDetails --> EditMarker(<b>Edit Marker</b>)
    LocationDetails --> StreetPreview(<b>Street Preview</b><br>Plays audio callouts and beacons as if the phone were teleported the location)
    LocationDetails --> ShareMarker(<b>Marker Share</b><br> Shares Marker details to another application)
    HelpAndTutorials --> RerunOnboarding(Rerun onboarding screens)
```
