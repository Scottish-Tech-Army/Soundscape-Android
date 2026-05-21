---
title: Latest Newsletter
layout: page
nav_order: 3
has_toc: false
---

# Newsletter - 21st May 2026
Welcome to our latest update on what's going on in Soundscape for Android development.

## Release update
We've been working away since the last newsletter and are finally approaching production release! This is really just the start and we have lots more planned for future releases.

[Release notes]({% link release-notes.md %}) are always available.

## What's different between Android Soundscape and old iOS Soundscape?
Some of you may be new to Android Soundscape so here are the highlights of features which are only available on Android and are available in the 1.0 releases:
* The app supports downloading map data to use when there's no Internet available, or to save the cost of mobile data. This is called offline maps and there are maps available for countries, states and groups of cities and towns.
* We generate the map tiles once a month from Open Street Map data and that includes the offline map extracts. It's up to the user to download new offline map extracts, though we'll aim to make this easier in future releases of the app.
* Search works with offline maps. If there's no Internet then Search automatically uses offline maps, or it can be forced to use the offline maps in the Settings.
* My location also works with offline maps and if the Open Street Map data includes house numbers for a street then it will use those too.
* The new app code can follow roads and paths more easily and this helps with figuring out which road the user is on and describing there the road or path goes. For example it describes unnamed roads e.g "Road to library" or "Path via steps to playground". This extra context helps disambiguate unnamed roads and because the descriptions are consistent helps with building a mental map.
* The new code also does a better job of figuring out which road or path a user is travelling along. The original code used the closest road to the GPS location and then had some simple rules to decide when to switch to a new road. The new code uses algorithms similar to those used by apps like Uber and it helps prevent the user suddenly jumping from being on a road over a bridge to the road under the bridge.
* Increased media controls functionality. It's possible to run in Audio Menu mode where media skip/play/previous navigate an audio menu so that the user can control the app even when the phone is in their pocket. This can be done from most headphones, though you can also use a small Bluetooth set of media control buttons to do this. You can keep that in your pocket and trigger Hear My Surroundings as well as starting and stopping routes and beacons.
* The servers are much cheaper to run and much more scalable with the new app.

Different users will find different features useful to them, but the ability to use the app without Internet or mobile data means that it can be used by many more people.

## What's next?
We've started getting a lot more feedback from users of iOS Soundscape which is very useful as those are the people who have the most experience often having many, many hours of use over the years. We'll be incorporating anything that we missed from iOS Soundscape so that the Android app will become even more familiar to those users.

We also don't want iOS users to miss out on all of the new features, so we are working on building an iOS version of app from the same codebase. This has the double benefit of having experienced iOS users able to test the new code as well as giving them access to offline maps and the other improvements that we've made. The initial test versions of this are still several months away, but they are coming. 

### User feedback!
As always we really do want your input! Please don't hesitate to use the Contact Support option in the app menu to get in touch with comments or suggestions. We really do appreciate it, this app is for you. We also plan to start reaching out directly to ask specific questions about the app and your use of it.

## That's it
Hopefully this gives you a clear picture of what's happening. We should be back with another update in a few months. In the meantime, thank you for your continued support, and please keep the feedback coming!
