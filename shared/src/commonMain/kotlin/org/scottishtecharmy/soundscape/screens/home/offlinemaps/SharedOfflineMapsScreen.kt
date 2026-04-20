package org.scottishtecharmy.soundscape.screens.home.offlinemaps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedOfflineMapsScreen(
    nearbyExtracts: List<Feature>,
    downloadedPaths: List<String>,
    downloadState: StateFlow<DownloadStateCommon>,
    onBack: () -> Unit,
    onDownload: (Feature) -> Unit,
    onDelete: (String) -> Unit,
    onCancelDownload: () -> Unit,
) {
    val dlState by downloadState.collectAsState()

    Scaffold(
        topBar = {
            CustomAppBar(
                title = "Offline Maps",
                onNavigateUp = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Download progress
            when (val state = dlState) {
                is DownloadStateCommon.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 1000f },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium),
                    )
                    Text(
                        text = "Downloading... ${state.progress / 10}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = spacing.medium),
                    )
                }
                is DownloadStateCommon.Caching -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium))
                    Text(
                        text = "Preparing download...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = spacing.medium),
                    )
                }
                is DownloadStateCommon.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(spacing.medium),
                    )
                }
                is DownloadStateCommon.Success -> {
                    Text(
                        text = "Download complete",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(spacing.medium),
                    )
                }
                else -> {}
            }

            // Downloaded extracts section
            if (downloadedPaths.isNotEmpty()) {
                Text(
                    text = "Downloaded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(spacing.medium),
                )
                for (path in downloadedPaths) {
                    val name = path.substringAfterLast("/").removeSuffix(".pmtiles")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.medium, vertical = spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconWithTextButton(
                            icon = Icons.Filled.Delete,
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.defaultMinSize(minHeight = spacing.targetSize),
                        ) {
                            onDelete(path)
                        }
                    }
                    HorizontalDivider()
                }
            }

            // Available extracts section
            if (nearbyExtracts.isNotEmpty()) {
                Text(
                    text = "Available Nearby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(spacing.medium),
                )
                for (feature in nearbyExtracts) {
                    val props = feature.properties ?: continue
                    val name = (props["name"] as? String) ?: (props["name_local"] as? String) ?: continue
                    val filename = (props["filename"] as? String) ?: continue
                    val isDownloaded = downloadedPaths.any {
                        it.endsWith(filename.substringAfterLast("/"))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                if (!isDownloaded) onDownload(feature)
                            }
                            .padding(horizontal = spacing.medium, vertical = spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val type = props["feature_type"] as? String
                            if (type != null) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (!isDownloaded) {
                            IconWithTextButton(
                                icon = Icons.Filled.CloudDownload,
                                text = "Download",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.defaultMinSize(minHeight = spacing.targetSize),
                            ) {
                                onDownload(feature)
                            }
                        } else {
                            Text(
                                text = "Downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            if (nearbyExtracts.isEmpty() && downloadedPaths.isEmpty()) {
                Text(
                    text = "Loading map data...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(spacing.large),
                )
            }
        }
    }
}
