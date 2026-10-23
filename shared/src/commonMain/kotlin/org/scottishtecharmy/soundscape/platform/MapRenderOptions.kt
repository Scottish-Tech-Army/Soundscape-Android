package org.scottishtecharmy.soundscape.platform

import org.maplibre.compose.map.RenderOptions

/**
 * Platform-specific RenderOptions for the MapLibre map. Both platforms currently use
 * RenderOptions.Standard. On Android the map now draws through a SurfaceView by default,
 * which is what we had forced before because of a leak in MapLibre: see SA-379.
 */
expect fun nativeMapRenderOptions(): RenderOptions
