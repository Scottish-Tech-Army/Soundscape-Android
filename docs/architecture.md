---
tags: [Mermaid]
mermaid: true
---

# Soundscape Android architecture

This document describes the basic architecture of the app and the various UI screens. The main purpose of the app is to provide audio cues to aid navigation and improve awareness of surroundings for the visually impaired. The screen UI is important for setting up the location of audio markers and creating routes, but the audio UI is at least as important. Because the audio has to continue running when the phone is locked, the audio UI is all driven from a foreground service which carries on running when other apps are in use and when the phone is locked.

## Soundscape foreground service
The foreground service needs to know the current location and the direction that the phone is pointing. It uses those in conjunction with GeoJSON tile data read from the `soundscape-backend` server to generate strings to describe the current location (known as callouts). Audio beacons and speech are played out via the `AudioEngine` described [here](audio-API.md).

The location and direction are provided by the `LocationProvider` and `DirectionProvider` classes. During normal operation these use the Android `FusedLocationProvider`, and `FusedOrientationProvider` APIs. However, in Street preview mode the `LocationProvider` can use a fixed location, effectively teleporting the user so that they can hear the callouts for somewhere they are planning on traveling to.

A `KalmanFilter` class is used to filter the locations from `FusedLocationProvider` to reduce jumps in location. Kalman filters perform a weighted average on the current location and the location from the OS to give a new location. The weighting is based on the accuracy value provided by the OS so that low accuracy locations move the position more slowly than high accuracy ones.

The GeoJSON parsing is a whole other area and will be described in a separate document.

## Onboarding Activity
The onboarding screens have been given their own activity. Onboarding screens guide the users through some initial choices of language,  permissions and audio beacon settings. Onboarding screens are only shown the first time through the app, or if the user selects *App Setup* from within the *Help & Tutorials* section of the menu.

## Main Activity
This is where the app normally spends its time. The `Home` screen looks like this:

<img src="HomeScreen.png" height="400"/>

The map is zoomed around the current location and rotated based on the direction that the phone is pointing in. Here's what can be accessed from the iOS Home screen:

<div class="mermaid">
flowchart LR
    Home(**Home**<br>Main screen with map of current location and various large buttons) --> Menu(Menu<p>Opens drawer menu on Home screen)
    Menu --> Settings(**Settings**<br>The various configurable options for the app)
    Menu --> HelpAndTutorials(**Help & Tutorials**<br>A large menu of help and tutorials covering use of the app)
    Menu --> SendFeedback
    Menu --> Rate(**Rate**<br>Rate the app in the app store)
    Menu --> Share
    Home --> Search(**Search**<br>A text box allowing searching of Markers database and Internet search)
    Home --> SleepUntilWoke(**Sleep until woke**<br>Sleeps the audio and location services until the wake button is pressed)
    SleepUntilWoke --> SleepUntilLeave(**Sleep until leave**<br> Sleeps the audio until the phone leaves it's location)
    Search --> SearchResults(**Search Results**<br>A list of results from the search.)
    SearchResults --> SearchDetails(**Search Details**<br>LocationDetails describing each search result.)
    Home --> PlacesNearby(**Places Nearby**<br>A search results page listing places nearby)
    Home --> MarkersAndRoutesScreen(**Markers and Routes**<br>A screen with two tabs, one for Markers and the other for Routes)
    Home --> CurrentLocation(**Current Location**<br>Shows LocationDetails of current location)
    Home --> MyLocation(**My Location**<br>Triggers audio callout of current location)
    Home --> AroundMe(**Around Me**<br>Triggers audio callout of nearby POI and intersections)
    Home --> AheadOfMe(**Ahead of Me**<br>Triggers audio callout of what's ahead)
    Home --> NearMe(**Near Me**<br>Triggers audio callout of what's Nearby)
    MarkersAndRoutesScreen --> MarkersScreen(**Markers**<br>Markers are named locations stored in a local database along with a description)
    MarkersAndRoutesScreen --> RoutesScreen(**Routes**<br>Routes are an ordered list of Markers that can be played back as audio beacons)
    MarkersScreen --> LocationDetails(**Location Details**)
    RoutesScreen --> NewRoute(**New Route**<br>Create a new route)
    RoutesScreen --> RouteDetails(**Route Details**)
    RouteDetails --> StartRoute(**Start Route**<br>Start playback of the route. This shows a map of the route with Markers shown by numbers and also a list of them with details below the map)
    RouteDetails --> EditRoute(**Edit Route**<br>Allows editing of the Markers in the route)
    RouteDetails --> RouteShare(**Route Share**<br>Shares a GPX file with other apps)
    NewRoute --> AddWaypoints(**Add Waypoints**<br>Allows the adding or Markers to a Route)
    LocationDetails --> StartAudioBeacon(**Start Audio Beacon**<br>Sets the current audio beacon at this location)
    LocationDetails --> EditMarker(**Edit Marker**)
    LocationDetails --> StreetPreview(**Street Preview**<br>Plays audio callouts and beacons as if the phone were teleported the location)
    LocationDetails --> ShareMarker(**Marker Share**<br> Shares Marker details to another application)
    HelpAndTutorials --> RerunOnboarding(Rerun onboarding screens)
</div>
