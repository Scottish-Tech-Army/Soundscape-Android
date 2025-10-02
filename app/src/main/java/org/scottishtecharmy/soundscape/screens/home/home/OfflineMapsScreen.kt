package org.scottishtecharmy.soundscape.screens.home.home

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsUiState
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsViewModel

@Composable
fun OfflineMapsScreenVM(
    navController: NavHostController,
    modifier: Modifier,
    viewModel: OfflineMapsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localActivity = LocalActivity.current as MainActivity
    OfflineMapsScreen(
        navController = navController,
        uiState = uiState,
        modifier = modifier,
        extractSelected = { viewModel.download(localActivity, it) }
    )
}

@Composable
fun OfflineMapsScreen(
    navController: NavHostController,
    uiState: OfflineMapsUiState,
    modifier: Modifier,
    extractSelected: (Feature) -> Unit) {

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title ="Offline Maps",
                navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                onNavigateUp = {
                    navController.navigateUp()
                },
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
                        text = "Loading list of offline maps...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.targetSize),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = SpaceBetween,
                    ) {
                        Text(
                            text = "Free space:",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    if (uiState.internalStorage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.targetSize)
                                .padding(spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = SpaceBetween,
                        ) {
                            Text(
                                text = "Internal storage",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text(
                                text = uiState.internalStorage.availableString,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                    if (uiState.externalStorages.isNotEmpty()) {
                        LazyColumn(
                            modifier = modifier.fillMaxWidth(),
                        ) {
                            itemsIndexed(uiState.externalStorages) { index, storage ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.extraSmall),
                                    horizontalArrangement = SpaceBetween,
                                ) {
                                    Text(
                                        text = "External",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                    Text(
                                        text = storage.availableString,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
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
                            text = "Nearby offline maps:",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    if (uiState.nearbyExtracts != null) {
                        LazyColumn(
                            modifier = modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                        ) {
                            itemsIndexed(uiState.nearbyExtracts.features) { index, extract ->
                                var name = ""
                                val description = StringBuilder()
                                if (extract.properties?.get("feature_type") == "country") {
                                    name = extract.properties?.get("name").toString()
                                } else if (extract.properties?.get("feature_type") == "city_cluster") {
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
                                val size = extract.properties?.get("extract-size-string")
                                if (size != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                extractSelected(extract)
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
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
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
        "/path/to/storage",
        isExternal = true,
        isPrimary = false,
        128*1024*1024*1024L,
        88*1024*1024*1024L,
        "88000 MB",
        90*1024*1024*1024L
    )

    OfflineMapsScreen(
        rememberNavController(),
        OfflineMapsUiState(
            isLoading = false,
            nearbyExtracts = fc,
            internalStorage = StorageUtils.StorageSpace(
                "/path/to/storage",
                isExternal = false,
                isPrimary = true,
                64*1024*1024*1024L,
                22*1024*1024*1024L,
                "22000 MB",
                23*1024*1024*1024L
            ),
            externalStorages = listOf(externalStorage)
        ),
        modifier = Modifier,
        extractSelected = {_ -> }
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
            internalStorage = null,
            externalStorages = emptyList()
        ),
        modifier = Modifier,
        extractSelected = {_ -> }
    )
}
