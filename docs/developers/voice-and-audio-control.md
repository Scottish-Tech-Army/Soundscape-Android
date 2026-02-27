---
title: App remote control
layout: page
parent: Information for developers
has_toc: false
---
# App remote control 
As well as the accessible GUI to control the app, we also have 3 other options which can control the app even when the screen is locked:

1. Hard coded media controls as per the iOS app
2. Voice control
3. An audio menu navigated via media controls

The first of these was implemented in an early release, but the second two are new.

## Voice control
Most voice commands will be relatively fixed and discoverable simply by asking. The expected design is that the user will tap the play/pause button on their headphones and that will prompt the user that it's listening for a command. The user speaks the command and after a short period of silence the listening will stop and the text is sent to the app to process.

It's possible to have a number of phrases that have the same action, though it's not clear whether that really makes sense. If you can use "My location" to trigger that feature, then having "Where am I" as well might just be confusing? In general sticking to the same text as is used in the GUI makes the most sense. To do that, we should actually use the same string resources so that the translations remain consistent. 

Because we want the functionality of voice control and audio menus to be similar, I've added in the text alongside the audio menu options in the next section.

## Audio menus
Each menu consists of a number of options to pick from. The main menu is simply a list of these sub menus. Each sub menu has an option which allows navigating back up to the main menu. That option obviously doesn't exist as a voice control command where all commands are in a flat structure.

### Existing features
#### My Surroundings
Selecting an option simply triggers in the same way as tapping the UI button.
```
My Surroundings
├── "My Location"
├── "Around Me"
├── "Ahead of Me"
├── "Nearby Markers"
└── Main Menu
```

#### Route control
These are the actions that are hard coded in iOS when a route is playing back. All button presses must have obvious audio feedback even if it's just to say "You're already at the first waypoint".
```
Route
├── "Next Waypoint"
├── "Prev Waypoint"
├── "Mute Beacon"
├── "Stop Route"
└── Main Menu
```

#### Start route
This is the first of our dynamically generated menus. It contains every route that has been set up. The routes should be sorted alphabetically.
```
Start Route
├── [route 1]
├── [route 2]
├── ...
├── ...
└── Main Menu
```

Two voice commands cover this:
1. "List routes"
2. "Start route [route name]"

This means that the user can discover the available routes and then start playing one back.

#### Start beacon
This is the same as start route, but for Markers. Many users don't create routes and so have fewer markers to scroll through. The markers should be sorted alphabetically.
```
Start Beacon
├── [marker 1]
├── [marker 2]
├── ...
├── ...
└── Main Menu
```

Again, two voice commands cover this:
1. "List markers"
2. "Start beacon at [marker name]"

Perhaps we could add filters to "List markers" e.g. "List markers nearby" or "List markers from F" where it start alphabetically.

### New features
One of the main reasons for having an extensible menu system is so that we can add in new remotely controlled features. We need to decide how we expose these within the on screen GUI too - in many ways it's a lot easier not to!

#### Audio profile
This is a new feature which would allow the user to pick a filter for points of interest. I think the implementation of the filters is the crucial thing here. Rather than ONLY playing out eating POI, perhaps it guarantees eating POI, but will play some other POI to aid navigation e.g. at least one POI per 100m if there have been no intersections? 
```
Audio Profile
├── "Eating"
├── "Shopping"
├── "Banking"
├── "Transport"
├── "Navigating"
├── ...
├── ...
└── Main Menu
```

#### Route creation
The UI for creating routes is hard to use, and the idea here is to allow users to create routes as they walk.
```
Route creation
├── "Start creating route"
├── "Add waypoint"
└── Main Menu
```

Voice control does have some major advantages as we can allow the user to specify names too. For example:

* "Start creating route 'To Tesco'"
* "Add waypoint 'Corner of Moor Road'"

If the name is empty, we can either guess a good marker name or fall back to using "Waypoint X".

#### Callback filtering
This sort of goes along with audio profiles, but it simply a way of getting the app to quieten down when there's too much to describe. Perhaps this should just be an audio profile e.g. "Quieter"?
```
Callback filtering
├── Mute callbacks
├── Reduce callbacks
├── Increase callbacks
└── Main Menu
```
