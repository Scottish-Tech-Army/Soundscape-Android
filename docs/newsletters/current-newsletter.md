---
title: Latest Newsletter
layout: page
nav_order: 3
has_toc: false
---

# Newsletter - 19th December 2025

Welcome to our latest update on what's going on in Soundscape for Android development.

## Release update
We had a couple of months where we didn't have any new releases, and that was because we were focusing on support for offline maps. This was a fairly large chunk of work because not only did it involve server changes as well as app changes, but we also changed the zoom level used by the app in order to reduce the amount of storage required for offline maps. The end result is that users can now download maps that cover every area of the planet. Once downloaded, Soundscape will use them instead of fetching them over the internet, and so will work reliably when there's no internet connection available.

Full [release notes]({% link release-notes.md %}) are always available.

On the language front, we're excited to add *Polish*, *Icelandic*, and *Russian*. Initial work was done by native speakers, though AI translation was used to complete the work and this is very likely to be imperfect. If you are a native speaker of any of the languages in Soundscape and you see something wrong with the language used in the app, please get in touch. You can do this either via Contact Support in the app, or you can find the translations in [Weblate](https://hosted.weblate.org/projects/soundscape-android/android-app/), where you can suggest improvements to every string. Please don't accept poor translations in the app. We really do need your help to improve them.

## What's next?
There will be a few small releases which:
1. Make the app more efficient. These releases won't really affect most users.
2. Add some extra data to Contact Support emails to make it easier for us to debug user issues.

However, now that we have offline maps, the main thing we're working on is adding offline search support. In fact, there are a few pieces of interrelated work for this.

### Improve "My Location" and "Current location"
Currently, these only describe the location to the nearest street and don't give any indication of where on the street the user is. We're working to improve this by increasing the number of data sources that we use. In order of accuracy (where I live), this means:
1. Using the Android built-in geocoder when available (not all phones have it, it does require an internet connection, and there's a limit to how many times it can be used each day). This gives a good address down to the street number.
2. Using our own search server to look up the location. Again, this requires an internet connection. It can give addresses including street numbers if those are mapped in OpenStreetMap.
3. Using the information that we already have in offline map tiles to describe the location more accurately. We should be able to do nearly as good a job as when using our own search server, as we have all of the same data available.

We expect this to be the next big feature to land, and when the app starts it will automatically announce the current location. Where street numbers have not been mapped, or in countries that don't use them, we'll have descriptions based on nearby cross streets or landmarks instead.

### Improve search and add offline search
Now that we have offline map tiles, we can add offline search too. The main thing we've learned so far is that multilingual, worldwide search is hard. However, we've got the start of a plan to implement this, and though it may take a little time, we believe that as well as adding offline search, the work will give much improved online search capability.

The main idea is that the initial search that’s typed in should only be for:
* Street names (not including numbers), or
* Points of interest names or types, for example pharmacy

If a street is found, then at that point users will be presented with possible locations along the street, for example street numbers, points of interest, etc. This will work in all languages, and the user interface may start relatively simply but will grow to form a very important piece of the app.

### A better introduction to using the app
We realize that on first launch it is not immediately obvious how to use the app. To address this, we're hoping to add a guided tour to help new users understand how the app can be used. We're also looking at making it easier to use some of the features. Very few users use Routes, and we're assuming it's because they are really hard to create, though it may also be because users aren't aware of what the feature is, or that it's just not useful to many users. That brings us to…

### User feedback!
We haven't been getting as much feedback as we'd like, and we really do want more interaction. Please don't hesitate to use Contact Support to get in touch with comments or suggestions. We really do appreciate it. We may also start reaching out directly to ask specific questions about the app and your use of it.

## That's it
Hopefully this gives you a clear picture of what's happening. We should be back with another update in a few months. In the meantime, thank you for your continued support, and please keep the feedback coming!
