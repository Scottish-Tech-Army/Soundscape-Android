package org.scottishtecharmy.soundscape.platform

import org.maplibre.compose.map.RenderOptions

/**
 * Platform-specific RenderOptions for the MapLibre map. On Android we force a
 * SurfaceView due to a leak in MapLibre: See SA-379
 * iOS already renders into a layer that composites correctly, so it falls back
 * to RenderOptions.Standard.
 */
expect fun nativeMapRenderOptions(): RenderOptions
