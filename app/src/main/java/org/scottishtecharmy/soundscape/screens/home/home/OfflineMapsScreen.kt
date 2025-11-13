package org.scottishtecharmy.soundscape.screens.home.home

import android.widget.Toast
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.screens.home.locationDetails.OfflineMapExtractDetails
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.talkbackHidden
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.DownloadState
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsUiState
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun OfflineMapsScreenVM(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: OfflineMapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = remember { mutableIntStateOf(0) }
    val progressForBar = remember { mutableIntStateOf(0) }
    val downloading = remember { mutableStateOf(false) }
    val caching = remember { mutableStateOf(false) }
    val userMessage = remember { mutableStateOf("") }
    val context = LocalContext.current

    // In your Composable or Activity/Fragment
    LaunchedEffect(Unit) {
        var lastUpdateTime = 0L
        val throttleDuration = 5.seconds.inWholeMilliseconds

        viewModel.downloadState.collect { state ->
            when (state) {
                is DownloadState.Idle -> {
                    println("Idle!")
                    downloading.value = false
                    caching.value = false
                }
                is DownloadState.Caching -> {
                    caching.value = true
                    downloading.value = true
                }
                is DownloadState.Downloading -> {
                    val currentTime = System.currentTimeMillis()
                    if(caching.value)
                        caching.value = false

                    if((progressForBar.intValue != state.progress / 10)  &&
                       (currentTime - lastUpdateTime) > throttleDuration) {
                        progressForBar.intValue = (state.progress / 10)
                        lastUpdateTime = currentTime
                    }
                    if (progress.intValue != state.progress) {
                        progress.intValue = state.progress
                    }
                    if(!downloading.value)
                        downloading.value = true
                }
                is DownloadState.Success -> {
                    println("Success!")
                    downloading.value = false
                    caching.value = false
                    userMessage.value = "Download complete"
                    viewModel.refreshExtracts()
                }
                is DownloadState.Error -> {
                    println("Error!")
                    userMessage.value = state.message
                    downloading.value = false
                    caching.value = false
                }
                is DownloadState.Canceled -> {
                    println("Cancelled!")
                    downloading.value = false
                    caching.value = false
                    userMessage.value = "Download cancelled"
                }
            }
        }
    }
    // Display error message if it exists
    LaunchedEffect(userMessage.value) {
        if(userMessage.value.isNotEmpty()) {
            Toast.makeText(context, userMessage.value, Toast.LENGTH_LONG).show()
            userMessage.value = ""
        }
    }

    OfflineMapsScreen(
        navController = navController,
        uiState = uiState,
        progressPrecise = progress.intValue,
        progressForBar = progressForBar.intValue,
        downloading = downloading.value,
        caching = caching.value,
        modifier = modifier,
        downloadExtract = { name, feature -> viewModel.download(name, feature) },
        deleteExtract = { feature -> viewModel.delete( feature) },
        cancelDownload = { viewModel.cancelDownload() }
    )
}

class ExtractDetails(
    extract: Feature
) {
    var localName = ""
    var alternateName = ""

    var localCities = ""
    var alternateCities = ""

    init {
        val namePropLocal = extract.properties?.get("name_local")
        val nameProp = extract.properties?.get("name")
        if(namePropLocal != null) {
            localName = namePropLocal.toString()
            alternateName = nameProp.toString()
        }
        else
            localName = nameProp.toString()

        val localCitiesProps = extract.properties?.get("city_local_names")
        val localCitiesBuilder = StringBuilder()
        if (localCitiesProps != null) {
            for (city in localCitiesProps as List<*>) {
                if (city != localCitiesProps.first())
                    localCitiesBuilder.append(", ")
                localCitiesBuilder.append(city)
            }
            localCities = localCitiesBuilder.toString()
        }

        val cities = extract.properties?.get("city_names")
        val citiesBuilder = StringBuilder()
        if (cities != null) {
            for (city in cities as List<*>) {
                if (city != cities.first())
                    citiesBuilder.append(", ")
                citiesBuilder.append(city)
            }
            alternateCities = citiesBuilder.toString()
        }
    }
}


