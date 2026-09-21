#!/usr/bin/env bash
#
# Vendors the Oboe prebuilt libraries and headers into the repo.
#
# Oboe is consumed as vendored prebuilts rather than as a Gradle dependency
# because the Gradle route requires `buildFeatures.prefab = true`, and AGP then
# validates the prefab metadata of *every* AAR on the module's classpath. MapLibre
# 13.4.0+ ships a prefab package declaring stl=c++_static, which is rejected against
# this app's c++_shared build with CXX1211 - even though we never link against it.
# Keeping prefab off decouples the native build from other dependencies entirely.
#
# The version is read from the `oboe` key in gradle/libs.versions.toml, which stays
# the single source of truth. To update Oboe: bump that key, run this script, build,
# test the audio engine, and commit the result.
#
# Usage:
#   scripts/update-oboe.sh            re-vendor the pinned version in place
#   scripts/update-oboe.sh --check    verify the committed files match the pinned
#                                     version, without writing anything (for CI)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CATALOG="$REPO_ROOT/gradle/libs.versions.toml"
JNI_LIBS="$REPO_ROOT/app/src/main/jniLibs"
INCLUDE_DIR="$REPO_ROOT/app/src/main/cpp/oboe/include"
BASE_URL="https://dl.google.com/dl/android/maven2/com/google/oboe/oboe"
ABIS=(arm64-v8a armeabi-v7a x86 x86_64)

mode="vendor"
if [[ "${1:-}" == "--check" ]]; then
  mode="check"
elif [[ $# -gt 0 ]]; then
  echo "error: unknown argument '$1' (expected --check or no arguments)" >&2
  exit 2
fi

version="$(sed -n 's/^oboe = "\([^"]*\)".*/\1/p' "$CATALOG")"
if [[ -z "$version" ]]; then
  echo "error: could not read the 'oboe' version key from $CATALOG" >&2
  exit 1
fi
echo "Oboe version pinned in the catalogue: $version"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

aar="$work/oboe-$version.aar"
url="$BASE_URL/$version/oboe-$version.aar"
echo "Downloading $url"
if ! curl -fsSL "$url" -o "$aar"; then
  echo "error: could not download the Oboe AAR for version $version." >&2
  echo "       Oboe is published to Google's Maven repo, not Maven Central." >&2
  echo "       Check that $version exists at $BASE_URL/" >&2
  exit 1
fi

unzip -q "$aar" -d "$work/aar"
src_libs="$work/aar/prefab/modules/oboe/libs"
src_include="$work/aar/prefab/modules/oboe/include"
for required in "$src_libs" "$src_include/oboe"; do
  if [[ ! -d "$required" ]]; then
    echo "error: the AAR has an unexpected layout - $required is missing." >&2
    echo "       Oboe may have changed how it packages prebuilts; update this script." >&2
    exit 1
  fi
done

failed=0

sync_file() {
  local src="$1" dest="$2"
  if [[ "$mode" == "check" ]]; then
    if [[ ! -f "$dest" ]]; then
      echo "  MISSING  ${dest#"$REPO_ROOT"/}"
      failed=1
    elif ! cmp -s "$src" "$dest"; then
      echo "  DIFFERS  ${dest#"$REPO_ROOT"/}"
      failed=1
    fi
  else
    mkdir -p "$(dirname "$dest")"
    cp "$src" "$dest"
    echo "  ${dest#"$REPO_ROOT"/}"
  fi
}

echo
echo "Native libraries:"
for abi in "${ABIS[@]}"; do
  src="$src_libs/android.$abi/liboboe.so"
  if [[ ! -f "$src" ]]; then
    echo "error: the AAR does not contain a prebuilt for ABI $abi." >&2
    exit 1
  fi
  sync_file "$src" "$JNI_LIBS/$abi/liboboe.so"
done

echo
echo "Headers:"
# Remove headers that Oboe has dropped, so a stale one can't keep compiling.
if [[ "$mode" == "vendor" && -d "$INCLUDE_DIR/oboe" ]]; then
  rm -rf "$INCLUDE_DIR/oboe"
fi
while IFS= read -r -d '' src; do
  sync_file "$src" "$INCLUDE_DIR/oboe/$(basename "$src")"
done < <(find "$src_include/oboe" -maxdepth 1 -type f -name '*.h' -print0 | sort -z)

if [[ "$mode" == "check" ]]; then
  echo
  if [[ "$failed" -ne 0 ]]; then
    echo "FAILED: the vendored Oboe files do not match version $version."
    echo "Run scripts/update-oboe.sh and commit the result."
    exit 1
  fi
  echo "OK: vendored Oboe files match version $version."
else
  echo
  echo "Done. Build and exercise the audio engine before committing."
fi
