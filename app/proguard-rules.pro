# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# For debugging, uncomment the next two lines
#-keepattributes SourceFile,LineNumberTable
#-dontobfuscate

# Preserve protobuf generated classes (for MVT vector_tiles)
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Fix SimpleRouteData GSON generation and parsing
-keep class org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.*  { *; }
-keep class com.google.gson.internal.LinkedTreeMap  { *; }

# Ensure that we can callback from the AudioEngine
-keep class org.scottishtecharmy.soundscape.audio.NativeAudioEngine {
    public void onAllBeaconsCleared();
}

# The :shared module reads VERSION_NAME, DUMMY_ANALYTICS and TILE_PROVIDER_URL
# from this class via Class.forName(...).getField(...) (see PlatformInfo.android.kt
# and PlatformMapContainer.kt). Without this rule R8 renames BuildConfig in
# release builds, the reflective lookup fails, and the drawer shows "0.0.0"
# while the map renders with an empty tile URL.
-keep class org.scottishtecharmy.soundscape.BuildConfig { *; }