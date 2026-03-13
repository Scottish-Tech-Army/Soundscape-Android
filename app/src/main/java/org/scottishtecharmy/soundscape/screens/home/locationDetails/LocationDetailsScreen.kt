package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.fromLatLng
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.home.home.generateOfflineMapScreenRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun generateLocationDetailsRoute(locationDescription: LocationDescription): String {
    // Generate JSON for the LocationDescription and append it to the route
    val json = GsonBuilder().create().toJson(locationDescription)
    return "${HomeRoutes.LocationDetails.route}/${URLEncoder.encode(json, StandardCharsets.UTF_8.toString())}"
}

@Composable
fun LocationDetailsScreen(
    locationDescription: LocationDescription,
    location : LngLatAlt?,
    heading: Float,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: LocationDetailsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // Check if this location already exists as a marker in the database
    val finalLocationDescription = remember(locationDescription) {
        if (locationDescription.databaseId == 0L) {
            val existingMarker = viewModel.getMarkerAtLocation(locationDescription.location)
            if (existingMarker != null) {
                locationDescription.copy(
                    databaseId = existingMarker.markerId,
                    name = existingMarker.name,
                    description = existingMarker.fullAddress
                )
            } else {
                locationDescription
            }
        } else {
            locationDescription
        }
    }

    LocationDetails(
        navController = navController,
        locationDescription = finalLocationDescription,
        createBeacon = { loc ->
            viewModel.startBeacon(loc, finalLocationDescription.name)
            navController.popBackStack(HomeRoutes.Home.route, false)
        },
        saveMarker = { description, successMessage, failureMessage, duplicateMessage ->
            viewModel.createMarker(
                description,
                successMessage,
                failureMessage,
                duplicateMessage)
            navController.popBackStack(HomeRoutes.Home.route, false)
        },
        deleteMarker = { id ->
            viewModel.deleteMarker(id)
            navController.popBackStack(HomeRoutes.MarkersAndRoutes.route, false)
        },
        enableStreetPreview = { loc ->
            viewModel.enableStreetPreview(loc)
            navController.popBackStack(HomeRoutes.Home.route, false)
        },
        getLocationDescription = { locationForDescription ->
            viewModel.getLocationDescription(locationForDescription) ?:
                LocationDescription(
                    name = context.getString(R.string.general_error_location_services_find_location_error),
                    location = locationForDescription
                )
        },
        shareLocation = { message, description ->
            viewModel.shareLocation(context, message, description)
            navController.popBackStack(HomeRoutes.Home.route, false)
        },
        offlineMaps = { locationDescription ->
            navController.navigate(generateOfflineMapScreenRoute(locationDescription))
        },
        showDialog = {
            viewModel.showDialog()
        },
        location = location,
        heading = heading,
        modifier = modifier,
    )
}

@Composable
fun LocationDetails(
    locationDescription : LocationDescription,
    navController: NavHostController,
    location: LngLatAlt?,
    heading: Float,
    createBeacon: (location: LngLatAlt) -> Unit,
    saveMarker: (
        description: LocationDescription,
        successMessage: String,
        failureMessage: String,
        duplicateMessage: String) -> Unit,
    deleteMarker: (objectId: Long) -> Unit,
    enableStreetPreview: (location: LngLatAlt) -> Unit,
    shareLocation: (message: String, description : LocationDescription) -> Unit,
    offlineMaps: (locationDescription: LocationDescription) -> Unit,
    showDialog: () -> Unit,
    getLocationDescription: (location: LngLatAlt) -> LocationDescription,
    modifier: Modifier = Modifier) {

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)
    val dialogState = remember { mutableStateOf(false) }
    val fullscreenMap = remember { mutableStateOf(false) }
    val description = remember { mutableStateOf(locationDescription) }

    if(dialogState.value) {
        SaveAndEditMarkerDialog(
            description.value,
            location,
            heading,
            saveMarker,
            deleteMarker,
            modifier,
            dialogState)
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                CustomAppBar(
                    title = stringResource(R.string.location_detail_title_default),
                    onNavigateUp = {
                        navController.popBackStack()
                   },
                )
            },
            content = { padding ->
                if (fullscreenMap.value) {
                    MapContainerLibre(
                        beaconLocation = description.value.location,
                        allowScrolling = true,
                        onMapLongClick = { latLong ->
                            val clickLocation = fromLatLng(latLong)
                            val ld = getLocationDescription(clickLocation)

                            // This effectively replaces the current screen with the new one
                            navController.navigate(generateLocationDetailsRoute(ld)) {
                                println("entry: ${navController.currentBackStackEntry?.destination?.route}")
                                popUpTo(
                                    navController.currentBackStackEntry?.destination?.route
                                        ?: return@navigate
                                ) {
                                    inclusive = true
                                }
                                launchSingleTop = true // Prevents multiple instances of Home
                            }
                            true
                        },
                        // Center on the beacon
                        mapCenter = description.value.location,
                        userLocation = location ?: LngLatAlt(),
                        userSymbolRotation = heading,
                        routeData = null,
                        modifier = modifier.fillMaxSize(),
                        showMap = showMap
                    )
                } else {
                    Column(
                        modifier =
                            Modifier
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surface),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        LocationDescriptionTextsSection(
                            locationDescription = description.value,
                            userLocation = location
                        )
                        HorizontalDivider()
                        LocationDescriptionButtonsSection(
                            createBeacon = createBeacon,
                            locationDescription = description.value,
                            enableStreetPreview = enableStreetPreview,
                            shareLocation = shareLocation,
                            offlineMaps = offlineMaps,
                            dialogState = dialogState,
                            showDialog = showDialog
                        )

                        MapContainerLibre(
                            beaconLocation = description.value.location,
                            allowScrolling = true,
                            onMapLongClick = { latLong ->
                                val clickLocation = fromLatLng(latLong)
                                val ld = getLocationDescription(clickLocation)

                                // This effectively replaces the current screen with the new one
                                navController.navigate(generateLocationDetailsRoute(ld)) {
                                    println("entry: ${navController.currentBackStackEntry?.destination?.route}")
                                    popUpTo(
                                        navController.currentBackStackEntry?.destination?.route
                                            ?: return@navigate
                                    ) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true // Prevents multiple instances of Home
                                }
                                true
                            },
                            // Center on the beacon
                            mapCenter = description.value.location,
                            userLocation = location ?: LngLatAlt(),
                            userSymbolRotation = heading,
                            routeData = null,
                            modifier = modifier.fillMaxWidth().aspectRatio(1.0f),
                            showMap = showMap
                        )
                    }
                }
            },
            floatingActionButton = {
                if(showMap) FullScreenMapFab(fullscreenMap)
            }
        )
    }
}

