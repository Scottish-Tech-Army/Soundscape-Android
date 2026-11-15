---
name: merge-gpx
description: Merge a series of overlapping Soundscape GPX recordings (e.g. pixel-a/b/c/d.gpx, ios-a..e.gpx in ~/Downloads) into one long GPX per device. Use when asked to merge, join, stitch or combine GPX files/recordings into a single track.
---

# Merge overlapping GPX recordings

The app's recorder exports a rolling window of the last 3600 fixes, so exporting several times
during one long walk gives a series of files that overlap. `scripts/merge_gpx.py` stitches one
series back into a single track:

- files are ordered by their first fix (so filename order and typos don't matter);
- each file adds only the fixes later than everything already merged;
- each `<trkpt>` is copied unchanged, `<extensions>` included (Android `<steps>`, iOS
  `<headTracking>`), and the header (`recorderVersion`, `locationStream`) comes from the
  earliest file, so the result still works with GPX replay;
- fixes keep their order within a file, duplicates included (iOS writes some fixes twice, the
  second time with headTracking). Don't dedupe them.

## Steps

1. **Find the series.** `ls -la --time-style=full-iso ~/Downloads/*.gpx`, then pick the files
   exported together, one series per device. Check the first and last `<time>` of each file and
   that it has a single `<trkseg>`. Watch for misnamed files (an `ioc-c.gpx` belonged to the
   `ios-*` series). Never mix devices in one merge. If it isn't clear which files belong
   together, ask.
2. **Merge each series** (the files are downloads, so run with `-I`):
   ```
   python3 -I .claude/skills/merge-gpx/scripts/merge_gpx.py -o ~/Downloads/<device>-merged.gpx <files...>
   ```
   Before writing an output file, look at what's already at that path.
3. **Read the stderr report.** For each file it prints the time range, the number of fixes and
   how many were new. Report these warnings to the user, don't hide them:
   - `GAP before X`: the exports don't overlap, so fixes are missing between those files;
   - `overlapping fixes don't match`: the files aren't from the same recording, or the same
     device. Stop and check which files were picked;
   - fixes going backwards in time within a file.
   A file that adds 0 or only a few new fixes is normal when its window sits inside its
   neighbours' windows.
4. **Check the result:** parse it with `xml.etree.ElementTree` and list the largest time gaps
   between consecutive fixes. A gap inside one source file is in the original recording (no
   GPS), not a merge error. Say which kind each gap is.

Report the output paths, number of fixes and time span for each device.
