# Release notes

## 0.0.39
This release is still very limited in its features and again we're releasing it to:

1. Validate our build and release processes
2. Check support across a wider selection of real world devices
3. Measure map tile consumption and UI performance
4. Evaluate Google analytics feedback and where we can extend it
5. Get initial feedback on location/heading and audio beacons
6. A UI sanity check.

It's most definitely NOT for real use. For any issues found, or questions you have, please open an issue on the [github page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues) .

### Features
* **Onboarding screens** These are based on the iOS screens and guide the user through language selectio, permissions, and beacon style selection.
* **Home screen with UI map** An audio beacon can be created with a long press on the map, and then tapping the beacon marker will delete it (or creating a beacon elsewhere on the map). The maps used are vector tile maps and although not the final maps that we will be using, they are very similar.
* **Soundscape service** This runs even when the phone is locked or the app closed. This means that a beacon can be set and then the phone locked. The audio beacon will continue to sound. The service can be stopped and restarted by clicking on the icon in the very top right of the screen.
* **Initial My Location support** Tapping the icon in the bottom left of the screen will describe the location of the device. This is using the data from the original GeoJSON server used on the iOS app.
* **Filtering of location** This smooths jumps in location
* **Share location** The Share entry in the Menu allows the user to share the current location with developers. Clicking on the link will open the Soundscape app using that location instead of the GPS. This is to aid debugging of mapping issues as it allows the phone to teleport. 
* **Open geo intent** Clicking on a location in another app e.g. Google calendar, can open Soundscape with a beacon at that location.

### Missing features
Everything else!

Features in progress:
* Database storage of beacon locations and routes.
* Route playback.
* Increased text to speech features.

### Note on the UI maps
The maps are being served up from maptiler.com on a free account. This is limited to 100k vector tiles per month. The tiles are cached on the phone, but the limit is easy to eat into. As a result, during testing, feel free to zoom into your local area but don't go zooming in around the world until we get a feel for our usage. Using maptiler.com is just for initial development.

## Testing guidance
Here's a description of the two sets of screen. If anything listed in "what we expect to work" doesn't match that expectation then please file a bug. If it's only listed in "What will be a bit iffy" then it's not worth filing bugs on yet. If the guidance doesn't make sense, let use know and we'll clarify it!

### Onboarding screens
On first installation the user will be guided through the onboarding screens. Once they have been completed, the only way to return to them is to uninstall/reinstall the app.

 What we expect to work:
 * All buttons.
 * Translations to be present.
 * Selection of language and beacon type to persist into the app, both in the UI and in the speech.
 * The UI to be at least a bit usable on all phones in portrait, and at least a bit usable in landscape i.e. you can at least scroll to everything that needs clicked on.
 * Clicking the Listen button should result in some speech being heard
 * Clicking the various beacon types should result in the audio for that beacon type being heard.
 * Permissions dialogs for location, notifications (not on all Android versions) and activity.

What will be a bit iffy:
 * Landscape UI layout
 * Some portrait UI in some languages. We haven't spent a great deal of time on this yet.
 * We haven't done a lot of testing around declining permissions, so for now I'd recommend accepting all of the permissions.
 * Focus order of UI components. We're working on this as it's important for screen reader behaviour.

### Home screen
This is the main screen with the map.
 
 What we expect to work:
 * The display of a map, a number of buttons and a menu.
    - The map should be centered at the location of the phone
    - As the phone is turned, the map should turn.
    - The map can be zoomed in/out and moved. It will not return to the initial display unless the location service is turned off then on again (icon in top right corner).
 * Clicking in the My Location button should describe the current device location. This is a very basic description and there's more work being done here. However, if you think it's grossly incorrect please file a bug.
 * Clicking on the icon in the top left to turn off the location service will remove an audio beacon and the current location marker. Clicking it to turn it back on will re-center around the current device location. The UI map can still be manipulated with the location service off. 
 * A long press on the map should create an audio beacon at that location.
    - A single click on the newly created audio beacon marker should remove the marker and the audio beacon.
    - The audio beacon should be audible! If there's no audio that's a problem.
    - The audio beacon should sound in the direction it is relative to the phone location and the direction that the phone is pointing in. This will require headphones to verify this properly.
    - Exiting the app by swiping it up should not stop the audio beacon.
    - Locking the phone should not stop the audio beacon.
    - The only way to stop an active audio beacon (other than deleting it) is to hit the top right button on the home screen which stops the audio service and exits the app.
 * A notification should appear when the Soundscape service is started up. It should persist even when the user shuts the app (by swiping up etc). Clicking on the notification should restart the app. When restarted, the phone location and the location of any active audio beacon should be shown on the map.
 * In the Menu click on Share to see how to send a location that has an issue. It sends the current location.
  
 
### Known issues
 Some of these are currently tracked on Jira. We should probably move this to GitHub where everyone can more easily access it.
 
 * [No audio from the app](https://sta2020.atlassian.net/browse/SA-18) - needs the phone to be restarted to restore audio. Very interested if this is seen more widely.
 * Heading drift - the compass can be quite a lot off. Doing the compass calibration wiggle with the phone generally fixes this.
 * No heading data - in this case the map does not rotate with the phone and audio beacons appear static as if the user is always pointing north.
 * It takes a little longer than I would have expected for the location and speech to get running properly. As a result, pressing My Location immediately after starting the app doesn't always respond correctly. More work being done to improve thie behaviour.