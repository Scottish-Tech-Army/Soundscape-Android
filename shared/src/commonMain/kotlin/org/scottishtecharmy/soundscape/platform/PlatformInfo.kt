package org.scottishtecharmy.soundscape.platform

/**
 * Reads `versionName` from the platform's build metadata.
 * Android: BuildConfig.VERSION_NAME via reflection.
 * iOS: NSBundle's CFBundleShortVersionString.
 */
expect fun appVersionName(): String

/**
 * `appVersionName()` with the patch component stripped, e.g. "1.2.3" → "1.2".
 * Used to gate the new-release dialog.
 */
fun appVersionMinorTrimmed(): String = appVersionName().substringBeforeLast(".")

/**
 * `true` for production analytics builds. Android: !BuildConfig.DUMMY_ANALYTICS.
 * iOS: always true.
 */
expect val analyticsEnabled: Boolean

/**
 * `true` on iOS builds, `false` on Android. Used to switch help strings whose
 * content is fundamentally platform-specific (e.g. TTS setup steps).
 */
expect val isIos: Boolean

/**
 * `true` for a debug build, which is where the options that exist only for development show up.
 * Android: BuildConfig.DEBUG. iOS: a debug binary.
 */
expect val isDebugBuild: Boolean
