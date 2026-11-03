#!/usr/bin/env bash
# Replays the head tracker input in a travel recording (a .gpx shared from the app with "Record
# head tracker data" on) and writes <name>-headtracker.csv beside it: each tracker's status, yaw,
# calibration offset and heading at every sample.
#
#   scripts/replay-head-tracker.sh travel.gpx
set -euo pipefail
if [[ $# -ne 1 || ! -f "$1" ]]; then
    echo "usage: $0 recording.gpx" >&2
    exit 1
fi
gpx="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
cd "$(dirname "$0")/.."
HEAD_TRACKER_GPX="$gpx" ./gradlew :app:testDebugUnitTest \
    --tests 'org.scottishtecharmy.soundscape.HeadTrackerReplayTool' --rerun -i \
    | grep -E "(Replayed [0-9]+ samples|The recording starts|[0-9]+ gap\(s\)|Heading matched)" | sed "s/^ *//" || true
