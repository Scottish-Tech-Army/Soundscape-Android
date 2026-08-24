---
title: Test instructions for a new user
layout: page
parent: Testing
has_toc: false
---

# Testing the Soundscape app
The Android and iOS apps are now built from a single shared codebase, so the UI and behaviour
described in this document apply to both unless a section specifically calls out a difference
between the two platforms.

Unless you are an STA member and can ping us on Slack, all feedback should go via the Help Desk by emailing <soundscapeAndroid@scottishtecharmy.support>.

## Requirements
* Android: Android 11 (API 30 - see <https://apilevels.com/>) or later.
* iOS: iOS 16 or later.

We don't know of any other requirements, but that's one of the things this testing should help us understand.

## Installing the app
* Android: the app is freely available on the Play Store [here](https://play.google.com/store/apps/details?id=org.scottishtecharmy.soundscape).
* iOS: the rewritten app is currently only available via TestFlight, by invitation. If you're an STA member, ask on Slack for an invite; otherwise request one via the Help Desk by emailing <soundscapeAndroid@scottishtecharmy.support>. Once you have an invitation, install TestFlight from the App Store first and then follow the invitation link to install the beta build.

If you're updating from the older, Microsoft-authored iOS app rather than installing fresh, see [Upgrading from the legacy Soundscape iOS app]({{ "/ios-upgrade-from-legacy.html" | relative_url }}) for what's carried over automatically and what has changed.

## Running the app the first time
The first time you run the Soundscape app you will see a series of onboarding screens which let 
you select various initial settings. English is US English and is the base for all translations. If a string is missing in the language being used then it will be replaced by the English string instead. We already know about these, but all other language issues are of interest e.g. text that is difficult to understand or where there's a problem with the ordering of phrases.

Things we're interested in on the initial screens:

* Is there any text on the screens that you are unable to read or where words are split across 
  lines?
* Do you just hear silence when you click the **Listen** button on the **Hear Your Surroundings** screen?
* Do you only hear silence when selecting the different beacon sounds on the **Choose an Audio Beacon** screen?

Please report any of these issues via the Help Desk.

## Main app operation
Now that you're past the onboarding screens, you shouldn't see them again and you should be on 
the main screen:

<img src="{{ "/documentationScreens/homeScreen.png" | relative_url }}" width="200" alt="Screenshot of the Soundscape home screen">

Soundscape will now continue to run in the background. To exit it, click on the top right corner 
to put the app to sleep, and then close the app (swipe up from the app switcher, or the 
equivalent gesture on your device).

Soundscape is designed to be used with headphones.

Things that should happen on the Home screen and we're interested if they do not:

* The map should show your current location with a red triangle. (There should be a map!)
* The red triangle on the map should rotate as you point the phone in different directions.
* Speech describing your surroundings should be heard when you click on each of the 4 buttons at the bottom of the screen.

If you've got to this point and it all seems to be working, then you can move on to more detailed testing.

### Test 1 - Go for a walk
As you move around, Soundscape should periodically describe your location and call out any points of interest that you pass e.g. Shops, Bus Stops etc. We're interested if there's anything that doesn't sound right. The app will consume a little bit of data as it downloads maps as you move around, but in general those are fairly small (< 50kb for a 600m square bit of map). The map tiles are cached so they will generally only be downloaded once. If you'd rather not use any mobile data at all, or want to test the app with no network connection, see Test 3 below on offline maps.

### Test 2 - Create a route and play it back
This uses a bit more of the UI, but once set up it should be fairly straightforward.
#### Create some Markers
Markers are points on the map which can be added together to make a route. Markers can be saved from
the Location Details screen, but there are many ways to get to that.
1. A long tap on the map on the home screen, or the map on the _Current Location_ screen will bring up a Location Details screen for that location.
1. The _Current Location_ button on the main screen brings up the Location Details for the current location. The map there is scrollable and you can zoom in and out. It's possible to save markers, move to a new point, long tap and save another marker.
1. The _Places Nearby_ button on the home screen shows nearby points that can be clicked on for Location Details.
1. The search bar will bring up results which can be clicked on for Location Details.

Once saved, Markers appear in the screen that can be navigated from the _Markers and Routes_ button on the home screen. Once you have a number of Markers, you can create a route.

#### Create a route
1. With the _Routes_ tab of the _Markers and Routes_ screen selected, click on the + icon in the top right. 
1. Type in a name for your route, and an optional description.
1. Click _Add Waypoints_ and add the Markers you've created. Select the markers in the order that you want them to appear in the route and then click _Done_.
1. Click _Done_ again to save the route.

There should now be a route listed. Click on that and you can check that it's what you think it should be.

#### Play the route
Click _Start Route_ on the _Route Details_ screen to start an audio beacon playing at the first waypoint of the route. The audio beacon will sound from the direction of the waypoint from where you are. When you're using the Soundscape app and your phone is unlocked, the direction used is the direction that the phone is pointing in. You can lock your phone and put it in your bag and then it will start using the direction in which your walking. The sound of the beacon will be different if you are walking towards it or away from it. If you stop moving and your phone is locked then any beacon will go quieter to indicate that there's no available direction data.

### Test 3 - Offline maps
Soundscape can download map data for a region so that it keeps working - map, callouts and Places Nearby included - with no Internet connection at all. See [Offline map extracts]({{ "/users/help-offline-map-extracts.html" | relative_url }}) for the kinds of extracts available (country, region and city).

1. Open the Menu hamburger in the top left and tap _Offline maps_.
1. You should see any extracts that cover your current location listed. Pick one and tap _Download offline map_. The first time anyone downloads a particular extract there can be a short delay while the server prepares it - after that it should start straight away. Larger extracts (e.g. a whole country) will take longer and use more storage than a city extract, so pick whichever is convenient for testing.
1. You can also find nearby extracts from any Location Details screen - look for a _Nearby offline maps_ button.
1. Once a download completes, turn on Airplane Mode (or otherwise disable WiFi and mobile data) and repeat Test 1 and Test 2 above. The map should still draw, and callouts and Places Nearby should still work using the downloaded data.
1. In Settings there's a _Search mode_ option (Auto/Online/Offline). With no network, Search should automatically fall back to offline data (Auto); you can also force this by setting it to _Offline_ so you can test that path even when you do have a network connection.
1. Downloaded maps can be removed again from the _Offline maps_ screen - tap on a downloaded map and choose _Delete offline map_ - which is worth checking too if you're short on storage.

We're interested in anything that doesn't work the same offline as it does online, how long downloads take, and whether the list of available extracts makes sense for your area.

## Providing debug location trace
The app can store up to an hour buffer of the user location recorded whilst the app is running. This feature is disabled by default, and even when enabled the data stays on the phone unless the user chooses to share it via interaction with the app. To use the feature:
1. Tap on the Menu hamburger in the top left, and then tap on "Settings" scroll to the bottom and you'll see the "Enable recording of travel" option. Click to enable/disable.
2. With the setting enabled, a new option appears in the Menu drawer below "About Soundscape" which is "Share recording of travel". If you want to share a GPX track you can click on that and you can then choose to send the file to us via email etc. using the standard Android share menu or iOS share sheet.
The file contains the location data recorded by the device's location services for up to the last hour that the app has been running. Don't share it with us if you don't want us to know where you've been. There's no identifying data in it, though obviously we'll know who sent it.

We can load the GPX file into our test code and it will generate the callouts that the user will have heard and we can see which road/path the app thought it was following, and figure out why callouts were generated incorrectly or not generated at all.
Enabling the setting is absolutely optional, but it is useful to us for debugging.


## Final notes
There are other features in the app, but the focus for this testing is those above. If there's
anything unclear in these instructions let us know. Once we have some feedback, there'll be some
bugs to fix, and then we'll do incremental releases. If you are interested in helping out further
on the project, take a look at the STA volunteer app for some available roles.

Thanks for reading!