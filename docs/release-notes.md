---
title: Release notes
layout: page
nav_order: 5
has_toc: false
---

# Release notes

Soundscape 2.0 is a big release and is currently in closed Beta. The headline change is that
Soundscape now has something useful to say when you're travelling by car, bus or train, rather than
only when you're on foot. There's also a great deal of smaller work on how places are described,
twenty new languages, and a long list of fixes.

Notes for older versions are on the [Release notes for 1.x]({% link v1.0-release-notes.md %}) page.

## What's new in 2.0

* **Callouts while travelling by car, bus or train.** Soundscape recognises when you're moving at
  speed and describes your journey instead of your immediate surroundings.
* **Told when you cross water and railways.** Rivers, canals, firths and railway lines are called
  out as you cross them, whether you're walking or travelling.
* **Better addresses and place names.** Places that have no address of their own now get the street
  and area they're in, house numbers are matched to the correct side of the street, and bus stops
  in Great Britain use their official names.
* **Twenty new languages**, bringing the total to 46. The documentation website is translated too.
* **Wake on leave.** Sleep mode can now wake Soundscape up again when you leave the place where you
  put it to sleep.
* **Shorter, more natural distances**, using larger units when you're moving quickly.
* **A quicker way out.** *Exit Soundscape* is now at the top of the main menu.
* **Offline map improvements**, including updating a downloaded map in place and a map of the
  available regions on this website.
* **A lot of accessibility work** on TalkBack, particularly around the onboarding screens.
* **A great many crash and stability fixes.**

