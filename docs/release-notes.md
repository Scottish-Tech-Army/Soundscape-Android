# Release notes

## 0.0.42
This release is still fairly limited in its features but has a lot more than the last release. We're releasing it to:

1. Enable testing!
2. Validate our build and release processes
3. Check support across a wider selection of real world devices
4. Measure map tile consumption and UI performance
5. Evaluate Google analytics feedback and where we can extend it
6. Get initial feedback on location/heading and audio beacons
7. A UI sanity check.

It's most definitely NOT for real use. For any issues found, or questions you have, please open an issue on the [github page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues). I'm very away that this is a long list of features, any suggestions of a better format gratefully received!

### Features
* **Onboarding screens** These are based on the iOS screens and guide the user through language selection, permissions, and beacon style selection.
* **Home screen with UI map** An audio beacon can still be created with a long press on the map, and then tapping the beacon marker will delete it. This is an interim feature and will disappear/change once we have made further progress with waypoints and routes. The maps used are vector tile maps and although not the final maps that we will be using, they are very similar.
  * **Initial My Location support** Tapping the icon in the bottom left of the screen will describe the location of the device. This is using the data from the original GeoJSON server used on the iOS app.
  * **Initial Around and Ahead of Me support** There's a lot of work going on with the code that generates the speech for these two buttons. Their current behaviour is just proof of concept. 
  * **Current Location** brings up the new Location Details screen. It's not useful yet other than to show that it can be done.
  * **Markers and Routes** brings up the UI, but there's nothing behind it yet.
  * **Sleep mode** isn't fully implemented, and it's currently the icon in the top right of the Home screen. Clicking on that will disable the Soundscape service. Clicking it again will turn it back on. This is similar to the very simplest iOS sleep mode.

* **Soundscape service** This runs even when the phone is locked or the app closed. This means that a beacon can be set and then the phone locked. The audio beacon will continue to sound. The service can be stopped and restarted by clicking on the icon in the very top right of the screen.
  * **Filtering of location** This smooths jumps in location. Prior builds would only show the location on the map once a high accuracy location was received. In this release the initial location may be a bit off.
* **Share location** The Share entry in the Menu allows the user to share the current location with developers. Clicking on the link will open the Soundscape app using that location instead of the GPS. This is to aid debugging of mapping issues as it allows the phone to teleport. 
* **Open the app via Share/GPX open** There are a number of different ways to open the app. The usual clicking on the icon opens the app as normal, but we also support
  *  **Open from Google Calendar** Clicking on a location in another app e.g. Google calendar, will  open Soundscape in the Location Details screen for that location. This is using a `geo:` intent from the calendar. That screen lets you set an audio beacon or enter Street Preview mode (more on that later).
  *  **Open via Share from Google Maps** Selecting a place in Google Maps and then clicking on the share icon allows the location to be shared with the Soundscape app. Google Map shares a minified URL and so the Soundscape app follows that to get the full URL and then uses the built in Android Geocoder to get an address for that location. This requires Internet access. The result again is opening the Location Details screen.
  *  **Open a soundscape URI** The behaviour of these URI may well change as we develop further. Currently, it's just a latitude and longitude and clicking on a URI like [soundscape:55.9552485,-3.1928911](soundscape:55.9552485,-3.1928911) will open the Soundscape app in Street Preview mode at the location provided. This is very useful for testing problem locations found by users in the field. That URI is actually from [issue 201](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/201) and makes it simple to reproduce.
  *  **Open a GPX file** from the File application on Android. This supports a fairly limited set of GPX files which contain waypoints and routes. This isn't aimed at end users, but as an initial way to get routes and markers into the app whilst we work on the UI. It will work with ones from the iOS Soundscape application, but it's also possible to generate them with [RideWithGps](https://ridewithgps.com/home). Once a route has been created in RideWithGps it can be exported as a "GPX Route" (not a GPX Track). That creates a file with a Waypoint at each Cuepoint. When the Soundscape app opens the GPX file it doesn't save it to a database, so it only exists whilst the app is running. The app will immediately use that route and start playing the first Waypoint as an audio beacon. When the user reaches that beacon, the app will move to playing the next waypoint as an audio beacon. The location of the audio beacon is reflected on the map in the Home screen. This is the very start of Route playback, and also demonstrates importing routes. A bit more development and the routes and markers will be saved to a database so that they can be used on subsequent runs of the app.
* **Street Preview** is fairly complex on iOS. Our initial work is really to help developers test app behaviour without leaving the house! When Street Preview is entered, either via the Location Details screen or from a soundscape URI, the GPS location is replaced with the static location provided by the user. The Home screen will display that location is if the phone was there. If Street Preview mode is running then a (tiny) eye icon will appear in the top right of the Home screen. Clicking on that will exit Street Preview mode and return to using the phone's actual location. When in Street Preview mode the direction is still controlled by the phone orientation.
* **Menu** 
  * **Settings** allows the altering of a few settings that exist in the app. Changing the beacon type works, but only the next time that the app is run. The callout settings aren't yet hooked in.
  * **Share** shares the current location as a soundscape URI. This can be sent in WhatsApp/Email/Slack etc. This should make it easier to report problem locations.

### Missing features
Everything else!

Features in progress:
* Implementation of the Markers and Routes backend.
* Improved text to speech features.
* Backend infrastructure to allow Search and to hopefully merge map tiles with the data used for audio tiles.

### Note on the UI maps
The maps are being served up from maptiler.com on a free account. This is limited to 100k vector tiles per month. The tiles are cached on the phone, but the limit is easy to eat into. As a result, during testing, feel free to zoom into your local area but don't go zooming in around the world until we get a feel for our usage. This is just for initial development and we have plans for our own more cost effective map server.

## Testing guidance
I've moved this to a [separate document](testing.md).