@Composable
fun OfflineExtract(
    extract: Feature,
    extractSelected: (String, Feature) -> Unit,
    row: Int)
{
    val details = remember(extract) { ExtractDetails(extract) }
    val size = extract.properties?.get("extract-size-string")
    if (size != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    collectionItemInfo = CollectionItemInfo(row, 0, 0, 0)
                }
                .clickable(role = Role.Button) {
                    extractSelected(details.localName, extract)
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
                    text = details.localName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (details.alternateName.isNotEmpty()) {
                    Text(
                        text = details.alternateName,
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
    progressPrecise: Int,
    progressForBar: Int,
    downloading: Boolean,
    caching: Boolean,
    downloadExtract: (String, Feature) -> Unit,
    deleteExtract: (Feature) -> Unit,
    cancelDownload: () -> Unit
)
{
    val extractDetailsFeature = remember { mutableStateOf(null as Feature?) }
    val localExtractDetails =  remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if(extractDetailsFeature.value != null) {
            // Swipe back exits offset details page
            extractDetailsFeature.value = null
        } else if(!downloading) {
            // Ignore any back swipes when downloading content. Instead we should probably have a dialog
            // pop up at this point to check whether the user would really like to cancel the download.
            navController.navigateUp()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FlexibleAppBar(
                title = if(extractDetailsFeature.value != null)
                            stringResource(R.string.offline_map_details_title)
                        else
                            stringResource(R.string.offline_maps_title),
                leftSide = {
                    IconWithTextButton(
                        text = if(downloading) stringResource(R.string.general_alert_cancel) else stringResource(R.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft")
                    ) {
                        if(extractDetailsFeature.value != null) {
                            extractDetailsFeature.value = null
                        }
                        else if(downloading) {
                            cancelDownload()
                        }
                        else {
                            navController.navigateUp()
                        }
                    }
                }
            )
        },

        content = { padding ->
            if(extractDetailsFeature.value != null) {
                OfflineMapExtractDetails(
                    extractDetailsFeature.value!!,
                    { name, feature ->
                        downloadExtract(name, feature)
                        extractDetailsFeature.value = null
                    },
                    {
                        deleteExtract(it)
                        extractDetailsFeature.value = null
                    },
                    localExtractDetails.value,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                )
            } else if (downloading) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            if(caching)
                                R.string.offline_maps_caching
                            else
                                R.string.offline_maps_downloading).format(uiState.downloadingExtractName),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.talkbackLive()
                    )
                    LinearProgressIndicator (
                        progress = { progressForBar.toFloat() / 100.0f },
                        modifier = Modifier
                            .padding(spacing.medium)
                    )
                    Text(
                        text = "${progressPrecise / 10.0}%",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.talkbackHidden()
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
                        if(storage.path == uiState.currentPath) {
                            Text(
                                text = stringResource(R.string.offline_maps_storage).format(storage.description, storage.availableString),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .mediumPadding()
                            )
                        }
                    }

                    if((uiState.downloadedExtracts != null) && (uiState.downloadedExtracts.features.isNotEmpty())) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.targetSize),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.offline_maps_already_downloaded),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .semantics { heading() }
                            )
                        }
                        Column(
                            modifier = Modifier.semantics {
                                collectionInfo =
                                    CollectionInfo(
                                        rowCount = uiState.downloadedExtracts.features.size,
                                        columnCount = 1
                                    )
                            }
                        ) {
                            for ((index, extract) in uiState.downloadedExtracts.features.withIndex()) {
                                OfflineExtract(
                                    extract,
                                    { _, extract ->
                                        extractDetailsFeature.value = extract
                                        localExtractDetails.value = true
                                    },
                                    index
                                )
                            }
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
                            text = stringResource(R.string.offline_maps_nearby),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .semantics { heading() }
                        )
                    }
                    if (uiState.nearbyExtracts != null) {
                        Column(
                            modifier = Modifier.semantics {
                                collectionInfo =
                                    CollectionInfo(
                                        rowCount = uiState.nearbyExtracts.features.size,
                                        columnCount = 1
                                    )
                            }
                        ) {
                            for((index, extract) in uiState.nearbyExtracts.features.withIndex()) {
                                OfflineExtract(
                                    extract,
                                    { _, extract ->
                                        extractDetailsFeature.value = extract
                                        localExtractDetails.value = false
                                    },
                                    index
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.offline_maps_loading_manifest),
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
    properties["name"] = "Bristol"
    properties["extract-size"] = 120*1024*1024.0
    properties["extract-size-string"] = "120 MB"
    properties["city_names"] = listOf("Bristol", "Bath", "Cardiff", "Birmingham", "Exeter", "Northampton", "Chesterfield")
    city.properties = properties

    val state = Feature()
    val properties3: HashMap<String, Any?> = hashMapOf()
    properties3["feature_type"] = "admin1"
    properties3["name"] = "Iwate"
    properties3["name_local"] = "岩手県"
    properties3["country_name"] = "Japan"
    properties3["country_name_local"] = "日本"
    properties3["iso_a2"] = "JA"
    properties3["extract-size"] = 0.5*1024*1024*1024
    properties3["extract-size-string"] = "0.4 GB"
    state.properties = properties3

    val state1 = Feature()
    val properties4: HashMap<String, Any?> = hashMapOf()
    properties4["feature_type"] = "city_cluster"
    properties4["name"] = "Iwaki"
    properties4["name_local"] = "いわき市"
    properties4["country_name"] = "Japan"
    properties4["country_name_local"] = "日本"
    properties4["city_names"] = listOf("Hitachi","Nihommatsu","Kōriyama","Hitachi-ota","Sukagawa","Shirakawa","Iwaki")
    properties4["city_names_local"] = listOf("日立","二本松","郡山市","常陸太田","須賀川市","白河","いわき市")
    properties4["iso_a2"] = "JA"
    properties4["extract-size"] = 0.5*1024*1024*1024
    properties4["extract-size-string"] = "0.4 GB"
    state1.properties = properties4

    val country = Feature()
    val properties2: HashMap<String, Any?> = hashMapOf()
    properties2["feature_type"] = "country"
    properties2["name"] = "United Kingdom"
    properties2["extract-size"] = 3.4*1024*1024*1024
    properties2["extract-size-string"] = "3.4 GB"
    country.properties = properties2

    fc.addFeature(city)
    fc.addFeature(state)
    fc.addFeature(state1)
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
        downloadingExtractName = "",
        nearbyExtracts = fc,
        downloadedExtracts = fc,
        currentPath = "/path/to/internal",
        storages = listOf(internalStorage, externalStorage),
    )

    OfflineMapsScreen(
        rememberNavController(),
        modifier = Modifier,
        uiState = uiState,
        progressForBar = 49,
        progressPrecise = 491,
        downloading = false,
        caching = false,
        downloadExtract = { _,_ -> },
        deleteExtract = { _ ->},
        cancelDownload = {}
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineMapsScreenDownloadingPreview() {

    OfflineMapsScreen(
        rememberNavController(),
        OfflineMapsUiState(
            nearbyExtracts = FeatureCollection(),
            storages = emptyList(),
            downloadingExtractName = "United Kingdom"
        ),
        modifier = Modifier,
        progressForBar = 49,
        progressPrecise = 491,
        downloading = true,
        caching = true,
        downloadExtract = { _,_ -> },
        deleteExtract = { _ ->},
        cancelDownload = {}
    )
}
