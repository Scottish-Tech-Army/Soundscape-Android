# Soundscape
The [original Soundscape app](https://github.com/Scottish-Tech-Army/Soundscape) is an iOS only app that provides 3D audio navigation for the blind or visually impaired. This main aim of this project is to widen its use by creating a very similar app that runs on Android. A secondary aim is that by using modern technologies we can also generate an iOS app from the same codebase.

The app is developed using [Android Studio](https://developer.android.com/studio) with the majority of the app code written in Kotlin. For 3D audio rendering, the code uses [FMOD Studio audio engine by Firelight Technologies Pty Ltd.](https://www.fmod.com/). That library is wrapped in a C++ library with an API exposed to Kotlin. Note that for non-commerical use the [FMOD license](https://www.fmod.com/legal) allows use without payment. This project is non-commercial with an open source license and meets this requirement.

## Get started
The quickest way to get started building the app is to run Android Studio and select `File/New/Project from version control` from the menu. Select `Git` as the version control and paste in the HTTPS code link for this project from above. Click on Clone and the project will download, open and initialize ready to build.

## Current documents
* [Audio API](audio-API.md)
* [Framework choices](framework.md)
* [Pathway to product](pathway-to-product.md)
