---
title: Offline turn-by-turn routing
layout: page
parent: Information for developers
has_toc: false
---

# Offline turn-by-turn routing

Soundscape Android currently provides environmental awareness, audio beacons, and waypoint route playback. It does not provide arbitrary destination turn-by-turn navigation. The planned work adds in-app walking guidance while preserving Soundscape's existing audio model.

## Routing engine choice

GraphHopper is the first implementation target.

Reasons:

- JVM-based, so it fits Kotlin/Android proof work better than a C++ engine.
- OSM-based and Apache 2.0 licensed.
- Supports foot routing and maneuver instructions.
- Can run locally as a web server for development.
- Can later be evaluated as an embedded/offline engine.

Valhalla remains a strong later option. It has good pedestrian routing and clear route APIs, but first Android integration would require C++/NDK/JNI work or a hosted service. That raises risk before we have Soundscape's own navigation session model working.

## Data distinction

Soundscape PMTiles and GraphHopper graphs are different artifacts.

- PMTiles power MapLibre rendering and Soundscape's GeoEngine audio awareness.
- GraphHopper graph data powers route calculation.

Both can be built from the same OSM snapshot and same extract boundaries, but one cannot replace the other directly.

## Local proof setup

The scripts in `tools/routing` build a local GraphHopper proof using Monaco OSM data. Monaco is intentionally small so the first import runs quickly.

Run:

```powershell
.\tools\routing\setup-graphhopper-local.ps1
.\tools\routing\start-graphhopper-local.ps1
```

In another shell:

```powershell
.\tools\routing\query-graphhopper-local.ps1
```

The route response is saved to:

```text
tools/routing/.local/graphhopper/sample-foot-route.json
```

That sample is the seed for parser tests in Android.

## Expected GraphHopper endpoint

Android emulators reach the host machine at `10.0.2.2`, so local debug builds can set:

```properties
routingProviderUrl=http\://10.0.2.2\:8989/
```

The app's debug manifest allows cleartext HTTP for this local proof. Release builds should use HTTPS or an embedded/offline routing provider instead.

The local route endpoint uses foot routing:

```text
http://127.0.0.1:8989/route?profile=foot&point=43.7384,7.4246&point=43.7339,7.4213&locale=en&instructions=true&points_encoded=false&ch.disable=true
```

Expected response fields:

- `paths[0].distance`
- `paths[0].time`
- `paths[0].points.coordinates`
- `paths[0].instructions`

## Android integration direction

Create app-owned normalized models rather than exposing GraphHopper JSON everywhere:

- `NavigationRoute`
- `NavigationStep`
- `NavigationPoint`
- `ManeuverType`

Then implement:

- `GraphHopperRouteParser`
- `RoutingProvider`
- `NavigationSession`

`NavigationSession` should live near `SoundscapeService` and use existing systems:

- `LocationProvider.filteredLocationFlow`
- `MapMatchFilter`
- `AudioEngine` / `speakText`
- destination beacon
- MapLibre route drawing

## Device and accessibility testing

Use the Pixel 6 emulator as an older supported baseline. It can catch problems on smaller, older Pixel hardware. Do not treat it as the only target.

Before release, test at least:

- Pixel 6-class emulator or device, on a supported Android version
- current Pixel flagship-class emulator or device
- one lower-spec Android device if available
- TalkBack enabled
- Soundscape audio guidance enabled
- real GPS movement on hardware or Firebase Test Lab for sensor and location behavior

TalkBack testing checks UI labels, focus, actions, and permission flows. It does not replace Soundscape audio testing. Turn-by-turn guidance must also be checked through Soundscape's own speech output because that is the navigation channel users depend on while walking.

For local emulator testing:

```powershell
adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
adb shell settings put secure accessibility_enabled 1
```

Then open a destination and confirm that `Start Directions` is reachable and starts guidance. With TalkBack enabled, a normal coordinate tap may only move accessibility focus. Use a double tap or keyboard activation when manually testing.

To run the repeatable local emulator smoke check:

```powershell
.\tools\routing\run-turn-by-turn-local-smoke.ps1 -EnableTalkBack
```

The smoke check uses the local GraphHopper server, installs the debug APK, opens a Monaco destination, activates `Start Directions`, verifies that the route starts, moves the app off route, then verifies silent reroute behavior after the 5 second debounce.

The smoke check does not rely on emulator FusedLocation for route-test coordinates. It sends test coordinates to `SoundscapeService` through a debug-only intent guarded by `BuildConfig.DEBUG`. Release builds ignore that action.

This check is local only. Firebase Test Lab devices cannot reach the emulator host alias `10.0.2.2`, so cloud testing needs one of these later options:

- public HTTPS routing endpoint
- embedded/offline routing provider
- test build with a fake routing provider

## Offline app direction

The first app phase can call local GraphHopper JSON parsing from tests only. After that:

1. Decide whether to embed GraphHopper or run it as a local service abstraction during development.
2. Measure graph size for Monaco, then Scotland.
3. Decide download packaging alongside existing offline maps.
4. Align GraphHopper graph extracts with Soundscape PMTiles extract metadata.
