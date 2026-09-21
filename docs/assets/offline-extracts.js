(function () {
  'use strict';

  // The basemap is rendered by protomaps-leaflet straight onto a 2D <canvas>,
  // and the extract polygons are plain Leaflet SVG. Neither needs WebGL, so the
  // page works on machines/browsers where WebGL is unavailable or broken.
  const manifestUrl = window.OFFLINE_EXTRACTS_MANIFEST_URL;
  const pmtilesUrl = window.OFFLINE_EXTRACTS_PMTILES_URL;
  const panel = document.getElementById('offline-extracts-panel');
  const app = document.getElementById('offline-extracts-app');

  // User-facing strings, localized via the page (see _data/offline_extracts.yml).
  // English values are the fallback if a key is missing for the active language.
  const I18N = window.OFFLINE_EXTRACTS_I18N || {};
  function s(key, fallback) {
    return I18N[key] != null ? I18N[key] : fallback;
  }
  // Fill a single %s / %d placeholder.
  function fill(template, value) {
    return String(template).replace(/%[sd]/, value);
  }

  const FEATURE_TYPE_LABEL = {
    country: s('typeCountry', 'Country'),
    admin1: s('typeRegion', 'Region'),
    city_cluster: s('typeCity', 'City'),
  };

  // Draw order: country (bottom) -> region -> city (top), so the small city
  // clusters stay visible on top of the regions and countries they sit inside.
  const TYPE_DRAW_ORDER = ['country', 'admin1', 'city_cluster'];
  const TYPE_RANK = { country: 0, admin1: 1, city_cluster: 2 };

  const COLOR_BY_TYPE = {
    country: '#3b8de4',
    admin1: '#8a4fbf',
    city_cluster: '#dc6b1f',
  };

  // Many extracts in the manifest carry a placeholder "extract-size": the size
  // hasn't been measured yet, so the generator assigns one of a few default
  // constants. A real measured size is a byte count that is effectively unique;
  // an exact value shared by several extracts can only be such a default. So we
  // treat any size value recurring more than this many times as a placeholder.
  const PLACEHOLDER_MIN_REPEATS = 4;
  const placeholderSizes = new Set();

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function formatSize(bytes) {
    const gb = 1024 * 1024 * 1024;
    const mb = 1024 * 1024;
    const kb = 1024;
    if (bytes >= gb) return (bytes / gb).toFixed(1) + ' GB';
    if (bytes >= mb) return Math.round(bytes / mb) + ' MB';
    return Math.max(1, Math.round(bytes / kb)) + ' KB';
  }

  // A genuine vector-tile extract is never this small (the smallest real one in
  // the manifest is a few hundred KB), so any tinier size is an unmeasured
  // placeholder, however often or rarely that exact value happens to recur.
  const REAL_SIZE_FLOOR = 100 * 1024;

  // Real measured size -> formatted string; missing or placeholder -> null, so
  // the caller can show the localized "not yet available" phrase instead.
  // The value is wrapped in a left-to-right isolate (LRI…PDI) so that a number
  // plus unit like "339 MB" keeps its order inside right-to-left languages
  // (Arabic, Farsi) instead of being reordered to "MB 339".
  function formattedSize(bytes) {
    if (!bytes || isNaN(bytes) || bytes < REAL_SIZE_FLOOR || placeholderSizes.has(bytes)) {
      return null;
    }
    return '⁦' + formatSize(bytes) + '⁩';
  }

  // city_names / city_local_names are JSON arrays in the manifest. Be tolerant
  // of either a real array or a stringified one.
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

  // "A", "A and B", "A, B and C" (conjunction localized).
  function formatList(arr) {
    if (!arr || !arr.length) return '';
    if (arr.length === 1) return arr[0];
    return arr.slice(0, -1).join(', ') + ' ' + s('and', 'and') + ' ' + arr[arr.length - 1];
  }

  // ----- Point in polygon (lng/lat space) -------------------------------------

  function pointInRing(lng, lat, ring) {
    let inside = false;
    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
      const xi = ring[i][0], yi = ring[i][1];
      const xj = ring[j][0], yj = ring[j][1];
      const intersects = ((yi > lat) !== (yj > lat)) &&
        (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
      if (intersects) inside = !inside;
    }
    return inside;
  }

  // A polygon is [outerRing, hole1, hole2, ...].
  function pointInPolygon(lng, lat, polygon) {
    if (!polygon.length || !pointInRing(lng, lat, polygon[0])) return false;
    for (let h = 1; h < polygon.length; h++) {
      if (pointInRing(lng, lat, polygon[h])) return false;
    }
    return true;
  }

  function pointInGeometry(lng, lat, geom) {
    if (!geom) return false;
    if (geom.type === 'Polygon') return pointInPolygon(lng, lat, geom.coordinates);
    if (geom.type === 'MultiPolygon') {
      return geom.coordinates.some(function (poly) { return pointInPolygon(lng, lat, poly); });
    }
    return false;
  }

  function bboxOfGeometry(geom) {
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    const eat = function (ring) {
      for (let i = 0; i < ring.length; i++) {
        const x = ring[i][0], y = ring[i][1];
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    };
    if (geom.type === 'Polygon') {
      geom.coordinates.forEach(eat);
    } else if (geom.type === 'MultiPolygon') {
      geom.coordinates.forEach(function (poly) { poly.forEach(eat); });
    }
    return [minX, minY, maxX, maxY];
  }

  // ----- Detail panel ---------------------------------------------------------

  // One row per extract, e.g.:
  //   Saguenay (City), download size 20 MB. Includes Chicoutimi, Jonquiere and Saguenay.
  // Name uses the local name (mirroring the app), with the English name after a
  // slash where it differs.
  function featureRowHtml(props) {
    const nameLocal = props.name_local;
    const name = props.name;
    const localName = nameLocal || name || 'Unnamed';
    const altName = nameLocal && name && nameLocal !== name ? name : '';

    const featureType = props.feature_type;
    const ftLabel = FEATURE_TYPE_LABEL[featureType] || featureType || 'Map extract';

    const sizeStr = formattedSize(Number(props['extract-size']));
    const sizePhrase = sizeStr
      ? fill(s('downloadSize', 'download size %s'), sizeStr)
      : s('sizeUnavailable', 'download size not yet available');

    const localCities = asArray(props.city_local_names);
    const altCities = asArray(props.city_names);
    const cities = (localCities && localCities.length ? localCities : altCities) || [];

    let html = '<li class="extract-row">';
    html += '<span class="extract-name">' + escapeHtml(localName) + '</span>';
    if (altName) html += ' <span class="extract-alt">/ ' + escapeHtml(altName) + '</span>';
    html += ' <span class="extract-type ' + escapeHtml(featureType || '') + '">('
         + escapeHtml(ftLabel) + ')</span>';
    html += ', ' + escapeHtml(sizePhrase) + '.';
    if (featureType === 'city_cluster' && cities.length) {
      html += ' <span class="extract-includes">'
           + escapeHtml(fill(s('includes', 'Includes %s.'), formatList(cities))) + '</span>';
    }
    html += '</li>';
    return html;
  }

  function showExtractsAt(features) {
    if (!features.length) {
      panel.innerHTML = '<p class="placeholder">'
        + escapeHtml(s('none', 'No offline map extract covers this point. Tap one of the shaded areas.'))
        + '</p>';
      return;
    }
    const sorted = features.slice().sort(function (a, b) {
      return (TYPE_RANK[a.properties.feature_type] || 0) - (TYPE_RANK[b.properties.feature_type] || 0);
    });
    const heading = sorted.length === 1
      ? s('coversOne', '1 extract covers this point')
      : fill(s('coversMany', '%d extracts cover this point'), sorted.length);
    let html = '<p class="panel-heading">' + heading + '</p>';
    html += '<ul class="extract-list">';
    html += sorted.map(function (f) { return featureRowHtml(f.properties); }).join('');
    html += '</ul>';
    panel.innerHTML = html;
    panel.scrollTop = 0;
  }

  // ----- Boot -----------------------------------------------------------------

  app.classList.add('loading');

  function fail(message) {
    app.classList.remove('loading');
    panel.innerHTML = '<p class="placeholder">' + escapeHtml(message) + '</p>';
    console.error('[offline-extracts]', message);
  }

  if (typeof L === 'undefined') {
    fail(s('errLibrary', 'Map library failed to load. Check your network connection and reload the page.'));
    return;
  }
  if (typeof protomapsL === 'undefined') {
    fail(s('errRenderer', 'Map renderer failed to load. Check your network connection and reload the page.'));
    return;
  }

  const map = L.map('offline-extracts-map', {
    center: [25, 10],
    zoom: 2,
    minZoom: 1,
    maxZoom: 9,
    worldCopyJump: true,
    attributionControl: true,
  });

  // OpenMapTiles-schema paint rules for the low-zoom world basemap. Land is the
  // map background colour; water, boundaries and place labels are drawn on top.
  const num = function (v) { return v == null ? NaN : Number(v); };
  const paintRules = [
    {
      dataLayer: 'landcover',
      symbolizer: new protomapsL.PolygonSymbolizer({ fill: '#eaf0e4', opacity: 0.8 }),
      filter: function (z, f) {
        const c = f.props['class'] || f.props['subclass'];
        return c === 'wood' || c === 'grass' || c === 'scrub' || c === 'farmland';
      },
    },
    {
      dataLayer: 'landcover',
      symbolizer: new protomapsL.PolygonSymbolizer({ fill: '#eef3f7' }),
      filter: function (z, f) {
        return f.props['class'] === 'ice' || f.props['subclass'] === 'glacier';
      },
    },
    {
      dataLayer: 'water',
      symbolizer: new protomapsL.PolygonSymbolizer({ fill: '#a7cdf0' }),
    },
    {
      dataLayer: 'boundary',
      symbolizer: new protomapsL.LineSymbolizer({ color: '#c2c7cf', width: 0.7 }),
      filter: function (z, f) {
        const lvl = num(f.props['admin_level']);
        return lvl >= 3 && lvl <= 4 && num(f.props['maritime']) !== 1;
      },
    },
    {
      dataLayer: 'boundary',
      symbolizer: new protomapsL.LineSymbolizer({ color: '#8b929b', width: 1.1 }),
      filter: function (z, f) {
        return num(f.props['admin_level']) <= 2 && num(f.props['maritime']) !== 1;
      },
    },
  ];

  // Label the basemap in the page's language. The OpenMapTiles tiles carry
  // localized name fields (name:de, name:fr, name:ja, name:ar, ...); we map the
  // polyglot page language to the matching field and fall back to English then
  // the default name when a place has no localized name.
  const NAME_FIELD_BY_LANG = {
    en: 'en', 'en-GB': 'en', arz: 'ar', da: 'da', de: 'de', el: 'el', es: 'es',
    fa: 'fa', fi: 'fi', fr: 'fr', 'fr-CA': 'fr', hi: 'hi', is: 'is', it: 'it',
    ja: 'ja', nb: 'no', nl: 'nl', pl: 'pl', pt: 'pt', 'pt-BR': 'pt', ro: 'ro',
    ru: 'ru', sv: 'sv', tr: 'tr', uk: 'uk', 'zh-CN': 'zh-Hans',
  };
  function labelPropsForLang(lang) {
    const props = [];
    if (lang === 'zh-CN') {
      props.push('name:zh-Hans', 'name:zh');
    } else {
      props.push('name:' + (NAME_FIELD_BY_LANG[lang] || 'en'));
    }
    if (props.indexOf('name:en') === -1) props.push('name:en');
    props.push('name');
    return props;
  }
  const LABEL_PROPS = labelPropsForLang(window.OFFLINE_EXTRACTS_LANG || 'en');

  const labelRules = [
    {
      dataLayer: 'place',
      minzoom: 2,
      symbolizer: new protomapsL.CenteredTextSymbolizer({
        labelProps: LABEL_PROPS,
        fill: '#3a3a3a',
        stroke: '#ffffff',
        width: 2,
        fontFamily: 'sans-serif',
        fontWeight: 600,
        fontSize: 13,
      }),
      filter: function (z, f) { return f.props['class'] === 'country'; },
    },
    {
      dataLayer: 'place',
      minzoom: 4,
      symbolizer: new protomapsL.CenteredTextSymbolizer({
        labelProps: LABEL_PROPS,
        fill: '#5a5a5a',
        stroke: '#ffffff',
        width: 1.5,
        fontFamily: 'sans-serif',
        fontWeight: 400,
        fontSize: 11,
      }),
      filter: function (z, f) {
        const c = f.props['class'];
        return c === 'city' || c === 'town';
      },
    },
  ];

  let basemap;
  try {
    basemap = protomapsL.leafletLayer({
      url: pmtilesUrl,
      maxDataZoom: 5,
      backgroundColor: '#f4f1ea',
      attribution: '<a href="https://www.openmaptiles.org/" target="_blank" rel="noopener">&copy; OpenMapTiles</a> '
        + '<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">&copy; OpenStreetMap contributors</a>',
      paintRules: paintRules,
      labelRules: labelRules,
    });
    basemap.addTo(map);
  } catch (err) {
    fail(fill(s('errBasemap', 'Could not initialise the basemap (%s).'), (err && err.message ? err.message : err)));
    return;
  }

  // ----- Extract overlay + interaction ---------------------------------------

  const baseStyleFor = function (type) {
    return {
      color: COLOR_BY_TYPE[type] || '#888',
      weight: 0.8,
      opacity: 0.9,
      fillColor: COLOR_BY_TYPE[type] || '#888',
      fillOpacity: 0.22,
      interactive: false, // hit-testing is done ourselves so clicks reach all layers
    };
  };
  const HOVER_STYLE = { weight: 2.2, fillOpacity: 0.5 };

  // Every entry knows its feature, its Leaflet path layer and a bounding box for
  // a cheap first-pass filter before the (more expensive) point-in-polygon test.
  const entries = [];
  let hovered = null;
  let pendingPoint = null;
  let rafQueued = false;

  function setHover(entry) {
    if (hovered === entry) return;
    if (hovered) hovered.layer.setStyle(baseStyleFor(hovered.type));
    hovered = entry;
    if (hovered) hovered.layer.setStyle(HOVER_STYLE);
  }

  function entriesAt(latlng) {
    const lng = latlng.lng, lat = latlng.lat;
    const hits = [];
    for (let i = 0; i < entries.length; i++) {
      const e = entries[i];
      const b = e.bbox;
      if (lng < b[0] || lng > b[2] || lat < b[1] || lat > b[3]) continue;
      if (pointInGeometry(lng, lat, e.feature.geometry)) hits.push(e);
    }
    return hits;
  }

  // Topmost = smallest feature type rank-wise on top (city over region over country).
  function topmost(hits) {
    let best = null;
    for (let i = 0; i < hits.length; i++) {
      if (!best || (TYPE_RANK[hits[i].type] || 0) >= (TYPE_RANK[best.type] || 0)) best = hits[i];
    }
    return best;
  }

  function processHover() {
    rafQueued = false;
    if (!pendingPoint) return;
    const hits = entriesAt(pendingPoint);
    setHover(topmost(hits));
    map.getContainer().style.cursor = hits.length ? 'pointer' : '';
  }

  map.on('mousemove', function (e) {
    pendingPoint = e.latlng;
    if (!rafQueued) { rafQueued = true; L.Util.requestAnimFrame(processHover); }
  });

  map.on('mouseout', function () {
    pendingPoint = null;
    setHover(null);
    map.getContainer().style.cursor = '';
  });

  map.on('click', function (e) {
    showExtractsAt(entriesAt(e.latlng).map(function (en) { return en.feature; }));
  });

  fetch(manifestUrl)
    .then(function (resp) {
      if (!resp.ok) throw new Error('Failed to load manifest: ' + resp.status);
      return resp.json();
    })
    .then(function (data) {
      // Identify placeholder sizes: any exact byte value shared by more than
      // PLACEHOLDER_MIN_REPEATS extracts is a generator default, not a real size.
      const sizeCounts = new Map();
      (data.features || []).forEach(function (f) {
        const s = Number(f.properties && f.properties['extract-size']);
        if (s) sizeCounts.set(s, (sizeCounts.get(s) || 0) + 1);
      });
      sizeCounts.forEach(function (count, size) {
        if (count > PLACEHOLDER_MIN_REPEATS) placeholderSizes.add(size);
      });

      const byType = { country: [], admin1: [], city_cluster: [] };
      (data.features || []).forEach(function (f) {
        const t = f.properties && f.properties.feature_type;
        if (byType[t]) byType[t].push(f);
      });

      TYPE_DRAW_ORDER.forEach(function (type) {
        L.geoJSON(byType[type], {
          style: baseStyleFor(type),
          onEachFeature: function (feature, layer) {
            entries.push({ feature: feature, layer: layer, type: type, bbox: bboxOfGeometry(feature.geometry) });
          },
        }).addTo(map);
      });

      app.classList.remove('loading');
    })
    .catch(function (err) {
      fail(fill(s('errManifest', 'Could not load the list of map extracts: %s'), (err && err.message ? err.message : err)));
    });
})();
