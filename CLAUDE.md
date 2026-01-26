# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Soundscape is an Android accessibility app that provides audio-based spatial awareness for blind and low-vision users. It uses GPS location, device orientation, and spatial audio (via FMOD engine) to describe nearby features using OpenStreetMap data.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing keys in local.properties)
./gradlew assembleRelease

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "org.scottishtecharmy.soundscape.GeoUtilsTest"

# Run a single test method
./gradlew testDebugUnitTest --tests "org.scottishtecharmy.soundscape.GeoUtilsTest.testBearing"

# Run lint
./gradlew lint

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean
```

## Local Configuration

Create `local.properties` in the root directory with tile/search provider URLs:
```
sdk.dir=/path/to/Android/Sdk
tileProviderUrl=https://PROTOMAPS_SERVER_URL
tileProviderApiKey=YOUR_API_KEY
searchProviderUrl=https://PHOTON_SERVER_URL
searchProviderApiKey=
extractProviderUrl=https://EXTRACT_SERVER_URL
```

Tests calling `tileProviderAvailable()` will be skipped if these aren't configured.

## Architecture Overview

### Core Service Layer
- **SoundscapeService** (`services/SoundscapeService.kt`): MediaSessionService running in foreground. Orchestrates location/sensor processing, audio playback, and provides StateFlows for UI.
- **LocationProvider/DirectionProvider** (`locationprovider/`): Abstract location sources. `AndroidLocationProvider` uses FusedLocationProvider; `StaticLocationProvider` enables Street Preview mode.

### GeoEngine (`geoengine/`)
The spatial analysis core that processes map data:
- Parses Mapbox Vector Tiles (MVT) into GeoJSON-like structures
- Manages a 2x2 tile grid around user location (`GridState`)
- Creates `FeatureTree` r-trees for efficient spatial queries
- Builds `Way` and `Intersection` graph for road network traversal
- `MapMatchFilter` tracks which road the user is on
- `UserGeometry` encapsulates position, heading, and motion state

### Audio System (`audio/`)
- **NativeAudioEngine**: JNI wrapper for FMOD C++ library. Creates spatial audio beacons positioned in 3D space.
- **TtsEngine**: Text-to-speech for callouts
- Beacons update geometry every ~100ms based on user position/heading

### Data Flow
```
LocationProvider → GeoEngine → FeatureTrees → Callout Generation → AudioEngine → Spatial Audio Output
                      ↓
                  StateFlows → Compose UI
```

### UI Layer (`screens/`)
- Jetpack Compose with Material3
- MVVM pattern with Hilt dependency injection
- Key screens: Home (map + controls), PlacesNearby, LocationDetails, MarkersAndRoutes, Settings

### Database (`database/`)
- Room database for markers and routes
- DataStore for preferences and onboarding state

## Key Concepts

- **Callouts**: Automatic spoken descriptions of nearby POIs, intersections, and roads
- **Audio Beacons**: Spatial audio that guides users toward a destination
- **Street Preview**: "Teleport" to a location to hear callouts without physically being there
- **Map Matching**: Tracking which road/path the user is walking on for accurate intersection callouts
- **Way/Intersection graph**: Road segments between intersections, enabling traversal and name confection for unnamed paths

## Translation Guidelines

Only modify `res/values/strings.xml` (English base). All other translations come via Weblate merges. Include descriptive comments above new strings for translator context.

To add a new language:
1. Add to `resourceConfigurations` in `app/build.gradle.kts`
2. Add to `getAllLanguages` in `LanguageScreen.kt` with native language name
3. Add to `res/xml/locales_config.xml`
