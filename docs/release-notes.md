# Release notes

## 0.0.45

This release is still very limited in its features but as ever a lot has changed since the last
release. Many of the changes over the last couple of months have been in underlying code, the
biggest change being that the app is now using a single source of geographical tile data. The same
tiles are used to draw the GUI map and to generate the audio callouts. These come from a standard
mapping server ([protomaps](https://protomaps.com/)) which is much more cost effective to run than the iOS
soundscape-backend server. We are generating our own mapping data from OSM and have flexibility to
style the map and its contents to match our requirements both for the GUI and the audio callouts.
The map in the cloud now covers the whole world which means that the app can be tested anywhere.

We're releasing the app to:

1. Enable wider testing and check support across a wider selection of real world devices
1. Get initial feedback on accuracy of geographical data especially for auto callouts
1. Get feedback on location/heading and audio beacons
1. Measure map tile consumption and UI performance
1. Evaluate Google analytics feedback and where we can extend it
1. GUI accessibility testing. There have been a large number of accessibility improvements made to the GUI.

It's most definitely NOT yet for real use. For any issues found, or questions you have, please open
an issue on the [github page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues).

### Features
#### Onboarding screens
These are based on the iOS screens and guide the user through language selection, permissions, and
beacon style selection.

#### A note on audio callouts
Although based on the iOS code, the audio callouts are currently a much reduced implementation. They
are there to test that the geographical data is being interpreted correctly and that the framework
for audio callouts based on location and heading works. One of the main areas of near term work will
be to extend these callouts to better match the complex scenarios that the iOS app supports. 

#### Home screen with UI map
A long press on the map now brings up the Location Details screen which allows creation of an 
audio beacon, or entering Street Preview mode. Tapping the beacon marker will delete it. Clickable
items on the home screen that are implemented are:
  * **Initial My Location, Around Me and Ahead of Me support** Tapping the icons at the bottom of
    the screen will describe the location of the device subject to the previous caveat on callouts.
  * **Current Location** brings up the new Location Details screen. It's not particularly useful yet 
    other than to show that it can be done.
  * **Markers and Routes** brings up the UI, but there's nothing behind it yet.
  * **Sleep mode** isn't fully implemented, and it's currently the icon in the top right of the Home
    screen. Clicking on that will disable the Soundscape service. Clicking it again will turn it back
    on. This is similar to the very simplest iOS sleep mode.
  * **Street Preview** We've improved our Street Preview implementation in this version so that 
    it is getting closer to how the iOS app works. When Street Preview is entered, either via 
    the Location Details screen or from a soundscape URI, the GPS location is replaced with the
    static location provided by the user. The Home screen will display that location is if the 
    phone was there. If Street Preview mode is running then a (tiny) eye icon will appear in the 
    top right of the Home screen, and a tiny play button. These are placeholder until we get the 
    UI implemented properly. For now, the behaviour is:
    * Clicking on the eye icon will exit Street Preview mode and return to using the phone's 
      actual location. When in Street Preview mode the direction is still controlled by the phone
      orientation.
    * Clicking on the Play icon will initially jump to the nearest road. Subsequent presses will 
      jump to the next intersection on the road. Which road to choose at the intersection is 
      chosen by rotating the phone. This should behave pretty much as the iOS app does, but with 
      much smaller buttons/feedback.
  * **Search** using a Komoot Photon backend. This is new to this release and allows 
    geo-searching from within the app. It updates the results if you pause typing in an 
    auto-suggest type way. Tapping on a result opens the Location Details screen where either 
    Street Preview mode can be entered or an audio beacon created.

#### Soundscape service
This runs even when the phone is locked. This means that a beacon can be set and then the phone
locked. The audio beacon will continue to sound. The service can be stopped and restarted by
clicking on the icon in the very top right of the screen. The service is responsible for the audio
beacon play out and the audio callouts.

* **Initial Auto callout support** These are enabled by default and announce upcoming intersections
  and nearby points of interest.
* **Media Playback** controls now work allowing headphone or other Bluetooth triggering of some 
  types of callout. They aren't currently mapped the same as on iOS, though we will change this.
  Instead they're mapped like this:
  * ⏯ Around Me
  * ⏮ Ahead of Me
  * ⏭ My Location

#### Menu
Some of the options within the Menu have been implemented:
* **Share location** This allows the user to share the current location with developers. Clicking on
  the link will open the Soundscape app using that location instead of the GPS. This is to aid
  debugging of mapping issues as it allows the phone to teleport. 
* **Settings** allows the altering of a few settings that exist in the app. This includes 
  selecting the text to speech voice and its speed. Support for alternatives to the Google 
  text to speech engines have been tested including CereVoices, Vocalizer TTS and Acapela TTS 
  engines.
* **Share** shares the current location as a soundscape URI. This can be sent in  
  WhatsApp/Email/Slack etc. This should make it easier to report problem locations.

#### Opening the app
The main way that a user might open the app is by tapping on its icon. However, there are other
ways that we've also implemented.
*  **Open the app via Share/GPX open** There are a number of different ways to open the app. The 
   usual clicking on the icon opens the app as normal, but we also support
*  **Open from Google Calendar** Clicking on a location in another app e.g. Google calendar, 
   will open Soundscape in the Location Details screen for that location. This is using a `geo:` 
   intent from the calendar. That screen lets you set an audio beacon or enter Street Preview mode.
*  **Open via Share from Google Maps** Selecting a place in Google Maps and then clicking on the 
   share icon allows the location to be shared with the Soundscape app. Google Map shares a 
   minified URL and so the Soundscape app follows that to get the full URL and then uses the 
   built in Android Geocoder to get an address for that location. This requires Internet access. 
   The result again is opening the Location Details screen from where Street Preview mode can be 
   enabled.
*  **Open a soundscape URI** The behaviour of these URI may well change as we develop further.  
   Currently, it's just a latitude and longitude and clicking on a URI like [soundscape:55.
   9552485,-3.1928911](soundscape:55.9552485,-3.1928911) will open the Soundscape app in Street 
   Preview mode at the location provided. This is very useful for testing problem locations 
   found by users in the field. That URI is actually from [issue 201](https://github.
   com/Scottish-Tech-Army/Soundscape-Android/issues/201) and makes it simple to  reproduce.
*  **Open a GPX file** from the File application on Android. This supports a fairly limited set 
   of GPX files which contain waypoints and routes. This isn't aimed at end users, but as an 
   initial way to get routes and markers into the app whilst we work on the UI. It will work 
   with ones from the iOS Soundscape application, but it's also possible to generate them with  
   [RideWithGps](https://ridewithgps.com/home). Once a route has been created in RideWithGps it 
   can be exported as a "GPX Route" (not a GPX Track). That creates a file with a Waypoint at 
   each Cuepoint. When the Soundscape app opens the GPX file it doesn't save it to a database, 
   so it only exists whilst the app is running. The app will immediately use that route and 
   start playing the first Waypoint as an audio beacon. When the user reaches that beacon, the 
   app will move to playing the next waypoint as an audio beacon. The location of the audio 
   beacon is reflected on the map in the Home screen. This is the very start of Route playback, 
   and also demonstrates importing routes. A bit more development and the routes and markers 
   will be saved to a database so that they can be used on subsequent runs of the app.

### Missing features
Everything else!

Features currently in progress (in rough order of completion):
* Waypoints and Routes UI.
* Extended and improved callouts.
* Street preview full UI.

## Testing guidance
We're interested in all feedback relating to the API, but are particularly interested in:
* Any accessibility issues in the app. The tiny icons for street preview and sleep we know about,
  but any other areas we welcome feedback.
* Missing points of interest in the audio callouts, or missing information on the GUI map. 
  Walking through areas that you are familiar with and noting any shops or other navigation 
  points of interest that are not being called out is very useful. Please share the problem  
  locations (using the Menu/Share option) along with a brief description of what's missing.
* We don't currently detect "In Vehicle" mode very reliably and callouts only work correctly 
  whilst walking.