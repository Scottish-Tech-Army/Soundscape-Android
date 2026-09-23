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

## Original media controls
These started as the iOS app's bindings, and apply whenever the audio menu isn't selected:

* **Play/Pause** toggles the beacon audio on and off.
* **Next** moves the beacon to the next waypoint while a route is playing, and otherwise calls out *Around Me*.
* **Previous** moves the beacon to the previous waypoint while a route is playing, and otherwise steps the *Callout Detail* setting down one level: Detailed, Balanced, Quiet, Silent, and round to Detailed again. It speaks only the level it reached.

*My Location* is not on the buttons; it's a home screen button and a Siri or Gemini command.

Stepping the Callout Detail is on the same button in both modes, so that the press worth finding in a hurry doesn't depend on which mode is chosen. Automatic callouts are held off while the new level is spoken, so nothing talks over it. The implementation is the `MediaControllableService.cycleCalloutDetailAndSay()` extension in `CalloutDetailCycle.kt`, called from both `OriginalMediaControls` and `AudioMenuMediaControls`.

## Voice control

*Voice control has been disabled for now. It was too unreliable in its operation, which was simply confusing to users.*

Most voice commands will be relatively fixed and discoverable simply by asking. The expected design is that the user will tap the play/pause button on their headphones and that will prompt the user that it's listening for a command. The user speaks the command and after a short period of silence the listening will stop and the text is sent to the app to process.

It's possible to have a number of phrases that have the same action, though it's not clear whether that really makes sense. If you can use "My location" to trigger that feature, then having "Where am I" as well might just be confusing? In general sticking to the same text as is used in the GUI makes the most sense. To do that, we should actually use the same string resources so that the translations remain consistent. 

Because we want the functionality of voice control and audio menus to be similar, I've added in the text alongside the audio menu options in the next section.

## Audio menus
**Next** moves through the options at the current level, wrapping round after the last one, and **Play/Pause** selects the one just spoken. **Previous** does not step backwards through the menu: it changes the Callout Detail exactly as in the original mode (see above).

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
*This now exists as the **Places to Call Out** setting in Manage Callouts: a list to tick of Everything, Landmarks, Public Transit, Food and Drink, Groceries, Banks, or No Places. The narrowing choices are the same groups as the Places Nearby filters. It is only in Settings, not in the audio menu. The logic is `PlacesToCallOut` and `CalloutPoiSelection` in `CalloutVerbosity.kt`. The rest of this section is the original proposal.*

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
*This now exists as the **Callout Detail** setting: Silent, Quiet, Balanced or Detailed. Rather than a menu, it's on the Previous button in both media control modes, a slider in Settings, and a Siri or Gemini command ("Soundscape detail quiet" on iOS, `setCalloutDetail` in the Android AppFunctions). Silent replaced the old Allow Callouts switch. The levels are bundles of thresholds in `CalloutVerbosity.kt`. The rest of this section is the original proposal.*

This sort of goes along with audio profiles, but it simply a way of getting the app to quieten down when there's too much to describe. Perhaps this should just be an audio profile e.g. "Quieter"?
```
Callback filtering
├── Mute callbacks
├── Reduce callbacks
├── Increase callbacks
└── Main Menu
```

## How Bluetooth headphones work (or don't)
One of the main issues that came to light whilst developing Voice control pertains to how Bluetooth headphones and headsets work. For the vast majority of these, they can either be playing high quality stereo audio OR you can use the microphone, you can't do both. This is because microphone input is only supported in handset profile and that only supports mono audio at a low sample rate. Gamers have known this for a long time, and gaming headphones are usually either wired or support other wireless protocols in addition to Bluetooth. Alternatively, gamers may use a separate wired microphone for Discord and Bluetooth headphones for in game audio. The low microphone quality is why it's almost impossible to buy a standalone Bluetooth microphone. Instead, wireless microphones normally attach to phones with a USB dongle.

These constraints are why "Hey Google" hasn't worked from Bluetooth headphones. You can't listen to high quality audio from Spotify and have the microphone listening for the "wake words". The built in microphones on the phone don't have these limitations as they are not Bluetooth. The industry solution to this which is rolling out in newer hardware is [Bluetooth low energy audio](https://www.bluetooth.com/learn-about-bluetooth/feature-enhancements/le-audio/). If a phone and headphones are using this, then no mode switching is required to use the microphone. This means that voice assistants can work more seamlessly. However, this does need a modern phone (Pixel 8 or newer on Google phones, and Android 12 or later is required) and headphones that support it. Although many of the headphones are expensive I just bought a pair of [Creative Zen Air Plus](https://uk.creative.com/p/audio-enthusiasts/creative-zen-air-plus) for £30 for testing. With these plus a Pixel 8 using voice control will be a seamless experience where Soundscape can play out tones and speech and listen to the microphone for input without too much effort.

What about older headphones and headsets? When the media control button is pressed to start listening for a voice command, Soundscape has to switch the Bluetooth audio to the mode that supports microphone input. This can take a second or so, and many headphones will make various beeps and chirps as they do it. Once done, the user can speak and the speech will be recognized. On completion, Soundscape switches back to stereo audio mode and continues. Again, some headphones will give feedback when this happens which is not ideal as it's not usually clear where each sound is coming from. As a result, we've added an option for Voice Control to disable prompts generated by Soundscape. If your headphones are already making plenty of noise, then Soundscape doing it too just makes it worse. We've also added an option to selecting which microphone to use. This means that the user can select the Bluetooth headset microphone, the built in microphone, or they can plug in another microphone and use that. The latter two of these will give a more seamless performance as there's no mode switching taking place.

### Test scenarios
Each of these with Android 11 and Android 12 or later:
1. Older Bluetooth headset that requires SCO
2. Bluetooth LE headset
3. Separate external microphone (unlikely, but it's a possible configuration)
4. Built in phone microphone

2 should be the best option and 1 the most likely/convenient.