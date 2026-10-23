package org.scottishtecharmy.soundscape.platform

import org.maplibre.compose.map.RenderOptions

// The view a map draws through is now chosen by MapUiOptions.renderMode rather than here, and
// defaults to AndroidRenderMode.Surface (a SurfaceView).
actual fun nativeMapRenderOptions(): RenderOptions = RenderOptions.Standard
