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

When you launch the new build for the first time, a one-shot migration runs
silently in the background. It reads the legacy database and preferences out of
the app's existing container and writes them into the new app's storage. After
it finishes, the legacy files are removed.

Migrated automatically:

- **All saved markers** — name, address, latitude/longitude. Temporary "audio
  beacon" markers (the ones the old app created when you started a beacon) are
  not migrated; only the markers you explicitly saved.
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

The migration runs only once. If something goes wrong (e.g. a damaged database
file), it leaves your legacy data untouched and tries again on the next launch
rather than discarding anything.

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
  notes) is not preserved by the migration — the new schema doesn't include a
  notes field. If you've written notes on a marker that you want to keep,
  export your markers from the legacy app as a backup before upgrading.
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
