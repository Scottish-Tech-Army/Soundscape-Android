# Release notes

## 0.0.56

This release has many of the main features at least partially implemented. The maps from the cloud cover the whole world which means that the app can be tested anywhere.

We're releasing the app to:

1. Enable wider testing and check support across a wider selection of real world devices
1. Get initial feedback on accuracy of geographical data, both for callouts and in the UI
1. Get feedback on location/heading and audio beacons
1. We want to measure map tile consumption and UI performance
1. GUI accessibility testing. There have been a large number of accessibility improvements made to the GUI.

It's not yet ready for real use in that it has had very little real world testing. For any issues found, or questions you have, please open an issue on the [github page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues).

### Features
#### Onboarding screens
These are based on the iOS screens and guide the user through language selection, permissions, and beacon style selection.

#### Home screen with UI map
Unlike iOS, a long press on the map brings up the Location Details screen which is central to the UI. As on iOS that screen allows 
- creation of an audio beacon at the location
- saving the location as a marker
- entering Street Preview mode at the location
- _Sharing of the location will be added soon_

Clickable items on the home screen that are implemented are:

  * **My Location, Around Me and Nearby Markers support** Tapping the icons at the bottom of the screen will describe the location of the device via voice using a similar algorithm to Soundscape iOS. 
  * _**Ahead of Me** behaviour doesn't currently match iOS and will be updated soon._
  * **Places Nearby** lists all of the POI within the local area. It doesn't yet have the filtering by category available in iOS. Clicking on an entry in the list brings up the Location Details screen.
  * **Markers and Routes** brings up the UI for marker editing and route creation/editing. This is similar to iOS though the **Places Nearby** lists need to be added to **Add Waypoint** dialog which is currently Markers only.
  * **Current Location** brings up the Location Details screen for the current location.
  * **Sleep mode** Clicking on that will disable the Soundscape service and bring up the full screen sleep screen. This is similar to the very simplest iOS sleep mode, wake on leave is not yet implemented.
  * **Street Preview** This is getting closer to how the iOS app works. When Street Preview is entered via the Location Details screen the GPS location is replaced with the static location provided by the user. The Home screen will display that location is if the phone was there. If Street Preview mode is running then buttons appear at the top of the home screen showing the options available.
    * Exiting Street Preview mode and returning to using the phone's actual location. When in Street Preview mode the direction is still controlled by the phone orientation.
    * Clicking on the other button jumps to the next intersection on that road. Which road to choose at the intersection is 
      chosen by rotating the phone. This should behave pretty much as the iOS app does, but with less animated buttons/feedback.
  * **Search** using a Komoot Photon backend. This allows geo-searching from within the app. It updates the results if you pause typing in an auto-suggest type way. Tapping on a result opens the Location Details screen.

#### Soundscape service
This runs even when the phone is locked. It means that a beacon can be set or a route played and then the phone locked. The audio beacon will continue to sound. The service can be stopped and restarted by entering and exiting Sleep mode. The service is responsible for the audio beacon play out and the audio callouts. The heading logic is similar to iOS which means that when the phone is locked the heading is based on the current direction of travel. If the phone is unlocked - or is locked and held flat in front of the user - then the heading used is the direction that the phone is pointing in.

* **Initial Auto callout support** These are enabled by default and announce upcoming intersections and nearby points of interest.
* **Media Playback** controls now work allowing headphone or other Bluetooth triggering of some types of callout. They aren't currently mapped the same as on iOS, though we will change this.
  Instead they're mapped like this:
  * ⏯ Around Me
  * ⏮ Ahead of Me
  * ⏭ My Location

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

### Work in progress
Features currently in progress:
 * Ahead of Me
 * Improving the audio callouts
 * Improving the graphical map style to be higher contrast and more accessible
 * Adding callouts for Markers and de-duplicating their callout with any POIs that they might be coincident with.
 * Improving the Places Nearby list and adding it to the Add Waypoints list.
 * Improving Talkback behaviour especially hints which were present in iOS.
 * Add the ability to switch between color themes.
 * In vehicle mode needs some attention, we've focussed mostly on audio callouts whilst walking.

## Testing guidance
We're interested in all feedback, but we aren't visually impaired and so particularly value usability feedback - especially if we fall short compared with the iOS Soundscape app.

A suggested ["smoke test"](smoke_test.md) has a list of features to test and how.
