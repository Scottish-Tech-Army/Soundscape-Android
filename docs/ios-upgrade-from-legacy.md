---
title: Upgrading from the legacy iOS app
layout: page
parent: "Using Soundscape"
has_toc: false
---

# Upgrading from the legacy Soundscape iOS app

This page is for users of the legacy Microsoft Soundscape iOS app who are
updating to the new Scottish Tech Army release. The new app is a complete
rewrite that shares its codebase with the Android version. It ships under the
same bundle identifier (`org.scottishtecharmy.soundscape`), so an App Store /
TestFlight update keeps your saved data in place — but a few features have
changed or are not yet present.

## What carries over

When you launch the new build for the first time, a one-shot migration reads the
legacy database and preferences out of the app's existing container and writes
them into the new app's storage. It copies rather than moves: the legacy
database and settings are left exactly where they were, untouched, so nothing
is lost if you go back to the legacy build or if support needs to look at them
later.

Your markers and routes are imported on a screen shown once, just after the app
opens, with a progress count. **You'll need an internet connection for this**,
or an offline map covering the places you saved: the old app didn't store a name
for markers you created from a place on the map — it looked the name up each
time it drew the list — so the new app has to look those names up too. If it
can't, nothing is imported and you're offered a "Try again". You can also skip
it with "Not now" and be asked again the next time you open Soundscape; your
saved data stays where it is in the meantime.

Migrated automatically:

- **All saved markers** — name, address, latitude/longitude. Markers you gave
  your own name to keep it. Markers you saved from a place on the map are named
  from current map data instead, so a few may come across under a slightly
  different name than before — the underlying map has moved on since the old
  app's data was frozen. Temporary "audio beacon" markers (the ones the old app
  created when you started a beacon) are not migrated; only the markers you
  explicitly saved.
- **All saved routes** — name, description, and waypoint order. Each waypoint
  is reconnected to its underlying marker.
- **Most preferences** with a direct equivalent in the new app:
  - Measurement units (metric / imperial)
  - App language
  - Beacon style (mapped to the closest equivalent in the new beacon set)
  - Speech rate (rescaled from the old 0–1 slider to the new 0.5×–2× range)
  - Master "automatic callouts" toggle
  - Mix-with-other-audio
  - Marker sort preference (distance / alphabetical)

The migration runs only once, and never deletes anything belonging to the
legacy app. If something goes wrong (e.g. a damaged database file, or no
connection to look place names up with), it imports nothing at all and tries
again on the next launch rather than discarding anything or leaving you with a
half-finished set of markers.

## What's new

- A fully accessible, Compose-based UI shared with Android — the iOS and
  Android apps now look and behave the same.
- **GPX recording**, plus GPX and Soundscape route file import/export through
  the standard iOS share sheet.
- **Offline map downloads** — pre-download an area for use when you're off the
  network.
- **Apple Maps / share-sheet integration** via a Share Extension — share a
  location from Maps, Safari, etc. directly into Soundscape.
- **AirPods head-tracking** — when you're wearing AirPods that report head
  orientation, beacons spatialise relative to where your head is pointing
  rather than where the phone is pointing.

## What's missing or changed

These were available in the legacy app but are **not present** in the rewrite.
Some are deliberate trade-offs and some are simply work that hasn't been done
yet — none of them block the migration, but you should know about them before
upgrading.

### Removed

- **iCloud sync of markers and routes.** The legacy app used iCloud's
  key-value store to mirror your markers and routes between devices
  automatically. The new app stores everything locally only. **Workaround:**
  use the new GPX export/import to move data between devices manually. Cloud
  sync may return as a future feature, but it isn't on the immediate roadmap.
- **Apple Watch app.** No watchOS companion is shipped.
- **CarPlay support.** Not implemented.
- **Siri Shortcuts.** The legacy app donated `NSUserActivity` shortcuts so you
  could say things like "Hey Siri, what's around me?". This isn't wired up
  yet. The standalone voice command UI is also not implemented on iOS.
- **Push-notification subscriptions** for service announcements.
- **The custom `.soundscape` document file format.** The new app uses
  industry-standard GPX and a JSON route format instead. Legacy `.soundscape`
  files cannot be opened directly — re-export them as GPX from the legacy app
  if you have a copy installed, or from another device that still has them.
- **Sharing a single marker via universal link** (e.g. a `links.soundscape...`
  URL pointing at one marker). Universal links from older shares still resolve
  the way they did before, but new shares are expressed as GPX files instead.

### Reduced or changed

- **Per-category callout toggles.** The legacy app had seven separate switches
  for callouts (places, landmarks, mobility, information, safety,
  intersections, destination). The new app has fewer, broader categories
  (places & landmarks, mobility, distance to beacon) plus the master "allow
  callouts" switch. During migration, your old per-category preferences are
  collapsed into the new ones — for example, "places" and "landmarks" are
  combined into "places & landmarks" (on if either was on).
- **Per-channel audio gains.** The legacy app exposed separate gain controls
  for TTS, beacon and effects, plus three per-channel volume sliders. The new
  app has a single speech rate plus a "mix with other audio" toggle; per-
  channel gain is no longer adjustable. These settings are not migrated.
- **Marker notes.** The legacy "annotation" field on a marker (free-text user
  notes) is not carried over by the migration — the new schema doesn't include
  a notes field. The legacy database still holds them, so nothing is lost, but
  the new app has nowhere to show them.
- **Beacon variants.** The "haptic-only" beacon and the legacy "Classic" /
  "V2" beacons are mapped to the closest current beacon style. The new
  catalogue is broader (Original, Current, Tactile, Flare, Shimmer, Ping,
  Drop, Signal variants, Mallet variants) — you'll likely want to revisit
  Settings → Audio → Beacon style after upgrading.
- **Headphone-motion calibration.** The new app auto-detects head tracking
  from AirPods and skips the manual calibration screen the legacy app had.

## I had a problem with the upgrade

If you find that markers or routes are missing after the upgrade, **don't
delete the new app yet.** Send the support team:

1. The model and iOS version of your device.
2. Roughly how many markers and routes you had before upgrading.
3. Whether you'd previously enabled iCloud backups for the legacy app.

You can reach support from Settings → Help → Contact support inside the new
app.
