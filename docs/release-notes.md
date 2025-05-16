# Release notes

# 0.0.71
## New in this release
* The code that deals with the map data has changed significantly. This particularly affects all callouts and StreetPreview,
* Improved matching from the GPS location to a location on the map, tracking which road a user is travelling on. It's impossible to be perfect, as there's error both in the GPS location and in the mapping data, but it's much improved. This should lead to more accurate callouts describing the nearby intersections, especially when there are roads at different altitudes e.g. bridges, tunnels etc.
* Un-named roads and paths are now described with some additional context where possible. The code follows along an un-named road or path until it hits a split in the road and then tries to name it.  There are various issues filed to increase this capability, but for now this includes:
  * <Un-named Way> to <named Way> e.g. "Path to Moor Road"
  * <Un-named Way> to <POI> e.g. "Service to "Police Station"
  * <Un-named Way> to <Marker> e.g. "Path to Dave's first marker". This means that all un-named ways can be 'named' by use of Markers.
  * <Way> to dead-end e.g. "Glassford Street to dead end"
  * "Via" which includes whether an un-named Way goes via "steps", "a bridge" or "a tunnel" e.g "Path via steps to Crossvegate" or "Path via tunnel to train station"
* Special treatment for paths marked as "sidewalk" in OpenStreetMap. When travelling along a sidewalk, the callouts are now given relative to the road that the sidewalk is next to. That means that paths joining just the sidewalk won't be called out, but that road junctions are much more clearly described.
* GPX (GPS track) sharing for up to the last hour of app use. This is off by default, but can be enabled in the settings. Once enabled, each GPS location is recorded into a buffer which will hold the last hour of data. From the menu "Share recording of travel" can be used to generate a GPX file from the buffer and share it with another app e.g. Slack/Gmail etc. If when testing a user has a callout which they think is wrong, they can share the GPX with us and we can investigate it more easily.
* Beacons started from the Location Details page can now be stopped from the UI.

### Still to come
The issues listed [here](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues) are still being worked on. There's definitely still more work to do improving the callouts, especially reducing less useful ones.

## General information
This release has the majority of the main features implemented. The maps served up from the cloud cover the whole world which means that the app can be tested anywhere.

We're releasing the app to:

1. Enable wider testing and check support across a wider selection of real world devices
2. Get feedback on the accuracy of geographical data, both for audio callouts and the UI
3. Get feedback on location/heading and audio beacons
4. We want to measure map tile consumption and UI performance
5. GUI accessibility testing. There have been a large number of accessibility improvements made to the GUI.

Known issues can be seen on our [github page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues) and if you have a new issue you can open it there, though it does require creation of a (free) GitHub account.

### Features
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
  * **Search** using a Komoot Photon backend. This allows geo-searching from within the app. It updates the results if you pause typing in an auto-suggest type way. Tapping on a result opens the Location Details screen.

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

### Work in progress
Features currently in progress:
 * Improving the audio callouts
 * Improving the graphical map style to be higher contrast and more accessible
 * De-duplicating marker callouts with any POIs that they might be coincident with.
 * In vehicle mode needs some attention, we've focussed mostly on audio callouts whilst walking.

## Testing guidance
We're interested in all feedback, but we aren't visually impaired and so particularly value usability feedback - especially if we fall short compared with the iOS Soundscape app.

A suggested ["smoke test"](smoke_test.md) has a list of features to test and how.
