package org.scottishtecharmy.soundscape.screens.home.offlinemaps

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.offline_map_download_cancelled
import org.scottishtecharmy.soundscape.resources.offline_map_download_complete
import org.scottishtecharmy.soundscape.resources.offline_map_download_error
import org.scottishtecharmy.soundscape.resources.offline_map_details_title
import org.scottishtecharmy.soundscape.resources.offline_maps_already_downloaded
import org.scottishtecharmy.soundscape.resources.offline_maps_caching
import org.scottishtecharmy.soundscape.resources.offline_maps_downloading
import org.scottishtecharmy.soundscape.resources.offline_maps_loading_manifest
import org.scottishtecharmy.soundscape.resources.offline_maps_manifest_failed
import org.scottishtecharmy.soundscape.resources.offline_maps_nearby
import org.scottishtecharmy.soundscape.resources.offline_maps_storage
import org.scottishtecharmy.soundscape.resources.offline_maps_title
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.talkbackHidden
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedOfflineMapsScreen(
    uiState: OfflineMapsUiState,
    downloadState: StateFlow<DownloadStateCommon>,
    onBack: () -> Unit,
    onDownload: (String, Feature) -> Unit,
    onDelete: (Feature) -> Unit,
    onCancelDownload: () -> Unit,
    preferencesProvider: org.scottishtecharmy.soundscape.preferences.PreferencesProvider? = null,
    modifier: Modifier = Modifier,
) {
    val progress = remember { mutableIntStateOf(0) }
    val progressForBar = remember { mutableIntStateOf(0) }
    val downloading = remember { mutableStateOf(false) }
    val caching = remember { mutableStateOf(false) }
    var userMessage by remember { mutableStateOf("") }

    LaunchedEffect(downloadState) {
        downloadState.collect { state ->
            when (state) {
                is DownloadStateCommon.Idle -> {
                    downloading.value = false
                    caching.value = false
                }
                is DownloadStateCommon.Caching -> {
                    caching.value = true
                    downloading.value = true
                    progress.intValue = 0
                    progressForBar.intValue = 0
                }
                is DownloadStateCommon.Downloading -> {
                    if (caching.value) caching.value = false
                    if (progressForBar.intValue != state.progress / 10) {
                        progressForBar.intValue = state.progress / 10
                    }
                    if (progress.intValue != state.progress) {
                        progress.intValue = state.progress
                    }
                    if (!downloading.value) downloading.value = true
                }
                is DownloadStateCommon.Success -> {
                    downloading.value = false
                    caching.value = false
                }
                is DownloadStateCommon.Error -> {
                    downloading.value = false
                    caching.value = false
                }
                is DownloadStateCommon.Canceled -> {
                    downloading.value = false
                    caching.value = false
                }
            }
        }
    }

    OfflineMapsScreenContent(
        uiState = uiState,
        progressPrecise = progress.intValue,
        progressForBar = progressForBar.intValue,
        downloading = downloading.value,
        caching = caching.value,
        onBack = onBack,
        onDownload = onDownload,
        onDelete = onDelete,
        onCancelDownload = onCancelDownload,
        preferencesProvider = preferencesProvider,
        modifier = modifier,
    )
}

