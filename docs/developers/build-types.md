---
title: Build types and analytics
layout: page
parent: Information for developers
has_toc: false
---

# Build types and analytics

The app has three build types, defined in `app/build.gradle.kts`. They differ in two things: whether code is minified/shrunk, and whether Firebase analytics + Crashlytics are wired in.

| Build type | Minified | Signing config | `DUMMY_ANALYTICS` | Source set used for analytics |
| --- | --- | --- | --- | --- |
| `debug` | no | debug | `true` | `app/src/debug/.../utils/PlatformAnalytics.kt` — returns `NoOpAnalytics` |
| `release` | yes | release | `false` | `app/src/release/.../utils/PlatformAnalytics.kt` — returns `FirebaseAnalyticsImpl`. `app/src/release/` is the only source set that compiles `FirebaseAnalyticsImpl.kt` against the Firebase SDK. |
| `releaseTest` | yes (inherits `release`) | release | `true` | `app/src/releaseTest/.../utils/PlatformAnalytics.kt` — returns `NoOpAnalytics` |

`releaseTest` is built with `initWith(getByName("release"))`, so it picks up R8/proguard, the release keystore and everything else — only the analytics implementation and the `DUMMY_ANALYTICS` flag differ. It exists so we can install a release-shaped build (signed, minified, optimised) on a developer device for debugging or pre-release testing without writing events into the production Firebase project.

## How the variant split works

The trick is plain Android source-set merging. The shared API lives in `app/src/main/java/.../utils/Analytics.kt`:

```kotlin
interface Analytics {
    fun logEvent(name: String, params: Bundle? = null)
    fun logCostlyEvent(name: String, params: Bundle? = null)
    fun crashSetCustomKey(key: String, value: String)
    fun crashLogNotes(name: String)

    companion object {
        fun getInstance(dummy: Boolean? = null, context: Context? = null): Analytics { ... }
    }
}
```

Each build type provides its own `PlatformAnalytics.kt` with a top-level `createPlatformAnalytics(context)` function. `Analytics.getInstance(dummy = false, context = ...)` calls that function — and because only one variant's source set is on the classpath for any given build, the right implementation is picked up at compile time. Firebase classes never leak into debug or `releaseTest` builds, so those variants do not need `google-services.json` or the Firebase SDK at runtime.

`NoOpAnalytics` is in `app/src/main/`, so all three variants can fall back to it; `FirebaseAnalyticsImpl` lives only in `app/src/release/`.

## Three reasons to use dummy analytics

`MainActivity.onCreate` decides at runtime whether to use the real analytics instance even on a `release` build:

```kotlin
Analytics.getInstance(
    BuildConfig.DUMMY_ANALYTICS ||
        !hasPlayServices(this) ||
        "true" == Settings.System.getString(contentResolver, "firebase.test.lab"),
    context = applicationContext,
)
```

i.e. dummy analytics if any of:

1. The build type sets `DUMMY_ANALYTICS = true` (debug, releaseTest).
2. The device has no Google Play Services.
3. The app is running in [Firebase Test Lab](https://firebase.google.com/docs/test-lab/android/android-studio#modify_instrumented_test_behavior_for) — Google's pre-launch checks run every release and would otherwise pollute the analytics with synthetic sessions.

## Runtime gating on `BuildConfig.DUMMY_ANALYTICS`

Beyond picking the analytics backend, `BuildConfig.DUMMY_ANALYTICS` is also used to suppress UI that only makes sense on real release builds:

* The new-release dialog (`Home.kt`) — only shown when `DUMMY_ANALYTICS != true`.
* The language-mismatch dialog (`Home.kt`) — same.

Add to this list cautiously: if you're tempted to gate something on `DUMMY_ANALYTICS`, ask whether the gate really wants "is this a real release build" (use `DUMMY_ANALYTICS`) or "is this a debug build" (consider `BuildConfig.DEBUG` instead — `releaseTest` is also minified and signed).

## `BuildConfig` values from `local.properties`

The same `defaultConfig` block also reads five values from `local.properties` and exposes them as `BuildConfig` strings:

```
TILE_PROVIDER_URL       TILE_PROVIDER_API_KEY
SEARCH_PROVIDER_URL     SEARCH_PROVIDER_API_KEY
EXTRACT_PROVIDER_URL
```

`local.properties` is not under version control. Each developer fills it in by hand (the format is documented in [Developer information]({% link developers/developers.md %})). On GitHub Actions, the workflow writes `local.properties` from a repo secret before invoking Gradle — see [GitHub actions]({% link developers/actions.md %}). If a value is missing the build still succeeds but those `BuildConfig` strings are empty, and the corresponding feature will fail at runtime.