Two things have been **removed** in 2.0: the voice control feature, and the language menu inside the
app. See [Things that have been removed](#things-that-have-been-removed) below for what to do
instead.

---

## In more detail

### Travelling by car, bus or train

This is the largest new feature for existing users. Previously Soundscape had very little to say
once you got into a vehicle - it carried on describing your immediate surroundings, which at speed
meant a stream of things you had already gone past.

Soundscape now notices when you're travelling faster than walking pace and changes what it tells
you. There's nothing to switch on, and it goes back to normal by itself once you slow down or get
out and walk.

While you're travelling you'll hear:

* **Where you are**, every so often - the road you're on and the direction you're going, for
  example "Travelling north along M8". Roads with a number are announced by their number, and
  Soundscape won't keep re-announcing the same road each time its street name changes.
* **Towns and villages** you're heading towards, with the distance, as well as ones you're moving
  away from or simply passing.
* **Motorway junctions and exits** as you reach them.
* **Large landmarks** as you pass them, such as parks, hospitals, stadiums and shopping centres.
* **Bus, tram and train stops** as you pass them. Soundscape only mentions the stops on your side
  of the road, since the ones on the far side serve the opposite direction.
* **Rivers, canals and railways you cross.**
* **Tunnels**, which mainly explains why Soundscape is about to go quiet - there's no GPS signal
  inside one.

On a **train**, Soundscape works out that you're on a railway rather than a road, and tells you
which towns you're passing and how far you've come since the last station. Working this out is
harder than it sounds, because motorways and railway lines are often built alongside each other for
miles at a time, so a good deal of the work in this release went into not mistaking one for the
other.

The ordinary walking callouts - nearby shops, road crossings and so on - are deliberately held back
while you're travelling, and the distances at which things are announced are stretched a long way so
that you hear about something before you've passed it.

### Crossing water and railways

Soundscape now tells you when you cross a river, canal, firth, bay or railway line. This works when
you're walking as well as when you're travelling, and it covers going underneath as well as over the
top, so a footbridge and an underpass are both described.

### Better addresses and place names

A lot of work has gone into Soundscape describing places the way a person would:

* Places with no address of their own are now described by the street and area they're in, rather
  than being left vague.
* House numbers are matched to the correct side of the street. Previously an address could be
  reported from the opposite pavement.
* A place's address no longer repeats the place's own name back to you.
* Bus stops in Great Britain use their official public transport names, which are usually the ones
  on the timetable and the sign at the stop.
* Unnamed footpaths that run along a river or canal are now named after the water they follow.
* Paths and roads with no name are described more sensibly, and the words used for them are
  properly translated rather than appearing in English.

### Languages

Twenty new languages have been added in 2.0: Arabic, Bengali, Bulgarian, Catalan, Croatian, Czech,
Hausa, Hungarian, Indonesian, Korean, Marathi, Serbian, Slovak, Slovenian, Swahili, Tamil, Telugu,
Thai, Urdu and Vietnamese. These languages are all in alpha, and we're keen to get feedback on
their accuracy. In total Soundscape is now available in 46 languages, and this documentation
website has been translated too.

Egyptian Arabic has been folded into Arabic, and Luganda has been withdrawn, as neither had enough
translated text to be useful.

Translations are community work and we'd welcome your help with them, or corrections where something
reads badly. Any string can be improved at
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Sleep mode

Sleep mode has gained **wake on leave**. When you put Soundscape to sleep you can ask it to wake up
again once you leave the area, which is useful when you arrive somewhere and want it quiet until
you next set off.

### Distances and speech

Spoken distances have been shortened and made more natural, and Soundscape now switches to larger
units when you're moving quickly - miles or kilometres rather than a long count of feet or metres.
Each language decides for itself how to say a fractional distance, which had been forced into an
English-shaped pattern before.

### Offline maps

Offline maps arrived in 1.0 and have been steadily improved:

* A downloaded map can now be updated in place when a newer version is available, from the extract's
  details screen.
* Maps that can't be used - a download that was corrupted, for instance - are now clearly marked
  rather than silently failing.
* Downloads are more reliable, and the screen shows what's happening while the list of available
  maps is being fetched instead of a full-screen spinner.
* A finished download only appears as finished once it's genuinely ready to use.
* There's a [map of the available regions]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  on this website.

### Accessibility

A great deal of work has gone into screen reader behaviour, especially in the onboarding screens
where focus used to jump to the wrong place. Other improvements include better reading of file sizes
and decimal numbers, correct "double tap to..." hints in languages that put the verb last, and
sensible hints where none had been set at all.

### Menus and navigation

* **Exit Soundscape** is now the first item in the main menu, rather than being somewhere further
  down.
* The main menu no longer has a strip of the screen showing down one side, which had given screen
  reader users a confusing extra area to tap.
* The system back gesture no longer skips a level when you're browsing categories in Places Nearby.
* The *Audio Tutorial* has been renamed the **Guided Tutorial**.
* Settings has been tidied, and *Reset to defaults* now properly clears everything.

### Stability

2.0 includes a long list of crash and freeze fixes, among them the app freezing on the splash
screen, freezes when resetting settings, crashes when a downloaded map was damaged, crashes on
opening route details from the home screen, crashes when changing language, and several problems
reported automatically through the Play Store. Battery and start-up behaviour have also been made
more robust on phones that aggressively shut background apps down.

### Things that have been removed
{: #things-that-have-been-removed }

* **Voice control** has been removed. It never worked reliably enough to be worth keeping, and the
  media control buttons on headphones cover most of the same ground - see
  [Help using Media Controls]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **The language menu inside the app** has gone. Soundscape now follows the language you've set for
  your phone, which is what most people expected it to do. To change it, change your phone's
  language, or set a per-app language in your phone's settings if it offers that.

## Telling us about problems

If something isn't right, we'd like to hear about it. Email the Help Desk at
<soundscapeAndroid@scottishtecharmy.support>, or ask on Slack if you're an STA member.

If a callout was wrong or didn't happen, a recording of your journey helps us enormously - we can
replay it and see exactly what Soundscape was working from. There are instructions for that under
[Providing a debug location trace]({% link testing/test-instructions.md %}#providing-a-debug-location-trace).

## A note on iPhone

Everything above is about the Android app, but it's worth knowing where the rest of the work in this
release went. Soundscape now runs on iPhone too, and both apps are built from the same shared code -
the same screens, the same wording, and the same callouts, so a new feature like the travel callouts
above arrives on both at once instead of being written twice. That shared foundation is why 2.0 took
as long as it did, and it's what should make future releases arrive more quickly on both. The iPhone
app is currently available through TestFlight by invitation: ask on Slack if you're an STA member,
or email the Help Desk.
