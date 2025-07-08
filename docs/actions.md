# Continuous integration actions

This document describes the initial GitHGub actions in the repo. They're written by someone new to GitHub, so improvements are always welcome. 

## GitHub secrets
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

### Tile provider secrets
These are the secrets that are used to get mapping tiles from the protomaps server.
* TILE_PROVIDER_URL - the base URL pointing at our protomaps server
* TILE_PROVIDER_API_KEY - the API key required to access the protomaps server

### Repo commit without pull request
The `run-test.yaml` action bumps the version number, committing the change back into the repo. The repo has branch protection enabled which requires a pull request for any commits. We pass in a token as described [here](https://github.com/stefanzweifel/git-auto-commit-action?tab=readme-ov-file#push-to-protected-branches) to allow the pull request to be bypassed:
* PAT_TOKEN - token generated on an admin account which allows write access to public repos.

## Actions
`run-tests.yaml` is the action which is run on each Pull Request. It runs several layers of tests:    
* Lint of the repo
* Runs unit tests
* Builds a debug release
* Runs instrumentation tests locally on an emulator

`build-app.yaml` is the action used to build a release and is manually triggered. It's steps are: 
* Bump the version code and name in `app/build.gradle.kts` and commit it back to the repo with a tag containing the version number
* Obtains the `google-service.json` and `keystore.jks` from the secrets.
* Lints the repo
* Runs unit tests
* Builds a debug build APK
* Builds a release build and signs it as an APK and AAB
* Builds the instrumentation tests into an APK
* Uploads the artefacts
* Triggers a run of Firebase instrumentation tests  

`jekyll-gh-pages.yml` is the action to build the GitHub Pages documentation site (including this page!). It's triggered whenever there is a changes submitted within the `/docs` directory. To work it needs GitHub Pages to be enabled on the repository and for them to be configured with the Source being GitHub Actions. 