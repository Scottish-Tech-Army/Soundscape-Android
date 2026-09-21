package org.scottishtecharmy.soundscape.platform

import org.maplibre.compose.map.RenderOptions

actual fun nativeMapRenderOptions(): RenderOptions =
    RenderOptions(renderMode = RenderOptions.RenderMode.SurfaceView)
