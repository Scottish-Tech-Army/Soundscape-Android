---
title: Release notes
layout: page
nav_order: 5
has_toc: false
---

# Release notes

# 0.0.94
We keep track of issues in GitHub [here](https://github.com/Scottish-Tech-Army/Soundscape-Android/milestones).
Issues tagged _user-facing_ are those which describe issues from a user perspective. 

## New in this release
* Support for better audio interaction between apps. Phone and video calls will automatically mute the app, and other apps will drop their volume when Soundscape callouts play.
* Closing the app by swiping up now stops the audio service too, shutting down the app completely.

### Longer term known issues
The largest issues which are still being worked on are:
* [Support for Street Preview is very preliminary](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/528), especially when it comes to use by people with a visual impairment.
* [Callouts for roundabouts need work](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/442). We have a plan for this, but it's not yet implemented.

## Features
This is a brief description of the various features of the app.

#### Onboarding screens
These are based on the iOS screens and guide the user through language selection, permissions, and beacon style selection.

#### Home screen with UI map
Unlike iOS, a long press on the map brings up the Location Details screen which is central to the UI. As on iOS that screen allows 
- creation of an audio beacon at the location
- saving the location as a marker
- entering Street Preview mode at the location
- sharing of the location to another app e.g. to email it, or send it on Slack

Clickable items on the home screen that are implemented are:

  * **My Location, Around Me, Ahead of Me and Nearby Markers support** Tapping the icons at the bottom of the screen will describe the location of the device via voice using a similar algorithm to Soundscape iOS. 
  * **Places Nearby** lists either all of the POI within the local area, or a filtered subset depending on the selection chosen. Clicking on an entry in the list brings up the Location Details screen.
  * **Markers and Routes** brings up the UI for marker editing and route creation/editing. It includes the Places Nearby lists so that Markers can be created as Waypoints are added.
  * **Current Location** brings up the Location Details screen for the current location.
  * **Sleep mode** Clicking on that will disable the Soundscape service and bring up the full screen sleep screen. This is similar to the very simplest iOS sleep mode, wake on leave is not yet implemented.
  * **Street Preview** This is getting closer to how the iOS app works. When Street Preview is entered via the Location Details screen the GPS location is replaced with the static location provided by the user. The Home screen will display that location is if the phone was there. If Street Preview mode is running then buttons appear at the top of the home screen showing the options available.
    * Exiting Street Preview mode and returning to using the phone's actual location. When in Street Preview mode the direction is still controlled by the phone orientation.
    * Clicking on the other button jumps to the next intersection on that road. Which road to choose at the intersection is 
      chosen by rotating the phone. This should behave pretty much as the iOS app does, but with less animated buttons/feedback.
  * **Search** This allows geo-searching from within the app. It updates the results if you pause typing in an auto-suggest type way. Tapping on a result opens the Location Details screen.

#### Soundscape service
This runs even when the phone is locked. It means that a beacon can be set or a route played and then the phone locked. The audio beacon will continue to sound. The service can be stopped and restarted by entering and exiting Sleep mode. The service is responsible for the audio beacon play out and the audio callouts. The heading logic is similar to iOS which means that when the phone is locked the heading is based on the current direction of travel. If the phone is unlocked - or is locked and held flat in front of the user - then the heading used is the direction that the phone is pointing in.

* **Initial Auto callout support** These are enabled by default and announce upcoming intersections and nearby points of interest.
* **Media Playback** controls now work allowing headphone or other Bluetooth triggering of some types of callout. They work the same as iOS.

#### Menu
Some of the options within the Menu have been implemented:
* **Settings** allows the altering of a few settings that exist in the app. This includes selecting the text to speech voice and its speed. Support for alternatives to the Google text to speech engines have been tested including CereVoices, Vocalizer TTS and Acapela TTS engines.
* **Help and Tutorials** duplicates the text from the iOS app. This will be updated in future to reflect differences between the Android and iOS apps.

#### Opening the app
The main way that a user might open the app is by tapping on its icon. However, there are other ways to do it:

*  **Open from Google Calendar** Clicking on a location in another app e.g. Google calendar, will open Soundscape in the Location Details screen for that location. This is using a `geo:` intent from the calendar.
*  **Open via Share from Google Maps** Selecting a place in Google Maps and then clicking on the share icon allows the location to be shared with the Soundscape app. This requires Internet access. Once again the result is opened in the Location Details screen.
*  **Open a soundscape URI** The behaviour of these URI may well change as we develop further. Currently, it's just a latitude and longitude and clicking on a URI like [soundscape:55.
   9552485,-3.1928911](soundscape:55.9552485,-3.1928911) will open the Soundscape app in Street Preview mode at the location provided. This is very useful for testing problem locations found by users in the field. That URI is actually from [issue 201](https://github.
   com/Scottish-Tech-Army/Soundscape-Android/issues/201) and makes it simple to  reproduce.
*  **Open a GPX/JSON file** from the File application on Android. This supports a fairly limited set of GPX files along with routes saved from the iOS app. The route opens in the "Create Route" screen allowing it to be saved to the app which saves the Markers and the Route.

A suggested [smoke test]({% link testing/smoke_test.md %}) has a list of features to test and how.
