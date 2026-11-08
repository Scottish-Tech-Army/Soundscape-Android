---
title: Head tracking
layout: default
nav_exclude: true
search_exclude: true
permalink: /head-tracking/
---

# Head tracking on Android
Head tracking is an alpha test feature on Android. It works, but it's still being tested, so it's hidden unless you choose to try it.

## What head tracking does
Without head tracking, Soundscape positions its audio using the direction you're walking in, or the direction your phone is pointing when you hold it flat. With head tracking, it uses the direction your head is facing instead. Turn your head towards the audio beacon and it moves round in front of you, just as a real sound would.

On Android, Soundscape currently supports two head trackers:
* a WitMotion WT9011DCL Bluetooth motion sensor, which you attach to your head, for example to the band of your headphones or to a hat
* Bose Frames audio glasses

AirPods head tracking is only available in the iPhone app.

We're eager to support more head trackers. Some headphones have head tracking built in but don't make it available to apps, so Soundscape can't use it. These include Google Pixel Buds and Shokz OpenFit Pro.

### The WitMotion sensor
The WitMotion sensor measures 32.5 × 23.5 × 11.6 millimetres (about 1.3 × 0.9 × 0.5 inches) and weighs 9 grams. It runs on its own battery, so charge it before you use it. Soundscape tells you its battery level each time it connects.

It has a single button:
* press it to switch the sensor on
* press and hold it for 3 seconds to switch it off

You don't have to switch it off yourself: it switches off on its own 60 seconds after Soundscape stops using it, when you close Soundscape or turn head tracking off.

## How calibration works
A head tracker can feel your head turning, but it doesn't know which way is north. Soundscape works that out for itself while you walk. It compares how your head turns with the direction you're walking in, from your phone's GPS, and from that it learns which way the tracker is pointing.

This is called calibration. It needs about 10 seconds of steady walking, and you'll hear "Head tracking calibrated, your head direction now controls the audio" when it's done. Until then Soundscape uses your walking direction, as it does without a head tracker. Once calibrated it carries on checking as you walk, so small errors are corrected over time.

If you hold your phone flat in front of you, pointing the way you're facing, Soundscape can also calibrate from the phone's compass without you walking. With the phone in a pocket or bag, the compass is ignored.

The sensor can be attached any way up, as long as it can't move or slip on your head. Calibration starts again every time a head tracker connects.

## Trying it out
1. Open **Settings**, then **Debug options**, and turn on **Enable alpha test features**.
1. In **Settings**, open **Audio** and turn on **Enable head tracking**.
1. When Android asks to let Soundscape find and connect to nearby devices, choose **Allow**. Without this, Soundscape can't connect to a head tracker. You can also allow it later under **Nearby devices** in the app's Android permissions.
1. Switch your head tracker on: for a WitMotion sensor, press its button. You don't need to pair it in Android's Bluetooth settings, because Soundscape finds it and connects to it itself.
1. When it connects you'll hear, for example, "WitMotion head tracker connected, battery 80 percent". Bose Frames don't report their battery level.
1. Start walking, and after about 10 seconds you'll hear that head tracking is calibrated.

Below the **Enable head tracking** switch, Soundscape shows what the head tracker is doing: searching, connected or calibrated.

### Connecting a WitMotion sensor for the first time
* Keep your phone's screen on until you hear that it has connected. Android pauses the search for a new sensor while the screen is off. After the first connection Soundscape remembers your sensor and reconnects to it even when the phone is locked.
* Make sure the sensor isn't connected to anything else, such as WitMotion's own app, on this phone or any other. A sensor that's already connected can't be found.
* If more than one WitMotion sensor is switched on nearby, Soundscape connects to the first one it finds.
* Resetting Soundscape's settings to their defaults makes it forget your sensor, so the next connection is like the first.

## What you'll hear
* When a head tracker connects, its name and, for a WitMotion sensor, its battery level.
* When calibration is done.
* When a head tracker disconnects, a few seconds after it goes. If it reconnects within those seconds, nothing is said.
* When a WitMotion sensor's battery falls to 20 percent, and again at 10 percent.

Nothing is said when you turn head tracking off yourself.

## Things to know
* Calibration needs a good GPS signal and a walking pace, or holding your phone flat and facing 
  directly ahead.
* The calibration algorithm assumes that the direction of travel and the direction that you 
  are facing are the same. This is generally true, but if you sit on a train or a bus facing 
  sideways or backwards then the calibration will be incorrect. This is not a new problem 
  and happens without head tracking too. Once you start walking it will recalibrate.   
* If the audio seems to come from the wrong direction, walk straight for 10 to 20 seconds and it 
  will correct itself.

## Reporting problems
We'd like to hear how head tracking works for you. If something goes wrong, a recording of the walk helps us see exactly what happened:
1. In **Debug options**, turn on **Enable recording of travel** and, with the alpha test features on, **Record head tracker data**.
1. Then switch your head tracker on, so that the recording starts before it connects.
1. After the walk, choose **Share recording of travel** from the menu on the home screen and send it to us.

Recordings with head tracker data are larger than usual: with a WitMotion sensor, about 20 MB for each hour of walking.
