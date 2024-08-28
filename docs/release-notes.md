# Release notes

## 0.0.37
This release is the first one suitable for a general release to internal testing on the Play Store. It's very limited in its features and we're releasing this to:

1. Validate our build and release processes
2. Check support across a wide selection of real world devices
3. Measure map tile consumption and UI performance
4. Evaluate Google analytics feedback and where we can extend it
5. Get initial feedback on location/heading and audio beacons
6. A UI sanity check.

It's most definitely NOT for real use. Ideally bugs to be reported through [STA Jira](https://sta2020.atlassian.net/jira/software/projects/SA/issues?jql=project%20%3D%20%22SA%22%20ORDER%20BY%20created%20DESC). Testing guidance is at the bottom of this document.

### Features
* **Onboarding screens** These are based on the iOS screens and guide the user through language selectio, permissions, and beacon style selection.
* **Home screen with UI map** An audio beacon can be created with a long press on the map, and then tapping the beacon marker will delete it (or creating a beacon elsewhere on the map).
* **Soundscape service** This runs even when the phone is locked or the app closed. This means that a beacon can be set and then the phone locked. The audio beacon will continue to sound. To close down the service and exit the app, click on the icon in the very top right of the screen. That behaviour will be altered to match iOS - likely in the next release.

### Missing features
Everything else!

The most likely to appear next feature is:
* Some initial text to speech features. The audio support is there, and much of the GeoJSON to query *My Location* and *Around Me*. The next step is to hook these together and link up those buttons.
* Better filtering of location and possibly heading to prevent sudden location jumps due to poor GPS/cell tower switching.

More long term next features:
* Database storage of beacon locations and routes.
* Route playback.
* Increased text to speech features

### Note on the UI maps
The purple water is deliberate! The maps are vector based which means that they are rendered on the phone and can be *styled* locally. To show this we've locally set all water to purple. The *Places Nearby* button also changes the style, toggling the size of the icons for *Food* points of interest (POI). This is just to show that we can, and could be a way to provide improved mapping for those with some vision.

The maps are being served up from maptiler.com on a free account. This is limited to 100k vector tiles per month. The tiles are cached on the phone, but the limit is easy to eat into. As a result, during testing, feel free to zoom into your local area but don't go zooming in around the world until we get a feel for our usage. Using maptiler.com may just be for initial development and we can move to a more cost effective solution (e.g. [cloud hosted protomaps](https://docs.protomaps.com/deploy/cost)).

## Testing guidance

Here's a description of the two sets of screen. If anything listed in "what we expect to work" doesn't match that expectation then please file a bug. If it's only listed in "What will be a bit iffy" then it's not worth filing bugs on yet. If the guidance doesn't make sense, let use know and we'll clarify it!

### Onboarding screens
On first installation the user will be guided through the onboarding screens. Once they have been completed, the only way to return to them is to uninstall/reinstall the app.

 What we expect to work:
 * All buttons.
 * Translations to be present.
 * Selection of language and beacon type to persist into the app.
 * The UI to be at least a bit usable on all phones in portrait, and at least a bit usable in landscape i.e. you can at least scroll to everything that needs clicked on.
 * Clicking the Listen button should result in some speech being heard.
 * Clicking the various beacon types should result in the audio for that beacon type being heard.
 * Permissions dialogs for location, notifications (not on all Android versions) and activity.

What will be a bit iffy:
 * Landscape UI layout
 * Some portrait UI in some languages. We haven't spent a great deal of time on this yet.
 * We haven't done a lot of testing around declining permissions, so for now I'd recommend accepting all of the permissions.

### Home screen
This is the main screen with the map.
 
 What we expect to work:
 * The display of a map, a number of buttons and a menu.
    - The map should be centered at the location of the phone
    - As the phone is turned, the map should turn.
    - The map can be zoomed in/out and moved. It will not return to the initial display unless the app is closed and then re-opened.
 * The top right button should exit the app including the service which plays the audio.
 * A long press on the map should create an audio beacon at that location.
    - A single click on the newly created audio beacon marker should remove the marker and the audio beacon.
    - The audio beacon should be audible! If there's no audio that's a problem.
    - The audio beacon should sound in the direction it is relative to the phone location and the direction that the phone is pointing in. This will require headphones to verify this properly.
    - Exiting the app by swiping it up should not stop the audio beacon.
    - Locking the phone should not stop the audio beacon.
    - The only way to stop an active audio beacon (other than deleting it) is to hit the top right button on the home screen which stops the audio service and exits the app.
 * A notification should appear when the Soundscape service is started up. It should persist even when the user shuts the app (by swiping up etc). Clicking on the notification should restart the app. When restarted, the phone location and the location of any active audio beacon should be shown on the map.
 
What will be a bit iffy:
 * None of the other buttons do anything useful and so don't need tested.
 * None of the menu items do anything useful and so don't need tested.
 
### Known issues
 Some of these are currently tracked on Jira. We should probably move this to GitHub where everyone can more easily access it.
 
 * [No audio from the app](https://sta2020.atlassian.net/browse/SA-18) - needs the phone to be restarted to restore audio. Very interested if this is seen more widely.
 * Location jumps - this is due to a lack of filtering, working on that next
 * Heading drift - the compass can be quite a lot off. Doing the compass calibration wiggle with the phone generally fixes this.
 * No heading data - in this case the map does not rotate with the phone and audio beacons appear static as if the user is always pointing north.