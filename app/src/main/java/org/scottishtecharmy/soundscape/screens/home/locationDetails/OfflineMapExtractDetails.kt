package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.screens.home.home.ExtractDetails
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun OfflineMapExtractDetails(
    extract: Feature,
    downloadExtract: (String, Feature) -> Unit,
    deleteExtract: (Feature) -> Unit,
    local: Boolean,
    modifier: Modifier = Modifier) {

    val details = remember(extract) { ExtractDetails(extract) }

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)

    val fc = FeatureCollection()
    fc.addFeature(extract)
    val adapter = GeoJsonObjectMoshiAdapter()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        MapExtractTextsSection(
            extract = extract,
            details = details,
            local = local
        )
        HorizontalDivider()
        MapExtractButtonsSection(
            deleteExtract = { deleteExtract(extract) },
            downloadExtract = { downloadExtract(details.name, extract) },
            local = local
        )

        MapContainerLibre(
            beaconLocation = null,
            allowScrolling = true,
            routeData = null,
            mapCenter = LngLatAlt(),
            userLocation = LngLatAlt(),
            userSymbolRotation = 0.0f,
            onMapLongClick = { latLong ->
                true
            },
            showMap = true,
            modifier = modifier.fillMaxWidth().aspectRatio(1.0f),
            overlayGeoJson = adapter.toJson(fc)
        )
    }
}

@Composable
private fun MapExtractButtonsSection(
    downloadExtract: () -> Unit,
    deleteExtract: () -> Unit,
    local: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.none),
    ) {
        if(local) {
            IconWithTextButton(
                icon = Icons.Filled.Delete,
                text = "Delete offline map",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDelete"),
                onClick = deleteExtract
            )
        } else {
            IconWithTextButton(
                icon = Icons.Filled.Download,
                text = "Download offline map",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDownload"),
                onClick = downloadExtract
            )
        }
    }
}

@Composable
private fun MapExtractTextsSection(
    extract: Feature,
    details: ExtractDetails,
    local: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = "Map name: " + details.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val size = extract.properties?.get("extract-size-string")
        if(size != null) {
            val sizeString = if(local)
                "Size on phone: " + size as String
            else
                "Download size: " + size as String
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if(local)
                    Icon(
                        imageVector = Icons.Filled.SdStorage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                else
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                Text(
                    text = sizeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = details.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OfflineMapExtractDetailsPreview() {

    val geojson = "{\"geometry\":{\"coordinates\":[[[-122.96915039986946,37.99873320994975],[-123.26677281849926,37.99666737454134],[-123.27881573612977,38.8974515792339],[-122.12613231818196,38.901366771718415],[-122.12694587978226,38.56226888429727],[-121.68414374217957,38.56079344929706],[-121.69065575488919,37.78058710763911],[-121.31813082063552,37.778070210414974],[-121.32982831815029,36.87711386897666],[-122.45144613722326,36.88103273247048],[-122.45186425531398,37.08993803868671],[-122.86555789729708,37.088671343767786],[-122.8673898155377,37.3324341952235],[-122.96318056822776,37.33193196725721],[-122.96915039986946,37.99873320994975]]],\"type\":\"Polygon\"},\"properties\":{\"anchor_lat\":37.784262651527904,\"continent\":\"North America\",\"anchor_pop_max\":3450000.0,\"anchor_country\":\"United States of America\",\"extract-size\":2.9363229E8,\"countries\":[\"United States of America\"],\"city_count\":7.0,\"anchor_iso_a3\":\"USA\",\"feature_type\":\"city_cluster\",\"iso_a3s\":[\"USA\"],\"filename\":\"san-francisco-united-states-of-america.pmtiles\",\"anchor_lon\":-122.39959956304557,\"dbscan_cluster_id\":78.0,\"extract-size-string\":\"294 MB\",\"total_pop_max\":8129114.0,\"anchor_city\":\"San Francisco\",\"city_names\":[\"Berkeley\",\"Oakland\",\"San Francisco\",\"San Jose\",\"San Mateo\",\"Santa Rosa\",\"Vallejo\"]},\"type\":\"Feature\"}"
    val adapter = GeoJsonObjectMoshiAdapter()
    val feature = adapter.fromJson(geojson) as Feature

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        OfflineMapExtractDetails(feature, { _,_ -> }, { _ -> }, false)
        OfflineMapExtractDetails (feature, { _,_ -> }, { _ -> }, true)
    }
}
