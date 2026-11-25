---
name: replay-gpx
description: Replay a Soundscape GPX recording (e.g. a travel.gpx a user emailed in) through the production-equivalent callout replay and open the result on a local map. Use when asked to replay, run, visualise, map or look at the callouts for a GPX/recording/trace.
---

# Replay a GPX recording

`scripts/replay-gpx.py` does the whole job: it picks and downloads the offline extract(s) covering
the track (cached in `~/.cache/soundscape-replay/extracts`), runs
`MvtTileTest.replayExternalGpx` against them, and writes
`app/build/test-output/replay/<name>.{geojson,txt,html}`. The HTML is a self-contained map with
the callout list, drawn over a base map built from the same extract (`scripts/replay_basemap.py`,
OpenMapTiles style). It fetches nothing when opened. See `docs/developers/debugging-geojson.md`.

## Steps

1. **Find the file.** User recordings arrive by email and usually end up in `~/Downloads`
   (`ls -la --time-style=full-iso ~/Downloads/*.gpx`). If there are several overlapping files
   from one walk, merge them first with the `merge-gpx` skill. If it isn't clear which file is
   meant, ask.
2. **Run it.** The file is a download, so use `-I`:
   ```
   python3 -I scripts/replay-gpx.py ~/Downloads/travel.gpx
   ```
   - `--start N --end M` replays only part of the track (GPX point indices, as in the `[index]`
     markers in the callout text and the `index` property of each fix).
   - `--extract a.pmtiles ...` uses local extracts instead of the manifest.
   - `--no-open` skips opening the browser.
   - The first run for a new area downloads its extract, which can be hundreds of MB. A 503
     means the provider is building it; the script retries.
3. **Report.** Give the paths it prints, the callout count, and any warning about points outside
   every extract. If asked about specific callouts, read `<name>.txt`.

## Don'ts

- Don't copy a user's GPX into `app/src/test/res/.../gpxFiles/` or commit it unless Dave asks.
  User recordings are private.
- Don't upload the GPX or the output anywhere, gists included.
- In anything written about a recording, describe its start and end neutrally (see the memory
  about never labelling recordings as home).
