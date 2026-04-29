package org.scottishtecharmy.soundscape.screens.home.locationDetails

import org.scottishtecharmy.soundscape.resources.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.navigation.NavHostController
import com.google.gson.GsonBuilder
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.generateOfflineMapScreenRoute
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun generateLocationDetailsRoute(locationDescription: LocationDescription): String {
    val json = GsonBuilder().create().toJson(locationDescription)
    return "${HomeRoutes.LocationDetails.route}/${URLEncoder.encode(json, StandardCharsets.UTF_8.toString())}"
}

@Composable
fun LocationDetailsScreen(
    locationDescription: LocationDescription,
    location: LngLatAlt?,
    heading: Float,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: LocationDetailsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val preferencesProvider: PreferencesProvider = koinInject()

    // Check if this location already exists as a marker in the database
    val finalLocationDescription = produceState(locationDescription, locationDescription) {
        if (locationDescription.databaseId == 0L) {
            val existingMarker = viewModel.getMarkerAtLocation(locationDescription.location)
            if (existingMarker != null) {
                value = locationDescription.copy(
                    databaseId = existingMarker.markerId,
                    name = existingMarker.name,
                    description = existingMarker.fullAddress
                )
            }
        }
    }.value

    val dialogState = remember { mutableStateOf(false) }

    // Parse share message markdown once
    val shareMessageResource = stringResource(Res.string.universal_links_marker_share_message)
    val shareMessage = remember(shareMessageResource) {
        val parser = Parser.builder().build()
        val document = parser.parse(shareMessageResource)
        val renderer = HtmlRenderer.builder().build()
        AnnotatedString.fromHtml(
            htmlString = renderer.render(document),
            linkStyles = TextLinkStyles(
                style = SpanStyle(textDecoration = TextDecoration.Underline)
            )
        ).text
    }

    if (dialogState.value) {
        SaveAndEditMarkerDialog(
            locationDescription = finalLocationDescription,
            location = location,
            heading = heading,
            saveMarker = { description, successMessage, failureMessage, duplicateMessage ->
                viewModel.createMarker(description, successMessage, failureMessage, duplicateMessage)
                navController.popBackStack(HomeRoutes.Home.route, false)
            },
            deleteMarker = { id ->
                viewModel.deleteMarker(id)
                navController.popBackStack(HomeRoutes.MarkersAndRoutes.route, false)
            },
            modifier = modifier,
            dialogState = dialogState,
        )
    } else {
        SharedLocationDetailsScreen(
            locationDescription = finalLocationDescription,
            userLocation = location,
            heading = heading,
            preferencesProvider = preferencesProvider,
            onNavigateUp = { navController.popBackStack() },
            onStartBeacon = { loc, name ->
                viewModel.startBeacon(loc, name)
                navController.popBackStack(HomeRoutes.Home.route, false)
            },
            onSaveMarker = { _ ->
                viewModel.showDialog()
                dialogState.value = true
            },
            onEditMarker = { _ ->
                dialogState.value = true
            },
            onEnableStreetPreview = { loc ->
                viewModel.enableStreetPreview(loc)
                navController.popBackStack(HomeRoutes.Home.route, false)
            },
            onShareLocation = { desc ->
                viewModel.shareLocation(context, shareMessage, desc)
                navController.popBackStack(HomeRoutes.Home.route, false)
            },
            onOfflineMaps = { desc ->
                navController.navigate(generateOfflineMapScreenRoute(desc))
            },
        )
    }
}
