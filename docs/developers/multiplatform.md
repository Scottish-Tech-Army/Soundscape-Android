# Multiplatform
---
## Install Homebrew
https://brew.sh/

## Install `xcodegen` from Homebrew
```bash
brew install xcodegen
```

## Create a `Local.xcconfig` file

In the directory `{project root}/iosApp/` create a file named `Local.xcconfig` and populate it with 
the following, removing the placeholders `<>` and the contents of the placeholders:

_Note: Xcode treats `//` within a `.xcconfig` file as a comment, so in order to escape this, add `$()`
between // . For example: `https:/$()/example.com`_

```bash
DEVELOPMENT_TEAM = <APPLE_DEVELOPMENT_TEAM_ID>
TILE_PROVIDER_URL = https:/$()/tiles.example.com/
SEARCH_PROVIDER_URL = https:/$()/search.example.com/
EXTRACT_PROVIDER_URL = https:/$()/extracts.example.com/
```
You can find your `DEVELOPMENT_TEAM_ID` at https://developer.apple.com/

## Firebase (optional for local dev)

Firebase Analytics and Crashlytics are wired up on iOS but skipped on Debug builds and
whenever XCTest is running, so the placeholder `iosApp/iosApp/GoogleService-Info.plist`
checked into the repo is enough for local development and PR CI. If you need to exercise
the Firebase path, download the real plist for the `org.scottishtecharmy.soundscape`
iOS app from the Firebase console and drop it in over the placeholder, then archive
Release. See [Build types and analytics]({% link developers/build-types.md %}) for the
gating detail.

### If using Android Studio

- Build configurations should auto generate, and you can run the app on a **Simulator** / **Device** in the same way as you would an 
Android App on an **Emulator** / **Device**.
- Android Studio may notify you that the build process may be slower due to a missing guard, it may be 
effective to implement the fix suggested.