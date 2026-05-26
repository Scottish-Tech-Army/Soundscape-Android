(function () {
  'use strict';

  const manifestUrl = window.OFFLINE_EXTRACTS_MANIFEST_URL;
  const panel = document.getElementById('offline-extracts-panel');

  const FEATURE_TYPE_LABEL = {
    country: 'Country',
    admin1: 'Region',
    city_cluster: 'City',
  };

  const COLOR_BY_TYPE = {
    country: '#3b8de4',
    admin1: '#8a4fbf',
    city_cluster: '#dc6b1f',
  };

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function formatSize(bytes) {
    if (bytes == null || isNaN(bytes)) return '';
    const gb = 1024 * 1024 * 1024;
    const mb = 1024 * 1024;
    if (bytes >= gb) return (bytes / gb).toFixed(1) + ' GB';
    if (bytes >= mb) return (bytes / mb).toFixed(0) + ' MB';
    return Math.max(1, Math.round(bytes / 1024)) + ' KB';
  }

  // MapLibre serialises feature properties through vector layers, so arrays
  // come back as JSON strings. Try to recover the original value.
  function asArray(value) {
    if (Array.isArray(value)) return value;
    if (typeof value === 'string') {
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) return parsed;
      } catch (e) { /* ignore */ }
    }
    return null;
  }

  function showDetails(properties) {
    const nameLocal = properties.name_local;
    const name = properties.name;
    const localName = nameLocal || name || 'Unnamed';
    const altName = nameLocal && name && nameLocal !== name ? name : '';

    const featureType = properties.feature_type;
    const ftLabel = FEATURE_TYPE_LABEL[featureType] || featureType || 'Map extract';

    const localCitiesArr = asArray(properties.city_local_names);
    const altCitiesArr = asArray(properties.city_names);
    const localCities = localCitiesArr ? localCitiesArr.join(', ') : '';
    const altCities = altCitiesArr ? altCitiesArr.join(', ') : '';
    const cities = localCities || altCities;

    const sizeBytes = Number(properties['extract-size']);
    const sizeStr = formatSize(sizeBytes);

    let html = '';
    html += '<h3>' + escapeHtml(localName) + '</h3>';
    if (altName) {
      html += '<p class="alt-name">' + escapeHtml(altName) + '</p>';
    }
    html += '<p><span class="feature-type-pill ' + escapeHtml(featureType || '') + '">'
         + escapeHtml(ftLabel) + '</span></p>';
    html += '<dl>';
    if (sizeStr) {
      html += '<dt>Download size</dt><dd>' + escapeHtml(sizeStr) + '</dd>';
    }
    if (cities) {
      html += '<dt>Includes</dt><dd>' + escapeHtml(cities) + '</dd>';
      if (localCities && altCities && localCities !== altCities) {
        html += '<dt>Also known as</dt><dd>' + escapeHtml(altCities) + '</dd>';
      }
    }
    if (properties.continent) {
      html += '<dt>Continent</dt><dd>' + escapeHtml(properties.continent) + '</dd>';
    }
    if (properties.country_name) {
      html += '<dt>Country</dt><dd>' + escapeHtml(properties.country_name) + '</dd>';
    }
    if (properties.iso_a2) {
      html += '<dt>Country code</dt><dd>' + escapeHtml(properties.iso_a2) + '</dd>';
    }
    html += '</dl>';
    panel.innerHTML = html;
  }

  function addExtractLayer(map, featureType, beforeId) {
    const layerId = 'extracts-fill-' + featureType;
    const outlineId = 'extracts-outline-' + featureType;
    const color = COLOR_BY_TYPE[featureType] || '#888';

    map.addLayer({
      id: layerId,
      type: 'fill',
      source: 'extracts',
      filter: ['==', ['get', 'feature_type'], featureType],
      paint: {
        'fill-color': color,
        'fill-opacity': [
          'case',
          ['boolean', ['feature-state', 'hover'], false], 0.55,
          0.25,
        ],
      },
    }, beforeId);

    map.addLayer({
      id: outlineId,
      type: 'line',
      source: 'extracts',
      filter: ['==', ['get', 'feature_type'], featureType],
      paint: {
        'line-color': color,
        'line-width': [
          'case',
          ['boolean', ['feature-state', 'hover'], false], 2,
          0.6,
        ],
        'line-opacity': 0.9,
      },
    }, beforeId);

    return layerId;
  }

  function wireInteraction(map, fillLayerIds) {
    let hovered = null;

    function topFeatureAt(point) {
      const features = map.queryRenderedFeatures(point, { layers: fillLayerIds });
      return features.length ? features[0] : null;
    }

    function clearHover() {
      if (hovered !== null) {
        map.setFeatureState({ source: 'extracts', id: hovered }, { hover: false });
        hovered = null;
      }
    }

    map.on('mousemove', function (e) {
      const f = topFeatureAt(e.point);
      if (!f) {
        clearHover();
        map.getCanvas().style.cursor = '';
        return;
      }
      if (hovered !== f.id) {
        clearHover();
        hovered = f.id;
        map.setFeatureState({ source: 'extracts', id: hovered }, { hover: true });
      }
      map.getCanvas().style.cursor = 'pointer';
    });

    map.on('mouseout', function () {
      clearHover();
      map.getCanvas().style.cursor = '';
    });

    map.on('click', function (e) {
      const f = topFeatureAt(e.point);
      if (f) showDetails(f.properties);
    });
  }

  const app = document.getElementById('offline-extracts-app');
  app.classList.add('loading');

  function fail(message) {
    app.classList.remove('loading');
    panel.innerHTML = '<p class="placeholder">' + escapeHtml(message) + '</p>';
    console.error('[offline-extracts]', message);
  }

  if (typeof maplibregl === 'undefined') {
    fail('Map library failed to load. Check your network connection and reload the page.');
    return;
  }

  let map;
  try {
    map = new maplibregl.Map({
    container: 'offline-extracts-map',
    style: {
      version: 8,
      sources: {
        osm: {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          maxzoom: 19,
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        },
      },
      layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
    },
    center: [10, 25],
    zoom: 1.2,
    minZoom: 0.5,
    maxZoom: 10,
    });
  } catch (err) {
    fail('Could not initialise the map (' + (err.message || err) + '). Your browser may not support WebGL.');
    return;
  }

  map.on('error', function (e) {
    console.error('[offline-extracts] map error', e && e.error ? e.error : e);
  });

  map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');
  map.addControl(new maplibregl.ScaleControl({ unit: 'metric' }), 'bottom-left');

  map.on('load', function () {
    fetch(manifestUrl)
      .then(function (resp) {
        if (!resp.ok) throw new Error('Failed to load manifest: ' + resp.status);
        return resp.json();
      })
      .then(function (data) {
        // Assign deterministic ids so we can drive hover feature-state.
        data.features.forEach(function (f, i) { f.id = i; });

        map.addSource('extracts', { type: 'geojson', data: data });

        // Draw biggest things at the bottom so small city clusters stay clickable.
        const countryLayer = addExtractLayer(map, 'country');
        const admin1Layer = addExtractLayer(map, 'admin1');
        const cityLayer = addExtractLayer(map, 'city_cluster');

        wireInteraction(map, [cityLayer, admin1Layer, countryLayer]);

        app.classList.remove('loading');
      })
      .catch(function (err) {
        fail('Could not load the list of map extracts: ' + (err.message || err));
      });
  });
})();
