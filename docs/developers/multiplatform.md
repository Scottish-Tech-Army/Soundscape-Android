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

### If using Android Studio

- Build configurations should auto generate, and you can run the app on a **Simulator** / **Device** in the same way as you would an 
Android App on an **Emulator** / **Device**.
- Android Studio may notify you that the build process may be slower due to a missing guard, it may be 
effective to implement the fix suggested.