@Composable
private fun LocationDescriptionButtonsSection(
    createBeacon: (location: LngLatAlt) -> Unit,
    locationDescription: LocationDescription,
    enableStreetPreview: (location: LngLatAlt) -> Unit,
    shareLocation: (message: String, locationDescription : LocationDescription) -> Unit,
    offlineMaps: (locationDescription: LocationDescription) -> Unit,
    dialogState: MutableState<Boolean>,
    showDialog: () -> Unit
) {
    // Parse markdown only once, not on every recomposition
    val shareMessageResource = stringResource(R.string.universal_links_marker_share_message)
    val shareMessage = remember(shareMessageResource) {
        val parser: Parser = Parser.builder().build()
        val document: Node? = parser.parse(shareMessageResource)
        val renderer = HtmlRenderer.builder().build()
        AnnotatedString.fromHtml(
            htmlString = renderer.render(document),
            linkStyles = TextLinkStyles(
                style = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                )
            )
        ).text
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.none),
        modifier = Modifier.fillMaxWidth()
    ) {
        IconWithTextButton(
            icon = Icons.Filled.LocationOn,
            text = stringResource(R.string.location_detail_action_beacon),
            talkbackHint = stringResource(R.string.location_detail_action_beacon_hint),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.targetSize)
                .fillMaxWidth()
                .testTag("locationDetailsStartBeacon")
        ) {
            createBeacon(locationDescription.location)
        }

        if(locationDescription.databaseId != 0L) {
            IconWithTextButton(
                icon = Icons.Filled.EditLocation,
                text = stringResource(R.string.markers_edit_screen_title_edit),
                talkbackHint = stringResource(R.string.location_detail_action_edit_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsEditMarker")
            ) {
                dialogState.value = true
            }
        } else {
            IconWithTextButton(
                icon = Icons.Filled.AddLocation,
                text = stringResource(R.string.universal_links_alert_action_marker),
                talkbackHint = stringResource(R.string.location_detail_action_save_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsSaveAsMarker")
            ) {
                showDialog()
                dialogState.value = true
            }
        }

        IconWithTextButton(
            icon = Icons.Filled.Navigation,
            text = stringResource(R.string.preview_title),
            talkbackHint = stringResource(R.string.location_detail_action_preview_hint),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.targetSize)
                .fillMaxWidth()
                .testTag("locationDetailsStreetPreview")
        ) {
            enableStreetPreview(locationDescription.location)
        }

        IconWithTextButton(
            icon = Icons.Filled.ShareLocation,
            text = stringResource(R.string.share_title),
            talkbackHint = stringResource(R.string.location_detail_action_share_hint),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.targetSize)
                .fillMaxWidth()
                .testTag("locationDetailsShare")
        ) {
            shareLocation(shareMessage, locationDescription)
        }

        IconWithTextButton(
            icon = Icons.Rounded.Download,
            text = stringResource(R.string.offline_maps_nearby),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.targetSize)
                .fillMaxWidth()
                .testTag("locationDetailsOfflineMaps")
        ) {
            offlineMaps(locationDescription)
        }
    }
}

@Composable
private fun LocationDescriptionTextsSection(
    locationDescription: LocationDescription,
    userLocation: LngLatAlt?
) {
    val context = LocalContext.current
    val distanceString = remember(userLocation) {
        // If the location changes, recalculate the distance string
        if(userLocation == null) return@remember ""
        val ruler = userLocation.createCheapRuler()
        return@remember formatDistanceAndDirection(
            ruler.distance(userLocation, locationDescription.location),
            ruler.bearing(userLocation, locationDescription.location),
            context)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = locationDescription.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        locationDescription.typeDescription?.let {
            if(it.additionalText?.isNotEmpty() == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it.additionalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if(distanceString.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = distanceString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        locationDescription.description?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationDetailsPreview() {
    LocationDetails(
        LocationDescription(
            name = "Pizza hut",
            location = LngLatAlt(),
            description = "139 boulevard gambetta \n59000 Lille\nFrance",
            typeDescription = TextForFeature("Blah", false,"Restaurant")
        ),
        createBeacon = { _ ->
        },
        enableStreetPreview = { _ ->
        },
        getLocationDescription = { _ ->
            LocationDescription("Current location", LngLatAlt())
        },
        navController = NavHostController(LocalContext.current),
        location = null,
        heading = 45.0F,
        saveMarker = {_,_,_,_ ->},
        deleteMarker = {},
        shareLocation = {_,_ ->},
        offlineMaps = {_ ->},
        showDialog = {}
    )
}
