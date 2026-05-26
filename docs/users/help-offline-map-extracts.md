---
title: Offline map extracts
layout: page
parent: Using Soundscape
has_toc: false
---

# Offline map extracts

Soundscape can download regions of map data to your phone so that it keeps working when you have no internet connection. The map below shows the regions that are currently available to download. They come in three flavours:

- **Country** — the whole country in a single extract.
- **Region** — a state, province or other top level subdivision of a country.
- **City** — a small extract covering a city and the towns immediately around it.

Tap on any region to see its name, size and the towns or cities it includes. The same information is shown in the **Offline maps** screen inside the app.

<link rel="stylesheet" href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" />
<link rel="stylesheet" href="{{ '/assets/offline-extracts.css' | relative_url }}" />

<div id="offline-extracts-app">
  <div id="offline-extracts-map" role="application" aria-label="World map of offline map extracts"></div>
  <aside id="offline-extracts-panel" aria-live="polite">
    <p class="placeholder">Tap a region on the map to see its details here.</p>
  </aside>
</div>

<div id="offline-extracts-legend" aria-hidden="true">
  <span class="swatch country"></span> Country
  <span class="swatch admin1"></span> Region
  <span class="swatch city"></span> City
</div>

<script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
<script>
  window.OFFLINE_EXTRACTS_MANIFEST_URL = "{{ '/assets/manifest.geojson' | relative_url }}";
</script>
<script src="{{ '/assets/offline-extracts.js' | relative_url }}"></script>
