---
title: Release notes
layout: page
nav_order: 5
has_toc: false
---

# Release notes

# 0.0.114
We keep track of issues in GitHub [here](https://github.com/Scottish-Tech-Army/Soundscape-Android/milestones).

## New changes since 0.0.110
* *Current Location* now appears as an option in the *Add Waypoint* screen
* Improvements have been made to deal with cases where permissions (location and notifications) have been revoked.
* Initial Persian and Arabic language support has been added and various additions to other languages. We welcome suggested improvements on Weblate <https://hosted.weblate.org/projects/soundscape-android/android-app/>
* The *Search* feature was broken for many languages, it is now fixed. There's also a new *Settings* option to force *Search* results to use English. A use case for this would be a Spanish speaker travelling in Japan. Instead of getting results in the local Japanese language it may be easier to have the English results e.g. Tokyo instead of 東京都.
* Street Preview calls out a single POI after each jump
* Marker callouts now use a field-of-view to prevent calling out markers which have been passed
* Distance to beacons is now adaptive for long journeys. The frequency of the callouts increases as the beacon gets closer.
* GPS location filtering had been broken and so although the map matching to the current street worked well, beacons were unstable. The filtering has been re-instated and the beacons behaviour is back to what it should be.
* *Save Marker* screen wasn't reflecting the user direction
* A long term issue where the graphical map wasn't updating beacons and routes immediately has been fixed.
* More analytics added to track text to speech engine usage and startup errors.
* Route playback had a race so that it wasn't always playing back correctly after the first route played.
* Beacon setting now works across all languages, though it still doesn't playback audio as the selections are made.


## Changes between 0.0.96 and 0.0.110
* A new option in settings to select miles and feet instead of kilometers and meters.
* Improved light and dark themes and defaulting to use the system setting.
* Improved handling of locations shared from Google Maps especially those of places with names containing non-ASCII characters.
* The onboarding screen now honours the safe window so that buttons don't appear under system buttons on newer Samsung phones.
* When the onscreen keyboard is visible for typing into the search bar, the "Hear my surroundings" and "Title bar" both disappear to give more screen estate. This is most important when using large font sizes.
* The accessible map style is now enabled by default. It can still be disabled/enabled in the app settings.
* Stability improvements:
  * Fixed memory leak when entering Sleep mode
  * Fix for OnePlus Pro8 startup crash
  * Fix for Honor X8c crash relating to text to speech engine
  * A fix for a locking issue in audio beacon destruction
* Improved Media Control buttons behaviour when there's only 1 audio beacon in a route. Instead of trying to skip/preview they fallback to their other behaviour e.g. "My location"
* Audio focus is now maintained when switching between audio beacons in a route
* Initial Ukrainian language support has been added
* Settings now includes the ability to select the TTS engine to use as well as the voice from within that engine. Audio engine support is also much more robust.
* Locales from all voices are listed for TTS engines and not just those from the chosen locale
* New option in Settings to disable graphical maps from screen
* Improved behaviour when permissions haven't been allowed - no crashing and the user is prompted to change the permissions with a link to the app settings

### Longer term known issues
The largest issues which are being worked on are:
* [Offline mapping](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/236) has been tested as a proof of concept but still requires a lot of work.
* [Route playback](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/667) has various imperfections which are being worked on.
* [Improving mapping data](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/605) to try and improve both search, the graphical maps and the contents of Places Nearby.
* [Callouts for roundabouts need work](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/442). We have a plan for this, but it's not yet implemented.
* [Support for Street Preview is very preliminary](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/528), especially when it comes to use by people with a visual impairment.

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
