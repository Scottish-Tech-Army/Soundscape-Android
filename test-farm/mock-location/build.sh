#!/bin/bash
# Builds the test farm's mock location helper (build/farm-location.apk) straight
# from the SDK tools: one Java class, no resources, no Gradle. Signed with the
# local debug key; farm.py installs it on each Android phone.
set -euo pipefail

cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BT="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"
JAR="$(ls -d "$SDK"/platforms/android-[0-9]* | sort -V | tail -1)/android.jar"
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/dex"

javac -nowarn --release 11 -classpath "$JAR" -d "$OUT/classes" \
    $(find src -name '*.java')
"$BT/d8" --min-api 26 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$JAR" --manifest AndroidManifest.xml
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --out "$OUT/farm-location.apk" "$OUT/aligned.apk"
echo "built $OUT/farm-location.apk"
