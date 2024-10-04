Here's a description of the current features that we have and how they work. If anything listed in "what we expect to work" doesn't match that expectation then please file a bug. If it's only listed in "What will be a bit iffy" then it's not worth filing bugs on yet. If the guidance doesn't make sense, let use know and we'll clarify it!

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
 
### Home screen
This is the main screen with the map.
 
 What we expect to work:
 * The display of a map, a number of buttons and a menu.
    - The map should be centered at the location of the phone
    - As the phone is turned, the map should turn.
    - The map can be zoomed in/out. It will always re-center to the current location when the location or orientation is updated.
 * Clicking in the My Location button should describe the current device location. This is a very basic description and there's more work being done here. However, if you think it's grossly incorrect please file a bug.
 * Clicking on the icon in the top left to turn off the location service will remove an audio beacon and the map. Clicking it to turn it back on will re-center around the current device location.
 * A long press on the map should create an audio beacon at that location.
    - A single click on the newly created audio beacon marker should remove the marker and the audio beacon.
    - The audio beacon should be audible! If there's no audio that's a problem.
    - The audio beacon should sound in the direction it is relative to the phone location and the direction that the phone is pointing in. This will require headphones to verify this properly.
    - Exiting the app by swiping it up should not stop the audio beacon. The Soundscape service should carry on running and playing the audio beacon.
    - Locking the phone should not stop the audio beacon.
    - The only way to stop an active audio beacon (other than deleting it) is to hit the top right button on the home screen which stops the Soundscape service. If the app is exited whilst in this state, then the app is fully exited.
 * A notification should appear when the Soundscape service is started up. It should persist even when the user shuts the app (by swiping up etc). Clicking on the notification should bring the app back up. When restarted, the phone location and the location of any active audio beacon should be shown on the map.
 * In the Menu click on Share to see how to send a location that has an issue. It sends the current location.
  
 
### Known issues
 Some of these are currently tracked on Jira. We should probably move this to GitHub where everyone can more easily access it.
 
 * [No audio from the app](https://sta2020.atlassian.net/browse/SA-18) - needs the phone to be restarted to restore audio. Very interested if this is seen more widely.
 * Heading drift - the compass can be quite a lot off. Doing the compass calibration wiggle with the phone generally fixes this. This seems to be a general Android (and iOS too I expect) frailty
 * No heading data - in this case the map does not rotate with the phone and audio beacons appear static as if the user is always pointing north.
 