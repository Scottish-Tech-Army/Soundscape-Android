---
title: Newsletter - 19th August 2025
layout: page
nav_order: 3
has_toc: false
---

# Newsletter - 19th August 2025

This is the first of what will be an irregular series of newsletters to give an update on what we're working on.

## Initial open beta release
The first open beta release went live on 8th August and given that we didn't really announce it we've been very pleased with the initial uptake. We now have around 400 users testing the release and this has kept us very busy! There are a number of places where we gather feedback:

* Automatic reports of any crashes that the app has, and any "App Not Responding" problems.
* Our helpdesk system at <soundscapeAndroid@scottishtecharmy.support>
* Play Store feedback
* Interaction on forums

We've been working through all of these as efficiently as we can and have already made a couple of new releases that fix some of the initial reported issues. We list these in the [release notes]({% link release-notes.md %}).

The main priority has been to fix crashes and issues which were preventing some users from running the app. As many of you will know, Android phones vary a great deal, and manufacturers can install their own software for features like TextToSpeech and Talkback. Now that we have more users running the app we have a greater variety of phones and so some phone specific issues were inevitable.

## What could be better?
One issue where feedback suggests that we could do better is improving _Search_. I'll leave discussion of this until another newsletter, but we're thinking a lot about this and how we can improve it. The quality and abundance of geo data varies wildly across the world and it's important to have a solution that reflects that.

## What we're working on next
### Increased language support
A few days of effort have been spent making sure that we've got as much information on our translation site [Weblate](https://hosted.weblate.org/projects/soundscape-android/). We now have screenshots and a short video to help anyone interested in translating Soundscape to a new language has as much information as possible. There's almost certainly more work to do here, but what it means is that we can leave the translation to those with language skills and we already have one new language 60% translated. We've had offers of more, so expect to see these land as and when they are completed.

Soundscape hasn't supported any right-to-left languages yet e.g. Arabic, but once we have a translation we'll work on making sure that it works correctly.

If you are interested in helping translate Soundscape do get in touch via the support email. It's absolutely possible to just go and get started on [Weblate](https://hosted.weblate.org/projects/soundscape-android/) but if we have several offers for the same language, then we can perhaps coordinate and help divide up the work.

### Improving the user interface
There are number of smaller issues that were filed where the user interface either didn't work as expected or it wasn't quite complete. One example of this would be the ability to change the language that the app is using which is currently stuck at whatever was picked when the app was first run.

We maintain a list of all issues on our [GitHub page](https://github.com/Scottish-Tech-Army/Soundscape-Android/issues?q=is%3Aissue%20state%3Aopen%20milestone%3A1.0) though that requires a GitHub account (free) and I wouldn't necessarily recommend it to non-programmers. We will of course announce the issues as we fix them in the release notes.

### Finish Street Preview
Street Preview is largely a proof of concept right now and needs a lot more work for the audio user interface. We also think that there are some improvements that can be made over the iOS Soundscape e.g. making _Places Nearby_ work so that Markers can be created whilst previewing an area.

### Switch the underlying graphical map library
This is an internal nerdy issue and will have very little impact on users but it's an example of something that takes a considerable amount of time to do. By fixing this it removes a lot of code that is unique to us and replaces it with standard code that is used by many other apps. This reduces bugs and makes long term maintenance much simpler.

## That's it
Hopefully this gives some insight into what's been happening. We'll update you all again in a month or so, in the meantime thank you all for your support.
