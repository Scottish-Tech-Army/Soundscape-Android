package org.scottishtecharmy.soundscape.screens.home.home

import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.spatialk.geojson.Geometry
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.mapstyle.AccessibleTheme
import org.scottishtecharmy.soundscape.mapstyle.accessibleLayerOverrides
import org.scottishtecharmy.soundscape.mapstyle.argbToRgba
import org.scottishtecharmy.soundscape.mapstyle.buildMapStyle
import org.scottishtecharmy.soundscape.mapstyle.resolveTileSourceUrl

@Composable
actual fun PlatformMapContainer(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier,
    currentBeaconWaypointIndex: Int,
    extractGeometry: Geometry?,
    forceOnlineTiles: Boolean,
    onInteractionChanged: (Boolean) -> Unit,
    onBeaconLocationEdited: ((LngLatAlt) -> Unit)?,
) {
    val context = LocalContext.current

    val foregroundColor = argbToRgba(MaterialTheme.colorScheme.onBackground.toArgb())
    val backgroundColor = argbToRgba(MaterialTheme.colorScheme.background.toArgb())
    val overrides = accessibleLayerOverrides(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor
    )

    // Read TILE_PROVIDER_URL from Android BuildConfig via reflection
    val tileProviderUrl = remember {
        try {
            val buildConfigClass = Class.forName("org.scottishtecharmy.soundscape.BuildConfig")
            buildConfigClass.getField("TILE_PROVIDER_URL").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    // Use offline extracts if available, otherwise fall back to network — unless
    // the caller explicitly requires online tiles (e.g. the offline-map details
    // preview, which should always render from the network regardless of what's
    // already downloaded).
    //
    // Extracts are downloaded to <user-selected storage>/Downloads (see
    // AndroidOfflineMapsManager.extractsDir() and SoundscapeService.startGeoEngine()
    // in the app module), not the app's external-files root — this must match or
    // downloaded extracts are silently never found and the map falls back to
    // network tiles. MainActivity.SELECTED_STORAGE_KEY/_DEFAULT aren't reachable
    // from here (shared can't depend on the app module), so the well-known
    // AndroidX preference key/file naming convention is replicated directly, same
    // as the BuildConfig.TILE_PROVIDER_URL reflection lookup above.
    val extractsPath = remember {
        val prefs = context.getSharedPreferences(
            "${context.packageName}_preferences",
            android.content.Context.MODE_PRIVATE,
        )
        val selectedStorage = prefs.getString("SelectedStorage", "") ?: ""
        "$selectedStorage/${android.os.Environment.DIRECTORY_DOWNLOADS}"
    }
    val tileSourceUrl = remember(mapCenter, forceOnlineTiles) {
        if (forceOnlineTiles) {
            resolveTileSourceUrl(
                location = null,
                extractsPath = "",
                networkTileUrl = tileProviderUrl
            )
        } else {
            resolveTileSourceUrl(
                location = mapCenter,
                extractsPath = extractsPath,
                networkTileUrl = tileProviderUrl,
            )
        }
    }

    val baseStyle = remember(foregroundColor, backgroundColor, tileSourceUrl) {
        buildMapStyle(
            theme = AccessibleTheme,
            spritePath = "asset://osm-liberty-accessible/osm-liberty",
            glyphsPath = "asset://osm-liberty-accessible/fonts/{fontstack}/{range}.pbf",
            tileSourceUrl = tileSourceUrl,
            overrides = overrides,
            symbolForegroundColor = foregroundColor,
            symbolBackgroundColor = backgroundColor,
        )
    }

    RedrawUntilSurfacesCreated()

    DestroyMapWhenDisposed {
        MapContainerLibre(
            mapCenter = mapCenter,
            allowScrolling = allowScrolling,
            userLocation = userLocation,
            userSymbolRotation = userSymbolRotation,
            beaconLocation = beaconLocation,
            routeData = routeData,
            modifier = modifier,
            currentBeaconWaypointIndex = currentBeaconWaypointIndex,
            baseStyle = baseStyle,
            extractGeometry = extractGeometry,
            onInteractionChanged = onInteractionChanged,
            onBeaconLocationEdited = onBeaconLocationEdited,
        )
    }
}

/**
 * Destroys the map's MapView when [content] leaves the composition.
 *
 * maplibre-compose (0.13) drives the MapView from LocalLifecycleOwner, and only destroys it on that
 * lifecycle's ON_DESTROY. Leaving the composition just stops observing it, so a map whose screen is
 * still alive is never destroyed: Home's, every time another screen is opened on top of it, as Home
 * stays on the back stack. MapLibre only frees a map's native side (its style, tiles and glyphs) in
 * MapView.onDestroy, so each of those maps stayed in memory for good: about 2.7 MB of native heap
 * each time Home was returned to on a Samsung A52s, and up to 2 GB on the test farm's phones.
 *
 * So the map is given a lifecycle of its own, which follows the screen's until the map leaves the
 * composition and is then destroyed. That has to happen while maplibre-compose is still observing
 * it: the DisposableEffect below comes after the map, and the effects being disposed run in the
 * reverse of their order in the composition, so it runs first.
 */
@Composable
private fun DestroyMapWhenDisposed(content: @Composable () -> Unit) {
    val screenLifecycle = LocalLifecycleOwner.current.lifecycle
    val mapLifecycleOwner = remember(screenLifecycle) { MapLifecycleOwner(screenLifecycle) }
    CompositionLocalProvider(LocalLifecycleOwner provides mapLifecycleOwner) {
        content()
    }
    DisposableEffect(mapLifecycleOwner) {
        onDispose { mapLifecycleOwner.destroy() }
    }
}

private class MapLifecycleOwner(private val screenLifecycle: Lifecycle) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val follower = LifecycleEventObserver { _, _ ->
        if (registry.currentState != Lifecycle.State.DESTROYED) {
            registry.currentState = screenLifecycle.currentState
        }
    }

    init {
        screenLifecycle.addObserver(follower)
    }

    fun destroy() {
        screenLifecycle.removeObserver(follower)
        // Already destroyed with the screen, or never created at all
        if (!registry.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        if (registry.observerCount == 0) {
            // Nothing to tell: maplibre-compose has already stopped observing, so its MapView
            // won't be destroyed. If this is ever logged, the ordering above no longer holds.
            Log.w(TAG, "Map lifecycle has no observer left to destroy its MapView")
        }
        registry.currentState = Lifecycle.State.DESTROYED
    }

    private companion object {
        const val TAG = "PlatformMapContainer"
    }
}

/**
 * Keeps asking for frames until every SurfaceView on screen has its surface, for up to
 * [SURFACE_CREATION_FRAMES] frames after the map is composed.
 *
 * A SurfaceView only creates its surface from a pre-draw pass, and MapLibre's is attached partway
 * through the frame that composes the map - after that frame's pre-draw has already gone by. It
 * gets its surface on the next frame, but nothing guarantees there is one: once the map has been
 * composed the screen can be completely static. On a phone with a compass the heading keeps
 * redrawing the user symbol so it never shows, but on one without (the Cubot J10) pressing the full
 * screen map button left a blank map with no surface until the screen was turned off and on again.
 * It happened every time with a finger press and only sometimes with an instant one, because a
 * short press's ripple was often still animating and so supplied the frame.
 */
@Composable
private fun RedrawUntilSurfacesCreated() {
    val view = LocalView.current
    LaunchedEffect(view) {
        repeat(SURFACE_CREATION_FRAMES) {
            withFrameNanos { }
            if (!hasShownSurfaceViewWithoutSurface(view.rootView)) return@LaunchedEffect
            view.invalidate()
        }
    }
}

private const val SURFACE_CREATION_FRAMES = 60

private fun hasShownSurfaceViewWithoutSurface(view: View): Boolean {
    if (view is SurfaceView && view.isShown && view.width > 0 && view.height > 0 &&
        !view.holder.surface.isValid
    ) return true
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            if (hasShownSurfaceViewWithoutSurface(view.getChildAt(i))) return true
        }
    }
    return false
}
