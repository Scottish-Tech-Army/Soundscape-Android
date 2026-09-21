---
title: GitHub actions
layout: page
parent: Information for developers
has_toc: false
---

# Continuous integration actions

This document describes the GitHGub actions in the repo. They're written by someone new to GitHub, so improvements are always welcome. 

## GitHub secrets

We have a number of GitHub secrets which are used to protect the following data:
* Keys used for creating release packages and accessing Google service. These prevent someone else from publishing a build that claimed to be our app.
* The URL and key required to get map tiles from our protomaps server. The aim here is to prevent use of our map tile server by other apps. Our server costs are covered by charities and we simply aim to minimize their costs.

More details follow on each secret here which is useful during setup.

### Release build signing
The approach to signing in `build-app.yaml` is based on [this description](https://www.droidcon.com/2023/04/04/securely-create-android-release-using-github-actions/).

There are 5 secrets required, those are what were used when creating the signing keystore (in this case done from within Android Studio with Generate Signed App Bundle or APK):
* SIGNING_STORE_PASSWORD - the key used when creating the keystore
* SIGNING_KEY_ALIAS - the key alias used
* SIGNING_KEY_PASSWORD - the key password used
* SIGNING_KEY_STORE_PATH - not really secret, always set to `keystore.jks`!
* SIGNING_KEY_STORE_BASE64 - based64 encoded `keystore.jks` which was the end result of the above 

### Google services
The `google-services.json` file is not super secret so long as the server is configured appropriately, but we protect it just in case. The approach taken is the same as for the keystore: 
* GOOGLE_SERVICES - base64 encoded `google-services.json` generated from the firebase console. This was generated on linux with `cat google-services.json | openssl enc -A -base64` which generates bas64 without line breaks.
* GOOGLE_SERVICES_PATH - not really secret, always set to `google-services.json`

There's a mock `google-services.json` file checked in to GitHub to allow builds without access to secrets to complete.
Authenticate for talking to the Firebase servers is done using [google-github-actions/auth@v2](https://github.com/google-github-actions/auth). We pass in the `credentials_json`, see [this page](https://github.com/google-github-actions/auth?tab=readme-ov-file#inputs-service-account-key-json) for details of what this is and how to format it. (To remove line breaks run `cat soundscape-android-CREDENTIALS-NAME.json | tr -d '\012\015'`).  
* GCLOUD_CREDENTIALS_JSON - the value passed in to  `credentials_json`.
* FIREBASE_PROJECT_ID - This is the Project ID from Firebase. 

#### iOS Firebase
Same pattern for iOS. The iOS app must be registered in the same Firebase project as the Android one (bundle id `org.scottishtecharmy.soundscape`) so its `GoogleService-Info.plist` can be downloaded.
* GOOGLE_SERVICE_INFO_PLIST - base64 encoded `GoogleService-Info.plist` from the Firebase console. Generated with `base64 -i GoogleService-Info.plist`.
* GOOGLE_SERVICE_INFO_PLIST_PATH - not really secret, always set to `iosApp/iosApp/GoogleService-Info.plist`.

A placeholder `GoogleService-Info.plist` is committed so PR CI (which has no access to secrets) still builds. The real plist is written over the top by the `ios-build` jobs in `build-app.yaml` and `nightly.yaml`. The `ios-firebase` (Test Lab XCTest) job and `run-tests.yaml`'s ios-test job do not need the real plist — Firebase's iOS runtime gate (`shouldEnableFirebase()` in `FirebaseAnalyticsBridge.swift`) returns false whenever XCTest is present, so no Firebase call is ever made in those jobs. Unlike Android, iOS Firebase Test Lab does not set a well-known env var; instead the gate checks for `XCTestConfigurationFilePath` in the environment and for the `XCTestCase` class in the running process.

### Tile provider secrets
These are the secrets that are used to get mapping tiles from the protomaps server.
* TILE_PROVIDER_URL - the base URL pointing at our protomaps server
* TILE_PROVIDER_API_KEY - the API key required to access the protomaps server

### Search provider secrets
These are the secrets that are used with the search server.
* SEARCH_PROVIDER_URL - the base URL pointing at our photon server (https://github.com/komoot/photon)
* SEARCH_PROVIDER_API_KEY - the API key required to access the photon server

### Repo commit without pull request
The `run-test.yaml` action bumps the version number, committing the change back into the repo. The repo has branch protection enabled which requires a pull request for any commits. We pass in a token as described [here](https://github.com/stefanzweifel/git-auto-commit-action?tab=readme-ov-file#push-to-protected-branches) to allow the pull request to be bypassed:
* PAT_TOKEN - token generated on an admin account which allows write access to public repos.

### iOS code signing
These secrets sign the iOS archive produced by the `ios-build` jobs in `nightly.yaml` and `build-app.yaml`. They are not used by `run-tests.yaml`, which builds for the simulator only and disables code signing. The CI workflow's `setup-ios-signing` composite action (in `.github/actions/setup-ios-signing/`) imports them into a temporary keychain at the start of each iOS job; the keychain is discarded with the runner.
* IOS_DIST_CERT_BASE64 - base64-encoded `.p12` containing the Apple Distribution certificate **and** its private key. Export from Keychain Access (right-click the "Apple Distribution: <Team Name>" identity → Export → `.p12`) and encode with `base64 -i Distribution.p12`.
* IOS_DIST_CERT_PASSWORD - the password chosen when exporting the `.p12` above.
* IOS_PROVISIONING_PROFILE_BASE64 - base64-encoded App Store distribution `.mobileprovision` for bundle ID `org.scottishtecharmy.soundscape`. Download from [developer.apple.com](https://developer.apple.com) → Certificates, Identifiers & Profiles → Profiles, or Xcode → Settings → Accounts → Manage Profiles. Encode with `base64 -i Soundscape_AppStore.mobileprovision`.
* IOS_SHAREEXT_PROVISIONING_PROFILE_BASE64 - base64-encoded App Store distribution `.mobileprovision` for the Share Extension target (bundle ID `org.scottishtecharmy.soundscape.share`). Encoded the same way. The Share Extension is a separate App ID with its own profile because Apple's `xcodebuild -exportArchive` signs every embedded binary individually.
* IOS_KEYCHAIN_PASSWORD - any random string. The CI workflow creates a temporary keychain to import the certificate into and locks it with this password. Generate with `openssl rand -base64 32`.

### App Store Connect API key
Used by `xcodebuild` to authenticate when `-allowProvisioningUpdates` is set (every iOS job that archives or builds for testing) and by `xcrun altool` when uploading to TestFlight. CI runners have no Apple ID logged in, so the API key is the only way `xcodebuild` can talk to Apple. Generate at [App Store Connect](https://appstoreconnect.apple.com) → Users and Access → Integrations (the page Apple labels "App Store Connect API"). Role *App Manager* is enough for TestFlight uploads; *Admin* is needed if you want `xcodebuild` to create new provisioning profiles.
* APPSTORE_CONNECT_API_KEY_ID - the 10-character identifier shown next to the key in the API Keys list.
* APPSTORE_CONNECT_API_ISSUER_ID - the UUID-style issuer ID shown at the top of the API Keys page; it is the same for every key on the team.
* APPSTORE_CONNECT_API_KEY_BASE64 - base64-encoded `.p8` private key. Apple lets you download the `.p8` only once — keep a backup. Encode with `base64 -i AuthKey_XXXXXXXXXX.p8`.

## Actions
### Run tests `run-tests.yaml`
This is the action which is run on each Pull Request. It runs several layers of tests:    
* Lint of the repo
* Runs unit tests
* Builds a debug release
* Runs instrumentation tests locally on an emulator
* In a parallel `ios-test` job on a macOS runner: compiles the Kotlin/Native iOS test sources, builds the iOS app for the simulator, and runs the XCTest suite. No iOS signing secrets are used here — the simulator build sets `CODE_SIGNING_ALLOWED=NO`.

Note that because of the way GitHub triggers `run-tests.yaml` it cannot use secrets. This affects tests which use the tile provider which have to be skipped when run in this way. This is straightforward to do - see callers of `tileProviderAvailable()` in the test code. The iOS job behaves the same way: the committed (gitignored) `iosApp/Local.xcconfig` is rewritten with the runtime secrets when available, and tests that need provider URLs check at runtime.

### Tag and build release `build-app.yaml`
This is the action used to build a release and is manually triggered from the GitHub GUI. It's steps are: 
* Bump the version code and name in `app/build.gradle.kts` and commit it back to the repo with a tag containing the version number
* Obtains the `google-service.json` and `keystore.jks` from the secrets.
* Lints the repo
* Runs unit tests
* Builds a debug build APK
* Builds a release build and signs it as an APK and AAB
* Builds the instrumentation tests into an APK
* Uploads the artifacts
* Triggers a run of Firebase instrumentation tests
* Triggers a run of Firebase robo tests
* In parallel iOS jobs: archives a signed `.ipa` (reusing the version bumped on the Android side via `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`), uploads it to TestFlight via `xcrun altool`, and runs the XCTest suite on real devices via Firebase Test Lab. iOS Robo testing is not offered by Test Lab, so the iOS Firebase job runs XCTest instead.

### Deploy docs `jekyll-gh-pages.yml`
This is the action to build the GitHub Pages documentation site (including this page!). It's triggered whenever there is a changes submitted within the `/docs` directory. To work it needs GitHub Pages to be enabled on the repository and for them to be configured with the Source being GitHub Actions.

### Nightly build and test `nightly.yaml`
This is similar to the code that tags and builds a release, the main difference being is that it doesn't bump the version number and it only builds a debug version of the app. It does run the instrumentation tests locally followed by the Firebase tests on the release. A parallel `ios-build` job archives and exports a signed `.ipa` artifact so iOS signing breakage is detected daily; nothing is uploaded anywhere.