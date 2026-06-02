package org.scottishtecharmy.soundscape.screens.home.home

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions.createFromAttributes
import org.maplibre.android.maps.MapView
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun rememberMapViewWithLifecycle(disposeCode : (map : MapView) -> Unit): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        val options = createFromAttributes(context)
        val view = MapView(context, options)
        return@remember view
    }

    // The MapView can be torn down from two places: the lifecycle ON_DESTROY event and the
    // DisposableEffect onDispose. Depending on how the screen is left these can arrive in either
    // order - in particular, when the whole Activity is destroyed (e.g. returning to the app via a
    // notification) ON_DESTROY arrives BEFORE onDispose. We must always run the app's cleanup
    // (disposeCode - which removes SymbolManager layers etc.) while the native map is still alive,
    // i.e. BEFORE mapView.onDestroy(), and we must only do it once. Otherwise SymbolManager.onDestroy()
    // reads a freed native layer and crashes in libmaplibre.so (mbgl::android::Layer::getId).
    val destroyed = remember { AtomicBoolean(false) }
    fun destroyMap() {
        if (destroyed.getAndSet(true)) return
        // Cleanup first, while the native map/style/layers still exist...
        disposeCode(mapView)
        // ...then destroy the MapView itself.
        mapView.onDestroy()
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        // Make MapView follow the current lifecycle
        val lifecycleObserver = getMapLifecycleObserver(mapView, ::destroyMap)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            // Tidy up and destroy the mapView (no-op if ON_DESTROY already did it).
            destroyMap()
        }
    }

    return mapView
}

private fun getMapLifecycleObserver(
    mapView: MapView,
    destroyMap: () -> Unit,
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                println("MapView: ON_DESTROY")
                destroyMap()
            }
            else -> throw IllegalStateException()
        }
    }
