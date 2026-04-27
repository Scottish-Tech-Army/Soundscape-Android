package org.scottishtecharmy.soundscape.screens.home.offlinemaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.maplibre.spatialk.geojson.Position
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.offline_map_details_alternate_city_list
import org.scottishtecharmy.soundscape.resources.offline_map_details_city_list
import org.scottishtecharmy.soundscape.resources.offline_map_details_delete
import org.scottishtecharmy.soundscape.resources.offline_map_details_download
import org.scottishtecharmy.soundscape.resources.offline_map_details_name
import org.scottishtecharmy.soundscape.resources.offline_map_details_size_on_phone
import org.scottishtecharmy.soundscape.resources.offline_map_details_size_on_server
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedOfflineMapExtractDetails(
    extract: Feature,
    downloadExtract: (String, Feature) -> Unit,
    deleteExtract: (Feature) -> Unit,
    local: Boolean,
    modifier: Modifier = Modifier,
) {
    val details = remember(extract) { ExtractDetails(extract) }
    val extractGeometry = remember(extract) { extract.toMaplibreGeometry() }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        MapExtractTextsSection(
            extract = extract,
            details = details,
            local = local,
        )
        HorizontalDivider()
        MapExtractButtonsSection(
            deleteExtract = { deleteExtract(extract) },
            downloadExtract = { downloadExtract(details.localName, extract) },
            local = local,
        )

        PlatformMapContainer(
            beaconLocation = null,
            allowScrolling = true,
            routeData = null,
            mapCenter = LngLatAlt(),
            userLocation = null,
            userSymbolRotation = 0.0f,
            extractGeometry = extractGeometry,
            forceOnlineTiles = true,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f),
        )
    }
}

private fun ArrayList<LngLatAlt>.toPositionRing(): List<Position> =
    map { Position(it.longitude, it.latitude) }

private fun Feature.toMaplibreGeometry(): org.maplibre.spatialk.geojson.Geometry? {
    return when (val g = geometry) {
        is Polygon -> org.maplibre.spatialk.geojson.Polygon(
            g.coordinates.map { ring -> ring.toPositionRing() }
        )
        is MultiPolygon -> org.maplibre.spatialk.geojson.MultiPolygon(
            g.coordinates.map { polygon -> polygon.map { ring -> ring.toPositionRing() } }
        )
        else -> null
    }
}

@Composable
private fun MapExtractButtonsSection(
    downloadExtract: () -> Unit,
    deleteExtract: () -> Unit,
    local: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.none),
    ) {
        if (local) {
            IconWithTextButton(
                icon = Icons.Filled.Delete,
                text = stringResource(Res.string.offline_map_details_delete),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDelete"),
                onClick = deleteExtract,
            )
        } else {
            IconWithTextButton(
                icon = Icons.Filled.Download,
                text = stringResource(Res.string.offline_map_details_download),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDownload"),
                onClick = downloadExtract,
            )
        }
    }
}

@Composable
private fun MapExtractTextsSection(
    extract: Feature,
    details: ExtractDetails,
    local: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = stringResource(Res.string.offline_map_details_name, details.localName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (details.alternateName.isNotEmpty()) {
            Text(
                text = details.alternateName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        val size = extract.properties?.get("extract-size-string")
        if (size != null) {
            val sizeString = if (local)
                stringResource(Res.string.offline_map_details_size_on_phone, size.toString())
            else
                stringResource(Res.string.offline_map_details_size_on_server, size.toString())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if (local) {
                    Icon(
                        imageVector = Icons.Filled.SdStorage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = sizeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (extract.properties?.get("feature_type") == "city_cluster") {
            val cities = details.localCities.ifEmpty { details.alternateCities }
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
                    text = stringResource(Res.string.offline_map_details_city_list, cities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (details.alternateCities.isNotEmpty() && details.localCities.isNotEmpty()) {
                Text(
                    text = stringResource(
                        Res.string.offline_map_details_alternate_city_list,
                        details.alternateCities,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