@Composable
fun OfflineMapsScreenContent(
    uiState: OfflineMapsUiState,
    progressPrecise: Int,
    progressForBar: Int,
    downloading: Boolean,
    caching: Boolean,
    onBack: () -> Unit,
    onDownload: (String, Feature) -> Unit,
    onDelete: (Feature) -> Unit,
    onCancelDownload: () -> Unit,
    preferencesProvider: org.scottishtecharmy.soundscape.preferences.PreferencesProvider? = null,
    modifier: Modifier = Modifier,
) {
    val extractDetailsFeature = remember { mutableStateOf<Feature?>(null) }
    val localExtractDetails = remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            FlexibleAppBar(
                title = if (extractDetailsFeature.value != null)
                    stringResource(Res.string.offline_map_details_title)
                else
                    stringResource(Res.string.offline_maps_title),
                leftSide = {
                    IconWithTextButton(
                        text = if (downloading) stringResource(Res.string.general_alert_cancel)
                        else stringResource(Res.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("appBarLeft"),
                    ) {
                        when {
                            extractDetailsFeature.value != null -> extractDetailsFeature.value = null
                            downloading -> onCancelDownload()
                            else -> onBack()
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            extractDetailsFeature.value != null -> {
                SharedOfflineMapExtractDetails(
                    extract = extractDetailsFeature.value!!,
                    downloadExtract = { name, feature ->
                        onDownload(name, feature)
                        extractDetailsFeature.value = null
                    },
                    deleteExtract = { feature ->
                        onDelete(feature)
                        extractDetailsFeature.value = null
                    },
                    local = localExtractDetails.value,
                    preferencesProvider = preferencesProvider,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
            downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (caching) Res.string.offline_maps_caching
                            else Res.string.offline_maps_downloading,
                            uiState.downloadingExtractName,
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.talkbackLive(),
                    )
                    LinearProgressIndicator(
                        progress = { progressForBar.toFloat() / 100.0f },
                        modifier = Modifier.padding(spacing.medium),
                    )
                    Text(
                        text = "${progressPrecise / 10.0}%",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.talkbackHidden(),
                    )
                }
            }
            else -> {
                OfflineMapsList(
                    uiState = uiState,
                    onSelectDownloaded = { feature ->
                        extractDetailsFeature.value = feature
                        localExtractDetails.value = true
                    },
                    onSelectNearby = { feature ->
                        extractDetailsFeature.value = feature
                        localExtractDetails.value = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

@Composable
private fun OfflineMapsList(
    uiState: OfflineMapsUiState,
    onSelectDownloaded: (Feature) -> Unit,
    onSelectNearby: (Feature) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        for (storage in uiState.storages) {
            if (storage.path == uiState.currentPath) {
                Text(
                    text = stringResource(
                        Res.string.offline_maps_storage,
                        storage.description,
                        storage.availableString,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .mediumPadding(),
                )
            }
        }

        val downloaded = uiState.downloadedExtracts
        if (downloaded != null && downloaded.features.isNotEmpty()) {
            SectionHeader(text = stringResource(Res.string.offline_maps_already_downloaded))
            Column(
                modifier = Modifier.semantics {
                    collectionInfo = CollectionInfo(
                        rowCount = downloaded.features.size,
                        columnCount = 1,
                    )
                },
            ) {
                for ((index, extract) in downloaded.features.withIndex()) {
                    OfflineExtractRow(
                        extract = extract,
                        row = index,
                        onSelect = { onSelectDownloaded(extract) },
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(spacing.small))
        SectionHeader(text = stringResource(Res.string.offline_maps_nearby))
        val nearby = uiState.nearbyExtracts
        if (nearby != null) {
            Column(
                modifier = Modifier.semantics {
                    collectionInfo = CollectionInfo(
                        rowCount = nearby.features.size,
                        columnCount = 1,
                    )
                },
            ) {
                for ((index, extract) in nearby.features.withIndex()) {
                    OfflineExtractRow(
                        extract = extract,
                        row = index,
                        onSelect = { onSelectNearby(extract) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(
                    if (uiState.manifestError) Res.string.offline_maps_manifest_failed
                    else Res.string.offline_maps_loading_manifest,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .talkbackLive()
                    .mediumPadding(),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = spacing.targetSize),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(spacing.tiny)
                .semantics { heading() },
        )
    }
}

@Composable
private fun OfflineExtractRow(
    extract: Feature,
    row: Int,
    onSelect: () -> Unit,
) {
    val details = remember(extract) { ExtractDetails(extract) }
    val size = extract.properties?.get("extract-size-string") ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { collectionItemInfo = CollectionItemInfo(row, 0, 0, 0) }
            .clickable(role = Role.Button) { onSelect() }
            .padding(spacing.extraSmall),
        horizontalArrangement = SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .padding(spacing.small)
                .align(Alignment.CenterVertically)
                .weight(1f),
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
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}
