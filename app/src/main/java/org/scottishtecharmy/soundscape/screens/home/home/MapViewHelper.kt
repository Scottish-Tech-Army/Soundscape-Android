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

@Composable
fun rememberMapViewWithLifecycle(disposeCode : (map : MapView) -> Unit): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        val options = createFromAttributes(context)
//        options.apply {
//            pixelRatio(4.0F)
//        }
        val view = MapView(context, options)
        return@remember view
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        // Make MapView follow the current lifecycle
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            // Removing the observer means that no ON_DESTROY event is ever received
            lifecycle.removeObserver(lifecycleObserver)
            // Call the disposal code and destroy the mapView
            disposeCode(mapView)
            mapView.onDestroy()
        }
    }

    return mapView
}

private fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                // We should never get this event
                assert(false)
            }
            else -> throw IllegalStateException()
        }
    }
