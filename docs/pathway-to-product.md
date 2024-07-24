# Pathway to product

The aim of this document is to take some of the modules that Adam has identified and to put them together into staged product development.


## Stage 1 - Simple Android test app
The aim of this would be to allow some initial testing of the OpenStreetMap parsing and some audio. The app would simply "Describe what is around me" every 30 seconds or so. It requires:
* The OpenStreetMap work that Adam has done
* The text-to-speech and audio positioning work completed (currently POC only)
* GPS and compass (including Kalmann filter)
* The code landed in STA github

It wouldn't need any UI work, but would be the start of the Soundscape Android app.

To enable wider testing we'd then need to be able to build the app for distribution which means that we'd need these which may be easy for someone else to do:
* Android deployment
* Android CI/CD within github

For the easiest testing we'd also need:
* Ability to run Soundscape app in background when phone is locked

With these in place, anyone with access to the app would be able to wander/drive the streets with the app running and see how it behaves

Missing features that could then be added, in rough order would be:

* On screen Open Streem Map map
* UI
* Routes of waypoints - import from GPX initially
* Audio Beacons 
* Offline mode (?)
