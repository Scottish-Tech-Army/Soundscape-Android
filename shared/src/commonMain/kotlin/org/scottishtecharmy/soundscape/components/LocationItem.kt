package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.location_item_move_down
import org.scottishtecharmy.soundscape.resources.location_item_move_up
import org.scottishtecharmy.soundscape.resources.location_item_not_selected
import org.scottishtecharmy.soundscape.resources.location_item_selected
import org.scottishtecharmy.soundscape.resources.route_detail_action_start_route_hint
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

data class EnabledFunction(
    var enabled: Boolean = false,
    var functionLocation: (LocationDescription) -> Unit = {},
    var functionBoolean: (Boolean) -> Unit = {},
    var value: Boolean = false,
    var hintWhenOn: String = "",
    var hintWhenOff: String = "",
    var hint: String = ""
)

data class LocationItemDecoration(
    val location: Boolean = false,
    val source: LocationSource = LocationSource.UnknownSource,
    val index: Int = -1,
    val editRoute: EnabledFunction = EnabledFunction(),
    val details: EnabledFunction = EnabledFunction(),
    var indexDescription: String = "",
    var reorderable: Boolean = false,
    var moveUp: (Int) -> Boolean = { false },
    var moveDown: (Int) -> Boolean = { false },
    var startPlayback: EnabledFunction = EnabledFunction()
)

@Composable
fun LocationItem(
    item: LocationDescription,
    userLocation: LngLatAlt?,
    modifier: Modifier = Modifier,
    decoration: LocationItemDecoration = LocationItemDecoration(),
) {
    var distanceString = ""
    var distanceStringA11y = ""
    val selectionText =
        if (decoration.editRoute.value) stringResource(Res.string.location_item_selected) else stringResource(
            Res.string.location_item_not_selected
        )
    val moveUpLabel = stringResource(Res.string.location_item_move_up)
    val moveUpDown = stringResource(Res.string.location_item_move_down)
    val defaultStartPlaybackLabel = stringResource(Res.string.route_detail_action_start_route_hint)
    val startPlaybackLabel = decoration.startPlayback.hint.ifEmpty { defaultStartPlaybackLabel }
    if (userLocation != null) {
        val ruler = item.location.createCheapRuler()
        val distance = ruler.distance(userLocation, item.location)
        val bearing = ruler.bearing(userLocation, item.location)
        val localized = ComposeLocalizedStrings()
        distanceString = formatDistanceAndDirection(distance, bearing, localized)
        distanceStringA11y = formatDistanceAndDirection(
            distance, bearing, localized, forAccessibility = true
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .smallPadding()
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                if (decoration.details.enabled) {
                    decoration.details.functionLocation(item)
                } else if (decoration.editRoute.enabled) {
                    decoration.editRoute.functionBoolean(!decoration.editRoute.value)
                }
            }
            .testTag("LocationItem-${item.name}-${item.orderId}")
            .clearAndSetSemantics {
                contentDescription = when {
                    decoration.editRoute.enabled ->
                        "$selectionText. ${item.name}"

                    decoration.index != -1 ->
                        "${decoration.indexDescription} ${decoration.index + 1}. ${item.name}"

                    else -> item.description?.takeIf { it.startsWith(item.name) }
                        ?: listOfNotNull(
                            item.name,
                            item.typeDescription?.additionalText,
                            item.description ?: item.street,
                            distanceStringA11y
                        ).joinToString(", ")
                }

                if (decoration.editRoute.enabled) {
                    onClick(
                        label = if (decoration.editRoute.value) decoration.editRoute.hintWhenOn else decoration.editRoute.hintWhenOff,
                        action = { false }
                    )
                }
                if (decoration.reorderable) {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = moveUpLabel,
                            action = { decoration.moveUp(decoration.index) }
                        ),
                        CustomAccessibilityAction(
                            label = moveUpDown,
                            action = { decoration.moveDown(decoration.index) }
                        ),
                    )
                }
                if (decoration.startPlayback.enabled) {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = startPlaybackLabel,
                            action = {
                                decoration.startPlayback.functionLocation(item)
                                true
                            }
                        ),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (decoration.location) {
            val icon = when (decoration.source) {
                LocationSource.OfflineGeocoder -> Icons.Rounded.LocationOff
                else -> Icons.Rounded.LocationOn
            }
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(spacing.icon)
            )
        } else if (decoration.index != -1) {
            Text(
                text = (decoration.index + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(spacing.targetSize).align(Alignment.CenterVertically)
            )
        }
        Column(
            modifier = Modifier.padding(start = spacing.small).weight(1F),
        ) {
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            if (distanceString.isNotEmpty()) {
                Text(
                    text = distanceString,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            // A POI with a real address shows it; one without falls back to the street it was
            // associated with at tile load, which is what tells a screen full of identical
            // "Post Box" rows apart.
            val addressLine = item.description?.takeIf { it.isNotEmpty() }
                ?: item.street?.takeIf { it.isNotEmpty() }
            addressLine?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (item.typeDescription?.generic != true) {
                item.typeDescription?.additionalText?.let { text ->
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (decoration.editRoute.enabled) {
            Switch(
                checked = decoration.editRoute.value,
                onCheckedChange = null,                     // Handled by the row
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    uncheckedThumbColor = Color.Red,
                ),
                modifier = Modifier.extraSmallPadding().testTag("locationItemEditRouteSwitch")
            )
        } else if (decoration.details.enabled) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(spacing.icon).testTag("locationItemDetailsIcon")
            )
        }
    }
    if (!decoration.reorderable) {
        HorizontalDivider(
            thickness = spacing.tiny,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun FolderItem(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .smallPadding()
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                onClick()
            }
            .testTag("folderItem-$name"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(spacing.icon)
        )
        Column(
            modifier = Modifier.padding(start = spacing.small).weight(1F),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(spacing.icon)
        )
    }
    HorizontalDivider(
        thickness = spacing.tiny,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
