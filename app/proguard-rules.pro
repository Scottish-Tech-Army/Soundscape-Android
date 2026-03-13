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