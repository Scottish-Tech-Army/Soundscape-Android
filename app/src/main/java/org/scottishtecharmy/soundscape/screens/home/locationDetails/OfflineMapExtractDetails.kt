package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.screens.home.home.ExtractDetails
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.ui.theme.tinyPadding

@Composable
fun OfflineMapExtractDetails(
    extract: Feature,
    downloadExtract: (String, Feature, Boolean) -> Unit,
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
        modifier = modifier.verticalScroll(rememberScrollState()),
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
            downloadExtract = { wifiOnly -> downloadExtract(details.localName, extract, wifiOnly) },
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
            showMap = showMap,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.0f),
            overlayGeoJson = adapter.toJson(fc)
        )
    }
}

@Composable
private fun MapExtractButtonsSection(
    downloadExtract: (wifiOnly : Boolean) -> Unit,
    deleteExtract: () -> Unit,
    local: Boolean
) {
    val wifiOnly = remember { mutableStateOf(true) }
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.none),
    ) {
        if(local) {
            IconWithTextButton(
                icon = Icons.Filled.Delete,
                text = stringResource(R.string.offline_map_details_delete),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDelete"),
                onClick = deleteExtract
            )
        } else {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .toggleable(
                        value = wifiOnly.value,
                        onValueChange = { wifiOnly.value = it },
                        role = Role.Checkbox,
                    )
                    .tinyPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.offline_map_wifi_only),
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp)
                Checkbox(
                    checked = wifiOnly.value,
                    onCheckedChange = null
                )
            }
            IconWithTextButton(
                icon = Icons.Filled.Download,
                text = stringResource(R.string.offline_map_details_download),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .testTag("offlineMapDownload"),
                onClick = { downloadExtract(wifiOnly.value) }
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
            text = stringResource(R.string.offline_map_details_name).format(details.localName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if(details.alternateName.isNotEmpty())
            Text(
                text = details.alternateName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        val size = extract.properties?.get("extract-size-string")
        if (size != null) {
            val sizeString = if (local)
                stringResource(R.string.offline_map_details_size_on_phone).format(size)
            else
                stringResource(R.string.offline_map_details_size_on_server).format(size)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if (local)
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
                    text = stringResource(R.string.offline_map_details_city_list).format(cities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if(details.alternateCities.isNotEmpty() &&  details.localCities.isNotEmpty())
                Text(
                    text = stringResource(R.string.offline_map_details_alternate_city_list).format(details.alternateCities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OfflineMapExtractDetailsPreview() {

    val geojson = "{\"type\": \"Feature\", \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[139.81691185318002, 37.56831909920214], [139.81452073905476, 37.84856243009939], [139.86499116717772, 37.84884359203526], [139.86341400048522, 38.03211295423315], [141.00240215693833, 38.03278598895171], [140.99885371261206, 37.500096384164024], [141.45101968072916, 37.49735188393407], [141.4398807979843, 36.59634687676648], [141.20879156854664, 36.59795080805193], [141.20471272846316, 36.147418099935344], [141.08364793550157, 36.14807554122666], [141.08328622945226, 36.099285004209975], [139.97284530020468, 36.09951739243942], [139.96894767976792, 36.66802927279343], [139.7042382614323, 36.66653760928801], [139.69471400090455, 37.56755097639593], [139.81691185318002, 37.56831909920214]]]}, \"properties\": {\"name\": \"Iwaki\", \"iso_a2\": \"JP\", \"feature_type\": \"city_cluster\", \"name_local\": \"いわき市\", \"city_names\": [\"Hitachi\", \"Nihommatsu\", \"Kōriyama\", \"Hitachi-ota\", \"Sukagawa\", \"Shirakawa\", \"Iwaki\"], \"city_local_names\": [\"日立\", \"二本松\", \"郡山市\", \"常陸太田\", \"須賀川市\", \"白河\", \"いわき市\"], \"extract-size\": 87491126, \"extract-size-string\":\"0.4GB\", \"filename\": \"iwaki-jp.pmtiles\"}}"
    val adapter = GeoJsonObjectMoshiAdapter()
    val feature = adapter.fromJson(geojson) as Feature

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        OfflineMapExtractDetails(feature, { _,_,_ -> }, { _ -> }, false)
        OfflineMapExtractDetails (feature, { _,_,_ -> }, { _ -> }, true)
    }
}
