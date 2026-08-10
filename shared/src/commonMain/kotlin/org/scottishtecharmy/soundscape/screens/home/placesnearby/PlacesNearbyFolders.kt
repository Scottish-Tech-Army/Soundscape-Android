package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.ForkLeft
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.featureHasEntrances
import org.scottishtecharmy.soundscape.geoengine.utils.featureIsInFilterGroup
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.all_places_nearby_description
import org.scottishtecharmy.soundscape.resources.banks_places_nearby_description
import org.scottishtecharmy.soundscape.resources.filter_all
import org.scottishtecharmy.soundscape.resources.filter_banks
import org.scottishtecharmy.soundscape.resources.filter_food_drink
import org.scottishtecharmy.soundscape.resources.filter_groceries
import org.scottishtecharmy.soundscape.resources.filter_transit
import org.scottishtecharmy.soundscape.resources.food_drink_places_nearby_description
import org.scottishtecharmy.soundscape.resources.groceries_places_nearby_description
import org.scottishtecharmy.soundscape.resources.intersections_places_nearby_description
import org.scottishtecharmy.soundscape.resources.osm_intersection
import org.scottishtecharmy.soundscape.resources.public_transit_places_nearby_description
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription

data class Folder(
    val nameResource: StringResource,
    val icon: ImageVector,
    val filter: String,
    val talkbackDescriptionResource: StringResource
)

val placesNearbyFolders = listOf(
    Folder(
        Res.string.filter_all,
        Icons.Rounded.ControlCamera,
        "",
        Res.string.all_places_nearby_description
    ),
    Folder(
        Res.string.filter_transit,
        Icons.Rounded.DirectionsBus,
        "transit",
        Res.string.public_transit_places_nearby_description
    ),
    Folder(
        Res.string.filter_food_drink,
        Icons.Rounded.Fastfood,
        "food_and_drink",
        Res.string.food_drink_places_nearby_description
    ),
    Folder(
        Res.string.filter_groceries,
        Icons.Rounded.LocalGroceryStore,
        "groceries",
        Res.string.groceries_places_nearby_description
    ),
    Folder(
        Res.string.filter_banks,
        Icons.Rounded.AttachMoney,
        "banks",
        Res.string.banks_places_nearby_description
    ),
    Folder(
        Res.string.osm_intersection,
        Icons.Rounded.ForkLeft,
        "intersections",
        Res.string.intersections_places_nearby_description
    ),
)

fun filterLocations(
    uiState: PlacesNearbyUiState,
    strings: LocalizedStrings,
): List<LocationDescription> {
    val location = uiState.userLocation ?: LngLatAlt()
    val ruler = CheapRuler(location.latitude)
    return if (uiState.filter == "intersections") {
        uiState.nearbyIntersections.features.filter { feature ->
            // Filter out un-named intersections
            (feature as MvtFeature).name.toString().isNotEmpty()
        }.map { feature ->
            LocationDescription(
                name = (feature as MvtFeature).name.toString(),
                location = getDistanceToFeature(location, feature, ruler).point
            )
        }.sortedBy {
            uiState.userLocation?.let { location ->
                ruler.distance(location, it.location)
            } ?: 0.0
        }
    } else {
        uiState.nearbyPlaces.features.filter { feature ->
            // Filter based on any folder selected and filter out POIs with entrances
            !featureHasEntrances(feature) &&
                    featureIsInFilterGroup(feature, uiState.filter) &&
                    (feature as MvtFeature).getText(strings).text.isNotEmpty()
        }.map { feature ->
            feature.deferredToLocationDescription(
                LocationSource.OfflineGeocoder,
                getDistanceToFeature(location, feature, ruler).point,
                (feature as MvtFeature).getText(strings)
            )
        }.sortedBy {
            uiState.userLocation?.let { location ->
                ruler.distance(location, it.location)
            } ?: 0.0
        }
    }
}
