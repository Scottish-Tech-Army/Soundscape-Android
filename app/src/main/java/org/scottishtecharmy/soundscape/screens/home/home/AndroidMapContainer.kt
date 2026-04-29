package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.preference.PreferenceManager
import org.maplibre.compose.style.BaseStyle
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import android.os.Environment
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.mapstyle.AccessibleTheme
import org.scottishtecharmy.soundscape.mapstyle.accessibleLayerOverrides
import org.scottishtecharmy.soundscape.mapstyle.argbToRgba
import org.scottishtecharmy.soundscape.mapstyle.buildMapStyle
import org.scottishtecharmy.soundscape.mapstyle.resolveTileSourceUrl

/**
 * Create a location marker drawable which has location_marker as its background, and an integer
 * in the foreground. These are to mark on the map locations of waypoints within a route.
 * @param context The context to use
 * @param number The number to display within the drawable
 * @return A composited ImageBitmap
 */
fun createLocationMarkerImageBitmap(context: Context, number: Int): ImageBitmap {
    val frameLayout = FrameLayout(context)
    frameLayout.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )

    val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.location_marker)
    backgroundDrawable?.let {
        val imageView = ImageView(context)
        imageView.setImageDrawable(it)
        frameLayout.addView(imageView)
    }

    val numberTextView = TextView(context)
    numberTextView.apply {
        text = "$number"
        setTextColor(Color.WHITE)
        textSize = 11f
        gravity = Gravity.CENTER
    }

    frameLayout.addView(numberTextView)
    frameLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    val bitmap = createBitmap(frameLayout.measuredWidth, frameLayout.measuredHeight)
    val canvas = android.graphics.Canvas(bitmap)
    frameLayout.draw(canvas)

    return bitmap.asImageBitmap()
}

@Composable
fun rememberMapBaseStyle(mapCenter: LngLatAlt? = null): BaseStyle {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val foregroundColor = argbToRgba(MaterialTheme.colorScheme.onBackground.toArgb())
    val backgroundColor = argbToRgba(MaterialTheme.colorScheme.background.toArgb())
    val overrides = accessibleLayerOverrides(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
    )

    val extractsPath = sharedPreferences.getString(
        MainActivity.SELECTED_STORAGE_KEY,
        MainActivity.SELECTED_STORAGE_DEFAULT
    )!! + "/" + Environment.DIRECTORY_DOWNLOADS

    return remember(foregroundColor, backgroundColor, mapCenter) {
        val tileSourceUrl = resolveTileSourceUrl(
            location = mapCenter,
            extractsPath = extractsPath,
            networkTileUrl = BuildConfig.TILE_PROVIDER_URL,
        )

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
}

@Composable
fun rememberRouteMarkerImages(): List<ImageBitmap> {
    val context = LocalContext.current
    return remember {
        List(100) { index -> createLocationMarkerImageBitmap(context, index + 1) }
    }
}

/**
 * Android convenience wrapper for [MapContainerLibre] that automatically provides the map style
 * and route marker images.
 */
@Composable
fun AndroidMapContainerLibre(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier = Modifier,
    editBeaconLocation: Boolean = false,
    onMapLongClick: ((LngLatAlt) -> Boolean)? = null,
) {
    val baseStyle = rememberMapBaseStyle(mapCenter = mapCenter)
    val routeMarkerImages = if (routeData != null) rememberRouteMarkerImages() else null

    MapContainerLibre(
        mapCenter = mapCenter,
        allowScrolling = allowScrolling,
        userLocation = userLocation,
        userSymbolRotation = userSymbolRotation,
        beaconLocation = beaconLocation,
        routeData = routeData,
        modifier = modifier,
        editBeaconLocation = editBeaconLocation,
        onMapLongClick = onMapLongClick,
        baseStyle = baseStyle,
        routeMarkerImages = routeMarkerImages
    )
}
