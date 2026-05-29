# Android XR Implementation for Soundscape

This document outlines the steps taken to implement Android XR support in the Soundscape app, focusing on AI glasses as a companion device for visually impaired users.

## 1. Project Configuration

### Dependencies
Added the Jetpack XR SDK libraries to `gradle/libs.versions.toml`:
- `androidx.xr.runtime:runtime`
- `androidx.xr.scenecore:scenecore`
- `androidx.xr.compose:compose`
- `androidx.xr.glimmer:glimmer` (UI toolkit for AI glasses)
- `androidx.xr.projected:projected` (API for projecting activities to glasses)
- `androidx.xr.arcore:arcore`

Integrated these into `app/build.gradle.kts`.

### Manifest
Registered `GlassesMainActivity` in `AndroidManifest.xml` with the required attribute for projection:
```xml
<activity
    android:name=".xr.GlassesMainActivity"
    android:exported="true"
    android:requiredDisplayCategory="xr_projected"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
    </intent-filter>
</activity>
```

## 2. Companion Activity (`GlassesMainActivity`)

Created a dedicated activity for the glasses experience using **Jetpack Compose Glimmer**.
- **UI**: Uses `GlimmerTheme` and components like `Card`, `Text`, and `Button` optimized for the transparent display of AI glasses.
- **Session Management**: Initializes an XR `Session` and passes it to the `SoundscapeService` to enable spatial audio routing.
- **Projection**: Started from the phone using `ProjectedContext.createProjectedActivityOptions` to target the connected glasses display.

## 3. Spatial Audio Integration

The core of the XR feature is routing Soundscape's audio through the glasses using 3D spatialization.

### Audio Engine Abstraction
The `AudioEngine` interface was refactored to support multiple implementations, allowing the app to switch between the standard phone audio (Oboe-based) and XR spatial audio.

### XR Audio Engine (`XrAudioEngine`)
Created a new implementation using `androidx.xr.scenecore`:
- **Entity Positioning**: Uses a virtual `PanelEntity` within the XR `ActivitySpace` to act as the sound source.
- **Coordinate Transformation**: Implemented logic to convert geographic coordinates (Latitude/Longitude) relative to the user into XR vector coordinates (`Vector3`). 
- **TTS Routing**: Text-to-speech is synthesized through the glasses' context, appearing to originate from the physical location of the marker or beacon.

## 4. Service Orchestration

Modified `SoundscapeService` to manage the transition between audio engines:
- Added `setXrSession(session)` to handle the arrival/departure of an XR session.
- **Dynamic Swapping**: When a session is active, the service destroys the native engine and initializes the `XrAudioEngine`.
- **Cleanup**: Reverts to `NativeAudioEngine` when the glasses activity is closed or disconnected.

## 5. UI Entry Point

Added a **Start XR** button to the `HomeTopAppBar` in the main mobile UI. 
- Uses the `SoundscapeXR` utility object to check for device compatibility (Android 15+) and launch the projected activity.

## 6. Testing

Development was performed with the **Android XR Emulator** (AI Glasses profile). 
- To test: Create a virtual "AI Glasses" device in Android Studio's AVD Manager.
- Launch the app and tap the AR icon in the top bar to project the companion UI and activate spatial audio.
