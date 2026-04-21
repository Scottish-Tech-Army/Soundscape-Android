package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.map
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun MainViewController() = ComposeUIViewController {
    val service = remember { IosSoundscapeService.getInstance() }
    val mgr = service.offlineMapManager

    App(
        flows = AppFlows(
            locationFlow = service.getLocationFlow(),
            directionFlow = service.getOrientationFlow(),
            homeState = service.homeState,
            markersUiState = service.markersUiState,
            routesUiState = service.routesUiState,
            placesNearbyUiState = service.placesNearbyUiState,
            offlineMapsNearbyExtracts = mgr.availableExtracts,
            offlineMapsDownloaded = mgr.downloadedExtracts,
            offlineMapsDownloadState = mgr.downloadState,
        ),
        callbacks = AppCallbacks(
            onStartBeacon = { lat, lng, name ->
                service.startBeacon(LngLatAlt(lng, lat), name)
            },
            onStopBeacon = {
                service.destroyBeacon()
            },
            onSpeak = { text ->
                service.speakCallout(text)
            },
            onSearch = { query -> service.search(query) },
            onSaveMarker = { desc -> service.saveMarker(desc) },
            onDeleteMarker = { markerId -> service.deleteMarker(markerId) },
            onSaveRoute = { name, desc, waypoints -> service.saveRoute(name, desc, waypoints) },
            onDeleteRoute = { routeId -> service.deleteRoute(routeId) },
            onMyLocation = { service.myLocation() },
            onWhatsAroundMe = { service.whatsAroundMe() },
            onAheadOfMe = { service.aheadOfMe() },
            onNearbyMarkers = { service.nearbyMarkers() },
            onPlacesNearbyClickFolder = { filter, title ->
                service.placesNearbyClickFolder(filter, title)
            },
            onPlacesNearbyClickBack = {
                service.placesNearbyClickBack()
            },
            onOfflineMapsRefresh = {
                mgr.refresh()
            },
            onOfflineMapsGetExtracts = { location ->
                mgr.getExtractsContaining(location)
            },
            onOfflineMapsDownload = { feature ->
                val props = feature.properties ?: return@AppCallbacks
                val filename = props["filename"] as? String ?: return@AppCallbacks
                val extractSize = (props["extract-size"] as? String)?.toDoubleOrNull()
                mgr.startDownload(filename, extractSize)
            },
            onOfflineMapsDelete = { path ->
                mgr.deleteExtract(path)
            },
            onOfflineMapsCancelDownload = {
                mgr.cancelDownload()
            },
        ),
    )
}
