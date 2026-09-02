---
title: Test instructions for a new user
layout: page
parent: Testing
has_toc: false
---

# Testing the Soundscape app

The Android and iOS apps are now built from a single shared codebase, so the UI and behaviour
described here apply to both unless a section specifically calls out a difference between them.

**Reporting problems:** if you're an STA member you can ping us on Slack. Otherwise please send all
feedback to the Help Desk at <soundscapeAndroid@scottishtecharmy.support>.

## Requirements

* **Android:** Android 11 (API 30 - see <https://apilevels.com/>) or later.
* **iOS:** iOS 16 or later.

We don't know of any other requirements, but that's one of the things this testing should help us
understand.

## Installing the app

### Android

The released version of the app is freely available on the
[Play Store](https://play.google.com/store/apps/details?id=org.scottishtecharmy.soundscape), and
installs like any other app.

Most of what we need tested, though, is in the **beta** releases, which come out much more often
than the full releases. The beta is a closed one, so you can't simply opt in from the Play Store -
we have to register your email address before Google will offer you the beta builds at all:

1. Send us the **Google account email address** you use on the phone you'll be testing with. STA
   members can do this on Slack; otherwise email the Help Desk at
   <soundscapeAndroid@scottishtecharmy.support>. It has to be the account that's signed in to the
   Play Store on that phone - if you send us a different address, the beta won't appear.
1. We'll add you to the tester list and send you an opt-in link back.
1. Follow that link, signed in as the same account, and accept the invitation to become a tester.
1. Install or update Soundscape from the Play Store as normal. There can be a delay of a few
   minutes to a few hours between accepting the invitation and the beta actually being offered to
   you, so if you still see the released version, leave it a while and check again.

After that, beta updates arrive through the Play Store like any other app update. If you want to
stop testing later, just let us know and we'll take you off the list.

### iOS

The rewritten app is currently only available via TestFlight, by invitation. If you're an STA
member, ask on Slack for an invite; otherwise request one via the Help Desk at
<soundscapeAndroid@scottishtecharmy.support>. Once you have an invitation, install TestFlight from
the App Store first, then follow the invitation link to install the beta build.

If you're updating from the older, Microsoft-authored iOS app rather than installing fresh, see
[Upgrading from the legacy Soundscape iOS app]({{ "/ios-upgrade-from-legacy.html" | relative_url }})
for what's carried over automatically and what has changed.

## Running the app the first time

The first time you run Soundscape you'll see a series of onboarding screens which let you choose
some initial settings.

The base language is US English, and all translations are made from it. If a string hasn't been
translated yet it appears in English instead - we already know about those, so there's no need to
report them. Any *other* language problem is of interest, though: text that's hard to understand,
or where the ordering of the phrases has come out wrong.

Things we're interested in on the initial screens:

* Is there any text you're unable to read, or where words are split across lines?
* Do you hear only silence when you tap the **Listen** button on the **Hear Your Surroundings**
  screen?
* Do you hear only silence when trying the different beacon sounds on the **Choose an Audio Beacon**
  screen?

Please report any of these via the Help Desk.

## Main app operation

Once you're past the onboarding screens you shouldn't see them again, and you should be on the main
screen:

<img src="{{ "/documentationScreens/homeScreen.png" | relative_url }}" width="200" alt="Screenshot of the Soundscape home screen">

Soundscape keeps running in the background from this point on. When you want to close it for good
you can exit from the main menu, or swipe the app closed - exactly how you do that depends on your
phone.

Soundscape is designed to be used with headphones.

These things should all happen on the Home screen, and we're interested if any of them don't:

* The map shows your current location with a red triangle. (There should be a map!)
* The red triangle rotates as you point the phone in different directions.
* You hear speech describing your surroundings when you tap each of the four buttons at the bottom
  of the screen.

If you've got this far and it all seems to be working, you can move on to the more detailed tests
below.

### Test 1 - Go for a walk

As you move around, Soundscape should periodically describe your location and call out points of
interest that you pass - shops, bus stops and so on. We're interested in anything that doesn't
sound right.

The app uses a little mobile data as it downloads maps along the way, but not much: roughly 50kb
for a 600m square of map. Tiles are cached, so they're generally only downloaded once. If you'd
rather not use mobile data at all, or you want to test with no network connection, see
[Test 4](#test-4---offline-maps).

### Test 2 - Create a route and play it back

This uses a bit more of the UI, but once it's set up it should be fairly straightforward.

#### Create some markers

Markers are points on the map which can be combined into a route. They're saved from the Location
Details screen, and there are several ways to get there:

* Long tap the map on the Home screen, or the map on the _Current Location_ screen.
* Tap _Current Location_ on the Home screen. That map scrolls and zooms, so you can save a marker,
  move to a new point, long tap and save another.
* Tap _Places Nearby_ on the Home screen and pick one of the nearby points.
* Search, and tap one of the results.

Saved markers appear under the _Markers and Routes_ button on the Home screen. Once you have a few,
you can make a route from them.

#### Create a route

1. On the _Markers and Routes_ screen, select the _Routes_ tab and tap the **+** icon in the top
   right.
1. Type a name for your route, and a description if you want one.
1. Tap _Add Waypoints_ and add the markers you've created, selecting them in the order you want
   them to appear in the route. Tap _Done_.
1. Tap _Done_ again to save the route.

Your route should now be listed. Tap it to check it looks the way you expect.

#### Play the route

Tap _Start Route_ on the _Route Details_ screen. An audio beacon starts playing at the first
waypoint, and sounds from the direction of that waypoint relative to where you are.

* With the app open and your phone unlocked, the direction used is the way the **phone** is
  pointing.
* Lock the phone and put it in your bag, and it switches to the direction you're **walking** in.
* The beacon sounds different depending on whether you're walking towards it or away from it.
* If you stop moving with the phone locked, the beacon gets quieter to indicate that there's no
  direction data available.

### Test 3 - Travelling by car, bus or train

Soundscape behaves differently when you're moving at speed, and this part of the app is newer and
less well tested than the walking behaviour, so we're particularly keen for feedback on it. You
should be travelling as a **passenger** - please don't test this while driving.

There's nothing to turn on. The app switches into travelling mode by itself whenever you're moving
faster than about 10mph, and switches back once you slow down or get out and walk. Ordinary
pedestrian callouts (nearby shops, road crossings and so on) are deliberately suppressed while
you're travelling, because at speed there are far too many of them to be useful, and the range at
which things are called out is stretched a long way so that you hear about them before you've
already gone past.

Go for a journey of at least fifteen minutes or so - a bus route or train trip you know well is
ideal, because you'll be able to tell whether what you hear matches reality. Things you should hear
along the way:

* **Where you are, every so often**, e.g. "Travelling north along M8" or "Travelling east along A81
  (Glasgow Road) close to Milngavie". Numbered roads should be announced by number, and shouldn't
  be re-announced every time the street name changes along the same road.
* **Settlements you're heading towards** with a distance, and the ones you're moving away from or
  simply passing near.
* **Motorway junctions and exits** as you reach them.
* **Large landmarks** as you pass them - parks, hospitals, stadiums, shopping centres.
* **Bus, tram and train stops** as you pass them. On a two-way street you should only hear about
  stops on your side of the road, not the ones serving the opposite direction.
* **Rivers, canals and railways you cross**, e.g. "Passing over the River Clyde".
* **Tunnels**, e.g. "Entering a tunnel" - which is mainly there to explain why things are about to
  go quiet, since GPS is lost inside.

Note that unlike most Soundscape callouts, the periodic "where you are" announcement isn't
positioned in 3D - it's spoken from straight ahead, because it isn't describing something at a
particular place around you.

On a **train** it's a little different. The app matches you against the railway network rather
than the road network, so what you hear is the settlements you pass and how far you've come since
the last station, e.g. "On train and close to Partick, 1.6 km since Westerton". Railway lines are
just called "train" for now rather than being named - the line names aren't in our map data yet, so
there's no need to report that. Trains also don't get compass directions, road junctions or
landmark callouts.

The thing to watch for on this one is being told you're on a train when you're actually in a car or
bus. Motorways and railway lines are often built alongside each other for miles at a time, and
telling the two apart is genuinely hard, so we'd very much like to hear about it if that happens.

Things we're interested in:

* Callouts for roads, stations or places that are simply wrong, or in the wrong order.
* Being told about the same thing over and over, or long silent stretches where you passed things
  worth mentioning.
* Anything announced too late to be useful.
* Being told you're on a train when you're in a car or bus, or the other way round.

This test benefits enormously from a location recording - see
[Providing a debug location trace](#providing-a-debug-location-trace) below. If you can turn that
on before you set off and share the trace afterwards, we can replay the whole journey and see
exactly what the app was thinking.

### Test 4 - Offline maps

Soundscape can download map data for a region so that it keeps working - map, callouts and Places
Nearby included - with no Internet connection at all. See
[Offline map extracts]({{ "/users/help-offline-map-extracts.html" | relative_url }}) for the kinds
of extracts available (country, region and city).

1. Open the menu (the hamburger in the top left) and tap _Offline maps_.
1. You should see any extracts covering your current location listed. Pick one and tap _Download
   offline map_. The first time anyone downloads a particular extract there can be a short delay
   while the server prepares it; after that it should start straight away. A whole country takes
   longer and uses more storage than a city, so pick whichever is convenient for testing.
1. You can also find nearby extracts from any Location Details screen - look for the _Nearby
   offline maps_ button.
1. Once a download finishes, turn on Airplane Mode (or otherwise disable WiFi and mobile data) and
   repeat Tests 1, 2 and 3 above. The map should still draw, and callouts and Places Nearby should
   still work from the downloaded data.
1. Settings has a _Search mode_ option (Auto/Online/Offline). With no network, search should fall
   back to offline data by itself in Auto; you can also force it to _Offline_ to test that path
   while you still have a connection.
1. Downloaded maps can be removed again from the _Offline maps_ screen - tap a downloaded map and
   choose _Delete offline map_. Worth checking too, especially if you're short on storage.

We're interested in anything that doesn't work the same offline as it does online, how long
downloads take, and whether the list of available extracts makes sense for your area.

## Providing a debug location trace

The app can keep up to an hour of recorded location data while it's running. The feature is off by
default, and even when it's on the data stays on your phone unless you choose to share it.

1. Open the menu (the hamburger in the top left), tap _Settings_, scroll to the bottom and turn on
   **Enable recording of travel**.
1. With that enabled, a new **Share recording of travel** option appears in the menu drawer below
   _About Soundscape_. Tap it to send us the GPX track by email or anything else in the standard
   Android share menu or iOS share sheet.

The file holds the location data recorded by your phone for up to the last hour the app has been
running. Don't share it if you'd rather we didn't know where you've been. There's no identifying
data in it, though obviously we'll know who sent it.

It's genuinely useful to us: we can load a GPX file into our test code, regenerate the callouts you
would have heard, see which road or path the app thought you were on, and work out why a callout
came out wrong or didn't happen at all. Enabling it is entirely optional.

## Final notes

There are other features in the app, but the focus for this testing is the ones above. If anything
in these instructions is unclear, let us know. Once we have some feedback there'll be bugs to fix,
and then we'll do incremental releases. If you're interested in helping out further on the project,
take a look at the STA volunteer app for some available roles.

Thanks for reading!
