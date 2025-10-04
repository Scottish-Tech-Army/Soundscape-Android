package org.scottishtecharmy.soundscape.screens.home.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.StorageItem
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsUiState
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsViewModel

@Composable
fun OfflineMapsScreenVM(
    navController: NavHostController,
    downloadId: Long,
    modifier: Modifier,
    viewModel: OfflineMapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if(downloadId != -1L)
        viewModel.midDownload(downloadId)

    BackHandler(enabled = true) {
        // Ignore any back swipes when downloading content. Instead we should probably have a dialog
        // pop up at this point to check whether the user would really like to cancel the download.
        if(!uiState.isDownloading) {
            navController.navigateUp()
        }
    }

    OfflineMapsScreen(
        navController = navController,
        uiState = uiState,
        modifier = modifier,
        extractSelectedForDownload = { name, feature -> viewModel.download(name, feature) },
        localExtractSelected = { _, feature -> viewModel.delete( feature) },
        cancelDownload = { viewModel.cancelDownload() }
    )
}

@Composable
fun OfflineExtract(extract: Feature, extractSelected: (String, Feature) -> Unit) {
    var name: String
    val description = StringBuilder()
    val featureType = extract.properties?.get("feature_type")
    when (featureType) {
        "city_cluster" -> {
            name = extract.properties?.get("anchor_city").toString()
            val cities = extract.properties?.get("city_names")
            if (cities != null) {
                for (city in cities as List<*>) {
                    if (city != cities.first())
                        description.append(", ")
                    description.append(city)
                }
            }
        }

        else ->
            name = extract.properties?.get("name").toString()
    }
    val size = extract.properties?.get("extract-size-string")
    if (size != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    extractSelected(name, extract)
                }
                .padding(spacing.extraSmall),
            horizontalArrangement = SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .padding(spacing.small)
                    .align(Alignment.CenterVertically)
                    .weight(1F),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = size.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun OfflineMapsScreen(
    navController: NavHostController,
    uiState: OfflineMapsUiState,
    modifier: Modifier,
    extractSelectedForDownload: (String, Feature) -> Unit,
    localExtractSelected: (String, Feature) -> Unit,
    cancelDownload: () -> Unit)
{
    Scaffold(
        modifier = modifier,
        topBar = {
            FlexibleAppBar(
                title = "Offline Maps",
                leftSide = {
                    IconWithTextButton(
                        text = if(uiState.isDownloading) stringResource(R.string.general_alert_cancel) else stringResource(R.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft")
                    ) {
                        if(uiState.isDownloading)
                            cancelDownload()
                        else
                            navController.navigateUp()
                    }
                }
            )
        },

        content = { padding ->
            if (uiState.isLoading) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (uiState.isDownloading) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Downloading",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.downloadingExtractName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.downloadProgress}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                ) {

                    for(storage in uiState.storages) {
                        if(storage.path == uiState.currentPath)
                            StorageItem(
                                0,
                                storage.description,
                                storage.availableString + " free",
                                false,
                                { },
                                foregroundColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("storageButton"),
                            )
                    }

                    if(uiState.downloadedExtracts != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.targetSize),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = SpaceBetween,
                        ) {
                            Text(
                                text = "Maps already downloaded:",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        for(extract in uiState.downloadedExtracts.features) {
                            OfflineExtract(extract, localExtractSelected)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(spacing.small))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.targetSize),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = SpaceBetween,
                    ) {
                        Text(
                            text = "Nearby offline maps:",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    if (uiState.nearbyExtracts != null) {
                        for(extract in uiState.nearbyExtracts.features) {
                            OfflineExtract(extract, extractSelectedForDownload)
                        }
                    } else {
                        Text(
                            text = "Loading list of offline maps",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable 
fun OfflineMapsScreenPreview() {
    val fc = FeatureCollection()
    val city = Feature()
    val properties: HashMap<String, Any?> = hashMapOf()
    properties["feature_type"] = "city_cluster"
    properties["anchor_city"] = "Bristol"
    properties["extract-size"] = 120*1024*1024.0
    properties["extract-size-string"] = "120 MB"
    properties["city_names"] = listOf("Bristol", "Bath", "Cardiff", "Birmingham", "Exeter", "Northampton", "Chesterfield")
    city.properties = properties

    val country = Feature()
    val properties2: HashMap<String, Any?> = hashMapOf()
    properties2["feature_type"] = "country"
    properties2["name"] = "United Kingdom"
    properties2["extract-size"] = 3.4*1024*1024*1024
    properties2["extract-size-string"] = "3.4 GB"
    country.properties = properties2

    fc.addFeature(city)
    fc.addFeature(country)

    val externalStorage = StorageUtils.StorageSpace(
        "/path/to/external",
        description = "SD",
        isExternal = true,
        isPrimary = false,
        128*1024*1024*1024L,
        88*1024*1024*1024L,
        "88000 MB",
        90*1024*1024*1024L
    )
    val internalStorage = StorageUtils.StorageSpace(
        "/path/to/internal",
        description = "Internal shared storage",
        isExternal = false,
        isPrimary = false,
        64*1024*1024*1024L,
        22*1024*1024*1024L,
        "22000 MB",
        23*1024*1024*1024L
    )

    val uiState = OfflineMapsUiState(
        isLoading = false,
        isDownloading = false,
        downloadingExtractName = "",
        downloadProgress = 0,
        nearbyExtracts = fc,
        downloadedExtracts = fc,
        currentPath = "/path/to/internal",
        storages = listOf(internalStorage, externalStorage),
    )

    OfflineMapsScreen(
        rememberNavController(),
        modifier = Modifier,
        uiState = uiState,
        extractSelectedForDownload = {_,_ -> },
        localExtractSelected = {_,_ ->},
        cancelDownload = {}
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineMapsScreenLoadingPreview() {

    OfflineMapsScreen(
        rememberNavController(),
        OfflineMapsUiState(
            isLoading = true,
            nearbyExtracts = FeatureCollection(),
            storages = emptyList()
        ),
        modifier = Modifier,
        extractSelectedForDownload = {_,_ -> },
        localExtractSelected = {_,_ ->},
        cancelDownload = {}
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineMapsScreenDownloadingPreview() {

    OfflineMapsScreen(
        rememberNavController(),
        OfflineMapsUiState(
            isLoading = false,
            isDownloading = true,
            nearbyExtracts = FeatureCollection(),
            storages = emptyList(),
            downloadingExtractName = "United Kingdom"
        ),
        modifier = Modifier,
        extractSelectedForDownload = {_,_ -> },
        localExtractSelected = {_,_ ->},
        cancelDownload = {}
    )
}
