package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.gson.GsonBuilder
import org.koin.androidx.compose.koinViewModel
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.SharedAddAndEditRouteScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class SimpleMarkerData(
    var addressName: String = "",
    var location: LngLatAlt = LngLatAlt(),
)

private data class SimpleRouteData(
    var name: String = "",
    var description: String = "",
    var waypoints: MutableList<SimpleMarkerData> = mutableListOf(),
)

fun generateRouteDetailsRoute(routeData: RouteWithMarkers): String {
    val simpleRouteData = SimpleRouteData()
    simpleRouteData.name = routeData.route.name
    simpleRouteData.description = routeData.route.description
    for (waypoint in routeData.markers) {
        simpleRouteData.waypoints.add(
            SimpleMarkerData(
                waypoint.name,
                LngLatAlt(waypoint.longitude, waypoint.latitude),
            )
        )
    }

    val json = GsonBuilder().create().toJson(simpleRouteData)
    val urlEncodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
    return "${HomeRoutes.AddAndEditRoute.route}?command=import&data=$urlEncodedJson"
}

fun parseSimpleRouteData(jsonData: String): RouteWithMarkers {
    val gson = GsonBuilder().create()
    val simpleRouteData = gson.fromJson(jsonData, SimpleRouteData::class.java)

    val markers = mutableListOf<MarkerEntity>()
    for (waypoint in simpleRouteData.waypoints) {
        markers.add(
            MarkerEntity(
                name = waypoint.addressName,
                longitude = waypoint.location.longitude,
                latitude = waypoint.location.latitude,
            )
        )
    }
    return RouteWithMarkers(
        RouteEntity(
            name = simpleRouteData.name,
            description = simpleRouteData.description,
        ),
        markers,
    )
}

@Composable
fun AddAndEditRouteScreenVM(
    navController: NavController,
    modifier: Modifier,
    userLocation: LngLatAlt?,
    editRoute: Boolean,
    heading: Float,
    getCurrentLocationDescription: () -> LocationDescription,
    viewModel: AddAndEditRouteViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    SharedAddAndEditRouteScreen(
        holder = viewModel.holder,
        isEditing = editRoute,
        userLocation = userLocation,
        heading = heading,
        getCurrentLocationDescription = getCurrentLocationDescription,
        onNavigateUp = { navController.popBackStack() },
        onSaveComplete = { navController.popBackStack() },
        onDeleteComplete = {
            navController.navigate(HomeRoutes.MarkersAndRoutes.route + "?tab=routes") {
                popUpTo(HomeRoutes.Home.route) { inclusive = false }
            }
        },
        onShowError = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        },
        onShowSuccess = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
    )
}
